package isml.aidev.starlib

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import isml.aidev.util.Unique
import org.api4.java.datastructure.graph.implicit.IGraphGenerator
import org.api4.java.datastructure.graph.implicit.ISingleRootGenerator
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator

class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<SymbolsNode, RuleEdge> {
    private val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator(): ISingleRootGenerator<SymbolsNode> {
        return ISingleRootGenerator<SymbolsNode> { SymbolsNode(mutableListOf(Unique(grammar.startSymbol))) }
    }

    override fun getSuccessorGenerator(): ISuccessorGenerator<SymbolsNode, RuleEdge> {
        return this.successorGenerator
    }
}