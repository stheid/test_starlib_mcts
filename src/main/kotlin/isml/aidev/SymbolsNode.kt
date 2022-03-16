package isml.aidev

import isml.aidev.util.Chain

data class SymbolsNode(
    val NTs: ArrayList<Symbol.UniqueNT>,
    val root: SymbolsNode? = null,
    val depth: Int = 0,
) {
    val currNT = NTs.let {
        if (NTs.isNotEmpty())
            return@let NTs.removeLast()
        else
            return@let null
    }

    var succ: SymbolsNode? = null
    var substitution: List<Symbol>? = null
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
        var symbol = node.currNT!!
        val symbols = Chain(listOf(symbol as Symbol))
        val nts = hashMapOf(symbol to symbols.linkIterator().asSequence().first())

        while (node.succ != null) {
            symbol = node.currNT!!

            if (node.substitution!!.isNotEmpty()) {
                val substChain = Chain(node.substitution!!).also { chain ->
                    chain.linkIterator().asSequence().filter { it.value is Symbol.UniqueNT }.forEach {
                        nts[it.value as Symbol.UniqueNT] = it
                    }
                }
                symbols.substitute(nts[symbol]!!, substChain)
            }

            node = node.succ!!
        }

        // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
        return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
    }
}

/*
data class SymbolsNode(
    val symbols :List<Symbol>,
    val NTs: ArrayList<Symbol.NonTerminal>,
    val root: SymbolsNode? = null,
    val depth: Int = 0
) {
    val expandableNT = NTs.randomOrNull()

    companion object {
        fun fromCollection(collection: List<Symbol>): SymbolsNode {
            return SymbolsNode(
                collection,
                collection.filterIsInstance(Symbol.NonTerminal::class.java) as ArrayList<Symbol.NonTerminal>,
                depth = 0
            )
        }
    }


    val isFinished: Boolean
        get() = NTs.isEmpty()

    fun createChild(substitution: List<Symbol>): SymbolsNode {
        require(expandableNT != null) { "cannot create childs when there are no nonterminals" }

        val str = let {
            val list = symbols.toMutableList()
            list.removeAt(expandableNT)
            list.addAll(expandableNT, substitution)
            return@let list
        }.toList()

        val indices = NTs.take(expandableNT.index) +
                // shift indices of the substitution by the offset of the index at which they are added
                fromCollection(substitution).NTs.map { it + expandableNT.value } +
                // shift elements after substitution by the size of the substitution
                NTs.drop(expandableNT.index + 1).map { it + substitution.size - 1 }

        return SymbolsNode(str, indices, depth + 1)
    }


    override fun toWord(): String {
        if (isFinished)
            return symbols.joinToString(separator = "") {
                when (it) {
                    is Symbol.NonTerminal -> it.value
                    is Symbol.Terminal -> it.value
                }
            }
        return "<h>" + Triple(expandableNT?.value, symbols, nonTerminalIndices).toString() + "</h>"
    }
}*/