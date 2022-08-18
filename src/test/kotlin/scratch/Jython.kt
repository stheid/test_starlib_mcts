package scratch


import org.python.core.PyBoolean
import org.python.core.PyDictionary
import org.python.util.PythonInterpreter


fun main() {
    // initialization
    val interp = PythonInterpreter()
    interp.exec("from collections import defaultdict")
    interp.exec("loc = defaultdict(int)")

    // evaluating expressions
    interp.exec("res = eval(\"x<4\",globals(),loc)")
    println((interp["res"] as PyBoolean).booleanValue)

    // executing statements
    interp.exec("exec (\"y+=1\",globals(),loc)")
    val vars = interp["loc"]
    println((vars as PyDictionary).toKt())
}

private fun PyDictionary.toKt(): Map<String, Int> {
    return this.toList().associate { it.first as String to it.second as Int }
}
