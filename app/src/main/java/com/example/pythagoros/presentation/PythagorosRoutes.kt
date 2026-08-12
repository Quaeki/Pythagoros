package com.example.pythagoros.presentation

import android.content.Context
import androidx.compose.runtime.Composable
import com.example.pythagoros.data.auth.AuthRequestCodeResult
import com.example.pythagoros.data.auth.AuthVerifyCodeResult
import com.example.pythagoros.data.auth.GoogleFirebaseSignInResult
import com.example.pythagoros.data.auth.ProviderIdentity
import com.example.pythagoros.data.billing.PlayBillingClient
import com.example.pythagoros.data.prefs.AppPreferences
import com.example.pythagoros.domain.math.MathState
import com.example.pythagoros.domain.math.insertText
import com.example.pythagoros.domain.math.toDisplayText
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.domain.model.Visualization
import com.example.pythagoros.presentation.components.BottomTab
import com.example.pythagoros.presentation.components.NavDirection
import com.example.pythagoros.presentation.navigation.OpenSolution
import com.example.pythagoros.presentation.navigation.Overlay
import com.example.pythagoros.presentation.navigation.Stage
import com.example.pythagoros.presentation.screens.AiFallbackScreen
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
import com.example.pythagoros.presentation.viewmodel.AuthViewModel
import com.example.pythagoros.presentation.viewmodel.NavigationViewModel
import com.example.pythagoros.presentation.viewmodel.SolverViewModel
import com.example.pythagoros.presentation.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun FirstLaunchRoute(
    stage: Stage,
    prefs: AppPreferences,
    authViewModel: AuthViewModel,
    phone: String,
    debugCode: String?,
    isLoading: Boolean,
    errorMessage: String?,
    scope: CoroutineScope,
    onYandex: () -> Unit,
    onProviderSignedIn: (ProviderIdentity) -> Unit,
    goTo: (Stage, NavDirection) -> Unit,
) {
    when (stage) {
        Stage.Splash -> SplashScreen(
            onLoaded = {
                goTo(if (prefs.onboardingCompleted) Stage.Main else Stage.Language, NavDirection.Forward)
            }
        )

        Stage.Language -> LanguageScreen(
            systemLanguageCode = prefs.languageCode,
            onContinue = { language ->
                prefs.languageCode = language.code
                goTo(Stage.Onboarding, NavDirection.Forward)
            },
        )

        Stage.Onboarding -> OnboardingScreen(
            onFinish = { goTo(Stage.SignIn, NavDirection.Forward) }
        )

        Stage.SignIn -> SignInScreen(
            isLoading = isLoading,
            errorMessage = errorMessage,
            onBack = { goTo(Stage.Onboarding, NavDirection.Back) },
            onRequestCode = { entered ->
                scope.launch {
                    when (authViewModel.requestCode(entered)) {
                        is AuthRequestCodeResult.Success -> goTo(Stage.SmsCode, NavDirection.Forward)
                        is AuthRequestCodeResult.Failure -> Unit
                    }
                }
            },
            onGoogle = {
                if (!isLoading) {
                    authViewModel.beginExternalSignIn()
                    scope.launch {
                        when (val result = authViewModel.signInWithGoogle()) {
                            is GoogleFirebaseSignInResult.Success -> onProviderSignedIn(result.identity)
                            is GoogleFirebaseSignInResult.Failure -> {
                                authViewModel.finishExternalSignInFailure(result.message)
                            }
                        }
                    }
                }
            },
            onYandex = onYandex,
            onEmail = { goTo(Stage.LevelGoal, NavDirection.Forward) },
            onSkip = { goTo(Stage.LevelGoal, NavDirection.Forward) },
        )

        Stage.SmsCode -> SmsCodeScreen(
            phone = phone,
            isLoading = isLoading,
            errorMessage = errorMessage,
            debugCode = debugCode,
            onBack = { goTo(Stage.SignIn, NavDirection.Back) },
            onChangePhone = { goTo(Stage.SignIn, NavDirection.Back) },
            onConfirm = { code ->
                scope.launch {
                    when (authViewModel.verifyCode(code)) {
                        is AuthVerifyCodeResult.Success -> goTo(Stage.LevelGoal, NavDirection.Forward)
                        is AuthVerifyCodeResult.Failure -> Unit
                    }
                }
            },
        )

        Stage.LevelGoal -> LevelGoalScreen(
            onDone = { _, _ ->
                prefs.onboardingCompleted = true
                goTo(Stage.Main, NavDirection.Forward)
            }
        )

        Stage.Main -> Unit
    }
}

