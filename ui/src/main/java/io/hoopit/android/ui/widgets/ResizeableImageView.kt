package io.hoopit.android.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ResizeableImageView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {
    // http://stackoverflow.com/questions/5554682/android-imageview-adjusting-parents-height-and-fitting-width

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val imageDrawable = drawable

        if (imageDrawable != null) {
            // ceil not round - avoid thin vertical gaps along the left/right edges
            val width = View.MeasureSpec.getSize(widthMeasureSpec)
            val height =
                Math.ceil(
                    (width.toFloat() * imageDrawable.intrinsicHeight.toFloat() /
                        imageDrawable.intrinsicWidth.toFloat())
                        .toDouble()
                )
                    .toInt()
            setMeasuredDimension(width, height)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
