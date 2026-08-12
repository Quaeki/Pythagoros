package com.example.pythagoros.domain.usecase

import com.example.pythagoros.domain.model.Expression
import javax.inject.Inject

/**
 * Разбирает строку, введённую или поправленную пользователем на экране «Проверьте условие».
 *
 * Возвращает [Result], а не `Flow`: разбор синхронный и мгновенный, наблюдать тут не за чем.
 * Неудача — это синтаксическая ошибка ввода, её показывает то же поле формулы.
 */
class ParseExpressionUseCase @Inject constructor() {

    operator fun invoke(raw: String): Result<Expression> =
        runCatching {
            val normalized = MathExpressionParser.normalize(raw)
            MathExpressionParser.validate(normalized)
            Expression(normalized)
        }
}

class ParseExpressionException(message: String) : IllegalArgumentException(message)

private object MathExpressionParser {
    private val superscripts = mapOf(
        '⁰' to '0',
        '¹' to '1',
        '²' to '2',
        '³' to '3',
        '⁴' to '4',
        '⁵' to '5',
        '⁶' to '6',
        '⁷' to '7',
        '⁸' to '8',
        '⁹' to '9',
    )

    fun normalize(raw: String): String {
        val compact = raw
            .trim()
            .replace('−', '-')
            .replace('–', '-')
            .replace('—', '-')
            .replace('×', '*')
            .replace('·', '*')
            .replace('÷', '/')
            .replace('√', '√')
            .replace("≤", "<=")
            .replace("≥", ">=")
            .replace(Regex("\\s+"), " ")

        if (compact.isBlank()) {
            throw ParseExpressionException("Введите выражение")
        }

        val withPowers = StringBuilder()
        var index = 0
        while (index < compact.length) {
            val current = compact[index]
            if (current in superscripts) {
                withPowers.append('^')
                while (index < compact.length && compact[index] in superscripts) {
                    withPowers.append(superscripts.getValue(compact[index]))
                    index++
                }
                continue
            }
            withPowers.append(current)
            index++
        }

        return insertImplicitMultiplication(expandAsciiVariablePowers(tokenize(withPowers.toString())))
            .joinToString(" ") { token ->
                when (token) {
                    is Token.Number -> token.value
                    is Token.Identifier -> token.value
                    is Token.Function -> token.value
                    is Token.Operator -> token.value
                    Token.Integral -> "∫"
                    is Token.Differential -> "d${token.variable}"
                    Token.LeftParen -> "("
                    Token.RightParen -> ")"
                    Token.Comma -> ","
                    is Token.Comparator -> token.value
                }
            }
            .replace("( ", "(")
            .replace(" )", ")")
            .replace(" ,", ",")
            .replace(Regex("""\b(sqrt|sin|cos|tan|ctg|ln|log|abs|exp) \("""), "$1(")
            .trim()
    }

    fun validate(normalized: String) {
        val tokens = tokenize(normalized)
        if (tokens.isEmpty()) {
            throw ParseExpressionException("Введите выражение")
        }

        if (tokens.any { it == Token.Integral }) {
            validateIntegral(tokens)
            return
        }

        val parts = splitByTopLevelComparators(tokens)
        if (parts.size > 2) {
            throw ParseExpressionException("В выражении слишком много знаков сравнения")
        }
        parts.forEach { part ->
            if (part.isEmpty()) {
                throw ParseExpressionException("Одна из частей выражения пустая")
            }
            Parser(part).parse()
        }
    }

