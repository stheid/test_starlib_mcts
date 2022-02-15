package isml.aidev

data class SymbolsNode(
    val NTs: ArrayList<Symbol.NonTerminal>,
    val root: SymbolsNode? = null,
    val depth: Int = 0
) {
    val currNT = NTs.let {
        val idx = (0 until it.size).randomOrNull()
        if (idx != null)
            return@let NTs.removeAt(idx)
        return@let null
    }

    var succ: SymbolsNode? = null
    var toRule: RuleEdge? = null
    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: RuleEdge): SymbolsNode {
        val child = SymbolsNode(
            (NTs + rule.substitution.filterIsInstance(Symbol.NonTerminal::class.java)) as ArrayList<Symbol.NonTerminal>,
            root ?: this,
            depth + 1
        )
        toRule = rule
        succ = child
        return child
    }

    fun toWord(): String {
        if (!isFinished)
            return ""

        var node = root ?: this
        var symbols = arrayListOf<Symbol>(node.currNT!!)
        while (node.succ != null) {
            // todo get the right index and not any index!!
            symbols = (symbols.take(symbols.indexOf(node.currNT!!))
                    + node.toRule!!.substitution
                    + symbols.drop(symbols.indexOf(node.currNT!!) + 1)) as ArrayList<Symbol>
            node = node.succ!!
        }

        // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
        return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
    }
}