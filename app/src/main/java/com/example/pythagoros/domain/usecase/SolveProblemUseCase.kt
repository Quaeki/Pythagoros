package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.solver.CasSolver
import com.example.pythagoros.domain.solver.LocalSolver

class SolveProblemUseCase(
    private val localSolver: LocalSolver = LocalSolver(),
    private val casSolver: CasSolver = CasSolver(),
) {
    operator fun invoke(expression: Expression, problemType: ProblemType): SolveResult {
        val localResult = localSolver.solve(expression, problemType)
        if (localResult is SolveResult.Success) {
            return localResult
        }

        return when (val casResult = casSolver.solve(expression, problemType)) {
            is SolveResult.Success -> casResult
            is SolveResult.Unsupported -> localResult
        }
    }
}
