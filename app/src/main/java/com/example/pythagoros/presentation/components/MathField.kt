package com.example.pythagoros.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pythagoros.domain.math.MathCursor
import com.example.pythagoros.domain.math.MathNode
import com.example.pythagoros.domain.math.MathRow
import com.example.pythagoros.domain.math.MathSlot
import com.example.pythagoros.domain.math.MathState
import com.example.pythagoros.ui.theme.Accent
import com.example.pythagoros.ui.theme.DisplayFont
import com.example.pythagoros.ui.theme.PlaceholderInk
import com.example.pythagoros.ui.theme.TextPrimary

/**
 * Поле ввода формулы (макеты 5a, 5b): рисует дерево выражения и курсор в нём.
 *
 * Это не `TextField` — дробь и степень занимают два этажа, поэтому строка собирается
 * из вложенных `Row`/`Column`, а курсор ставится между узлами по своему пути.
 */
@Composable
fun MathFieldView(
    state: MathState,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 26.sp,
    placeholder: String? = null,
) {
    Box(modifier) {
        if (state.isEmpty && placeholder != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Caret(fontSize)
                Text(
                    " $placeholder",
                    color = PlaceholderInk,
                    fontFamily = DisplayFont,
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        } else {
            MathRowView(
                row = state.root,
                path = emptyList(),
                cursor = state.cursor,
                fontSize = fontSize,
            )
        }
    }
}

@Composable
private fun MathRowView(
    row: MathRow,
    path: List<MathSlot>,
    cursor: MathCursor,
    fontSize: TextUnit,
) {
    val cursorHere = cursor.path == path
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        if (row.isEmpty && !cursorHere) {
            EmptySlot(fontSize)
            return@Row
        }
        row.nodes.forEachIndexed { index, node ->
            if (cursorHere && cursor.index == index) Caret(fontSize)
            MathNodeView(
                node = node,
                path = path + MathSlot(index, 0),
                nodeIndex = index,
                parentPath = path,
                cursor = cursor,
                fontSize = fontSize,
            )
        }
        if (cursorHere && cursor.index >= row.nodes.size) Caret(fontSize)
    }
}

@Composable
private fun MathNodeView(
    node: MathNode,
    path: List<MathSlot>,
    nodeIndex: Int,
    parentPath: List<MathSlot>,
    cursor: MathCursor,
    fontSize: TextUnit,
) {
    val nested = fontSize * 0.78f
    when (node) {
        is MathNode.Sym -> Text(
            node.text,
            color = TextPrimary,
            fontFamily = DisplayFont,
            fontSize = fontSize,
            lineHeight = fontSize * 1.4f,
            fontWeight = FontWeight.SemiBold,
        )

        is MathNode.Frac -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 3.dp),
        ) {
            MathRowView(node.top, parentPath + MathSlot(nodeIndex, 0), cursor, nested)
            Box(
                Modifier
                    .padding(vertical = 3.dp)
                    .height(2.dp)
                    .defaultMinSize(minWidth = fontSize.value.dp * 0.8f)
                    .background(TextPrimary)
            )
            MathRowView(node.bottom, parentPath + MathSlot(nodeIndex, 1), cursor, nested)
        }

        is MathNode.Sup -> Box(
            Modifier.padding(bottom = fontSize.value.dp * 0.5f),
        ) {
            MathRowView(node.exponent, parentPath + MathSlot(nodeIndex, 0), cursor, nested)
        }

        is MathNode.Sqrt -> Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "√",
                color = TextPrimary,
                fontFamily = DisplayFont,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
            )
            Box(
                Modifier
                    .padding(start = 1.dp)
                    .border(width = 2.dp, color = TextPrimary, shape = TopLineShape)
                    .padding(top = 4.dp, start = 3.dp, end = 3.dp)
            ) {
                MathRowView(node.body, parentPath + MathSlot(nodeIndex, 0), cursor, fontSize)
            }
        }

        is MathNode.Paren -> Row(verticalAlignment = Alignment.CenterVertically) {
            Bracket("(", fontSize)
            MathRowView(node.body, parentPath + MathSlot(nodeIndex, 0), cursor, fontSize)
            Bracket(")", fontSize)
        }
    }
}

/** Обводка только сверху — подкоренная черта. */
private val TopLineShape = RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp)

@Composable
private fun Bracket(text: String, fontSize: TextUnit) = Text(
    text,
    color = TextPrimary,
    fontFamily = DisplayFont,
    fontSize = fontSize,
    fontWeight = FontWeight.Normal,
)

/** Мигающая полоска курсора — 2×30 в макете. */
@Composable
private fun Caret(fontSize: TextUnit) {
    val transition = rememberInfiniteTransition(label = "caret")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    Box(
        Modifier
            .width(2.dp)
            .height(fontSize.value.dp * 1.15f)
            .alpha(alpha)
            .background(Accent)
    )
}

/** Пустой слот дроби или корня — видно, куда набирать. */
@Composable
private fun EmptySlot(fontSize: TextUnit) = Box(
    Modifier
        .width(fontSize.value.dp * 0.6f)
        .height(fontSize.value.dp * 0.95f)
        .clip(RoundedCornerShape(4.dp))
        .background(PlaceholderInk.copy(alpha = 0.35f))
)
