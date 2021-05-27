import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import isml.aidev.Algorithm

class CLI : CliktCommand() {
    val maxIterations: Int by option(help = "Number of MCTS runs").int().default(2)
    val grammar: String by option(help = "Path to the grammar").default("grammar.yml")

    override fun run() {
        val algo = Algorithm(maxIterations, grammar)
        repeat(maxIterations) {
            algo.createInput().decodeToString()
            algo.observe(listOf(1, 0, 0, 0).map { it.toByte() }.toByteArray())
        }
        algo.join()
    }
}

fun main(args: Array<String>) = CLI().main(args)