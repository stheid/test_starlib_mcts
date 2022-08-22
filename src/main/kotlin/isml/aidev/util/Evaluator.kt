package isml.aidev.util

import org.python.core.PyBoolean
import org.python.core.PyDictionary
import org.python.core.PyInteger
import org.python.core.PyString
import org.python.modules._collections.PyDefaultDict
import org.python.util.PythonInterpreter


class Evaluator private constructor() {
    companion object {
        fun instance() = Evaluator()
    }

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

    fun eval(expr: String, vars: Map<String, Int> = mapOf()): Boolean {
        // evaluating expressions
        val code = "res = eval(\"${expr}\",{},loc)"
        interp.set("loc", vars.toDefaultDict())
        interp.exec(code)
        return (interp["res"] as? PyBoolean)?.booleanValue
            ?: error("the result of $code could not be parsed as a boolean. " +
                    "Did you use correct python Syntax (e.g. Boolean literals must be capital like \"True\")")
    }

    fun exec(stmt: String, vars: Map<String, Int> = mapOf()): Map<String, Int> {
        // executing statements
        interp.set("loc", vars.toDefaultDict())
        interp.exec("exec (\"${stmt}\",{},loc)")
        val outVars = interp["loc"]
        return (outVars as PyDictionary).toList().associate { it.first as String to it.second as Int }
    }
}