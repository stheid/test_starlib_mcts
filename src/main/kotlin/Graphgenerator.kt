import org.api4.java.ai.graphsearch.problem.IPathSearchInput
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator
import org.api4.java.datastructure.graph.ILabeledPath
import org.api4.java.datastructure.graph.implicit.*
import java.lang.RuntimeException
import java.util.*

open class Symbol(value: String)
data class NonTerminal(val value: String) : Symbol(value)
data class Terminal(val value: String) : Symbol(value)

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


// edge type
// right hand side of a production rulec
data class Rule(val substitution: List<Symbol>, val weight: Double)


data class Grammar(val startSymbol: Symbol, val prodRules: ProdRules)
typealias ProdRules = Map<NonTerminal, Sequence<Rule>>


class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<Symbols, Rule> {
    val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator(): IRootGenerator<Symbols> {
        return object : IRootGenerator<Symbols> {
            override fun getRoots(): Collection<Symbols> {
                return setOf(Symbols.fromCollection(listOf(grammar.startSymbol)))
            }
        }
    }

    override fun getSuccessorGenerator(): ISuccessorGenerator<Symbols, Rule> {
        return this.successorGenerator
    }
}


class PCFGSuccGen(val prodRules: ProdRules) : ILazySuccessorGenerator<Symbols, Rule> {
    override fun getIterativeGenerator(node: Symbols): Iterator<INewNodeDescription<Symbols, Rule>> {
        return node.nonTerminals
            .asSequence()
            .flatMap { nt ->
                prodRules[nt.value]
                    .map {
                        object : INewNodeDescription<Symbols, Rule> {
                            override fun getFrom(): Symbols {
                                return node
                            }

                            override fun getTo(): Symbols {
                                return node.substitute(nt, it.substitution)
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


// TODO load grammar


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

class PCFGPathEvaluator() : IPathEvaluator<Symbols, Rule, Float> {
    override fun evaluate(path: ILabeledPath<Symbols, Rule>): Float {
        if (!path.head.isTerminalsOnly)
            throw RuntimeException("can't evaluate inner nodes")
        // TODO

        // calculate performance with respect to the already observed

        //
    }
}