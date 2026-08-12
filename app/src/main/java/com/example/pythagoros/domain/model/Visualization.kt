package com.example.pythagoros.domain.model

/**
 * Что показывать в слоте визуализации экрана решения.
 *
 * Каркас экрана один и тот же (макеты 1a, 2a, 4a, 4b, 4c) — меняется только этот слот,
 * поэтому выбор варианта живёт в домене, а не в UI.
 */
sealed interface Visualization {
    /** Заголовок тёмной карточки: «График строится», «Чертёж строится», «Схема сил». */
    val caption: String

    /** График функции: оси, корни, вершина, кривая (макет 1a / 1b). */
    data class Plot2D(
        val graph: PolynomialGraph,
        override val caption: String = "График строится",
    ) : Visualization

    /**
     * Чертёж или схема, собранные из примитивов в нормализованных координатах
     * (макеты 4a геометрия и 4b физика). Картинок с сервера нет — только примитивы.
     */
    data class Figure(
        val primitives: List<FigurePrimitive>,
        override val caption: String,
        val aspectRatio: Float = 300f / 210f,
    ) : Visualization

    /**
     * Визуализация задаче не нужна — вместо карточки пунктирная плашка (макет 2a).
     * [actionText] задают только те задачи, у которых всё же есть что показать.
     */
    data class NotNeeded(
        val hint: String = "Для этой задачи график не нужен — весь разбор в шагах",
        val actionText: String? = null,
        override val caption: String = "",
    ) : Visualization

    /** Pro-функция для бесплатного пользователя: размытый чертёж и CTA (макет 4c). */
    data class LockedPro(
        val preview: List<FigurePrimitive>,
        val title: String,
        val subtitle: String,
        override val caption: String = "",
    ) : Visualization
}

/** Идентификатор элемента визуализации: шаг решения перечисляет, что уже показано. */
@JvmInline
value class MarkId(val value: String)

/** Стандартные отметки графика функции — их открывают шаги решения квадратного уравнения. */
object PlotMarks {
    val Roots = MarkId("plot.roots")
    val Vertex = MarkId("plot.vertex")
    val Curve = MarkId("plot.curve")
}

/** Точка в нормализованных координатах: 0..1 по ширине и высоте области рисования. */
data class NormPoint(val x: Float, val y: Float)

/** Роль примитива — из неё UI берёт цвет, если он не задан явно. */
enum class FigureRole {
    /** Основная фигура/тело — акцент. */
    Primary,

    /** Вспомогательное построение: высота, медиана — жёлтый пунктир. */
    Auxiliary,

    /** Отметка-подтверждение: прямой угол, равенство — мятный. */
    Check,

    /** Опорная поверхность: наклонная плоскость, стена — серый контур. */
    Support,

    /** Вектор силы тяжести. */
    VectorWeight,

    /** Вектор трения. */
    VectorFriction,

    /** Вектор нормальной реакции. */
    VectorNormal,
}

/**
 * Примитив чертежа. Набор намеренно маленький: отрезок, многоугольник, вектор,
 * прямой угол, точка, повёрнутый прямоугольник и подпись — этого хватает и геометрии, и механике.
 */
sealed interface FigurePrimitive {
    val id: MarkId
    val role: FigureRole

    data class Polygon(
        override val id: MarkId,
        val points: List<NormPoint>,
        override val role: FigureRole = FigureRole.Primary,
        val filled: Boolean = true,
        val closed: Boolean = true,
    ) : FigurePrimitive

    data class Segment(
        override val id: MarkId,
        val from: NormPoint,
        val to: NormPoint,
        override val role: FigureRole = FigureRole.Auxiliary,
        val dashed: Boolean = false,
    ) : FigurePrimitive

    /** Отрезок со стрелкой на конце — вектор силы. */
    data class Vector(
        override val id: MarkId,
        val from: NormPoint,
        val to: NormPoint,
        override val role: FigureRole = FigureRole.VectorWeight,
    ) : FigurePrimitive

    /** Квадратик прямого угла: [corner] — вершина, [along] и [across] задают стороны. */
    data class RightAngle(
        override val id: MarkId,
        val corner: NormPoint,
        val along: NormPoint,
        val across: NormPoint,
        override val role: FigureRole = FigureRole.Check,
    ) : FigurePrimitive

    data class Dot(
        override val id: MarkId,
        val at: NormPoint,
        override val role: FigureRole = FigureRole.Auxiliary,
    ) : FigurePrimitive

    /** Тело на схеме сил: прямоугольник с поворотом вокруг своего центра. */
    data class Body(
        override val id: MarkId,
        val center: NormPoint,
        val width: Float,
        val height: Float,
        val rotationDegrees: Float = 0f,
        override val role: FigureRole = FigureRole.Primary,
    ) : FigurePrimitive

    /** Подпись: вершина «C», длина «6», вектор «mg». */
    data class Label(
        override val id: MarkId,
        val at: NormPoint,
        val text: String,
        override val role: FigureRole = FigureRole.Primary,
        val emphasized: Boolean = false,
    ) : FigurePrimitive
}
