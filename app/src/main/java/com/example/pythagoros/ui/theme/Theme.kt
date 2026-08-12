package com.example.pythagoros.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

/**
 * Макет нарисован в одной светлой схеме, поэтому ни системная тема, ни dynamic color
 * на неё не влияют. Тёмные участки (карточка визуализации, paywall, камера)
 * рисуются явными токенами `Dark*`, а не сменой схемы.
 */
private val PythagorosColors = lightColorScheme(
    primary = Accent,
    onPrimary = SurfaceWhite,
    primaryContainer = AccentTint,
    onPrimaryContainer = Accent,
    secondary = Mint,
    onSecondary = Ink,
    secondaryContainer = MintTint,
    onSecondaryContainer = MintText,
    tertiary = Warn,
    onTertiary = Ink,
    tertiaryContainer = WarnTint,
    onTertiaryContainer = WarnText,
    background = SurfaceWhite,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Canvas,
    onSurfaceVariant = TextSecondary,
    error = Destructive,
    onError = SurfaceWhite,
    outline = TextTertiary,
    outlineVariant = BorderColor,
    scrim = Ink.copy(alpha = 0.5f),
)

/**
 * Базовый стиль текста: только семейство и цвет.
 * Кегль, начертание и интерлиньяж каждый элемент задаёт сам — в макете они выверены поштучно.
 */
private val BaseTextStyle = TextStyle(fontFamily = UiFont, color = TextPrimary)

@Composable
fun PythagorosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PythagorosColors,
        typography = Typography,
    ) {
        CompositionLocalProvider(LocalTextStyle provides BaseTextStyle, content = content)
    }
}
