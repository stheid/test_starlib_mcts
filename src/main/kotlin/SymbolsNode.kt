data class LinkedElement<V>(var pre: LinkedElement<V>?, var succ: LinkedElement<V>?, val value: V) {
    companion object {
        fun <V> chain(collection: List<V>): List<LinkedElement<V>> {
            val first = LinkedElement(null, null, collection[0])
            val linkedElems = mutableListOf(first)
            collection.drop(0).map {
                val new = LinkedElement(linkedElems.last(), null, it)
                linkedElems.last().succ = new
                linkedElems.add(new)
            }
            return linkedElems.toList()
        }
    }
}

data class Substitution( val from: LinkedElement<Symbol>, val to: Rule)


class SymbolsNode(
    val parent: SymbolsNode?,
    val substitution:Substitution,
    nonTerminals: Set<LinkedElement<Symbol>>
) {
    var nonTerminals: Set<LinkedElement<Symbol>> = mutableSetOf()

    val isFinished: Boolean get() = nonTerminals.isEmpty()

    fun substitute(substitution: Substitution): SymbolsNode {
        /**
         * Creates a child by substituting a random non terminal
         */
        val sub = LinkedElement.chain(substitution.to.substitution).filterIsInstance<LinkedElement<NonTerminal>>().
                map { it as LinkedElement<Symbol> }


        val remainingNonTerminals = this.nonTerminals.minus(substitution.from).plus(sub)

        return SymbolsNode(this, substitution,remainingNonTerminals)

    }

    fun concat() {
        /**
         * backtrack to root, collect all substitution rules
         * than play out substitution rules
         */



    }

}

fun test() {
    val root = SymbolsNode(
        null,
        Pair<LinkedElement<Symbol>?, Rule>(null, Rule(listOf(NonTerminal("S")), 1.0))
    )


}


/*

Each node knows:
 - its parent
 - substitution wrt. its parent
 - references to the remaining non-terminals

when in leaf-node:
- backtrack to the root and record all substitution rules
- when in root play out all substitutions to retrieve the string

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