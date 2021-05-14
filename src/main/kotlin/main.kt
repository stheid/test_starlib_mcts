import ai.libs.jaicore.problems.nqueens.NQueensProblem
import ai.libs.jaicore.search.algorithms.mdp.mcts.uct.UCTFactory
import ai.libs.jaicore.search.algorithms.standard.mcts.MCTSPathSearchFactory
import ai.libs.jaicore.search.exampleproblems.nqueens.NQueensToGraphSearchReducer
import ai.libs.jaicore.search.exampleproblems.nqueens.QueenNode
import ai.libs.jaicore.search.probleminputs.GraphSearchInput
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput
import org.api4.java.ai.graphsearch.problem.pathsearch.pathevaluation.IPathEvaluator


fun main_() {
    /* create path search problem */
    val rawInput = GraphSearchInput(NQueensToGraphSearchReducer().encodeProblem(NQueensProblem(8)))

    /* create a version with costs */
    val input = GraphSearchWithPathEvaluationsInput(rawInput, IPathEvaluator { 1.0 })

    /* create MCTS algorithm */
    val factory = MCTSPathSearchFactory<QueenNode, String>()
    val uct = UCTFactory<QueenNode, String>()
    uct.withMaxIterations(100)
    val mcts = factory.withMCTSFactory(uct).withProblem(input).algorithm

    /* solve problem */
    val solution = mcts.call()
    println(solution.head)
    println(solution.score)
}

fun main() {
    /* create path search problem */
    val rawInput = PCFGSearchInput(
        Grammar(
            NonTerminal("A"),
            mapOf(
                NonTerminal("A") to sequenceOf(
                    Rule(Symbols.fromCollection(listOf(NonTerminal("A"), Terminal("a"))), 1.0),
                    Rule(Symbols.fromCollection((listOf(Terminal("a")))), 1.0)
                )
            )
        )
    )

    /* create a version with costs */
    val input = GraphSearchWithPathEvaluationsInput(rawInput, IPathEvaluator { 1.0 })

    /* create MCTS algorithm */
    val factory = MCTSPathSearchFactory<Symbols, Rule>()
    val uct = UCTFactory<Symbols, Rule>()
    uct.withMaxIterations(100)
    val mcts = factory.withMCTSFactory(uct).withProblem(input).algorithm

    /* solve problem */
    val solution = mcts.call()
    println(solution.head)
    println(solution.score)

}