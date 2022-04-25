package isml.aidev.starlib

import isml.aidev.Grammar
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import org.api4.java.ai.graphsearch.problem.IPathSearchInput
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester
import org.api4.java.datastructure.graph.implicit.IGraphGenerator


class PCFGSearchInput(private val grammar: Grammar) : IPathSearchInput<SymbolsNode, RuleEdge> {
    override fun getGraphGenerator(): IGraphGenerator<SymbolsNode, RuleEdge> =
        PCFGGraphGenerator(grammar)

    override fun getGoalTester(): IPathGoalTester<SymbolsNode, RuleEdge> =
        IPathGoalTester<SymbolsNode, RuleEdge> { it.head.isFinished }
}