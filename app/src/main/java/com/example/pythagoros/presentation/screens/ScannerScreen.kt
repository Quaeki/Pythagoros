package com.example.pythagoros.presentation.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.pythagoros.data.ocr.FrameInsight
import com.example.pythagoros.data.ocr.LiveFrameAnalyzer
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.presentation.components.FootnoteText
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
import com.example.pythagoros.ui.theme.Dark5
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.Mint
import com.example.pythagoros.ui.theme.ScannerBackground
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.Warn
import java.io.File
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Режимы съёмки: влияют на кроп кадра и на подсказку. */
enum class CaptureMode(val title: String) {
    Problem("Задача"),
    Figure("Чертёж"),
    Plot("График"),
    Page("Страница"),
}

/** Статус фокуса над рамкой (макет 6c). */
private enum class FocusState(val title: String) {
    Searching("Ищем текст в рамке"),
    Ready("Текст в фокусе · можно снимать"),
}

/**
 * 6c. Сканер условия — заменяет прежний экран камеры.
 *
 * Тип задачи определяется по живому кадру локально (ML Kit), поэтому подсказка
 * и Pro-гейт появляются до нажатия затвора и до платного распознавания.
 */
@Composable
fun ScannerScreen(
    modifier: Modifier = Modifier,
    isPro: Boolean = false,
    onClose: () -> Unit = {},
    onShutter: (String) -> Unit = {},
    onPickFromGallery: () -> Unit = {},
    onManualInput: () -> Unit = {},
    onHelp: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var captureInProgress by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(CaptureMode.Problem) }
    var insight by remember { mutableStateOf(FrameInsight()) }
    var gateDismissed by remember { mutableStateOf(false) }

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

    val proKind = insight.problemType.takeIf { it == ProblemType.Physics || it == ProblemType.Geometry }
    val gateVisible = proKind != null && !isPro && !gateDismissed

    // Кадр меняется каждые несколько миллисекунд, и тип задачи может пропасть
    // прямо во время анимации ухода — последний известный держим отдельно.
    var hintKind by remember { mutableStateOf(ProblemType.Physics) }
    LaunchedEffect(proKind) {
        if (proKind != null) hintKind = proKind
    }

    SystemBarsAppearance(lightIcons = true)
    Box(
        modifier
            .fillMaxSize()
            .background(ScannerBackground)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = 44.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GlassCircleButton(PythIcons.Close, "Закрыть", onClick = onClose)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassCircleButton(
                        icon = PythIcons.Bolt,
                        contentDescription = if (flashOn) "Выключить вспышку" else "Включить вспышку",
                        tint = if (flashOn) Warn else SurfaceWhite,
                        onClick = { flashOn = !flashOn },
                    )
                    GlassCircleButton(PythIcons.Question, "Как снимать", onClick = onHelp)
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Dark5)
            ) {
                if (hasCameraPermission) {
                    ScannerPreview(
                        flashOn = flashOn,
                        onImageCaptureReady = { imageCapture = it },
                        onInsight = { insight = it },
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
                        )
                        Box(Modifier.height(14.dp))
                        PrimaryButton("Разрешить", height = 48.dp, fontSize = 15.sp) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }

                ScannerFrame(Modifier.fillMaxSize())

                FocusPill(
                    state = if (insight.hasText) FocusState.Ready else FocusState.Searching,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 44.dp),
                )

                // Подсказка приходит от анализа кадра, то есть посреди наводки —
                // выезд снизу читается спокойнее, чем мгновенное появление.
                // Полное имя — иначе перегрузка разрешается в ColumnScope-вариант.
                androidx.compose.animation.AnimatedVisibility(
                    visible = proKind != null && !gateVisible,
                    enter = slideInVertically(
                        animationSpec = tween(Motion.Step, easing = Motion.Emphasized),
                        initialOffsetY = { it / 2 },
                    ) + fadeIn(tween(Motion.Step)),
                    exit = fadeOut(tween(Motion.Step)),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 88.dp),
                ) {
                    LiveHint(problemType = hintKind)
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                CaptureMode.entries.forEach { item ->
                    ModePill(item, active = item == mode, onClick = { mode = item })
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, top = 6.dp, bottom = 26.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val galleryPress = rememberPressFeedback()
                Box(
                    Modifier
                        .pressScale(galleryPress)
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Dark4)
                        .border(1.5.dp, SurfaceWhite.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
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
                    pressedScale = 0.9f,
                )
                Box(
                    Modifier
                        .pressScale(shutterPress)
                        .size(82.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite)
                        .border(6.dp, Accent, CircleShape)
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
                                    captureInProgress = false
                                    onShutter(file.absolutePath)
                                },
                                onFailed = { captureInProgress = false },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    // Пока кадр сохраняется, «зрачок» затвора сжимается — снимок принят.
                    val innerSize by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (captureInProgress) 42.dp else 54.dp,
                        animationSpec = tween(Motion.Press * 2, easing = Motion.Emphasized),
                        label = "shutterCore",
                    )
                    Box(
                        Modifier
                            .size(innerSize)
                            .clip(CircleShape)
                            .background(Accent)
                    )
                }

                val inputPress = rememberPressFeedback()
                Column(
                    Modifier
                        .pressScale(inputPress)
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(SurfaceWhite.copy(alpha = 0.12f))
                        .pressClickable(inputPress, onClick = onManualInput),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        PythIcons.Keyboard,
                        contentDescription = null,
                        tint = SurfaceWhite,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "Ввод",
                        color = SurfaceWhite,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        ScannerProGate(
            visible = gateVisible,
            problemType = hintKind,
            onSubscribe = onOpenPaywall,
            onAnswerOnly = { gateDismissed = true },
            onRetake = { gateDismissed = true },
        )
    }
}

