package isml.aidev

import isml.aidev.util.Chain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ChainTest {
    val long = listOf(4, 5, 6, 7, 8, 9, 10)
    val short = listOf(1, 2, 3)

    @Test
    fun test_basic_constructor() {
        val chain = Chain(long)
        assert(long.size == chain.toList().size)
    }

    @Test
    fun test_emptylist() {
        assertThrows<NoSuchElementException> {
            Chain(emptyList<Int>())
        }
    }

    @Test
    fun test_entries() {
        val chain = Chain(long)

        assert(chain.first?.value == long.first())
        assert(chain.last?.value == long.last())

        assert(chain.first?.pred == null)
        assert(chain.last?.succ == null)
    }

    @Test
    fun test_check_connection() {
        checkConnection(Chain(long))
    }

    @Test
    fun test_substitute_first() {
        val chain = Chain(long)
        val subchain = Chain(short)

        assert(chain.first?.value == long.first())
        chain.first?.substitute(subchain)
        assert(chain.first?.value == short.first())
        checkConnection(chain)
    }

    @Test
    fun test_substitute_last() {
        val chain = Chain(long)
        val subchain = Chain(short)

        assert(chain.last?.value == long.last())
        chain.last?.substitute(subchain)
        assert(chain.last?.value == short.last())
        checkConnection(chain)
    }

    @Test
    fun test_substitute_middle() {
        val chain = Chain(long)
        val subchain = Chain(short)
        chain.first?.succ?.substitute(subchain)

        // assert first and last values are not changed
        assert(chain.first?.value == long.first())
        assert(chain.last?.value == long.last())
        // assert 2nd element is 1
        chain.first?.succ?.run { assert(value == short.first()) }
        checkConnection(chain)
    }

    fun <T> checkConnection(chain: Chain<T>) {
        var link = chain.first
        while (link?.succ != null)
            link = link.succ!!
        assert(link == chain.last)

        link = chain.last
        while (link?.pred != null)
            link = link.pred!!
        assert(link == chain.first)
    }
}