package com.example.pythagoros.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.data.ocr.MlKitProblemRecognizer
import com.example.pythagoros.domain.model.Expression
import com.example.pythagoros.domain.model.ProblemType
import com.example.pythagoros.domain.model.RecognitionResult
import com.example.pythagoros.presentation.components.ActionBar
import com.example.pythagoros.presentation.components.BackButton
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.Chip
import com.example.pythagoros.presentation.components.ChipStyle
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.OutlinedCard
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.ScreenTitle
import com.example.pythagoros.presentation.components.SecondaryButton
import com.example.pythagoros.presentation.components.WarnCard
import com.example.pythagoros.presentation.components.shimmer
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextTertiary

// ─────────────────────── Распознавание и его ошибки ───────────────────────

private const val SampleEquation = "y = x² − 4x + 3"

/**
 * Состояние между «снял» и «проверьте условие»: распознавание идёт на устройстве,
 * поэтому вместо прогресса показываем скелетон будущих карточек шагов.
 */
@Composable
fun RecognizingScreen(
    modifier: Modifier = Modifier,
    imagePath: String? = null,
    onClose: () -> Unit = {},
    onRecognized: (RecognitionResult) -> Unit = {},
) {
    val context = LocalContext.current
    val recognizer = remember(context) { MlKitProblemRecognizer(context.applicationContext) }

    LaunchedEffect(imagePath) {
        val result = runCatching {
            if (imagePath != null) {
                recognizer.recognize(imagePath)
            } else {
                RecognitionResult.Success(
                    expression = Expression(SampleEquation),
                    guessedType = ProblemType.QuadraticEquation,
                )
            }
        }.getOrElse { RecognitionResult.Failure(RecognitionResult.Reason.NoFormulaFound) }
        onRecognized(result)
    }

    PythScreen(modifier) {
        Row(Modifier.padding(start = ScreenPadding, top = 8.dp)) {
            BackButton(onClick = onClose)
        }
        Spacer(Modifier.height(10.dp))
        ScreenSection(gap = 16.dp) {
            ScreenTitle("Читаем условие", fontSize = 20.sp)
            PhotoPreview(imagePath)
            CaptionText("Разбираем символы")
            repeat(3) { index ->
                SkeletonStep(alpha = 1f - index * 0.25f, shimmerDelay = index * 140)
            }
        }
        Filler()
        Box(Modifier.padding(horizontal = ScreenPadding, vertical = 26.dp)) {
            BodyText(
                "Распознавание идёт на устройстве. Интернет нужен только для AI-разбора.",
                fontSize = 13.5.sp,
            )
        }
    }
}

/**
 * Скелет карточки шага — заполняет ленту, пока решение не готово.
 *
 * Блик идёт по всем карточкам с разным сдвигом: волна сверху вниз читается как
 * «разбираем по порядку», а не как три независимо мигающих блока.
 */
@Composable
private fun SkeletonStep(alpha: Float, shimmerDelay: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite)
            .border(1.dp, com.example.pythagoros.ui.theme.Divider, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Canvas.copy(alpha = alpha))
                .shimmer(delayMillis = shimmerDelay)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.7f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Canvas.copy(alpha = alpha))
                    .shimmer(delayMillis = shimmerDelay)
            )
            Box(
                Modifier
                    .height(12.dp)
                    .fillMaxWidth(0.45f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Canvas.copy(alpha = alpha))
                    .shimmer(delayMillis = shimmerDelay)
            )
        }
    }
}

/** OCR не справился: предложить переснять или ввести вручную. */
@Composable
fun RecognitionFailedScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onRetake: () -> Unit = {},
    onManualInput: () -> Unit = {},
) {
    PythScreen(modifier) {
        Row(Modifier.padding(start = ScreenPadding, top = 8.dp)) {
            BackButton(onClick = onBack)
        }
        Spacer(Modifier.height(20.dp))
        ScreenSection(gap = 14.dp) {
            ScreenTitle("Не удалось прочитать условие")
            BodyText(
                "Текст получился смазанным или обрезанным. Снимите ещё раз так, чтобы условие целиком попало в рамку, — или наберите его вручную.",
            )
            WarnCard(caption = "Что помогает") {
                BodyText(
                    "Ровный свет без бликов, лист целиком в кадре, телефон параллельно странице.",
                    color = com.example.pythagoros.ui.theme.WarnText,
                    fontSize = 14.5.sp,
                )
            }
        }
        Filler()
        ActionBar {
            PrimaryButton("Переснять", onClick = onRetake)
            SecondaryButton("Ввести вручную", height = 54.dp, cornerRadius = 18.dp, onClick = onManualInput)
        }
    }
}

// ─────────────────────── 1d. Проверка распознанного ───────────────────────

/**
 * Пользователь подтверждает и правит OCR перед решением — от этого напрямую
 * зависит точность, поэтому шаг обязательный, а не необязательный просмотр.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VerifyScreen(
    expression: String,
    modifier: Modifier = Modifier,
    imagePath: String? = null,
    uncertainSymbol: String? = null,
    goals: List<String> = listOf("Построить график", "Найти нули"),
    selectedGoal: String = goals.firstOrNull().orEmpty(),
    onExpressionChange: (String) -> Unit = {},
    onGoalSelected: (String) -> Unit = {},
    onAddGoal: () -> Unit = {},
    onSolve: () -> Unit = {},
) {
    PythScreen(modifier) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(10.dp))
            ScreenSection {
                ScreenTitle("Всё верно распознали?", fontSize = 20.sp)
            }
            Spacer(Modifier.height(14.dp))
            ScreenSection(gap = 16.dp) {
                PhotoPreview(imagePath)
            }
            Spacer(Modifier.height(16.dp))

            ScreenSection(gap = 12.dp) {
                OutlinedCard {
                    CaptionText("Условие")
                    BasicTextField(
                        value = expression,
                        onValueChange = onExpressionChange,
                        textStyle = TextStyle(
                            color = TextPrimary,
                            fontFamily = DisplayFont,
                            fontSize = 22.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("Править", fontSize = 13.sp, horizontalPadding = 12.dp, verticalPadding = 7.dp)
                        Chip(
                            text = "x² вместо x2",
                            fontSize = 13.sp,
                            horizontalPadding = 12.dp,
                            verticalPadding = 7.dp,
                            onClick = { onExpressionChange(expression.replace("x2", "x²")) },
                        )
                    }
                }

                if (uncertainSymbol != null) {
                    WarnCard(caption = "Не уверены в одном символе") {
                        BodyText(
                            "«$uncertainSymbol»? Нажмите, чтобы выбрать правильный вариант — так решение будет точным.",
                            fontSize = 14.5.sp,
                        )
                    }
                }

                OutlinedCard {
                    CaptionText("Что требуется")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        goals.forEach { goal ->
                            Chip(
                                text = goal,
                                style = if (goal == selectedGoal) ChipStyle.Accented else ChipStyle.Neutral,
                                onClick = { onGoalSelected(goal) },
                            )
                        }
                    Chip("+ добавить", onClick = onAddGoal)
                }
            }
            }
        }

        ActionBar {
            PrimaryButton("Решать", onClick = onSolve)
        }
    }
}

/** Превью снимка: сам кадр, если он есть, иначе плашка-заглушка. */
@Composable
private fun PhotoPreview(imagePath: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(imagePath) {
        imagePath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Canvas),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Снимок условия",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text("фото условия", color = TextTertiary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
