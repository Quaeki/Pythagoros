package com.example.pythagoros.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.pythagoros.presentation.components.BottomTab
import com.example.pythagoros.presentation.components.NavDirection
import com.example.pythagoros.presentation.navigation.OpenSolution
import com.example.pythagoros.presentation.navigation.Overlay
import com.example.pythagoros.presentation.navigation.Stage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NavigationViewModel @Inject constructor() : ViewModel() {

    var stage by mutableStateOf(Stage.Splash)
        private set

    var tab by mutableStateOf(BottomTab.Task)
        private set

    val overlays = mutableStateListOf<Overlay>()

    var navDirection by mutableStateOf(NavDirection.Forward)
        private set

    var selectedSolution by mutableStateOf<OpenSolution?>(null)
        private set

    var currentStep by mutableIntStateOf(0)
        private set

    var detailOpen by mutableStateOf(false)
        private set

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

    fun showSolution(target: OpenSolution, isProActive: Boolean) {
        selectedSolution = target
        currentStep = 0
        detailOpen = false
        push(if (target.isPro && !isProActive) Overlay.LockedSolution else Overlay.Solution)
    }

    fun selectStep(index: Int) {
        currentStep = index.coerceAtLeast(0)
    }

    fun resetStep() {
        currentStep = 0
    }

    fun previousStep() {
        currentStep = (currentStep - 1).coerceAtLeast(0)
    }

    fun nextStep(lastIndex: Int) {
        currentStep = (currentStep + 1).coerceAtMost(lastIndex.coerceAtLeast(0))
    }

    fun openDetail() {
        detailOpen = true
    }

    fun closeDetail() {
        detailOpen = false
    }
}
