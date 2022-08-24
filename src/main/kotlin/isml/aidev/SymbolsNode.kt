package isml.aidev

import isml.aidev.util.Evaluator

/**
 * Must not be a data-class because we use object references for equality and caching
 */
class SymbolsNode(
    val currNT: Symbol.NonTerminal?,
    val substitutionNTs: List<Symbol.NonTerminal> = emptyList(),
    private val parent: SymbolsNode? = null,
    val localvars: Map<String, Int> = mapOf(),
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
        val newLocalVars = rule.expressions[nextNT?.value]?.let {
            Evaluator.instance().exec(it, localvars)
        } ?: localvars
        return SymbolsNode(nextNT, sub, this, newLocalVars, depth + 1)
    }
}
