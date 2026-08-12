package com.example.pythagoros.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.math.MathNode
import com.example.pythagoros.presentation.icons.PythIcons
import com.example.pythagoros.ui.theme.Canvas
import com.example.pythagoros.ui.theme.Destructive
import com.example.pythagoros.ui.theme.DestructiveTint
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.Ink
import com.example.pythagoros.ui.theme.KeyOperation
import com.example.pythagoros.ui.theme.SurfaceWhite
import com.example.pythagoros.ui.theme.TextPrimary

/** Что делает клавиша. */
sealed interface MathKey {
    /** Вставляет символы: цифру, букву, имя функции. */
    data class Insert(val label: String, val text: String = label) : MathKey

    /** Вставляет узел со слотами: дробь, степень, корень, скобки. */
    data class Node(val label: String, val node: MathNode) : MathKey

    data object Backspace : MathKey
    data object Left : MathKey
    data object Right : MathKey
}

/** Вкладки клавиатуры из макета. */
enum class KeyboardTab(val title: String) {
    Numbers("123"),
    Functions("ƒ(x)"),
    Greek("αβγ"),
    Letters("ABC"),
}

private val Operation = MathKey.Insert("")

/** Клавиша считается «операционной» и красится темнее — как в макете. */
private fun MathKey.isOperation(): Boolean = when (this) {
    is MathKey.Node -> true
    is MathKey.Insert -> label in OperationLabels
    else -> false
}

private val OperationLabels = setOf(
    "+", "−", "×", "=", "∫", "d/dx", "Σ", "lim", "≤", "≥", "≠", "<", ">",
)

/**
 * Математическая клавиатура (макеты 5a, 5b).
 *
 * Системная клавиатура не используется даже для текстовых условий: раскладка ABC —
 * часть той же панели, иначе поле ввода теряет дерево выражения.
 */
@Composable
fun MathKeyboard(
    tab: KeyboardTab,
    modifier: Modifier = Modifier,
    dimmed: Boolean = false,
    solveEnabled: Boolean = true,
    onTabChange: (KeyboardTab) -> Unit = {},
    onKey: (MathKey) -> Unit = {},
    onSolve: () -> Unit = {},
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(Canvas)
            .alpha(if (dimmed) 0.4f else 1f)
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeyboardTab.entries.forEach { item ->
                TabPill(item, active = item == tab, onClick = { onTabChange(item) })
            }
        }

        when (tab) {
            KeyboardTab.Numbers -> NumbersLayout(onKey)
            KeyboardTab.Functions -> FunctionsLayout(onKey)
            KeyboardTab.Greek -> GridLayout(GreekKeys, columns = 4, onKey = onKey)
            KeyboardTab.Letters -> LettersLayout(onKey)
        }

        PrimaryButton(
            text = "Решить",
            enabled = solveEnabled,
            height = 56.dp,
            fontSize = 16.5.sp,
            onClick = onSolve,
        )
    }
}

@Composable
private fun TabPill(tab: KeyboardTab, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(if (active) Ink else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        Text(
            tab.title,
            color = if (active) SurfaceWhite else TextPrimary,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}

/** 5a: слева цифры 3×4, справа операции 2×4, снизу ряд с курсором и стиранием. */
@Composable
private fun NumbersLayout(onKey: (MathKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DigitRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key -> KeyButton(key, Modifier.weight(1f), onKey = onKey) }
                    }
                }
            }
            Column(
                Modifier.weight(2f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OperationRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key -> KeyButton(key, Modifier.weight(1f), onKey = onKey) }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(MathKey.Insert("x"), Modifier.width(52.dp), fontSize = 19.sp, onKey = onKey)
            KeyButton(MathKey.Insert("y"), Modifier.width(52.dp), fontSize = 19.sp, onKey = onKey)
            KeyButton(MathKey.Left, Modifier.width(52.dp), onKey = onKey)
            KeyButton(MathKey.Right, Modifier.width(52.dp), onKey = onKey)
            KeyButton(MathKey.Backspace, Modifier.weight(1f), onKey = onKey)
        }
    }
}

/** 5b: сетка 4×4 функций и знаков сравнения. */
@Composable
private fun FunctionsLayout(onKey: (MathKey) -> Unit) {
    GridLayout(FunctionKeys, columns = 4, onKey = onKey)
}

