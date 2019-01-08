package io.hoopit.android.ui.widgets

import android.content.Context
import android.graphics.PorterDuff
import android.text.Spannable
import android.text.style.ImageSpan
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import java.util.regex.Pattern

class TextImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    override fun setText(text: CharSequence, type: TextView.BufferType) {
        val spannable = getTextWithImages(
            context,
            text,
            lineHeight,
            currentTextColor
        )
        super.setText(spannable, TextView.BufferType.SPANNABLE)
    }

    companion object {

        private const val DRAWABLE = "drawable"
        /**
         * Regex pattern that looks for embedded images of the format: [img src=imageName/]
         */
        private const val PATTERN = "\\Q[img src=\\E([a-zA-Z0-9_]+?)\\Q/]\\E"
        private val refImg: Pattern = Pattern.compile(PATTERN)

        private fun getTextWithImages(
            context: Context,
            text: CharSequence,
            lineHeight: Int,
            colour: Int
        ): Spannable {
            val spannable = Spannable.Factory.getInstance().newSpannable(text)
            addImages(context, spannable, lineHeight, colour)
            return spannable
        }

        private fun addImages(
            context: Context,
            spannable: Spannable,
            lineHeight: Int,
            colour: Int
        ): Boolean {
            var hasChanges = false

            val matcher = refImg.matcher(spannable)
            while (matcher.find()) {
                var set = true
                for (span in spannable.getSpans(
                    matcher.start(),
                    matcher.end(),
                    ImageSpan::class.java
                )) {
                    if (spannable.getSpanStart(span) >= matcher.start() &&
                        spannable.getSpanEnd(span) <= matcher.end()
                    ) {
                        spannable.removeSpan(span)
                    } else {
                        set = false
                        break
                    }
                }
                val resName = spannable.subSequence(matcher.start(1), matcher.end(1)).toString()
                    .trim { it <= ' ' }
                val id = context.resources
                    .getIdentifier(resName, DRAWABLE, context.packageName)
                if (set) {
                    hasChanges = true
                    spannable.setSpan(
                        makeImageSpan(
                            context,
                            id,
                            lineHeight,
                            colour
                        ),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return hasChanges
        }

        /**
         * Create an ImageSpan for the given icon drawable. This also sets the image size and colour.
         * Works best with a white, square icon because of the colouring and resizing.
         *
         * @param context The Android Context.
         * @param drawableResId A drawable resource Id.
         * @param size The desired size (i.e. width and height) of the image icon in pixels.
         * Use the lineHeight of the TextView to make the image inline with the
         * surrounding text.
         * @param colour The colour (careful: NOT a resource Id) to apply to the image.
         * @return An ImageSpan, aligned with the bottom of the text.
         */
        private fun makeImageSpan(
            context: Context,
            drawableResId: Int,
            size: Int,
            colour: Int
        ): ImageSpan? {
            val nativeDrawable = ContextCompat.getDrawable(context, drawableResId) ?: return null
            val drawable = DrawableCompat.wrap(nativeDrawable.mutate())
            drawable.setColorFilter(colour, PorterDuff.Mode.SRC_IN)
            drawable.setBounds(0, 0, size, size)
            return ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM)
        }
    }
}
