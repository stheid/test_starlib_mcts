package isml.aidev

import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow
import ai.libs.jaicore.search.algorithms.mdp.mcts.uct.UCTFactory
import ai.libs.jaicore.search.algorithms.standard.mcts.MCTSPathSearchFactory
import ai.libs.jaicore.search.model.other.EvaluatedSearchGraphPath
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput
import isml.aidev.starlib.PCFGSearchInput
import isml.aidev.util.Chain
import isml.aidev.util.Unique
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator
import org.api4.java.datastructure.graph.ILabeledPath
import kotlin.concurrent.thread


class Algorithm(
    maxIterations: Int = 100,
    grammarPath: String = "grammar.yaml",
    maxPathLength: Int = 20,
    headless: Boolean = true,
) {
    private val inputChannel = Channel<ByteArray>()
    private val covChannel = Channel<Double>()
    private lateinit var solution: EvaluatedSearchGraphPath<SymbolsNode, RuleEdge, Double>
    private var worker: Thread

    init {
        val rawInput = PCFGSearchInput(
            // parse grammar to object representation of the grammar
            Grammar.fromFile(grammarPath)
        )

        // create a version with costs
        val input = GraphSearchWithPathEvaluationsInput(rawInput, IPathEvaluator {
            runBlocking {
                inputChannel.send(it.toWord().toByteArray())
                covChannel.receive()
            }
        })

        // create MCTS algorithm
        val factory = MCTSPathSearchFactory<SymbolsNode, RuleEdge>()
        val uct = UCTFactory<SymbolsNode, RuleEdge>().withDefaultPolicy { symbols, rules ->
            val stop_extending = symbols.depth > maxPathLength

            rules.toList().choice(
                p = rules.map { it.weight * if (stop_extending && it.is_extending) 1e-10 else 1.0 }.toDoubleArray()
                    .normalize()
            )
        }
        uct.withMaxIterations(maxIterations)
        val mcts = factory.withMCTSFactory(uct).withProblem(input).algorithm

        if (!headless) {
            val window = AlgorithmVisualizationWindow(mcts)
            window.withMainPlugin(GraphViewPlugin())
            window.withPlugin(NodeInfoGUIPlugin { it.toString() })
        }

        // start mcts call in background
        worker = thread { solution = mcts.call() }
    }

    fun createInput() =
        runBlocking { inputChannel.receive() }

    fun observe(reward: Double) =
        runBlocking { covChannel.send(reward) }

    fun join() {
        worker.join()

        println(solution.toWord())
        println(solution.score)
    }
}


private fun ILabeledPath<SymbolsNode, RuleEdge>.toWord(): String {
    val node = this.root
    val symbol = node.currNT!!
    val symbols = Chain(listOf<Any>(symbol))
    val nts = hashMapOf(symbol to symbols.linkIterator().asSequence().first())

    // root has been processed, now we look at the production rules and the successor nodes
    this.arcs.zip(this.nodes.drop(1)).forEach { (rule, succ) ->

        // dereference chainlink (GC) and prepare for substitution
        val linkToSubstitute = nts.remove(succ.currNT)!!

        var substChain = Chain(emptyList<Any>())

        // for non-ε rules:
        if (rule.substitution.isNotEmpty()) {
            val sub = succ.substitutionNTs.iterator().let { iterator ->
                return@let rule.substitution.map {
                    if (it is Symbol.Terminal)
                        it
                    else
                        // if its a non-terminal, take the equivalent Unique<NonTerminal> from the node
                        iterator.next() }
            }
            substChain = Chain(sub)

            // store references to chainlinks containing non-terminals
            substChain.linkIterator().asSequence()
                .forEach {
                    if (it.value is Unique<*> && it.value.value is Symbol.NonTerminal) {
                        @Suppress("UNCHECKED_CAST")
                        nts[it.value as Unique<Symbol.NonTerminal>] = it
                    }
                }

            // substitute chainlink with chain
        }

        linkToSubstitute.substitute(substChain)
    }

    // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
    return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }

}