    private fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < source.length) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++
                char.isDigit() || char == '.' || char == ',' && source.getOrNull(index + 1)?.isDigit() == true -> {
                    val start = index
                    var hasDecimal = false
                    if (char == ',') {
                        hasDecimal = true
                        index++
                    }
                    while (index < source.length) {
                        val current = source[index]
                        if (current.isDigit()) {
                            index++
                        } else if ((current == '.' || current == ',') && !hasDecimal) {
                            hasDecimal = true
                            index++
                        } else {
                            break
                        }
                    }
                    val number = source.substring(start, index).replace(',', '.')
                    if (number == "." || number == ",") {
                        throw ParseExpressionException("Некорректное число")
                    }
                    tokens += Token.Number(number)
                }
                char.isLetter() || char == '_' || char == 'π' -> {
                    val start = index
                    while (index < source.length && (source[index].isLetter() || source[index] == '_' || source[index] == 'π')) {
                        index++
                    }
                    val value = source.substring(start, index)
                    tokens += if (value.lowercase() in knownFunctions) {
                        Token.Function(value.lowercase())
                    } else if (value.length == 2 && value[0] == 'd' && value[1].isLetter()) {
                        Token.Differential(value[1].toString())
                    } else {
                        Token.Identifier(value)
                    }
                }
                char == '∫' -> {
                    tokens += Token.Integral
                    index++
                }
                char == '√' -> {
                    tokens += Token.Function("sqrt")
                    index++
                }
                char == '(' -> {
                    tokens += Token.LeftParen
                    index++
                }
                char == ')' -> {
                    tokens += Token.RightParen
                    index++
                }
                char == ',' -> {
                    tokens += Token.Comma
                    index++
                }
                char in "+-*/^" -> {
                    tokens += Token.Operator(char.toString())
                    index++
                }
                char == '=' -> {
                    tokens += Token.Comparator("=")
                    index++
                }
                char == '<' || char == '>' -> {
                    val next = source.getOrNull(index + 1)
                    if (next == '=') {
                        tokens += Token.Comparator("$char=")
                        index += 2
                    } else {
                        tokens += Token.Comparator(char.toString())
                        index++
                    }
                }
                else -> throw ParseExpressionException("Неподдерживаемый символ: $char")
            }
        }
        return tokens
    }

    private fun expandAsciiVariablePowers(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens

        val result = mutableListOf<Token>()
        var index = 0
        while (index < tokens.size) {
            val current = tokens[index]
            val next = tokens.getOrNull(index + 1)
            if (
                current is Token.Identifier &&
                current.value.length == 1 &&
                next is Token.Number &&
                next.value.matches(Regex("""[2-9]"""))
            ) {
                result += current
                result += Token.Operator("^")
                result += next
                index += 2
            } else {
                result += current
                index++
            }
        }
        return result
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens

        val result = mutableListOf<Token>()
        tokens.forEachIndexed { index, token ->
            val previous = result.lastOrNull()
            if (previous != null && needsMultiplication(previous, token)) {
                result += Token.Operator("*")
            }
            result += token
        }
        return result
    }

    private fun needsMultiplication(left: Token, right: Token): Boolean {
        val leftCanEnd = left is Token.Number || left is Token.Identifier || left == Token.RightParen
        val rightCanStart = right is Token.Number || right is Token.Identifier || right is Token.Function || right == Token.LeftParen

        if (!leftCanEnd || !rightCanStart) return false
        if (left is Token.Function) return false
        if (left == Token.Integral || right == Token.Integral) return false
        if (left is Token.Differential || right is Token.Differential) return false
        if (left is Token.Operator || right is Token.Operator) return false
        if (left is Token.Comparator || right is Token.Comparator) return false
        return true
    }

    private fun validateIntegral(tokens: List<Token>) {
        if (tokens.count { it == Token.Integral } != 1 || tokens.first() != Token.Integral) {
            throw ParseExpressionException("Интеграл должен начинаться со знака ∫")
        }
        if (tokens.any { it is Token.Comparator }) {
            throw ParseExpressionException("Интеграл не должен содержать знак сравнения")
        }

        val differential = tokens.lastOrNull() as? Token.Differential
            ?: throw ParseExpressionException("Укажите дифференциал, например dx")
        if (differential.variable.isBlank()) {
            throw ParseExpressionException("Укажите переменную интегрирования")
        }

        val integrand = tokens.drop(1).dropLast(1)
        if (integrand.isEmpty()) {
            throw ParseExpressionException("Подынтегральное выражение пустое")
        }
        Parser(integrand).parse()
    }

    private fun splitByTopLevelComparators(tokens: List<Token>): List<List<Token>> {
        val parts = mutableListOf<MutableList<Token>>(mutableListOf())
        var depth = 0
        tokens.forEach { token ->
            when (token) {
                Token.LeftParen -> {
                    depth++
                    parts.last() += token
                }
                Token.RightParen -> {
                    depth--
                    if (depth < 0) throw ParseExpressionException("Лишняя закрывающая скобка")
                    parts.last() += token
                }
                is Token.Comparator -> {
                    if (depth == 0) {
                        parts.add(mutableListOf())
                    } else {
                        parts.last() += token
                    }
                }
                else -> parts.last() += token
            }
        }
        if (depth != 0) {
            throw ParseExpressionException("Скобки не закрыты")
        }
        return parts
    }

    private val knownFunctions = setOf(
        "sqrt",
        "sin",
        "cos",
        "tan",
        "ctg",
        "ln",
        "log",
        "abs",
        "exp",
    )

    private sealed interface Token {
        data class Number(val value: String) : Token
        data class Identifier(val value: String) : Token
        data class Function(val value: String) : Token
        data class Operator(val value: String) : Token
        data class Comparator(val value: String) : Token
        data class Differential(val variable: String) : Token
        data object Integral : Token
        data object LeftParen : Token
        data object RightParen : Token
        data object Comma : Token
    }

    private class Parser(
        private val tokens: List<Token>,
    ) {
        private var position = 0

        fun parse() {
            parseExpression()
            if (!isAtEnd()) {
                throw ParseExpressionException("Лишний токен в выражении")
            }
        }

        private fun parseExpression() {
            parseTerm()
            while (matchOperator("+", "-")) {
                parseTerm()
            }
        }

        private fun parseTerm() {
            parsePower()
            while (matchOperator("*", "/")) {
                parsePower()
            }
        }

        private fun parsePower() {
            parseUnary()
            while (matchOperator("^")) {
                parseUnary()
            }
        }

        private fun parseUnary() {
            if (matchOperator("+", "-")) {
                parseUnary()
            } else {
                parsePrimary()
            }
        }

        private fun parsePrimary() {
            when (val token = advanceOrNull()) {
                is Token.Number -> Unit
                is Token.Identifier -> Unit
                is Token.Function -> {
                    if (!match<Token.LeftParen>()) {
                        throw ParseExpressionException("После функции ${token.value} нужны скобки")
                    }
                    parseFunctionArguments()
                }
                Token.LeftParen -> {
                    parseExpression()
                    if (!match<Token.RightParen>()) {
                        throw ParseExpressionException("Скобки не закрыты")
                    }
                }
                null -> throw ParseExpressionException("Выражение оборвано")
                else -> throw ParseExpressionException("Ожидалось число, переменная или скобка")
            }
        }

        private fun parseFunctionArguments() {
            parseExpression()
            while (match<Token.Comma>()) {
                parseExpression()
            }
            if (!match<Token.RightParen>()) {
                throw ParseExpressionException("Аргументы функции не закрыты")
            }
        }

        private fun matchOperator(vararg operators: String): Boolean {
            val token = current()
            if (token is Token.Operator && token.value in operators) {
                position++
                return true
            }
            return false
        }

        private inline fun <reified T : Token> match(): Boolean {
            if (current() is T) {
                position++
                return true
            }
            return false
        }

        private fun advanceOrNull(): Token? =
            if (isAtEnd()) null else tokens[position++]

        private fun current(): Token? =
            tokens.getOrNull(position)

        private fun isAtEnd(): Boolean =
            position >= tokens.size
    }
}
