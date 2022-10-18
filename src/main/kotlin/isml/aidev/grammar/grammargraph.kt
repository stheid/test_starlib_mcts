package isml.aidev.grammar

import isml.aidev.RuleEdge
import isml.aidev.grammar.Symbol.NonTerminal
import isml.aidev.grammar.Symbol.Terminal
import isml.aidev.util.Chain
import org.jgrapht.Graphs
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge

open class Node(open val nodes: Chain<Symbol>?)

// ConditionalNode can contain non-terminals based on conditions in annotated grammar
// Mustn't be a dataclass since there could be nts with the same condition
class ConditionalNode(val cond: String?, override val nodes: Chain<Symbol>? = null) : Node(nodes)

// ComplexNode can contain multiple terminals and non-terminals
data class ComplexNode(override val nodes: Chain<Symbol>?) : Node(nodes)

// SimpleNodes contain either a single non-terminal or a list of terminals
data class SimpleNode(override val nodes: Chain<Symbol>?) : Node(nodes) {
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

class RuleEdgeSimplify(var statement: String?, var weight: Float = 0.0f) : DefaultEdge()
class ComplexEdge : DefaultEdge()
class CondEdge : DefaultEdge()


fun ProdRules.processAsGraph(doSimplify: Boolean, startSymbol: NonTerminal): ProdRules =
    this.toGraph().run { if (doSimplify) simplify() else this }.normalizeWeights().toProdRules(startSymbol)

internal fun ProdRules.toGraph(): DefaultDirectedGraph<Node, DefaultEdge> {
    // Example JGraphT code https://jgrapht.org/guide/UserOverview
    val graph = DefaultDirectedGraph<Node, DefaultEdge>(DefaultEdge::class.java)
    val nodes = mutableMapOf<String, Node>()
    val complexNodes = mutableMapOf<String, Node>()

    fun Node.addAsChildOf(parent: Node, EdgeType: DefaultEdge = DefaultEdge()): Node {
        graph.addVertex(this)
        graph.addEdge(parent, this, EdgeType)
        return this
    }

    // add all nodes as vertices to graph,
    // add edges between keys and values in production rules.
    this.forEach { (nt_key, cond_ruleEdges) ->
        val nt = NonTerminal(nt_key)
        val keyNode = nodes.getOrPut(nt.value) {
            SimpleNode(Chain(listOf(nt))).apply {
                graph.addVertex(this)
            }
        }

        val nAlternatives = cond_ruleEdges.size

        cond_ruleEdges.forEach { (cond, ruleEdges) ->
            // if there are multiple options we need to create conditional Nodes,
            // otherwise we attach the children directly to the NT Node
            val anchor = if (nAlternatives == 1) keyNode else ConditionalNode(cond).addAsChildOf(
                keyNode,
                CondEdge()
            )
            ruleEdges.forEach {
                val value = it.substitution
                val stmt = it.expression
                val weight = it.weight
                when (value.size) {
                    0 -> {
                        SimpleNode(null)
                            .addAsChildOf(anchor, RuleEdgeSimplify(stmt, weight))
                    }

                    1 -> {
                        val ntVal = value.single().value
                        nodes.getOrPut(ntVal) { SimpleNode(Chain(value)) }
                            .addAsChildOf(anchor, RuleEdgeSimplify(stmt, weight))
                    }

                    else -> {
                        complexNodes.getOrPut(value.joinToString { it.value }) { ComplexNode(Chain(value)) }
                            .addAsChildOf(anchor, RuleEdgeSimplify(stmt, weight))
                    }
                }
            }
        }
    }
    // add edges among Non-terminal nodes and complex nodes
    complexNodes.values.forEach { c ->
        c.nodes?.filter { it !is Terminal }?.forEach {
            graph.addEdge(c, nodes[it.value]!!, ComplexEdge())
        }
    }
    return graph
}


private fun <V, E> DefaultDirectedGraph<V, E>.preds(vert: V) = Graphs.predecessorListOf(this, vert)!!
private fun <V, E> DefaultDirectedGraph<V, E>.succs(vert: V) = Graphs.successorListOf(this, vert)!!

internal fun DefaultDirectedGraph<Node, DefaultEdge>.simplify(): DefaultDirectedGraph<Node, DefaultEdge> {
    val nodesToProcess = vertexSet().toMutableList()

    while (nodesToProcess.isNotEmpty()) {
        val node = nodesToProcess.removeFirst()
        // The first condition below is important because on deletion of certain nodes during
        // complex nodes' simplification, it throws an error.
        if (node in this.vertexSet() && succs(node).size == 1 && preds(node).size == 1) {
            Triple(preds(node).single(), node, succs(node).single())
                .also { (pred, node, succ) ->
                    val firstEdge = getEdge(pred, node)
                    val secondEdge = getEdge(node, succ)
                    when {
                        // Only Simple Edges involved
                        firstEdge is RuleEdgeSimplify && secondEdge is RuleEdgeSimplify -> {
                            // pred, node and succ are primitive nodes
                            // make nodes predecessor refer to nodes successor
                            addEdge(
                                pred, succ, RuleEdgeSimplify(
                                    listOfNotNull(
                                        firstEdge.statement,
                                        secondEdge.statement
                                    ).joinToString(";"), firstEdge.weight
                                )
                            )
                            removeVertex(node)
                            nodesToProcess.addAll(listOf(pred, succ))
                        }
                        // complex node with simple node child and grandchild
                        firstEdge is ComplexEdge
                                && secondEdge is RuleEdgeSimplify
                                && pred is ComplexNode
                                && node is SimpleNode
                                && succ is SimpleNode
                                && succ.nodes?.all { it is Terminal } == true -> {
                            // child node of a complex node must always SimpleNode with only one non-terminal
                            // pred (ComplexNode) -> node (SimpleNode) -> succ (SimpleNode: only terminals)
                            // modify pred to point to succ
                            addEdge(pred, succ, ComplexEdge())
                            removeVertex(node)

                            // remove this succ and update ComplexNode
                            val oldNT = node.nodes?.single() // The Non-terminal
                            // find the chain link in "pred" containing this non-terminal
                            val link = pred.nodes!!.linkIterator().asSequence().single { it.value.symbolEqual(oldNT) }
                            // replace the non-terminal with all terminals from "succ" and remove the "succ"
                            pred.nodes!!.replace(link, succ.nodes)
                            removeVertex(succ)

                            // integrate the condition of the removed edge to the edges pointing to "pred"
                            preds(pred).forEach {
                                (getEdge(it, pred) as RuleEdgeSimplify).statement =
                                    listOfNotNull(
                                        (getEdge(it, pred) as RuleEdgeSimplify).statement,
                                        secondEdge.statement
                                    ).joinToString(";")
                            }

                            // Special case: if pred (the ComplexNode) contains only terminals we convert it to a SimpleNode
                            if (pred.nodes!!.all { it is Terminal }) {
                                val newNode = SimpleNode(pred.nodes) as Node

                                addVertex(newNode)
                                // eliminate "pred" by letting its predecessors point to "newNode"
                                preds(pred).forEach {
                                    val oldEdge = getEdge(it, pred) as RuleEdgeSimplify
                                    addEdge(
                                        it, newNode,
                                        RuleEdgeSimplify(
                                            (getEdge(it, pred) as RuleEdgeSimplify).statement,
                                            oldEdge.weight
                                        )
                                    )
                                }
                                removeVertex(pred)
                                nodesToProcess.addAll(preds(newNode))
                            }
                        }
                    }
                }
        }
    }
    return this
}

internal fun DefaultDirectedGraph<Node, DefaultEdge>.normalizeWeights(): DefaultDirectedGraph<Node, DefaultEdge> {
// for all simple edges:
    // get sibling edges
    // normalize weights
    // mark all siblings as processed
    val unprocessed = edgeSet().filterIsInstance<RuleEdgeSimplify>().toMutableList()
    while (unprocessed.isNotEmpty()) {
        val currEdge = unprocessed.removeFirst()
        val siblings = outgoingEdgesOf(getEdgeSource(currEdge))
        val totalWeight = siblings.map { (it as RuleEdgeSimplify).weight }.toFloatArray().sum()
        siblings.forEach { (it as RuleEdgeSimplify).weight /= totalWeight }
        unprocessed.removeAll(siblings)
    }
    return this
}

internal fun DefaultDirectedGraph<Node, DefaultEdge>.toProdRules(startSymbol: NonTerminal): ProdRules {
    val prodrules = mutableMapOf<String, MutableMap<String?, MutableList<RuleEdge>>>()
    // shortest paths from the root
    val shortestPathToRoot =
        BellmanFordShortestPath(this).getPaths(vertexSet().single {
            it.nodes?.singleOrNull()?.run { value == startSymbol.value } ?: false
        })

    fun createRules(nt: String, parent: Node, successors: List<Node>, cond: String? = null) {
        successors.forEachIndexed { i, succ ->
            val substitution = mutableListOf<Symbol>()
            val ruleEdges = successors.map { getEdge(parent, it) as RuleEdgeSimplify }

            succ.nodes?.forEach { it_sn ->
                when (it_sn) {
                    is NonTerminal -> substitution.add(
                        NonTerminal(
                            it_sn.value,
                            // Nodes that are closer to the root are more abstract
                            -vertexSet().filter {
                                // get all nodes that use the non-terminal
                                it.nodes?.any { sym ->
                                    sym.value == it_sn.value
                                } == true
                            }.map {
                                // calculate average distance of those nodes to the root node
                                // only count simple nodes. Last node might be simple OR complex, therefore we omit it from the calculation
                                shortestPathToRoot.getPath(it).vertexList.dropLast(1)
                                    .filterIsInstance<SimpleNode>().size
                            }.toIntArray().average().toFloat()
                        )
                    )

                    is Terminal -> substitution.add(Terminal(it_sn.value))
                }
            }

            prodrules.getOrPut(nt) {
                // in case the non-terminal has not been added to the grammar yet
                mutableMapOf()
            }// for that NT
                .getOrPut(cond) {
                    // in case the condition has not been added yet
                    mutableListOf()
                }.add(
                    RuleEdge(
                        substitution,
                        (getEdge(parent, succ) as? RuleEdgeSimplify)?.statement,
                        ruleEdges[i].weight
                    )
                )
        }
    }

    // iterate over all simple nodes and conditional nodes. Simple nodes with conditions are filtered,
    // such that every rule is only processed exactly once
    vertexSet()
        .filter { it !is ComplexNode }
        .forEach {
            val succs = succs(it)
            if (it !is ConditionalNode && !succs(it).any { it_ -> it_ is ConditionalNode }) {
                // SimpleNodes (with no condition): create rules from non-terminal to the rule substitutions
                createRules(it.nodes?.first?.value?.value.toString(), it, succs)
            } else if (it is ConditionalNode) {
                // ConditionalNodes: create rules from its non-terminal to the rule substitutions
                val nt = preds(it).single().nodes?.first?.value?.value.toString()
                createRules(nt, it, succs, it.cond)
            }
        }

    return prodrules
}