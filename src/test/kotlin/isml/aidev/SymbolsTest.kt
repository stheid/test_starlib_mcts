package isml.aidev

import isml.aidev.Symbol
import isml.aidev.Symbols
import org.junit.jupiter.api.Test

internal class SymbolsTest {

    @Test
    fun createChild() {
        val child = Symbols.fromCollection(listOf(Symbol.NonTerminal("A")))
            .createChild( listOf(Symbol.NonTerminal("B")))
        assert(child.nonTerminalIndices == listOf(0))
    }

    @Test
    fun hasNoChilds() {
        val child = Symbols.fromCollection(listOf(Symbol.NonTerminal("A")))
            .createChild( listOf(Symbol.Terminal("b")))
        assert(child.isTerminalsOnly)
    }

    @Test
    fun randomNT() {
        val symbols =  Symbols.fromCollection(
            listOf(Symbol.NonTerminal("A"), Symbol.NonTerminal("B"))
        )

        assert(symbols.expandableNT == symbols.expandableNT)
    }

    @Test
    fun isTerminalsOnly() {
    }
}