package com.example.pythagoros.domain.model

data class Solution(
    val expression: Expression,
    val problemType: ProblemType,
    val answer: String,
    val steps: List<SolutionStep>,
    val graph: PolynomialGraph? = null,
)

data class SolutionStep(
    val title: String,
    val formula: String,
    val explanation: String,
    /** Что этот шаг открывает на визуализации: корни, вершину, высоту, вектор. */
    val reveal: Set<MarkId> = emptySet(),
    /** Содержимое шторки «Разобрать этот шаг подробно»; грузится лениво. */
    val detail: StepDetail? = null,
)

/** Полный вывод шага для шторки 3a-detail. */
data class StepDetail(
    /** Название применённого правила. */
    val rule: String,
    /** «Почему так» — объяснение простыми словами. */
    val why: String,
    /** Вывод по действиям. */
    val substeps: List<SubStep>,
    /** Проверка или подстановка, мятная карточка внизу шторки. */
    val verification: String,
)

data class SubStep(
    val math: String,
    val comment: String,
)

data class PolynomialGraph(
    val title: String,
    val variable: String,
    val coefficients: Map<Int, Double>,
    val roots: List<Double> = emptyList(),
    val vertex: GraphPoint? = null,
) {
    fun valueAt(x: Double): Double =
        coefficients.entries.sumOf { (power, coefficient) ->
            coefficient * x.powInt(power)
        }
}

data class GraphPoint(
    val x: Double,
    val y: Double,
    val label: String? = null,
)

sealed interface SolveResult {
    data class Success(val solution: Solution) : SolveResult
    data class Unsupported(val reason: Reason) : SolveResult

    enum class Reason {
        UnknownType,
        RequiresPremium,
        UnsupportedForm,
        InvalidEquation,
    }
}

private fun Double.powInt(power: Int): Double {
    var result = 1.0
    repeat(power) {
        result *= this
    }
    return result
}