@Composable
internal fun MainTabRoute(
    tab: BottomTab,
    prefs: AppPreferences,
    isPro: Boolean,
    solvedCount: Int,
    plotsCount: Int,
    historyEntries: List<SolutionHistoryEntry>,
    onOpenEntry: (SolutionHistoryEntry) -> Unit,
    onUpdatePendingImage: (String?) -> Unit,
    onResetInput: () -> Unit,
    classifyImage: suspend (String) -> ProblemType,
    switchTo: (BottomTab) -> Unit,
    push: (Overlay) -> Unit,
    goTo: (Stage, NavDirection) -> Unit,
) {
    when (tab) {
        BottomTab.Task -> ScannerScreen(
            isPro = isPro,
            onClose = { switchTo(BottomTab.History) },
            onShutter = { path ->
                onUpdatePendingImage(path)
                push(Overlay.Recognizing)
            },
            onPickFromGallery = { push(Overlay.RecognitionFailed) },
            onManualInput = {
                onUpdatePendingImage(null)
                onResetInput()
                push(Overlay.Input)
            },
            onOpenPaywall = { push(Overlay.Paywall) },
            onClassifyImage = classifyImage,
        )

        BottomTab.History -> HistoryScreen(
            entries = historyEntries,
            onOpenEntry = onOpenEntry,
            onTabSelected = switchTo,
        )

        BottomTab.Profile -> ProfileScreen(
            userName = prefs.userDisplayName.ifBlank {
                prefs.userEmail.ifBlank {
                    prefs.userPhone.ifBlank { "Гость" }
                }
            },
            solvedCount = solvedCount,
            plotsCount = plotsCount,
            isPro = isPro,
            onOpenPaywall = { push(Overlay.Paywall) },
            onOpenSubscription = { push(Overlay.Subscription) },
            onOpenLanguage = { goTo(Stage.Language, NavDirection.Forward) },
            onTabSelected = switchTo,
        )
    }
}

@Composable
internal fun CaptureRoute(
    overlay: Overlay,
    solverViewModel: SolverViewModel,
    pendingImagePath: String?,
    recognizedExpression: String,
    inputState: MathState,
    isPro: Boolean,
    scope: CoroutineScope,
    pop: () -> Unit,
    push: (Overlay) -> Unit,
    solveAndOpen: suspend (String) -> Unit,
) {
    when (overlay) {
        Overlay.Recognizing -> RecognizingScreen(
            imagePath = pendingImagePath,
            onClose = pop,
            onRecognizeImage = solverViewModel::recognizeImage,
            onRecognized = { result ->
                pop()
                when (result) {
                    is RecognitionResult.Success -> {
                        solverViewModel.updateRecognizedExpression(result.expression.source)
                        push(Overlay.Verify)
                    }

                    is RecognitionResult.Failure -> push(Overlay.RecognitionFailed)
                }
            },
        )

        Overlay.RecognitionFailed -> RecognitionFailedScreen(
            onBack = pop,
            onRetake = pop,
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
                    solverViewModel.classify(typed)
                },
                suggestions = suggestionsFor(typed),
                isPro = isPro,
                onStateChange = solverViewModel::updateInputState,
                onSuggestion = { hint -> solverViewModel.updateInputState(inputState.insertText(hint)) },
                onClose = pop,
                onOpenScanner = pop,
                onOpenPaywall = { push(Overlay.Paywall) },
                onSolve = { source ->
                    solverViewModel.updateRecognizedExpression(typed)
                    scope.launch { solveAndOpen(source) }
                },
            )
        }

        Overlay.Verify -> VerifyScreen(
            expression = recognizedExpression,
            imagePath = pendingImagePath,
            onExpressionChange = solverViewModel::updateRecognizedExpression,
            onSolve = { scope.launch { solveAndOpen(recognizedExpression) } },
        )

        else -> Unit
    }
}

