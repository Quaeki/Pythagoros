package com.example.pythagoros.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.model.FigurePrimitive
import com.example.pythagoros.domain.model.FigureRole
import com.example.pythagoros.domain.model.MarkId
import com.example.pythagoros.domain.model.NormPoint
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.Dark5
import com.example.pythagoros.ui.theme.FigureFill
import com.example.pythagoros.ui.theme.Mint
import com.example.pythagoros.ui.theme.PlaneFill
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.UiFont
import com.example.pythagoros.ui.theme.VectorBlue
import com.example.pythagoros.ui.theme.Warn
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// ─────────────────────────── График функции ───────────────────────────

/**
 * График из макета 1a/1b, который достраивается по шагам:
 * сначала оси, потом корни, потом вершина, потом кривая.
 *
 * Кривая рисуется не целиком, а отрезком пути через [PathMeasure] — так же,
 * как в моке анимировался `stroke-dashoffset`.
 */
@Composable
fun FunctionPlot(
    graph: PolynomialGraph,
    modifier: Modifier = Modifier,
    revealRoots: Boolean = true,
    revealVertex: Boolean = true,
    revealCurve: Boolean = true,
    /** Насколько прорисована кривая, когда она открыта: 1f — целиком. */
    curveFraction: Float = 1f,
    height: Dp = 200.dp,
    curveColor: Color = Accent,
    rootColor: Color = Warn,
    vertexColor: Color = Mint,
    axisColor: Color = Dark5,
    dotRadius: Dp = 7.dp,
) {
    val rootsAlpha by animateFloatAsState(
        targetValue = if (revealRoots) 1f else 0f,
        animationSpec = tween(Motion.Reveal),
        label = "roots",
    )
    val vertexAlpha by animateFloatAsState(
        targetValue = if (revealVertex) 1f else 0f,
        animationSpec = tween(Motion.Reveal),
        label = "vertex",
    )
    val curveProgress by animateFloatAsState(
        targetValue = if (revealCurve) curveFraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(Motion.Curve, easing = FastOutSlowInEasing),
        label = "curve",
    )
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val plot = PlotFrame(graph, size.width, size.height, 14.dp.toPx())

        // Оси
        drawLine(
            axisColor,
            Offset(plot.left, plot.yToPx(0.0)),
            Offset(plot.right, plot.yToPx(0.0)),
            strokeWidth = 2.dp.toPx(),
        )
        drawLine(
            axisColor,
            Offset(plot.xToPx(0.0), plot.top),
            Offset(plot.xToPx(0.0), plot.bottom),
            strokeWidth = 2.dp.toPx(),
        )

        // Кривая
        if (curveProgress > 0f) {
            val path = plot.curvePath(graph)
            drawProgressivePath(path, curveProgress, curveColor, 5.dp.toPx())
        }

        // Корни
        if (rootsAlpha > 0f) {
            graph.roots.forEach { root ->
                val center = Offset(plot.xToPx(root), plot.yToPx(0.0))
                drawCircle(rootColor, dotRadius.toPx(), center, alpha = rootsAlpha)
                drawPlotLabel(
                    measurer = textMeasurer,
                    text = formatPlotNumber(root),
                    at = Offset(center.x, center.y + 10.dp.toPx()),
                    color = rootColor,
                    alpha = rootsAlpha,
                    centerHorizontally = true,
                )
            }
        }

        // Вершина
        val vertex = graph.vertex
        if (vertex != null && vertexAlpha > 0f) {
            val center = Offset(plot.xToPx(vertex.x), plot.yToPx(vertex.y))
            drawCircle(vertexColor, dotRadius.toPx(), center, alpha = vertexAlpha)
            val label = vertex.label
                ?: "(${formatPlotNumber(vertex.x)}; ${formatPlotNumber(vertex.y)})"
            drawPlotLabel(
                measurer = textMeasurer,
                text = label,
                at = Offset(center.x + 11.dp.toPx(), center.y - 8.dp.toPx()),
                color = vertexColor,
                alpha = vertexAlpha,
                fontSize = 13f,
            )
        }
    }
}

