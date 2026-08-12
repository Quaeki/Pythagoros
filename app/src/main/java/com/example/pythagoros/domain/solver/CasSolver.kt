package com.example.pythagoros.domain.solver

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.SolveResult
import java.util.concurrent.TimeUnit
import org.matheclipse.core.eval.ExprEvaluator

class CasSolver(
    private val timeoutSeconds: Long = 4,
) {
    fun solve(expression: Expression, problemType: ProblemType): SolveResult {
        if (problemType.requiresPremium) {
            return SolveResult.Unsupported(SolveResult.Reason.RequiresPremium)
        }

        val request = runCatching { buildRequest(expression.source, problemType) }
            .getOrElse { return SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm) }

        val rawAnswer = runCatching { evaluate(request.command) }
            .getOrElse { return SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm) }

        if (rawAnswer.isBlank() || rawAnswer == "Null" || rawAnswer == "\$Aborted") {
            return SolveResult.Unsupported(SolveResult.Reason.UnsupportedForm)
        }

        return SolveResult.Success(
            Solution(
                expression = expression,
                problemType = problemType,
                answer = formatCasOutput(rawAnswer, request),
                steps = listOf(
                    SolutionStep(
                        title = "Передаём задачу в CAS",
                        formula = request.displayCommand,
                        explanation = "Система компьютерной алгебры получает нормализованное выражение.",
                    ),
                    SolutionStep(
                        title = "Выполняем символьное преобразование",
                        formula = rawAnswer,
                        explanation = request.explanation,
                    ),
                    SolutionStep(
                        title = "Ответ",
                        formula = formatCasOutput(rawAnswer, request),
                        explanation = "Результат получен локальным CAS без обращения к серверу.",
                    ),
                ),
            )
        )
    }

    private fun evaluate(command: String): String {
        val evaluator = ExprEvaluator(false, 100.toShort())
        val result = evaluator.evaluateWithTimeout(
            command,
            timeoutSeconds,
            TimeUnit.SECONDS,
            true,
            null,
        )
        return result.toString()
    }

    private fun buildRequest(source: String, problemType: ProblemType): CasRequest =
        when (problemType) {
            ProblemType.LinearEquation,
            ProblemType.QuadraticEquation -> buildEquationRequest(source)
            ProblemType.Integral -> buildIntegralRequest(source)
            ProblemType.Derivative -> buildDerivativeRequest(source)
            ProblemType.Limit -> buildLimitRequest(source)
            ProblemType.Unknown -> buildExpressionRequest(source)
            else -> throw IllegalArgumentException("Unsupported CAS problem type: $problemType")
        }

    private fun buildEquationRequest(source: String): CasRequest {
        require(source.count { it == '=' } == 1) { "Equation must contain one equals sign" }
        val variable = detectVariable(source)
        val equation = source.replace("=", "==").toCasExpression()
        return CasRequest(
            command = "Solve($equation,$variable)",
            displayCommand = source,
            kind = CasRequestKind.Equation(variable),
            explanation = "CAS решает уравнение относительно переменной $variable.",
        )
    }

    private fun buildIntegralRequest(source: String): CasRequest {
        val match = Regex("""^\s*∫\s*(.+)\s+d([a-zA-Z])\s*$""").matchEntire(source)
            ?: throw IllegalArgumentException("Integral must look like ∫ f(x) dx")
        val integrand = match.groupValues[1].toCasExpression()
        val variable = match.groupValues[2]
        return CasRequest(
            command = "Integrate($integrand,$variable)",
            displayCommand = source,
            kind = CasRequestKind.Integral(variable),
            explanation = "CAS ищет первообразную по переменной $variable.",
        )
    }

    private fun buildDerivativeRequest(source: String): CasRequest {
        val variable = detectVariable(source)
        val expression = source.toCasExpression()
        return CasRequest(
            command = "D($expression,$variable)",
            displayCommand = source,
            kind = CasRequestKind.Derivative(variable),
            explanation = "CAS дифференцирует выражение по переменной $variable.",
        )
    }

    private fun buildLimitRequest(source: String): CasRequest {
        val match = Regex("""^\s*lim\s+([a-zA-Z])\s*->\s*([^\s]+)\s+(.+)$""").matchEntire(source)
            ?: throw IllegalArgumentException("Limit must look like lim x -> 0 f(x)")
        val variable = match.groupValues[1]
        val point = match.groupValues[2].toCasExpression()
        val body = match.groupValues[3].toCasExpression()
        return CasRequest(
            command = "Limit($body,$variable->$point)",
            displayCommand = source,
            kind = CasRequestKind.Limit,
            explanation = "CAS вычисляет предел в указанной точке.",
        )
    }

    private fun buildExpressionRequest(source: String): CasRequest {
        require("=" !in source) { "Plain expression must not contain equation signs" }
        val expression = source.toCasExpression()
        return CasRequest(
            command = "Simplify($expression)",
            displayCommand = source,
            kind = CasRequestKind.Expression,
            explanation = "CAS упрощает или вычисляет выражение.",
        )
    }

    private fun detectVariable(source: String): String =
        Regex("""\b[a-zA-Z]\b""").findAll(source)
            .map { it.value }
            .firstOrNull { it !in ReservedSingleLetterSymbols }
            ?: "x"

    private fun String.toCasExpression(): String =
        trim()
            .replace(" ", "")
            .replace("π", "Pi")
            .replace(Regex("""\bsqrt\("""), "Sqrt(")
            .replace(Regex("""\bsin\("""), "Sin(")
            .replace(Regex("""\bcos\("""), "Cos(")
            .replace(Regex("""\btan\("""), "Tan(")
            .replace(Regex("""\bctg\("""), "Cot(")
            .replace(Regex("""\bln\("""), "Log(")
            .replace(Regex("""\blog\("""), "Log(")
            .replace(Regex("""\babs\("""), "Abs(")
            .replace(Regex("""\bexp\("""), "Exp(")

    private fun formatCasOutput(rawAnswer: String, request: CasRequest): String =
        when (request.kind) {
            is CasRequestKind.Equation -> rawAnswer
                .replace("->", " = ")
                .replace("{", "")
                .replace("}", "")
            is CasRequestKind.Integral -> "$rawAnswer + C"
            is CasRequestKind.Derivative,
            CasRequestKind.Expression,
            CasRequestKind.Limit -> rawAnswer
        }
}

private data class CasRequest(
    val command: String,
    val displayCommand: String,
    val kind: CasRequestKind,
    val explanation: String,
)

private sealed interface CasRequestKind {
    data class Equation(val variable: String) : CasRequestKind
    data class Integral(val variable: String) : CasRequestKind
    data class Derivative(val variable: String) : CasRequestKind
    data object Expression : CasRequestKind
    data object Limit : CasRequestKind
}

private val ReservedSingleLetterSymbols = setOf("d", "e", "E", "I")
