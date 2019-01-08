package io.hoopit.android.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.widget.TextViewCompat
import com.google.android.material.textfield.TextInputLayout
import io.hoopit.android.ui.R

class CustomTextInputLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextInputLayout(context, attrs, defStyleAttr) {

    private var typedArray = context.obtainStyledAttributes(
        attrs,
        R.styleable.CustomTextInputLayout,
        defStyleAttr,
        0
    )
    private var currentTint: Int? = null

    init {
        addOptionalTextView()
    }

    private val textWatcher = TintTextWatcher()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        editText?.addTextChangedListener(textWatcher)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        editText?.removeTextChangedListener(textWatcher)
    }

    override fun onDraw(canvas: Canvas?) {
        if (currentTint == null) {
            editText?.compoundDrawablesRelative?.forEach { drawable ->
                if (drawable == null) return@forEach
                val wrap = DrawableCompat.wrap(drawable.mutate())
                currentTint = typedArray.getColor(R.styleable.CustomTextInputLayout_defaultTint, 0)
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
                typedArray.getColor(R.styleable.CustomTextInputLayout_defaultTint, 0)
            } else {
                typedArray.getColor(R.styleable.CustomTextInputLayout_imageTint, 0)
            }
            // Avoid tinting with the same color repeatedly
            if (color == currentTint) return
            editText?.compoundDrawablesRelative?.forEach { drawable ->
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

    private fun addOptionalTextView() {
        if (typedArray.getBoolean(R.styleable.CustomTextInputLayout_showOptional, false)) {
            val tvOptional = TextView(context)
            tvOptional.text = resources.getString(R.string.CustomTextInputLayout_optional)
            val params =
                LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            params.gravity = GravityCompat.END
            TextViewCompat.setTextAppearance(tvOptional, R.style.TextAppearance_MaterialComponents_Caption)
            this.addView(tvOptional, params)
        }
    }
}
