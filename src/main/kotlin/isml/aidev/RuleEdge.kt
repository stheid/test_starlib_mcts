package isml.aidev

class RuleEdge(
    val substitution: List<Symbol>,
    val expressions: Map<Symbol.NonTerminal, String> = mapOf(),
    val weight: Float = 1.0f)
{
    override fun toString() = " -> $substitution$expressions"

    val isExtending get() = substitution.filterIsInstance<Symbol.NonTerminal>().size > 1
}