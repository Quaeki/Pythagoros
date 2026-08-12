package com.example.pythagoros.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.math.MathState
import com.example.pythagoros.domain.math.backspace
import com.example.pythagoros.domain.math.insert
import com.example.pythagoros.domain.math.insertText
import com.example.pythagoros.domain.math.moveLeft
import com.example.pythagoros.domain.math.moveRight
import com.example.pythagoros.domain.math.toDisplayText
import com.example.pythagoros.domain.math.toSource
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FootnoteText
import com.example.pythagoros.presentation.components.KeyboardTab
import com.example.pythagoros.presentation.components.MathFieldView
import com.example.pythagoros.presentation.components.MathKey
import com.example.pythagoros.presentation.components.MathKeyboard
import com.example.pythagoros.presentation.components.Motion
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.ProBadge
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.AccentBorder
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Dark4
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDark
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn
import com.example.pythagoros.ui.theme.WarnText
import com.example.pythagoros.ui.theme.WarnTint

/**
 * 5a / 5b. Ручной ввод условия математической клавиатурой.
 *
 * Тип задачи определяется прямо во время набора: он и подсказывает пользователю,
 * что распознано, и решает, показывать ли Pro-гейт (макет 6a) — до вызова решателя.
 */
@Composable
fun MathInputScreen(
    state: MathState,
    modifier: Modifier = Modifier,
    problemType: ProblemType = ProblemType.Unknown,
    suggestions: List<String> = emptyList(),
    isPro: Boolean = false,
    onStateChange: (MathState) -> Unit = {},
    onSuggestion: (String) -> Unit = {},
    onClose: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onSolve: (String) -> Unit = {},
    onOpenPaywall: () -> Unit = {},
) {
    var tab by remember { mutableStateOf(KeyboardTab.Numbers) }
    // Гейт закрыт до тех пор, пока пользователь не наберёт условие другого типа.
    var gateDismissedFor by remember { mutableStateOf<ProblemType?>(null) }

    val needsPro = problemType.requiresPro && !isPro
    val gateVisible = needsPro && gateDismissedFor != problemType

    PythScreen(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = ScreenPadding, end = ScreenPadding, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SquareGlyphButton(PythIcons.Close, "Закрыть", onClick = onClose)
            Text(
                "Ввести условие",
                Modifier.weight(1f),
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            )
            SquareGlyphButton(PythIcons.Camera, "Снять камерой", onClick = onOpenScanner)
        }

        Box(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 12.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, Ink, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                MathFieldView(state, placeholder = "наберите условие")
            }
        }

        // Строка живёт прямо во время набора: тип задачи и подсказки то появляются,
        // то исчезают, поэтому она не схлопывается рывком, а раскрывается.
        AnimatedVisibility(
            visible = problemType != ProblemType.Unknown || suggestions.isNotEmpty(),
            enter = expandVertically(tween(Motion.Step, easing = Motion.Emphasized)) +
                fadeIn(tween(Motion.Step)),
            exit = shrinkVertically(tween(Motion.Step, easing = Motion.Accelerated)) +
                fadeOut(tween(Motion.Step)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ScreenPadding)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (problemType != ProblemType.Unknown) {
                    TypeChip(problemType = problemType, warn = needsPro)
                }
                suggestions.forEach { hint ->
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(Canvas)
                            .clickable { onSuggestion(hint) }
                            .padding(horizontal = 13.dp, vertical = 8.dp)
                    ) {
                        Text(
                            hint,
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Filler()

        MathKeyboard(
            tab = tab,
            dimmed = gateVisible,
            solveEnabled = !state.isEmpty && !needsPro,
            onTabChange = { tab = it },
            onKey = { key -> onStateChange(state.applyKey(key)) },
            onSolve = { onSolve(state.root.toSource()) },
        )
    }

    // Тип запоминаем отдельно: гейт уезжает вниз уже после того, как условие
    // перестало быть платным, и подписи в нём не должны на лету меняться.
    var gateType by remember { mutableStateOf(problemType) }
    LaunchedEffect(gateVisible, problemType) {
        if (gateVisible) gateType = problemType
    }

    ProGateSheet(
        visible = gateVisible,
        problemType = gateType,
        onSubscribe = onOpenPaywall,
        onDismiss = { gateDismissedFor = problemType },
    )
}

/** Тип задачи под полем: обычный — акцентный чип, платный — жёлтый. */
@Composable
private fun TypeChip(problemType: ProblemType, warn: Boolean) {
    val label = if (warn) "Похоже на ${problemType.hint}" else problemType.title
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (warn) WarnTint else AccentTint)
            .then(
                if (warn) Modifier else Modifier.border(1.dp, AccentBorder, CircleShape)
            )
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            color = if (warn) WarnText else com.example.pythagoros.ui.theme.Accent,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SquareGlyphButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Canvas)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = TextPrimary, modifier = Modifier.size(17.dp))
    }
}

