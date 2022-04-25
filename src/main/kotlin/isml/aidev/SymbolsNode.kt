package isml.aidev

/**
 * Must not be a data-class because we use object references for equality and caching
 */
class SymbolsNode(
    val currNT: Symbol.NonTerminal?,
    val substitutionNTs: List<Symbol.NonTerminal> = emptyList(),
    private val parent: SymbolsNode? = null,
    val depth: Int = 0,
) {
    private var _remainingNTs: Set<Symbol.NonTerminal>? = null
    val remainingNTs: Set<Symbol.NonTerminal>
        get() {
            // return cached instance if possible
            return _remainingNTs
            // else
            // recursively calculate the remaining non-terminals from the parents
            // get the parents nts or create an empty set
                ?: (parent?.remainingNTs?.toMutableSet() ?: mutableSetOf())
                    .apply {
                        addAll(substitutionNTs)
                        remove(currNT)

                        // storing the result in the field
                        _remainingNTs = this
                    }
        }

    val isFinished: Boolean
        get() = currNT == null

    override fun toString(): String =
        "SymbolNode(currNT=${currNT}, depth=${depth})"

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { it.copy() }.toList()
        val nextNT = (remainingNTs.toList() + sub).randomOrNull()
        return SymbolsNode(nextNT, sub, this, depth + 1)
    }
}
