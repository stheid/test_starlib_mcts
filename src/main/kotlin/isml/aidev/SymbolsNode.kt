package isml.aidev

import isml.aidev.util.Chain

data class SymbolsNode(
    val NTs: ArrayList<Symbol.UniqueNT>,
    val root: SymbolsNode? = null,
    val depth: Int = 0,
) {
    val currNT = NTs.removeLastOrNull()
    private var substitution: List<Symbol>? = null
    private var succ: SymbolsNode? = null

    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.map { if (it is Symbol.NonTerminal) Symbol.UniqueNT(it) else it }

        val child = SymbolsNode(
            (NTs + sub.filterIsInstance<Symbol.UniqueNT>().shuffled()) as ArrayList<Symbol.UniqueNT>,
            root ?: this,
            depth + 1
        )
        substitution = sub
        succ = child
        return child
    }


    fun toWord(): String {
        if (!isFinished)
            return ""

        var node = root ?: this
        val symbol = node.currNT!!
        val symbols = Chain(listOf(symbol as Symbol))
        val nts = hashMapOf(symbol to symbols.linkIterator().asSequence().first())

        while (node.succ != null) {
            // dereference chainlink (GC) and prepare for substitution
            val linkToSubstitute = nts.remove(node.currNT!!)!!

            // for non-ε rules:
            if (node.substitution!!.isNotEmpty()) {
                val substChain = Chain(node.substitution!!)

                // store references to chainlinks containing non-terminals
                substChain.linkIterator().asSequence().filter { it.value is Symbol.UniqueNT }.forEach {
                    nts[it.value as Symbol.UniqueNT] = it
                }

                // substitute chainlink with chain
                linkToSubstitute.substitute(substChain)
            }

            node = node.succ!!
        }

        // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
        return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
    }
}