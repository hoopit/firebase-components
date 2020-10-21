package io.hoopit.android.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.hoopit.android.ui.R

class EmptyStateImage @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        val view = ConstraintLayout.inflate(
            context, R.layout.empty_state_image__default, this
        )
        val typedArray = context.obtainStyledAttributes(
            attrs,
            R.styleable.EmptyStateImage
        )
        val imageView: ImageView = view.findViewById(R.id.iv_empty_image)
        val titleView: TextView = view.findViewById(R.id.tv_empty_title)
        @DrawableRes val drawableRes =
            typedArray.getResourceId(R.styleable.EmptyStateImage_image, 0)
        if (drawableRes != 0) imageView.setImageResource(drawableRes)
        titleView.text = typedArray.getText(R.styleable.EmptyStateImage_title)
        setText(typedArray.getText(R.styleable.EmptyStateImage_text))
        typedArray.recycle()
    }

    fun setTitle(title: CharSequence?) {
        if (title == null) return
        val titleView: TextView = findViewById(R.id.tv_empty_title)
        titleView.text = title
    }

    fun setText(text: CharSequence?) {
        if (text == null) return
        val textView: TextView = findViewById(R.id.tv_empty_text)
        textView.text = text
    }
}
