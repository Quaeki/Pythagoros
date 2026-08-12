package com.example.pythagoros.domain.model

/**
 * Итог распознавания снимка. Неудача — обычный исход, а не исключение:
 * под неё в макете отведён отдельный экран «Условие читается нечётко».
 */
sealed interface RecognitionResult {

    /**
     * @param uncertainRanges участки [Expression.source], в которых ядро не уверено.
     *   Экран «Проверьте условие» подсвечивает их жёлтым и просит проверить.
     */
    data class Success(
        val expression: Expression,
        val guessedType: ProblemType,
        val uncertainRanges: List<IntRange> = emptyList(),
    ) : RecognitionResult

    data class Failure(val reason: Reason) : RecognitionResult

    /** Причина определяет, какие подсказки показать на экране неудачи. */
    enum class Reason {
        /** Снимок смазан или не в фокусе. */
        Blurred,

        /** В кадре не нашлось ни одной формулы. */
        NoFormulaFound,

        /** Задач в кадре несколько — нужно снять по одной. */
        MultipleProblems,
    }
}
