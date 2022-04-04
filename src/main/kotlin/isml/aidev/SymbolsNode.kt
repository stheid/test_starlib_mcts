package isml.aidev

import isml.aidev.util.Unique

data class SymbolsNode(val NTs: ArrayList<Unique<Symbol.NonTerminal>>) {
    val currNT = NTs.removeLastOrNull()
    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: RuleEdge): SymbolsNode {
        val newNTs = NTs + rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { Unique(it) }.shuffled()
        return SymbolsNode(newNTs as ArrayList<Unique<Symbol.NonTerminal>>)
    }
}