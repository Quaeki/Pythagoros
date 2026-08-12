package com.example.pythagoros.data.ai

import com.example.pythagoros.domain.ai.PremiumAiGraph
import com.example.pythagoros.domain.ai.PremiumAiPoint
import com.example.pythagoros.domain.ai.PremiumAiPrimitive
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiViewport
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.FigurePrimitive
import com.example.pythagoros.domain.model.FigureRole
import com.example.pythagoros.domain.model.GraphPoint
import com.example.pythagoros.domain.model.MarkId
import com.example.pythagoros.domain.model.NormPoint
import com.example.pythagoros.domain.model.PlotMarks
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.Visualization
import com.example.pythagoros.domain.solver.PolynomialEquationParser
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MappedAiSolution(
    val solution: Solution,
    val visualization: Visualization,
)

fun PremiumAiSolveResult.Success.toMappedSolution(
    originalExpression: Expression,
    problemType: ProblemType,
): MappedAiSolution {
    val visualization = graph.toVisualization(originalExpression, problemType)
    val cleanedSteps = steps.mapIndexed { index, step ->
        SolutionStep(
            title = step.title.ifBlank { "Шаг ${index + 1}" },
            formula = step.formula.normalizeAiMath(),
            explanation = step.explanation.normalizeAiMath(),
            reveal = when {
                visualization is Visualization.Plot2D && index == 0 -> setOf(PlotMarks.Roots)
                visualization is Visualization.Plot2D && index == 1 -> setOf(PlotMarks.Vertex)
                visualization is Visualization.Plot2D && index >= 2 -> setOf(PlotMarks.Curve)
                else -> emptySet()
            },
        )
    }

    return MappedAiSolution(
        solution = Solution(
            expression = originalExpression,
            problemType = problemType,
            answer = answer.normalizeAiMath(),
            steps = cleanedSteps.ifEmpty {
                listOf(
                    SolutionStep(
                        title = "Ответ AI",
                        formula = answer.normalizeAiMath(),
                        explanation = "Backend вернул итог без дополнительных шагов.",
                    )
                )
            },
            graph = (visualization as? Visualization.Plot2D)?.graph,
        ),
        visualization = visualization,
    )
}

private fun PremiumAiGraph?.toVisualization(
    originalExpression: Expression,
    problemType: ProblemType,
): Visualization {
    if (this == null) return Visualization.NotNeeded()

    toStructuredFigureOrNull()?.let { return it }
    if (problemType == ProblemType.Geometry) {
        toRightTriangleFigureOrNull(originalExpression.source)?.let { return it }
    }
    toFigureOrNull()?.let { return it }
    toPolynomialPlotOrNull()?.let { return it }

    return Visualization.NotNeeded(
        hint = "AI вернул описание визуализации: ${title.normalizeAiMath()}",
        actionText = "формат пока не поддержан",
    )
}

private fun PremiumAiGraph.toPolynomialPlotOrNull(): Visualization.Plot2D? {
    val source = expression
        .substringAfter("=")
        .normalizeGraphExpression()
        .takeIf { it.isNotBlank() }
        ?: return null

    return runCatching {
        val parsed = PolynomialEquationParser.parsePolynomial(source, variable)
        val coefficients = parsed.polynomial.coefficients
        val degree = parsed.polynomial.degree()
        val roots = when (degree) {
            1 -> {
                val a = parsed.polynomial.coefficient(1)
                val b = parsed.polynomial.coefficient(0)
                if (abs(a) > 1e-9) listOf(-b / a) else emptyList()
            }

            2 -> {
                val a = parsed.polynomial.coefficient(2)
                val b = parsed.polynomial.coefficient(1)
                val c = parsed.polynomial.coefficient(0)
                val discriminant = b * b - 4 * a * c
                if (abs(a) > 1e-9 && discriminant >= 0.0) {
                    val root = sqrt(discriminant)
                    listOf((-b - root) / (2 * a), (-b + root) / (2 * a))
                } else {
                    emptyList()
                }
            }

            else -> emptyList()
        }
        val vertex = if (degree == 2) {
            val a = parsed.polynomial.coefficient(2)
            val b = parsed.polynomial.coefficient(1)
            if (abs(a) > 1e-9) {
                val x = -b / (2 * a)
                GraphPoint(x, parsed.polynomial.valueAt(x))
            } else {
                null
            }
        } else {
            null
        }

        Visualization.Plot2D(
            graph = PolynomialGraph(
                title = title.normalizeAiMath().ifBlank { "y = $source" },
                variable = parsed.variable,
                coefficients = coefficients,
                roots = roots,
                vertex = vertex,
            ),
            caption = "График от AI",
        )
    }.getOrNull()
}

