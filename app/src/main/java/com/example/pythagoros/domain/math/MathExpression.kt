package com.example.pythagoros.domain.math

/**
 * Выражение, которое пользователь набирает математической клавиатурой (макеты 5a, 5b).
 *
 * Это дерево, а не строка: дробь и степень — узлы со своими слотами, поэтому клавиша «a⁄b»
 * вставляет пустую дробь, а стрелки ходят по слотам, а не по символам.
 */
data class MathRow(val nodes: List<MathNode> = emptyList()) {
    val isEmpty: Boolean get() = nodes.isEmpty()
}

sealed interface MathNode {
    /** Сколько слотов у узла: 0 — атом, 1 — корень/степень/скобки, 2 — дробь. */
    val slotCount: Int

    /** Атом: цифра, буква, знак операции, имя функции. */
    data class Sym(val text: String) : MathNode {
        override val slotCount: Int get() = 0
    }

    /** Дробь: слот 0 — числитель, слот 1 — знаменатель. */
    data class Frac(val top: MathRow = MathRow(), val bottom: MathRow = MathRow()) : MathNode {
        override val slotCount: Int get() = 2
    }

    /** Степень: показатель в верхнем индексе, основание — то, что стоит слева. */
    data class Sup(val exponent: MathRow = MathRow()) : MathNode {
        override val slotCount: Int get() = 1
    }

    data class Sqrt(val body: MathRow = MathRow()) : MathNode {
        override val slotCount: Int get() = 1
    }

    data class Paren(val body: MathRow = MathRow()) : MathNode {
        override val slotCount: Int get() = 1
    }
}

/** Шаг пути к слоту: индекс узла в текущей строке и номер его слота. */
data class MathSlot(val node: Int, val slot: Int)

/**
 * Курсор: путь до строки, в которой он стоит, и позиция между её узлами
 * (0 — перед первым узлом, `nodes.size` — после последнего).
 */
data class MathCursor(val path: List<MathSlot> = emptyList(), val index: Int = 0)

/** Состояние поля ввода: дерево плюс позиция курсора. */
data class MathState(
    val root: MathRow = MathRow(),
    val cursor: MathCursor = MathCursor(),
) {
    val isEmpty: Boolean get() = root.isEmpty
}

// ─────────────────────────── Чтение дерева ───────────────────────────

/** Строка, лежащая по пути [path]. */
fun MathRow.rowAt(path: List<MathSlot>): MathRow {
    var row = this
    for (step in path) {
        row = row.nodes.getOrNull(step.node)?.slot(step.slot) ?: return MathRow()
    }
    return row
}

private fun MathNode.slot(index: Int): MathRow? = when (this) {
    is MathNode.Sym -> null
    is MathNode.Frac -> if (index == 0) top else bottom
    is MathNode.Sup -> exponent
    is MathNode.Sqrt -> body
    is MathNode.Paren -> body
}

private fun MathNode.withSlot(index: Int, row: MathRow): MathNode = when (this) {
    is MathNode.Sym -> this
    is MathNode.Frac -> if (index == 0) copy(top = row) else copy(bottom = row)
    is MathNode.Sup -> copy(exponent = row)
    is MathNode.Sqrt -> copy(body = row)
    is MathNode.Paren -> copy(body = row)
}

/** Заменить строку по пути [path] на [newRow]. */
fun MathRow.replaceRowAt(path: List<MathSlot>, newRow: MathRow): MathRow {
    if (path.isEmpty()) return newRow
    val step = path.first()
    val node = nodes.getOrNull(step.node) ?: return this
    val inner = node.slot(step.slot) ?: return this
    val updated = node.withSlot(step.slot, inner.replaceRowAt(path.drop(1), newRow))
    return MathRow(nodes.toMutableList().also { it[step.node] = updated })
}

// ─────────────────────────── Редактирование ───────────────────────────

/**
 * Вставить узел в позицию курсора.
 * У узла со слотами курсор уезжает внутрь первого слота — так набор идёт без лишних нажатий.
 */
fun MathState.insert(node: MathNode): MathState {
    val row = root.rowAt(cursor.path)
    val nodes = row.nodes.toMutableList()
    val at = cursor.index.coerceIn(0, nodes.size)
    nodes.add(at, node)
    val newRoot = root.replaceRowAt(cursor.path, MathRow(nodes))
    val newCursor = if (node.slotCount > 0) {
        MathCursor(cursor.path + MathSlot(at, 0), 0)
    } else {
        cursor.copy(index = at + 1)
    }
    return MathState(newRoot, newCursor)
}

/** Вставить последовательность символов: «sin», «lim», число из нескольких цифр. */
fun MathState.insertText(text: String): MathState =
    text.fold(this) { state, char -> state.insert(MathNode.Sym(char.toString())) }

/**
 * Удалить символ слева от курсора.
 * Если слева ничего нет, курсор выходит из слота наружу — и следующее нажатие
 * удалит сам узел (дробь, корень) целиком.
 */
