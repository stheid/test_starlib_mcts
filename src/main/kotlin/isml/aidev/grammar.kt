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
    open class NonTerminal(val value: String) : Symbol() {
        override fun toString(): String {
            return "nt: ${value}"
        }

        override fun equals(other: Any?): Boolean {
            other as NonTerminal
            if (this === other) return true

            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    class UniqueNT(value: String) : NonTerminal(value) {
        constructor(nt: NonTerminal) : this(nt.value)

        val uuid = Random.nextLong()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true

            // if type mixed, we call "NonTerminal" equal method for comparison
            if (javaClass != other?.javaClass)
                return super.equals(other)

            other as UniqueNT

            if (value != other.value) return false
            if (uuid != other.uuid) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + uuid.hashCode()
            return result
        }
    }
}


@Serializable
data class Grammar(
    val startSymbol: Symbol.NonTerminal,
    private val prodRules_: Map<String, List<RuleEdge>>,
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


