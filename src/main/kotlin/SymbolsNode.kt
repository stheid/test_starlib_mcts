

data class LinkedElement<V>(var pre: LinkedElement<V>?, var succ: LinkedElement<V>?, val value: V)


class SymbolsNode {
}



// node type
/*data class Symbols(val collection: List<Symbol>, val nonTerminals: Set<NonTerminal>) : ArrayList<Symbol>(collection) {
    fun substitute(i: Int, substitution: Symbols): Symbols {
        return Symbols(
            this.take(i) + substitution + this.drop(i + 1),
            this.nonTerminals (setOf(i))
            // TODO: increment all nonterminal indices > i by len(substitution)-1
        )
    }

    val isTerminalsOnly: Boolean
        get() = this.ntIndices.isEmpty()
}
*/

data class Symbols(val first: LinkedElement<Symbol>, val nonTerminals: MutableSet<LinkedElement<Symbol>>) {
    companion object {
        fun fromCollection(collection: List<Symbol>): Symbols {
            val first = LinkedElement(null, null, collection[0])
            val nonTerminals: MutableSet<LinkedElement<Symbol>> = mutableSetOf(first)
            var current = first
            collection.drop(0).map {
                val new = LinkedElement(current, null, it)
                current.succ = new
                current = new
                nonTerminals.add(new)
            }
            return Symbols(first, nonTerminals)
        }
    }


    fun substitute(nonTerminalRef: LinkedElement<Symbol>, substitution: Symbols): Symbols {
        val left = nonTerminalRef.pre
        val right = nonTerminalRef.succ
        // TODO

        return Symbols(
            this.take(i) + substitution + this.drop(i + 1),
            (setOf(i))
        )
    }

    val isTerminalsOnly: Boolean
        get() = this.ntIndices.isEmpty()
}