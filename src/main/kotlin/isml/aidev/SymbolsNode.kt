package isml.aidev

import isml.aidev.util.Unique
import kotlin.random.Random

data class SymbolsNode(
    val currNT: Unique<Symbol.NonTerminal>?,
    //val NTs: MutableList<Unique<Symbol.NonTerminal>>,
    val substitutionNTs: List<Unique<Symbol.NonTerminal>> = emptyList(),
    val parent: SymbolsNode? = null,
    val depth: Int = 0,
    val uuid: Long = Random.nextLong(),
) {
    val remainingNTs: Set<Unique<Symbol.NonTerminal>>
        get() {
            // recursively calculate the remaining Nonterminals from the parents
            val _remainingNTs = substitutionNTs.toMutableSet().apply { remove(currNT) }
            parent?.let { _parent ->
                _parent.remainingNTs.let { _remainingNTs.addAll(it) }
                _parent.currNT?.let { _remainingNTs.remove(it) }
            }
            return _remainingNTs
        }

    val isFinished: Boolean
        get() = currNT == null

    override fun toString(): String {
        return "SymbolNode(currNT=${currNT}, depth=${depth})"
    }

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { Unique(it) }.toList()
        val nextNT = (remainingNTs + sub).shuffled().toMutableList().removeFirstOrNull()
        return SymbolsNode(nextNT, sub, this, depth + 1)
    }
}