/**
 * 6a. Шторка Pro-гейта поверх ввода.
 *
 * Открывается по типу задачи, а не по результату решения: платный OCR и AI-запрос
 * до подписки не вызываются вовсе.
 *
 * Видимостью управляет [visible], а не условие на стороне вызова: шторке нужно
 * успеть уехать вниз, поэтому она остаётся в композиции и на время закрытия.
 */
@Composable
fun ProGateSheet(
    problemType: ProblemType,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    onSubscribe: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(Motion.Scrim)),
        exit = fadeOut(tween(Motion.Scrim, delayMillis = Motion.Sheet - Motion.Scrim)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Ink.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Column(
                Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            animationSpec = tween(Motion.Sheet, easing = Motion.Emphasized),
                            initialOffsetY = { it },
                        ),
                        exit = slideOutVertically(
                            animationSpec = tween(Motion.Sheet, easing = Motion.Accelerated),
                            targetOffsetY = { it },
                        ),
                    )
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(Ink)
                    .clickable(enabled = false) {}
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Dark4)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ProBadge()
                    Text(
                        problemType.gateLabel,
                        color = TextOnDarkSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(
                    "Физику и геометрию решает AI из Pro",
                    color = SurfaceWhite,
                    fontSize = 25.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Он разберёт условие, построит схему сил и объяснит каждый шаг. " +
                        "Обычный решатель такие задачи не берёт.",
                    color = TextOnDarkSecondary,
                    fontSize = 14.5.sp,
                    lineHeight = 22.5.sp,
                )

                Column(
                    Modifier.padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    GateBullet("Чертёж и схема сил строятся по шагам")
                    GateBullet("Разбор с формулами и проверкой ответа")
                }

                PrimaryButton(
                    text = "Попробовать 7 дней бесплатно",
                    background = Warn,
                    contentColor = Ink,
                    height = 56.dp,
                    fontSize = 16.5.sp,
                    onClick = onSubscribe,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Dark4, RoundedCornerShape(16.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Ввести другую задачу",
                        color = SurfaceWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                FootnoteText("Потом 699 ₽ в месяц за Pro. Отменить можно в любой момент.")
            }
        }
    }
}

@Composable
private fun GateBullet(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Warn),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PythIcons.Star, null, tint = Ink, modifier = Modifier.size(11.dp))
        }
        Text(text, color = TextOnDark, fontSize = 14.5.sp, lineHeight = 21.sp)
    }
}

/** Чип под полем: «Похоже на физику · механика». */
private val ProblemType.hint: String
    get() = when (this) {
        ProblemType.Physics -> "физику · механика"
        ProblemType.Geometry -> "геометрию · чертёж"
        else -> title.lowercase()
    }

/** Подпись рядом с бейджем в шторке — в именительном падеже. */
private val ProblemType.gateLabel: String
    get() = when (this) {
        ProblemType.Physics -> "Физика · механика"
        ProblemType.Geometry -> "Геометрия · чертёж"
        else -> title
    }

/** Задачи, которые берёт только AI из Pro. */
private val ProblemType.requiresPro: Boolean
    get() = this == ProblemType.Physics || this == ProblemType.Geometry

/** Применить нажатие клавиши к состоянию ввода. */
private fun MathState.applyKey(key: MathKey): MathState = when (key) {
    is MathKey.Insert -> insertText(key.text)
    is MathKey.Node -> insert(key.node)
    MathKey.Backspace -> backspace()
    MathKey.Left -> moveLeft()
    MathKey.Right -> moveRight()
}

/** Текст условия для классификатора и истории. */
fun MathState.displayText(): String = root.toDisplayText()
