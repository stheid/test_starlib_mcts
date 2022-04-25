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
    private var worker: Thread
    private lateinit var solution: EvaluatedSearchGraphPath<SymbolsNode, RuleEdge, Double>

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
                p = rules.map { it.weight * if (stop_extending && it.isExtending) 1e-10 else 1.0 }.toDoubleArray()
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
    val symbols = Chain(listOf<Symbol>(symbol))
    val nts = hashMapOf(symbol to symbols.linkIterator().asSequence().first())

    // root has been processed, now we look at the production rules and the successor nodes
    arcs.zip(nodes.zipWithNext()).forEach { (rule, nodepair) ->
        // dereference chainlink (GC) and prepare for substitution
        val linkToSubstitute = nts.remove(nodepair.first.currNT!!)!!

        if (rule.substitution.isEmpty()) {
            // for ε-rules just remove the reference
            linkToSubstitute.substitute(null)
        } else {
            // for non-ε rules:
            // create the substitution by melding the unique non-terminals from the node and adding the missing
            // terminals from the rule
            // e.g.  substitutionNTs: B3,D4,D6 (unique NTs); rule.substitution: aBcDDc (nts and terminals)
            val sub = nodepair.second.substitutionNTs.iterator().let { iterator ->
                rule.substitution.map {
                    // if it's a non-terminal, take the equivalent Unique<NonTerminal> from the node
                    it as? Symbol.Terminal ?: iterator.next()
                }
            }
            val substChain = Chain(sub)

            // store references to chainlinks containing non-terminals
            substChain.linkIterator().asSequence()
                .forEach {
                    if (it.value is Symbol.NonTerminal)
                        nts[it.value] = it
                }

            // substitute chainlink with chain
            linkToSubstitute.substitute(substChain)
        }
    }

    return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
}
