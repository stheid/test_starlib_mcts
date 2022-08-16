package scratch

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import org.jgrapht.ext.DOTExporter
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import javax.naming.spi.ObjectFactory

open class Node(open val value: String)
data class ComplexNode(override val value: String) : Node(value)
data class NtNode(override val value: String) : Node(value)
data class TermNode(override val value: String) : Node(value)

fun main() {
    val grammar = Grammar.fromFile(ObjectFactory::class.java.getResource("json_simple_gram.yml")?.path!!)

    val graph = grammar.toGraph().simplify()

    val exporter = DOTExporter<Node, DefaultEdge>()
    exporter.export(File("grammar.dot").bufferedWriter(), graph)

    val simplegrammar = Grammar.fromGraph(graph)
    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {
    // TODO convert grammar into a graph
    this

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)

    graph.addEdge(ComplexNode(""""c" B"""), NtNode("B"))
    graph.addEdge(NtNode("B"), TermNode(""""b""""))
    return graph
}

private fun <V, E> DefaultDirectedGraph<V, E>.simplify(): DefaultDirectedGraph<V, E> {
    return this
}


private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar {
    val startsymbol = Symbol.NonTerminal("")
    val grammar = mapOf<String, List<RuleEdge>>()

    return Grammar(startsymbol, grammar)
}