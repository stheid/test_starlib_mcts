package isml.aidev.util

import org.python.core.PyBoolean
import org.python.core.PyDictionary
import org.python.core.PyInteger
import org.python.core.PyString
import org.python.modules._collections.PyDefaultDict
import org.python.util.PythonInterpreter


class Evaluator {
    private val interp = PythonInterpreter().apply {
        exec("from collections import defaultdict")
    }

    private fun Map<String, Int>.toDefaultDict(): PyDefaultDict {
        return PyDefaultDict(
            null,
            this.entries.associate { PyString(it.key) to PyInteger(it.value) }).apply {
            defaultFactory = PyInteger.TYPE
        }
    }

    public fun eval(expr: String, vars: Map<String, Int> = mapOf()): Boolean {
        // evaluating expressions
        interp.set("loc", vars.toDefaultDict())
        interp.exec("res = eval(\"${expr}\",{},loc)")
        return (interp["res"] as PyBoolean).booleanValue
    }

    public fun exec(stmt: String, vars: Map<String, Int> = mapOf()): Map<String, Int> {
        // executing statements
        interp.set("loc", vars.toDefaultDict())
        interp.exec("exec (\"${stmt}\",{},loc)")
        val outVars = interp["loc"]
        return (outVars as PyDictionary).toList().associate { it.first as String to it.second as Int }
    }
}