/** Перевод математических координат в пиксели области рисования. */
private class PlotFrame(
    graph: PolynomialGraph,
    width: Float,
    height: Float,
    padding: Float,
) {
    val left = padding
    val right = width - padding
    val top = padding
    val bottom = height - padding

    private val xMin: Double
    private val xMax: Double
    private val yMin: Double
    private val yMax: Double

    init {
        val anchors = buildList {
            addAll(graph.roots)
            graph.vertex?.let { add(it.x) }
        }
        val centre = if (anchors.isEmpty()) 0.0 else (anchors.min() + anchors.max()) / 2
        val spread = if (anchors.size > 1) anchors.max() - anchors.min() else 0.0
        val half = max(spread * 0.9, 2.5)
        xMin = centre - half
        xMax = centre + half

        var lo = 0.0
        var hi = 0.0
        val samples = 96
        repeat(samples + 1) { i ->
            val x = xMin + (xMax - xMin) * i / samples
            val y = graph.valueAt(x)
            if (y.isFinite()) {
                lo = minOf(lo, y)
                hi = maxOf(hi, y)
            }
        }
        graph.vertex?.let {
            lo = minOf(lo, it.y)
            hi = maxOf(hi, it.y)
        }
        if (hi - lo < 1e-6) {
            lo -= 1.0
            hi += 1.0
        }
        val pad = (hi - lo) * 0.14
        yMin = lo - pad
        yMax = hi + pad
    }

    fun xToPx(x: Double): Float =
        left + ((x - xMin) / (xMax - xMin)).toFloat() * (right - left)

    fun yToPx(y: Double): Float =
        bottom - ((y - yMin) / (yMax - yMin)).toFloat() * (bottom - top)

    /** Кривая строится сэмплированием: полином любой степени рисуется одинаково. */
    fun curvePath(graph: PolynomialGraph): Path {
        val path = Path()
        val samples = 160
        var started = false
        repeat(samples + 1) { i ->
            val x = xMin + (xMax - xMin) * i / samples
            val y = graph.valueAt(x)
            if (!y.isFinite()) return@repeat
            val px = xToPx(x)
            val py = yToPx(y)
            // За пределами карточки кривую не тянем, чтобы не было длинных хвостов.
            if (py < top - 40f || py > bottom + 40f) {
                started = false
                return@repeat
            }
            if (started) {
                path.lineTo(px, py)
            } else {
                path.moveTo(px, py)
                started = true
            }
        }
        return path
    }
}

/** Отрисовка первых [progress] процентов пути — аналог `stroke-dashoffset` из мока. */
private fun DrawScope.drawProgressivePath(
    path: Path,
    progress: Float,
    color: Color,
    strokeWidth: Float,
) {
    val measure = PathMeasure()
    measure.setPath(path, false)
    val length = measure.length
    if (length <= 0f) return
    val visible = Path()
    measure.getSegment(0f, length * progress.coerceIn(0f, 1f), visible, true)
    drawPath(visible, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

/**
 * Подпись на графике.
 *
 * Цвет задаём параметром `drawText`, а не только стилем: стиль в измеренной раскладке
 * до отрисовки не долетал и текст выходил чёрным.
 */
private fun DrawScope.drawPlotLabel(
    measurer: TextMeasurer,
    text: String,
    at: Offset,
    color: Color,
    alpha: Float,
    fontSize: Float = 14f,
    centerHorizontally: Boolean = false,
) {
    val layout = measurer.measure(
        text = text,
        style = TextStyle(
            fontFamily = UiFont,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
        ),
    )
    val topLeft = if (centerHorizontally) {
        Offset(at.x - layout.size.width / 2f, at.y)
    } else {
        at
    }
    drawText(layout, color = color, topLeft = topLeft, alpha = alpha)
}

/** «1», «−1», «4,8» — как в макете, с запятой в дробной части. */
fun formatPlotNumber(value: Double): String {
    val rounded = (value * 100).roundToInt() / 100.0
    if (abs(rounded - rounded.roundToInt()) < 1e-6) return rounded.roundToInt().toString().replace("-", "−")
    return rounded.toString().replace(".", ",").replace("-", "−")
}

// ─────────────────────────── Чертежи и схемы ───────────────────────────

/**
 * Чертёж геометрии (4a) и схема сил (4b): примитивы в нормализованных координатах.
 * Показываются только те, чей [MarkId] уже открыт шагами решения.
 */
@Composable
fun FigureCanvas(
    primitives: List<FigurePrimitive>,
    modifier: Modifier = Modifier,
    visible: Set<MarkId>? = null,
    aspectRatio: Float = 300f / 210f,
    blurRadius: Dp = 0.dp,
) {
    val textMeasurer = rememberTextMeasurer()
    val shown = primitives.filter { visible == null || it.id in visible }

    // Уже показанное и то, что открыл текущий шаг, рисуем разными слоями: так
    // новая часть чертежа проявляется, а старая не мигает вместе с ней.
    var settled by remember(primitives) { mutableStateOf(visible.orEmpty()) }
    var incoming by remember(primitives) { mutableStateOf(emptySet<MarkId>()) }
    val reveal = remember(primitives) { Animatable(1f) }

    LaunchedEffect(primitives, visible) {
        val now = visible.orEmpty()
        val fresh = now - settled
        settled = now
        if (fresh.isEmpty()) return@LaunchedEffect
        incoming = fresh
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(Motion.Reveal, easing = FastOutSlowInEasing))
    }

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
    ) {
        FigureLayer(
            primitives = shown.filterNot { it.id in incoming },
            aspectRatio = aspectRatio,
            textMeasurer = textMeasurer,
        )
        FigureLayer(
            primitives = shown.filter { it.id in incoming },
            aspectRatio = aspectRatio,
            textMeasurer = textMeasurer,
            modifier = Modifier.graphicsLayer { alpha = reveal.value },
        )
    }
}

