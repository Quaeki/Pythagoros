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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.pythagoros.BuildConfig
import com.example.pythagoros.data.ai.BackendPremiumAiSolver
import com.example.pythagoros.data.ai.toMappedSolution
import com.example.pythagoros.data.auth.AuthRequestCodeResult
import com.example.pythagoros.data.auth.AuthProviderSignInResult
import com.example.pythagoros.data.auth.AuthVerifyCodeResult
import com.example.pythagoros.data.auth.BackendAuthClient
import com.example.pythagoros.data.auth.GoogleFirebaseAuthClient
import com.example.pythagoros.data.auth.GoogleFirebaseSignInResult
import com.example.pythagoros.data.auth.ProviderIdentity
import com.example.pythagoros.data.billing.BillingProductPrice
import com.example.pythagoros.data.billing.PlayBillingClient
import com.example.pythagoros.data.history.HistoryRepository
import com.example.pythagoros.data.history.PythagorosDatabase
import com.example.pythagoros.data.prefs.AppPreferences
import com.example.pythagoros.domain.ai.PremiumAiSolveRequest
import com.example.pythagoros.domain.ai.PremiumAiSolveResult
import com.example.pythagoros.domain.math.MathState
import com.example.pythagoros.domain.math.insertText
import com.example.pythagoros.domain.math.mathStateFromText
import com.example.pythagoros.domain.math.toDisplayText
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.domain.model.SolveResult
import com.example.pythagoros.domain.model.Visualization
import com.example.pythagoros.domain.usecase.ClassifyProblemUseCase
import com.example.pythagoros.domain.usecase.ParseExpressionUseCase
import com.example.pythagoros.domain.usecase.SolveProblemUseCase
import com.example.pythagoros.presentation.components.BottomTab
import com.example.pythagoros.presentation.components.NavDirection
import com.example.pythagoros.presentation.components.screenTransition
import com.example.pythagoros.presentation.screens.AiFallbackScreen
import com.example.pythagoros.presentation.screens.DefaultPlans
import com.example.pythagoros.presentation.screens.GraphHeroScreen
import com.example.pythagoros.presentation.screens.HistoryScreen
import com.example.pythagoros.presentation.screens.LanguageScreen
import com.example.pythagoros.presentation.screens.LevelGoalScreen
import com.example.pythagoros.presentation.screens.LockedProSolutionScreen
import com.example.pythagoros.presentation.screens.ManageSubscriptionScreen
import com.example.pythagoros.presentation.screens.MathInputScreen
import com.example.pythagoros.presentation.screens.OnboardingScreen
import com.example.pythagoros.presentation.screens.PaywallScreen
import com.example.pythagoros.presentation.screens.ProfileScreen
import com.example.pythagoros.presentation.screens.RecognitionFailedScreen
import com.example.pythagoros.presentation.screens.RecognizingScreen
import com.example.pythagoros.presentation.screens.ScannerScreen
import com.example.pythagoros.presentation.screens.SignInScreen
import com.example.pythagoros.presentation.screens.SmsCodeScreen
import com.example.pythagoros.presentation.screens.SolutionScreen
import com.example.pythagoros.presentation.screens.SplashScreen
import com.example.pythagoros.presentation.screens.StepDetailSheet
import com.example.pythagoros.presentation.screens.SubscriptionPlan
import com.example.pythagoros.presentation.screens.VerifyScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.yandex.authsdk.YandexAuthLoginOptions
import com.yandex.authsdk.YandexAuthOptions
import com.yandex.authsdk.YandexAuthResult
import com.yandex.authsdk.YandexAuthSdk

/** Экраны первого запуска идут строго по порядку из макета (тур 3). */
private enum class Stage { Splash, Language, Onboarding, SignIn, SmsCode, LevelGoal, Main }

/** Экраны, которые открываются поверх вкладки и закрываются кнопкой «назад». */
private enum class Overlay {
    Recognizing,
    Verify,
    Input,
    RecognitionFailed,
    Solution,
    LockedSolution,
    AiFallback,
    GraphHero,
    Paywall,
    Subscription,
}

