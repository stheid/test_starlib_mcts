package isml.aidev

class HashedArrayList<E> : ArrayList<E>() {
    override fun indexOf(element: E): Int {
        return 0
    }

    operator fun plus(list: HashedArrayList<E>) {

    }
}