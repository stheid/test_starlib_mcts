import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class PCFGSuccGenTest {
    val prodRules = mapOf(
        Symbol.NonTerminal("A") to listOf(
            Rule(listOf(Symbol.NonTerminal("A"), Symbol.Terminal("a")), 1.0),
            Rule(listOf(Symbol.Terminal("a")), 1.0)
        )
    )

    @Test
    fun getIterativeGenerator() {
    }

    @Test
    fun generateSuccessors() {
        val succs = PCFGSuccGen(prodRules).generateSuccessors(
            Symbols.fromCollection(listOf(Symbol.NonTerminal("A")))
        ).map { it.to.symbols }

        assert(
            succs == listOf(
                listOf(Symbol.NonTerminal("A"), Symbol.Terminal("a")),
                listOf( Symbol.Terminal("a"))
            )
        )
    }

    @Test
    fun generateSuccessorsForLeaf() {
        val succs = PCFGSuccGen(prodRules).generateSuccessors(
            Symbols.fromCollection(listOf(Symbol.Terminal("a")))
        ).map { it.to.symbols }

        assert(succs == listOf<List<Symbol>>())
    }
}