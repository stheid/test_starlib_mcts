package isml.aidev.grammar

import isml.aidev.util.exportParseTree
import isml.aidev.util.toWord
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
        val result = grammar.sample()
        result.exportParseTree(File("parsetree.dot"))
        println(result.toWord())
    }

    @Test
    fun sampleAnotatedGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_unconstrained.yaml")?.path!!)
        println(grammar.sample())
        for (i in 1..100){
            println(grammar.sample())
        }

        //File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }

    @Test
    fun sampleAnotatedLocalGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_localvar.yaml")?.path!!)
        assert(grammar.sample().toWord() == "133355555")
        for (i in 1..100) {
            println(grammar.sample())
        }
    }

    @Test
    fun sampleAnotatedGlobalGram() {
        val grammar =
            Grammar.fromFile(this::class.java.classLoader.getResource("simple_annotated_globvar.yaml")?.path!!)
        val result = grammar.sample().toWord()
        println(result)
        assert(result.length == 5)
        for (i in 1..100) {
            println(grammar.sample())
        }

        //File("out.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
    }

    @Test
    fun sampleXmlAnnotGram() {
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("xml_gen_annot.yaml")?.path!!)
        for (i in 1..500) {
            val byteseq = grammar.sample()
            println(byteseq)
//            File("annotated/$i.bin").writeBytes(byteseq.map { it.code.toByte() }.toByteArray())
        }
    }

    @Test
    fun sampleJsAnnotatedGram() {
        val fname = "/home/ajrox/Programs/aidev_zest_bench/src/main/resources/js_gen_inputs/"
        val grammar = Grammar.fromFile(this::class.java.classLoader.getResource("js_gen.yml")?.path!!)
        for (i in 1..50) {
            val result = grammar.sample()
//            result.exportParseTree(File("$fname/parsetree_${i}.dot"))
//            println(result.toWord())
            File("$fname/$i.bin").writeBytes(result.toWord().map { it.code.toByte() }.toByteArray())
        }
   }
}