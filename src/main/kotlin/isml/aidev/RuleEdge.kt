package isml.aidev

class RuleEdge(val substitution: List<Pair<Symbol, String>>, val weight: Float = 1.0f) {
    override fun toString() = " -> $substitution"

    val isExtending get() = substitution.filterIsInstance<Symbol.NonTerminal>().size > 1
}