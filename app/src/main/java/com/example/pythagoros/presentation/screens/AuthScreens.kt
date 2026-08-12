package com.example.pythagoros.presentation.screens

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.presentation.components.ActionBar
import com.example.pythagoros.presentation.components.BackButton
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.Chip
import com.example.pythagoros.presentation.components.ChipStyle
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FootnoteText
import com.example.pythagoros.presentation.components.ListDivider
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.ProgressTrack
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.ScreenTitle
import com.example.pythagoros.presentation.components.SecondaryButton
import com.example.pythagoros.presentation.components.SelectableRow
import com.example.pythagoros.presentation.components.SoftButton
import com.example.pythagoros.presentation.components.WarnCard
import com.example.pythagoros.presentation.components.WideScreenPadding
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.BorderColor
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextSecondary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.WarnText
import kotlinx.coroutines.delay

/**
 * 3d. Вход.
 *
 * Google-кнопка ведёт в Credential Manager, почта — во второй способ входа;
 * «без аккаунта» пускает в приложение, но история остаётся только на устройстве.
 */
@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onBack: () -> Unit = {},
    onRequestCode: (String) -> Unit = {},
    onGoogle: () -> Unit = {},
    onYandex: () -> Unit = {},
    onEmail: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    var phone by rememberSaveable { mutableStateOf("") }
    val canSubmit = phone.filter { it.isDigit() }.length >= 10

    PythScreen(modifier) {
        Row(Modifier.padding(start = ScreenPadding, top = 8.dp)) {
            BackButton(onClick = onBack)
        }
        Spacer(Modifier.height(20.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 10.dp) {
            ScreenTitle("Вход в Формулу", fontSize = 28.sp)
            BodyText("История задач и прогресс сохранятся на всех устройствах.")
        }
        Spacer(Modifier.height(24.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 12.dp) {
            PhoneField(value = phone, onValueChange = { phone = it })
            if (errorMessage != null) {
                AuthErrorCard(errorMessage)
            }
            PrimaryButton(
                text = if (isLoading) "Отправляем..." else "Получить код",
                enabled = canSubmit && !isLoading,
            ) {
                onRequestCode(phone)
            }
            OrDivider()
            SecondaryButton(
                text = "Продолжить с Google",
                height = 54.dp,
                cornerRadius = 18.dp,
                fontSize = 15.5.sp,
                onClick = onGoogle,
            )
            SecondaryButton(
                text = "Продолжить с Яндекс ID",
                height = 54.dp,
                cornerRadius = 18.dp,
                fontSize = 15.5.sp,
                onClick = onYandex,
            )
            SecondaryButton(
                text = "Войти по почте",
                height = 54.dp,
                cornerRadius = 18.dp,
                fontSize = 15.5.sp,
                leadingIcon = PythIcons.Mail,
                onClick = onEmail,
            )
            SoftButton(
                text = "Продолжить без аккаунта",
                height = 54.dp,
                cornerRadius = 18.dp,
                fontSize = 15.5.sp,
                contentColor = TextSecondary,
                onClick = onSkip,
            )
        }
        Filler()
        Box(Modifier.padding(horizontal = WideScreenPadding, vertical = 28.dp)) {
            FootnoteText("Продолжая, вы принимаете условия использования и политику конфиденциальности.")
        }
    }
}

/** Поле телефона: в фокусе обводка становится ink-чёрной, как в макете. */
@Composable
private fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.5.dp, if (focused || value.isNotEmpty()) Ink else BorderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        CaptionText("Телефон")
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter { char -> char.isDigit() || char in "+ -" }) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChangedCompat { focused = it },
            textStyle = TextStyle(
                color = TextPrimary,
                fontFamily = DisplayFont,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        "+7 900 123-45-67",
                        color = TextTertiary,
                        fontFamily = DisplayFont,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                inner()
            },
        )
    }
}

private fun Modifier.onFocusChangedCompat(onChanged: (Boolean) -> Unit): Modifier =
    onFocusChanged { onChanged(it.isFocused) }

@Composable
private fun OrDivider() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ListDivider(Modifier.weight(1f))
        Text("или", color = TextTertiary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        ListDivider(Modifier.weight(1f))
    }
}

/** Длина кода из SMS. */
private const val CodeLength = 4

/** Сколько секунд ждать перед повторной отправкой. */
private const val ResendSeconds = 60

/**
 * 3e. Код из SMS.
 *
 * В проде код подставляется сам через SMS Retriever API — жёлтая плашка об этом и говорит,
 * поэтому ручной ввод оставлен запасным путём.
 */
