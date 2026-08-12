package com.example.pythagoros.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pythagoros.BuildConfig
import com.example.pythagoros.data.ai.toMappedSolution
import com.example.pythagoros.data.auth.AuthProviderSignInResult
import com.example.pythagoros.data.auth.ProviderIdentity
import com.example.pythagoros.data.billing.PlayBillingClient
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.model.Visualization
import com.example.pythagoros.presentation.components.BottomTab
import com.example.pythagoros.presentation.components.NavDirection
import com.example.pythagoros.presentation.components.screenTransition
import com.example.pythagoros.presentation.navigation.OpenSolution
import com.example.pythagoros.presentation.navigation.Overlay
import com.example.pythagoros.presentation.navigation.Stage
import com.example.pythagoros.presentation.viewmodel.AuthViewModel
import com.example.pythagoros.presentation.viewmodel.HistoryViewModel
import com.example.pythagoros.presentation.viewmodel.NavigationViewModel
import com.example.pythagoros.presentation.viewmodel.SolverViewModel
import com.example.pythagoros.presentation.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.launch
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk

@Composable
fun PythagorosApp(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel,
    solverViewModel: SolverViewModel,
    historyViewModel: HistoryViewModel,
    subscriptionViewModel: SubscriptionViewModel,
    navigationViewModel: NavigationViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = authViewModel.prefs

    val stage = navigationViewModel.stage
    val tab = navigationViewModel.tab
    val overlays = navigationViewModel.overlays
    val navDirection = navigationViewModel.navDirection

    val isPro = subscriptionViewModel.isPro
    val subscriptionPlans = subscriptionViewModel.plans
    val purchaseLoading = subscriptionViewModel.purchaseLoading
    val purchaseError = subscriptionViewModel.purchaseError
    val phone = authViewModel.phone
    val authDebugCode = authViewModel.debugCode
    val authLoading = authViewModel.isLoading
    val authError = authViewModel.errorMessage
    val pendingImagePath = solverViewModel.pendingImagePath
    val recognizedExpression = solverViewModel.recognizedExpression
    val inputState = solverViewModel.inputState

    val open = navigationViewModel.selectedSolution
    val currentStep = navigationViewModel.currentStep
    val detailOpen = navigationViewModel.detailOpen
    val pendingAiExpression = solverViewModel.pendingAiExpression
    val pendingAiProblemType = solverViewModel.pendingAiProblemType
    val aiLoading = solverViewModel.aiLoading
    val aiError = solverViewModel.aiError

    val savedEntries by historyViewModel.historyEntries.collectAsStateWithLifecycle(emptyList())

    // Свои разборы идут первыми, следом — примеры из макета: по ним открываются
    // геометрия, механика и задача без графика, пока решатель их сам не выдаёт.
    val historyEntries = remember(savedEntries) { savedEntries + DemoHistory }

    fun push(overlay: Overlay) = navigationViewModel.push(overlay)
    fun pop() = navigationViewModel.pop()
    fun closeAll() = navigationViewModel.closeAll()
    fun switchTo(target: BottomTab) = navigationViewModel.switchTo(target)
    fun goTo(target: Stage, direction: NavDirection = NavDirection.Forward) =
        navigationViewModel.goTo(target, direction)
    fun openSolution(target: OpenSolution) =
        navigationViewModel.showSolution(target, isPro)

    val playBillingClient = remember(context) {
        PlayBillingClient(
            context = context,
            onEntitlementChanged = { active, _ ->
                subscriptionViewModel.updatePro(active)
                subscriptionViewModel.updatePurchaseLoading(false)
                if (active && overlays.lastOrNull() == Overlay.Paywall) {
                    closeAll()
                }
            },
            onPurchaseMessage = { message ->
                subscriptionViewModel.updatePurchaseLoading(false)
                subscriptionViewModel.updatePurchaseError(message)
            },
        )
    }

    DisposableEffect(playBillingClient) {
        playBillingClient.start(
            onReady = {
                playBillingClient.queryProductPrices { prices ->
                    subscriptionViewModel.setPlansFromBilling(prices)
                }
                playBillingClient.restorePurchases()
            },
            onError = { message ->
                subscriptionViewModel.updatePurchaseError(message)
            },
        )
        onDispose {
            playBillingClient.endConnection()
        }
    }

    fun openGooglePlaySubscriptionCenter() {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=${context.packageName}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        runCatching { context.startActivity(intent) }
            .onFailure { subscriptionViewModel.updatePurchaseError("Не удалось открыть управление подпиской Google Play") }
    }

    fun finishProviderSignIn(identity: ProviderIdentity) {
        scope.launch {
            when (val result = authViewModel.signInWithProvider(identity)) {
                is AuthProviderSignInResult.Success -> goTo(Stage.LevelGoal)
                is AuthProviderSignInResult.Failure -> Unit
            }
        }
    }

    var startYandexSignIn: () -> Unit = {
        authViewModel.finishExternalSignInFailure(
            "Добавь yandex.client.id в local.properties и зарегистрируй Android-приложение в Yandex OAuth"
        )
    }
    if (BuildConfig.YANDEX_CLIENT_ID.isNotBlank()) {
        val yandexSdk = remember(context) { YandexAuthSdk.create(YandexAuthOptions(context)) }
        val yandexLauncher = rememberLauncherForActivityResult(yandexSdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    val token = result.token
                    val jwt = runCatching { yandexSdk.getJwt(token) }.getOrNull()
                    finishProviderSignIn(
                        ProviderIdentity(
                            provider = "yandex",
                            idToken = jwt,
                            accessToken = token.value,
                        )
                    )
                }

                is YandexAuthResult.Failure -> {
                    authViewModel.finishExternalSignInFailure(
                        result.exception.message ?: "Не удалось войти через Яндекс"
                    )
                }

                YandexAuthResult.Cancelled -> {
                    authViewModel.cancelExternalSignIn()
                }
            }
        }
        startYandexSignIn = {
            if (!authLoading) {
                authViewModel.beginExternalSignIn()
                yandexLauncher.launch(YandexAuthLoginOptions())
            }
        }
    }

    suspend fun saveSolution(solution: Solution) {
        val duplicate = savedEntries.firstOrNull()?.let {
            it.expression == solution.expression.source && it.result == solution.answer
        } == true
        if (duplicate) return
        val now = System.currentTimeMillis()
        historyViewModel.saveHistoryEntry(
            SolutionHistoryEntry(
                id = now,
                createdAtMillis = now,
                recognizedText = recognizedExpression,
                imagePath = pendingImagePath,
                solution = solution,
            )
        )
    }

    fun openAiFallback(expression: Expression, type: ProblemType) {
        solverViewModel.prepareAiFallback(expression, type)
        push(Overlay.AiFallback)
    }

    fun solveWithAi() {
        if (!isPro) {
            push(Overlay.Paywall)
            return
        }
        val expression = pendingAiExpression ?: Expression(recognizedExpression.trim())
        val type = pendingAiProblemType
        if (aiLoading) return

        scope.launch {
            solverViewModel.updateAiLoading(true)
            solverViewModel.updateAiError(null)
            when (
                val result = solverViewModel.solveWithAi(
                    PremiumAiSolveRequest(
                        expression = expression,
                        problemType = type,
                        imagePath = pendingImagePath,
                    )
                )
            ) {
                is PremiumAiSolveResult.Success -> {
                    val mapped = result.toMappedSolution(
                        originalExpression = expression,
                        problemType = type,
                    )
                    saveSolution(mapped.solution)
                    pop()
                    openSolution(
                        OpenSolution(
                            title = if (type == ProblemType.Unknown) "AI-разбор" else "${type.title} · AI",
                            solution = mapped.solution,
                            visualization = mapped.visualization,
                            conditionIsFormula = expression.source.isFormulaLike(),
                            isPro = true,
                            xp = "+20 XP",
                        )
                    )
                }

                is PremiumAiSolveResult.Failure -> {
                    solverViewModel.updateAiError(result.message)
                }
            }
            solverViewModel.updateAiLoading(false)
        }
    }

    /**
     * Общий путь к решению: и правка распознанного условия, и ручной ввод
     * приходят сюда, чтобы ветка «локальное ядро не берёт» была одна на оба входа.
     */
    suspend fun solveAndOpen(source: String) {
        val expression = solverViewModel.parse(source).getOrNull()
        if (expression == null) {
            openAiFallback(Expression(source.trim()), solverViewModel.classify(source))
            return
        }
        val type = solverViewModel.classify(expression)
        when (val result = solverViewModel.solveLocal(expression, type)) {
            is SolveResult.Success -> {
                val solution = result.solution
                saveSolution(solution)
                pop()
                openSolution(
                    OpenSolution(
                        title = type.title,
                        solution = solution,
                        visualization = solution.graph
                            ?.let { Visualization.Plot2D(it) }
                            ?: Visualization.NotNeeded(),
                    )
                )
            }

            // Локальное ядро не берёт такие задачи — это территория Pro.
            is SolveResult.Unsupported -> openAiFallback(expression, type)
        }
    }

    BackHandler(enabled = overlays.isNotEmpty() || tab != BottomTab.Task) {
        if (overlays.isNotEmpty()) {
            pop()
        } else {
            switchTo(BottomTab.Task)
        }
    }

    val screen: Any = when {
        stage != Stage.Main -> stage
        overlays.isNotEmpty() -> overlays.last()
        else -> tab
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = { screenTransition(navDirection) },
        label = "screen",
        modifier = modifier,
    ) { state ->
        when (state) {
            is Stage -> FirstLaunchRoute(
                stage = state,
                prefs = prefs,
                authViewModel = authViewModel,
                phone = phone,
                debugCode = authDebugCode,
                isLoading = authLoading,
                errorMessage = authError,
                scope = scope,
                onYandex = startYandexSignIn,
                onProviderSignedIn = ::finishProviderSignIn,
                goTo = ::goTo,
            )

            is BottomTab -> MainTabRoute(
                tab = state,
                prefs = prefs,
                isPro = isPro,
                solvedCount = savedEntries.size,
                plotsCount = savedEntries.count { it.solution.graph != null },
                historyEntries = historyEntries,
                onOpenEntry = { entry -> openSolution(entry.toOpenSolution(isPro)) },
                onUpdatePendingImage = solverViewModel::updatePendingImage,
                onResetInput = solverViewModel::resetInputState,
                classifyImage = solverViewModel::classifyImage,
                switchTo = ::switchTo,
                push = ::push,
                goTo = ::goTo,
            )

            is Overlay -> when (state) {
                Overlay.Recognizing,
                Overlay.RecognitionFailed,
                Overlay.Input,
                Overlay.Verify -> CaptureRoute(
                    overlay = state,
                    solverViewModel = solverViewModel,
                    pendingImagePath = pendingImagePath,
                    recognizedExpression = recognizedExpression,
                    inputState = inputState,
                    isPro = isPro,
                    scope = scope,
                    pop = ::pop,
                    push = ::push,
                    solveAndOpen = ::solveAndOpen,
                )

                Overlay.AiFallback,
                Overlay.Solution,
                Overlay.GraphHero,
                Overlay.LockedSolution -> SolutionRoute(
                    overlay = state,
                    open = open,
                    currentStep = currentStep,
                    detailOpen = detailOpen,
                    pendingAiExpression = pendingAiExpression,
                    isPro = isPro,
                    aiLoading = aiLoading,
                    aiError = aiError,
                    navigationViewModel = navigationViewModel,
                    pop = ::pop,
                    push = ::push,
                    solveWithAi = ::solveWithAi,
                )

                Overlay.Paywall,
                Overlay.Subscription -> SubscriptionRoute(
                    overlay = state,
                    context = context,
                    isPro = isPro,
                    plans = subscriptionPlans,
                    purchaseLoading = purchaseLoading,
                    purchaseError = purchaseError,
                    subscriptionViewModel = subscriptionViewModel,
                    playBillingClient = playBillingClient,
                    pop = ::pop,
                    push = ::push,
                    openGooglePlaySubscriptionCenter = ::openGooglePlaySubscriptionCenter,
                )
            }
        }
    }
}

