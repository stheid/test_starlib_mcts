package isml.aidev.util

import kotlin.random.Random

data class Unique<T>(val value: T) {
    val uuid = Random.nextLong()
}