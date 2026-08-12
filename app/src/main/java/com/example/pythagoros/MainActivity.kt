package com.example.pythagoros

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.pythagoros.presentation.PythagorosApp
import com.example.pythagoros.ui.theme.PythagorosTheme
import com.example.pythagoros.ui.theme.SurfaceWhite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Статус-бар в макете нарисован; в приложении он системный и прозрачный.
        // Цвет иконок каждый экран задаёт сам через SystemBarsAppearance.
        enableEdgeToEdge()
        setContent {
            PythagorosTheme {
                PythagorosApp(
                    Modifier
                        .fillMaxSize()
                        .background(SurfaceWhite)
                )
            }
        }
    }
}
