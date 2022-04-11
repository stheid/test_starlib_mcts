package isml.aidev

import isml.aidev.util.Unique

data class SymbolsNode(
    val NTs: ArrayList<Unique<Symbol.NonTerminal>>,
    val substitutionNTs: List<Unique<Symbol.NonTerminal>> = emptyList(),
    val depth: Int = 0,
) {
    val currNT = NTs.removeLastOrNull()
    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { Unique(it) }
        val newNTs = (NTs + sub).shuffled()
        return SymbolsNode(newNTs as ArrayList<Unique<Symbol.NonTerminal>>,sub, depth + 1)
    }
}