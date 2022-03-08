package isml.aidev.util

class ChainLink<T>(val value: T)  {
    var pred: ChainLink<T>? = null
        set(value) {
            field = value
            value?.succ = this
        }
    var succ: ChainLink<T>? = null
        set(value) {
            field = value
            value?.pred = this
        }

    /**
     * substitute "this" with a chain of links
     *
     * will basically clear all references to "this" and nit in references to the start and end of chain.
     * @return return the provided chain object embedded in the larger chain
     */
    fun substitute(chain: Chain<T>) {
        val chainHead = chain.first
        val chainTail = chain.last
 //       chain.forEach { it.first = first }
        pred?.succ = chainHead
        succ?.pred = chainTail

    }
/*
    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var pointer = first

            override fun hasNext(): Boolean {
                return pointer.succ != null
            }

            override fun next(): T {
                pointer = pointer.succ!!
                return pointer
            }
        }
    }
 */
}