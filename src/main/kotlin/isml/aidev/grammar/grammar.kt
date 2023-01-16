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
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import java.io.File
import java.util.*
import kotlin.collections.LinkedHashMap

open class Nodex(open val value: String)

data class SimpleNodex(override val value: String): Nodex(value){

    override fun hashCode(): Int = value.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false
//        if (this.hashCode() != other.hashCode()) return false

        other as SimpleNodex

        if (value != other.value) return false

        return true
    }
}

open class Edgex(): DefaultEdge()

open class SimpleEdge(var local_var: String?): Edgex()


sealed class Symbol(open val value: String) {
    data class Terminal(override val value: String) : Symbol(value) {
        override fun toString() = "term: $value"
    }

    /**
     * Mustn't be a data class as we need to uniquely identify non-terminals by their object reference.
     * For a non-unique comparison we can use the Non-Terminals internal string.
     * @param nExpectedSymbols the expected number of terminal symbols this non-terminal will evaluate to in the current grammar
     */
    class NonTerminal(override val value: String, var abstractness: Float = 0.0f, var nExpectedSymbols: Float = 0.0f) :
        Symbol(value) {
        override fun toString() = "nt: $value (${"abs:%.2f".format(abstractness)}, ${"ExptdSymb:%.2f".format(nExpectedSymbols)})"

        fun copy() = NonTerminal(value)
    }

    fun symbolEqual(symbol: Symbol?) = value == symbol?.value
}

typealias ProdRules = Map<String, Map<String?, List<RuleEdge>>>

data class Grammar(val startSymbol: NonTerminal, val prodRules: ProdRules, val ntMap: MutableMap<String, NonTerminal>) {
    companion object {
        fun fromFile(path: String, doSimplify: Boolean = true): Grammar {
            val root = Yaml.default.parseToYamlNode(File(path).bufferedReader().readText())
            val startSymbol: NonTerminal
            val ntMap: MutableMap<String, NonTerminal> = mutableMapOf()

            val prodrules = root.yamlMap.entries.entries.map { (leftProduction, rightProduction) ->
                val (NT, cond) = leftProduction.content.splitNtAndCond()
                // the default condition is "true"
                // (true literal in eval does not work in eval, we need to use another true expression instead)
                Triple(NT, cond, rightProduction.parseRule()
                    // we need to flatmap because there are range like rules (byte-> \x00 .. \xff) that will be expanded into multiple rules
                    .flatMap { (substitution, weight) ->
                        val ruleEdges = substitution.toRuleEdges(weight, ntMap)
                        ruleEdges
                    })
            }.groupBy { (NT, _, _) -> NT }.entries.associate { (NT, group) ->
                NT to group.associate { (_, cond, rules) -> cond to rules }
            }.run {
                ntMap.getOrPut(keys.first()){NonTerminal(keys.first())}
                startSymbol = ntMap[keys.first()]!!
                // this will be able to simplify the graph
                // and add calculate the distance between non-terminals and leafs
                processAsGraph(doSimplify, startSymbol, ntMap)
            }

            return Grammar(startSymbol, prodrules, ntMap)
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
    fun sampleWithTree(): Pair<String, DefaultDirectedGraph<Nodex, Edge>> {
        var nt: NonTerminal? = startSymbol
        val graph = DefaultDirectedGraph<Nodex, Edge>(Edge::class.java)
        var currSymbol = SymbolsNode(nt)
        val path = SearchGraphPath<SymbolsNode, RuleEdge>(currSymbol)
        val aqueue: Deque<Int> = LinkedList()

        var n = 0
        val root = "gen_${n}"
        val aa: Nodex = SimpleNodex(value = root)
        graph.addVertex(aa)
        var a = aa.hashCode()
        aqueue.push(a)

        while (nt != null) {
            // sample Rule from valid rules
            val rule = validRules(nt, currSymbol.vars(nt)).let { rules ->
                rules.choice(rules.map { it.weight.toDouble() }.toDoubleArray().normalize())
            }
            a = aqueue.removeFirst()
            val leftnode = graph.vertexSet().single {
                it.hashCode() == a.hashCode()
            }
            rule.substitution.forEach{
                n += 1
                val sub = SimpleNodex((if(it !is Terminal) it.value else "term") +"_$n")
                graph.addVertex(sub)
                graph.addEdge( leftnode, sub)
                if(it !is Terminal){
                    aqueue.addLast(sub.hashCode())
                }
            }
            currSymbol = currSymbol.createChild(rule)

            // SymbolNode will automatically select the next non-terminal to be processed
            nt = currSymbol.currNT
            path.extend(currSymbol, rule)
        }

        return Pair(path.toWord(), graph)
    }
}


private fun YamlNode.parseRule(): Map<String, Float> {
    return if (this is YamlList) this.yamlList.items.associate { it.yamlScalar.content to 1.0f }
    else this.yamlMap.entries.entries.associate { (key, value) -> key.content to value.yamlScalar.toFloat() }
}

private fun String.isSingleChar(): Boolean = Regex(""".""").matches(this)
private fun String.isInt(): Boolean = this.toIntOrNull() != null

private fun List<String>.tryExpand(): List<Terminal>? {
    return if (this.size == 3 && this[0].isQuoted() && this[1] == "." && this[2].isQuoted()) {
        val start_r = this[0].unQuote()
        val end_r = this[2].unQuote()
        if (start_r.isSingleChar() && end_r.isSingleChar()) {
            // convert first and last character to byte character value
            val start = start_r.single().code
            val end = end_r.single().code
            start.rangeTo(end).map { Terminal(it.toChar().toString()) }
        } else if (start_r.isInt() && end_r.isInt()) {
            start_r.toInt().rangeTo(end_r.toInt()).map { Terminal(it.toString()) }
        } else null
    } else null
}

private fun String.toRuleEdges(weight: Float, ntMap: MutableMap<String, NonTerminal>): List<RuleEdge> {
    // quoted strings can have whitespaces (print), the nonterminals must only be composed by printable non-whitespace chars (graph)
    val (rawSymbols, statement) = splitSymbolsAndStmt()

    return rawSymbols.splitSymbols().let { rawRule ->
        rawRule.tryExpand()
            // All expandable rules must consist entirely of Terminals
            ?.map { term -> RuleEdge(listOf(term)) } ?: listOf(let {
            // is not expandable, means we create on single rule out of the raw rule
            val symbols = rawRule.map { symbol ->
                // neutral statement without sideffects is pass
                if (symbol.isQuoted())
                    Terminal(symbol.unQuote())
                else{
                    val nt = NonTerminal(symbol)
                    ntMap.getOrPut(symbol){ nt }
                    nt
                }
            }
            RuleEdge(symbols, statement, weight)
        })
    }
}

private fun String.isQuoted(): Boolean {
    return if (this.startsWith("\"") && this.endsWith("\""))
        true
    else if (this.startsWith("\"") || this.endsWith("\""))
        error("$this is half quoted, please fix the grammar by adding a quote or by seperating the non-terminal and terminal by whitespace. If you intended to use a quote in a non-terminal you need to excape it as a unicode symbol.")
    else
        false
}

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