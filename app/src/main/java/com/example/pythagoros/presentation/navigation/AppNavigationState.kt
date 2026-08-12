package com.example.pythagoros.presentation.navigation

import com.example.pythagoros.domain.model.Solution
import com.example.pythagoros.domain.model.Visualization

/** Экраны первого запуска идут строго по порядку из макета (тур 3). */
enum class Stage { Splash, Language, Onboarding, SignIn, SmsCode, LevelGoal, Main }

/** Экраны, которые открываются поверх вкладки и закрываются кнопкой «назад». */
enum class Overlay {
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
data class OpenSolution(
    val title: String,
    val solution: Solution,
    val visualization: Visualization?,
    val conditionIsFormula: Boolean = true,
    val isPro: Boolean = false,
    val xp: String? = "+15 XP",
)
