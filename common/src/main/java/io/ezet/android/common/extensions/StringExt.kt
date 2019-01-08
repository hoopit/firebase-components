package io.ezet.android.common.extensions

fun String?.nullIfBlank(): String? {
    return if (isNullOrBlank()) null else this
}
