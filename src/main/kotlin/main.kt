fun main(args: Array<String>) {
    val algo = Algorithm(2)
    algo.createInput().decodeToString()
    algo.observe(listOf(1, 0, 0, 0).map { it.toByte() }.toByteArray())
    algo.createInput().decodeToString()
    algo.observe(listOf(1, 2, 3, 4).map { it.toByte() }.toByteArray())
    algo.join()
}

