package isml.aidev

import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGenerator
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow
import ai.libs.jaicore.search.algorithms.mdp.mcts.uct.UCTFactory
import ai.libs.jaicore.search.algorithms.standard.mcts.MCTSPathSearchFactory
import ai.libs.jaicore.search.model.other.EvaluatedSearchGraphPath
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput
import isml.aidev.starlib.PCFGSearchInput
import isml.aidev.util.Chain
import isml.aidev.util.ChainLink
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


private fun <N, A> ILabeledPath<N, A>.toWord(): String {
    return this.head.toString()

    // TODO actually the "succ" reference does not work, as we talk about a tree. we need to do it with parent references. this also eliminates the need of a root reference

    var node = root ?: this
    val symbol = node.currNT!!
    val symbols = Chain(listOf<Unique<out Symbol>>(symbol))
    val nts = hashMapOf<Unique<Symbol.NonTerminal>, ChainLink<Unique<out Symbol>>>(symbol to symbols.linkIterator().asSequence().first())

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
