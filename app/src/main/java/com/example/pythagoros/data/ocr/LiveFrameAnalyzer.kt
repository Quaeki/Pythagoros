package com.example.pythagoros.data.ocr

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** Что видно в кадре прямо сейчас — для подсказок сканера (макет 6c). */
data class FrameInsight(
    val hasText: Boolean = false,
    val recognizedText: String = "",
    val problemType: ProblemType = ProblemType.Unknown,
)

/**
 * Живой разбор кадра для подсказок сканера.
 *
 * Работает локально на ML Kit и только подсказывает: платное распознавание
 * запускается уже после нажатия затвора, по одному снимку.
 */
class LiveFrameAnalyzer(
    private val onInsight: (FrameInsight) -> Unit,
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val classifyProblem = ClassifyProblemUseCase()
    private var lastRunAt = 0L

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val now = System.currentTimeMillis()
        // Классификация не нужна на каждом кадре: раз в 700 мс глазу хватает,
        // а батарее заметно легче.
        if (now - lastRunAt < AnalysisIntervalMillis) {
            image.close()
            return
        }
        lastRunAt = now

        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }

        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                val text = result.text.replace(Regex("\\s+"), " ").trim()
                onInsight(
                    FrameInsight(
                        hasText = text.length >= MinTextLength,
                        recognizedText = text,
                        problemType = if (text.isBlank()) {
                            ProblemType.Unknown
                        } else {
                            classifyProblem(text)
                        },
                    )
                )
            }
            .addOnCompleteListener { image.close() }
    }

    private companion object {
        const val AnalysisIntervalMillis = 700L

        /** Короче — это шум: пара засечек на столе, а не условие задачи. */
        const val MinTextLength = 8
    }
}
