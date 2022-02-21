package isml.aidev

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import kotlin.random.Random

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
        val uuid = Random.nextLong()

        override fun toString(): String {
            return "nt: ${value}"
        }

        override fun equals(other: Any?): Boolean {
            if (other is NonTerminal)
                return value == other.value && uuid == other.uuid
            return false
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + uuid.hashCode()
            return result
        }
    }

    data class UniqueNT(val nonTerminal: NonTerminal):Symbol(){
        val uuid = Random.nextLong()

        override fun toString(): String {
            return nonTerminal.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (other is UniqueNT)
                return nonTerminal==other.nonTerminal && uuid == other.uuid
            return super.equals(other)
        }

        override fun hashCode(): Int {
            var result = nonTerminal.hashCode()
            result = 31 * result + uuid.hashCode()
            return result
        }
    }
}



@Serializable
data class Grammar(
    val startSymbol: Symbol.NonTerminal,
    private val prodRules_: Map<String, List<RuleEdge>>
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

typealias ProdRules = Map<Symbol.NonTerminal, List<RuleEdge>>


