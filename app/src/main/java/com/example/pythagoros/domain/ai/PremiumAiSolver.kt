package com.example.pythagoros.domain.ai

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolutionStep

/**
 * Pro fallback for tasks that the local solver cannot handle.
 *
 * Implement this through your backend. Do not call Gemini directly from Android:
 * the API key must stay on the server.
 */
interface PremiumAiSolver {
    suspend fun solve(request: PremiumAiSolveRequest): PremiumAiSolveResult
}

data class PremiumAiSolveRequest(
    val expression: Expression,
    val problemType: ProblemType,
    val imagePath: String? = null,
)

sealed interface PremiumAiSolveResult {
    data class Success(
        val answer: String,
        val steps: List<SolutionStep>,
        val graph: PremiumAiGraph? = null,
    ) : PremiumAiSolveResult

    data class Failure(val message: String) : PremiumAiSolveResult
}

data class PremiumAiGraph(
    val title: String,
    val variable: String,
    val expression: String,
    val notes: List<String> = emptyList(),
    val kind: String? = null,
    val viewport: PremiumAiViewport? = null,
    val primitives: List<PremiumAiPrimitive> = emptyList(),
)

data class PremiumAiViewport(
    val xMin: Double?,
    val xMax: Double?,
    val yMin: Double?,
    val yMax: Double?,
)

data class PremiumAiPoint(
    val x: Double,
    val y: Double,
)

data class PremiumAiPrimitive(
    val type: String,
    val id: String? = null,
    val role: String? = null,
    val points: List<PremiumAiPoint> = emptyList(),
    val from: PremiumAiPoint? = null,
    val to: PremiumAiPoint? = null,
    val center: PremiumAiPoint? = null,
    val radius: Double? = null,
    val startAngle: Double? = null,
    val endAngle: Double? = null,
    val at: PremiumAiPoint? = null,
    val text: String? = null,
    val label: String? = null,
    val dashed: Boolean = false,
    val filled: Boolean = false,
    val closed: Boolean? = null,
)
