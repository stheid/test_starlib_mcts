package isml.aidev.starlib

import isml.aidev.ProdRules
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import org.api4.java.datastructure.graph.implicit.INewNodeDescription
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator

class PCFGSuccGen(private val prodRules: ProdRules) : ISuccessorGenerator<SymbolsNode, RuleEdge> {
    private val cache = HashMap<SymbolsNode, List<INewNodeDescription<SymbolsNode, RuleEdge>>>()

    override fun generateSuccessors(node: SymbolsNode) =
        cache.computeIfAbsent(node) {
            // else create new children and filter for existing ones
            node.currNT?.run {
                // TODO
                prodRules[value]!!.values.single()
                    .map { node.createChild(it) to it }
                    .map { (child, rule) ->
                        object : INewNodeDescription<SymbolsNode, RuleEdge> {
                            override fun getFrom() = node
                            override fun getTo() = child
                            override fun getArcLabel() = rule
                        }
                    }
            } ?: emptyList()
        }
}