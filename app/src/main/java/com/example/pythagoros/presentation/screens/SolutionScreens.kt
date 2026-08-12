package com.example.pythagoros.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.model.MarkId
import com.example.pythagoros.domain.model.PlotMarks
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.Visualization
import com.example.pythagoros.presentation.components.ActionBar
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.ConditionCard
import com.example.pythagoros.presentation.components.FigureCanvas
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FootnoteText
import com.example.pythagoros.presentation.components.FunctionPlot
import com.example.pythagoros.presentation.components.LoadingDots
import com.example.pythagoros.presentation.components.Motion
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.ProBadge
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.SecondaryButton
import com.example.pythagoros.presentation.components.StepDot
import com.example.pythagoros.presentation.components.StepNavRow
import com.example.pythagoros.presentation.components.TopBar
import com.example.pythagoros.presentation.components.VisualCard
import com.example.pythagoros.presentation.components.XpBadge
import com.example.pythagoros.presentation.components.dashedBorder
import com.example.pythagoros.presentation.components.pressClickable
import com.example.pythagoros.presentation.components.pressScale
import com.example.pythagoros.presentation.components.rememberPressFeedback
import com.example.pythagoros.presentation.components.staggeredReveal
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Divider
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.SurfaceMuted
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextSecondary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn

/**
 * Экран решения (макеты 1a, 2a, 4a, 4b).
 *
 * Каркас один: условие → слот визуализации → лента шагов → навигация.
 * Что именно окажется в слоте, решает [visualization], поэтому график, чертёж,
 * схема сил и «график не нужен» — это один и тот же экран с одной вьюмоделью.
 */
