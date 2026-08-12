package com.example.pythagoros.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.BorderColor
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Caption
import com.example.pythagoros.ui.theme.Dark2
import com.example.pythagoros.ui.theme.Destructive
import com.example.pythagoros.ui.theme.Divider
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextSecondary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn
import com.example.pythagoros.ui.theme.WarnText
import com.example.pythagoros.ui.theme.WarnTint

// ─────────────────────────── Кнопки ───────────────────────────

/** Квадратная кнопка «назад» 36×36, radius 12. */
@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    dark: Boolean = false,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback()
    Box(
        modifier
            .pressScale(press)
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (dark) Dark2 else Canvas)
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            PythIcons.ArrowLeft,
            contentDescription = "Назад",
            tint = if (dark) SurfaceWhite else TextPrimary,
            modifier = Modifier.size(17.dp),
        )
    }
}

/**
 * Основная кнопка макета: высота 54, radius 18, сплошная заливка.
 * Неактивное состояние — заливка border-серым (экран кода из SMS).
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color = Accent,
    contentColor: Color = SurfaceWhite,
    height: Dp = 54.dp,
    fontSize: TextUnit = 16.sp,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback(enabled = enabled)
    // Заливка меняется при блокировке кнопки («Решаем через AI...») — переводим её плавно.
    val fill by animateColorAsState(
        targetValue = if (enabled) background else BorderColor,
        animationSpec = tween(Motion.Tint),
        label = "primaryFill",
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.78f,
        animationSpec = tween(Motion.Tint, easing = Motion.Emphasized),
        label = "primaryContentAlpha",
    )
    Box(
        modifier
            .staggeredReveal(index = 1)
            .pressScale(press)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .pressClickable(press, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.graphicsLayer { alpha = contentAlpha },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text,
                color = if (enabled) contentColor else SurfaceWhite,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
            )
            if (trailingIcon != null) {
                Icon(
                    trailingIcon,
                    contentDescription = null,
                    tint = if (enabled) contentColor else SurfaceWhite,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/** Вторичная кнопка: только обводка 1.5dp, radius 16. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    fontSize: TextUnit = 14.5.sp,
    cornerRadius: Dp = 16.dp,
    leadingIcon: ImageVector? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(cornerRadius)
    val press = rememberPressFeedback()
    Box(
        modifier
            .staggeredReveal(index = 2)
            .pressScale(press)
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .border(1.5.dp, BorderColor, shape)
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, null, tint = TextPrimary, modifier = Modifier.size(18.dp))
            }
            Text(text, color = TextPrimary, fontSize = fontSize, fontWeight = FontWeight.Bold)
        }
    }
}

/** Приглушённая кнопка на canvas-фоне: «Продолжить без аккаунта», «Похожий пример». */
@Composable
fun SoftButton(
    text: String,
    modifier: Modifier = Modifier,
    height: Dp = 50.dp,
    fontSize: TextUnit = 15.sp,
    cornerRadius: Dp = 16.dp,
    contentColor: Color = TextPrimary,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback()
    Box(
        modifier
            .staggeredReveal(index = 3)
            .pressScale(press)
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Canvas)
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = contentColor, fontSize = fontSize, fontWeight = FontWeight.Bold)
    }
}