/** Кириллическая раскладка для текстовых условий. */
@Composable
private fun LettersLayout(onKey: (MathKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LetterRows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { letter ->
                    KeyButton(
                        key = MathKey.Insert(letter),
                        modifier = Modifier.weight(1f),
                        height = 46.dp,
                        fontSize = 17.sp,
                        onKey = onKey,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyButton(MathKey.Insert(",", ", "), Modifier.width(52.dp), height = 46.dp, onKey = onKey)
            KeyButton(
                key = MathKey.Insert("пробел", " "),
                modifier = Modifier.weight(1f),
                height = 46.dp,
                fontSize = 14.sp,
                onKey = onKey,
            )
            KeyButton(MathKey.Backspace, Modifier.width(74.dp), height = 46.dp, onKey = onKey)
        }
    }
}

@Composable
private fun GridLayout(keys: List<MathKey>, columns: Int, onKey: (MathKey) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> KeyButton(key, Modifier.weight(1f), onKey = onKey) }
                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Клавиша: белая для символов, `#E8E6DE` для операций, красная для стирания. */
@Composable
private fun KeyButton(
    key: MathKey,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    fontSize: TextUnit = 22.sp,
    onKey: (MathKey) -> Unit,
) {
    val isBackspace = key == MathKey.Backspace
    val operation = key.isOperation()
    val background = when {
        isBackspace -> DestructiveTint
        operation -> KeyOperation
        else -> SurfaceWhite
    }
    val shape = RoundedCornerShape(14.dp)
    val label = when (key) {
        is MathKey.Insert -> key.label
        is MathKey.Node -> key.label
        MathKey.Backspace -> "⌫"
        MathKey.Left -> "←"
        MathKey.Right -> "→"
    }
    Box(
        modifier
            .height(height)
            .then(if (background == SurfaceWhite) Modifier.shadow(1.dp, shape) else Modifier)
            .clip(shape)
            .background(background)
            .clickable { onKey(key) },
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            MathKey.Left, MathKey.Right -> Icon(
                if (key == MathKey.Left) PythIcons.ArrowLeft else PythIcons.ArrowRight,
                contentDescription = if (key == MathKey.Left) "Курсор влево" else "Курсор вправо",
                tint = TextPrimary,
                modifier = Modifier.height(17.dp),
            )

            else -> Text(
                label,
                color = if (isBackspace) Destructive else TextPrimary,
                fontFamily = DisplayFont,
                fontSize = if (label.length > 2) fontSize * 0.8f else fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

// ─────────────────────────── Раскладки ───────────────────────────

private val DigitRows: List<List<MathKey>> = listOf(
    listOf("7", "8", "9"),
    listOf("4", "5", "6"),
    listOf("1", "2", "3"),
    listOf("0", ",", "π"),
).map { row -> row.map { MathKey.Insert(it) } }

private val OperationRows: List<List<MathKey>> = listOf(
    listOf(MathKey.Insert("+"), MathKey.Insert("−")),
    listOf(MathKey.Insert("×"), MathKey.Node("a⁄b", MathNode.Frac())),
    listOf(MathKey.Node("xⁿ", MathNode.Sup()), MathKey.Node("√", MathNode.Sqrt())),
    listOf(MathKey.Node("( )", MathNode.Paren()), MathKey.Insert("=")),
)

private val FunctionKeys: List<MathKey> = listOf(
    MathKey.Insert("sin", "sin("),
    MathKey.Insert("cos", "cos("),
    MathKey.Insert("tg", "tg("),
    MathKey.Insert("ctg", "ctg("),
    MathKey.Insert("log", "log("),
    MathKey.Insert("ln", "ln("),
    MathKey.Insert("eˣ", "e^"),
    MathKey.Insert("|x|", "abs("),
    MathKey.Insert("∫"),
    MathKey.Insert("d/dx"),
    MathKey.Insert("Σ"),
    MathKey.Insert("lim"),
    MathKey.Insert("≤"),
    MathKey.Insert("≥"),
    MathKey.Insert("≠"),
    MathKey.Backspace,
)

private val GreekKeys: List<MathKey> = listOf(
    "α", "β", "γ", "δ",
    "θ", "λ", "μ", "π",
    "ρ", "σ", "φ", "ω",
    "Δ", "Ω", "°", "∞",
).map { MathKey.Insert(it) }

private val LetterRows: List<List<String>> = listOf(
    listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х"),
    listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"),
    listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "ъ", "ё"),
)
