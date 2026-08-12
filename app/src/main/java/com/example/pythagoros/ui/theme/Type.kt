package com.example.pythagoros.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pythagoros.R

/**
 * Интерфейсный шрифт макета — Manrope 400/500/600/700.
 * Он же несёт кириллицу, поэтому русские заголовки набираем им.
 *
 * В `res/font` лежат статические начертания, снятые с вариативного файла:
 * ось `wght` у вариативного шрифта на устройстве применялась не ко всем весам,
 * и весь текст рисовался одним начертанием.
 */
val UiFont = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

/**
 * Дисплейный шрифт макета — Space Grotesk 500/600/700: формулы, числа, латиница.
 *
 * Кириллицы в Space Grotesk нет (и в самом HTML-моке русский текст уже показывался
 * системным гротеском), поэтому русские заголовки набираем [UiFont], а [DisplayFont]
 * оставляем математике: `y = x² − 4x + 3`, `∫ (3x² + 2x) dx`, счётчикам и статистике.
 */
val DisplayFont = FontFamily(
    Font(R.font.space_grotesk_medium, FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, FontWeight.Bold),
)

/** Caption: 11sp, 700, uppercase, letter-spacing .1em. */
val Caption = TextStyle(
    fontFamily = UiFont,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 1.1.sp,
)

/** Caption на тёмной карточке визуализации — на кегль крупнее и трекинг .08em. */
val CaptionOnDark = TextStyle(
    fontFamily = UiFont,
    fontWeight = FontWeight.Bold,
    fontSize = 12.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.96.sp,
)

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 33.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 29.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = UiFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
    ),
)
