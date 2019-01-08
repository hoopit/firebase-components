package io.ezet.android.common.extensions

/**
 * Returns the sum of all values produced by [selector] function applied to each element in the collection.
 */
inline fun <T> Iterable<T>.sumBy(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun <E> Collection<E>?.isNullOrEmpty(): Boolean {
    return this == null || this.isEmpty()
}

fun <E> Collection<E>?.isNotNullOrEmpty(): Boolean {
    return !(this == null || this.isEmpty())
}