private fun PremiumAiGraph.toStructuredFigureOrNull(): Visualization.Figure? {
    if (primitives.isEmpty()) return null

    val bounds = primitives.coordinateBounds(viewport) ?: return null
    fun PremiumAiPoint.norm(): NormPoint = bounds.normalize(this)

    var generatedId = 0
    fun nextId(prefix: String): MarkId = MarkId("ai.structured.$prefix.${generatedId++}")

    val figurePrimitives = buildList {
        primitives.forEach { primitive ->
            val role = primitive.role.toFigureRole(primitive.type)
            when (primitive.type.lowercase()) {
                "axis", "line", "segment" -> {
                    val from = primitive.from ?: primitive.points.firstOrNull()
                    val to = primitive.to ?: primitive.points.getOrNull(1)
                    if (from != null && to != null) {
                        add(
                            FigurePrimitive.Segment(
                                id = primitive.markIdOr(nextId("segment")),
                                from = from.norm(),
                                to = to.norm(),
                                role = role,
                                dashed = primitive.dashed,
                            )
                        )
                        primitive.label?.takeIf { it.isNotBlank() }?.let { label ->
                            add(
                                FigurePrimitive.Label(
                                    id = nextId("segment.label"),
                                    at = PremiumAiPoint((from.x + to.x) / 2.0, (from.y + to.y) / 2.0).norm(),
                                    text = label.normalizeVisualLabel(),
                                    role = role,
                                )
                            )
                        }
                    }
                }

                "vector" -> {
                    val from = primitive.from ?: primitive.points.firstOrNull()
                    val to = primitive.to ?: primitive.points.getOrNull(1)
                    if (from != null && to != null) {
                        add(
                            FigurePrimitive.Vector(
                                id = primitive.markIdOr(nextId("vector")),
                                from = from.norm(),
                                to = to.norm(),
                                role = role,
                            )
                        )
                        val label = primitive.label ?: primitive.text
                        label?.takeIf { it.isNotBlank() }?.let {
                            add(
                                FigurePrimitive.Label(
                                    id = nextId("vector.label"),
                                    at = PremiumAiPoint(
                                        x = from.x + (to.x - from.x) * 0.72,
                                        y = from.y + (to.y - from.y) * 0.72,
                                    ).norm(),
                                    text = it.normalizeVisualLabel(),
                                    role = role,
                                    emphasized = true,
                                )
                            )
                        }
                    }
                }

                "curve", "polyline" -> {
                    val points = primitive.points
                    if (points.size >= 2) {
                        add(
                            FigurePrimitive.Polygon(
                                id = primitive.markIdOr(nextId("curve")),
                                points = points.map { it.norm() },
                                role = role,
                                filled = false,
                                closed = false,
                            )
                        )
                    }
                }

                "polygon", "triangle" -> {
                    if (primitive.points.size >= 2) {
                        add(
                            FigurePrimitive.Polygon(
                                id = primitive.markIdOr(nextId("polygon")),
                                points = primitive.points.map { it.norm() },
                                role = role,
                                filled = primitive.filled,
                                closed = primitive.closed ?: true,
                            )
                        )
                    }
                }

                "circle" -> {
                    val center = primitive.center ?: primitive.at
                    val radius = primitive.radius
                    if (center != null && radius != null && radius > 0.0) {
                        add(
                            FigurePrimitive.Polygon(
                                id = primitive.markIdOr(nextId("circle")),
                                points = sampleCircle(center, radius, 96).map { it.norm() },
                                role = role,
                                filled = false,
                                closed = true,
                            )
                        )
                        primitive.label?.takeIf { it.isNotBlank() }?.let { label ->
                            add(
                                FigurePrimitive.Label(
                                    id = nextId("circle.label"),
                                    at = PremiumAiPoint(center.x + radius * 0.35, center.y + radius * 0.35).norm(),
                                    text = label.normalizeVisualLabel(),
                                    role = role,
                                )
                            )
                        }
                    }
                }

                "arc" -> {
                    val center = primitive.center ?: primitive.at
                    val radius = primitive.radius
                    if (center != null && radius != null && radius > 0.0) {
                        add(
                            FigurePrimitive.Polygon(
                                id = primitive.markIdOr(nextId("arc")),
                                points = sampleArc(
                                    center = center,
                                    radius = radius,
                                    startAngle = primitive.startAngle ?: 0.0,
                                    endAngle = primitive.endAngle ?: 180.0,
                                    steps = 48,
                                ).map { it.norm() },
                                role = role,
                                filled = false,
                                closed = false,
                            )
                        )
                    }
                }

                "right_angle" -> {
                    val corner = primitive.at ?: primitive.center ?: primitive.points.firstOrNull()
                    val along = primitive.from ?: primitive.points.getOrNull(1)
                    val across = primitive.to ?: primitive.points.getOrNull(2)
                    if (corner != null && along != null && across != null) {
                        add(
                            FigurePrimitive.RightAngle(
                                id = primitive.markIdOr(nextId("right_angle")),
                                corner = corner.norm(),
                                along = along.norm(),
                                across = across.norm(),
                                role = role,
                            )
                        )
                    }
                }

                "point", "dot" -> {
                    val at = primitive.at ?: primitive.center ?: primitive.points.firstOrNull()
                    if (at != null) {
                        add(FigurePrimitive.Dot(primitive.markIdOr(nextId("point")), at.norm(), role))
                        val label = primitive.label ?: primitive.text
                        label?.takeIf { it.isNotBlank() }?.let {
                            add(
                                FigurePrimitive.Label(
                                    id = nextId("point.label"),
                                    at = at.norm(),
                                    text = it.normalizeVisualLabel(),
                                    role = role,
                                    emphasized = true,
                                )
                            )
                        }
                    }
                }

                "label", "text" -> {
                    val at = primitive.at ?: primitive.points.firstOrNull()
                    val text = primitive.text ?: primitive.label
                    if (at != null && !text.isNullOrBlank()) {
                        add(
                            FigurePrimitive.Label(
                                id = primitive.markIdOr(nextId("label")),
                                at = at.norm(),
                                text = text.normalizeVisualLabel(),
                                role = role,
                            )
                        )
                    }
                }
            }
        }
    }

    if (figurePrimitives.isEmpty()) return null
    return Visualization.Figure(
        primitives = figurePrimitives,
        caption = title.normalizeAiMath().ifBlank { "Визуализация" },
        aspectRatio = bounds.aspectRatio,
    )
}

