package scratch

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import isml.aidev.Symbol.*
import org.apache.commons.lang3.ObjectUtils.Null
import org.jgrapht.ext.DOTExporter
import org.jgrapht.ext.VertexNameProvider
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import javax.naming.spi.ObjectFactory
import kotlin.collections.ArrayList as ArrayList

open class Node(open val value: String)
data class ComplexNode(override val value: String) : Node(value)
data class NtNode(override val value: String) : Node(value)
data class TermNode(override val value: String) : Node(value)

fun main() {
    val grammar = Grammar.fromResource("extremely_simple_gram.yml")

    val graph = grammar.toGraph().simplify()
    println(graph)
    val exporter = DOTExporter<Node, DefaultEdge>()
    exporter.export(File("grammar.dot").bufferedWriter(), graph)

//    val simplegrammar = Grammar.fromGraph(graph)
//    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)

    // TODO convert grammar into a graph
    this.prodRules.forEach{(key,value) ->
        val keyNode = NtNode(key)
        if (keyNode !in graph.vertexSet()){
            graph.addVertex(NtNode(key))
        }

        value.forEach{
            val valNode = getNode(it.substitution)
            if(valNode !in graph.vertexSet()){ graph.addVertex(valNode) }
            graph.addEdge(valNode, keyNode)
        }
    }

//    graph.addEdge(ComplexNode(""""b" C"""), NtNode("B"))
//    graph.addEdge(NtNode("B"), TermNode(""""b""""))
    return graph
}

private fun getNode(value: List<Symbol>): Node {
    // analyse the value and
    if (value.size == 1){
        val a = value[0]
        if (a is NonTerminal){
            return NtNode(a.value)
        }else if(a is Terminal){
            return TermNode(a.value)
        }
    }else{
        var complexString = ""
        value.forEach{
            complexString += it.value
        }
        return ComplexNode(complexString)
    }
    return Node("")
}

private fun <V, E> DefaultDirectedGraph<V, E>.simplify(): DefaultDirectedGraph<V, E> {
    return this
}
private fun Map<String, List<RuleEdge>>.simplify(): Map<String, List<RuleEdge>> {
    // find simple NT->[[term+]] rules, so basically non-terminals that are only part of one single rule that is made up
    // entirely by terminals
    val groups = this.entries.groupBy { (_, value) -> value.size == 1 && value[0].substitution.all { it is Terminal } }
        .entries.associate { it.key to it.value.associate { (k,v) -> k to v } }
    val complex = groups[false] ?: emptyMap()
    val simple = groups[true] ?: emptyMap()


    val simpleSub = simple.entries.associate { it.key to it.value.single().substitution }

    // substitute uses of this NTs in other rules
    return complex.entries.associate { (key, value) ->
        key to value.map { rule ->
            val newSubs = rule.substitution.flatMap { simpleSub[it.value] ?: listOf(it) }
            RuleEdge(newSubs, rule.weight)
        }
    }
}


private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar {
    val startsymbol = NonTerminal("")
    val grammar = mapOf<String, List<RuleEdge>>()

    return Grammar(startsymbol, grammar)
}