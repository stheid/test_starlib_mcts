package isml.aidev

import org.junit.jupiter.api.Test
import java.io.File

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
    fun readAnnotGrammar() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_unconstrained.yaml")?.path!!)
        println(grammar)
    }

    @Test
    fun sample() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_unconstrained.yaml")?.path!!)
        val byteseq = grammar.sample()
        println(byteseq)

        File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }
}