private fun PremiumAiGraph.toFigureOrNull(): Visualization.Figure? {
    val text = buildString {
        append(title)
        append(' ')
        append(expression)
        append(' ')
        append(notes.joinToString(" "))
    }
    val points = CoordinateRegex.findAll(text)
        .map { match ->
            AiPoint(
                label = match.groupValues[1],
                x = match.groupValues[2].replace(',', '.').toFloat(),
                y = match.groupValues[3].replace(',', '.').toFloat(),
            )
        }
        .distinctBy { it.label }
        .toList()

    if (points.size < 3) return null

    val xMin = points.minOf { it.x }
    val xMax = points.maxOf { it.x }
    val yMin = points.minOf { it.y }
    val yMax = points.maxOf { it.y }
    val xRange = (xMax - xMin).takeIf { abs(it) > 1e-6f } ?: 1f
    val yRange = (yMax - yMin).takeIf { abs(it) > 1e-6f } ?: 1f

    fun AiPoint.norm(): NormPoint =
        NormPoint(
            x = 0.12f + (x - xMin) / xRange * 0.76f,
            y = 0.88f - (y - yMin) / yRange * 0.76f,
        )

    val primitives = buildList {
        add(
            FigurePrimitive.Polygon(
                id = MarkId("ai.figure.polygon"),
                points = points.map { it.norm() },
                role = FigureRole.Primary,
                filled = false,
            )
        )
        points.forEach { point ->
            add(FigurePrimitive.Dot(MarkId("ai.figure.${point.label}.dot"), point.norm()))
            add(
                FigurePrimitive.Label(
                    id = MarkId("ai.figure.${point.label}.label"),
                    at = point.norm(),
                    text = point.label,
                    emphasized = true,
                )
            )
        }
    }

    return Visualization.Figure(
        primitives = primitives,
        caption = title.normalizeAiMath().ifBlank { "Чертёж от AI" },
    )
}

