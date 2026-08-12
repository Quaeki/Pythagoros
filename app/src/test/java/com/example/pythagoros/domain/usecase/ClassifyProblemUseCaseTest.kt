package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyProblemUseCaseTest {
    private val classify = ClassifyProblemUseCase()

    @Test
    fun detectsQuadraticEquation() {
        assertEquals(
            ProblemType.QuadraticEquation,
            classify(Expression("x ^ 2 - 4 * x + 3 = 0")),
        )
    }

    @Test
    fun detectsLinearEquation() {
        assertEquals(
            ProblemType.LinearEquation,
            classify(Expression("3 * x - 9 = 0")),
        )
    }

    @Test
    fun detectsEquationSystem() {
        assertEquals(
            ProblemType.EquationSystem,
            classify("x + y = 5; x - y = 1"),
        )
    }

    @Test
    fun detectsLimit() {
        assertEquals(
            ProblemType.Limit,
            classify("lim x -> 0 sin(x) / x"),
        )
    }

    @Test
    fun detectsIntegral() {
        assertEquals(
            ProblemType.Integral,
            classify("∫ x^2 dx"),
        )
    }

    @Test
    fun detectsDerivative() {
        assertEquals(
            ProblemType.Derivative,
            classify("f'(x) = 2x"),
        )
    }

    @Test
    fun detectsWordProblem() {
        assertEquals(
            ProblemType.WordProblem,
            classify("Из двух городов навстречу друг другу выехали автомобили. Расстояние 240 км. Через сколько часов они встретятся?"),
        )
    }

    @Test
    fun detectsPhysics() {
        assertEquals(
            ProblemType.Physics,
            classify("Тело массой 2 кг движется с ускорением 3 м/с^2. Найти силу."),
        )
    }
}
