package io.hoopit.android.common.extensions

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

fun Activity?.hideSoftInput() {
    if (this == null) return
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
}

fun Context.getStringOrDefault(
    @StringRes stringRes: Int,
    default: String?,
    vararg vars: String
): String? {
    @Suppress("SwallowedException")
    return try {
        getString(stringRes, *vars)
    } catch (e: Resources.NotFoundException) {
        default
    }
}

fun Context.getStringOrDefault(
    @StringRes stringRes: Int,
    default: String?
): String? {
    @Suppress("SwallowedException")
    return try {
        getString(stringRes)
    } catch (e: Resources.NotFoundException) {
        default
    }
}

fun Context.getExternalCacheUri(): Uri {
    return FileProvider.getUriForFile(
        this,
        applicationContext.packageName + ".provider",
        File(externalCacheDir, UUID.randomUUID().toString() + ".jpg")
    )
}

// https://learnpainless.com/android/material/make-fully-android-transparent-status-bar
fun Activity.enableTransparentStatusBar() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        window.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        window.setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT
    }
}

fun Window.setWindowFlag(bits: Int, on: Boolean) {
    val winParams = attributes
    if (on) {
        winParams.flags = winParams.flags or bits
    } else {
        winParams.flags = winParams.flags and bits.inv()
    }
    attributes = winParams
}
