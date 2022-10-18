package isml.aidev

import isml.aidev.grammar.Symbol

class RuleEdge(
    val substitution: List<Symbol>,
    val expression: String? = null,
    val weight: Float = 1.0f,
) {
    override fun toString() = "$substitution${expression ?: ""} (${weight})"

    val isExtending get() = substitution.filterIsInstance<Symbol.NonTerminal>().size > 1
}