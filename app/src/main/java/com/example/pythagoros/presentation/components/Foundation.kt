package com.example.pythagoros.presentation.components

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.BorderColor
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Caption
import com.example.pythagoros.ui.theme.CaptionOnDark
import com.example.pythagoros.ui.theme.DashedBorderColor
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Divider
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextTertiary

/** Горизонтальные поля экрана в макете — 20dp. */
val ScreenPadding = 20.dp

/** Поля экранов онбординга и входа — там макет даёт 24dp. */
val WideScreenPadding = 24.dp

/**
 * Корневой контейнер экрана.
 *
 * Статус-бар в макете нарисован («9:41 · 84%»), в приложении он системный:
 * содержимое отодвигаем через [systemBarsPadding], а цвет его иконок задаём по фону.
 */
@Composable
fun PythScreen(
    modifier: Modifier = Modifier,
    background: Color = SurfaceWhite,
    lightStatusBarIcons: Boolean = background.luminanceIsDark(),
    content: @Composable ColumnScope.() -> Unit,
) {
    SystemBarsAppearance(lightIcons = lightStatusBarIcons)
    Column(
        modifier
            .fillMaxSize()
            .background(background)
            .systemBarsPadding(),
        content = content,
    )
}

/** Тёмный фон — светлые иконки статус-бара, и наоборот. */
private fun Color.luminanceIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.6f

@Composable
fun SystemBarsAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightIcons
            isAppearanceLightNavigationBars = !lightIcons
        }
    }
}

/**
 * Шапка «← Заголовок · слот справа».
 * Кнопка назад — квадрат 36dp radius 12 на canvas, как в макете, а не круглая иконка M3.
 */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    dark: Boolean = false,
    titleIsFormula: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .staggeredReveal(index = 0, initialOffsetY = 12f)
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) BackButton(onClick = onBack, dark = dark)
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = if (dark) SurfaceWhite else TextPrimary,
            fontFamily = if (titleIsFormula) DisplayFont else null,
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        trailing()
    }
}

/** Заголовок экрана без кнопки назад: «История», «Язык приложения». */
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 26.sp,
) = Text(
    text,
    modifier,
    color = TextPrimary,
    fontSize = fontSize,
    lineHeight = fontSize * 1.25f,
    fontWeight = FontWeight.Bold,
)

/** Мелкая подпись секции: 11sp, uppercase, трекинг .1em. */
@Composable
fun CaptionText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextTertiary,
) = Text(text.uppercase(), modifier, color = color, style = Caption)

/** Та же подпись на тёмной карточке визуализации — 12sp, трекинг .08em. */
@Composable
fun CaptionOnDarkText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = SurfaceWhite,
) = Text(text.uppercase(), modifier, color = color, style = CaptionOnDark)

/** Карточка условия задачи: canvas-фон, radius 20, caption + текст или формула. */
@Composable
fun ConditionCard(
    modifier: Modifier = Modifier,
    caption: String = "Условие",
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.Step, easing = Motion.Emphasized))
            .clip(RoundedCornerShape(20.dp))
            .background(Canvas)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CaptionText(caption)
        content()
    }
}

/**
 * Тёмная карточка визуализации: заголовок слева, счётчик «шаг N / M» справа,
 * ниже — область рисования. Общая для графика (1a), чертежа (4a) и схемы сил (4b).
 */
@Composable
fun VisualCard(
    caption: String,
    modifier: Modifier = Modifier,
    counter: String? = null,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.Step, easing = Motion.Emphasized))
            .clip(RoundedCornerShape(24.dp))
            .background(Ink)
            .padding(start = contentPadding, end = contentPadding, top = contentPadding, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (caption.isNotEmpty() || counter != null) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CaptionOnDarkText(caption, Modifier.weight(1f))
                if (counter != null) {
                    Text(
                        counter,
                        color = TextOnDarkSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
        content()
    }
}

/** Пунктирная рамка: «график не нужен», «вывод закрыт в Pro». */
fun Modifier.dashedBorder(
    color: Color = DashedBorderColor,
    cornerRadius: Dp = 20.dp,
    strokeWidth: Dp = 2.dp,
) = drawBehind {
    val stroke = Stroke(
        width = strokeWidth.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
    )
    val inset = strokeWidth.toPx() / 2f
    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = stroke,
    )
}

/** Карточка с обводкой: списки истории, «Что требуется», лимит подписки. */
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    borderColor: Color = BorderColor,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    verticalGap: Dp = 8.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.Step, easing = Motion.Emphasized))
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalGap),
        content = content,
    )
}

/** Горизонтальный разделитель списка настроек. */
@Composable
fun ListDivider(modifier: Modifier = Modifier) = Box(
    modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Divider)
)

/** Полоса заполнения: прогресс онбординга, лимит задач, индикатор загрузки на сплеше. */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = Accent,
    trackColor: Color = Canvas,
) {
    // Доля приезжает шагами (следующий экран онбординга, ещё одна решённая задача),
    // поэтому полоса дотягивается до неё, а не перепрыгивает.
    val filled by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(Motion.Step, easing = Motion.Emphasized),
        label = "progress",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(filled)
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Полоса без процентов: бегунок ходит по треку, пока идёт неизмеримая работа
 * (восстановление сессии на сплеше). Долю заполнения показывает [ProgressTrack].
 */
@Composable
fun IndeterminateTrack(
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    color: Color = Accent,
    trackColor: Color = Canvas,
    thumbFraction: Float = 0.35f,
) {
    val shift = rememberInfiniteTransition(label = "loading").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = Motion.Emphasized),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "thumb",
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
    ) {
        Box(
            Modifier
                .fillMaxWidth(thumbFraction)
                .fillMaxHeight()
                // Позицию читаем на этапе отрисовки, а не композиции: полоса на сплеше
                // не должна пересобирать экран каждый кадр.
                .graphicsLayer {
                    translationX = shift.value * size.width * (1f - thumbFraction) / thumbFraction
                }
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/** Хэндл шторки — 44×5 на border-сером. */
@Composable
fun SheetHandle(modifier: Modifier = Modifier) = Box(
    modifier
        .width(44.dp)
        .height(5.dp)
        .clip(RoundedCornerShape(999.dp))
        .background(BorderColor)
)
