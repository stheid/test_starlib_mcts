package isml.aidev

import isml.aidev.util.Unique
import org.junit.jupiter.api.Test

internal class SymbolsNodeTest {
    private val grammar = Grammar.fromFile(SymbolsNodeTest::class.java.getResource("/test_gram.yaml")!!.path)

    @Test
    fun createChild() {
        var node = SymbolsNode(Unique(grammar.startSymbol))
        listOf(
            grammar.prodRules[Symbol.NonTerminal("S")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![1],
        ).forEach{
            node = node.createChild(it)
        }

        println(node.isFinished)
    }

    @Test
    fun createChild2() {
        var node = SymbolsNode(Unique(grammar.startSymbol))
        listOf(
            grammar.prodRules[Symbol.NonTerminal("S")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![0],
            grammar.prodRules[Symbol.NonTerminal("A")]!![0],
        ).forEach{
            node = node.createChild(it)
        }

        println(node.isFinished)
    }
}