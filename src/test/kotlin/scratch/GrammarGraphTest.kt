package scratch

import isml.aidev.Grammar
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
    fun toGraphTest(){
        val grammar = Grammar.fromResource("extremely_simple_gram.yml")
        val graph = grammar.toGraph()
        assert(graph.vertexSet().toList().size == 10)
    }
}