/** Четыре угловых маркера, затемнение снаружи и бегущая линия сканирования. */
@Composable
private fun ScannerFrame(modifier: Modifier = Modifier) {
    val progress by rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "scanLine",
    )

    Canvas(modifier) {
        val inset = Rect(
            left = 20.dp.toPx(),
            top = 88.dp.toPx(),
            right = size.width - 20.dp.toPx(),
            bottom = size.height - 150.dp.toPx(),
        )
        val radius = CornerRadius(16.dp.toPx())

        val hole = Path().apply { addRoundRect(RoundRect(inset, radius)) }
        clipPath(hole, clipOp = ClipOp.Difference) {
            drawRect(ScannerBackground.copy(alpha = 0.5f))
        }

        val corner = 34.dp.toPx()
        val stroke = 4.dp.toPx()
        listOf(
            Triple(inset.left, inset.top, 1f to 1f),
            Triple(inset.right, inset.top, -1f to 1f),
            Triple(inset.left, inset.bottom, 1f to -1f),
            Triple(inset.right, inset.bottom, -1f to -1f),
        ).forEach { (x, y, dir) ->
            val (dx, dy) = dir
            drawLine(
                color = Warn,
                start = Offset(x, y),
                end = Offset(x + corner * dx, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Warn,
                start = Offset(x, y),
                end = Offset(x, y + corner * dy),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        val lineY = inset.top + (inset.bottom - inset.top) * progress
        drawLine(
            brush = Brush.horizontalGradient(
                0f to Warn.copy(alpha = 0f),
                0.5f to Warn,
                1f to Warn.copy(alpha = 0f),
                startX = inset.left,
                endX = inset.right,
            ),
            start = Offset(inset.left, lineY),
            end = Offset(inset.right, lineY),
            strokeWidth = 3.dp.toPx(),
        )
    }
}

@Composable
private fun FocusPill(state: FocusState, modifier: Modifier = Modifier) {
    val ready = state == FocusState.Ready
    // Статус переключается по каждому проанализированному кадру: без перехода
    // пилюля мигала бы зелёным на границе распознавания.
    val fill by animateColorAsState(
        targetValue = if (ready) Mint.copy(alpha = 0.16f) else SurfaceWhite.copy(alpha = 0.14f),
        animationSpec = tween(Motion.Tint),
        label = "focusFill",
    )
    val stroke by animateColorAsState(
        targetValue = if (ready) Mint else Dark4,
        animationSpec = tween(Motion.Tint),
        label = "focusStroke",
    )
    val ink by animateColorAsState(
        targetValue = if (ready) Mint else SurfaceWhite,
        animationSpec = tween(Motion.Tint),
        label = "focusInk",
    )
    Box(
        modifier
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, stroke, CircleShape)
            .padding(horizontal = 13.dp, vertical = 7.dp)
    ) {
        Text(
            state.title,
            color = ink,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Стеклянная подсказка: что распознано в кадре и что за это даёт Pro. */
@Composable
private fun LiveHint(problemType: ProblemType, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (problemType == ProblemType.Geometry) PythIcons.Triangle else PythIcons.Bolt,
                contentDescription = null,
                tint = SurfaceWhite,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(
            buildString {
                append(if (problemType == ProblemType.Geometry) "Похоже на геометрию" else "Похоже на физику")
                append(" — разбор в Pro")
            },
            color = SurfaceWhite,
            fontSize = 13.5.sp,
            lineHeight = 19.5.sp,
        )
    }
}

@Composable
private fun ModePill(mode: CaptureMode, active: Boolean, onClick: () -> Unit) {
    val press = rememberPressFeedback()
    val fill by animateColorAsState(
        targetValue = if (active) SurfaceWhite else SurfaceWhite.copy(alpha = 0.12f),
        animationSpec = tween(Motion.Tint),
        label = "modeFill",
    )
    val ink by animateColorAsState(
        targetValue = if (active) Ink else SurfaceWhite,
        animationSpec = tween(Motion.Tint),
        label = "modeInk",
    )
    Box(
        Modifier
            .pressScale(press)
            .clip(CircleShape)
            .background(fill)
            .pressClickable(press, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(
            mode.title,
            color = ink,
            fontSize = 13.5.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = SurfaceWhite,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback()
    val iconTint by animateColorAsState(tint, tween(Motion.Tint), label = "glassTint")
    Box(
        modifier
            .pressScale(press)
            .size(38.dp)
            .clip(CircleShape)
            .background(SurfaceWhite.copy(alpha = 0.12f))
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = iconTint, modifier = Modifier.size(17.dp))
    }
}

/**
 * 6b. Pro-гейт прямо в сканере: условие уже видно, но разбор с чертежом — платный.
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
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(Motion.Scrim)),
        exit = fadeOut(tween(Motion.Scrim, delayMillis = Motion.Sheet - Motion.Scrim)),
    ) {
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
                        if (problemType == ProblemType.Geometry) "Геометрия · треугольник" else "Физика · механика",
                        color = TextOnDarkSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    if (problemType == ProblemType.Geometry) {
                        "На фото задача с чертежом"
                    } else {
                        "На фото задача по механике"
                    },
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
    flashOn: Boolean,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onInsight: (FrameInsight) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    LaunchedEffect(previewView, lifecycleOwner, flashOn) {
        val cameraProvider = context.awaitCameraProvider()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            .build()
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(analysisExecutor, LiveFrameAnalyzer(onInsight)) }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            capture,
            analysis,
        )
        onImageCaptureReady(capture)
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
