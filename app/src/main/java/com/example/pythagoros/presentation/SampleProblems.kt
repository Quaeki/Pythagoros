package com.example.pythagoros.presentation

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.FigurePrimitive
import com.example.pythagoros.domain.model.FigureRole
import com.example.pythagoros.domain.model.GraphPoint
import com.example.pythagoros.domain.model.MarkId
import com.example.pythagoros.domain.model.NormPoint
import com.example.pythagoros.domain.model.PlotMarks
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.StepDetail
import com.example.pythagoros.domain.model.SubStep
import com.example.pythagoros.domain.model.Visualization

/**
 * Разборы из макета: квадратичная функция (1a), интеграл (2a), геометрия (4a) и механика (4b).
 *
 * Они нужны, пока решатель не умеет заполнять `reveal` и `detail` сам:
 * по ним видно, как экран решения ведёт себя со всеми четырьмя слотами визуализации.
 */
object SampleProblems {

    private val quadraticGraph = PolynomialGraph(
        title = "y = x² − 4x + 3",
        variable = "x",
        coefficients = mapOf(2 to 1.0, 1 to -4.0, 0 to 3.0),
        roots = listOf(1.0, 3.0),
        vertex = GraphPoint(2.0, -1.0, "(2; −1)"),
    )

    /** 1a. Построение графика квадратичной функции. */
    val quadratic = Solution(
        expression = Expression("Постройте график y = x² − 4x + 3 и найдите нули функции."),
        problemType = ProblemType.QuadraticEquation,
        answer = "x₁ = 1, x₂ = 3; вершина (2; −1)",
        graph = quadraticGraph,
        steps = listOf(
            SolutionStep(
                title = "Смотрим на функцию",
                formula = "y = x² − 4x + 3",
                explanation = "Это квадратичная функция: старший коэффициент 1 > 0, значит ветви параболы вверх.",
                detail = StepDetail(
                    rule = "Вид квадратичной функции y = ax² + bx + c",
                    why = "Знак a решает всё: при a > 0 ветви идут вверх и у функции есть минимум, при a < 0 — вниз и максимум. Дальше нам останется найти всего три опорные точки.",
                    substeps = listOf(
                        SubStep("a = 1, b = −4, c = 3", "Выписываем коэффициенты"),
                        SubStep("a = 1 > 0", "Ветви вверх, есть минимум"),
                        SubStep("c = 3", "График пересекает ось Y в точке (0; 3)"),
                    ),
                    verification = "Проверьте себя: если бы было y = −x² + 4x − 3, ветви смотрели бы вниз.",
                ),
            ),
            SolutionStep(
                title = "Находим нули",
                formula = "(x − 1)(x − 3) = 0 → x = 1; x = 3",
                explanation = "Приравниваем к нулю и раскладываем на множители. Точки пересечения с осью X появляются на графике.",
                reveal = setOf(PlotMarks.Roots),
                detail = StepDetail(
                    rule = "Нули функции: y = 0",
                    why = "Нули — это точки, где график касается оси X. Их две, потому что дискриминант положительный.",
                    substeps = listOf(
                        SubStep("x² − 4x + 3 = 0", "Приравниваем к нулю"),
                        SubStep("D = 16 − 12 = 4", "Считаем дискриминант"),
                        SubStep("x = (4 ± 2)/2", "Формула корней"),
                        SubStep("x₁ = 1, x₂ = 3", "Два корня — две точки на оси X"),
                    ),
                    verification = "Подстановка: 1 − 4 + 3 = 0 ✓ и 9 − 12 + 3 = 0 ✓",
                ),
            ),
            SolutionStep(
                title = "Ищем вершину",
                formula = "x₀ = 2, y₀ = −1",
                explanation = "Вершина посередине между корнями. Подставляем x = 2 и получаем минимум функции.",
                reveal = setOf(PlotMarks.Roots, PlotMarks.Vertex),
                detail = StepDetail(
                    rule = "Абсцисса вершины x₀ = −b / 2a",
                    why = "Парабола симметрична, поэтому вершина ровно посередине между корнями. Это и есть минимум функции.",
                    substeps = listOf(
                        SubStep("x₀ = −(−4) / (2·1) = 2", "По формуле вершины"),
                        SubStep("y₀ = 4 − 8 + 3 = −1", "Подставляем x₀ в функцию"),
                        SubStep("(2; −1)", "Координаты вершины"),
                    ),
                    verification = "Середина между 1 и 3 — это 2. Совпало ✓",
                ),
            ),
            SolutionStep(
                title = "Проводим кривую",
                formula = "Вершина (2; −1), ветви вверх",
                explanation = "Через корни и вершину плавно проводим параболу. Готово — график построен по точкам, а не наугад.",
                reveal = setOf(PlotMarks.Roots, PlotMarks.Vertex, PlotMarks.Curve),
                detail = StepDetail(
                    rule = "Построение по опорным точкам",
                    why = "Трёх точек и знания направления ветвей достаточно, чтобы провести параболу однозначно. Ось симметрии — прямая x = 2.",
                    substeps = listOf(
                        SubStep("(1; 0) и (3; 0)", "Нули функции"),
                        SubStep("(2; −1)", "Вершина, самая нижняя точка"),
                        SubStep("(0; 3) и (4; 3)", "Симметричная пара для точности"),
                    ),
                    verification = "Готово: y < 0 при 1 < x < 3, y > 0 вне этого промежутка.",
                ),
            ),
        ),
    )

