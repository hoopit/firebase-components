package io.hoopit.android.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.graphics.drawable.DrawableCompat
import io.hoopit.android.ui.R

class LinearLayoutCompatTint @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {

        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.LinearLayoutCompatTint,
            defStyleAttr,
            0
        )

        if (typedArray.hasValue(R.styleable.LinearLayoutCompatTint_backgroundTint)) {
            val color = typedArray.getColor(R.styleable.LinearLayoutCompatTint_backgroundTint, 0)

            val bgDrawable = background
            if (bgDrawable != null) {
                DrawableCompat.setTint(DrawableCompat.wrap(bgDrawable).mutate(), color)
            }
        }
        typedArray.recycle()
    }
}
