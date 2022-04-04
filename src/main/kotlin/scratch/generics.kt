package scratch

import isml.aidev.util.Unique

sealed class Shape() {
    class Circ : Shape()
    class Rect : Shape()
}

fun main() {
    val list: MutableList<Shape> = mutableListOf()
    val circs = arrayListOf(Shape.Circ(), Shape.Circ(), Shape.Circ())
    val rects = arrayListOf(Shape.Rect())
    val mixed = arrayListOf(Shape.Rect(), Shape.Circ())

    list.add(circs.removeFirst())
    list.add(circs.removeFirst())
    list.add(circs.removeFirst())
    list.add(rects.removeFirst())
}

fun main2() {
    val list: MutableList<Unique<out Shape>> = mutableListOf()
    val circs = arrayListOf(Unique(Shape.Circ()), Unique(Shape.Circ()), Unique(Shape.Circ()))
    val rects = arrayListOf(Unique(Shape.Rect()))
    val mixed = arrayListOf(Unique(Shape.Rect()), Unique(Shape.Circ()))

    list.add(circs.removeFirst())
    list.add(circs.removeFirst())
    list.add(circs.removeFirst())
    val filtered = mixed.filterIsInstance<Unique<Shape.Circ>>().toCollection(ArrayList())
    list.add(filtered.removeFirst())
}

fun main3() {
    val list: MutableList<Unique<out Unique<out Shape>>> = mutableListOf()
    val circs = arrayListOf(Unique(Unique(Shape.Circ())))
    val rects = arrayListOf(Unique(Unique(Shape.Rect())))
    val mixed = arrayListOf(Unique(Unique(Shape.Rect())), Unique(Unique(Shape.Circ())))

    list.add(circs.removeFirst())
    val filtered = mixed.filterIsInstance<Unique<Unique<Shape.Circ>>>().toCollection(ArrayList())
    list.add(filtered.removeFirst())
}