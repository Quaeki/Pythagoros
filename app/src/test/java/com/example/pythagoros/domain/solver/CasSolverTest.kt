package com.example.pythagoros.domain.solver

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolveResult
import org.junit.Assert.assertTrue
import org.junit.Test

class CasSolverTest {
    private val solver = CasSolver()

    @Test
    fun solvesCubicEquation() {
        val result = solver.solve(
            Expression("x ^ 3 - 1 = 0"),
            ProblemType.LinearEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertTrue(solution.answer.contains("x"))
        assertTrue(solution.answer.contains("1"))
    }

    @Test
    fun integratesTrigonometricExpression() {
        val result = solver.solve(
            Expression("∫ sin(x) dx"),
            ProblemType.Integral,
        )

        val solution = (result as SolveResult.Success).solution
        assertTrue(solution.answer.contains("Cos"))
        assertTrue(solution.answer.contains("+ C"))
    }

    @Test
    fun evaluatesPlainExpression() {
        val result = solver.solve(
            Expression("sqrt(9)"),
            ProblemType.Unknown,
        )

        val solution = (result as SolveResult.Success).solution
        assertTrue(solution.answer.contains("3"))
    }
}