private fun PremiumAiGraph.toRightTriangleFigureOrNull(condition: String): Visualization.Figure? {
    val text = "$condition $title ${notes.joinToString(" ")}"
    if (!text.contains("треуголь", ignoreCase = true)) return null
    if (!text.contains("прямоуголь", ignoreCase = true) && !text.contains("угол C = 90")) return null

    val ac = SideLengthRegex.findAll(text)
        .firstOrNull { it.groupValues[1].equals("AC", ignoreCase = true) }
        ?.groupValues
        ?.get(2)
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?: return null
    val bc = SideLengthRegex.findAll(text)
        .firstOrNull { it.groupValues[1].equals("BC", ignoreCase = true) }
        ?.groupValues
        ?.get(2)
        ?.replace(',', '.')
        ?.toFloatOrNull()
        ?: return null

    if (ac <= 0f || bc <= 0f) return null

    val a = GeoPoint(0f, ac)
    val b = GeoPoint(bc, 0f)
    val c = GeoPoint(0f, 0f)
    val center = GeoPoint(bc / 2f, ac / 2f)
    val radius = sqrt(center.x * center.x + center.y * center.y)
    val xMin = center.x - radius
    val xMax = center.x + radius
    val yMin = center.y - radius
    val yMax = center.y + radius
    val range = maxOf(xMax - xMin, yMax - yMin).takeIf { it > 1e-6f } ?: return null
    val xPad = (range - (xMax - xMin)) / 2f
    val yPad = (range - (yMax - yMin)) / 2f

    fun GeoPoint.norm(): NormPoint =
        NormPoint(
            x = 0.10f + (x - xMin + xPad) / range * 0.80f,
            y = 0.90f - (y - yMin + yPad) / range * 0.80f,
        )

    val circle = (0..72).map { index ->
        val angle = 2.0 * PI * index / 72.0
        GeoPoint(
            x = center.x + radius * cos(angle).toFloat(),
            y = center.y + radius * sin(angle).toFloat(),
        ).norm()
    }

    val primitives = buildList {
        add(
            FigurePrimitive.Polygon(
                id = MarkId("ai.figure.circumcircle"),
                points = circle,
                role = FigureRole.Auxiliary,
                filled = false,
                closed = false,
            )
        )
        add(
            FigurePrimitive.Polygon(
                id = MarkId("ai.figure.triangle"),
                points = listOf(a.norm(), c.norm(), b.norm()),
                role = FigureRole.Primary,
                filled = false,
            )
        )
        add(
            FigurePrimitive.RightAngle(
                id = MarkId("ai.figure.right_angle"),
                corner = c.norm(),
                along = b.norm(),
                across = a.norm(),
            )
        )
        listOf("A" to a, "B" to b, "C" to c).forEach { (label, point) ->
            add(FigurePrimitive.Dot(MarkId("ai.figure.$label.dot"), point.norm()))
            add(
                FigurePrimitive.Label(
                    id = MarkId("ai.figure.$label.label"),
                    at = point.norm(),
                    text = label,
                    emphasized = true,
                )
            )
        }
        add(
            FigurePrimitive.Label(
                id = MarkId("ai.figure.ac.label"),
                at = GeoPoint(-radius * 0.08f, ac / 2f).norm(),
                text = trimLength(ac),
                role = FigureRole.Support,
            )
        )
        add(
            FigurePrimitive.Label(
                id = MarkId("ai.figure.bc.label"),
                at = GeoPoint(bc / 2f, -radius * 0.08f).norm(),
                text = trimLength(bc),
                role = FigureRole.Support,
            )
        )
        add(
            FigurePrimitive.Label(
                id = MarkId("ai.figure.radius.label"),
                at = GeoPoint(center.x + radius * 0.18f, center.y + radius * 0.18f).norm(),
                text = "R = ${trimLength(radius)}",
                role = FigureRole.Auxiliary,
            )
        )
    }

    return Visualization.Figure(
        primitives = primitives,
        caption = "Чертёж прямоугольного треугольника",
        aspectRatio = 1f,
    )
}

