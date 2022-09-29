package isml.aidev.grammar

import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.Attribute
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.dot.DOTExporter
import java.io.File

fun createExporter(): DOTExporter<Node, DefaultEdge> {
    val exporter = DOTExporter<Node, DefaultEdge> {
        when (it) {
            is ConditionalNode -> """"${it.cond + " " + it.hashCode().toString().substring(0..4)}""""

            else -> """"${
                if (it.nodes == null) it.hashCode() else
                    it.nodes.toString().filter { it.isLetterOrDigit() || it == ' ' }
            }""""
        }
    }

    exporter.setVertexAttributeProvider {
        mutableMapOf<String, Attribute>(
            "color" to DefaultAttribute.createAttribute(
                when (it) {
                    is ComplexNode -> "red"
                    is ConditionalNode -> "blue"
                    else -> "black"
                }
            )
        )
    }
    exporter.setEdgeAttributeProvider {
        mutableMapOf<String, Attribute>(
            "color" to DefaultAttribute.createAttribute(
                when (it) {
                    is ComplexEdge -> "red"
                    is CondEdge -> "blue"
                    else -> "black"
                }
            ),
            "label" to DefaultAttribute.createAttribute(
                when (it) {
                    is RuleEdgeSimplify -> it.statement ?: ""
                    else -> ""
                }
            )
        )
    }
    return exporter
}

fun main() {
    val grammar = Grammar.fromResource("simple_annotated_globvar.yaml", false)
//    val grammar = Grammar.fromResource("extremely_simple_gram.yml")

    println(grammar)
    var graph = grammar.prodRules.toGraph()

    createExporter().exportGraph(graph, File("grammar_raw.dot").bufferedWriter())
    graph = graph.simplify()
    createExporter().exportGraph(graph, File("grammar_simple.dot").bufferedWriter())

    val rules = graph.toProdRules()
    println(rules)
}