@Composable
fun SmsCodeScreen(
    phone: String,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    debugCode: String? = null,
    onBack: () -> Unit = {},
    onChangePhone: () -> Unit = {},
    onConfirm: (String) -> Unit = {},
) {
    var code by rememberSaveable { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(ResendSeconds) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }

    PythScreen(modifier) {
        Row(Modifier.padding(start = ScreenPadding, top = 8.dp)) {
            BackButton(onClick = onBack)
        }
        Spacer(Modifier.height(20.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 10.dp) {
            ScreenTitle("Введите код", fontSize = 28.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BodyText("Отправили SMS на ${phone.ifBlank { "ваш номер" }}.")
                Text(
                    "Изменить номер",
                    Modifier.clickable(onClick = onChangePhone),
                    color = Accent,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                )
            }
        }
        Spacer(Modifier.height(26.dp))

        CodeCells(
            code = code,
            onCodeChange = { code = it },
            focusRequester = focusRequester,
            modifier = Modifier.padding(horizontal = WideScreenPadding),
        )

        Spacer(Modifier.height(16.dp))
        if (debugCode != null) {
            Box(Modifier.padding(horizontal = WideScreenPadding).padding(bottom = 12.dp)) {
                WarnCard(caption = "Тестовый код") {
                    BodyText(
                        "Пока SMS-провайдер не подключён, введите код: $debugCode",
                        color = WarnText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        if (errorMessage != null) {
            Box(Modifier.padding(horizontal = WideScreenPadding).padding(bottom = 12.dp)) {
                AuthErrorCard(errorMessage)
            }
        }
        Text(
            if (secondsLeft > 0) {
                "Отправить код повторно через 0:%02d".format(secondsLeft)
            } else {
                "Отправить код повторно"
            },
            Modifier
                .padding(horizontal = WideScreenPadding)
                .then(if (secondsLeft == 0) Modifier.clickable { secondsLeft = ResendSeconds } else Modifier),
            color = if (secondsLeft > 0) TextTertiary else Accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(20.dp))
        Box(Modifier.padding(horizontal = WideScreenPadding)) {
            WarnCard {
                BodyText(
                    "Код придёт в течение минуты. Мы подставим его автоматически.",
                    color = WarnText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Filler()
        ActionBar(horizontalPadding = WideScreenPadding, topPadding = 0.dp) {
            PrimaryButton(
                text = if (isLoading) "Проверяем..." else "Подтвердить",
                enabled = code.length == CodeLength && !isLoading,
            ) {
                onConfirm(code)
            }
        }
    }
}

@Composable
private fun AuthErrorCard(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AccentTint)
            .border(1.dp, Accent, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        BodyText(message, color = TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Ячейки кода: одно невидимое поле ввода, поверх которого нарисованы четыре ячейки.
 * Так работает автоподстановка и системная клавиатура, а вид остаётся из макета.
 */
@Composable
private fun CodeCells(
    code: String,
    onCodeChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = code,
        onValueChange = { value ->
            onCodeChange(value.filter { it.isDigit() }.take(CodeLength))
        },
        modifier = modifier.focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        decorationBox = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                repeat(CodeLength) { index ->
                    val active = index == code.length.coerceAtMost(CodeLength - 1) &&
                        code.length < CodeLength
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clip(shape)
                            .background(if (active) AccentTint else Color.Transparent)
                            .border(
                                width = if (active) 2.dp else 1.5.dp,
                                color = if (active) Accent else BorderColor,
                                shape = shape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            code.getOrNull(index)?.toString().orEmpty(),
                            color = if (active) Accent else TextPrimary,
                            fontFamily = DisplayFont,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )
}

/** Уровень пользователя — от него зависит язык объяснений. */
val StudyLevels = listOf("7–8 класс", "9–11 класс", "Вуз", "Для себя")

/** Цель — под неё подстраивается подбор похожих задач. */
val StudyGoals = listOf(
    "Сдать ЕГЭ / ОГЭ",
    "Подтянуть школьную программу",
    "Быстро решать домашку",
    "Разобраться в вузовской математике",
)

/** 3f. Уровень и цель — последний шаг первого запуска. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelGoalScreen(
    modifier: Modifier = Modifier,
    onDone: (level: String, goal: String) -> Unit = { _, _ -> },
) {
    var level by rememberSaveable { mutableStateOf(StudyLevels[1]) }
    var goal by rememberSaveable { mutableStateOf(StudyGoals[0]) }

    PythScreen(modifier) {
        Box(Modifier.padding(horizontal = ScreenPadding, vertical = 14.dp)) {
            ProgressTrack(fraction = 0.7f)
        }
        Spacer(Modifier.height(6.dp))
        ScreenSection {
            ScreenTitle("Кто ты и зачем пришёл?")
        }
        Spacer(Modifier.height(16.dp))
        ScreenSection(gap = 10.dp) {
            CaptionText("Уровень")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StudyLevels.forEach { item ->
                    Chip(
                        text = item,
                        style = if (item == level) ChipStyle.Accented else ChipStyle.Neutral,
                        fontSize = 14.sp,
                        horizontalPadding = 15.dp,
                        verticalPadding = 10.dp,
                        onClick = { level = item },
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        ScreenSection(gap = 10.dp) {
            CaptionText("Цель")
            StudyGoals.forEach { item ->
                SelectableRow(
                    title = item,
                    selected = item == goal,
                    onClick = { goal = item },
                )
            }
        }
        Filler()
        ActionBar {
            PrimaryButton("Готово") { onDone(level, goal) }
        }
    }
}
