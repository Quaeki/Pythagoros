package com.example.pythagoros.domain.solver

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolveResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSolverTest {
    private val solver = LocalSolver()

    @Test
    fun solvesLinearEquation() {
        val result = solver.solve(
            Expression("3 * x - 9 = 0"),
            ProblemType.LinearEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("x = 3", solution.answer)
        assertEquals(3, solution.steps.size)
    }

    @Test
    fun solvesLinearEquationWithParentheses() {
        val result = solver.solve(
            Expression("2 * (x + 1) = 8"),
            ProblemType.LinearEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("x = 3", solution.answer)
        assertEquals(listOf(3.0), solution.graph?.roots)
        assertEquals(2.0, solution.graph?.coefficients?.get(1) ?: error("No graph"), 1e-9)
        assertEquals(-6.0, solution.graph?.coefficients?.get(0) ?: error("No graph"), 1e-9)
    }

    @Test
    fun solvesQuadraticEquationWithTwoRoots() {
        val result = solver.solve(
            Expression("x ^ 2 - 4 * x + 3 = 0"),
            ProblemType.QuadraticEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("x₁ = 1, x₂ = 3", solution.answer)
        assertEquals(4, solution.steps.size)
        assertEquals(listOf(1.0, 3.0), solution.graph?.roots)
        assertEquals(2.0, solution.graph?.vertex?.x ?: error("No vertex"), 1e-9)
        assertEquals(-1.0, solution.graph?.vertex?.y ?: error("No vertex"), 1e-9)
    }

    @Test
    fun solvesQuadraticEquationWithOneRoot() {
        val result = solver.solve(
            Expression("x ^ 2 - 2 * x + 1 = 0"),
            ProblemType.QuadraticEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("x = 1", solution.answer)
    }

    @Test
    fun reportsNoRealRoots() {
        val result = solver.solve(
            Expression("x ^ 2 + 1 = 0"),
            ProblemType.QuadraticEquation,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("Нет действительных корней", solution.answer)
    }

    @Test
    fun solvesPolynomialIntegral() {
        val result = solver.solve(
            Expression("∫ (3 * x ^ 2 + 2 * x + 1) dx"),
            ProblemType.Integral,
        )

        val solution = (result as SolveResult.Success).solution
        assertEquals("x^3 + x² + x + C", solution.answer)
        assertEquals(3, solution.steps.size)
        assertEquals(1.0, solution.graph?.coefficients?.get(3) ?: error("No graph"), 1e-9)
        assertEquals(1.0, solution.graph?.coefficients?.get(2) ?: error("No graph"), 1e-9)
        assertEquals(1.0, solution.graph?.coefficients?.get(1) ?: error("No graph"), 1e-9)
    }

    @Test
    fun routesPremiumTypesAwayFromLocalSolver() {
        val result = solver.solve(
            Expression("Тело массой 2 кг движется с ускорением 3 м/с^2. Найти силу."),
            ProblemType.Physics,
        )

        assertTrue(result is SolveResult.Unsupported)
        assertEquals(SolveResult.Reason.RequiresPremium, (result as SolveResult.Unsupported).reason)
    }
}
