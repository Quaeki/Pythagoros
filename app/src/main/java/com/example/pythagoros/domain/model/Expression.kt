package com.example.pythagoros.domain.model

/**
 * Математическое выражение задачи в нормализованной записи.
 *
 * Пока это обёртка над строкой: разбор в дерево появится вместе с символьным ядром,
 * и тогда здесь добавится сам разобранный узел, а [source] останется для показа на экране.
 */
@JvmInline
value class Expression(val source: String) {
    override fun toString(): String = source
}
