package isml.aidev

import org.junit.jupiter.api.Test

internal class SymbolTest {
    @Test
    fun ntEquality() = assert(Symbol.NonTerminal("f") == Symbol.NonTerminal("f"))

    @Test
    fun uniqueNtEquality() = assert(Symbol.UniqueNT("f") != Symbol.UniqueNT("f"))

    @Test
    fun uniqueNtEquality2() {
        val nt = Symbol.NonTerminal("f")
        assert(Symbol.UniqueNT(nt) != Symbol.UniqueNT(nt))
    }
}

