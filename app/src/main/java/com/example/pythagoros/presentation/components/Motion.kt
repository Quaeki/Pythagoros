package com.example.pythagoros.presentation.components

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.pythagoros.ui.theme.Accent
import kotlinx.coroutines.delay

/**
 * Тайминги движения — в одном месте, чтобы экраны не расходились по ощущению.
 *
 * Системную настройку «убрать анимацию» отдельно обрабатывать не нужно:
 * Compose масштабирует длительности сам через `MotionDurationScale`,
 * который платформа берёт из `Settings.Global.ANIMATOR_DURATION_SCALE`.
 */
object Motion {
    /** Появление корней и вершины на графике: opacity 0→1. */
    const val Reveal = 500

    /** Прорисовка кривой от начала до конца. */
    const val Curve = 900

    /** Переход между шагами и раскрытие пояснения. */
    const val Step = 300

    /** Приход нового экрана. */
    const val Screen = 260

    /** Затухание старого экрана — оно идёт в конце перехода, см. [screenTransition]. */
    const val ScreenExit = 160

    /** Шторка снизу: Pro-гейт, подробный разбор шага. */
    const val Sheet = 300

    /** Затемнение под шторкой. */
    const val Scrim = 220

    /** Смена цвета: активный шаг, выбранный чип, вкладка. */
    const val Tint = 220

    /** Отклик на касание — заметно быстрее всего остального. */
    const val Press = 90

    /** Полный проход блика по скелетону. */
    const val Shimmer = 1150

    /** Один цикл «дышащих» точек ожидания. */
    const val Pulse = 900

    /** Задержка между элементами каскадного появления. */
    const val Stagger = 55

    /** Основная кривая: быстрый разгон, мягкая остановка. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Уход с экрана: разгоняется и не тормозит. */
    val Accelerated: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
}

// ─────────────────────────── Каскадное появление ───────────────────────────

/**
 * Мягкий вход элемента: opacity + небольшой сдвиг снизу + микромасштаб.
 *
 * Используется для секций, карточек и нижних панелей. Значения читаются в
 * graphicsLayer, поэтому анимация не пересобирает содержимое каждый кадр.
 */
@Composable
fun Modifier.staggeredReveal(
    index: Int = 0,
    enabled: Boolean = true,
    initialOffsetY: Float = 22f,
    initialScale: Float = 0.985f,
): Modifier {
    var visible by remember { mutableStateOf(!enabled) }
    LaunchedEffect(enabled) {
        if (!enabled) {
            visible = true
            return@LaunchedEffect
        }
        delay(index.coerceAtLeast(0) * Motion.Stagger.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.Screen, easing = Motion.Emphasized),
        label = "revealAlpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else initialOffsetY,
        animationSpec = tween(Motion.Screen, easing = Motion.Emphasized),
        label = "revealOffset",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else initialScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "revealScale",
    )
    return graphicsLayer {
        this.alpha = alpha
        translationY = offsetY
        scaleX = scale
        scaleY = scale
    }
}

// ─────────────────────────── Отклик на касание ───────────────────────────

/**
 * Состояние нажатия: общий источник взаимодействий и посчитанный по нему масштаб.
 *
 * Разделено на два модификатора, потому что порядок в цепочке разный:
 * масштаб должен стоять до `clip`/`background` (иначе фон останется вне слоя),
 * а обработчик клика — после, чтобы попадать по уже обрезанной форме.
 */
@Stable
class PressFeedback internal constructor(
    internal val source: MutableInteractionSource,
    internal val scale: State<Float>,
)

@Composable
fun rememberPressFeedback(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
): PressFeedback {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "press",
    )
    return remember(source) { PressFeedback(source, scale) }
}

/** Ставится в начало цепочки: в слой должны попасть и фон, и содержимое. */
fun Modifier.pressScale(press: PressFeedback): Modifier = graphicsLayer {
    val value = press.scale.value
    scaleX = value
    scaleY = value
}

/** Клик, кормящий [pressScale] нажатиями; подсветка остаётся штатная. */
@Composable
fun Modifier.pressClickable(
    press: PressFeedback,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    interactionSource = press.source,
    indication = LocalIndication.current,
    enabled = enabled,
    onClick = onClick,
)

// ─────────────────────────── Смена экрана ───────────────────────────

/** Куда идёт переход: вглубь стека, обратно или вбок между вкладками. */
enum class NavDirection { Forward, Back, Lateral }

/**
 * Экран приходит с той стороны, куда движется навигация, и сдвигается всего на
 * шестую долю ширины: полноэкранный слайд на каждом шаге разбора утомляет.
 *
 * Новый экран всегда сверху, а старый держит непрозрачность почти до конца:
 * оба заливают фон целиком, и одновременное затухание показало бы пустое окно.
 */
fun screenTransition(direction: NavDirection): ContentTransform {
    val fadeInSpec = tween<Float>(Motion.Screen, easing = Motion.Emphasized)
    val fadeOutSpec = tween<Float>(
        durationMillis = Motion.ScreenExit,
        delayMillis = Motion.Screen - Motion.ScreenExit,
        easing = Motion.Accelerated,
    )
    val slideIn = tween<IntOffset>(Motion.Screen, easing = Motion.Emphasized)
    val slideOut = tween<IntOffset>(Motion.Screen, easing = Motion.Accelerated)

    // Знак сдвига: вперёд — новый экран наезжает справа, назад — слева.
    val side = when (direction) {
        NavDirection.Forward -> 1
        NavDirection.Back -> -1
        NavDirection.Lateral -> 0
    }
    return ContentTransform(
        targetContentEnter = slideInHorizontally(slideIn) { side * it / 6 } + fadeIn(fadeInSpec),
        initialContentExit = slideOutHorizontally(slideOut) { -side * it / 14 } + fadeOut(fadeOutSpec),
        targetContentZIndex = 1f,
    )
}

// ─────────────────────────── Ожидание ───────────────────────────

/**
 * Блик, пробегающий по скелетону.
 *
 * Прогресс распознавания на устройстве неизвестен, поэтому вместо процентов
 * показываем, что процесс идёт. Ставится после `clip`, чтобы блик обрезался формой.
 */
@Composable
fun Modifier.shimmer(
    highlight: Color = Color.White.copy(alpha = 0.7f),
    bandFraction: Float = 0.4f,
    delayMillis: Int = 0,
): Modifier {
    val progress by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.Shimmer, delayMillis = delayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerBand",
    )
    return drawWithCache {
        val band = size.width * bandFraction
        val start = -band + (size.width + band) * progress
        val brush = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.5f to highlight,
            1f to Color.Transparent,
            startX = start,
            endX = start + band,
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush)
        }
    }
}

/**
 * Три «дышащие» точки: ответ AI идёт по сети, длительность заранее неизвестна,
 * поэтому индикатор без процентов.
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    color: Color = Accent,
    dotSize: Dp = 7.dp,
) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            // Значение читаем в graphicsLayer, а не в композиции: тогда пульсация
            // перерисовывает точки, но не пересобирает экран каждый кадр.
            val alpha = transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = Motion.Pulse,
                        delayMillis = index * Motion.Pulse / 4,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                Modifier
                    .graphicsLayer { this.alpha = alpha.value }
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
