package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.ImageSource
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.domain.ocr.ProblemRecognizer
import javax.inject.Inject

/**
 * Распознаёт условие на снимке. Работает на устройстве — интернет нужен только AI-разбору.
 */
class RecognizeProblemUseCase @Inject constructor(
    private val problemRecognizer: ProblemRecognizer,
) {

    suspend operator fun invoke(image: ImageSource): RecognitionResult =
        problemRecognizer.recognize(image)

    suspend fun classify(image: ImageSource): ProblemType =
        problemRecognizer.classify(image)
}
