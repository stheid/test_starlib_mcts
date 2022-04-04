package isml.aidev

import isml.aidev.util.Chain
import isml.aidev.util.ChainLink
import isml.aidev.util.Unique

data class SymbolsNode(
    val NTs: ArrayList<Unique<Symbol.NonTerminal>>,
    val root: SymbolsNode? = null,
    val depth: Int = 0,
) {
    val currNT = NTs.removeLastOrNull()
    private var substitution: List<Unique<Symbol>>? = null
    private var succ: SymbolsNode? = null

    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: RuleEdge): SymbolsNode {
        val sub = rule.substitution.map { Unique(it) }

        val child = SymbolsNode(
            (NTs + sub.filterIsInstance<Unique<Symbol.NonTerminal>>()
                .shuffled()) as ArrayList<Unique<Symbol.NonTerminal>>,
            root ?: this,
            depth + 1
        )
        substitution = sub
        succ = child
        return child
    }


    fun toWord(): String {
        //todo maybe this whole function should operate on a solution path (SEARCHGRAPH PATH)

        if (!isFinished)
            return ""

        // TODO actually the "succ" reference does not work, as we talk about a tree. we need to do it with parent references. this also eliminates the need of a root reference

        var node = root ?: this
        val symbol = node.currNT!!
        val symbols = Chain(listOf<Unique<out Symbol>>(symbol))
        val nts = hashMapOf<Unique<Symbol.NonTerminal>,ChainLink<Unique<out Symbol>>>(symbol to symbols.linkIterator().asSequence().first())

        while (node.succ != null) {
            // dereference chainlink (GC) and prepare for substitution
            val linkToSubstitute = nts.remove(node.currNT!!)!!

            // for non-ε rules:
            if (node.substitution!!.isNotEmpty()) {
                val substChain = Chain<Unique<out Symbol>>(node.substitution!!)

                // store references to chainlinks containing non-terminals
                substChain.linkIterator().asSequence().filterIsInstance<ChainLink<Unique<Symbol.NonTerminal>>>()
                    .forEach {
                        nts[it.value] = it
                    }

                // substitute chainlink with chain
                linkToSubstitute.substitute(substChain)
            } else{

                // todo for epsilon rules we need to substitute the element with a empty chain
            }

            node = node.succ!!
        }

        // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
        return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
    }
}