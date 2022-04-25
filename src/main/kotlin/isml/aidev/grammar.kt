package isml.aidev

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
sealed class Symbol {
    @SerialName("terminal")
    @Serializable
    data class Terminal(val value: String) : Symbol() {
        override fun toString(): String =
            "term: $value"
    }

    @SerialName("nonterminal")
    @Serializable
    /**
     * Must not be a data class as we need to uniquely identify non-terminals by their object reference.
     * For a non-unique comparison we can use the Non-Terminals internal string.
     */
    class NonTerminal(val value: String) : Symbol() {
        override fun toString(): String =
            "nt: $value"

        fun copy(): NonTerminal =
            NonTerminal(value)
    }
}

typealias ProdRules = Map<String, List<RuleEdge>>

@Serializable
data class Grammar(
    val startSymbol: Symbol.NonTerminal,
    val prodRules_: ProdRules,
) {
    companion object {
        fun fromFile(path: String): Grammar =
            Yaml(configuration = YamlConfiguration(polymorphismStyle = PolymorphismStyle.Property))
                .decodeFromString(serializer(), File(path).bufferedReader().readText())
    }
}