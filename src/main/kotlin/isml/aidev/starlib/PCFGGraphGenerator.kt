package isml.aidev.starlib

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import org.api4.java.datastructure.graph.implicit.IGraphGenerator
import org.api4.java.datastructure.graph.implicit.ISingleRootGenerator

class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<SymbolsNode, RuleEdge> {
    private val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator() =
        ISingleRootGenerator { SymbolsNode(grammar.startSymbol) }

    override fun getSuccessorGenerator() =
        this.successorGenerator
}