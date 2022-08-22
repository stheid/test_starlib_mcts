package scratch

import ai.libs.jaicore.search.syntheticgraphs.treasuremodels.islands.noisymean.ATreasureMeanFunction
import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import isml.aidev.Symbol.*
import org.graphstream.stream.file.FileSourceGraphML.GraphMLConstants.EdgeAttribute
import org.jfree.xml.attributehandlers.StringAttributeHandler
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File

open class Node(open val value: String)
data class ComplexNode(override val value: String) : Node(value)
data class NtNode(override val value: String) : Node(value)
data class TermNode(override val value: String) : Node(value)

class ComplexEdge : DefaultEdge()

fun main() {
    val grammar = Grammar.fromResource("extremely_simple_gram.yml")

    val graph = grammar.toGraph().simplify()
    //val graph = grammar.toGraph()
    val exporter = DOTExporter<Node, DefaultEdge> { """"${it.value}"""" }
    exporter.setVertexAttributeProvider {
        mutableMapOf<String, Attribute>(
            "color" to DefaultAttribute.createAttribute(if (it is ComplexNode) "red" else "black")
        )
    }
    exporter.setEdgeAttributeProvider {
        mutableMapOf<String, Attribute>(
            "color" to DefaultAttribute.createAttribute(if (it is ComplexEdge) "red" else "black")
        )
    }

    exporter.exportGraph(graph, File("grammar.dot").bufferedWriter())
    println(graph)

    val simplegrammar = Grammar.fromGraph(graph)
    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)

    // TODO convert grammar into a graph
    this.prodRules.forEach { (key, vallue) ->
        val keyNode = NtNode(key)
        if (keyNode !in graph.vertexSet()) {
            graph.addVertex(NtNode(key))
        }

        vallue.forEach { it ->
            val value = it.substitution
            if (value.size == 1) {
                if (value[0] is NonTerminal) {
                    graph.addVertex(NtNode(value[0].value))
                    graph.addEdge(keyNode, NtNode(value[0].value))
                } else if (value[0] is Terminal) {
                    graph.addVertex(TermNode(value[0].value))
                    graph.addEdge(keyNode, TermNode(value[0].value))
                }
            } else {
                var complexString = ""
                val cs_list: MutableList<Node> = mutableListOf()
                value.forEach { its ->
                    complexString += " " + its.value
                    if (its is NonTerminal) {
                        cs_list.add(NtNode(its.value))
                    } else if (its is Terminal) {
                        cs_list.add(TermNode(its.value))
                    }
                }
                val cn = ComplexNode(complexString)
                graph.addVertex(cn)
                graph.addEdge(keyNode, cn)
                cs_list.forEach {
                    if (it !in graph.vertexSet()) {
                        graph.addVertex(it)
                    }
                    graph.addEdge(cn, it, ComplexEdge())
                }
            }
        }
    }
    return graph
}

private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar {
    val startsymbol = NonTerminal("")
    val grammar = mutableMapOf<String, List<RuleEdge>>()

//    graph.vertexSet().forEach{
//        if(!grammar.keys.contains(it.value)){
//            val a = graph.edgesOf(it)
//            a.forEach{
//                RuleEdge(it)
//            }
//            grammar[it.value] =
////            println("h")
//        }
//    }

    return Grammar(startsymbol, grammar)
}

private fun <V, E> DefaultDirectedGraph<V, E>.targetEdgesOf(node: V): MutableList<DefaultEdge> {
    val totedges: MutableList<DefaultEdge> = mutableListOf()
    this.edgesOf(node).forEach { itx ->
        if (getEdgeSource(itx) == node) {
            totedges.add(itx as DefaultEdge)
        }
    }
    return totedges
}

fun <V, E> DefaultDirectedGraph<V, E>.preds(vert: V): MutableList<V> {
    return Graphs.predecessorListOf(this, vert)!!
}

fun <V, E> DefaultDirectedGraph<V, E>.succs(vert: V): MutableList<V> {
    return Graphs.successorListOf(this, vert)!!
}

private inline fun <V, reified DefaultEdge> DefaultDirectedGraph<V, DefaultEdge>.simplify(): DefaultDirectedGraph<V, DefaultEdge> {
    vertexSet().toList().forEach { node ->
        if (succs(node).size == 1 && preds(node).size == 1) {
            Triple(preds(node).single(), node, succs(node).single())
                .also { (pred, node, succ) ->
                    if (getEdge(pred, node)!!.javaClass == DefaultEdge::class.java
                        && getEdge(node, succ)!!.javaClass == DefaultEdge::class.java
                    ) {
                        // make nodes predecessor refer to nodes successor
                        addEdge(pred, succ)
                        removeVertex(node)

                        // TODO handle complex nodes diffrently

                    }
                }
        }
    }

    return this
}