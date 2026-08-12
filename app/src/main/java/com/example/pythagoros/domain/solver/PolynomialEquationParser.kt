package com.example.pythagoros.domain.solver

import kotlin.math.abs

internal data class Polynomial(
    val coefficients: Map<Int, Double>,
) {
    operator fun plus(other: Polynomial): Polynomial =
        merge(other, scale = 1.0)

    operator fun minus(other: Polynomial): Polynomial =
        merge(other, scale = -1.0)

    operator fun times(other: Polynomial): Polynomial {
        val result = mutableMapOf<Int, Double>()
        coefficients.forEach { (leftPower, leftValue) ->
            other.coefficients.forEach { (rightPower, rightValue) ->
                val power = leftPower + rightPower
                require(power <= MaxSupportedPower) { "Unsupported polynomial power: $power" }
                result[power] = (result[power] ?: 0.0) + leftValue * rightValue
            }
        }
        return Polynomial(result).clean()
    }

    operator fun div(other: Polynomial): Polynomial {
        val denominator = other.constantOrNull()
            ?: throw IllegalArgumentException("Division by non-constant polynomial is unsupported")
        require(abs(denominator) > Epsilon) { "Division by zero" }
        return Polynomial(coefficients.mapValues { (_, value) -> value / denominator }).clean()
    }

    fun coefficient(power: Int): Double =
        coefficients[power] ?: 0.0

    fun degree(): Int =
        coefficients.filterValues { abs(it) > Epsilon }.keys.maxOrNull() ?: 0

    private fun merge(other: Polynomial, scale: Double): Polynomial {
        val result = coefficients.toMutableMap()
        other.coefficients.forEach { (power, value) ->
            result[power] = (result[power] ?: 0.0) + value * scale
        }
        return Polynomial(result).clean()
    }

    private fun constantOrNull(): Double? {
        val cleaned = clean()
        return if (cleaned.coefficients.keys.all { it == 0 }) cleaned.coefficient(0) else null
    }

    private fun clean(): Polynomial =
        Polynomial(coefficients.filterValues { abs(it) > Epsilon })

    companion object {
        fun constant(value: Double): Polynomial =
            Polynomial(if (abs(value) > Epsilon) mapOf(0 to value) else emptyMap())

        fun variable(): Polynomial =
            Polynomial(mapOf(1 to 1.0))
    }
}

internal data class ParsedEquation(
    val polynomial: Polynomial,
    val variable: String,
)

internal data class ParsedPolynomial(
    val polynomial: Polynomial,
    val variable: String,
)

internal data class ParsedIntegral(
    val polynomial: Polynomial,
    val variable: String,
    val integrandSource: String,
)

internal object PolynomialEquationParser {
    fun parseEquation(source: String): ParsedEquation {
        val parts = source.split("=")
        require(parts.size == 2) { "Equation must contain exactly one equals sign" }

        val variable = Regex("""[a-zA-Z]""").find(source)?.value ?: "x"
        val left = Parser(parts[0], variable).parse()
        val right = Parser(parts[1], variable).parse()
        return ParsedEquation(left - right, variable)
    }

    fun parsePolynomial(source: String, variable: String? = null): ParsedPolynomial {
        val resolvedVariable = variable ?: Regex("""[a-zA-Z]""").find(source)?.value ?: "x"
        return ParsedPolynomial(
            polynomial = Parser(source, resolvedVariable).parse(),
            variable = resolvedVariable,
        )
    }
}

internal object PolynomialIntegralParser {
    fun parse(source: String): ParsedIntegral {
        val match = Regex("""^\s*∫\s*(.+)\s+d([a-zA-Z])\s*$""").matchEntire(source)
            ?: throw IllegalArgumentException("Integral must look like ∫ f(x) dx")
        val integrandSource = match.groupValues[1].trim()
        val variable = match.groupValues[2]
        val parsed = PolynomialEquationParser.parsePolynomial(integrandSource, variable)
        return ParsedIntegral(
            polynomial = parsed.polynomial,
            variable = parsed.variable,
            integrandSource = integrandSource,
        )
    }
}

