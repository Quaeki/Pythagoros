package com.example.pythagoros.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.model.GraphPoint
import com.example.pythagoros.domain.model.PolynomialGraph
import com.example.pythagoros.presentation.components.ActionBar
import com.example.pythagoros.presentation.components.BodyText
import com.example.pythagoros.presentation.components.Filler
import com.example.pythagoros.presentation.components.FunctionPlot
import com.example.pythagoros.presentation.components.IndeterminateTrack
import com.example.pythagoros.presentation.components.Motion
import com.example.pythagoros.presentation.components.PrimaryButton
import com.example.pythagoros.presentation.components.PythScreen
import com.example.pythagoros.presentation.components.ScreenPadding
import com.example.pythagoros.presentation.components.ScreenSection
import com.example.pythagoros.presentation.components.ScreenTitle
import com.example.pythagoros.presentation.components.SelectableRow
import com.example.pythagoros.presentation.components.WideScreenPadding
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.BorderColor
import com.example.pythagoros.ui.theme.Dark5
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.Mint
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextOnSplash
import com.example.pythagoros.ui.theme.TextSecondary
import com.example.pythagoros.ui.theme.TextTertiary
import com.example.pythagoros.ui.theme.Warn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 3a. Сплеш.
 *
 * В проде основную задержку закрывает `SplashScreen API`, а этот экран висит,
 * пока восстанавливается сессия, — поэтому индикатор без процентов.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onLoaded: () -> Unit = {},
) {
    // Появление логотипа запускаем отдельным кадром: иначе анимация стартовала бы
    // с целевого значения и экран открывался бы уже собранным.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appeared = true
        delay(1100)
        onLoaded()
    }

    val logoScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "logoScale",
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(Motion.Screen, easing = Motion.Emphasized),
        label = "splashLogo",
    )
    // Подпись догоняет знак, а не появляется вместе с ним.
    val textAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(Motion.Screen, delayMillis = 140, easing = Motion.Emphasized),
        label = "splashText",
    )

    Box(
        modifier
            .fillMaxSize()
            .background(Accent),
        contentAlignment = Alignment.Center,
    ) {
        com.example.pythagoros.presentation.components.SystemBarsAppearance(lightIcons = true)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
                    .size(104.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(SurfaceWhite),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "ƒ",
                    color = Accent,
                    fontFamily = DisplayFont,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(
                Modifier.graphicsLayer { alpha = textAlpha },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Формула", color = SurfaceWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Сфотографируй — и всё станет понятно",
                    color = TextOnSplash,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .width(150.dp)
        ) {
            // Восстановление сессии не измеряется в процентах — полоса ходит,
            // а не заполняется, чтобы не обещать конкретного прогресса.
            IndeterminateTrack(
                height = 4.dp,
                color = Warn,
                trackColor = SurfaceWhite.copy(alpha = 0.25f),
            )
        }
    }
}

/** Язык приложения. Порядок фиксированный: системный первым. */
data class AppLanguage(val code: String, val title: String, val flag: String)

val SupportedLanguages = listOf(
    AppLanguage("ru", "Русский", "🇷🇺"),
    AppLanguage("kk", "Қазақша", "🇰🇿"),
    AppLanguage("en", "English", "🇬🇧"),
    AppLanguage("uz", "Oʻzbekcha", "🇺🇿"),
)

/**
 * 3b. Выбор языка.
 *
 * Реально язык применяется через per-app language
 * (`AppCompatDelegate.setApplicationLocales`) — здесь только выбор.
 */
