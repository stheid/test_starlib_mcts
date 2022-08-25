package scratch

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import isml.aidev.Symbol.*
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File

open class Node(open var nodes: List<Symbol>) {
    val value: String
        get() = nodes.toString()
}
data class ComplexNode(override var nodes: List<Symbol>) : Node(nodes)
open class SimpleNode(override var nodes: List<Symbol>) : Node(nodes)
data class NtNode(override var nodes: List<Symbol>) : SimpleNode(nodes)
data class TermNode(override var nodes: List<Symbol>) : SimpleNode(nodes)

class ComplexEdge : DefaultEdge()

fun main() {
    val grammar = Grammar.fromResource("extremely_simple_gram.yml")

    var graph = grammar.toGraph()
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
    graph = graph.simplify()
    exporter.exportGraph(graph, File("grammar_simple.dot").bufferedWriter())

    val simplegrammar = Grammar.fromGraph(graph)
    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)

    // TODO convert grammar into a graph
    this.prodRules.forEach { (key, vallue) ->
        val keyNode = NtNode(mutableListOf(NonTerminal(key)))
        graph.addVertex(keyNode)


        vallue.forEach { it ->
            val value = it.substitution
            if (value.size == 1) {
                var tempVertex = Node(mutableListOf())
                if (value[0] is NonTerminal) {
                    tempVertex = NtNode(mutableListOf(NonTerminal(value[0].value)))
                } else if (value[0] is Terminal) {
                    tempVertex = TermNode(mutableListOf(Terminal(value[0].value)))
                }
                if (tempVertex !in graph.vertexSet()) {
                    graph.addVertex(tempVertex)
                }
                graph.addEdge(keyNode, tempVertex)
            } else {
                var complexString = ""
                val cs_list: MutableList<Symbol> = mutableListOf()
                value.forEach { its ->
                    complexString += " " + its.value
                    if (its is NonTerminal) {
                        cs_list.add(NonTerminal(its.value))
                    } else if (its is Terminal) {
                        cs_list.add(Terminal(its.value))
                    } // get rid of it
                }
                val cn = ComplexNode(cs_list)
                if(cn !in graph.vertexSet()) {
                    graph.addVertex(cn)
                }
                graph.addEdge(keyNode, cn)
////                cs_list.forEach{
////                    var tempnode = Node(mutableListOf())
////                    if (it is Terminal){
////                        tempnode = TermNode(mutableListOf(it))
////                    }else{tempnode = NtNode(mutableListOf(it))}
////                    if (tempnode !in graph.vertexSet()) {
////                        graph.addVertex(tempnode)
////                    }
////                    graph.addEdge(cn, tempnode, ComplexEdge())
//                }
            }
        }
    }
    return graph
}

private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar {
    val startsymbol = NonTerminal(graph.vertexSet().toList()[0].value)
    val grammar = mutableMapOf<String, MutableList<RuleEdge>>()

    graph.vertexSet()
        .filter{it !is ComplexNode}
        .forEach{
        val succs = graph.succs(it)
        succs.forEach { succ ->
            val symbols = mutableListOf<Symbol>()
            if (succ is NtNode) {
                symbols.add(NonTerminal(succ.value))
            } else if (succ is TermNode) {
                symbols.add(Terminal(succ.value))
            } else if (succ is ComplexNode){
                // todo: remove trim here, just for testing
                succ.value.trim().split(" ").forEach{
                    it_sn ->
                    val charval = it_sn.toCharArray().single()
                    if(charval.isLowerCase()){
                        symbols.add(Terminal(it_sn))
                    }else{
                        symbols.add(NonTerminal(it_sn))
                    }

                }
//                succ.nodes.forEach{
//                        it_sn ->
//                    if (it_sn is NtNode) {
//                        symbols.add(NonTerminal(it_sn.value))
//                    } else if (it_sn is TermNode) {
//                        symbols.add(Terminal(it_sn.value))
//                    }
//                }
            }
            val redge = RuleEdge(substitution = symbols, weight = 1.0F)
            if (it.value in grammar.keys) {
                grammar[it.value]?.add(redge)
            } else {
                grammar[it.value] = mutableListOf(redge)
            }
        }
    }

    return Grammar(startsymbol, grammar)
}

fun <V, E> DefaultDirectedGraph<V, E>.preds(vert: V): MutableList<V> {
    return Graphs.predecessorListOf(this, vert)!!
}

fun <V, E> DefaultDirectedGraph<V, E>.succs(vert: V): MutableList<V> {
    return Graphs.successorListOf(this, vert)!!
}

fun <Node, DefaultEdge> DefaultDirectedGraph<Node, DefaultEdge>.simplify(): DefaultDirectedGraph<Node, DefaultEdge> {
    vertexSet().toList().forEach { node ->
        // The first condition below is important because on deletion of certain nodes during
        // complex nodes' simplification, it throws an error.
        if (node in this.vertexSet() && succs(node).size == 1 && preds(node).size == 1) {
            Triple(preds(node).single(), node, succs(node).single())
                .also { (pred, node, succ) ->
                    if (getEdge(pred, node)!!.javaClass == DefaultEdge().javaClass
                        && getEdge(node, succ)!!.javaClass == DefaultEdge().javaClass
                    ) {
                        // pred, node and succ are primitive nodes
                        // make nodes predecessor refer to nodes successor
                        addEdge(pred, succ)
                        removeVertex(node)


                    } else if (getEdge(pred, node)!!.javaClass == ComplexEdge().javaClass
                        && succ is SimpleNode
                    ){
                        // complex node -> NT -> Terminal
                        // pred         -> node -> succ
                        // predecessor is a complex node
                        // modify pred to point to succ
//                        addEdge(pred, succ, ComplexEdge() as DefaultEdge)
//                        // modify the internal value of the predecessor according to the succ
//                        (pred as ComplexNode).value = (pred as ComplexNode).value.replace((node as SimpleNode).value, (succ as SimpleNode).value)
//                        removeVertex(node)
//                        // todo: After above simplification, check if complex node points to only terminals and then remove them.
//                        if(succ is TermNode)
//                            removeVertex(succ)
//                        // update ComplexNode.nodes
//                        println("")
                    }
                }
        }
    }
    return this
}
