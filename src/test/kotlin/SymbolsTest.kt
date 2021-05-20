import org.junit.jupiter.api.Test

internal class SymbolsTest {

    @Test
    fun createChild() {
        val child = Symbols.fromCollection(listOf(Symbol.NonTerminal("A")))
            .createChild(0, listOf(Symbol.NonTerminal("B")))
        assert(child.nonTerminalIndices == listOf(0))
    }

    @Test
    fun isTerminalsOnly() {
    }
}