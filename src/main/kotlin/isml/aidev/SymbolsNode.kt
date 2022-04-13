package isml.aidev

import isml.aidev.util.Unique

class SymbolsNode(
    val currNT: Unique<Symbol.NonTerminal>?,
    val substitutionNTs: List<Unique<Symbol.NonTerminal>> = emptyList(),
    private val parent: SymbolsNode? = null,
    val depth: Int = 0,
) {
    private var _remainingNTs: Set<Unique<Symbol.NonTerminal>>? = null
    val remainingNTs: Set<Unique<Symbol.NonTerminal>>
        get() {
            if (_remainingNTs != null)
                return _remainingNTs!!
            return run {
                // recursively calculate the remaining Nonterminals from the parents
                val nts = parent?.remainingNTs?.toMutableSet() ?: mutableSetOf()
                nts.apply {
                    addAll(substitutionNTs)
                    remove(currNT)
                }
            }.apply { _remainingNTs = this }
        }

    val isFinished: Boolean
        get() = currNT == null

    override fun toString(): String {
        return "SymbolNode(currNT=${currNT}, depth=${depth})"
    }

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { Unique(it) }.toList()
        val nextNT = (remainingNTs.toList() + sub).randomOrNull()
        return SymbolsNode(nextNT, sub, this, depth + 1)
    }
}