/** Квадратная кнопка-иконка рядом с «Дальше»: назад по шагам, подсказка «?». */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    background: Color = Canvas,
    tint: Color = TextPrimary,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback()
    Box(
        modifier
            .pressScale(press)
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────── Чипы и бейджи ───────────────────────────

enum class ChipStyle {
    /** Невыбранный чип: canvas-фон, тёмный текст. */
    Neutral,

    /** Выбранный чип действия: акцент, белый текст. */
    Accented,

    /** Выбранный фильтр в истории: ink, белый текст. */
    Inverse,
}

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Neutral,
    fontSize: TextUnit = 13.sp,
    horizontalPadding: Dp = 13.dp,
    verticalPadding: Dp = 8.dp,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback()
    // Фильтр в истории и цель в проверке переключаются на месте — цвет ведём анимацией.
    val background by animateColorAsState(
        targetValue = when (style) {
            ChipStyle.Neutral -> Canvas
            ChipStyle.Accented -> Accent
            ChipStyle.Inverse -> Ink
        },
        animationSpec = tween(Motion.Tint),
        label = "chipFill",
    )
    val contentColor by animateColorAsState(
        targetValue = if (style == ChipStyle.Neutral) TextPrimary else SurfaceWhite,
        animationSpec = tween(Motion.Tint),
        label = "chipInk",
    )
    Box(
        modifier
            .pressScale(press)
            .clip(CircleShape)
            .background(background)
            .pressClickable(press, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        Text(
            text,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = if (style == ChipStyle.Neutral) FontWeight.SemiBold else FontWeight.Bold,
        )
    }
}

/** Жёлтый счётчик опыта в шапке решения: «+15 XP». */
@Composable
fun XpBadge(text: String, modifier: Modifier = Modifier) = Box(
    modifier
        .clip(CircleShape)
        .background(WarnTint)
        .padding(horizontal = 10.dp, vertical = 6.dp)
) {
    Text(text, color = WarnText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

/** Бейдж «PRO · AI» в шапке платных разборов. */
@Composable
fun ProBadge(
    text: String = "PRO · AI",
    modifier: Modifier = Modifier,
    background: Color = AccentTint,
    contentColor: Color = Accent,
) = Box(
    modifier
        .clip(CircleShape)
        .background(background)
        .padding(horizontal = 10.dp, vertical = 6.dp)
) {
    Text(text, color = contentColor, style = Caption.copy(letterSpacing = 0.66.sp, color = contentColor))
}

/** Кружок-номер шага 26dp: пройденные и активный — акцент, будущие — canvas. */
@Composable
fun StepDot(
    number: Int,
    reached: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 26.dp,
) {
    // Шаг «загорается», когда до него дошли: это главный индикатор прогресса в ленте.
    val fill by animateColorAsState(
        targetValue = if (reached) Accent else Canvas,
        animationSpec = tween(Motion.Tint),
        label = "stepDotFill",
    )
    val ink by animateColorAsState(
        targetValue = if (reached) SurfaceWhite else TextTertiary,
        animationSpec = tween(Motion.Tint),
        label = "stepDotInk",
    )
    val scale by animateFloatAsState(
        targetValue = if (reached) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "stepDotScale",
    )
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(size)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            number.toString(),
            color = ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Круглая галочка выбора 22dp — язык, цель, тариф. */
@Composable
fun CheckDot(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    background: Color = Accent,
    tint: Color = SurfaceWhite,
) = Box(
    modifier
        .size(size)
        .clip(CircleShape)
        .background(background),
    contentAlignment = Alignment.Center,
) {
    Icon(PythIcons.Check, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.6f))
}

/** Пустой кружок невыбранного тарифа. */
@Composable
fun EmptyDot(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    borderColor: Color = BorderColor,
) = Box(
    modifier
        .size(size)
        .clip(CircleShape)
        .border(2.dp, borderColor, CircleShape)
)

// ─────────────────────────── Строки списков ───────────────────────────

/** Строка настроек: заголовок слева, значение или шеврон справа, разделитель снизу. */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    showChevron: Boolean = false,
    danger: Boolean = false,
    showDivider: Boolean = true,
    onClick: () -> Unit = {},
) {
    val press = rememberPressFeedback(pressedScale = 0.985f)
    Column(
        modifier
            .pressScale(press)
            .fillMaxWidth()
            .pressClickable(press, onClick = onClick)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                color = if (danger) Destructive else TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (value != null) {
                Text(value, color = TextTertiary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (showChevron) {
                Icon(
                    PythIcons.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (showDivider) ListDivider()
    }
}

/**
 * Строка выбора одного варианта: язык, цель, тариф.
 * Выбранная — обводка акцентом, фон accent-tint и галочка справа.
 */
@Composable
fun SelectableRow(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (RowScope.() -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(18.dp)
    val press = rememberPressFeedback()
    val fill by animateColorAsState(
        targetValue = if (selected) AccentTint else Color.Transparent,
        animationSpec = tween(Motion.Tint),
        label = "rowFill",
    )
    val stroke by animateColorAsState(
        targetValue = if (selected) Accent else BorderColor,
        animationSpec = tween(Motion.Tint),
        label = "rowStroke",
    )
    Row(
        modifier
            .staggeredReveal(index = if (selected) 0 else 1)
            .pressScale(press)
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.5.dp, stroke, shape)
            .pressClickable(press, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        leading?.invoke(this)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            )
            if (subtitle != null) {
                Text(subtitle, color = TextTertiary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // Галочка «выпрыгивает», а не появляется мгновенно — выбор заметен без подсветки строки.
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(Motion.Tint)),
            exit = scaleOut(tween(Motion.Tint)) + fadeOut(tween(Motion.Tint)),
        ) {
            CheckDot()
        }
    }
}

// ─────────────────────────── Нижняя навигация ───────────────────────────

enum class BottomTab(val title: String) { History("История"), Task("Задача"), Profile("Профиль") }

/**
 * Таб-бар из макета 1e: две подписи по краям и приподнятая круглая кнопка камеры между ними.
 * Кнопка выступает над панелью на 32dp, поэтому у панели нет клипа по верхней границе.
 */
@Composable
fun BottomNav(
    selected: BottomTab,
    modifier: Modifier = Modifier,
    onSelect: (BottomTab) -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divider))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            NavLabel(BottomTab.History, selected == BottomTab.History) { onSelect(BottomTab.History) }
            CameraFab(onClick = { onSelect(BottomTab.Task) })
            NavLabel(BottomTab.Profile, selected == BottomTab.Profile) { onSelect(BottomTab.Profile) }
        }
    }
}

@Composable
private fun NavLabel(tab: BottomTab, active: Boolean, onClick: () -> Unit) {
    val press = rememberPressFeedback()
    val color by animateColorAsState(
        targetValue = if (active) TextPrimary else TextTertiary,
        animationSpec = tween(Motion.Tint),
        label = "navLabel",
    )
    Text(
        tab.title,
        Modifier
            .pressScale(press)
            .clip(RoundedCornerShape(10.dp))
            .pressClickable(press, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun CameraFab(onClick: () -> Unit) {
    val press = rememberPressFeedback(pressedScale = 0.92f)
    Box(
        Modifier
            .offset(y = (-32).dp)
            .pressScale(press)
            .size(62.dp)
            .clip(CircleShape)
            .background(Accent)
            .pressClickable(press, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            PythIcons.Camera,
            contentDescription = "Снять условие",
            tint = SurfaceWhite,
            modifier = Modifier.size(26.dp),
        )
    }
}

// ─────────────────────────── Мелочи ───────────────────────────

/** Центрированная сноска под кнопкой: дисклеймер подписки, счётчик бесплатных задач. */
@Composable
fun FootnoteText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextTertiary,
    textAlign: androidx.compose.ui.text.style.TextAlign? = androidx.compose.ui.text.style.TextAlign.Center,
) = Text(
    text,
    modifier.fillMaxWidth(),
    color = color,
    fontSize = 12.5.sp,
    lineHeight = 19.sp,
    textAlign = textAlign,
)

/** Абзац основного текста: 15sp / 1.55, вторичный цвет. */
@Composable
fun BodyText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary,
    fontSize: TextUnit = 15.sp,
    fontWeight: FontWeight = FontWeight.Normal,
    fontFamily: FontFamily? = null,
) = Text(
    text,
    modifier,
    color = color,
    fontSize = fontSize,
    lineHeight = fontSize * 1.55f,
    fontWeight = fontWeight,
    fontFamily = fontFamily,
)

/** Слот, растягивающий колонку: в макете это `<div style="flex:1">`. */
@Composable
fun ColumnScope.Filler() = Box(Modifier.weight(1f))

/** Прижатая книзу панель действий с полями экрана. */
@Composable
fun ActionBar(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ScreenPadding,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 28.dp,
    gap: Dp = 10.dp,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier
        .staggeredReveal(index = 3, initialOffsetY = 34f)
        .fillMaxWidth()
        .padding(start = horizontalPadding, end = horizontalPadding, top = topPadding, bottom = bottomPadding),
    verticalArrangement = Arrangement.spacedBy(gap),
    content = content,
)

/** Пара «← | Дальше →» под лентой шагов. */
@Composable
fun StepNavRow(
    nextText: String,
    modifier: Modifier = Modifier,
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    nextBackground: Color = Accent,
    leadingIcon: ImageVector = PythIcons.ArrowLeft,
    leadingDescription: String = "Предыдущий шаг",
    nextTrailingIcon: ImageVector? = PythIcons.ArrowRight,
    onLeading: (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SquareIconButton(
            icon = leadingIcon,
            contentDescription = leadingDescription,
            onClick = onLeading ?: onPrev,
        )
        PrimaryButton(
            text = nextText,
            modifier = Modifier.weight(1f),
            background = nextBackground,
            height = 52.dp,
            trailingIcon = nextTrailingIcon,
            onClick = onNext,
        )
    }
}

/** Жёлтая плашка-предупреждение: «не уверены в символе», «код придёт автоматически». */
@Composable
fun WarnCard(
    modifier: Modifier = Modifier,
    caption: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) = OutlinedCard(
    modifier = modifier,
    background = com.example.pythagoros.ui.theme.WarnTintSoft,
    borderColor = Warn,
    cornerRadius = 20.dp,
    verticalGap = 6.dp,
) {
    if (caption != null) CaptionText(caption, color = WarnText)
    content()
}

/** Контейнер-обёртка для содержимого с горизонтальными полями экрана. */
@Composable
fun ScreenSection(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = ScreenPadding,
    gap: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier
        .staggeredReveal(index = 1)
        .fillMaxWidth()
        .padding(horizontal = horizontalPadding),
    verticalArrangement = Arrangement.spacedBy(gap),
    content = content,
)
