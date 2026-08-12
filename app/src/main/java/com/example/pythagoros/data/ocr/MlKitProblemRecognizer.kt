package com.example.pythagoros.data.ocr

import android.content.Context
import android.net.Uri
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ImageSource
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.domain.ocr.ProblemRecognizer
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.example.pythagoros.domain.usecase.ParseExpressionUseCase
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MlKitProblemRecognizer(
    private val context: Context,
) : ProblemRecognizer {
    private val parseExpression = ParseExpressionUseCase()
    private val classifyProblem = ClassifyProblemUseCase()

    override suspend fun recognize(image: ImageSource): RecognitionResult {
        val rawText = recognizeText(image.reference)

        if (rawText.isBlank()) {
            return RecognitionResult.Failure(RecognitionResult.Reason.NoFormulaFound)
        }

        val guessedType = classifyProblem(rawText)
        val expression = parseExpression(rawText).getOrElse {
            if (guessedType != ProblemType.Unknown) {
                Expression(normalizeReadableText(rawText))
            } else {
                return RecognitionResult.Failure(RecognitionResult.Reason.NoFormulaFound)
            }
        }

        if (expression.source.isBlank()) {
            return RecognitionResult.Failure(RecognitionResult.Reason.NoFormulaFound)
        }

        return RecognitionResult.Success(
            expression = expression,
            guessedType = guessedType.takeUnless { it == ProblemType.Unknown } ?: classifyProblem(expression),
            uncertainRanges = emptyList(),
        )
    }

    /**
     * Тип задачи по снимку — считается локально и сразу после затвора (макет 6c):
     * по нему решается, показывать ли Pro-гейт `6b` вместо платного разбора.
     */
    override suspend fun classify(image: ImageSource): ProblemType {
        val rawText = recognizeText(image.reference)
        return if (rawText.isBlank()) ProblemType.Unknown else classifyProblem(rawText)
    }

    private suspend fun recognizeText(imagePath: String): String {
        val image = InputImage.fromFilePath(context, Uri.fromFile(File(imagePath)))
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(image).await().text.trim()
        } finally {
            recognizer.close()
        }
    }

    private fun normalizeReadableText(rawText: String): String =
        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
}

private suspend fun <T> Task<T>.await(): T =
    suspendCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
    }