/**
 * Один слой чертежа. Слои лежат друг на друге с одинаковыми пропорциями,
 * поэтому нормализованные координаты в них совпадают.
 */
@Composable
private fun FigureLayer(
    primitives: List<FigurePrimitive>,
    aspectRatio: Float,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(primitives) {
        // Подписи всегда поверх линий, иначе их перекрывают заливки фигур.
        primitives.filterNot { it is FigurePrimitive.Label } +
            primitives.filterIsInstance<FigurePrimitive.Label>()
    }

    Canvas(modifier.fillMaxWidth().aspectRatio(aspectRatio)) {
        ordered.forEach { primitive ->
            when (primitive) {
                is FigurePrimitive.Polygon -> drawFigurePolygon(primitive)
                is FigurePrimitive.Segment -> drawFigureSegment(primitive)
                is FigurePrimitive.Vector -> drawFigureVector(primitive)
                is FigurePrimitive.RightAngle -> drawRightAngle(primitive)
                is FigurePrimitive.Dot -> drawCircle(
                    primitive.role.strokeColor(),
                    6.dp.toPx(),
                    px(primitive.at),
                )
                is FigurePrimitive.Body -> drawBody(primitive)
                is FigurePrimitive.Label -> drawFigureLabel(primitive, textMeasurer)
            }
        }
    }
}

/** Нормализованная точка → пиксели текущей области рисования. */
private fun DrawScope.px(point: NormPoint) = Offset(point.x * size.width, point.y * size.height)

private fun DrawScope.drawFigurePolygon(primitive: FigurePrimitive.Polygon) {
    if (primitive.points.size < 2) return
    val path = Path()
    primitive.points.forEachIndexed { index, point ->
        val offset = px(point)
        if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
    }
    if (primitive.closed) path.close()
    if (primitive.filled) drawPath(path, primitive.role.fillColor())
    drawPath(
        path,
        primitive.role.strokeColor(),
        style = Stroke(width = primitive.role.strokeWidth().toPx()),
    )
}

private fun DrawScope.drawFigureSegment(primitive: FigurePrimitive.Segment) {
    val effect = if (primitive.dashed) {
        androidx.compose.ui.graphics.PathEffect.dashPathEffect(
            floatArrayOf(7.dp.toPx(), 6.dp.toPx())
        )
    } else {
        null
    }
    drawLine(
        color = primitive.role.strokeColor(),
        start = px(primitive.from),
        end = px(primitive.to),
        strokeWidth = 4.dp.toPx(),
        pathEffect = effect,
    )
}

