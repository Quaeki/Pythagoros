package com.example.pythagoros.domain.model

/**
 * Тип задачи. Определяет чипы на экране «Проверьте условие», доступность графика
 * и то, требует ли задача платного разбора.
 */
enum class ProblemType(
    val title: String,
    /** Можно ли построить график — от этого зависит чип «Есть график». */
    val hasPlot: Boolean,
    /** Типы, которые локальное ядро не разбирает: они помечены значком Pro. */
    val requiresPremium: Boolean = false,
) {
    LinearEquation("Линейное уравнение", hasPlot = true),
    QuadraticEquation("Квадратное уравнение", hasPlot = true),
    EquationSystem("Система уравнений", hasPlot = true),
    Derivative("Производная", hasPlot = true),
    Integral("Интеграл", hasPlot = true),
    Limit("Предел", hasPlot = false),
    WordProblem("Текстовая задача", hasPlot = false, requiresPremium = true),
    Geometry("Геометрия", hasPlot = false, requiresPremium = true),
    Physics("Физика", hasPlot = false, requiresPremium = true),
    Unknown("Не определён", hasPlot = false),
}
