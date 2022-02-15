package isml.aidev

import kotlinx.serialization.Serializable

@Serializable
data class RuleEdge(val substitution: List<Symbol>, val weight: Double, val is_extending: Boolean) {
    override fun toString(): String {
        return " -> $substitution"
    }
}