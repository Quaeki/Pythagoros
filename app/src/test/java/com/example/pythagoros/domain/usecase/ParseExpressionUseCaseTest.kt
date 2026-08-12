package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.ProblemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseExpressionUseCaseTest {
    private val parse = ParseExpressionUseCase()
    private val classify = ClassifyProblemUseCase()

    @Test
    fun normalizesQuadraticEquationFromHumanInput() {
        val expression = parse("x² − 4x + 3 = 0").getOrThrow()

        assertEquals("x ^ 2 - 4 * x + 3 = 0", expression.source)
        assertEquals(ProblemType.QuadraticEquation, classify(expression))
    }

    @Test
    fun normalizesImplicitMultiplicationAroundParentheses() {
        val expression = parse("2(x + 1)(x - 3)").getOrThrow()

        assertEquals("2 * (x + 1) * (x - 3)", expression.source)
    }

    @Test
    fun supportsFunctionsAndDecimalComma() {
        val expression = parse("sqrt(0,25) + sin(x)").getOrThrow()

        assertEquals("sqrt(0.25) + sin(x)", expression.source)
    }

    @Test
    fun normalizesIntegralWithAsciiPowerFromOcr() {
        val expression = parse("∫ (3x2 + 2x + 1)dx").getOrThrow()

        assertEquals("∫ (3 * x ^ 2 + 2 * x + 1) dx", expression.source)
        assertEquals(ProblemType.Integral, classify(expression))
    }

    @Test
    fun rejectsUnclosedParenthesis() {
        val result = parse("(x + 1")

        assertTrue(result.isFailure)
    }

    @Test
    fun rejectsBrokenOperatorSequence() {
        val result = parse("x + * 2")

        assertTrue(result.isFailure)
    }
}