/** Что показывает экран решения: разбор и слот визуализации к нему. */
private data class OpenSolution(
    val title: String,
    val solution: Solution,
    val visualization: Visualization?,
    val conditionIsFormula: Boolean = true,
    val isPro: Boolean = false,
    val xp: String? = "+15 XP",
)

@Composable
fun PythagorosApp(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { AppPreferences(context) }
    val historyRepository = remember {
        HistoryRepository(PythagorosDatabase.get(context).historyDao())
    }

    var stage by remember { mutableStateOf(Stage.Splash) }
    var tab by remember { mutableStateOf(BottomTab.Task) }
    val overlays = remember { mutableStateListOf<Overlay>() }

    var isPro by remember { mutableStateOf(prefs.isPro) }
    var subscriptionPlans by remember { mutableStateOf(DefaultPlans) }
    var purchaseLoading by remember { mutableStateOf(false) }
    var purchaseError by remember { mutableStateOf<String?>(null) }
    var phone by remember { mutableStateOf("") }
    var authRequestId by remember { mutableStateOf("") }
    var authDebugCode by remember { mutableStateOf<String?>(null) }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var pendingImagePath by remember { mutableStateOf<String?>(null) }
    var recognizedExpression by remember { mutableStateOf("y = x² − 4x + 3") }
    var inputState by remember { mutableStateOf(MathState()) }

    var open by remember { mutableStateOf<OpenSolution?>(null) }
    var currentStep by remember { mutableIntStateOf(0) }
    var detailOpen by remember { mutableStateOf(false) }
    var pendingAiExpression by remember { mutableStateOf<Expression?>(null) }
    var pendingAiProblemType by remember { mutableStateOf(ProblemType.Unknown) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiError by remember { mutableStateOf<String?>(null) }

    val savedEntries = remember { mutableStateListOf<SolutionHistoryEntry>() }

    val parseExpression = remember { ParseExpressionUseCase() }
    val classifyProblem = remember { ClassifyProblemUseCase() }
    val solveProblem = remember { SolveProblemUseCase() }
    val premiumAiSolver = remember { BackendPremiumAiSolver() }
    val authClient = remember { BackendAuthClient() }
    val googleAuthClient = remember { GoogleFirebaseAuthClient(context) }

    LaunchedEffect(historyRepository) {
        historyRepository.observeAll().collect { entries ->
            savedEntries.clear()
            savedEntries.addAll(entries)
        }
    }

    // Свои разборы идут первыми, следом — примеры из макета: по ним открываются
    // геометрия, механика и задача без графика, пока решатель их сам не выдаёт.
    val historyEntries = savedEntries + DemoHistory

    // Направление последнего перехода: по нему [AnimatedContent] решает, с какой
    // стороны приходит новый экран. Иначе «назад» визуально неотличимо от «вперёд».
    var navDirection by remember { mutableStateOf(NavDirection.Forward) }

    fun push(overlay: Overlay) {
        navDirection = NavDirection.Forward
        overlays.add(overlay)
    }

    fun pop() {
        if (overlays.isEmpty()) return
        navDirection = NavDirection.Back
        overlays.removeAt(overlays.lastIndex)
    }

    fun closeAll() {
        navDirection = NavDirection.Back
        overlays.clear()
    }

    fun switchTo(target: BottomTab) {
        navDirection = NavDirection.Lateral
        overlays.clear()
        tab = target
    }

    /** Шаги первого запуска идут по порядку, поэтому направление задаётся явно. */
    fun goTo(target: Stage, direction: NavDirection = NavDirection.Forward) {
        navDirection = direction
        stage = target
    }

    val playBillingClient = remember(context) {
        PlayBillingClient(
            context = context,
            onEntitlementChanged = { active, _ ->
                isPro = active
                prefs.isPro = active
                purchaseLoading = false
                if (active && overlays.lastOrNull() == Overlay.Paywall) {
                    closeAll()
                }
            },
            onPurchaseMessage = { message ->
                purchaseLoading = false
                purchaseError = message
            },
        )
    }

    DisposableEffect(playBillingClient) {
        playBillingClient.start(
            onReady = {
                playBillingClient.queryProductPrices { prices ->
                    subscriptionPlans = DefaultPlans.withBillingPrices(prices)
                }
                playBillingClient.restorePurchases()
            },
            onError = { message ->
                purchaseError = message
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
            .onFailure { purchaseError = "Не удалось открыть управление подпиской Google Play" }
    }

    fun finishProviderSignIn(identity: ProviderIdentity) {
        if (authLoading) return
        authLoading = true
        authError = null
        scope.launch {
            when (val result = authClient.signInWithProvider(identity)) {
                is AuthProviderSignInResult.Success -> {
                    prefs.userId = result.userId
                    prefs.userPhone = result.phone.orEmpty()
                    prefs.userEmail = result.email.orEmpty()
                    prefs.userDisplayName = result.displayName.orEmpty()
                    prefs.sessionToken = result.sessionToken
                    authLoading = false
                    goTo(Stage.LevelGoal)
                }

                is AuthProviderSignInResult.Failure -> {
                    authLoading = false
                    authError = result.message
                }
            }
        }
    }

    var startYandexSignIn: () -> Unit = {
        authError = "Добавь yandex.client.id в local.properties и зарегистрируй Android-приложение в Yandex OAuth"
    }
    if (BuildConfig.YANDEX_CLIENT_ID.isNotBlank()) {
        val yandexSdk = remember(context) { YandexAuthSdk.create(YandexAuthOptions(context)) }
        val yandexLauncher = rememberLauncherForActivityResult(yandexSdk.contract) { result ->
            when (result) {
                is YandexAuthResult.Success -> {
                    val token = result.token
                    val jwt = runCatching { yandexSdk.getJwt(token) }.getOrNull()
                    authLoading = false
                    finishProviderSignIn(
                        ProviderIdentity(
                            provider = "yandex",
                            idToken = jwt,
                            accessToken = token.value,
                        )
                    )
                }

                is YandexAuthResult.Failure -> {
                    authLoading = false
                    authError = result.exception.message ?: "Не удалось войти через Яндекс"
                }

                YandexAuthResult.Cancelled -> {
                    authLoading = false
                    authError = null
                }
            }
        }
        startYandexSignIn = {
            if (!authLoading) {
                authLoading = true
                authError = null
                yandexLauncher.launch(YandexAuthLoginOptions())
            }
        }
    }

    fun openSolution(target: OpenSolution) {
        open = target
        currentStep = 0
        detailOpen = false
        // Pro-разбор без подписки открывается сразу в закрытом виде (макет 4c).
        push(if (target.isPro && !isPro) Overlay.LockedSolution else Overlay.Solution)
    }

    suspend fun saveSolution(solution: Solution) {
        val duplicate = savedEntries.firstOrNull()?.let {
            it.expression == solution.expression.source && it.result == solution.answer
        } == true
        if (duplicate) return
        val now = System.currentTimeMillis()
        historyRepository.save(
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
        pendingAiExpression = expression
        pendingAiProblemType = type
        aiError = null
        aiLoading = false
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
            aiLoading = true
            aiError = null
            when (
                val result = premiumAiSolver.solve(
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
                    aiError = result.message
                }
            }
            aiLoading = false
        }
    }

    /**
     * Общий путь к решению: и правка распознанного условия, и ручной ввод
     * приходят сюда, чтобы ветка «локальное ядро не берёт» была одна на оба входа.
     */
    suspend fun solveAndOpen(source: String) {
        val expression = parseExpression(source).getOrNull()
        if (expression == null) {
            openAiFallback(Expression(source.trim()), classifyProblem(source))
            return
        }
        val type = classifyProblem(expression)
        when (val result = withContext(Dispatchers.Default) { solveProblem(expression, type) }) {
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
            navDirection = NavDirection.Lateral
            tab = BottomTab.Task
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
            // ── Первый запуск ──
            Stage.Splash -> SplashScreen(
                onLoaded = {
                    goTo(if (prefs.onboardingCompleted) Stage.Main else Stage.Language)
                }
            )

            Stage.Language -> LanguageScreen(
                systemLanguageCode = prefs.languageCode,
                onContinue = { language ->
                    prefs.languageCode = language.code
                    goTo(Stage.Onboarding)
                },
            )

            Stage.Onboarding -> OnboardingScreen(onFinish = { goTo(Stage.SignIn) })

            Stage.SignIn -> SignInScreen(
                isLoading = authLoading,
                errorMessage = authError,
                onBack = { goTo(Stage.Onboarding, NavDirection.Back) },
                onRequestCode = { entered ->
                    phone = entered
                    authError = null
                    authLoading = true
                    scope.launch {
                        when (val result = authClient.requestCode(entered)) {
                            is AuthRequestCodeResult.Success -> {
                                authRequestId = result.requestId
                                authDebugCode = result.debugCode
                                authLoading = false
                                goTo(Stage.SmsCode)
                            }

                            is AuthRequestCodeResult.Failure -> {
                                authLoading = false
                                authError = result.message
                            }
                        }
                    }
                },
                onGoogle = {
                    if (!authLoading) {
                        authLoading = true
                        authError = null
                        scope.launch {
                            when (val result = googleAuthClient.signIn()) {
                                is GoogleFirebaseSignInResult.Success -> {
                                    authLoading = false
                                    finishProviderSignIn(result.identity)
                                }

                                is GoogleFirebaseSignInResult.Failure -> {
                                    authLoading = false
                                    authError = result.message
                                }
                            }
                        }
                    }
                },
                onYandex = startYandexSignIn,
                onEmail = { goTo(Stage.LevelGoal) },
                onSkip = { goTo(Stage.LevelGoal) },
            )

            Stage.SmsCode -> SmsCodeScreen(
                phone = phone,
                isLoading = authLoading,
                errorMessage = authError,
                debugCode = authDebugCode,
                onBack = { goTo(Stage.SignIn, NavDirection.Back) },
                onChangePhone = { goTo(Stage.SignIn, NavDirection.Back) },
                onConfirm = { code ->
                    authError = null
                    authLoading = true
                    scope.launch {
                        when (val result = authClient.verifyCode(authRequestId, code)) {
                            is AuthVerifyCodeResult.Success -> {
                                prefs.userId = result.userId
                                prefs.userPhone = result.phone
                                prefs.sessionToken = result.sessionToken
                                authLoading = false
                                authDebugCode = null
                                goTo(Stage.LevelGoal)
                            }

                            is AuthVerifyCodeResult.Failure -> {
                                authLoading = false
                                authError = result.message
                            }
                        }
                    }
                },
            )

            Stage.LevelGoal -> LevelGoalScreen(
                onDone = { _, _ ->
                    prefs.onboardingCompleted = true
                    goTo(Stage.Main)
                }
            )

            // ── Вкладки ──
            BottomTab.Task -> ScannerScreen(
                isPro = isPro,
                onClose = { switchTo(BottomTab.History) },
                onShutter = { path ->
                    pendingImagePath = path
                    push(Overlay.Recognizing)
                },
                onPickFromGallery = { push(Overlay.RecognitionFailed) },
                onManualInput = {
                    pendingImagePath = null
                    inputState = MathState()
                    push(Overlay.Input)
                },
                onOpenPaywall = { push(Overlay.Paywall) },
            )

            BottomTab.History -> HistoryScreen(
                entries = historyEntries,
                onOpenEntry = { entry -> openSolution(entry.toOpenSolution(isPro)) },
                onTabSelected = ::switchTo,
            )

            BottomTab.Profile -> ProfileScreen(
                userName = prefs.userDisplayName.ifBlank {
                    prefs.userEmail.ifBlank {
                        prefs.userPhone.ifBlank { "Гость" }
                    }
                },
                solvedCount = savedEntries.size,
                plotsCount = savedEntries.count { it.solution.graph != null },
                isPro = isPro,
                onOpenPaywall = { push(Overlay.Paywall) },
                onOpenSubscription = { push(Overlay.Subscription) },
                onOpenLanguage = { goTo(Stage.Language) },
                onTabSelected = ::switchTo,
            )

            // ── Съёмка ──
            Overlay.Recognizing -> RecognizingScreen(
                imagePath = pendingImagePath,
                onClose = ::pop,
                onRecognized = { result ->
                    pop()
                    when (result) {
                        is RecognitionResult.Success -> {
                            recognizedExpression = result.expression.source
                            push(Overlay.Verify)
                        }

                        is RecognitionResult.Failure -> push(Overlay.RecognitionFailed)
                    }
                },
            )

            Overlay.RecognitionFailed -> RecognitionFailedScreen(
                onBack = ::pop,
                onRetake = ::pop,
                onManualInput = {
                    pop()
                    push(Overlay.Verify)
                },
            )

            Overlay.Input -> {
                val typed = inputState.root.toDisplayText()
                MathInputScreen(
                    state = inputState,
                    problemType = if (typed.isBlank()) {
                        ProblemType.Unknown
                    } else {
                        classifyProblem(typed)
                    },
                    suggestions = suggestionsFor(typed),
                    isPro = isPro,
                    onStateChange = { inputState = it },
                    onSuggestion = { hint -> inputState = inputState.insertText(hint) },
                    onClose = ::pop,
                    onOpenScanner = ::pop,
                    onOpenPaywall = { push(Overlay.Paywall) },
                    onSolve = { source ->
                        recognizedExpression = typed
                        scope.launch { solveAndOpen(source) }
                    },
                )
            }

            Overlay.Verify -> VerifyScreen(
                expression = recognizedExpression,
                imagePath = pendingImagePath,
                onExpressionChange = { recognizedExpression = it },
                onSolve = { scope.launch { solveAndOpen(recognizedExpression) } },
            )

            // ── Решение ──
            Overlay.AiFallback -> {
                val expression = pendingAiExpression
                if (expression != null) {
                    AiFallbackScreen(
                        condition = expression.source,
                        isPro = isPro,
                        isLoading = aiLoading,
                        errorMessage = aiError,
                        onBack = ::pop,
                        onOpenPaywall = { push(Overlay.Paywall) },
                        onSolveWithAi = ::solveWithAi,
                    )
                }
            }

            Overlay.Solution -> {
                val current = open
                if (current != null) {
                    SolutionScreen(
                        title = current.title,
                        condition = current.solution.expression.source,
                        conditionIsFormula = current.conditionIsFormula,
                        steps = current.solution.steps,
                        currentStep = currentStep,
                        visualization = current.visualization,
                        xp = current.xp,
                        isPro = current.isPro,
                        alwaysShowStepFormula = current.isPro || current.visualization is Visualization.NotNeeded,
                        dimFutureSteps = current.visualization is Visualization.NotNeeded,
                        nextLabel = if (current.visualization is Visualization.NotNeeded) {
                            "Следующий шаг"
                        } else {
                            "Дальше"
                        },
                        onBack = ::pop,
                        onSelectStep = { currentStep = it },
                        onPrev = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                        onNext = {
                            val lastStep = current.solution.steps.lastIndex.coerceAtLeast(0)
                            if (currentStep >= lastStep) {
                                pop()
                            } else {
                                currentStep = (currentStep + 1).coerceAtMost(lastStep)
                            }
                        },
                        onOpenDetail = { detailOpen = true },
                        onOpenVisualization = {
                            if (current.visualization is Visualization.Plot2D) push(Overlay.GraphHero)
                        },
                        onHint = if (current.visualization is Visualization.NotNeeded) {
                            { }
                        } else {
                            null
                        },
                    )

                    val step = current.solution.steps.getOrNull(currentStep)
                    val detail = step?.detail
                    if (detailOpen && step != null && detail != null) {
                        StepDetailSheet(
                            stepNumber = currentStep + 1,
                            step = step,
                            detail = detail,
                            onDismiss = { detailOpen = false },
                        )
                    }
                }
            }

            Overlay.GraphHero -> {
                val graph = (open?.visualization as? Visualization.Plot2D)?.graph
                if (graph != null) {
                    GraphHeroScreen(
                        graph = graph,
                        steps = open?.solution?.steps.orEmpty(),
                        currentStep = currentStep,
                        onBack = ::pop,
                        onReset = { currentStep = 0 },
                        onSelectStep = { currentStep = it },
                        onPrev = { currentStep = (currentStep - 1).coerceAtLeast(0) },
                        onNext = {
                            val last = open?.solution?.steps?.lastIndex ?: 0
                            currentStep = (currentStep + 1).coerceAtMost(last)
                        },
                    )
                }
            }

            Overlay.LockedSolution -> {
                val current = open
                if (current != null) {
                    LockedProSolutionScreen(
                        title = current.title,
                        condition = current.solution.expression.source,
                        answer = current.solution.answer,
                        visualization = current.visualization as? Visualization.LockedPro
                            ?: SampleProblems.geometryLocked,
                        onBack = ::pop,
                        onOpenPaywall = { push(Overlay.Paywall) },
                    )
                }
            }

            // ── Подписка ──
            Overlay.Paywall -> PaywallScreen(
                plans = subscriptionPlans,
                purchaseInProgress = purchaseLoading,
                purchaseError = purchaseError,
                onClose = ::pop,
                onRestore = {
                    purchaseLoading = true
                    purchaseError = null
                    playBillingClient.restorePurchases { restored ->
                        purchaseLoading = false
                        if (!restored) {
                            purchaseError = "Активная подписка не найдена в Google Play"
                        }
                    }
                },
                onSubscribe = { plan ->
                    val activity = context.findActivity()
                    if (activity == null) {
                        purchaseError = "Не удалось открыть оплату: экран приложения не найден"
                    } else {
                        purchaseLoading = true
                        purchaseError = null
                        playBillingClient.launchPurchase(activity, plan.id) { message ->
                            if (message != null) {
                                purchaseLoading = false
                                purchaseError = message
                            }
                        }
                    }
                },
            )

            Overlay.Subscription -> ManageSubscriptionScreen(
                isPro = isPro,
                onBack = ::pop,
                onOpenPaywall = { push(Overlay.Paywall) },
                onCancel = ::openGooglePlaySubscriptionCenter,
            )

            else -> Unit
        }
    }
}

/**
 * Подсказки под полем ввода: только то, что действительно достраивает условие.
 * Уравнение без правой части — самая частая недописанная запись.
 */
private fun suggestionsFor(typed: String): List<String> = buildList {
    val isFormula = typed.none { it in 'а'..'я' || it in 'А'..'Я' }
    if (typed.isNotBlank() && isFormula && "=" !in typed) add("= 0")
}

private fun List<SubscriptionPlan>.withBillingPrices(prices: List<BillingProductPrice>): List<SubscriptionPlan> {
    val pricesById = prices.associateBy { it.productId }
    return map { plan ->
        val billingPrice = pricesById[plan.id]?.formattedPrice ?: return@map plan
        val limit = when (plan.id) {
            PlayBillingClient.ProMaxMonthly -> "400 AI-задач"
            else -> "150 AI-задач"
        }
        plan.copy(
            price = "$billingPrice в месяц · $limit",
            renewalText = "Потом $billingPrice в месяц. $limit обновляются каждый месяц.",
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
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
private fun SolutionHistoryEntry.toOpenSolution(isPro: Boolean): OpenSolution = when (solution) {
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
