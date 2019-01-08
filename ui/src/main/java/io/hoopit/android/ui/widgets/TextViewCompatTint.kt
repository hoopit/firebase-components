package io.hoopit.android.ui.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.drawable.DrawableCompat
import io.hoopit.android.ui.R

class TextViewCompatTint @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.TextViewCompatTint,
        defStyleAttr,
        0
    )

    override fun setCompoundDrawablesRelative(
        start: Drawable?,
        top: Drawable?,
        end: Drawable?,
        bottom: Drawable?
    ) {
        super.setCompoundDrawablesRelative(start, top, end, bottom)
        for (drawable in compoundDrawablesRelative) {
            if (drawable == null) continue
            if (typedArray.hasValue(R.styleable.TextViewCompatTint_drawableTint)) {
                val color = typedArray.getColor(R.styleable.TextViewCompatTint_drawableTint, 0)
                val wrap = DrawableCompat.wrap(drawable.mutate())
                DrawableCompat.setTint(wrap, color)
                if (typedArray.hasValue(R.styleable.TextViewCompatTint_drawableIconSize)) {
                    val dimens = typedArray.getDimension(
                        R.styleable.TextViewCompatTint_drawableIconSize,
                        0f
                    )
                    val inset =
                        Math.round((resources.displayMetrics.density * DEFAULT_SIZE - dimens) / DIVIDER)
                    val size = Math.round(dimens)
                    drawable.setBounds(0 + inset, 0 + inset, size + inset, size + inset)
                }
            }
        }
        typedArray.recycle()
    }

    companion object {
        private const val DEFAULT_SIZE = 24f
        private const val DIVIDER = 4
    }
}
