package com.example.pythagoros.presentation.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.domain.model.SolutionStep
import com.example.pythagoros.domain.model.StepDetail
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FunctionPlot
import com.example.pythagoros.presentation.components.ListDivider
import com.example.pythagoros.presentation.components.Motion
import com.example.pythagoros.presentation.components.OutlinedCard
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.SheetHandle
import com.example.pythagoros.presentation.components.SoftButton
import com.example.pythagoros.presentation.components.SquareIconButton
import com.example.pythagoros.presentation.components.TopBar
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentBorder
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Dark2
import com.example.pythagoros.ui.theme.Dark4
import com.example.pythagoros.ui.theme.Dark5
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.Mint
import com.example.pythagoros.ui.theme.MintText
import com.example.pythagoros.ui.theme.MintTint
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn

/**
 * 3a-detail. Шторка подробного разбора шага.
 *
 * Содержимое привязано к текущему шагу: при его смене шторка обновляется,
 * поэтому наружу отдаётся только факт закрытия.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepDetailSheet(
    stepNumber: Int,
    step: SolutionStep,
    detail: StepDetail,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onSimilarExample: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                SheetHandle()
            }
        },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CaptionText("Шаг $stepNumber подробно")
                Text(
                    step.title,
                    color = TextPrimary,
                    fontSize = 23.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            OutlinedCard(
                background = AccentTint,
                borderColor = AccentBorder,
                borderWidth = 1.dp,
                cornerRadius = 18.dp,
                contentPadding = 16.dp,
                verticalGap = 5.dp,
            ) {
                CaptionText("Правило", color = Accent)
                Text(
                    detail.rule,
                    color = TextPrimary,
                    fontFamily = DisplayFont,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CaptionText("Почему так")
                BodyText(detail.why, fontSize = 15.sp)
            }

            Column {
                CaptionText("Вывод по действиям", Modifier.padding(bottom = 8.dp))
                detail.substeps.forEachIndexed { index, sub ->
                    ListDivider()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Canvas),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (index + 1).toString(),
                                color = com.example.pythagoros.ui.theme.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                sub.math,
                                color = TextPrimary,
                                fontFamily = DisplayFont,
                                fontSize = 17.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                sub.comment,
                                color = TextTertiary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MintTint)
                    .padding(horizontal = 16.dp, vertical = 13.dp)
            ) {
                Text(
                    detail.verification,
                    color = MintText,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("Похожий пример", Modifier.weight(1f), onClick = onSimilarExample)
                PrimaryButton(
                    text = "Понятно",
                    modifier = Modifier.weight(1f),
                    background = Ink,
                    height = 50.dp,
                    fontSize = 15.sp,
                    trailingIcon = null,
                    onClick = onDismiss,
                )
            }
        }
    }
}

/**
 * 1b. Решение с графиком-героем: крупный график сверху, шаг в белой шторке снизу.
 * Альтернатива ленте шагов — открывается тапом по карточке графика.
 */
@Composable
fun GraphHeroScreen(
    graph: PolynomialGraph,
    steps: List<SolutionStep>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onReset: () -> Unit = {},
    onSelectStep: (Int) -> Unit = {},
    onPrev: () -> Unit = {},
    onNext: () -> Unit = {},
) {
    val total = steps.size.coerceAtLeast(1)
    val index = currentStep.coerceIn(0, total - 1)

    PythScreen(modifier, background = Ink, lightStatusBarIcons = true) {
        TopBar(
            title = graph.title,
            onBack = onBack,
            dark = true,
            titleIsFormula = true,
        ) {
            SquareIconButton(
                icon = PythIcons.Reset,
                contentDescription = "Сначала",
                size = 36.dp,
                background = Dark2,
                tint = SurfaceWhite,
                onClick = onReset,
            )
        }

        Box(Modifier.padding(horizontal = 14.dp)) {
            FunctionPlot(
                graph = graph,
                revealRoots = index >= 1,
                revealVertex = index >= 2,
                revealCurve = index >= 3,
                height = 270.dp,
                curveColor = Mint,
                vertexColor = Accent,
                rootColor = Warn,
                axisColor = Dark4,
                dotRadius = 8.dp,
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding)
                .padding(top = 6.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            repeat(total) { pip ->
                val pipColor by animateColorAsState(
                    targetValue = if (pip <= index) Mint else Dark5,
                    animationSpec = tween(Motion.Tint),
                    label = "heroPip",
                )
                Box(
                    Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(pipColor)
                        .clickable { onSelectStep(pip) }
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(SurfaceWhite)
                .padding(horizontal = 22.dp)
                .padding(top = 22.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SheetHandle() }
            // Шторка остаётся на месте, меняется только её содержимое: шаг въезжает
            // в ту же сторону, в какую пользователь листает.
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val forward = targetState > initialState
                    val offset = if (forward) 1 else -1
                    slideInHorizontally(tween(Motion.Step, easing = Motion.Emphasized)) {
                        offset * it / 5
                    } + fadeIn(tween(Motion.Step)) togetherWith
                        fadeOut(tween(Motion.Step / 2))
                },
                label = "heroStep",
            ) { shown ->
                val current = steps.getOrNull(shown)
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CaptionText("Шаг ${shown + 1} из $total")
                    Text(
                        current?.title.orEmpty(),
                        color = TextPrimary,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    BodyText(current?.explanation.orEmpty(), fontSize = 15.5.sp)
                    if (!current?.formula.isNullOrBlank()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Canvas)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                current.formula,
                                color = TextPrimary,
                                fontFamily = DisplayFont,
                                fontSize = 18.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
            Filler()
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SquareIconButton(
                    icon = PythIcons.ArrowLeft,
                    contentDescription = "Предыдущий шаг",
                    size = 54.dp,
                    onClick = onPrev,
                )
                PrimaryButton(
                    text = "Понятно, дальше",
                    modifier = Modifier.weight(1f),
                    background = Ink,
                    height = 54.dp,
                    trailingIcon = null,
                    onClick = onNext,
                )
            }
        }
    }
}
