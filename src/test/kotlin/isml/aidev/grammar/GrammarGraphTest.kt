package isml.aidev.grammar

import isml.aidev.util.Chain
import org.junit.jupiter.api.Test
import java.io.File

class GrammarGraphTest {
    @Test
    fun nodeTest() {
        val grammar = Grammar.fromResource("extremely_simple_gram.yml")
        val node1 = Node(Chain(listOf(grammar.startSymbol)))
        println(node1.toString())
    }
    @Test
    fun expectationTest1(){
        val fname = "simple_annot_expectation_1"
        val grammar = Grammar.fromResource("${fname}.yml", false)
        println(grammar)
        var graph = grammar.prodRules.toGraph(grammar.ntMap)
        graph = graph.simplify().calculateExpectation(grammar.startSymbol)
        createExporter().exportGraph(graph, File("${fname}.dot").bufferedWriter())
        val prodrules = graph.toProdRules(grammar.startSymbol, grammar.ntMap)
        println(prodrules)
    }
}
