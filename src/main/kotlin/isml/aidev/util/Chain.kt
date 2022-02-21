package isml.aidev.util

class Chain<T>() : Sequence<T> {
    var first: ChainLink<T> = pred?.first ?: pred ?: this
    var last: ChainLink<T> = succ?.last ?: succ ?: this

    companion object{
        fun fromSequence(){

    }

    override fun iterator(): Iterator<T> {
        TODO("Not yet implemented")
    }

    override fun toString(): String {
        return super.toString()
    }
}