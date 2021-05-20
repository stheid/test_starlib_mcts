import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.api4.java.ai.graphsearch.problem.IPathSearchInput
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator
import org.api4.java.datastructure.graph.ILabeledPath
import org.api4.java.datastructure.graph.implicit.*

@Serializable
sealed class Symbol {
    @SerialName("terminal")
    @Serializable
    data class Terminal(val value: String) : Symbol()

    @SerialName("nonterminal")
    @Serializable
    data class NonTerminal(val value: String) : Symbol()

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
data class Rule(val substitution: List<Symbol>, val weight: Double)


class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<Symbols, Rule> {
    val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator(): ISingleRootGenerator<Symbols> {
        return ISingleRootGenerator<Symbols> { Symbols.fromCollection(listOf(grammar.startSymbol)) }
    }

    override fun getSuccessorGenerator(): ISuccessorGenerator<Symbols, Rule> {
        return this.successorGenerator
    }
}


data class Symbols(val symbols: List<Symbol>, val nonTerminalIndices: List<Int>) {
    companion object {
        fun fromCollection(collection: List<Symbol>): Symbols {
            return Symbols(collection,
                collection.withIndex().filter { it.value is Symbol.NonTerminal }.map { it.index })
        }
    }

    fun createChild(idxNonTerminal: Int, substitution: List<Symbol>): Symbols {
        val indexOfNT = nonTerminalIndices[idxNonTerminal]
        val str = this.symbols.toMutableList()
        str.removeAt(indexOfNT)
        str.addAll(indexOfNT, substitution)

        val indices = nonTerminalIndices.take(indexOfNT) +
                // shift indices of the substitution by the offset of the index at which they are added
                fromCollection(substitution).nonTerminalIndices.map { it + indexOfNT } +
                // shift elements after substitution by the size of the substitution
                nonTerminalIndices.drop(indexOfNT + 1).map { it + substitution.size - 1 }

        return Symbols(str.toList(), indices)
    }

    val isTerminalsOnly: Boolean
        get() = this.nonTerminalIndices.isEmpty()
}

class PCFGSuccGen(val prodRules: ProdRules) : ILazySuccessorGenerator<Symbols, Rule> {
    override fun getIterativeGenerator(node: Symbols): Iterator<INewNodeDescription<Symbols, Rule>> {
        if (node.isTerminalsOnly)
            return emptySequence<INewNodeDescription<Symbols, Rule>>().iterator()
        else
            return node.nonTerminalIndices.take(1)
                .withIndex()
                .asSequence()
                .flatMap { nt ->
                    prodRules.get(node.symbols[nt.value] as Symbol.NonTerminal)!!
                        .asSequence()
                        .map {
                            object : INewNodeDescription<Symbols, Rule> {
                                override fun getFrom(): Symbols {
                                    return node
                                }

                                override fun getTo(): Symbols {
                                    return node.createChild(nt.index, it.substitution)
                                }

                                override fun getArcLabel(): Rule {
                                    return it
                                }
                            }
                        }
                }.iterator()
    }

    override fun generateSuccessors(node: Symbols): List<INewNodeDescription<Symbols, Rule>> {
        return getIterativeGenerator(node).asSequence().toList()
    }
}


class PCFGSearchInput(val grammar: Grammar) : IPathSearchInput<Symbols, Rule> {
    override fun getGraphGenerator(): IGraphGenerator<Symbols, Rule> {
        return PCFGGraphGenerator(grammar)
    }

    override fun getGoalTester(): IPathGoalTester<Symbols, Rule> {
        return object : IPathGoalTester<Symbols, Rule> {
            override fun isGoal(path: ILabeledPath<Symbols, Rule>): Boolean {
                return path.head.isTerminalsOnly
            }
        }
    }
}

/*class PCFGPathEvaluator() : IPathEvaluator<Symbols, Rule, Float> {
    override fun evaluate(path: ILabeledPath<Symbols, Rule>): Float {
        if (!path.head.isTerminalsOnly)
            throw RuntimeException("can't evaluate inner nodes")
        // TODO

        // calculate performance with respect to the already observed

        //
    }
}*/