    val quadraticVisualization = Visualization.Plot2D(quadraticGraph)

    /** 2a. Интеграл — задача, которой график не нужен. */
    val integral = Solution(
        expression = Expression("∫ (3x² + 2x) dx"),
        problemType = ProblemType.Integral,
        answer = "x³ + x² + C",
        steps = listOf(
            SolutionStep(
                title = "Разбиваем на слагаемые",
                formula = "∫(3x² + 2x)dx = ∫3x²dx + ∫2xdx",
                explanation = "Интеграл суммы равен сумме интегралов — считаем каждое слагаемое отдельно.",
            ),
            SolutionStep(
                title = "Выносим коэффициенты",
                formula = "3∫x²dx + 2∫x dx",
                explanation = "Числовой множитель можно вынести за знак интеграла.",
            ),
            SolutionStep(
                title = "Применяем формулу степени",
                formula = "3·x³/3 + 2·x²/2",
                explanation = "∫xⁿdx = xⁿ⁺¹/(n+1). Для n = 2 и n = 1 подставляем и сокращаем.",
            ),
            SolutionStep(
                title = "Записываем ответ",
                formula = "x³ + x² + C",
                explanation = "Не забываем константу интегрирования C — первообразных бесконечно много.",
            ),
        ),
    )

    val integralVisualization = Visualization.NotNeeded(
        hint = "График здесь не нужен. Можно посмотреть площадь под кривой",
        actionText = "открыть визуализацию",
    )

    // ── 4a. Геометрия ──

    private object GeoMarks {
        val Triangle = MarkId("geo.triangle")
        val RightAngle = MarkId("geo.right-angle")
        val Vertices = MarkId("geo.vertices")
        val Sides = MarkId("geo.sides")
        val Height = MarkId("geo.height")
    }

    /**
     * Чертёж прямоугольного треугольника: прямой угол при C, катеты CA = 6 и CB = 8,
     * высота CH — перпендикуляр из C на гипотенузу AB.
     *
     * В HTML-моке прямой угол нарисован у другой вершины, а высота не попадает
     * на гипотенузу; здесь чертёж приведён к условию задачи — иначе AI-построение
     * противоречило бы тексту, который оно объясняет.
     */
    private val trianglePrimitives = listOf(
        FigurePrimitive.Polygon(
            id = GeoMarks.Triangle,
            points = listOf(
                NormPoint(0.130f, 0.860f), // C — прямой угол
                NormPoint(0.130f, 0.160f), // A
                NormPoint(0.783f, 0.860f), // B
            ),
        ),
        FigurePrimitive.RightAngle(
            id = GeoMarks.RightAngle,
            corner = NormPoint(0.130f, 0.860f),
            along = NormPoint(0.783f, 0.860f),
            across = NormPoint(0.130f, 0.160f),
        ),
        FigurePrimitive.Label(GeoMarks.Vertices, NormPoint(0.055f, 0.055f), "A", emphasized = true),
        FigurePrimitive.Label(GeoMarks.Vertices, NormPoint(0.045f, 0.880f), "C", emphasized = true),
        FigurePrimitive.Label(GeoMarks.Vertices, NormPoint(0.810f, 0.880f), "B", emphasized = true),
        FigurePrimitive.Label(GeoMarks.Sides, NormPoint(0.045f, 0.470f), "6", role = FigureRole.Support),
        FigurePrimitive.Label(GeoMarks.Sides, NormPoint(0.430f, 0.895f), "8", role = FigureRole.Support),
        FigurePrimitive.Segment(
            id = GeoMarks.Height,
            from = NormPoint(0.130f, 0.860f),
            to = NormPoint(0.365f, 0.412f),
            dashed = true,
        ),
        FigurePrimitive.Dot(GeoMarks.Height, NormPoint(0.365f, 0.412f)),
        FigurePrimitive.Label(GeoMarks.Height, NormPoint(0.390f, 0.330f), "H", role = FigureRole.Auxiliary),
    )

