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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.presentation.components.ActionBar
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.CaptionText
import com.example.pythagoros.presentation.components.CheckDot
import com.example.pythagoros.presentation.components.EmptyDot
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FootnoteText
import com.example.pythagoros.presentation.components.OutlinedCard
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.ProgressTrack
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.SettingsRow
import com.example.pythagoros.presentation.components.SystemBarsAppearance
import com.example.pythagoros.presentation.components.TopBar
import com.example.pythagoros.presentation.components.WideScreenPadding
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.AccentBorder
import com.example.pythagoros.ui.theme.AccentTint
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Caption
import com.example.pythagoros.ui.theme.Dark1
import com.example.pythagoros.ui.theme.Dark2
import com.example.pythagoros.ui.theme.Dark4
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.Mint
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnDark
import com.example.pythagoros.ui.theme.TextOnDarkSecondary
import com.example.pythagoros.ui.theme.TextPrimary
import com.example.pythagoros.ui.theme.TextSecondary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn

/** Тариф подписки. Цены и периоды приходят из Google Play Billing. */
data class SubscriptionPlan(
    val id: String,
    val title: String,
    val price: String,
    val renewalText: String,
    val badge: String? = null,
)

val DefaultPlans = listOf(
    SubscriptionPlan(
        id = "pro_monthly",
        title = "Pro",
        price = "699 ₽ в месяц · 150 AI-задач",
        renewalText = "Потом 699 ₽ в месяц. 150 AI-задач обновляются каждый месяц.",
        badge = "Рекомендуем",
    ),
    SubscriptionPlan(
        id = "pro_max_monthly",
        title = "Pro Max",
        price = "1 190 ₽ в месяц · 400 AI-задач",
        renewalText = "Потом 1 190 ₽ в месяц. 400 AI-задач обновляются каждый месяц.",
        badge = "Для активных",
    ),
)

/** Преимущество на paywall: AI-фичи помечены звездой, остальные — галочкой. */
private data class PaywallFeature(
    val highlight: String,
    val rest: String,
    val isAi: Boolean,
)

private val PaywallFeatures = listOf(
    PaywallFeature("150 AI-задач в Pro", " — геометрия, физика и сложные текстовые условия", isAi = true),
    PaywallFeature("400 AI-задач в Pro Max", " для активной подготовки и ежедневной учёбы", isAi = true),
    PaywallFeature("Чертёж строится сам", " по условию: треугольники, окружности, графики, векторы сил", isAi = true),
    PaywallFeature("", "Анимированное построение графиков и чертежей по шагам", isAi = false),
    PaywallFeature("", "Free остаётся для OCR, локальных задач и 2 AI-проб в месяц", isAi = false),
)

/**
 * 3g. Paywall.
 *
 * Текст об автопродлении обязателен для Google Play, поэтому он часть экрана,
 * а не необязательная сноска.
 */
@Composable
fun PaywallScreen(
    modifier: Modifier = Modifier,
    plans: List<SubscriptionPlan> = DefaultPlans,
    purchaseInProgress: Boolean = false,
    purchaseError: String? = null,
    onClose: () -> Unit = {},
    onRestore: () -> Unit = {},
    onSubscribe: (SubscriptionPlan) -> Unit = {},
) {
    var selectedId by rememberSaveable { mutableStateOf(plans.first().id) }
    val selected = plans.firstOrNull { it.id == selectedId } ?: plans.first()

    PythScreen(modifier, background = Ink, lightStatusBarIcons = true) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Восстановить покупку",
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onRestore)
                    .padding(vertical = 6.dp),
                color = TextOnDarkSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Dark2)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    PythIcons.Close,
                    contentDescription = "Закрыть",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 10.dp) {
            Text("Формула Pro", style = Caption.copy(color = Warn))
            Text(
                "AI-разборы с понятным лимитом",
                color = SurfaceWhite,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(20.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 12.dp) {
            PaywallFeatures.forEach { FeatureRow(it) }
        }

        Spacer(Modifier.height(24.dp))
        ScreenSection(horizontalPadding = WideScreenPadding, gap = 10.dp) {
            plans.forEach { plan ->
                PlanRow(
                    plan = plan,
                    selected = plan.id == selectedId,
                    onClick = { selectedId = plan.id },
                )
            }
        }

        Filler()
        ActionBar(horizontalPadding = WideScreenPadding, topPadding = 0.dp, gap = 12.dp) {
            PrimaryButton(
                text = if (purchaseInProgress) "Открываем Google Play..." else "Попробовать 7 дней бесплатно",
                enabled = !purchaseInProgress,
                background = SurfaceWhite,
                contentColor = Ink,
                height = 56.dp,
                fontSize = 16.5.sp,
                trailingIcon = null,
            ) { onSubscribe(selected) }
            purchaseError?.let { message ->
                Text(
                    message,
                    color = Warn,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FootnoteText("${selected.renewalText} Отменить можно в любой момент в Google Play.")
        }
    }
}

@Composable
private fun FeatureRow(feature: PaywallFeature) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (feature.isAi) Warn else Mint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (feature.isAi) PythIcons.Star else PythIcons.Check,
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(12.dp),
            )
        }
        Text(
            buildAnnotatedString {
                if (feature.highlight.isNotEmpty()) {
                    withStyle(SpanStyle(color = SurfaceWhite, fontWeight = FontWeight.Bold)) {
                        append(feature.highlight)
                    }
                }
                append(feature.rest)
            },
            color = TextOnDark,
            fontSize = 15.sp,
            lineHeight = 22.5.sp,
        )
    }
}