@Composable
fun SolutionScreen(
    title: String,
    condition: String,
    steps: List<SolutionStep>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    visualization: Visualization? = null,
    conditionIsFormula: Boolean = true,
    xp: String? = null,
    isPro: Boolean = false,
    /** В 2a формула шага видна всегда, а в 1a — только заголовок. */
    alwaysShowStepFormula: Boolean = false,
    /** В 2a будущие шаги приглушены и лежат на surface-muted. */
    dimFutureSteps: Boolean = false,
    nextLabel: String = "Дальше",
    showDetailButton: Boolean = true,
    onBack: () -> Unit = {},
    onSelectStep: (Int) -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
    onOpenDetail: () -> Unit = {},
    onOpenVisualization: () -> Unit = {},
    onHint: (() -> Unit)? = null,
) {
    val total = steps.size.coerceAtLeast(1)
    val step = currentStep.coerceIn(0, total - 1)
    val listState = rememberLazyListState()
    val stepStartIndex = if (visualization != null) 2 else 1
    val isLastStep = step >= total - 1

    LaunchedEffect(step, visualization != null) {
        val target = if (step == 0) 0 else stepStartIndex + step
        listState.animateScrollToItem(target)
    }

    PythScreen(modifier) {
        TopBar(title = title, onBack = onBack) {
            when {
                isPro -> ProBadge()
                xp != null -> XpBadge(xp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = ScreenPadding,
                end = ScreenPadding,
                bottom = 10.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ConditionCard(Modifier.staggeredReveal(index = 0)) {
                    if (conditionIsFormula) {
                        Text(
                            condition,
                            color = TextPrimary,
                            fontFamily = DisplayFont,
                            fontSize = 19.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else {
                        BodyText(
                            condition,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            if (visualization != null) {
                item {
                    VisualizationSlot(
                        visualization = visualization,
                        steps = steps,
                        currentStep = step,
                        onOpenVisualization = onOpenVisualization,
                    )
                }
            }
            items(
                count = steps.size,
                key = { index -> "${steps[index].title}:${steps[index].formula}:$index" },
            ) { index ->
                StepCard(
                    number = index + 1,
                    step = steps[index],
                    isActive = index == step,
                    isReached = index <= step,
                    alwaysShowFormula = alwaysShowStepFormula,
                    dimIfFuture = dimFutureSteps,
                    revealIndex = index + if (visualization != null) 2 else 1,
                    onClick = { onSelectStep(index) },
                )
            }
        }

        ActionBar(topPadding = 8.dp, bottomPadding = 26.dp) {
            if (showDetailButton && steps.getOrNull(step)?.detail != null) {
                SecondaryButton("Разобрать этот шаг подробно", onClick = onOpenDetail)
            }
            StepNavRow(
                nextText = if (alwaysShowStepFormula) {
                    "${if (isLastStep) "Готово" else nextLabel} (${step + 1}/$total)"
                } else {
                    if (isLastStep) "Готово" else nextLabel
                },
                onPrev = onPrev,
                onNext = onNext,
                leadingIcon = if (onHint != null) PythIcons.Question else PythIcons.ArrowLeft,
                leadingDescription = if (onHint != null) "Подсказка" else "Предыдущий шаг",
                nextTrailingIcon = if (onHint != null) null else PythIcons.ArrowRight,
                onLeading = onHint,
            )
        }
    }
}

/** Слот визуализации: график, чертёж, схема или плашка «не нужен». */
@Composable
private fun VisualizationSlot(
    visualization: Visualization,
    steps: List<SolutionStep>,
    currentStep: Int,
    onOpenVisualization: () -> Unit,
) {
    val counter = "шаг ${currentStep + 1} / ${steps.size.coerceAtLeast(1)}"
    when (visualization) {
        is Visualization.Plot2D -> {
            val revealed = revealedMarks(steps, currentStep)
            VisualCard(
                caption = visualization.caption,
                counter = counter,
                modifier = Modifier.staggeredReveal(index = 1),
            ) {
                FunctionPlot(
                    graph = visualization.graph,
                    revealRoots = PlotMarks.Roots in revealed,
                    revealVertex = PlotMarks.Vertex in revealed,
                    revealCurve = PlotMarks.Curve in revealed,
                    modifier = Modifier.clickable(onClick = onOpenVisualization),
                )
            }
        }

        is Visualization.Figure -> {
            val declared = steps.take(currentStep + 1).flatMap { it.reveal }.toSet()
            VisualCard(
                caption = visualization.caption,
                counter = counter,
                modifier = Modifier.staggeredReveal(index = 1),
            ) {
                FigureCanvas(
                    primitives = visualization.primitives,
                    visible = declared.ifEmpty { null },
                    aspectRatio = visualization.aspectRatio,
                )
            }
        }

        is Visualization.NotNeeded -> {
            Row(
                Modifier
                    .staggeredReveal(index = 1)
                    .fillMaxWidth()
                    .dashedBorder()
                    .clickable(onClick = onOpenVisualization)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(PythIcons.Chart, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Text(
                    buildAnnotatedString {
                        append(visualization.hint)
                        visualization.actionText?.let { action ->
                            append(" — ")
                            withStyle(SpanStyle(color = Accent, fontWeight = FontWeight.SemiBold)) {
                                append(action)
                            }
                        }
                    },
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    lineHeight = 19.5.sp,
                )
            }
        }

        is Visualization.LockedPro -> LockedProCard(
            visualization = visualization,
            onOpenPaywall = onOpenVisualization,
        )
    }
}

/**
 * Что уже открыто на визуализации к текущему шагу.
 *
 * Если решатель не проставил отметки (локальное ядро их пока не заполняет),
 * работает правило макета: шаг 2 — корни, шаг 3 — вершина, шаг 4 — кривая.
 */
private fun revealedMarks(steps: List<SolutionStep>, currentStep: Int): Set<MarkId> {
    val declared = steps.take(currentStep + 1).flatMap { it.reveal }.toSet()
    if (declared.isNotEmpty()) return declared
    return buildSet {
        if (currentStep >= 1) add(PlotMarks.Roots)
        if (currentStep >= 2) add(PlotMarks.Vertex)
        if (currentStep >= 3) add(PlotMarks.Curve)
    }
}

/** Карточка шага: пояснение раскрывается только у активного. */
@Composable
private fun StepCard(
    number: Int,
    step: SolutionStep,
    isActive: Boolean,
    isReached: Boolean,
    alwaysShowFormula: Boolean,
    dimIfFuture: Boolean,
    revealIndex: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val dimmed = dimIfFuture && !isReached
    val press = rememberPressFeedback(pressedScale = 0.985f)
    // Активная карточка «переезжает» по ленте при каждом «Дальше» — цвет, обводка
    // и приглушение будущих шагов ведутся анимацией, иначе лента дёргается.
    val fill by animateColorAsState(
        targetValue = when {
            isActive -> AccentTint
            dimmed -> SurfaceMuted
            else -> SurfaceWhite
        },
        animationSpec = tween(Motion.Tint),
        label = "stepFill",
    )
    val stroke by animateColorAsState(
        targetValue = if (isActive) Accent else Divider,
        animationSpec = tween(Motion.Tint),
        label = "stepStroke",
    )
    val dim by animateFloatAsState(
        targetValue = if (dimmed) 0.45f else 1f,
        animationSpec = tween(Motion.Tint),
        label = "stepDim",
    )
    val activeLift by animateFloatAsState(
        targetValue = if (isActive) -3f else 0f,
        animationSpec = tween(Motion.Step, easing = Motion.Emphasized),
        label = "stepLift",
    )
    val activeScale by animateFloatAsState(
        targetValue = if (isActive) 1.01f else 1f,
        animationSpec = tween(Motion.Step, easing = Motion.Emphasized),
        label = "stepScale",
    )
    Row(
        Modifier
            .staggeredReveal(index = revealIndex)
            .pressScale(press)
            .graphicsLayer {
                translationY = activeLift
                scaleX = activeScale
                scaleY = activeScale
            }
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, stroke, shape)
            .pressClickable(press, onClick = onClick)
            .alpha(dim)
            .padding(14.dp)
            .animateContentSize(tween(Motion.Step, easing = Motion.Emphasized)),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepDot(number = number, reached = isReached)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                step.title,
                color = TextPrimary,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            if (step.formula.isNotBlank() && alwaysShowFormula) {
                Text(
                    step.formula,
                    color = Accent,
                    fontFamily = DisplayFont,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            // Высоту карточки доводит animateContentSize, поэтому пояснению хватает
            // проявления: два одновременных раскрытия дали бы двойной рывок.
            AnimatedVisibility(
                visible = isActive && step.explanation.isNotBlank(),
                enter = slideInVertically(
                    animationSpec = tween(Motion.Step, delayMillis = Motion.Step / 5, easing = Motion.Emphasized),
                    initialOffsetY = { it / 3 },
                ) + fadeIn(tween(Motion.Step, delayMillis = Motion.Step / 5)),
                exit = slideOutVertically(
                    animationSpec = tween(Motion.Step / 2, easing = Motion.Accelerated),
                    targetOffsetY = { -it / 4 },
                ) + fadeOut(tween(Motion.Step / 2)),
            ) {
                BodyText(step.explanation, fontSize = 13.5.sp)
            }
        }
    }
}

/** 4c. Тёмная карточка с закрытой Pro-визуализацией. */
@Composable
private fun LockedProCard(
    visualization: Visualization.LockedPro,
    onOpenPaywall: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Ink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(78.dp)
                .alpha(0.55f),
            contentAlignment = Alignment.Center,
        ) {
            FigureCanvas(
                primitives = visualization.preview,
                aspectRatio = 300f / 140f,
                blurRadius = 5.dp,
            )
        }
        Text(
            visualization.title,
            color = SurfaceWhite,
            fontSize = 19.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            visualization.subtitle,
            color = TextOnDarkSecondary,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        PrimaryButton(
            text = "Открыть Pro — 7 дней бесплатно",
            background = Warn,
            contentColor = Ink,
            height = 50.dp,
            fontSize = 15.5.sp,
            onClick = onOpenPaywall,
        )
    }
}

/**
 * 4c. Решение Pro-задачи без подписки: ответ виден, вывод закрыт.
 *
 * Отдельный экран, а не режим [SolutionScreen], потому что лента шагов здесь
 * заменена на короткий блок «доступно бесплатно».
 */
@Composable
fun LockedProSolutionScreen(
    title: String,
    condition: String,
    answer: String,
    modifier: Modifier = Modifier,
    visualization: Visualization.LockedPro,
    freeAiLeft: Int = 2,
    onBack: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
) {
    PythScreen(modifier) {
        TopBar(title = title, onBack = onBack)

        ScreenSection(gap = 12.dp) {
            ConditionCard {
                BodyText(condition, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            LockedProCard(visualization = visualization, onOpenPaywall = onOpenPaywall)
        }

        Spacer(Modifier.height(12.dp))
        ScreenSection(gap = 10.dp) {
            CaptionText("Доступно бесплатно")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, Divider, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepDot(number = 1, reached = true)
                Text(
                    "Ответ: $answer",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceMuted)
                    .dashedBorder(cornerRadius = 18.dp, strokeWidth = 1.dp)
                    .clickable(onClick = onOpenPaywall)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(PythIcons.Lock, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                Text(
                    "Подробный вывод и построение доступны в Pro",
                    color = TextTertiary,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
            }
        }

        Filler()
        Box(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 28.dp)) {
            FootnoteText("Осталось $freeAiLeft AI-пробы на бесплатном тарифе в этом месяце")
        }
    }
}

/**
 * Локальное ядро остановилось: пользователь может открыть Pro или отправить задачу
 * в backend AI. Успешный ответ показывается обычным [SolutionScreen].
 */
@Composable
fun AiFallbackScreen(
    condition: String,
    isPro: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onBack: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onSolveWithAi: () -> Unit = {},
) {
    PythScreen(modifier) {
        TopBar(title = "AI-дорешивание", onBack = onBack) {
            ProBadge()
        }

        ScreenSection(gap = 12.dp) {
            ConditionCard {
                Text(
                    condition,
                    color = TextPrimary,
                    fontFamily = DisplayFont,
                    fontSize = 18.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(AccentTint)
                    .border(1.dp, Accent, RoundedCornerShape(22.dp))
                    .padding(18.dp)
                    // Карточка растёт под индикатор ожидания и под текст ошибки —
                    // высоту доводим плавно, чтобы кнопки внизу не прыгали.
                    .animateContentSize(tween(Motion.Step, easing = Motion.Emphasized)),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CaptionText("Локальный решатель остановился", color = Accent)
                Text(
                    "Эта задача уйдёт на backend AI. Ответ вернётся как обычная лента шагов, " +
                        "а если модель даст график, мы сохраним его вместе с разбором.",
                    color = TextSecondary,
                    fontSize = 14.5.sp,
                    lineHeight = 22.sp,
                )
                // Запрос уходит на backend, длительность заранее неизвестна —
                // пульсирующие точки показывают, что ожидание живое.
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        LoadingDots()
                        Text(
                            "Ждём ответ модели",
                            color = Accent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                if (errorMessage != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceWhite)
                            .padding(14.dp)
                    ) {
                        Text(
                            errorMessage,
                            color = Warn,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Filler()

        ActionBar {
            if (isPro) {
                PrimaryButton(
                    text = if (isLoading) "Решаем через AI..." else "Дорешать с AI",
                    enabled = !isLoading,
                    onClick = onSolveWithAi,
                )
                SecondaryButton(
                    text = "Вернуться к условию",
                    height = 50.dp,
                    onClick = onBack,
                )
            } else {
                PrimaryButton(
                    text = "Открыть Pro",
                    onClick = onOpenPaywall,
                )
                SecondaryButton(
                    text = "Вернуться и поправить",
                    height = 50.dp,
                    onClick = onBack,
                )
            }
        }
    }
}
