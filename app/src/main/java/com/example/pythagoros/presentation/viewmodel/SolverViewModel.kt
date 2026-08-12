package com.example.pythagoros.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiSolver
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.example.pythagoros.domain.usecase.ParseExpressionUseCase
import com.example.pythagoros.domain.usecase.SolveProblemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SolverViewModel @Inject constructor(
    private val parseExpression: ParseExpressionUseCase,
    private val classifyProblem: ClassifyProblemUseCase,
    private val solveProblem: SolveProblemUseCase,
    private val premiumAiSolver: PremiumAiSolver,
) : ViewModel() {

    fun parse(raw: String): Result<Expression> =
        parseExpression(raw)

    fun classify(raw: String): ProblemType =
        classifyProblem(raw)

    fun classify(expression: Expression): ProblemType =
        classifyProblem(expression)

    suspend fun solveLocal(expression: Expression, problemType: ProblemType): SolveResult =
        withContext(Dispatchers.Default) {
            solveProblem(expression, problemType)
        }

    suspend fun solveWithAi(request: PremiumAiSolveRequest): PremiumAiSolveResult =
        premiumAiSolver.solve(request)
}
