package com.example.pythagoros.data.history

import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.GraphPoint
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.domain.model.SolutionStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.inject.Inject

class HistoryRepository @Inject constructor(
    private val dao: HistoryDao,
) {
    fun observeAll(): Flow<List<SolutionHistoryEntry>> =
        dao.observeAll().map { entities -> entities.mapNotNull { it.toDomainOrNull() } }

    suspend fun save(entry: SolutionHistoryEntry) {
        dao.upsert(entry.toEntity())
    }
}

private fun SolutionHistoryEntry.toEntity(): HistoryEntity =
    HistoryEntity(
        id = id,
        createdAtMillis = createdAtMillis,
        recognizedText = recognizedText,
        imagePath = imagePath,
        expression = solution.expression.source,
        problemType = solution.problemType.name,
        answer = solution.answer,
        steps = solution.steps.encodeSteps(),
        graphTitle = solution.graph?.title,
        graphVariable = solution.graph?.variable,
        graphCoefficients = solution.graph?.coefficients?.entries
            ?.joinToString(";") { "${it.key}:${it.value}" },
        graphRoots = solution.graph?.roots?.joinToString(";"),
        graphVertexX = solution.graph?.vertex?.x,
        graphVertexY = solution.graph?.vertex?.y,
        graphVertexLabel = solution.graph?.vertex?.label,
    )

private fun HistoryEntity.toDomainOrNull(): SolutionHistoryEntry? {
    val type = runCatching { ProblemType.valueOf(problemType) }.getOrNull() ?: return null
    val graph = if (graphTitle != null && graphVariable != null && graphCoefficients != null) {
        PolynomialGraph(
            title = graphTitle,
            variable = graphVariable,
            coefficients = graphCoefficients.decodeCoefficients(),
            roots = graphRoots?.decodeDoubleList().orEmpty(),
            vertex = if (graphVertexX != null && graphVertexY != null) {
                GraphPoint(graphVertexX, graphVertexY, graphVertexLabel)
            } else {
                null
            },
        )
    } else {
        null
    }

    val solution = Solution(
        expression = Expression(expression),
        problemType = type,
        answer = answer,
        steps = steps.decodeSteps(),
        graph = graph,
    )

    return SolutionHistoryEntry(
        id = id,
        createdAtMillis = createdAtMillis,
        recognizedText = recognizedText,
        imagePath = imagePath,
        solution = solution,
    )
}

private fun List<SolutionStep>.encodeSteps(): String =
    joinToString("\n") { step ->
        listOf(step.title, step.formula, step.explanation)
            .joinToString("|") { it.base64() }
    }

private fun String.decodeSteps(): List<SolutionStep> =
    lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size != 3) return@mapNotNull null
            SolutionStep(
                title = parts[0].unbase64(),
                formula = parts[1].unbase64(),
                explanation = parts[2].unbase64(),
            )
        }
        .toList()

private fun String.decodeCoefficients(): Map<Int, Double> =
    split(";")
        .filter { it.isNotBlank() }
        .mapNotNull { item ->
            val parts = item.split(":")
            if (parts.size != 2) return@mapNotNull null
            val power = parts[0].toIntOrNull() ?: return@mapNotNull null
            val value = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            power to value
        }
        .toMap()

private fun String.decodeDoubleList(): List<Double> =
    split(";").mapNotNull { it.toDoubleOrNull() }

private fun String.base64(): String =
    Base64.getEncoder().encodeToString(toByteArray(Charsets.UTF_8))

private fun String.unbase64(): String =
    String(Base64.getDecoder().decode(this), Charsets.UTF_8)
