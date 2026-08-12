package com.example.pythagoros.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Токены из раздела «Design Tokens» макета «Формула».
 * Схема светлая и фиксированная: тёмными остаются только карточки визуализации,
 * paywall и экран съёмки — для них ниже отдельная группа `Dark*`.
 */

// ── Основа ──
val Ink = Color(0xFF14151A)
val InkSoft = Color(0xFF1B1C24)
val SurfaceWhite = Color(0xFFFFFFFF)
val Canvas = Color(0xFFF2F1EC)
val SurfaceMuted = Color(0xFFFAF9F5)
val BorderColor = Color(0xFFE4E2DA)
val Divider = Color(0xFFEFEDE6)
val DashedBorderColor = Color(0xFFD8D5CA)

// ── Текст ──
val TextPrimary = Ink
val TextSecondary = Color(0xFF5A5B66)
val TextTertiary = Color(0xFF8A8B96)
val TextOnDark = Color(0xFFE6E6EA)
val TextOnDarkSecondary = Color(0xFF9A9BA6)
val TextOnSplash = Color(0xFFD8D2FF)

// ── Акцент ──
val Accent = Color(0xFF6C5CE7)
val AccentPressed = Color(0xFF4B3ED6)
val AccentTint = Color(0xFFF4F2FF)
val AccentTint2 = Color(0xFFEDEAFF)
val AccentBorder = Color(0xFFE0DBFF)

// ── Успех ──
val Mint = Color(0xFF2ED3A0)
val MintTint = Color(0xFFE6FAF3)
val MintText = Color(0xFF0E6B4F)

// ── Предупреждение ──
val Warn = Color(0xFFFFC94A)
val WarnTint = Color(0xFFFFF0C2)
val WarnTintSoft = Color(0xFFFFFBEF)
val WarnTintCard = Color(0xFFFFF3D6)
val WarnText = Color(0xFF8A6A00)

// ── Прочее ──
val VectorBlue = Color(0xFF61A8FF)
val Destructive = Color(0xFFD14343)
val DestructiveTint = Color(0xFFFFE1E1)

// ── Математическая клавиатура ──
/** Клавиша операции — на тон темнее полотна клавиатуры. */
val KeyOperation = Color(0xFFE8E6DE)

/** Незаполненная часть шаблона в поле ввода. */
val PlaceholderInk = Color(0xFFC9C7BF)

/** Фон сканера — чуть глубже, чем ink: под превью камеры (макет 6c). */
val ScannerBackground = Color(0xFF0F1016)

// ── Тёмные поверхности (карточка визуализации, paywall, камера) ──
val Dark1 = Color(0xFF1F2029)
val Dark2 = Color(0xFF232430)
val Dark3 = Color(0xFF2A2B36)
val Dark4 = Color(0xFF33343E)
val Dark5 = Color(0xFF3A3B45)

/** Заливка фигуры на чертеже и полупрозрачная плоскость на схеме сил. */
val FigureFill = Accent.copy(alpha = 0.14f)
val PlaneFill = TextOnDarkSecondary.copy(alpha = 0.12f)