@Composable
private fun PlanRow(
    plan: SubscriptionPlan,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    val highlighted = plan.id == "pro_monthly"
    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(if (highlighted) Dark1 else Color.Transparent)
                .border(
                    width = if (highlighted) 2.dp else 1.5.dp,
                    color = if (highlighted) Warn else Dark4,
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(plan.title, color = SurfaceWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    plan.price,
                    color = TextOnDarkSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (selected) {
                CheckDot(background = if (highlighted) Warn else Accent, tint = Ink)
            } else {
                EmptyDot(borderColor = Dark4)
            }
        }
        if (plan.badge != null) {
            Box(
                Modifier
                    .offset(x = 16.dp, y = (-11).dp)
                    .clip(CircleShape)
                    .background(if (highlighted) Warn else Dark4)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    plan.badge,
                    color = if (highlighted) Ink else SurfaceWhite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** 3h. Управление подпиской и дневной лимит. */
@Composable
fun ManageSubscriptionScreen(
    modifier: Modifier = Modifier,
    isPro: Boolean = true,
    planTitle: String = "Формула Pro",
    renewalText: String = "Продлится автоматически 14 сентября 2026 — 699 ₽",
    solvedToday: Int = 0,
    freeLimit: Int = 2,
    onBack: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onCancel: () -> Unit = {},
) {
    PythScreen(modifier) {
        TopBar(title = "Подписка", onBack = onBack)

        ScreenSection(gap = 14.dp) {
            OutlinedCard(
                background = AccentTint,
                borderColor = AccentBorder,
                cornerRadius = 22.dp,
                contentPadding = 18.dp,
            ) {
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (isPro) Accent else TextTertiary)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (isPro) "PRO АКТИВНА" else "БЕСПЛАТНЫЙ ТАРИФ",
                        style = Caption.copy(color = SurfaceWhite, letterSpacing = 0.88.sp),
                    )
                }
                Text(
                    if (isPro) planTitle else "Формула Free",
                    color = TextPrimary,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                )
                BodyText(
                    if (isPro) {
                        "$renewalText. В тариф входит 150 AI-задач в месяц."
                    } else {
                        "Free: OCR, локальные задачи и 2 AI-пробы в месяц. Pro открывает чертежи и 150 AI-задач."
                    },
                    fontSize = 14.sp,
                )
            }

            OutlinedCard(cornerRadius = 22.dp, contentPadding = 18.dp, verticalGap = 12.dp) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    CaptionText("AI-лимит", Modifier.weight(1f))
                    Text(
                        if (isPro) "150 AI / месяц" else "$solvedToday из $freeLimit AI",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                ProgressTrack(
                    fraction = if (isPro) 1f else solvedToday.toFloat() / freeLimit,
                    height = 8.dp,
                    color = if (isPro) Mint else Accent,
                    trackColor = Canvas,
                )
                Text(
                    if (isPro) {
                        "Pro Max увеличивает лимит до 400 AI-задач в месяц."
                    } else {
                        "На бесплатном тарифе — 2 AI-пробы в месяц. OCR и локальные решения остаются доступными."
                    },
                    color = TextTertiary,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        ScreenSection(gap = 2.dp) {
            SettingsRow("Способ оплаты", value = "Google Play")
            SettingsRow("История платежей", showChevron = true)
            SettingsRow("Сменить тариф", showChevron = true, onClick = onOpenPaywall)
            SettingsRow("Промокод", showChevron = true)
            SettingsRow(
                title = if (isPro) "Отменить подписку" else "Оформить Pro",
                danger = isPro,
                showDivider = false,
                onClick = if (isPro) onCancel else onOpenPaywall,
            )
        }

        Filler()
        Box(Modifier.padding(horizontal = ScreenPadding).padding(bottom = 28.dp)) {
            FootnoteText(
                "После отмены Pro работает до конца оплаченного периода. История задач останется доступной.",
                textAlign = TextAlign.Start,
            )
        }
    }
}
