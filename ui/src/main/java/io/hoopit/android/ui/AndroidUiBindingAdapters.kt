//@file:Suppress("unused")

package io.hoopit.android.ui

import android.content.res.ColorStateList
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.widget.ImageViewCompat
import androidx.databinding.BindingAdapter
import androidx.databinding.BindingMethod
import androidx.databinding.BindingMethods
import com.google.android.material.textfield.TextInputLayout
import io.hoopit.android.ui.widgets.EmptyStateImage
import io.hoopit.android.ui.widgets.ResetErrorTextWatcher

@BindingMethods(
    BindingMethod(
        type = View::class,
        attribute = "backgroundTint",
        method = "setBackgroundTintList"
    ),
    // Enables srcCompat for data binding
    BindingMethod(
        type = ImageView::class,
        attribute = "srcCompat",
        method = "setImageDrawable"
    ),
    BindingMethod(
        type = ImageView::class,
        attribute = "android:tint",
        method = "imageTint"
    ),
    BindingMethod(
        type = EmptyStateImage::class,
        attribute = "text",
        method = "setText"
    )
)
class AndroidUiBindingAdapters

///**
// * Compat version of tooltip[]
// */
//@BindingAdapter("app:tooltipText")
//fun View.setTooltip(tooltipText: String) {
//    TooltipCompat.setTooltipText(this, tooltipText)
//}

@set:BindingAdapter("app:visibleGone")
var View.visibleOrGone
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.GONE
    }

@BindingAdapter("app:goneVisible")
fun View.goneVisible(value: Boolean?) {
    value?.let {
        visibility = if (it) View.GONE else View.VISIBLE
    }
}

@set:BindingAdapter("app:visibleHidden")
var View.visibleHidden
    get() = visibility == View.VISIBLE
    set(value) {
        visibility = if (value) View.VISIBLE else View.INVISIBLE
    }

/**
 * Set the error of a the TIL to a string resource
 */
@BindingAdapter("app:errorText")
fun TextInputLayout.setErrorMessage(@StringRes errorMessage: Int?) {
    error = if (errorMessage == null) {
        null
    } else {
        editText?.addTextChangedListener(ResetErrorTextWatcher(this))
        requestFocus()
        context.resources.getString(errorMessage)
    }
}

@BindingAdapter("app:tint")
fun ImageView.imageTint(@ColorInt colorRes: Int) {
    ImageViewCompat.setImageTintList(
        this,
        ColorStateList.valueOf(colorRes)
    )
}

/**
 * Used to avoid the safeUnbox warnings
 */
@BindingAdapter("android:checked")
fun CompoundButton.setChecked(isChecked: Boolean?) {
    this.isChecked = isChecked ?: false
}

