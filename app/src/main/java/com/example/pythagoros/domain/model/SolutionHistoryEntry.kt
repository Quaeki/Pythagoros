package com.example.pythagoros.domain.model

data class SolutionHistoryEntry(
    val id: Long,
    val createdAtMillis: Long,
    val recognizedText: String,
    val imagePath: String?,
    val solution: Solution,
) {
    val expression: String get() = solution.expression.source
    val result: String get() = solution.answer
    val problemType: ProblemType get() = solution.problemType
}
