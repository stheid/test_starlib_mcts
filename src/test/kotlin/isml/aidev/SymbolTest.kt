package isml.aidev

import isml.aidev.util.Unique
import org.junit.jupiter.api.Test

internal class SymbolTest {
    val nt = Symbol.NonTerminal("f")

    @Test
    fun ntEquality() = assert(nt == nt)

    @Test
    fun uniqueNtEquality() = assert(Unique(nt) != Unique(nt))

    @Test
    fun uniqueNtEquality2() {
        val uniqueNT = Unique(nt)
        assert(uniqueNT == uniqueNT)
    }
}