fun MathState.backspace(): MathState {
    val row = root.rowAt(cursor.path)
    if (cursor.index > 0) {
        val nodes = row.nodes.toMutableList()
        nodes.removeAt(cursor.index - 1)
        return MathState(
            root.replaceRowAt(cursor.path, MathRow(nodes)),
            cursor.copy(index = cursor.index - 1),
        )
    }
    if (cursor.path.isEmpty()) return this
    // Курсор в начале слота — выходим наружу, вставая перед самим узлом.
    val parentPath = cursor.path.dropLast(1)
    val step = cursor.path.last()
    return MathState(root, MathCursor(parentPath, step.node))
}

/** Шаг курсора влево: внутрь предыдущего узла, если у него есть слоты. */
fun MathState.moveLeft(): MathState {
    val row = root.rowAt(cursor.path)
    if (cursor.index > 0) {
        val previous = row.nodes[cursor.index - 1]
        if (previous.slotCount > 0) {
            val slot = previous.slotCount - 1
            val innerPath = cursor.path + MathSlot(cursor.index - 1, slot)
            return MathState(root, MathCursor(innerPath, root.rowAt(innerPath).nodes.size))
        }
        return MathState(root, cursor.copy(index = cursor.index - 1))
    }
    if (cursor.path.isEmpty()) return this
    val step = cursor.path.last()
    val parentPath = cursor.path.dropLast(1)
    // Из второго слота переходим в первый (из знаменателя в числитель), иначе наружу.
    return if (step.slot > 0) {
        val innerPath = parentPath + MathSlot(step.node, step.slot - 1)
        MathState(root, MathCursor(innerPath, root.rowAt(innerPath).nodes.size))
    } else {
        MathState(root, MathCursor(parentPath, step.node))
    }
}

/** Шаг курсора вправо — зеркально [moveLeft]. */
fun MathState.moveRight(): MathState {
    val row = root.rowAt(cursor.path)
    if (cursor.index < row.nodes.size) {
        val next = row.nodes[cursor.index]
        if (next.slotCount > 0) {
            return MathState(root, MathCursor(cursor.path + MathSlot(cursor.index, 0), 0))
        }
        return MathState(root, cursor.copy(index = cursor.index + 1))
    }
    if (cursor.path.isEmpty()) return this
    val step = cursor.path.last()
    val parentPath = cursor.path.dropLast(1)
    val parentRow = root.rowAt(parentPath)
    val parentNode = parentRow.nodes.getOrNull(step.node)
    return if (parentNode != null && step.slot + 1 < parentNode.slotCount) {
        MathState(root, MathCursor(parentPath + MathSlot(step.node, step.slot + 1), 0))
    } else {
        MathState(root, MathCursor(parentPath, step.node + 1))
    }
}

// ─────────────────────────── Вывод ───────────────────────────

/**
 * Плоская запись для решателя: дробь становится `(a)/(b)`, степень — `^(n)`.
 * Знаки приводим к ASCII, чтобы разбор не зависел от того, какой минус нажали.
 */
fun MathRow.toSource(): String = nodes.joinToString("") { node ->
    when (node) {
        is MathNode.Sym -> node.text.toSourceSymbol()
        is MathNode.Frac -> "(${node.top.toSource()})/(${node.bottom.toSource()})"
        is MathNode.Sup -> "^(${node.exponent.toSource()})"
        is MathNode.Sqrt -> "sqrt(${node.body.toSource()})"
        is MathNode.Paren -> "(${node.body.toSource()})"
    }
}

private fun String.toSourceSymbol(): String = when (this) {
    "×" -> "*"
    "·" -> "*"
    "−" -> "-"
    "," -> "."
    "π" -> "pi"
    "≤" -> "<="
    "≥" -> ">="
    "≠" -> "!="
    else -> this
}

/** Читаемая запись для истории и чипов-подсказок. */
fun MathRow.toDisplayText(): String = nodes.joinToString("") { node ->
    when (node) {
        is MathNode.Sym -> node.text
        is MathNode.Frac -> "${node.top.toDisplayText()}/${node.bottom.toDisplayText()}"
        is MathNode.Sup -> node.exponent.toDisplayText().toSuperscript()
        is MathNode.Sqrt -> "√(${node.body.toDisplayText()})"
        is MathNode.Paren -> "(${node.body.toDisplayText()})"
    }
}

private fun String.toSuperscript(): String = map { char ->
    when (char) {
        '0' -> '⁰'; '1' -> '¹'; '2' -> '²'; '3' -> '³'; '4' -> '⁴'
        '5' -> '⁵'; '6' -> '⁶'; '7' -> '⁷'; '8' -> '⁸'; '9' -> '⁹'
        'n' -> 'ⁿ'; '-' -> '⁻'
        else -> char
    }
}.joinToString("")

/** Разобрать готовую строку в дерево — вход из OCR и из истории. */
fun mathStateFromText(text: String): MathState {
    val nodes = text.map { MathNode.Sym(it.toString()) }
    return MathState(MathRow(nodes), MathCursor(emptyList(), nodes.size))
}