private fun DrawScope.drawFigureVector(primitive: FigurePrimitive.Vector) {
    val color = primitive.role.strokeColor()
    val start = px(primitive.from)
    val end = px(primitive.to)
    drawLine(color, start, end, strokeWidth = 4.dp.toPx())

    // Треугольная стрелка на конце вектора.
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = kotlin.math.hypot(dx, dy)
    if (length < 1f) return
    val ux = dx / length
    val uy = dy / length
    val head = 11.dp.toPx()
    val halfWidth = 5.5.dp.toPx()
    val baseX = end.x - ux * head
    val baseY = end.y - uy * head
    val arrow = Path().apply {
        moveTo(end.x, end.y)
        lineTo(baseX - uy * halfWidth, baseY + ux * halfWidth)
        lineTo(baseX + uy * halfWidth, baseY - ux * halfWidth)
        close()
    }
    drawPath(arrow, color)
}

private fun DrawScope.drawRightAngle(primitive: FigurePrimitive.RightAngle) {
    val corner = px(primitive.corner)
    val along = px(primitive.along)
    val across = px(primitive.across)
    val side = 24.dp.toPx()
    val u = (along - corner).normalizedOrZero() * side
    val v = (across - corner).normalizedOrZero() * side
    val path = Path().apply {
        moveTo(corner.x + u.x, corner.y + u.y)
        lineTo(corner.x + u.x + v.x, corner.y + u.y + v.y)
        lineTo(corner.x + v.x, corner.y + v.y)
    }
    drawPath(path, primitive.role.strokeColor(), style = Stroke(width = 3.dp.toPx()))
}

private fun DrawScope.drawBody(primitive: FigurePrimitive.Body) {
    val center = px(primitive.center)
    val w = primitive.width * size.width
    val h = primitive.height * size.height
    rotate(primitive.rotationDegrees, center) {
        drawRoundRect(
            color = primitive.role.strokeColor(),
            topLeft = Offset(center.x - w / 2, center.y - h / 2),
            size = androidx.compose.ui.geometry.Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
        )
    }
}

private fun DrawScope.drawFigureLabel(primitive: FigurePrimitive.Label, measurer: TextMeasurer) {
    val layout = measurer.measure(
        text = primitive.text,
        style = TextStyle(
            fontFamily = UiFont,
            fontWeight = if (primitive.emphasized) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = if (primitive.emphasized) 14.sp else 12.sp,
        ),
    )
    val anchor = px(primitive.at)
    val dx = if (primitive.emphasized) 6.dp.toPx() else 5.dp.toPx()
    val dy = if (primitive.emphasized) {
        -layout.size.height.toFloat() - 3.dp.toPx()
    } else {
        -layout.size.height / 2f
    }
    val topLeft = Offset(
        x = (anchor.x + dx).coerceIn(2.dp.toPx(), size.width - layout.size.width - 2.dp.toPx()),
        y = (anchor.y + dy).coerceIn(2.dp.toPx(), size.height - layout.size.height - 2.dp.toPx()),
    )
    drawText(layout, color = primitive.role.labelColor(), topLeft = topLeft)
}

private operator fun Offset.minus(other: Offset) = Offset(x - other.x, y - other.y)

private operator fun Offset.times(scale: Float) = Offset(x * scale, y * scale)

private fun Offset.normalizedOrZero(): Offset {
    val length = kotlin.math.hypot(x, y)
    return if (length < 1e-3f) Offset.Zero else Offset(x / length, y / length)
}

// ─────────────────────────── Цвета по ролям ───────────────────────────

private fun FigureRole.strokeColor(): Color = when (this) {
    FigureRole.Primary -> Accent
    FigureRole.Auxiliary -> Warn
    FigureRole.Check -> Mint
    FigureRole.Support -> Dark5
    FigureRole.VectorWeight -> Warn
    FigureRole.VectorFriction -> Mint
    FigureRole.VectorNormal -> VectorBlue
}

private fun FigureRole.fillColor(): Color = when (this) {
    FigureRole.Support -> PlaneFill
    else -> FigureFill
}

private fun FigureRole.strokeWidth(): Dp = when (this) {
    FigureRole.Support -> 3.dp
    else -> 4.dp
}

/** Подписи вершин белые, длин — серые, векторов — цветом вектора. */
private fun FigureRole.labelColor(): Color = when (this) {
    FigureRole.Primary -> SurfaceWhite
    FigureRole.Support -> TextOnDarkSecondary
    else -> strokeColor()
}
