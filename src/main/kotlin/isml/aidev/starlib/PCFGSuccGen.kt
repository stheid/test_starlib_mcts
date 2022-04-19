package isml.aidev.starlib

import isml.aidev.ProdRules
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import org.api4.java.datastructure.graph.implicit.INewNodeDescription
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator
import java.util.concurrent.ConcurrentHashMap

class PCFGSuccGen(private val prodRules: ProdRules) : ISuccessorGenerator<SymbolsNode, RuleEdge> {
    private val adjacencyList = ConcurrentHashMap<SymbolsNode, List<INewNodeDescription<SymbolsNode, RuleEdge>>>()

    override fun generateSuccessors(node: SymbolsNode): List<INewNodeDescription<SymbolsNode, RuleEdge>> {
        if (adjacencyList.containsKey(node)) {
            // if expansion known. return childs
            return adjacencyList[node]!!
        }

        // else create new children and filter for existing ones
        val children = node.currNT?.let { nt ->
            prodRules[nt.value]!!
                .map { node.createChild(it) to it }
                .map { (child, rule) ->
                    object : INewNodeDescription<SymbolsNode, RuleEdge> {
                        override fun getFrom(): SymbolsNode {
                            return node
                        }

                        override fun getTo(): SymbolsNode {
                            return child
                        }

                        override fun getArcLabel(): RuleEdge {
                            return rule
                        }
                    }
                }
        } ?: emptyList()

        return children.also { adjacencyList[node] = it }
    }
}