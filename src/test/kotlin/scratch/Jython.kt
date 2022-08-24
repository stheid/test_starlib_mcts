package scratch

import isml.aidev.util.Evaluator

fun main() {
    // initialization
    val evaluator = Evaluator.instance()

    println(evaluator.eval("x<5", mapOf("x" to 10)))
    println(evaluator.eval("1==1", mapOf("x" to 10)))
    println(evaluator.exec("del i", mapOf("i" to 10)))
    println(evaluator.exec("y+=1"))
}
