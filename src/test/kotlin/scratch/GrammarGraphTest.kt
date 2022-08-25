package scratch

import isml.aidev.Grammar

import org.junit.jupiter.api.Test
import java.io.File

class GrammarGraphTest {
    @Test
    fun nodeTest() {
        val grammar = Grammar.fromResource("extremely_simple_gram.yml")
        val node1 = Node(mutableListOf(grammar.startSymbol))
        println(node1.value)
    }

    @Test
    fun toGraphTest(){
        val grammar = Grammar.fromResource("extremely_simple_gram.yml")
        var graph = grammar.toGraph()
        graph = graph.simplify()
    }
}