private class Parser(
    source: String,
    private val variable: String,
) {
    private val tokens = tokenize(source)
    private var position = 0

    fun parse(): Polynomial {
        val result = parseExpression()
        require(position == tokens.size) { "Unexpected token" }
        return result
    }

    private fun parseExpression(): Polynomial {
        var result = parseTerm()
        while (true) {
            result = when {
                match("+") -> result + parseTerm()
                match("-") -> result - parseTerm()
                else -> return result
            }
        }
    }

    private fun parseTerm(): Polynomial {
        var result = parsePower()
        while (true) {
            result = when {
                match("*") -> result * parsePower()
                match("/") -> result / parsePower()
                else -> return result
            }
        }
    }

    private fun parsePower(): Polynomial {
        var result = parseUnary()
        while (match("^")) {
            val exponent = parseUnary().coefficient(0)
            require(abs(exponent - exponent.toInt()) < Epsilon) { "Exponent must be an integer" }
            require(exponent.toInt() in 0..MaxSupportedPower) { "Unsupported exponent" }
            result = repeatMultiply(result, exponent.toInt())
        }
        return result
    }

    private fun parseUnary(): Polynomial =
        when {
            match("+") -> parseUnary()
            match("-") -> Polynomial.constant(-1.0) * parseUnary()
            else -> parsePrimary()
        }

    private fun parsePrimary(): Polynomial {
        val token = advanceOrNull() ?: throw IllegalArgumentException("Expression ended unexpectedly")
        return when (token) {
            is Token.Number -> Polynomial.constant(token.value)
            is Token.Identifier -> {
                require(token.value == variable) { "Only one variable is supported" }
                Polynomial.variable()
            }
            is Token.Symbol -> {
                require(token.value == "(") { "Expected number, variable or parenthesis" }
                val nested = parseExpression()
                require(match(")")) { "Unclosed parenthesis" }
                nested
            }
            else -> throw IllegalArgumentException("Expected number, variable or parenthesis")
        }
    }

    private fun repeatMultiply(base: Polynomial, exponent: Int): Polynomial {
        if (exponent == 0) return Polynomial.constant(1.0)
        var result = Polynomial.constant(1.0)
        repeat(exponent) {
            result *= base
        }
        return result
    }

    private fun match(symbol: String): Boolean {
        val current = tokens.getOrNull(position)
        if (current is Token.Symbol && current.value == symbol) {
            position++
            return true
        }
        return false
    }

    private fun advanceOrNull(): Token? =
        tokens.getOrNull(position)?.also { position++ }

    private fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var index = 0
        while (index < source.length) {
            val char = source[index]
            when {
                char.isWhitespace() -> index++
                char.isDigit() || char == '.' -> {
                    val start = index
                    var hasDecimal = false
                    while (index < source.length) {
                        val current = source[index]
                        if (current.isDigit()) {
                            index++
                        } else if (current == '.' && !hasDecimal) {
                            hasDecimal = true
                            index++
                        } else {
                            break
                        }
                    }
                    tokens += Token.Number(source.substring(start, index).toDouble())
                }
                char.isLetter() -> {
                    val start = index
                    while (index < source.length && source[index].isLetter()) {
                        index++
                    }
                    tokens += Token.Identifier(source.substring(start, index))
                }
                char in "+-*/^()" -> {
                    tokens += Token.Symbol(char.toString())
                    index++
                }
                else -> throw IllegalArgumentException("Unsupported token: $char")
            }
        }
        return tokens
    }

    private sealed interface Token {
        data class Number(val value: Double) : Token
        data class Identifier(val value: String) : Token
        data class Symbol(val value: String) : Token
    }
}

internal const val MaxSupportedPower = 2
internal const val Epsilon = 1e-9
