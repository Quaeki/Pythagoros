package com.example.pythagoros

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.pythagoros.presentation.PythagorosApp
import com.example.pythagoros.presentation.viewmodel.AuthViewModel
import com.example.pythagoros.presentation.viewmodel.HistoryViewModel
import com.example.pythagoros.presentation.viewmodel.NavigationViewModel
import com.example.pythagoros.presentation.viewmodel.SolverViewModel
import com.example.pythagoros.presentation.viewmodel.SubscriptionViewModel
import com.example.pythagoros.ui.theme.PythagorosTheme
import com.example.pythagoros.ui.theme.SurfaceWhite
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val solverViewModel: SolverViewModel by viewModels()
    private val historyViewModel: HistoryViewModel by viewModels()
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()
    private val navigationViewModel: NavigationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Статус-бар в макете нарисован; в приложении он системный и прозрачный.
        // Цвет иконок каждый экран задаёт сам через SystemBarsAppearance.
        enableEdgeToEdge()
        setContent {
            PythagorosTheme {
                PythagorosApp(
                    authViewModel = authViewModel,
                    solverViewModel = solverViewModel,
                    historyViewModel = historyViewModel,
                    subscriptionViewModel = subscriptionViewModel,
                    navigationViewModel = navigationViewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SurfaceWhite)
                )
            }
        }
    }
}
