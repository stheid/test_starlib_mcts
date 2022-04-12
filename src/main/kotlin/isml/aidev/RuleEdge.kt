package isml.aidev

import kotlinx.serialization.Serializable

@Serializable
data class RuleEdge(val substitution: List<Symbol>, val is_extending: Boolean, val weight: Double = 1.0) {
    override fun toString(): String {
        return " -> $substitution"
    }
}