package isml.aidev.util

class Chain<T>(sequence: Sequence<T>) : Sequence<T> {
    var first: ChainLink<T>
    var last: ChainLink<T>

    init {
        // create a link for each individual and then link them
        sequence
            .map { ChainLink(it) }
            .apply {
                first = this.first()
                last = this.last()
            }
            .zipWithNext()
            .forEach { (a, b) ->
                a.succ = b
                b.pred = a
            }
    }

    override fun iterator(): Iterator<T> {
        TODO("Not    yet implemented")
    }

    override fun toString(): String {
        return super.toString()
    }
}