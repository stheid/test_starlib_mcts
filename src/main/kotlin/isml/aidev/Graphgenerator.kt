package isml.aidev

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.api4.java.ai.graphsearch.problem.IPathSearchInput
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester
import org.api4.java.datastructure.graph.implicit.IGraphGenerator
import org.api4.java.datastructure.graph.implicit.INewNodeDescription
import org.api4.java.datastructure.graph.implicit.ISingleRootGenerator
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator
import java.util.concurrent.ConcurrentHashMap

@Serializable
sealed class Symbol {
    @SerialName("terminal")
    @Serializable
    data class Terminal(val value: String) : Symbol() {
        override fun toString(): String {
            return "term: ${value}"
        }
    }

    @SerialName("nonterminal")
    @Serializable
    data class NonTerminal(val value: String) : Symbol() {
        override fun toString(): String {
            return "nt: ${value}"
        }
    }

}

@Serializable
data class Grammar(
    val startSymbol: Symbol.NonTerminal,
    private val prodRules_: Map<String, List<Rule>>
) {
    val prodRules
        get() = prodRules_.map { Symbol.NonTerminal(it.key) to it.value }.associate { it }
}

typealias ProdRules = Map<Symbol.NonTerminal, List<Rule>>


// edge type
// right hand side of a production rule
@Serializable
data class Rule(val substitution: List<Symbol>, val weight: Double, val is_extending: Boolean) {
    override fun toString(): String {
        return " -> $substitution"
    }
}


class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<Symbols, Rule> {
    private val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator(): ISingleRootGenerator<Symbols> {
        return ISingleRootGenerator<Symbols> { Symbols.fromCollection(listOf(grammar.startSymbol)) }
    }

    override fun getSuccessorGenerator(): ISuccessorGenerator<Symbols, Rule> {
        return this.successorGenerator
    }
}


data class Symbols(val symbols: List<Symbol>, val nonTerminalIndices: List<Int>, val pathLength: Int) {
    val expandableNT = nonTerminalIndices.withIndex().toList().randomOrNull()

    companion object {
        fun fromCollection(collection: List<Symbol>): Symbols {
            return Symbols(collection,
                collection.withIndex().filter { it.value is Symbol.NonTerminal }.map { it.index }, 0
            )
        }
    }

    fun createChild(substitution: List<Symbol>): Symbols {
        require(expandableNT != null) { "cannot create childs when there are no nonterminals" }

        val str = let {
            val list = this.symbols.toMutableList()
            list.removeAt(expandableNT.value)
            list.addAll(expandableNT.value, substitution)
            return@let list
        }.toList()

        val indices = nonTerminalIndices.take(expandableNT.index) +
                // shift indices of the substitution by the offset of the index at which they are added
                fromCollection(substitution).nonTerminalIndices.map { it + expandableNT.value } +
                // shift elements after substitution by the size of the substitution
                nonTerminalIndices.drop(expandableNT.index + 1).map { it + substitution.size - 1 }

        return Symbols(str, indices, pathLength + 1)
    }

    val isTerminalsOnly: Boolean
        get() = this.nonTerminalIndices.isEmpty()

    override fun toString(): String {
        if (isTerminalsOnly)
            return symbols.joinToString(separator = "") {
                when (it) {
                    is Symbol.NonTerminal -> it.value
                    is Symbol.Terminal -> it.value
                }
            }
        return "<h>"+Triple(expandableNT?.value, symbols, nonTerminalIndices).toString()+"</h>"
    }
}

class PCFGSuccGen(private val prodRules: ProdRules) : ISuccessorGenerator<Symbols, Rule> {
    val nodes = ConcurrentHashMap<Symbols, Boolean>()
    val adiacenceList = ConcurrentHashMap<Symbols, List<INewNodeDescription<Symbols, Rule>>>()


    override fun generateSuccessors(node: Symbols): List<INewNodeDescription<Symbols, Rule>> {
        if (adiacenceList.containsKey(node)) {
            // if expansion known. return childs
            return adiacenceList.get(node)!!
        }

        // else create new childs and filter for existing ones
        val children = node.expandableNT?.let { nt ->
            prodRules[node.symbols[nt.value] as Symbol.NonTerminal]!!
                .map { node.createChild(it.substitution) to it }
                .filter { (child, _) ->
                    val tmp = nodes.putIfAbsent(child, true) == null
                    return@filter tmp
                }
                .map { (child, rule) ->
                    object : INewNodeDescription<Symbols, Rule> {
                        override fun getFrom(): Symbols {
                            return node
                        }

                        override fun getTo(): Symbols {
                            return child
                        }

                        override fun getArcLabel(): Rule {
                            return rule
                        }
                    }
                }
        } ?: emptyList()

        // todo: handle what happens if node has no children, but is not a leaf
        // -> can happen because of filtering
        adiacenceList.put(node, children)
        return children
    }
}


class PCFGSearchInput(private val grammar: Grammar) : IPathSearchInput<Symbols, Rule> {
    override fun getGraphGenerator(): IGraphGenerator<Symbols, Rule> {
        return PCFGGraphGenerator(grammar)
    }

    override fun getGoalTester(): IPathGoalTester<Symbols, Rule> {
        return IPathGoalTester<Symbols, Rule> { path -> path.head.isTerminalsOnly }
    }
}

/*class PCFGPathEvaluator() : IPathEvaluator<isml.aidev.Symbols, isml.aidev.Rule, Float> {
    override fun evaluate(path: ILabeledPath<isml.aidev.Symbols, isml.aidev.Rule>): Float {
        if (!path.head.isTerminalsOnly)
            throw RuntimeException("can't evaluate inner nodes")
        // TODO

        // calculate performance with respect to the already observed

        //
    }
}*/