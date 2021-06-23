package isml.aidev

import org.junit.jupiter.api.Test

internal class MathKtTest {
    @Test
    fun normalize() {
        assert(doubleArrayOf(1.0, 1.0).normalize().contentEquals(doubleArrayOf(.5, .5)))
    }

    @Test
    fun cumsum() {
        assert(doubleArrayOf(1.0, 1.0).cumsum().contentEquals(doubleArrayOf(1.0, 2.0)))
    }

    @Test
    fun cumsum2() {
        assert(doubleArrayOf(1.0, 2.0, 1.0).cumsum().contentEquals(doubleArrayOf(1.0, 3.0, 4.0)))
    }

    @Test
    fun choice() {
        val res = listOf("helo", "foo").choice(doubleArrayOf(0.0, 1.0))
        assert(res == "foo") { res }
    }
}