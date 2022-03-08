package isml.aidev

internal class SymbolsTest {
    private val grammar = Grammar.fromFile(SymbolsTest::class.java.getResource("/test_gram.yaml")!!.path)
/*
    @Test
    fun createChild() {
        var node = SymbolsNode(arrayListOf(grammar.startSymbol))
        listOf(
            grammar.prodRules[Symbol.NonTerminal("S")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![1],
        ).forEach{
            node = node.createChild(it)
        }

        println(node.isFinished)
        println(node.toWord())
    }

    @Test
    fun createChild2() {
        var node = SymbolsNode(arrayListOf(grammar.startSymbol))
        listOf(
            grammar.prodRules[Symbol.NonTerminal("S")]!![1],
            grammar.prodRules[Symbol.NonTerminal("A")]!![0],
            grammar.prodRules[Symbol.NonTerminal("A")]!![0],
        ).forEach{
            node = node.createChild(it)
        }

        println(node.isFinished)
        println(node.toWord())
    }

    @Test
    fun toWord() {
    }

 */
}