package isml.aidev

import ai.libs.jaicore.search.model.other.SearchGraphPath
import com.charleskorn.kaml.*
import isml.aidev.Symbol.NonTerminal
import isml.aidev.Symbol.Terminal
import isml.aidev.util.toWord
import java.io.File

sealed class Symbol(open val value: String) {
    data class Terminal(override val value: String) : Symbol(value) {
        override fun toString() = "term: $value"
    }

    /**
     * Mustn't be a data class as we need to uniquely identify non-terminals by their object reference.
     * For a non-unique comparison we can use the Non-Terminals internal string.
     */
    class NonTerminal(override val value: String, val vars: MutableMap<String, Int> = mutableMapOf()) : Symbol(value) {
        override fun toString() = "nt: $value"

        fun copy() = NonTerminal(value, vars)
    }
}

typealias ProdRules = Map<String, List<RuleEdge>>

data class Grammar(val startSymbol: NonTerminal, val prodRules: ProdRules) {
    companion object {
        fun fromFile(path: String): Grammar {
            val root = Yaml.default.parseToYamlNode(File(path).bufferedReader().readText())
            val grammar = root.yamlMap.entries.entries.associate { (key, value) ->
                key.content to
                        value
                            .parseRule()
                            // we need to flatmap because there are range like rules (byte-> \x00 .. \xff) that will be expanded into multiple rules
                            .flatMap { (nt, weight) ->
                                nt.toRuleEdges(weight)
                            }
            }.simplify()

            return Grammar(NonTerminal(grammar.entries.first().key), grammar)
        }
    }

    fun sample(): String {
        var nt: NonTerminal? = startSymbol

        var currSymbol = SymbolsNode(nt)
        val path = SearchGraphPath<SymbolsNode, RuleEdge>(currSymbol)

        while (nt != null) {
            val rule = sampleRule(nt)
            currSymbol = currSymbol.createChild(rule)

            // SymbolNode will automatically select the next non-terminal to be processed
            nt = currSymbol.currNT
            path.extend(currSymbol, rule)
        }
        return path.toWord()
    }

    private fun sampleRule(nt: NonTerminal): RuleEdge {
        return prodRules[nt.value]!!.toList().let { rules ->
            rules.choice(
                p = rules.map { it.weight.toDouble() }.toDoubleArray().normalize()
            )
        }
    }
}

private fun Map<String, List<RuleEdge>>.simplify(): Map<String, List<RuleEdge>> {
    // find simple NT->[[term+]] rules, so basically non-terminals that are only part of one single rule that is made up entirely by terminals
    val groups = this.entries.groupBy { (_, value) -> value.size == 1 && value[0].substitution.all { it is Terminal } }
        .entries.associate { it.key to it.value.associate { (k,v) -> k to v } }
    val complex = groups[false] ?: emptyMap()
    val simple = groups[true] ?: emptyMap()


    val simpleSub = simple.entries.associate { it.key to it.value.single().substitution }

    // substitute uses of this NTs in other rules
    return complex.entries.associate { (key, value) ->
        key to value.map { rule ->
            val newSubs = rule.substitution.flatMap { simpleSub[it.value] ?: listOf(it) }
            RuleEdge(newSubs, rule.weight)
        }
    }
}

private fun YamlNode.parseRule(): Map<String, Float> {
    return if (this is YamlList)
        this.yamlList.items.associate { it.yamlScalar.content to 1.0f }
    else
        this.yamlMap.entries.entries.associate { (key, value) -> key.content to value.yamlScalar.toFloat() }
}

private fun List<String>.tryExpand(): List<Terminal>? {
    return if (this.size == 3 && this[0].isQuoted() && this[1] == "." && this[2].isQuoted()) {
        // convert first and last character to byte character value
        val start = this[0].unQuote().single().code
        val end = this[2].unQuote().single().code
        start.rangeTo(end).map { Terminal(it.toChar().toString()) }
    } else
        null
}

private fun String.toRuleEdges(weight: Float): List<RuleEdge> = // split
    // quoted strings can have whitespaces (print), the nonterminals must only be composed by printable non-whitespace chars (graph)
    Regex("""((?<!")"\p{Print}*?"(?!"))|\p{Graph}+""").findAll(this).toList()
        .map { it.value }
        .let {
            it.tryExpand()?.map { term -> RuleEdge(listOf(term)) }
                ?: listOf(
                    RuleEdge(
                        it.map { if (it.isQuoted()) Terminal(it.unQuote()) else NonTerminal(it) },
                        weight
                    )
                )
        }

private fun String.isQuoted(): Boolean = this.startsWith("\"") && this.endsWith("\"")
private fun String.unQuote(): String = this.drop(1).dropLast(1)
    .replace(Regex("""\\x\p{XDigit}\p{XDigit}""")) { it.value.drop(2).toInt(16).toChar().toString() }
    .replace(Regex("""\\u\p{XDigit}\p{XDigit}\p{XDigit}\p{XDigit}""")) {
        it.value.drop(2).toInt(16).toChar().toString()
    }
