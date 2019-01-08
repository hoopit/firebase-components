package io.ezet.android.common.extensions

import android.content.Context
import android.view.Menu
import androidx.annotation.ColorRes
import androidx.core.view.children

fun Menu.tintIcons(context: Context, @ColorRes id: Int) {
    this.children.forEach {
        it.icon?.tint(context, id)
    }
}
