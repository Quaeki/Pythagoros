package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType

/**
 * Определяет тип задачи по выражению — от него зависят чипы на экране «Проверьте условие»,
 * доступность графика и значок Pro.
 *
 * Возвращает [ProblemType.Unknown], если тип распознать не удалось: пользователь сможет
 * задать его вручную через чип «Изменить тип».
 */
class ClassifyProblemUseCase {

    operator fun invoke(expression: Expression): ProblemType =
        classify(expression.source)

    operator fun invoke(rawText: String): ProblemType =
        classify(rawText)

    private fun classify(rawText: String): ProblemType {
        val source = rawText
            .lowercase()
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('²', '2')
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            looksLikeLimit(source) -> ProblemType.Limit
            looksLikeIntegral(source) -> ProblemType.Integral
            looksLikeDerivative(source) -> ProblemType.Derivative
            looksLikeGeometry(source) -> ProblemType.Geometry
            looksLikePhysics(source) -> ProblemType.Physics
            looksLikeWordProblem(source) -> ProblemType.WordProblem
            looksLikeSystem(source) -> ProblemType.EquationSystem
            looksLikeQuadratic(source) -> ProblemType.QuadraticEquation
            looksLikeLinearEquation(source) -> ProblemType.LinearEquation
            else -> ProblemType.Unknown
        }
    }

    private fun looksLikeLimit(source: String): Boolean =
        Regex("""(^|\W)(lim|limit|предел)(\W|$)""").containsMatchIn(source) ||
            Regex("""lim\s*[_({]""").containsMatchIn(source)

    private fun looksLikeIntegral(source: String): Boolean =
        "∫" in source ||
            Regex("""(^|\W)(int|integral|интеграл|первообразн)(\W|$)""").containsMatchIn(source)

    private fun looksLikeDerivative(source: String): Boolean =
        Regex("""(^|\W)(derivative|производн|дифференц)(\W|$)""").containsMatchIn(source) ||
            Regex("""\bd\s*/\s*d[a-zа-я]""").containsMatchIn(source) ||
            Regex("""[a-zа-я]\s*'\s*(\(|=|$)""").containsMatchIn(source)

    private fun looksLikeQuadratic(source: String): Boolean =
        "=" in source &&
            (
                Regex("""[a-zа-я]\s*\^\s*2""").containsMatchIn(source) ||
                    Regex("""[a-zа-я]\s*2""").containsMatchIn(source)
                )

    private fun looksLikeSystem(source: String): Boolean =
        source.count { it == '=' } > 1 ||
            ";" in source ||
            Regex("""[{\[]\s*[a-zа-я].*=.*[,;]\s*[a-zа-я].*=""").containsMatchIn(source) ||
            Regex("""\n\s*[a-zа-я].*=""").containsMatchIn(source)

    private fun looksLikeWordProblem(source: String): Boolean =
        hasCyrillic(source) &&
            (
                source.split(Regex("\\s+")).size > 10 ||
                    Regex("""\b(найти|сколько|чему|через|если|известно|расстояние|скорость|работа|процент|встретятся)\b""")
                        .containsMatchIn(source)
                )

    private fun looksLikeGeometry(source: String): Boolean =
        Regex("""\b(треугольник\w*|прямоугольн\w*|окружност\w*|гипотенуз\w*|катет\w*|биссектрис\w*|медиан\w*|трапеци\w*|параллелограмм\w*|ромб\w*|радиус\w*|диаметр\w*|хорд\w*|вписан\w*|описан\w*)\b""")
            .containsMatchIn(source) ||
            (
                Regex("""\b(высот\w*|площад\w*|периметр\w*|угол|угла|углы)\b""").containsMatchIn(source) &&
                    Regex("""(°|градус)""").containsMatchIn(source)
                )

    private fun looksLikePhysics(source: String): Boolean =
        Regex("""\b(м/с2|м/с\^2|м/с|кг|ньютон|дж|джоул|вт|ватт|па|паскаль|ом|вольт|ампер|тесла|кулон)\b""")
            .containsMatchIn(source) ||
            Regex("""\b(сила|масса|ускорени[ея]|импульс|энерги[яи]|мощность|напряжение|сопротивление|ток|давление)\b""")
                .containsMatchIn(source)

    private fun looksLikeLinearEquation(source: String): Boolean =
        "=" in source &&
            Regex("""[a-zа-я]""").containsMatchIn(source) &&
            !looksLikeQuadratic(source)

    private fun hasCyrillic(source: String): Boolean =
        source.any { it in 'а'..'я' || it == 'ё' }
}