/**
 * Подсказки под полем ввода: только то, что действительно достраивает условие.
 * Уравнение без правой части — самая частая недописанная запись.
 */
internal fun suggestionsFor(typed: String): List<String> = buildList {
    val isFormula = typed.none { it in 'а'..'я' || it in 'А'..'Я' }
    if (typed.isNotBlank() && isFormula && "=" !in typed) add("= 0")
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Разборы из макета, которые лежат в истории, пока пользователь не решил ничего своего. */
private val DemoHistory: List<SolutionHistoryEntry> by lazy {
    val day = 24L * 60 * 60 * 1000
    val now = System.currentTimeMillis()
    listOf(
        SampleProblems.quadratic to now,
        SampleProblems.integral to now - day,
        SampleProblems.geometry to now - 2 * day,
        SampleProblems.physics to now - 3 * day,
    ).mapIndexed { index, (solution, createdAt) ->
        SolutionHistoryEntry(
            id = -(index + 1).toLong(),
            createdAtMillis = createdAt,
            recognizedText = solution.expression.source,
            imagePath = null,
            solution = solution,
        )
    }
}

/** Какой слот визуализации открыть для записи истории. */
internal fun SolutionHistoryEntry.toOpenSolution(isPro: Boolean): OpenSolution = when (solution) {
    SampleProblems.geometry -> OpenSolution(
        title = "Геометрия",
        solution = solution,
        visualization = if (isPro) SampleProblems.geometryVisualization else SampleProblems.geometryLocked,
        conditionIsFormula = false,
        isPro = true,
        xp = null,
    )

    SampleProblems.physics -> OpenSolution(
        title = "Механика",
        solution = solution,
        visualization = if (isPro) SampleProblems.physicsVisualization else SampleProblems.physicsLocked,
        conditionIsFormula = false,
        isPro = true,
        xp = null,
    )

    SampleProblems.integral -> OpenSolution(
        title = "Неопределённый интеграл",
        solution = solution,
        visualization = SampleProblems.integralVisualization,
        xp = "+10 XP",
    )

    else -> OpenSolution(
        title = "Разбор задачи",
        solution = solution,
        visualization = solution.graph?.let { Visualization.Plot2D(it) } ?: Visualization.NotNeeded(),
        conditionIsFormula = solution.expression.source.none { it in 'а'..'я' || it in 'А'..'Я' },
    )
}

private fun String.isFormulaLike(): Boolean =
    none { it in 'а'..'я' || it in 'А'..'Я' }
