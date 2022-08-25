package isml.aidev

class RuleEdge(
    val substitution: List<Symbol>,
    val expression: String? = null,
    val weight: Float = 1.0f,
) {
    override fun toString() = " -> $substitution${expression ?: ""}"

    val isExtending get() = substitution.filterIsInstance<Symbol.NonTerminal>().size > 1
}