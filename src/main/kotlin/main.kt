import ai.libs.jaicore.search.algorithms.mdp.mcts.uct.UCTFactory
import ai.libs.jaicore.search.algorithms.standard.mcts.MCTSPathSearchFactory
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput
import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator
import java.io.File

fun main() {
    /* create path search problem */
    val rawInput = PCFGSearchInput(
        Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)).decodeFromString(
            Grammar.serializer(), File("grammar.yaml").bufferedReader().readText()
        )
    )
    /* create a version with costs */
    val input = GraphSearchWithPathEvaluationsInput(rawInput, IPathEvaluator {
        it.head.symbols.size * 1.0
    })

    /* create MCTS algorithm */
    val factory = MCTSPathSearchFactory<Symbols, Rule>()
    val uct = UCTFactory<Symbols, Rule>()
    uct.withMaxIterations(1000)
    val mcts = factory.withMCTSFactory(uct).withProblem(input).algorithm
    /* solve problem */
    val solution = mcts.call()

    println(solution.head)
    println(solution.score)
}