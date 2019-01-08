package io.ezet.android.common.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat

fun Drawable.tint(context: Context, @ColorRes id: Int, mutate: Boolean = true) {
    val drawable = if (mutate) mutate() else this
    DrawableCompat.setTint(drawable, ContextCompat.getColor(context, id))
}
