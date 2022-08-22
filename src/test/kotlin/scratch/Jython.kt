package scratch

import isml.aidev.util.Evaluator

fun main() {
    // initialization
    val evaluator = Evaluator()

    println(evaluator.eval("x<5", mapOf("x" to 10)))
    println(evaluator.exec("y+=1"))
}
