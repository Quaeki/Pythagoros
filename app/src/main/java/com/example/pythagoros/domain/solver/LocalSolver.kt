package com.example.pythagoros.domain.solver

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.GraphPoint
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.SolveResult
import kotlin.math.abs
import kotlin.math.sqrt

class LocalSolver {
    fun solve(expression: Expression, problemType: ProblemType): SolveResult =
        when {
            problemType.requiresPremium -> SolveResult.Unsupported(SolveResult.Reason.RequiresPremium)
            problemType == ProblemType.LinearEquation -> solvePolynomialEquation(expression, problemType)
            problemType == ProblemType.QuadraticEquation -> solvePolynomialEquation(expression, problemType)
            problemType == ProblemType.Integral -> solvePolynomialIntegral(expression)
            else -> SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm)
        }

    private fun solvePolynomialEquation(
        expression: Expression,
        problemType: ProblemType,
    ): SolveResult {
        val parsed = runCatching { PolynomialEquationParser.parseEquation(expression.source) }
            .getOrElse { return SolveResult.Unsupported(SolveResult.Reason.InvalidEquation) }

        return when (parsed.polynomial.degree()) {
            0 -> SolveResult.Unsupported(SolveResult.Reason.InvalidEquation)
            1 -> solveLinear(expression, problemType, parsed)
            2 -> solveQuadratic(expression, parsed)
            else -> SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm)
        }
    }

    private fun solveLinear(
        expression: Expression,
        problemType: ProblemType,
        parsed: ParsedEquation,
    ): SolveResult {
        val a = parsed.polynomial.coefficient(1)
        val b = parsed.polynomial.coefficient(0)
        if (abs(a) < Epsilon) {
            return SolveResult.Unsupported(SolveResult.Reason.InvalidEquation)
        }

        val root = -b / a
        val variable = parsed.variable
        val normalized = "${format(a)}$variable ${formatSigned(b)} = 0"

        return SolveResult.Success(
            Solution(
                expression = expression,
                problemType = problemType,
                answer = "$variable = ${format(root)}",
                graph = PolynomialGraph(
                    title = polynomialTitle(variable, mapOf(1 to a, 0 to b)),
                    variable = variable,
                    coefficients = mapOf(1 to a, 0 to b),
                    roots = listOf(root),
                ),
                steps = listOf(
                    SolutionStep(
                        title = "Приводим к виду ax + b = 0",
                        formula = normalized,
                        explanation = "Переносим всё в левую часть и собираем коэффициенты.",
                    ),
                    SolutionStep(
                        title = "Изолируем переменную",
                        formula = "$variable = -${format(b)} / ${format(a)}",
                        explanation = "Для линейного уравнения корень равен -b / a.",
                    ),
                    SolutionStep(
                        title = "Получаем ответ",
                        formula = "$variable = ${format(root)}",
                        explanation = "Подставляем коэффициенты и упрощаем результат.",
                    ),
                ),
            )
        )
    }

    private fun solveQuadratic(
        expression: Expression,
        parsed: ParsedEquation,
    ): SolveResult {
        val a = parsed.polynomial.coefficient(2)
        val b = parsed.polynomial.coefficient(1)
        val c = parsed.polynomial.coefficient(0)
        if (abs(a) < Epsilon) {
            return solveLinear(expression, ProblemType.LinearEquation, parsed)
        }

        val discriminant = b * b - 4 * a * c
        val variable = parsed.variable
        val normalized = "${format(a)}$variable^2 ${formatSigned(b)}$variable ${formatSigned(c)} = 0"

        val answer: String
        val rootSteps: List<SolutionStep>
        if (discriminant > Epsilon) {
            val sqrtD = sqrt(discriminant)
            val x1 = (-b - sqrtD) / (2 * a)
            val x2 = (-b + sqrtD) / (2 * a)
            answer = "$variable₁ = ${format(x1)}, $variable₂ = ${format(x2)}"
            rootSteps = listOf(
                SolutionStep(
                    title = "Находим два корня",
                    formula = "$variable = (-b ± √D) / 2a",
                    explanation = "Так как D > 0, у квадратного уравнения два действительных корня.",
                ),
                SolutionStep(
                    title = "Получаем ответ",
                    formula = answer,
                    explanation = "Подставляем a, b, c в формулу корней.",
                ),
            )
        } else if (abs(discriminant) <= Epsilon) {
            val root = -b / (2 * a)
            answer = "$variable = ${format(root)}"
            rootSteps = listOf(
                SolutionStep(
                    title = "Находим единственный корень",
                    formula = "$variable = -b / 2a",
                    explanation = "Так как D = 0, оба корня совпадают.",
                ),
                SolutionStep(
                    title = "Получаем ответ",
                    formula = answer,
                    explanation = "Подставляем коэффициенты в формулу.",
                ),
            )
        } else {
            answer = "Нет действительных корней"
            rootSteps = listOf(
                SolutionStep(
                    title = "Проверяем дискриминант",
                    formula = "D < 0",
                    explanation = "При отрицательном дискриминанте действительных корней нет.",
                )
            )
        }

        return SolveResult.Success(
            Solution(
                expression = expression,
                problemType = ProblemType.QuadraticEquation,
                answer = answer,
                graph = PolynomialGraph(
                    title = polynomialTitle(variable, mapOf(2 to a, 1 to b, 0 to c)),
                    variable = variable,
                    coefficients = mapOf(2 to a, 1 to b, 0 to c),
                    roots = realRoots(discriminant, a, b),
                    vertex = GraphPoint(
                        x = -b / (2 * a),
                        y = discriminant.let { c - b * b / (4 * a) },
                        label = "вершина",
                    ),
                ),
                steps = listOf(
                    SolutionStep(
                        title = "Приводим к виду ax² + bx + c = 0",
                        formula = normalized,
                        explanation = "Переносим всё в левую часть и собираем коэффициенты.",
                    ),
                    SolutionStep(
                        title = "Считаем дискриминант",
                        formula = "D = b² - 4ac = ${format(discriminant)}",
                        explanation = "Дискриминант определяет количество действительных корней.",
                    ),
                ) + rootSteps,
            )
        )
    }

    private fun solvePolynomialIntegral(expression: Expression): SolveResult {
        val parsed = runCatching { PolynomialIntegralParser.parse(expression.source) }
            .getOrElse { return SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm) }

        val integralCoefficients = parsed.polynomial.coefficients
            .mapKeys { (power, _) -> power + 1 }
            .mapValues { (power, coefficient) -> coefficient / power }
            .filterValues { abs(it) > Epsilon }

        if (integralCoefficients.isEmpty()) {
            return SolveResult.Success(
                Solution(
                    expression = expression,
                    problemType = ProblemType.Integral,
                    answer = "C",
                    steps = listOf(
                        SolutionStep(
                            title = "Интегрируем константу 0",
                            formula = "∫ 0 d${parsed.variable} = C",
                            explanation = "Первообразная нуля равна произвольной постоянной.",
                        )
                    ),
                )
            )
        }

        val answerBody = polynomialBody(parsed.variable, integralCoefficients)
        return SolveResult.Success(
            Solution(
                expression = expression,
                problemType = ProblemType.Integral,
                answer = "$answerBody + C",
                graph = PolynomialGraph(
                    title = "y = $answerBody",
                    variable = parsed.variable,
                    coefficients = integralCoefficients,
                ),
                steps = listOf(
                    SolutionStep(
                        title = "Определяем тип интеграла",
                        formula = "∫ ${parsed.integrandSource} d${parsed.variable}",
                        explanation = "Подынтегральное выражение является многочленом, поэтому интегрируем каждый член отдельно.",
                    ),
                    SolutionStep(
                        title = "Применяем правило степени",
                        formula = "∫ a${parsed.variable}^n d${parsed.variable} = a${parsed.variable}^(n + 1) / (n + 1)",
                        explanation = "Коэффициент сохраняется, а степень увеличивается на единицу.",
                    ),
                    SolutionStep(
                        title = "Получаем первообразную",
                        formula = "$answerBody + C",
                        explanation = "Добавляем постоянную интегрирования C.",
                    ),
                ),
            )
        )
    }

    private fun format(value: Double): String {
        if (abs(value) < Epsilon) return "0"
        val rounded = value.toLong()
        return if (abs(value - rounded) < Epsilon) {
            rounded.toString()
        } else {
            "%.4f".format(value).trimEnd('0').trimEnd('.')
        }
    }

    private fun formatSigned(value: Double): String =
        if (value < 0) "- ${format(abs(value))}" else "+ ${format(value)}"

    private fun realRoots(discriminant: Double, a: Double, b: Double): List<Double> =
        when {
            discriminant > Epsilon -> {
                val sqrtD = sqrt(discriminant)
                listOf((-b - sqrtD) / (2 * a), (-b + sqrtD) / (2 * a))
            }
            abs(discriminant) <= Epsilon -> listOf(-b / (2 * a))
            else -> emptyList()
        }

    private fun polynomialTitle(variable: String, coefficients: Map<Int, Double>): String {
        val body = polynomialBody(variable, coefficients)
        return if (body == "0") "y = 0" else "y = $body"
    }

    private fun polynomialBody(variable: String, coefficients: Map<Int, Double>): String {
        val terms = coefficients
            .toSortedMap(compareByDescending { it })
            .mapNotNull { (power, coefficient) ->
                if (abs(coefficient) < Epsilon) return@mapNotNull null
                val sign = if (coefficient < 0) "-" else "+"
                val absCoefficient = abs(coefficient)
                val body = when (power) {
                    0 -> format(absCoefficient)
                    1 -> "${formatCoefficient(absCoefficient)}$variable"
                    2 -> "${formatCoefficient(absCoefficient)}$variable²"
                    else -> "${formatCoefficient(absCoefficient)}$variable^$power"
                }
                sign to body
            }

        if (terms.isEmpty()) return "0"

        val first = terms.first()
        val tail = terms.drop(1).joinToString("") { (sign, body) -> " $sign $body" }
        val prefix = if (first.first == "-") "-${first.second}" else first.second
        return "$prefix$tail"
    }

    private fun formatCoefficient(value: Double): String =
        if (abs(value - 1.0) < Epsilon) "" else format(value)
}
