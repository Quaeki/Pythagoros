package com.example.pythagoros.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiSolver
import com.example.pythagoros.domain.math.MathState
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ImageSource
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.example.pythagoros.domain.usecase.ParseExpressionUseCase
import com.example.pythagoros.domain.usecase.RecognizeProblemUseCase
import com.example.pythagoros.domain.usecase.SolveProblemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SolverViewModel @Inject constructor(
    private val parseExpression: ParseExpressionUseCase,
    private val classifyProblem: ClassifyProblemUseCase,
    private val recognizeProblem: RecognizeProblemUseCase,
    private val solveProblem: SolveProblemUseCase,
    private val premiumAiSolver: PremiumAiSolver,
) : ViewModel() {

    var pendingImagePath by mutableStateOf<String?>(null)
        private set

    var recognizedExpression by mutableStateOf("y = x² − 4x + 3")
        private set

    var inputState by mutableStateOf(MathState())
        private set

    var pendingAiExpression by mutableStateOf<Expression?>(null)
        private set

    var pendingAiProblemType by mutableStateOf(ProblemType.Unknown)
        private set

    var aiLoading by mutableStateOf(false)
        private set

    var aiError by mutableStateOf<String?>(null)
        private set

    fun updatePendingImage(path: String?) {
        pendingImagePath = path
    }

    fun updateRecognizedExpression(expression: String) {
        recognizedExpression = expression
    }

    fun updateInputState(state: MathState) {
        inputState = state
    }

    fun resetInputState() {
        inputState = MathState()
    }

    fun prepareAiFallback(expression: Expression, type: ProblemType) {
        pendingAiExpression = expression
        pendingAiProblemType = type
        aiError = null
        aiLoading = false
    }

    fun updateAiLoading(loading: Boolean) {
        aiLoading = loading
    }

    fun updateAiError(message: String?) {
        aiError = message
    }

    fun parse(raw: String): Result<Expression> =
        parseExpression(raw)

    fun classify(raw: String): ProblemType =
        classifyProblem(raw)

    fun classify(expression: Expression): ProblemType =
        classifyProblem(expression)

    suspend fun recognizeImage(path: String): RecognitionResult =
        withContext(Dispatchers.IO) {
            recognizeProblem(ImageSource(path, ImageSource.Origin.Camera))
        }

    suspend fun classifyImage(path: String): ProblemType =
        withContext(Dispatchers.IO) {
            recognizeProblem.classify(ImageSource(path, ImageSource.Origin.Camera))
        }

    suspend fun solveLocal(expression: Expression, problemType: ProblemType): SolveResult =
        withContext(Dispatchers.Default) {
            solveProblem(expression, problemType)
        }

    suspend fun solveWithAi(request: PremiumAiSolveRequest): PremiumAiSolveResult =
        premiumAiSolver.solve(request)
}
