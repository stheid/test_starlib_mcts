package isml.aidev

/**
 * Must not be a data-class because we use object references for equality and caching
 */
class SymbolsNode(
    val currNT: Symbol.NonTerminal?,
    val substitutionNTs: List<Symbol.NonTerminal> = emptyList(),
    private val parent: SymbolsNode? = null,
    val localvars: Map<String, Int>? = null,
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

    fun createChild(rule: RuleEdge, childsLocalVars:Map<String, Int>? = null): SymbolsNode {
        val sub = rule.substitution.filterIsInstance<Symbol.NonTerminal>().map { it.copy() }.toList()
        val nextNT = (remainingNTs.toList() + sub).randomOrNull()
        return SymbolsNode(nextNT, sub, this, if (nextNT != null) childsLocalVars else null, depth + 1)
    }
}
