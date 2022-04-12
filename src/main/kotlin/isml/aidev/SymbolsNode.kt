package isml.aidev

import isml.aidev.util.Unique
import kotlin.random.Random

data class SymbolsNode(
    val NTs: MutableList<Unique<Symbol.NonTerminal>>,
    val substitutionNTs: List<Unique<Symbol.NonTerminal>> = emptyList(),
    val depth: Int = 0,
    val uuid: Long = Random.nextLong(),
) {
    val currNT = NTs.removeLastOrNull()
    val isFinished: Boolean
        get() = currNT == null

    override fun toString(): String {
        return "SymbolNode(currNT=${currNT}, NTs=${NTs}, depth=${depth})"
    }

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { Unique(it) }
        val newNTs = (NTs + sub).shuffled().toMutableList()
        return SymbolsNode(newNTs, sub, depth + 1)
    }
}