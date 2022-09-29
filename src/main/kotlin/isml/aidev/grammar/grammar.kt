package isml.aidev.grammar

import ai.libs.jaicore.search.model.other.SearchGraphPath
import com.charleskorn.kaml.*
import isml.aidev.RuleEdge
import isml.aidev.SymbolsNode
import isml.aidev.choice
import isml.aidev.grammar.Symbol.NonTerminal
import isml.aidev.grammar.Symbol.Terminal
import isml.aidev.normalize
import isml.aidev.util.Evaluator
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
    class NonTerminal(override val value: String) : Symbol(value) {
        override fun toString() = "nt: $value"

        fun copy() = NonTerminal(value)
    }
}

typealias ProdRules = Map<String, Map<String?, List<RuleEdge>>>

data class Grammar(val startSymbol: NonTerminal, val prodRules: ProdRules) {
    companion object {
        fun fromFile(path: String, doSimplify: Boolean = true): Grammar {
            val root = Yaml.default.parseToYamlNode(File(path).bufferedReader().readText())
            val grammar = root.yamlMap.entries.entries.map { (leftProduction, rightProduction) ->
                val (NT, cond) = leftProduction.content.splitNtAndCond()
                // the default condition is "true"
                // (true literal in eval does not work in eval, we need to use another true expression instead)
                Triple(NT, cond, rightProduction.parseRule()
                    // we need to flatmap because there are range like rules (byte-> \x00 .. \xff) that will be expanded into multiple rules
                    .flatMap { (substitution, weight) ->
                        substitution.toRuleEdges(weight)
                    })
            }.groupBy { (NT, _, _) -> NT }.entries.associate { (NT, group) ->
                NT to group.associate { (_, cond, rules) -> cond to rules }
            }.run { if (doSimplify) simplify() else this }

            return Grammar(NonTerminal(grammar.entries.first().key), grammar)
        }

        fun fromResource(path: String, doSimplify: Boolean = true): Grammar {
            return fromFile(this::class.java.classLoader.getResource(path)?.path!!, doSimplify)
        }
    }

    fun validRules(nt: NonTerminal, vars: Map<String, Int>? = null): List<RuleEdge> {
        val ruleAlternatives = prodRules[nt.value] ?: error("Did not find NT ${nt.value}")
        val trueCondition = ruleAlternatives.keys.filterNotNull().singleOrNull { cond ->
            Evaluator.eval(cond, vars)
        }
        if (trueCondition !in ruleAlternatives)
            error("more or less than one production rule with a fulfilled condition. (${nt.value}")
        return ruleAlternatives[trueCondition]!!
    }

    fun sample(): String {
        var nt: NonTerminal? = startSymbol

        var currSymbol = SymbolsNode(nt)
        val path = SearchGraphPath<SymbolsNode, RuleEdge>(currSymbol)

        while (nt != null) {
            // sample Rule from valid rules
            val rule = validRules(nt, currSymbol.vars(nt)).let { rules ->
                rules.choice(rules.map { it.weight.toDouble() }.toDoubleArray().normalize())
            }
            currSymbol = currSymbol.createChild(rule)

            // SymbolNode will automatically select the next non-terminal to be processed
            nt = currSymbol.currNT
            path.extend(currSymbol, rule)
        }
        return path.toWord()
    }
}


private fun YamlNode.parseRule(): Map<String, Float> {
    return if (this is YamlList) this.yamlList.items.associate { it.yamlScalar.content to 1.0f }
    else this.yamlMap.entries.entries.associate { (key, value) -> key.content to value.yamlScalar.toFloat() }
}

private fun List<String>.tryExpand(): List<Terminal>? {
    return if (this.size == 3 && this[0].isQuoted() && this[1] == "." && this[2].isQuoted()) {
        // convert first and last character to byte character value
        val start = this[0].unQuote().single().code
        val end = this[2].unQuote().single().code
        start.rangeTo(end).map { Terminal(it.toChar().toString()) }
    } else null
}

private fun String.toRuleEdges(weight: Float): List<RuleEdge> {
    // quoted strings can have whitespaces (print), the nonterminals must only be composed by printable non-whitespace chars (graph)
    val (rawSymbols, statement) = this.splitSymbolsAndStmt()

    return rawSymbols.splitSymbols().let { rawRule ->
        rawRule.tryExpand()
            // All expandable rules must consist entirely of Terminals
            ?.map { term -> RuleEdge(listOf(term)) } ?: listOf(let {
            // is not expandable, means we create on single rule out of the raw rule
            val symbols = rawRule.map { symbol ->
                // neutral statement without sideffects is pass
                if (symbol.isQuoted())
                    Terminal(symbol.unQuote())
                else
                    NonTerminal(symbol)
            }

            RuleEdge(symbols, statement, weight)
        })
    }
}

private fun String.isQuoted(): Boolean = this.startsWith("\"") && this.endsWith("\"")
private fun String.unQuote(): String = this.drop(1).dropLast(1)
    .replace(Regex("""\\x\p{XDigit}\p{XDigit}""")) { it.value.drop(2).toInt(16).toChar().toString() }
    .replace(Regex("""\\u\p{XDigit}\p{XDigit}\p{XDigit}\p{XDigit}""")) {
        it.value.drop(2).toInt(16).toChar().toString()
    }

private fun String.splitNtAndCond(): Pair<String, String?> {
    val matches = Regex("""(?<nt>\p{Graph}+?)(|(\[(?<cond>\p{Print}+)]))""").matchEntire(this)
    val nt = matches?.groups?.get("nt")?.value ?: error("could not parse NT")
    val cond = matches.groups["cond"]?.value
    return nt to cond
}

private fun String.splitSymbolsAndStmt(): Pair<String, String?> {
    val matches = Regex("""(\[(?<expr>\p{Print}+)]|) ?(?<symbols>.*)""").matchEntire(this)
    val rawSymbols = matches?.groups?.get("symbols")?.value ?: ""
    val statement = matches?.groups?.get("expr")?.value
    return rawSymbols to statement
}

private fun String.splitSymbols(): List<String> {
    return Regex("""((?<!")"\p{Print}*?"(?!"))|(\p{Graph}+)""").findAll(this).toList().map { it.value }
}