    val geometry = Solution(
        expression = Expression("В треугольнике ABC угол C = 90°, AC = 6, BC = 8. Найдите высоту CH, опущенную на гипотенузу."),
        problemType = ProblemType.Geometry,
        answer = "CH = 4,8",
        steps = listOf(
            SolutionStep(
                title = "Строим прямоугольный треугольник",
                formula = "",
                explanation = "Отмечаем прямой угол при вершине C и катеты AC = 6, BC = 8.",
                reveal = setOf(GeoMarks.Triangle, GeoMarks.RightAngle, GeoMarks.Vertices),
            ),
            SolutionStep(
                title = "Гипотенуза по Пифагору: AB = 10",
                formula = "AB = √(6² + 8²) = 10",
                explanation = "Классическая тройка 6–8–10: гипотенуза находится сразу.",
                reveal = setOf(GeoMarks.Triangle, GeoMarks.RightAngle, GeoMarks.Vertices, GeoMarks.Sides),
            ),
            SolutionStep(
                title = "Проводим высоту CH",
                formula = "",
                explanation = "Высота из прямого угла делит гипотенузу на проекции. Площадь треугольника можно посчитать двумя способами — через катеты и через гипотенузу с высотой.",
                reveal = setOf(
                    GeoMarks.Triangle,
                    GeoMarks.RightAngle,
                    GeoMarks.Vertices,
                    GeoMarks.Sides,
                    GeoMarks.Height,
                ),
            ),
            SolutionStep(
                title = "CH = 6·8 / 10 = 4,8",
                formula = "CH = AC · BC / AB",
                explanation = "Приравниваем две записи площади и выражаем высоту.",
                reveal = setOf(
                    GeoMarks.Triangle,
                    GeoMarks.RightAngle,
                    GeoMarks.Vertices,
                    GeoMarks.Sides,
                    GeoMarks.Height,
                ),
            ),
        ),
    )

    val geometryVisualization = Visualization.Figure(
        primitives = trianglePrimitives,
        caption = "Чертёж строится",
    )

    /** 4c. Тот же чертёж, но закрытый для бесплатного пользователя. */
    val geometryLocked = Visualization.LockedPro(
        preview = listOf(
            FigurePrimitive.Polygon(
                id = GeoMarks.Triangle,
                points = listOf(
                    NormPoint(0.130f, 0.840f),
                    NormPoint(0.130f, 0.140f),
                    NormPoint(0.783f, 0.840f),
                ),
            ),
            FigurePrimitive.Segment(
                id = GeoMarks.Height,
                from = NormPoint(0.130f, 0.840f),
                to = NormPoint(0.365f, 0.400f),
            ),
        ),
        title = "Чертёж и решение геометрии — в Pro",
        subtitle = "AI строит чертёж по условию и объясняет каждое построение.",
    )

    // ── 4b. Физика ──

    private object ForceMarks {
        val Plane = MarkId("phys.plane")
        val Body = MarkId("phys.body")
        val Weight = MarkId("phys.mg")
        val Friction = MarkId("phys.friction")
        val Normal = MarkId("phys.normal")
        val Angle = MarkId("phys.angle")
    }

