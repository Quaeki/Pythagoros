package com.example.pythagoros.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.presentation.components.Motion
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.ProBadge
import com.example.pythagoros.presentation.components.SystemBarsAppearance
import com.example.pythagoros.presentation.components.pressClickable
import com.example.pythagoros.presentation.components.pressScale
import com.example.pythagoros.presentation.components.rememberPressFeedback
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.Dark4
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.ScannerBackground
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.Warn
import java.io.File
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Рамка кадрирования 6c: отступы от краёв экрана, как в макете 390×800. */
private val CropTop = 186.dp
private val CropBottom = 214.dp
private val CropSide = 26.dp
private val CropRadius = 18.dp
private val CropCorner = 30.dp
private val CropStroke = 3.dp

/** Рамка вокруг найденного чертежа (6b) — выше и уже, панель Pro занимает низ. */
private val FigureTop = 130.dp
private val FigureBottom = 300.dp

/**
 * 6c. Сканер условия — заменяет прежний экран камеры.
 *
 * В видоискателе нет ничего, кроме кадрирования и одной подписи: ни вспышки,
 * ни режимов съёмки, ни живых подсказок. Тип задачи определяется локально уже
 * после снимка — если это геометрия или физика без Pro, поверх кадра поднимается
 * гейт 6b, и платное распознавание не запускается.
 */
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
    onClose: () -> Unit = {},
    onShutter: (String) -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onManualInput: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onClassifyImage: suspend (String) -> ProblemType = { ProblemType.Unknown },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureInProgress by remember { mutableStateOf(false) }
    // Путь снимка, упёршегося в Pro-гейт: он нужен кнопке «Показать только ответ».
    var gate by remember { mutableStateOf<String?>(null) }
    // Гейт уезжает вниз анимированно, а путь обнуляется сразу — тип задачи
    // держим отдельно, иначе шторка на выходе меняла бы текст.
    var gateType by remember { mutableStateOf(ProblemType.Geometry) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    SystemBarsAppearance(lightIcons = true)
    Box(
        modifier
            .fillMaxSize()
            .background(ScannerBackground)
    ) {
        if (hasCameraPermission) {
            ScannerPreview(
                onImageCaptureReady = { imageCapture = it },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(26.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Разрешите доступ к камере, чтобы снять условие",
                    color = SurfaceWhite,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                )
                Box(Modifier.height(14.dp))
                PrimaryButton("Разрешить", height = 48.dp, fontSize = 15.sp) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        // Кадрирование и управление живут только в режиме съёмки: под гейтом
        // видоискатель показывает уже снятое, снимать поверх него нечего.
        AnimatedVisibility(
            visible = gate == null,
            enter = fadeIn(tween(Motion.Scrim)),
            exit = fadeOut(tween(Motion.Scrim)),
        ) {
            Box(Modifier.fillMaxSize()) {
                CropOverlay(Modifier.fillMaxSize())

                ViewfinderCaption("Наведите на условие", topPadding = CropTop - 54.dp)

                val closePress = rememberPressFeedback()
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(start = CropSide, top = 18.dp)
                        .pressScale(closePress)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite.copy(alpha = 0.14f))
                        .pressClickable(closePress, onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        PythIcons.Close,
                        contentDescription = "Закрыть сканер",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(17.dp),
                    )
                }

                // Панель плавает поверх превью, без собственного фона.
                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = CropSide, end = CropSide, top = 22.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val galleryPress = rememberPressFeedback()
                    Box(
                        Modifier
                            .pressScale(galleryPress)
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Dark4)
                            .border(1.dp, SurfaceWhite.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                            .pressClickable(galleryPress, onClick = onPickFromGallery),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            PythIcons.Gallery,
                            contentDescription = "Выбрать из галереи",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    val shutterPress = rememberPressFeedback(
                        enabled = !captureInProgress,
                        pressedScale = 0.92f,
                    )
                    Box(
                        Modifier
                            .pressScale(shutterPress)
                            .size(84.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .pressClickable(shutterPress, enabled = !captureInProgress) {
                                if (!hasCameraPermission) {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                    return@pressClickable
                                }
                                val capture = imageCapture ?: return@pressClickable
                                captureInProgress = true
                                takeScannerPhoto(
                                    context = context,
                                    imageCapture = capture,
                                    onSaved = { file ->
                                        scope.launch {
                                            val path = file.absolutePath
                                            // Классификация локальная и быстрая: платный разбор
                                            // не должен уходить в сеть раньше, чем станет ясно,
                                            // покажем ли мы гейт.
                                            val type = runCatching { onClassifyImage(path) }
                                                .getOrDefault(ProblemType.Unknown)
                                            captureInProgress = false
                                            if (!isPro && type.needsPro()) {
                                                gateType = type
                                                gate = path
                                            } else {
                                                onShutter(path)
                                            }
                                        }
                                    },
                                    onFailed = { captureInProgress = false },
                                )
                            },
                    )

                    val inputPress = rememberPressFeedback()
                    Box(
                        Modifier
                            .pressScale(inputPress)
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceWhite.copy(alpha = 0.14f))
                            .pressClickable(inputPress, onClick = onManualInput),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            PythIcons.Keyboard,
                            contentDescription = "Ввести условие вручную",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        ScannerProGate(
            visible = gate != null,
            problemType = gateType,
            onSubscribe = onOpenPaywall,
            onAnswerOnly = {
                val path = gate
                gate = null
                path?.let(onShutter)
            },
            onRetake = { gate = null },
        )
    }
}

private fun ProblemType.needsPro(): Boolean =
    this == ProblemType.Physics || this == ProblemType.Geometry

/** Затемнение вокруг рамки и четыре угловых маркера. Статично, не анимируется. */
@Composable
private fun CropOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val crop = Rect(
            left = CropSide.toPx(),
            top = CropTop.toPx(),
            right = size.width - CropSide.toPx(),
            bottom = size.height - CropBottom.toPx(),
        )
        val radius = CropRadius.toPx()

        val hole = Path().apply { addRoundRect(RoundRect(crop, CornerRadius(radius))) }
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(ScannerBackground.copy(alpha = 0.55f))
        }

        listOf(
            Triple(crop.left, crop.top, 1f to 1f),
            Triple(crop.right, crop.top, -1f to 1f),
            Triple(crop.left, crop.bottom, 1f to -1f),
            Triple(crop.right, crop.bottom, -1f to -1f),
        ).forEach { (x, y, dir) ->
            drawCropCorner(Offset(x, y), dir.first, dir.second, radius)
        }
    }
}

/**
 * Уголок рамки: два коротких луча со скруглением к внешнему углу — тот же
 * радиус 18, что у выреза, поэтому маркер ложится ровно на его край.
 */
private fun DrawScope.drawCropCorner(corner: Offset, dx: Float, dy: Float, radius: Float) {
    val leg = CropCorner.toPx()
    val center = Offset(corner.x + radius * dx, corner.y + radius * dy)
    val path = Path().apply {
        moveTo(corner.x + leg * dx, corner.y)
        lineTo(center.x, corner.y)
        arcTo(
            rect = Rect(center = center, radius = radius),
            startAngleDegrees = if (dy > 0) 270f else 90f,
            sweepAngleDegrees = -90f * dx * dy,
            forceMoveTo = false,
        )
        lineTo(corner.x, corner.y + leg * dy)
    }
    drawPath(
        path = path,
        color = SurfaceWhite,
        style = Stroke(width = CropStroke.toPx(), cap = StrokeCap.Round),
    )
}

/**
 * Единственная подпись видоискателя — общий язык для 6c и 6b.
 *
 * В макете это Space Grotesk, но кириллицы в нём нет, поэтому русский текст
 * набираем интерфейсным шрифтом (см. `DisplayFont` в теме).
 */
@Composable
private fun ViewfinderCaption(text: String, topPadding: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = CropSide, end = CropSide),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text,
            color = SurfaceWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 6b. Pro-гейт прямо в сканере: условие уже снято, но разбор с чертежом — платный.
 * Ответ гейт не закрывает — на это есть кнопка «Показать только ответ».
 */
@Composable
private fun ScannerProGate(
    visible: Boolean,
    problemType: ProblemType,
    onSubscribe: () -> Unit,
    onAnswerOnly: () -> Unit,
    onRetake: () -> Unit,
) {
    val geometry = problemType == ProblemType.Geometry
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Scrim)),
        exit = fadeOut(tween(Motion.Scrim, delayMillis = Motion.Sheet - Motion.Scrim)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // Тонкая рамка вокруг того, что нашли в кадре, и подпись над ней —
            // тем же языком, что подсказка сканера.
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = FigureTop, bottom = FigureBottom, start = CropSide, end = CropSide)
                    .clip(RoundedCornerShape(CropRadius))
                    .background(Accent.copy(alpha = 0.1f))
                    .border(2.dp, Accent, RoundedCornerShape(CropRadius))
            )
            ViewfinderCaption(
                if (geometry) "Чертёж найден" else "Задача по физике найдена",
                topPadding = FigureTop - 36.dp,
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.40f to ScannerBackground.copy(alpha = 0f),
                            0.62f to ScannerBackground.copy(alpha = 0.9f),
                            0.78f to ScannerBackground,
                        )
                    ),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column(
                    Modifier
                        .animateEnterExit(
                            enter = slideInVertically(
                                animationSpec = tween(Motion.Sheet, easing = Motion.Emphasized),
                                initialOffsetY = { it / 3 },
                            ),
                            exit = slideOutVertically(
                                animationSpec = tween(Motion.Sheet, easing = Motion.Accelerated),
                                targetOffsetY = { it / 3 },
                            ),
                        )
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 30.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        ProBadge(background = AccentTint)
                        Text(
                            if (geometry) "Геометрия · треугольник" else "Физика · механика",
                            color = TextOnDarkSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Text(
                        if (geometry) "На фото задача с чертежом" else "На фото задача по механике",
                        color = SurfaceWhite,
                        fontSize = 25.sp,
                        lineHeight = 31.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "AI из Pro построит чертёж по условию и покажет решение по шагам. " +
                            "Условие уже распознано — откройте Pro, чтобы увидеть разбор.",
                        color = TextOnDarkSecondary,
                        fontSize = 14.5.sp,
                        lineHeight = 22.5.sp,
                    )
                    PrimaryButton(
                        text = "Открыть Pro — 7 дней бесплатно",
                        background = Warn,
                        contentColor = Ink,
                        height = 56.dp,
                        fontSize = 16.5.sp,
                        onClick = onSubscribe,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GateSecondaryButton("Показать только ответ", Modifier.weight(1f), onAnswerOnly)
                        GateSecondaryButton("Снять заново", Modifier.weight(1f), onRetake)
                    }
                }
            }
        }
    }
}

@Composable
private fun GateSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Dark4, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = SurfaceWhite, fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ScannerPreview(
    onImageCaptureReady: (ImageCapture?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val currentCameraProvider by rememberUpdatedState(boundCameraProvider)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(previewView, lifecycleOwner) {
        val cameraProvider = context.awaitCameraProvider()
        boundCameraProvider = cameraProvider
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(ImageCapture.FLASH_MODE_OFF)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 960),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                        )
                    )
                    .build()
            )
            .setJpegQuality(85)
            .build()

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
        )
        onImageCaptureReady(capture)
    }

    DisposableEffect(Unit) {
        onDispose {
            onImageCaptureReady(null)
            currentCameraProvider?.unbindAll()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun takeScannerPhoto(
    context: Context,
    imageCapture: ImageCapture,
    onSaved: (File) -> Unit,
    onFailed: (ImageCaptureException) -> Unit,
) {
    val photoFile = File(context.cacheDir, "pythagoros-${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) = onSaved(photoFile)
            override fun onError(exception: ImageCaptureException) = onFailed(exception)
        },
    )
}

private suspend fun Context.awaitCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ continuation.resume(future.get()) }, ContextCompat.getMainExecutor(this))
    }
