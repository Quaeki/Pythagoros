package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.ImageSource
import com.example.pythagoros.domain.model.RecognitionResult

/**
 * Распознаёт условие на снимке. Работает на устройстве — интернет нужен только AI-разбору.
 *
 * Реализация появится вместе с [com.example.pythagoros.domain.repository]-слоем поверх
 * on-device OCR (ML Kit text recognition).
 */
class RecognizeProblemUseCase {

    suspend operator fun invoke(image: ImageSource): RecognitionResult =
        TODO("Подключить OCR: распознать формулу, отметить неуверенные символы, угадать тип задачи")
}