    /**
     * Схема сил на наклонной плоскости 30°.
     *
     * Направления выверены по физике, а не срисованы с мока: N перпендикулярна
     * поверхности, трение направлено вдоль неё против скольжения, mg — вертикально вниз.
     */
    private val forcePrimitives = listOf(
        FigurePrimitive.Polygon(
            id = ForceMarks.Plane,
            points = listOf(
                NormPoint(0.067f, 0.875f),
                NormPoint(0.933f, 0.875f),
                NormPoint(0.933f, 0.125f),
            ),
            role = FigureRole.Support,
        ),
        FigurePrimitive.Label(ForceMarks.Angle, NormPoint(0.800f, 0.780f), "30°", role = FigureRole.Support),
        FigurePrimitive.Body(
            id = ForceMarks.Body,
            center = NormPoint(0.458f, 0.450f),
            width = 0.153f,
            height = 0.150f,
            rotationDegrees = -30f,
        ),
        // mg — вертикально вниз.
        FigurePrimitive.Vector(
            id = ForceMarks.Weight,
            from = NormPoint(0.458f, 0.450f),
            to = NormPoint(0.458f, 0.825f),
            role = FigureRole.VectorWeight,
        ),
        FigurePrimitive.Label(ForceMarks.Weight, NormPoint(0.478f, 0.640f), "mg", role = FigureRole.VectorWeight),
        // N — по нормали к поверхности, вверх-влево.
        FigurePrimitive.Vector(
            id = ForceMarks.Normal,
            from = NormPoint(0.458f, 0.450f),
            to = NormPoint(0.341f, 0.147f),
            role = FigureRole.VectorNormal,
        ),
        FigurePrimitive.Label(ForceMarks.Normal, NormPoint(0.270f, 0.060f), "N", role = FigureRole.VectorNormal),
        // Fтр — вдоль плоскости вверх: брусок скользит вниз, трение его тормозит.
        FigurePrimitive.Vector(
            id = ForceMarks.Friction,
            from = NormPoint(0.458f, 0.450f),
            to = NormPoint(0.644f, 0.288f),
            role = FigureRole.VectorFriction,
        ),
        FigurePrimitive.Label(ForceMarks.Friction, NormPoint(0.660f, 0.180f), "Fтр", role = FigureRole.VectorFriction),
    )

    val physics = Solution(
        expression = Expression("Брусок массой 2 кг скользит по наклонной плоскости с углом 30°. Коэффициент трения 0,2. Найдите ускорение."),
        problemType = ProblemType.Physics,
        answer = "a ≈ 3,2 м/с²",
        steps = listOf(
            SolutionStep(
                title = "Расставляем силы: mg, N, Fтр",
                formula = "",
                explanation = "На брусок действуют тяжесть, нормальная реакция опоры и сила трения вдоль плоскости.",
                reveal = setOf(
                    ForceMarks.Plane,
                    ForceMarks.Angle,
                    ForceMarks.Body,
                    ForceMarks.Weight,
                    ForceMarks.Normal,
                    ForceMarks.Friction,
                ),
            ),
            SolutionStep(
                title = "Разложение по осям",
                formula = "ma = mg·sin30° − μ·mg·cos30°",
                explanation = "Ось X направляем вдоль наклонной. Вдоль неё тянет составляющая тяжести, тормозит трение.",
                reveal = setOf(
                    ForceMarks.Plane,
                    ForceMarks.Angle,
                    ForceMarks.Body,
                    ForceMarks.Weight,
                    ForceMarks.Normal,
                    ForceMarks.Friction,
                ),
            ),
            SolutionStep(
                title = "Подставляем числа",
                formula = "a = 10·(0,5 − 0,2·0,87) ≈ 3,2 м/с²",
                explanation = "Масса сокращается — ускорение от неё не зависит.",
                reveal = setOf(
                    ForceMarks.Plane,
                    ForceMarks.Angle,
                    ForceMarks.Body,
                    ForceMarks.Weight,
                    ForceMarks.Normal,
                    ForceMarks.Friction,
                ),
            ),
        ),
    )

    val physicsVisualization = Visualization.Figure(
        primitives = forcePrimitives,
        caption = "Схема сил",
        aspectRatio = 300f / 200f,
    )

    /** 4c для механики: та же плашка, но за размытием — схема сил, а не треугольник. */
    val physicsLocked = Visualization.LockedPro(
        preview = listOf(
            FigurePrimitive.Polygon(
                id = ForceMarks.Plane,
                points = listOf(
                    NormPoint(0.067f, 0.860f),
                    NormPoint(0.933f, 0.860f),
                    NormPoint(0.933f, 0.140f),
                ),
                role = FigureRole.Support,
            ),
            FigurePrimitive.Vector(
                id = ForceMarks.Weight,
                from = NormPoint(0.458f, 0.430f),
                to = NormPoint(0.458f, 0.820f),
                role = FigureRole.VectorWeight,
            ),
        ),
        title = "Разбор физики с векторами — в Pro",
        subtitle = "AI расставляет силы по условию и объясняет каждый переход.",
    )
}