@Composable
internal fun SolutionRoute(
    overlay: Overlay,
    open: OpenSolution?,
    currentStep: Int,
    detailOpen: Boolean,
    pendingAiExpression: Expression?,
    isPro: Boolean,
    aiLoading: Boolean,
    aiError: String?,
    navigationViewModel: NavigationViewModel,
    pop: () -> Unit,
    push: (Overlay) -> Unit,
    solveWithAi: () -> Unit,
) {
    when (overlay) {
        Overlay.AiFallback -> {
            if (pendingAiExpression != null) {
                AiFallbackScreen(
                    condition = pendingAiExpression.source,
                    isPro = isPro,
                    isLoading = aiLoading,
                    errorMessage = aiError,
                    onBack = pop,
                    onOpenPaywall = { push(Overlay.Paywall) },
                    onSolveWithAi = solveWithAi,
                )
            }
        }

        Overlay.Solution -> {
            if (open != null) {
                SolutionScreen(
                    title = open.title,
                    condition = open.solution.expression.source,
                    conditionIsFormula = open.conditionIsFormula,
                    steps = open.solution.steps,
                    currentStep = currentStep,
                    visualization = open.visualization,
                    xp = open.xp,
                    isPro = open.isPro,
                    alwaysShowStepFormula = open.isPro || open.visualization is Visualization.NotNeeded,
                    dimFutureSteps = open.visualization is Visualization.NotNeeded,
                    nextLabel = if (open.visualization is Visualization.NotNeeded) {
                        "Следующий шаг"
                    } else {
                        "Дальше"
                    },
                    onBack = pop,
                    onSelectStep = navigationViewModel::selectStep,
                    onPrev = navigationViewModel::previousStep,
                    onNext = {
                        val lastStep = open.solution.steps.lastIndex.coerceAtLeast(0)
                        if (currentStep >= lastStep) {
                            pop()
                        } else {
                            navigationViewModel.nextStep(lastStep)
                        }
                    },
                    onOpenDetail = navigationViewModel::openDetail,
                    onOpenVisualization = {
                        if (open.visualization is Visualization.Plot2D) push(Overlay.GraphHero)
                    },
                    onHint = if (open.visualization is Visualization.NotNeeded) {
                        { }
                    } else {
                        null
                    },
                )

                val step = open.solution.steps.getOrNull(currentStep)
                val detail = step?.detail
                if (detailOpen && step != null && detail != null) {
                    StepDetailSheet(
                        stepNumber = currentStep + 1,
                        step = step,
                        detail = detail,
                        onDismiss = navigationViewModel::closeDetail,
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
                    onBack = pop,
                    onReset = navigationViewModel::resetStep,
                    onSelectStep = navigationViewModel::selectStep,
                    onPrev = navigationViewModel::previousStep,
                    onNext = {
                        val last = open?.solution?.steps?.lastIndex ?: 0
                        navigationViewModel.nextStep(last)
                    },
                )
            }
        }

        Overlay.LockedSolution -> {
            if (open != null) {
                LockedProSolutionScreen(
                    title = open.title,
                    condition = open.solution.expression.source,
                    answer = open.solution.answer,
                    visualization = open.visualization as? Visualization.LockedPro
                        ?: SampleProblems.geometryLocked,
                    onBack = pop,
                    onOpenPaywall = { push(Overlay.Paywall) },
                )
            }
        }

        else -> Unit
    }
}

@Composable
internal fun SubscriptionRoute(
    overlay: Overlay,
    context: Context,
    isPro: Boolean,
    plans: List<SubscriptionPlan>,
    purchaseLoading: Boolean,
    purchaseError: String?,
    subscriptionViewModel: SubscriptionViewModel,
    playBillingClient: PlayBillingClient,
    pop: () -> Unit,
    push: (Overlay) -> Unit,
    openGooglePlaySubscriptionCenter: () -> Unit,
) {
    when (overlay) {
        Overlay.Paywall -> PaywallScreen(
            plans = plans,
            purchaseInProgress = purchaseLoading,
            purchaseError = purchaseError,
            onClose = pop,
            onRestore = {
                subscriptionViewModel.updatePurchaseLoading(true)
                subscriptionViewModel.updatePurchaseError(null)
                playBillingClient.restorePurchases { restored ->
                    subscriptionViewModel.updatePurchaseLoading(false)
                    if (!restored) {
                        subscriptionViewModel.updatePurchaseError("Активная подписка не найдена в Google Play")
                    }
                }
            },
            onSubscribe = { plan ->
                val activity = context.findActivity()
                if (activity == null) {
                    subscriptionViewModel.updatePurchaseError("Не удалось открыть оплату: экран приложения не найден")
                } else {
                    subscriptionViewModel.updatePurchaseLoading(true)
                    subscriptionViewModel.updatePurchaseError(null)
                    playBillingClient.launchPurchase(activity, plan.id) { message ->
                        if (message != null) {
                            subscriptionViewModel.updatePurchaseLoading(false)
                            subscriptionViewModel.updatePurchaseError(message)
                        }
                    }
                }
            },
        )

        Overlay.Subscription -> ManageSubscriptionScreen(
            isPro = isPro,
            onBack = pop,
            onOpenPaywall = { push(Overlay.Paywall) },
            onCancel = openGooglePlaySubscriptionCenter,
        )

        else -> Unit
    }
}
