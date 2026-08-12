package com.example.pythagoros.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.example.pythagoros.data.auth.BackendAuthClient
import com.example.pythagoros.data.auth.AuthProviderSignInResult
import com.example.pythagoros.data.auth.AuthRequestCodeResult
import com.example.pythagoros.data.auth.AuthVerifyCodeResult
import com.example.pythagoros.data.auth.GoogleFirebaseAuthClient
import com.example.pythagoros.data.auth.GoogleFirebaseSignInResult
import com.example.pythagoros.data.auth.ProviderIdentity
import com.example.pythagoros.data.history.HistoryRepository
import com.example.pythagoros.data.prefs.AppPreferences
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.ai.PremiumAiSolver
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.example.pythagoros.domain.usecase.ParseExpressionUseCase
import com.example.pythagoros.domain.usecase.SolveProblemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PythagorosViewModel @Inject constructor(
    val prefs: AppPreferences,
    private val parseExpression: ParseExpressionUseCase,
    private val classifyProblem: ClassifyProblemUseCase,
    private val solveProblem: SolveProblemUseCase,
    private val premiumAiSolver: PremiumAiSolver,
    private val authClient: BackendAuthClient,
    private val googleAuthClient: GoogleFirebaseAuthClient,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val historyEntries: Flow<List<SolutionHistoryEntry>> =
        historyRepository.observeAll()

    suspend fun saveHistoryEntry(entry: SolutionHistoryEntry) {
        historyRepository.save(entry)
    }

    fun parse(raw: String): Result<Expression> =
        parseExpression(raw)

    fun classify(raw: String): ProblemType =
        classifyProblem(raw)

    fun classify(expression: Expression): ProblemType =
        classifyProblem(expression)

    suspend fun solveLocal(expression: Expression, problemType: ProblemType): SolveResult =
        withContext(Dispatchers.Default) {
            solveProblem(expression, problemType)
        }

    suspend fun solveWithAi(request: PremiumAiSolveRequest): PremiumAiSolveResult =
        premiumAiSolver.solve(request)

    suspend fun requestCode(phone: String): AuthRequestCodeResult =
        authClient.requestCode(phone)

    suspend fun verifyCode(requestId: String, code: String): AuthVerifyCodeResult =
        authClient.verifyCode(requestId, code)

    suspend fun signInWithProvider(identity: ProviderIdentity): AuthProviderSignInResult =
        authClient.signInWithProvider(identity)

    suspend fun signInWithGoogle(): GoogleFirebaseSignInResult =
        googleAuthClient.signIn()
}
