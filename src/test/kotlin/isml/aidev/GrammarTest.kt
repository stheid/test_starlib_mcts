package isml.aidev

import org.junit.jupiter.api.Test

class GrammarTest {
    @Test
    fun readNewStyleGrammar() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("xml_gen.yaml")?.path!!)
        println(grammar)
    }

    @Test
    fun readJsonGrammar() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("json_simple_gram.yml")?.path!!)
        println(grammar)
    }

    @Test
    fun readSimpleGrammar() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("abc.yaml")?.path!!)
        println(grammar)
    }

    @Test
    fun readAnnotGrammar() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_unconstrained.yaml")?.path!!)
        println(grammar)
    }

    @Test
    fun sampleSimpleGram() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("abc.yaml")?.path!!)
        val byteseq = grammar.sample()
        println(byteseq)
    }

    @Test
    fun sampleAnotatedGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_unconstrained.yaml")?.path!!)
        val byteseq = grammar.sample()
        println(byteseq)

        //File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }

    @Test
    fun sampleAnotatedLocalGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_localvar.yaml")?.path!!)
        assert(grammar.sample() == "133355555")
    }

    @Test
    fun sampleAnotatedGlobalGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_globvar.yaml")?.path!!)
        val result = grammar.sample()
        println(result)
        assert(result.length == 5)

        //File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }

    @Test
    fun sampleXmlAnnotGram() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("xml_gen_annot.yaml")?.path!!)
        val byteseq = grammar.sample()
        println(byteseq)

        //File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }
}