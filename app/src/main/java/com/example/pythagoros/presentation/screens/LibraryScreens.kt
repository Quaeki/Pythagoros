package com.example.pythagoros.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.SolutionHistoryEntry
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.BottomNav
import com.example.pythagoros.presentation.components.BottomTab
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.Chip
import com.example.pythagoros.presentation.components.ChipStyle
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.ScreenTitle
import com.example.pythagoros.presentation.components.SettingsRow
import com.example.pythagoros.presentation.components.pressClickable
import com.example.pythagoros.presentation.components.pressScale
import com.example.pythagoros.presentation.components.rememberPressFeedback
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.AccentTint2
import com.example.pythagoros.ui.theme.BorderColor
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Caption
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.MintTint
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn
import com.example.pythagoros.ui.theme.WarnTintCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Фильтры списка истории. */
enum class HistoryFilter(val title: String) {
    All("Все"),
    WithPlot("С графиком"),
    Failed("Ошибки"),
}

/** 1e (левый). История разобранных задач. */
@Composable
fun HistoryScreen(
    entries: List<SolutionHistoryEntry>,
    modifier: Modifier = Modifier,
    onOpenEntry: (SolutionHistoryEntry) -> Unit = {},
    onTabSelected: (BottomTab) -> Unit = {},
) {
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.All) }
    val visible = remember(entries, filter) {
        when (filter) {
            HistoryFilter.All -> entries
            HistoryFilter.WithPlot -> entries.filter { it.solution.graph != null }
            HistoryFilter.Failed -> entries.filter { it.solution.steps.isEmpty() }
        }
    }

    PythScreen(modifier) {
        Spacer(Modifier.height(10.dp))
        ScreenSection {
            ScreenTitle("История")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryFilter.entries.forEach { item ->
                Chip(
                    text = item.title,
                    style = if (item == filter) ChipStyle.Inverse else ChipStyle.Neutral,
                    horizontalPadding = 14.dp,
                    onClick = { filter = item },
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        if (visible.isEmpty()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Здесь появятся разобранные задачи",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                BodyText(
                    "Снимите условие — и решение сохранится сюда вместе с графиком.",
                    fontSize = 14.5.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = ScreenPadding,
                    end = ScreenPadding,
                    bottom = 12.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Ключ по записи, а не по позиции: при смене фильтра строки уезжают
                // и приезжают, а не перекрашиваются на месте.
                items(visible, key = { it.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        modifier = Modifier.animateItem(),
                        onClick = { onOpenEntry(entry) },
                    )
                }
            }
        }

        BottomNav(BottomTab.History, onSelect = onTabSelected)
    }
}

@Composable
private fun HistoryRow(
    entry: SolutionHistoryEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val press = rememberPressFeedback(pressedScale = 0.98f)
    Row(
        modifier
            .pressScale(press)
            .fillMaxWidth()
            .clip(shape)
            .border(1.5.dp, BorderColor, shape)
            .pressClickable(press, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(entry.problemType.tint()),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                entry.problemType.icon(),
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                entry.displayTitle(),
                color = TextPrimary,
                fontFamily = if (entry.expression.isFormulaLike()) DisplayFont else null,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.metaLine(),
                color = TextTertiary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            PythIcons.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * В карточке истории стоит короткое имя задачи: формула — целиком,
 * а условие текстом сворачивается до типа задачи, иначе строка не читается.
 */
private fun SolutionHistoryEntry.displayTitle(): String =
    if (expression.isFormulaLike()) expression else problemType.title

private fun String.isFormulaLike(): Boolean =
    length <= 34 && none { it.isLetter() && it.isCyrillic() }

private fun Char.isCyrillic(): Boolean = this in '\u0400'..'\u04FF'

/** «Сегодня · график построен», «12 авг · 5 шагов». */
private fun SolutionHistoryEntry.metaLine(): String {
    val when_ = formatHistoryDate(createdAtMillis)
    val what = when {
        solution.graph != null -> "график построен"
        solution.steps.isNotEmpty() -> "${solution.steps.size} шагов"
        else -> "без разбора"
    }
    return "$when_ · $what"
}

private fun formatHistoryDate(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> "Сегодня"
        sameYear && dayDiff == 1 -> "Вчера"
        else -> SimpleDateFormat("d MMM", Locale.forLanguageTag("ru")).format(Date(millis))
    }
}

private fun ProblemType.tint(): Color = when (this) {
    ProblemType.QuadraticEquation, ProblemType.LinearEquation, ProblemType.EquationSystem -> AccentTint2
    ProblemType.Integral, ProblemType.Derivative, ProblemType.Limit -> MintTint
    ProblemType.Geometry -> WarnTintCard
    else -> Canvas
}

private fun ProblemType.icon(): ImageVector = when (this) {
    ProblemType.Geometry -> PythIcons.Triangle
    ProblemType.Physics -> PythIcons.Bolt
    ProblemType.WordProblem -> PythIcons.Sparkle
    else -> PythIcons.Chart
}

/** 1e (правый). Профиль и подписка. */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    userName: String = "Аня, 10 класс",
    streakDays: Int = 14,
    xp: Int = 320,
    solvedCount: Int = 0,
    plotsCount: Int = 0,
    isPro: Boolean = false,
    onOpenPaywall: () -> Unit = {},
    onOpenSubscription: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onTabSelected: (BottomTab) -> Unit = {},
) {
    PythScreen(modifier) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(top = 14.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Warn),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(userName, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "$streakDays дней подряд · $xp XP",
                        color = TextTertiary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatTile(solvedCount.toString(), "задач разобрано", Modifier.weight(1f))
                StatTile(plotsCount.toString(), "графиков построено", Modifier.weight(1f))
            }

            Box(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 16.dp)) {
                ProCard(isPro = isPro, onClick = if (isPro) onOpenSubscription else onOpenPaywall)
            }

            ScreenSection(gap = 2.dp) {
                SettingsRow("Уровень сложности объяснений", showChevron = true)
                SettingsRow("Напоминания о занятиях", showChevron = true)
                SettingsRow("Язык интерфейса", showChevron = true, showDivider = false, onClick = onOpenLanguage)
            }

            Spacer(Modifier.height(20.dp))
        }

        BottomNav(BottomTab.Profile, onSelect = onTabSelected)
    }
}

@Composable
private fun StatTile(value: String, caption: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Canvas)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            value,
            color = TextPrimary,
            fontFamily = DisplayFont,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(caption, color = TextTertiary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Тёмная карточка подписки в профиле. */
@Composable
private fun ProCard(isPro: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Ink)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Формула Pro", style = Caption.copy(color = Warn))
        Text(
            if (isPro) "Подписка активна" else "AI-разборы с чертежами",
            color = SurfaceWhite,
            fontSize = 21.sp,
            lineHeight = 27.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (isPro) "Управлять тарифом, лимитом и оплатой" else "Free: OCR, локальные задачи и 2 AI-пробы в месяц",
            color = TextOnDarkSecondary,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
        )
        Spacer(Modifier.height(6.dp))
        PrimaryButton(
            text = if (isPro) "Управлять подпиской" else "Pro · 699 ₽ / месяц",
            background = SurfaceWhite,
            contentColor = Ink,
            height = 50.dp,
            fontSize = 15.sp,
            trailingIcon = null,
            onClick = onClick,
        )
    }
}
