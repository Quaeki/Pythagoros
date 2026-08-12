package com.example.pythagoros.data.ai

import com.example.pythagoros.domain.ai.PremiumAiGraph
import com.example.pythagoros.domain.ai.PremiumAiPoint
import com.example.pythagoros.domain.ai.PremiumAiPrimitive
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiViewport
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.FigurePrimitive
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.Visualization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseMapperTest {
    @Test
    fun mapsGeometryAnswerWithoutCrashingOnLatexSqrt() {
        val original = Expression(
            "В прямоугольном треугольнике ABC угол C = 90°. " +
                "Катеты AC = 6 см и BC = 8 см. Найдите гипотенузу AB."
        )
        val result = PremiumAiSolveResult.Success(
            answer = "AB = \\sqrt{AC^2 + BC^2} = \\sqrt{100} = 10",
            steps = listOf(
                SolutionStep(
                    title = "Нахождение гипотенузы AB",
                    formula = "AB = \\sqrt{AC^2 + BC^2}",
                    explanation = "По теореме Пифагора AB = \\sqrt{6^2 + 8^2}.",
                )
            ),
            graph = PremiumAiGraph(
                title = "Гипотенуза AB в координатах C(0,0), A(0,6), B(8,0)",
                variable = "x",
                expression = "6 - 0.75*x",
                notes = listOf("C(0,0)", "A(0,6)", "B(8,0)"),
            ),
        )

        val mapped = result.toMappedSolution(original, ProblemType.Geometry)

        assertEquals(original, mapped.solution.expression)
        assertTrue(mapped.solution.answer.contains("√(AC^2 + BC^2)"))
        assertTrue(mapped.solution.steps.first().formula.contains("√(AC^2 + BC^2)"))
        assertTrue(mapped.visualization is Visualization.Figure)
        assertTrue(
            (mapped.visualization as Visualization.Figure).primitives.any {
                it.id.value == "ai.figure.circumcircle"
            }
        )
    }

    @Test
    fun buildsRightTriangleFigureWhenAiReturnsOnlyVisualizationDescription() {
        val original = Expression(
            "В прямоугольном треугольнике ABC угол C = 90°. " +
                "Катеты AC = 6 см и BC = 8 см. Найдите гипотенузу AB, " +
                "площадь треугольника и радиус описанной окружности. постройте рисунок"
        )
        val result = PremiumAiSolveResult.Success(
            answer = "AB = 10, S = 24, R = 5",
            steps = listOf(
                SolutionStep(
                    title = "Нахождение радиуса описанной окружности R",
                    formula = "R = AB/2",
                    explanation = "Радиус равен половине гипотенузы: R = 10/2 = 5 см.",
                )
            ),
            graph = PremiumAiGraph(
                title = "Верхняя полуокружность описанной окружности (R = 5)",
                variable = "",
                expression = "",
                notes = listOf("Показать треугольник ABC и описанную окружность"),
            ),
        )

        val mapped = result.toMappedSolution(original, ProblemType.Geometry)
        val visualization = mapped.visualization

        assertTrue(visualization is Visualization.Figure)
        assertFalse(visualization is Visualization.NotNeeded)
        assertTrue(
            (visualization as Visualization.Figure).primitives.any {
                it.id.value == "ai.figure.circumcircle"
            }
        )
    }

    @Test
    fun mapsStructuredVisualPrimitivesFromAi() {
        val result = PremiumAiSolveResult.Success(
            answer = "AB = 10, S = 24, R = 5",
            steps = listOf(
                SolutionStep(
                    title = "Чертёж",
                    formula = "C(0,0), A(0,6), B(8,0)",
                    explanation = "Строим треугольник и описанную окружность.",
                )
            ),
            graph = PremiumAiGraph(
                title = "Описанная окружность",
                kind = "geometry",
                variable = "x",
                expression = "",
                viewport = PremiumAiViewport(xMin = -2.0, xMax = 10.0, yMin = -3.0, yMax = 9.0),
                primitives = listOf(
                    PremiumAiPrimitive(
                        type = "polygon",
                        points = listOf(
                            PremiumAiPoint(0.0, 6.0),
                            PremiumAiPoint(0.0, 0.0),
                            PremiumAiPoint(8.0, 0.0),
                        ),
                    ),
                    PremiumAiPrimitive(
                        type = "circle",
                        id = "circumcircle",
                        center = PremiumAiPoint(4.0, 3.0),
                        radius = 5.0,
                        label = "R = 5",
                    ),
                    PremiumAiPrimitive(
                        type = "right_angle",
                        at = PremiumAiPoint(0.0, 0.0),
                        from = PremiumAiPoint(8.0, 0.0),
                        to = PremiumAiPoint(0.0, 6.0),
                    ),
                    PremiumAiPrimitive(type = "point", at = PremiumAiPoint(0.0, 6.0), label = "A (0, 6)"),
                ),
            ),
        )

        val mapped = result.toMappedSolution(Expression("geometry"), ProblemType.Geometry)
        val visualization = mapped.visualization as Visualization.Figure

        assertTrue(visualization.caption.contains("Описанная окружность"))
        assertTrue(visualization.primitives.any { it.id.value == "ai.structured.circumcircle" })
        assertTrue(
            visualization.primitives
                .filterIsInstance<FigurePrimitive.Label>()
                .any { it.text == "A" }
        )
        assertTrue(visualization.primitives.size >= 4)
    }

    @Test
    fun mapsPhysicsForceVectorsWithLabelsAndCleansLatex() {
        val result = PremiumAiSolveResult.Success(
            answer = "N \\approx 16.97 \\text{ Н}, F_тр \\approx 3.39 \\text{ Н}, a \\approx 3.20 \\text{ м/с^2}",
            steps = listOf(
                SolutionStep(
                    title = "Сила нормальной реакции",
                    formula = "N = m g \\cos 30^\\circ \\approx 16.97 \\text{ Н}",
                    explanation = "Перпендикулярно плоскости ускорения нет.",
                )
            ),
            graph = PremiumAiGraph(
                title = "Схема сил",
                kind = "physics",
                variable = "",
                expression = "",
                viewport = PremiumAiViewport(xMin = -1.0, xMax = 6.0, yMin = -1.0, yMax = 5.0),
                primitives = listOf(
                    PremiumAiPrimitive(
                        type = "polygon",
                        role = "support",
                        points = listOf(PremiumAiPoint(0.0, 0.0), PremiumAiPoint(5.0, 0.0), PremiumAiPoint(5.0, 3.0)),
                    ),
                    PremiumAiPrimitive(
                        type = "vector",
                        role = "weight",
                        from = PremiumAiPoint(2.7, 1.7),
                        to = PremiumAiPoint(2.7, 0.2),
                        label = "mg",
                    ),
                    PremiumAiPrimitive(
                        type = "vector",
                        role = "normal",
                        from = PremiumAiPoint(2.7, 1.7),
                        to = PremiumAiPoint(1.8, 3.0),
                        label = "N",
                    ),
                    PremiumAiPrimitive(
                        type = "vector",
                        role = "friction",
                        from = PremiumAiPoint(2.7, 1.7),
                        to = PremiumAiPoint(1.5, 1.0),
                        label = "Fтр",
                    ),
                ),
            ),
        )

        val mapped = result.toMappedSolution(Expression("physics"), ProblemType.Physics)
        val visualization = mapped.visualization as Visualization.Figure
        val labels = visualization.primitives
            .filterIsInstance<FigurePrimitive.Label>()
            .map { it.text }

        assertTrue(mapped.solution.answer.contains("≈"))
        assertFalse(mapped.solution.answer.contains("\\text"))
        assertTrue(mapped.solution.steps.first().formula.contains("30°"))
        assertTrue(labels.contains("mg"))
        assertTrue(labels.contains("N"))
        assertTrue(labels.contains("Fтр"))
    }
}
