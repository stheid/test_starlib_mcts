package scratch

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import isml.aidev.Symbol.NonTerminal
import isml.aidev.Symbol.Terminal
import isml.aidev.util.Chain
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

open class Node(open val nodes: Chain<Symbol>?)

// ConditionalNode can contain non-terminals based on conditions in annotated grammar
data class ConditionalNode(val cond: String?, override val nodes: Chain<Symbol>?) : Node(nodes)
// ComplexNode can contain multiple terminals and non-terminals
data class ComplexNode(override val nodes: Chain<Symbol>?) : Node(nodes)
// SimpleNodes contain either a single non-terminal or a list of terminals
data class SimpleNode(override val nodes: Chain<Symbol>?) : Node(nodes){
    override fun hashCode(): Int = nodes?.hashCode() ?: super.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as SimpleNode

        if (nodes != other.nodes) return false

        return true
    }
}

class ComplexEdge : DefaultEdge()
class ConditionalEdge(val cond: String?) : DefaultEdge()

fun main() {
//    val grammar = Grammar.fromResource("simple_annotated_grammargraph.yaml")
    val grammar = Grammar.fromResource("extremely_simple_gram.yml")

    var graph = grammar.toGraph()
//    val exporter = DOTExporter<Node, DefaultEdge> { """"${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"""" }
    val exporter = DOTExporter<Node, DefaultEdge> { """"${if(it.nodes == null) it.hashCode() else 
        it.nodes.toString().filter { it.isLetterOrDigit() }}"""" }

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

    exporter.exportGraph(graph, File("grammar_raw.dot").bufferedWriter())
    graph = graph.simplify()
    println(graph)
//    exporter.exportGraph(graph, File("grammar_simple.dot").bufferedWriter())

//    val simplegrammar = Grammar.fromGraph(graph)
//    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)
    val nodes = mutableMapOf<String, Node>()
    val complexNodes = mutableMapOf<String, Node>()

    fun Node.addToGraph(keyNode: Node): Node {
        graph.addVertex(this)
        graph.addEdge(keyNode, this)
        return this
    }
    // add all nodes as vertices to graph,
    // add edges among keys and values in production rules.
    this.prodRules.forEach { (nt_key, cond_ruleEdges) ->
        val nt = NonTerminal(nt_key)
        val keyNode = nodes.getOrPut(nt.toString()) {
            SimpleNode(Chain(listOf(nt))).apply {
                graph.addVertex(this)
            }
        }
        cond_ruleEdges.forEach { (cond, ruleEdges) ->
            if (cond == null){
                ruleEdges.forEach{
                    val value = it.substitution
                    when (value.size) {
                        0 -> {
                            SimpleNode(null).addToGraph(keyNode)
                        }
                        1 -> {
                            val ntVal = value.single().toString()
                            nodes.getOrPut(ntVal) {
                                SimpleNode(Chain(value)).addToGraph(keyNode)
                            }
                        }

                        else -> {
                            complexNodes.getOrPut(value.toString()) {
                                ComplexNode(Chain(value)).addToGraph(keyNode)
                            }
                        }
                    }
                }
            }
        }
    }
    // add edges among Non-terminal nodes and complex nodes
    complexNodes.values.forEach { c ->
        c.nodes?.filter { it !is Terminal }?.forEach {
            graph.addEdge(c, nodes[it.toString()]!!, ComplexEdge())
        }
    }
    return graph
}


//private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar? {
//    val startsymbol = graph.vertexSet().toList()[0].nodes?.first?.value?.let { NonTerminal(it.value) }
//    val grammar = mutableMapOf<String, MutableList<RuleEdge>>()

//    graph.vertexSet()
//        .filter { it !is ComplexNode }
//        .forEach {
//            val succs = graph.succs(it)
//            succs.forEach { succ ->
//                val symbols = mutableListOf<Symbol>()
//                if (succ is ComplexNode) {
//                    succ.nodes?.forEach { it_sn ->
//                        if (it_sn is NonTerminal) {
//                            symbols.add(NonTerminal(it_sn.value))
//                        } else if (it_sn is Terminal) {
//                            symbols.add(Terminal(it_sn.value))
//                        }
//                    }
//                } else { // simple node has only one value anyway
//                    val myNode = succ.nodes?.first?.value
//                    if (myNode != null) {
//                        if (myNode is NonTerminal) {
//                            symbols.add(NonTerminal(myNode.value))
//                        } else {
//                            symbols.add(Terminal(myNode.value))
//                        }
//                    }
//                }
//                val redge = RuleEdge(substitution = symbols, weight = 1.0F)
//                if (it.toString() in grammar.keys) {
//                    grammar[it.toString()]?.add(redge)
//                } else {
//                    grammar[it.toString()] = mutableListOf(redge)
//                }
//            }
//        }

//    return startsymbol?.let { Grammar(it, grammar) }
//}

fun <V, E> DefaultDirectedGraph<V, E>.preds(vert: V): MutableList<V> {
    return Graphs.predecessorListOf(this, vert)!!
}

fun <V, E> DefaultDirectedGraph<V, E>.succs(vert: V): MutableList<V> {
    return Graphs.successorListOf(this, vert)!!
}

private fun DefaultDirectedGraph<Node, DefaultEdge>.simplify(): DefaultDirectedGraph<Node, DefaultEdge> {
    val nodesToProcess = vertexSet().toMutableList()

    while (nodesToProcess.isNotEmpty()) {
        val node = nodesToProcess.removeFirst()
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

                        nodesToProcess.addAll(listOf(pred, succ))


                    } else if (getEdge(pred, node)!!.javaClass == ComplexEdge().javaClass
                        && pred is ComplexNode
                        && node is SimpleNode
                        && succ is SimpleNode
                        && succ.nodes?.all { it is Terminal } == true
                    ) {
                        // child node of a complex node must always SimpleNode with only one non-terminal
                        // pred (ComplexNode) -> node (SimpleNode) -> succ (SimpleNode: only terminals)
                        // modify pred to point to succ
                        addEdge(pred, succ, ComplexEdge())
                        removeVertex(node)

                        // remove this succ and update ComplexNode
                        val oldNT = node.nodes?.single() // The Non-terminal
                        // find the chain link in "pred" containing this non-terminal
                        val link = pred.nodes!!.linkIterator().asSequence()
                            .single { it.value.toString() == oldNT.toString() }
                        // replace the non-terminal with all terminals from "succ" and remove the "succ"
                        pred.nodes!!.replace(link, succ.nodes)
                        removeVertex(succ)

                        // Special case: if pred (the ComplexNode) contains only terminals we convert it to a SimpleNode
                        if (pred.nodes!!.all { it is Terminal }) {
                            val newNode = SimpleNode(pred.nodes) as Node

                            addVertex(newNode)
                            // eliminate "pred" by letting its predecessors point to "newNode"
                            preds(pred).forEach {
                                addEdge(it, newNode)
                            }
                            removeVertex(pred)
                            nodesToProcess.addAll(preds(newNode))
                        }
                    }
                }
        }
    }
    return this
}
