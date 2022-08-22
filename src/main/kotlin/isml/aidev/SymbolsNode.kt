package isml.aidev

/**
 * Must not be a data-class because we use object references for equality and caching
 */
class SymbolsNode(
    val currNT: Symbol.NonTerminal?,
    val substitutionNTs: List<Symbol.NonTerminal> = emptyList(),
    private val parent: SymbolsNode? = null,
    val vars: MutableMap<String, Int> = mutableMapOf(),
    val depth: Int = 0,
) {
    val isFinished
        get() = currNT == null

    val remainingNTs: Set<Symbol.NonTerminal> by lazy {
        // recursively calculate the remaining non-terminals from the parents
        // get the parents nts or create an empty set
        (parent?.remainingNTs?.toMutableSet() ?: mutableSetOf())
            .apply {
                addAll(substitutionNTs)
                remove(currNT)
            }
    }

    override fun toString() =
        "SymbolNode(currNT=${currNT}, depth=${depth})"

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { it.copy() }.toList()
        val nextNT = (remainingNTs.toList() + sub).randomOrNull()
        // TODO Evaluate Expression
        return SymbolsNode(nextNT, sub, this, depth = depth + 1)
    }
}
