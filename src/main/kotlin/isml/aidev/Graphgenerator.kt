package isml.aidev

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.api4.java.ai.graphsearch.problem.IPathSearchInput
import org.api4.java.ai.graphsearch.problem.implicit.graphgenerator.IPathGoalTester
import org.api4.java.datastructure.graph.implicit.IGraphGenerator
import org.api4.java.datastructure.graph.implicit.INewNodeDescription
import org.api4.java.datastructure.graph.implicit.ISingleRootGenerator
import org.api4.java.datastructure.graph.implicit.ISuccessorGenerator
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Serializable
sealed class Symbol {
    @SerialName("terminal")
    @Serializable
    data class Terminal(val value: String) : Symbol() {
        override fun toString(): String {
            return "term: ${value}"
        }
    }

    @SerialName("nonterminal")
    @Serializable
    data class NonTerminal(val value: String) : Symbol() {
        override fun toString(): String {
            return "nt: ${value}"
        }
    }

}

@Serializable
data class Grammar(
    val startSymbol: Symbol.NonTerminal,
    private val prodRules_: Map<String, List<Rule>>
) {
    companion object {
        fun fromFile(path: String): Grammar {
            return Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property)).decodeFromString(
                serializer(), File(path).bufferedReader().readText()
            )
        }
    }

    val prodRules
        get() = prodRules_.map { Symbol.NonTerminal(it.key) to it.value }.associate { it }
}

typealias ProdRules = Map<Symbol.NonTerminal, List<Rule>>


// edge type
// right hand side of a production rule
@Serializable
data class Rule(val substitution: List<Symbol>, val weight: Double, val is_extending: Boolean) {
    override fun toString(): String {
        return " -> $substitution"
    }
}


class PCFGGraphGenerator(private val grammar: Grammar) : IGraphGenerator<Symbols, Rule> {
    private val successorGenerator = PCFGSuccGen(grammar.prodRules)

    override fun getRootGenerator(): ISingleRootGenerator<Symbols> {
        return ISingleRootGenerator<Symbols> { Symbols(arrayListOf(grammar.startSymbol)) }
    }

    override fun getSuccessorGenerator(): ISuccessorGenerator<Symbols, Rule> {
        return this.successorGenerator
    }
}


data class Symbols(
    val NTs: ArrayList<Symbol.NonTerminal>,
    val root: Symbols? = null,
    val depth: Int = 0
) {
    val currNT = NTs.let {
        val idx = (0 until it.size).randomOrNull()
        if (idx != null)
            return@let NTs.removeAt(idx)
        return@let null
    }

    var succ: Symbols? = null
    var toRule: Rule? = null
    val isFinished: Boolean
        get() = currNT == null

    fun createChild(rule: Rule): Symbols {
        val child = Symbols(
            (NTs + rule.substitution.filterIsInstance(Symbol.NonTerminal::class.java)) as ArrayList<Symbol.NonTerminal>,
            root ?: this,
            depth + 1
        )
        toRule = rule
        succ = child
        return child
    }

    fun toWord(): String {
        if (!isFinished)
            return ""

        var node = root ?: this
        var symbols = arrayListOf<Symbol>(node.currNT!!)
        while (node.succ != null) {
            // todo get the right index and not any index!!
            symbols = (symbols.take(symbols.indexOf(node.currNT!!))
                    + node.toRule!!.substitution
                    + symbols.drop(symbols.indexOf(node.currNT!!) + 1)) as ArrayList<Symbol>
            node = node.succ!!
        }

        // todo there are somehow a lot of nonterminals left. is it again because of the epsilon rules?
        return symbols.filterIsInstance(Symbol.Terminal::class.java).joinToString(separator = "") { it.value }
    }
}

class PCFGSuccGen(private val prodRules: ProdRules) : ISuccessorGenerator<Symbols, Rule> {
    val nodes = ConcurrentHashMap<Symbols, Boolean>()
    val adiacenceList = ConcurrentHashMap<Symbols, List<INewNodeDescription<Symbols, Rule>>>()


    override fun generateSuccessors(node: Symbols): List<INewNodeDescription<Symbols, Rule>> {
        if (adiacenceList.containsKey(node)) {
            // if expansion known. return childs
            return adiacenceList.get(node)!!
        }

        // else create new childs and filter for existing ones
        val children = node.currNT?.let { nt ->
            prodRules[node.currNT]!!
                .map { node.createChild(it) to it }
                .filter { (child, _) ->
                    val tmp = nodes.putIfAbsent(child, true) == null
                    return@filter tmp
                }
                .map { (child, rule) ->
                    object : INewNodeDescription<Symbols, Rule> {
                        override fun getFrom(): Symbols {
                            return node
                        }

                        override fun getTo(): Symbols {
                            return child
                        }

                        override fun getArcLabel(): Rule {
                            return rule
                        }
                    }
                }
        } ?: emptyList()

        // todo: handle what happens if node has no children, but is not a leaf
        // -> can happen because of filtering
        adiacenceList[node] = children
        return children
    }
}


class PCFGSearchInput(private val grammar: Grammar) : IPathSearchInput<Symbols, Rule> {
    override fun getGraphGenerator(): IGraphGenerator<Symbols, Rule> {
        return PCFGGraphGenerator(grammar)
    }

    override fun getGoalTester(): IPathGoalTester<Symbols, Rule> {
        return IPathGoalTester<Symbols, Rule> { path -> path.head.isFinished }
    }
}