private data class AiPoint(
    val label: String,
    val x: Float,
    val y: Float,
)

private data class GeoPoint(
    val x: Float,
    val y: Float,
)

private data class VisualBounds(
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double,
) {
    val aspectRatio: Float
        get() = ((xMax - xMin) / (yMax - yMin)).toFloat().coerceIn(0.75f, 1.6f)

    fun normalize(point: PremiumAiPoint): NormPoint =
        NormPoint(
            x = (0.10 + (point.x - xMin) / (xMax - xMin) * 0.80).toFloat(),
            y = (0.90 - (point.y - yMin) / (yMax - yMin) * 0.80).toFloat(),
        )
}

private val CoordinateRegex = Regex("""([A-ZА-Я])\((-?\d+(?:[.,]\d+)?)\s*[,;]\s*(-?\d+(?:[.,]\d+)?)\)""")
private val SideLengthRegex = Regex("""\b(AC|BC)\s*=\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)

private fun trimLength(value: Float): String =
    if (abs(value - value.toInt()) < 1e-4f) value.toInt().toString() else value.toString()

private fun String.normalizeVisualLabel(): String {
    val cleaned = normalizeAiMath()
    val pointName = Regex("""^([A-ZА-Я])\s*\(.+\)$""").matchEntire(cleaned)
    return pointName?.groupValues?.get(1) ?: cleaned
}

private fun List<PremiumAiPrimitive>.coordinateBounds(viewport: PremiumAiViewport?): VisualBounds? {
    val xs = mutableListOf<Double>()
    val ys = mutableListOf<Double>()

    fun add(point: PremiumAiPoint?) {
        if (point == null) return
        if (!point.x.isFinite() || !point.y.isFinite()) return
        xs += point.x
        ys += point.y
    }

    viewport?.xMin?.let(xs::add)
    viewport?.xMax?.let(xs::add)
    viewport?.yMin?.let(ys::add)
    viewport?.yMax?.let(ys::add)

    forEach { primitive ->
        primitive.points.forEach(::add)
        add(primitive.from)
        add(primitive.to)
        add(primitive.at)
        add(primitive.center)
        val center = primitive.center ?: primitive.at
        val radius = primitive.radius
        if (center != null && radius != null && radius > 0.0) {
            add(PremiumAiPoint(center.x - radius, center.y - radius))
            add(PremiumAiPoint(center.x + radius, center.y + radius))
        }
    }

    if (xs.isEmpty() || ys.isEmpty()) return null

    var xMin = xs.min()
    var xMax = xs.max()
    var yMin = ys.min()
    var yMax = ys.max()
    if (abs(xMax - xMin) < 1e-6) {
        xMin -= 1.0
        xMax += 1.0
    }
    if (abs(yMax - yMin) < 1e-6) {
        yMin -= 1.0
        yMax += 1.0
    }

    val xPad = (xMax - xMin) * 0.08
    val yPad = (yMax - yMin) * 0.08
    return VisualBounds(
        xMin = xMin - xPad,
        xMax = xMax + xPad,
        yMin = yMin - yPad,
        yMax = yMax + yPad,
    )
}

private fun sampleCircle(center: PremiumAiPoint, radius: Double, steps: Int): List<PremiumAiPoint> =
    (0..steps).map { index ->
        val angle = 2.0 * PI * index / steps
        PremiumAiPoint(
            x = center.x + radius * cos(angle),
            y = center.y + radius * sin(angle),
        )
    }

private fun sampleArc(
    center: PremiumAiPoint,
    radius: Double,
    startAngle: Double,
    endAngle: Double,
    steps: Int,
): List<PremiumAiPoint> =
    (0..steps).map { index ->
        val t = index.toDouble() / steps
        val angle = (startAngle + (endAngle - startAngle) * t) * PI / 180.0
        PremiumAiPoint(
            x = center.x + radius * cos(angle),
            y = center.y + radius * sin(angle),
        )
    }

private fun PremiumAiPrimitive.markIdOr(fallback: MarkId): MarkId =
    id?.takeIf { it.isNotBlank() }?.let { MarkId("ai.structured.$it") } ?: fallback

private fun String?.toFigureRole(type: String): FigureRole =
    when (this?.lowercase()) {
        "primary", "main", "curve" -> FigureRole.Primary
        "aux", "auxiliary", "helper", "circle", "arc" -> FigureRole.Auxiliary
        "check", "angle", "right_angle" -> FigureRole.Check
        "support", "axis", "grid" -> FigureRole.Support
        "weight" -> FigureRole.VectorWeight
        "friction" -> FigureRole.VectorFriction
        "normal" -> FigureRole.VectorNormal
        else -> when (type.lowercase()) {
            "axis" -> FigureRole.Support
            "circle", "arc" -> FigureRole.Auxiliary
            "point", "dot", "label", "text" -> FigureRole.Support
            else -> FigureRole.Primary
        }
    }

private fun String.normalizeGraphExpression(): String =
    normalizeAiMath()
        .replace('−', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace('×', '*')
        .replace('·', '*')
        .replace(Regex("""\s+"""), "")

private fun String.normalizeAiMath(): String {
    var value = this
        .replace("\\cdot", "·")
        .replace("\\times", "×")
        .replace("\\pi", "π")
        .replace("\\mu", "μ")
        .replace("\\theta", "θ")
        .replace("\\alpha", "α")
        .replace("\\sin", "sin")
        .replace("\\cos", "cos")
        .replace("\\tan", "tan")
        .replace("\\approx", "≈")
        .replace("\\leq", "≤")
        .replace("\\le", "≤")
        .replace("\\geq", "≥")
        .replace("\\ge", "≥")
        .replace("\\left", "")
        .replace("\\right", "")
        .replace("\\,", " ")
        .replace("\\quad", "  ")
        .replace(Regex("""\^\s*\\circ"""), "°")
        .replace(Regex("""\\text\{([^{}]+)\}"""), "$1")
        .replace(Regex("""\\mathrm\{([^{}]+)\}"""), "$1")

    repeat(4) {
        value = value
            .replace(Regex("""\\sqrt\{([^{}]+)\}"""), "√($1)")
            .replace(Regex("""\\frac\{([^{}]+)\}\{([^{}]+)\}"""), "$1/$2")
    }

    return value
        .replace("{", "")
        .replace("}", "")
        .replace(Regex("""_\{([^{}]+)\}"""), "$1")
        .replace(Regex("""_([A-Za-zА-Яа-я]+)"""), "$1")
        .replace(Regex("""_([0-9]+)"""), "$1")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

private fun com.example.pythagoros.domain.solver.Polynomial.valueAt(x: Double): Double =
    coefficients.entries.sumOf { (power, coefficient) ->
        var result = 1.0
        repeat(power) { result *= x }
        coefficient * result
    }
