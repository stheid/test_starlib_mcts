package isml.aidev

class RuleEdge(val substitution: List<Symbol>, val weight: Float = 1.0f) {
    override fun toString() = " -> $substitution"

    val isExtending get() = substitution.filterIsInstance<Symbol.NonTerminal>().size > 1
}