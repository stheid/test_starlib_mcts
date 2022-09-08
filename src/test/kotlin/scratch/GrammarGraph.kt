package scratch

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.Symbol
import isml.aidev.Symbol.*
import isml.aidev.util.Chain
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File

open class Node(open var nodes: Chain<Symbol>) {
    val value: String
        get() = nodes.toString()

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Node
        return hashCode() == other.hashCode()
    }
}

data class ComplexNode(override var nodes: Chain<Symbol>) : Node(nodes)
data class SimpleNode(override var nodes: Chain<Symbol>) : Node(nodes)

class ComplexEdge : DefaultEdge()

fun main() {
//    val grammar = Grammar.fromResource("extremely_simple_gram.yml")
//    val grammar = Grammar.fromResource("simple_xml_gen.yml")
    val grammar = Grammar.fromResource("xml_gen.yaml")

    var graph = grammar.toGraph()
    val exporter = DOTExporter<Node, DefaultEdge> { """"${it.value.filter {  it.isLetterOrDigit() }}"""" }
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

//    exporter.exportGraph(graph, File("grammar_raw.dot").bufferedWriter())
    graph = graph.simplify(exporter)
    println(graph)
    exporter.exportGraph(graph, File("grammar_simple.dot").bufferedWriter())

    val simplegrammar = Grammar.fromGraph(graph)
    println(simplegrammar)
}


fun Grammar.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {

    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)
//    val nodes = mutableMapOf<String, Node>()
    val nodes = mutableMapOf<String, Node>()
    val complexNodes = mutableMapOf<String, Node>()
//    val complexNodes = mutableMapOf<String, Node>()

    fun Node.addToGraph(keyNode: Node): Node {
        graph.addVertex(this)
        graph.addEdge(keyNode, this)
        return this
    }
    // add all nodes as vertices to graph,
    // add edges among keys and values in production rules.
    this.prodRules.forEach { (key, vallue) ->
        val nt = NonTerminal(key)
//        val keyNode = nodes.getOrPut(nt.toString()) {
        val keyNode = nodes.getOrPut(nt.toString()) {
            SimpleNode(Chain(listOf(nt))).apply {
                graph.addVertex(this)
            }
        }

        vallue.forEach {
            val value = it.substitution
            when (value.size) {
                1 -> {
                    val ntVal = value.single().toString()
                    nodes.getOrPut(ntVal) {
//                    nodes.getOrPut(ntVal) {
                        SimpleNode(Chain(value)).addToGraph(keyNode)
                    }
                }
                else -> {
                    complexNodes.getOrPut(value.toString()) {
//                    complexNodes.getOrPut(value.toString()) {
                        ComplexNode(Chain(value)).addToGraph(keyNode)
                    }
                }
            }
        }
    }
    // add edges among Non-terminal nodes and complex nodes
    complexNodes.values.forEach { c ->
        c.nodes.filter { it !is Terminal }.forEach {
            graph.addEdge(c, nodes[it.toString()]!!, ComplexEdge())
//            graph.addEdge(c, nodes[it.toString()]!!, ComplexEdge())
        }
    }
    return graph
}


private fun Grammar.Companion.fromGraph(graph: DefaultDirectedGraph<Node, DefaultEdge>): Grammar? {
    val startsymbol = graph.vertexSet().toList()[0].nodes.first?.value?.let { NonTerminal(it.value) }
    val grammar = mutableMapOf<String, MutableList<RuleEdge>>()

    graph.vertexSet()
        .filter { it !is ComplexNode }
        .forEach {
            val succs = graph.succs(it)
            succs.forEach { succ ->
                val symbols = mutableListOf<Symbol>()
                if (succ is ComplexNode) {
                    succ.nodes.forEach{
                            it_sn ->
                        if (it_sn is NonTerminal) {
                            symbols.add(NonTerminal(it_sn.value))
                        } else if (it_sn is Terminal) {
                            symbols.add(Terminal(it_sn.value))
                        }
                    }
                } else{ // simple node has only one value anyway
                    val myNode = succ.nodes.first?.value
                    if(myNode != null){
                        if (myNode is NonTerminal){
                            symbols.add(NonTerminal(myNode.value))
                        }else{
                            symbols.add(Terminal(myNode.value))
                        }
                    }
                }
                val redge = RuleEdge(substitution = symbols, weight = 1.0F)
                if (it.value in grammar.keys) {
                    grammar[it.value]?.add(redge)
                } else {
                    grammar[it.value] = mutableListOf(redge)
                }
            }
        }

    return startsymbol?.let { Grammar(it, grammar) }
}

fun <V, E> DefaultDirectedGraph<V, E>.preds(vert: V): MutableList<V> {
    return Graphs.predecessorListOf(this, vert)!!
}

fun <V, E> DefaultDirectedGraph<V, E>.succs(vert: V): MutableList<V> {
    return Graphs.successorListOf(this, vert)!!
}

private fun <Node, DefaultEdge> DefaultDirectedGraph<Node, DefaultEdge>.simplify(exporter: DOTExporter<Node, DefaultEdge>? = null): DefaultDirectedGraph<Node, DefaultEdge> {
    val nodesToProcess = vertexSet().toMutableList()

    while (nodesToProcess.isNotEmpty()) {
        val node = nodesToProcess.removeFirst()
//        if (exporter != null) {
//            exporter.exportGraph(this, File("grammar.dot").bufferedWriter())
//        }
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

                        nodesToProcess.addAll(listOf( pred,succ))


                    } else if (getEdge(pred, node)!!.javaClass == ComplexEdge().javaClass
                        && pred is ComplexNode
                        && node is SimpleNode
                        && succ is SimpleNode
                        && succ.nodes.all { it is Terminal }
                    ) {
                        // complex node -> NT -> Terminal
                        // pred         -> node -> succ
                        // predecessor is a complex node
                        // modify pred to point to succ
                        addEdge(pred, succ, ComplexEdge() as DefaultEdge)
                        // modify the internal value of the predecessor according to the succ

                        removeVertex(node)
                        // todo: After above simplification, check if complex node points to only terminals and then remove them.

                        // remove this succ
                        // and update complexnode
                        // get node that we need to modify
                        val oldNT = node.nodes.single()
                        val link = pred.nodes.linkIterator().asSequence()
                            .single { it.value.toString() == oldNT.toString() }
                        pred.nodes.replace(link, succ.nodes)
                        removeVertex(succ)

                        if (pred.nodes.all { it is Terminal }) {
                            val newNode = SimpleNode(pred.nodes) as Node

                            addVertex(newNode)
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
