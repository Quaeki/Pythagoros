package com.example.pythagoros.data.ai

import com.example.pythagoros.BuildConfig
import com.example.pythagoros.domain.ai.PremiumAiGraph
import com.example.pythagoros.domain.ai.PremiumAiPoint
import com.example.pythagoros.domain.ai.PremiumAiPrimitive
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiSolver
import com.example.pythagoros.domain.ai.PremiumAiViewport
import com.example.pythagoros.domain.model.SolutionStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BackendPremiumAiSolver(
    private val baseUrl: String = BuildConfig.PYTHAGOROS_BACKEND_URL,
    private val token: String = BuildConfig.PYTHAGOROS_BACKEND_TOKEN,
) : PremiumAiSolver {
    override suspend fun solve(request: PremiumAiSolveRequest): PremiumAiSolveResult =
        withContext(Dispatchers.IO) {
            if (baseUrl.isBlank() || token.isBlank()) {
                return@withContext PremiumAiSolveResult.Failure(
                    "Backend не настроен: проверьте pythagoros.backend.url и pythagoros.backend.token",
                )
            }

            runCatching {
                val connection = (URL("${baseUrl.trimEnd('/')}/v1/solve/ai").openConnection() as HttpURLConnection)
                connection.requestMethod = "POST"
                connection.connectTimeout = 12_000
                connection.readTimeout = 75_000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")

                val body = JSONObject()
                    .put("expression", request.expression.source)
                    .put("problemType", request.problemType.name)
                    .put("locale", "ru")
                    .put("requireGraph", request.problemType.hasPlot || request.problemType.requiresPremium)
                    .put("localSteps", JSONArray())

                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray(Charsets.UTF_8))
                }

                val responseText = connection.responseText()
                if (connection.responseCode !in 200..299) {
                    return@withContext PremiumAiSolveResult.Failure(responseText.toApiErrorMessage())
                }

                responseText.toPremiumResult()
            }.getOrElse { error ->
                PremiumAiSolveResult.Failure(error.message ?: "AI backend недоступен")
            }
        }
}

private fun HttpURLConnection.responseText(): String {
    val stream = if (responseCode in 200..299) inputStream else errorStream ?: inputStream
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
}

private fun String.toPremiumResult(): PremiumAiSolveResult {
    val json = JSONObject(this)
    val steps = json.optJSONArray("steps").orEmptyObjects().map { item ->
        SolutionStep(
            title = item.optString("title").ifBlank { "Шаг" },
            formula = item.optString("formula"),
            explanation = item.optString("explanation"),
        )
    }

    return PremiumAiSolveResult.Success(
        answer = json.optString("answer"),
        steps = steps,
        graph = json.optJSONObject("graph")?.let { graph ->
            PremiumAiGraph(
                title = graph.optString("title").ifBlank { "График" },
                variable = graph.optString("variable").ifBlank { "x" },
                expression = graph.optString("expression"),
                notes = graph.optJSONArray("notes").orEmptyStrings(),
                kind = graph.optString("kind").takeIf { it.isNotBlank() },
                viewport = graph.optJSONObject("viewport")?.toAiViewport(),
                primitives = graph.optJSONArray("primitives").orEmptyObjects().mapNotNull { item ->
                    item.toAiPrimitiveOrNull()
                },
            )
        },
    )
}

private fun JSONObject.toAiViewport(): PremiumAiViewport =
    PremiumAiViewport(
        xMin = optNullableDouble("xMin"),
        xMax = optNullableDouble("xMax"),
        yMin = optNullableDouble("yMin"),
        yMax = optNullableDouble("yMax"),
    )

private fun JSONObject.toAiPrimitiveOrNull(): PremiumAiPrimitive? {
    val type = optString("type").takeIf { it.isNotBlank() } ?: return null
    return PremiumAiPrimitive(
        type = type,
        id = optString("id").takeIf { it.isNotBlank() },
        role = optString("role").takeIf { it.isNotBlank() },
        points = optJSONArray("points").orEmptyObjects().mapNotNull { it.toAiPointOrNull() },
        from = optJSONObject("from")?.toAiPointOrNull(),
        to = optJSONObject("to")?.toAiPointOrNull(),
        center = optJSONObject("center")?.toAiPointOrNull(),
        radius = optNullableDouble("radius"),
        startAngle = optNullableDouble("startAngle"),
        endAngle = optNullableDouble("endAngle"),
        at = optJSONObject("at")?.toAiPointOrNull(),
        text = optString("text").takeIf { it.isNotBlank() },
        label = optString("label").takeIf { it.isNotBlank() },
        dashed = optBoolean("dashed", false),
        filled = optBoolean("filled", false),
        closed = if (has("closed")) optBoolean("closed") else null,
    )
}

private fun JSONObject.toAiPointOrNull(): PremiumAiPoint? {
    if (!has("x") || !has("y")) return null
    return PremiumAiPoint(
        x = optDouble("x"),
        y = optDouble("y"),
    )
}

private fun JSONObject.optNullableDouble(name: String): Double? =
    if (has(name) && !isNull(name)) optDouble(name) else null

private fun JSONArray?.orEmptyObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(::add)
        }
    }
}

private fun JSONArray?.orEmptyStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}

private fun String.toApiErrorMessage(): String =
    runCatching {
        val json = JSONObject(this)
        json.optString("message").ifBlank {
            json.optString("code").ifBlank { "AI backend вернул ошибку" }
        }
    }.getOrDefault("AI backend вернул ошибку")
