package com.example.pythagoros.domain.ocr

import com.example.pythagoros.domain.model.ImageSource
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult

interface ProblemRecognizer {
    suspend fun recognize(image: ImageSource): RecognitionResult

    suspend fun classify(image: ImageSource): ProblemType
}