@Composable
fun LanguageScreen(
    modifier: Modifier = Modifier,
    systemLanguageCode: String = "ru",
    onContinue: (AppLanguage) -> Unit = {},
) {
    var selectedCode by rememberSaveable { mutableStateOf(systemLanguageCode) }

    PythScreen(modifier) {
        Spacer(Modifier.height(18.dp))
        ScreenSection(gap = 16.dp) {
            ScreenTitle("Язык приложения")
            BodyText(
                "Можно поменять позже в профиле. Условия задач распознаём на любом из них.",
                fontSize = 14.5.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        ScreenSection(gap = 10.dp) {
            SupportedLanguages.forEach { language ->
                SelectableRow(
                    title = language.title,
                    selected = language.code == selectedCode,
                    subtitle = if (language.code == systemLanguageCode) "Определён по системе" else null,
                    leading = { Text(language.flag, fontSize = 20.sp) },
                    onClick = { selectedCode = language.code },
                )
            }
        }
        Filler()
        ActionBar {
            PrimaryButton("Продолжить") {
                onContinue(SupportedLanguages.first { it.code == selectedCode })
            }
        }
    }
}

/** Страница онбординга: тёмная иллюстрация + заголовок + текст. */
private data class OnboardingPage(
    val title: String,
    val body: String,
    val revealRoots: Boolean,
    val revealVertex: Boolean,
    /** 0f — кривой ещё нет, 1f — проведена целиком. */
    val curveFraction: Float,
)

private val OnboardingPages = listOf(
    OnboardingPage(
        title = "Сфотографируй задачу",
        body = "Наведи камеру на условие целиком. Распознаём формулы, текст и чертёж — и сразу показываем, что поняли.",
        revealRoots = false,
        revealVertex = false,
        curveFraction = 0.35f,
    ),
    OnboardingPage(
        title = "График рисуется у тебя на глазах",
        body = "Не готовая картинка, а построение по шагам: сначала корни, потом вершина, потом кривая. Так запоминается.",
        revealRoots = true,
        revealVertex = false,
        curveFraction = 0.71f,
    ),
    OnboardingPage(
        title = "Каждый шаг можно разобрать",
        body = "К любому шагу открывается полный вывод: правило, объяснение простыми словами и проверка ответа.",
        revealRoots = true,
        revealVertex = true,
        curveFraction = 1f,
    ),
)

/** Иллюстрация онбординга — тот же график, что в решении. */
private val OnboardingGraph = PolynomialGraph(
    title = "y = x² − 4x + 3",
    variable = "x",
    coefficients = mapOf(2 to 1.0, 1 to -4.0, 0 to 3.0),
    roots = listOf(1.0, 3.0),
    vertex = GraphPoint(2.0, -1.0, "(2; −1)"),
)

/** 3c. Онбординг из трёх экранов. */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    onFinish: () -> Unit = {},
) {
    val pagerState = rememberPagerState(pageCount = { OnboardingPages.size })
    val scope = rememberCoroutineScope()

    // График «рисуется у тебя на глазах» — это обещание второй страницы, поэтому
    // построение запускается на той странице, которую пользователь сейчас видит.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }

    PythScreen(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                "Пропустить",
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                color = TextTertiary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
            val page = OnboardingPages[index]
            val built = appeared && index == pagerState.currentPage
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .padding(horizontal = ScreenPadding)
                        .padding(top = 14.dp)
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Ink)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FunctionPlot(
                        graph = OnboardingGraph,
                        revealRoots = built && page.revealRoots,
                        revealVertex = built && page.revealVertex,
                        revealCurve = built && page.curveFraction > 0f,
                        curveFraction = page.curveFraction,
                        height = 260.dp,
                        curveColor = Mint,
                        vertexColor = Accent,
                        axisColor = Dark5,
                    )
                }
                Column(
                    Modifier
                        .padding(horizontal = WideScreenPadding)
                        .padding(top = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        page.title,
                        color = com.example.pythagoros.ui.theme.TextPrimary,
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    BodyText(page.body, fontSize = 16.sp, color = TextSecondary)
                }
            }
        }

        ActionBar(horizontalPadding = WideScreenPadding, topPadding = 0.dp, gap = 18.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(OnboardingPages.size) { index ->
                    val active = index == pagerState.currentPage
                    // Активная точка вытягивается в полоску — переход показывает,
                    // куда именно ушёл пейджер, а не просто что он ушёл.
                    val width by animateDpAsState(
                        targetValue = if (active) 26.dp else 10.dp,
                        animationSpec = tween(Motion.Step, easing = Motion.Emphasized),
                        label = "pagerDot",
                    )
                    val color by animateColorAsState(
                        targetValue = if (active) Accent else BorderColor,
                        animationSpec = tween(Motion.Step),
                        label = "pagerDotColor",
                    )
                    Box(
                        Modifier
                            .width(width)
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(color)
                    )
                }
            }
            PrimaryButton(if (pagerState.currentPage == OnboardingPages.lastIndex) "Начать" else "Дальше") {
                if (pagerState.currentPage == OnboardingPages.lastIndex) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
        }
    }
}
