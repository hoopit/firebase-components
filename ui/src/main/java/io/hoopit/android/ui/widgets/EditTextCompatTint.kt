package io.hoopit.android.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.drawable.DrawableCompat
import io.hoopit.android.ui.R

class EditTextCompatTint @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.EditTextCompatTint,
        defStyleAttr,
        0
    )

    private var currentTint: Int? = null

    private val textWatcher = TintTextWatcher()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        addTextChangedListener(textWatcher)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeTextChangedListener(textWatcher)
    }

    override fun onDraw(canvas: Canvas?) {
        if (currentTint == null) {
            compoundDrawablesRelative.forEach { drawable ->
                if (drawable == null) return@forEach
                val wrap = DrawableCompat.wrap(drawable.mutate())
                currentTint = typedArray.getColor(R.styleable.EditTextCompatTint_defaultIconTint, 0)
                currentTint?.let { DrawableCompat.setTint(wrap, it) }
            }
        }
        super.onDraw(canvas)
    }

    inner class TintTextWatcher : android.text.TextWatcher {

        override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
        }

        override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            val color = if (charSequence.isBlank()) {
                typedArray.getColor(R.styleable.EditTextCompatTint_defaultIconTint, 0)
            } else {
                typedArray.getColor(R.styleable.EditTextCompatTint_iconTint, 0)
            }
            // Avoid tinting with the same color repeatedly
            if (color == currentTint) return
            compoundDrawablesRelative.forEach { drawable ->
                if (drawable == null) return@forEach
                // Only mutate if it hasn't been mutated before
                val wrap = if (currentTint == null) {
                    DrawableCompat.wrap(drawable.mutate())
                } else {
                    DrawableCompat.wrap(drawable)
                }
                DrawableCompat.setTint(wrap, color)
            }
            currentTint = color
        }

        override fun afterTextChanged(editable: android.text.Editable) {
        }
    }
}
