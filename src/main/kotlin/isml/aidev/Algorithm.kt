package isml.aidev

import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow
import ai.libs.jaicore.search.algorithms.mdp.mcts.uct.UCTFactory
import ai.libs.jaicore.search.algorithms.standard.mcts.MCTSPathSearchFactory
import ai.libs.jaicore.search.model.other.EvaluatedSearchGraphPath
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput
import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator
import java.io.File
import kotlin.concurrent.thread


class Algorithm(maxIterations: Int = 100, grammar: String = "grammar.yaml", maxPathLength: Int = 20) {
    private val inputChannel = Channel<ByteArray>()
    private val covChannel = Channel<Double>()
    private lateinit var solution: EvaluatedSearchGraphPath<Symbols, Rule, Double>
    private var worker: Thread

    init {
        val rawInput = PCFGSearchInput(
            // parse grammar to object representation of the grammar
            Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)).decodeFromString(
                Grammar.serializer(), File(grammar).bufferedReader().readText()
            )
        )

        // create a version with costs
        val input = GraphSearchWithPathEvaluationsInput(rawInput, IPathEvaluator {
            runBlocking {
                inputChannel.send(it.head.toString().toByteArray())
                covChannel.receive()
            }
        })

        // create MCTS algorithm
        val factory = MCTSPathSearchFactory<Symbols, Rule>()
        val uct = UCTFactory<Symbols, Rule>().withDefaultPolicy { symbols, rules ->
            val stop_extending = symbols.pathLength > maxPathLength

            rules.toList().choice(
                p = rules.map { it.weight * if (stop_extending && it.is_extending) 1e-10 else 1.0 }.toDoubleArray()
                    .normalize()
            )
        }
        uct.withMaxIterations(maxIterations)
        val mcts = factory.withMCTSFactory(uct).withProblem(input).algorithm

        val window = AlgorithmVisualizationWindow(mcts)
        window.withMainPlugin(GraphViewPlugin())
        window.withPlugin(NodeInfoGUIPlugin { it.toString() })

        // start mcts call in background
        worker = thread { solution = mcts.call() }
    }

    fun createInput() =
        runBlocking { inputChannel.receive() }

    fun observe(reward: Double) =
        runBlocking { covChannel.send(reward) }

    fun join() {
        worker.join()

        println(solution.head)
        println(solution.score)
    }
}
