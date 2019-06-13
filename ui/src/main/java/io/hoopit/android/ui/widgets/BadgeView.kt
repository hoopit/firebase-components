package io.hoopit.android.ui.widgets

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RoundRectShape
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewPropertyAnimator
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TabWidget
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * A simple text label view that can be applied as a "badge" to any given [View].
 * This class is intended to be instantiated at runtime rather than included in XML layouts.
 *
 */
@Suppress("MemberVisibilityCanBePrivate")
class BadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyle) {

    private var badgePosition: Int = 0
    /**
     * Returns the horizontal margin from the target View that is applied to this badge.
     *
     */
    var horizontalBadgeMargin: Int = 0
        set(value) {
            field = dipToPixels(value)
        }
    /**
     * Returns the vertical margin from the target View that is applied to this badge.
     *
     */
    var verticalBadgeMargin: Int = 0
        set(value) {
            field = dipToPixels(value)
        }

    private var badgeBg: ShapeDrawable? = null

    var showAnimator = fun View.(): ViewPropertyAnimator {
        return animate()
            .alpha(1f)
            .scaleY(1f)
            .scaleX(1f)
            .setDuration(DURATION)
            .setInterpolator(OvershootInterpolator(OVERSHOOT_TENSION))
            .withStartAction { visibility = View.VISIBLE }
    }

    var hideAnimator = fun View.(): ViewPropertyAnimator {
        return animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(DURATION)
            .setInterpolator(AnticipateInterpolator())
            .withEndAction { visibility = View.GONE }
    }

    var upperBound = DEFAULT_UPPER_BOUND

    var limitSuffix = DEFAULT_UPPER_BOUND_SUFFIX

    var visibilityBound = 0

    @ColorInt
    fun getAccent(): Int {
        val colorAttr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            android.R.attr.colorAccent
        } else {
            resources.getIdentifier("colorAccent", "attr", context.packageName)
        }
        val value = TypedValue()
        context.theme?.resolveAttribute(colorAttr, value, true)
        return value.data
    }

    private val defaultBackground: ShapeDrawable
        get() {
            val pixel = dipToPixels(DEFAULT_CORNER_RADIUS_DIP)
            val outerR = floatArrayOf(
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat(),
                pixel.toFloat()
            )

            val rr = RoundRectShape(outerR, null, null)
            val drawable = ShapeDrawable(rr)
            drawable.paint.color = getAccent()
            return drawable
        }

    /**
     * Returns the color value of the badge background.
     *
     */

    /**
     * Constructor -
     *
     * create a new BadgeView instance attached to a target [View].
     *
     * @param context context for this view.
     * @param target the View to attach the badge to.
     */
    @Suppress("unused")
    constructor(context: Context, target: View) : this(context, null, android.R.attr.textViewStyle) {
        val parentGroup = target.parent as? ViewGroup ?: return
        val container = FrameLayout(context)
        val index = parentGroup.indexOfChild(target)
        this.visibility = View.GONE
        container.addView(target)
        container.addView(this)
        parentGroup.removeView(target)
        parentGroup.addView(container, index, target.layoutParams)
        parentGroup.invalidate()
    }

    /**
     * Constructor -
     *
     * create a new BadgeView instance attached to a target [TabWidget]
     * tab at a given index.
     *
     * @param context context for this view.
     * @param target the TabWidget to attach the badge to.
     * @param index the position of the tab within the target.
     */
    @Suppress("unused")
    constructor(context: Context, target: TabWidget, index: Int) : this(
        context,
        null,
        android.R.attr.textViewStyle
    ) {
        val tab = target.getChildTabViewAt(index)
        this.visibility = View.GONE
        val container = FrameLayout(context)
        container.addView(this)
        (tab as? ViewGroup)?.addView(
            container,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
    }

    constructor(context: Context, bottomNavigationView: BottomNavigationView, @IdRes viewId: Int) : this(
        context,
        null,
        android.R.attr.textViewStyle
    ) {
        val menuView = requireNotNull(bottomNavigationView.getChildAt(0) as? BottomNavigationMenuView)
        val itemView = menuView.findViewById<BottomNavigationItemView>(viewId)
        itemView.addView(this)
        horizontalBadgeMargin = DEFAULT_BOTTOM_NAVIGATION_MARGIN
        verticalBadgeMargin = DEFAULT_BOTTOM_NAVIGATION_MARGIN
        badgePosition = POSITION_BOTTOM_NAVIGATION
    }

    init {
        visibility = View.GONE
        hideAnimator.invoke(this).setDuration(0).start()

        typeface = Typeface.DEFAULT_BOLD
        val paddingPixels = dipToPixels(DEFAULT_LR_PADDING_DIP)
        setPadding(paddingPixels, 0, paddingPixels, 0)
        setTextColor(DEFAULT_TEXT_COLOR)

        applyLayoutParams()
    }

    fun show(animate: Boolean = true) {
        applyLayoutParams()
        if (isShown) return
        if (background == null) {
            if (badgeBg == null) {
                badgeBg = defaultBackground
            }
            background = badgeBg
        }
        if (animate)
            showAnimator.invoke(this)
        else
            visibility = View.VISIBLE
    }

    fun hide(animate: Boolean = true) {
        if (visibility == View.GONE) return
        if (animate)
            hideAnimator.invoke(this)
        else
            visibility = View.GONE
    }

    fun setNumber(value: Int?) {
        if (value == null || value <= visibilityBound) {
            hide(true)
            return
        }
        val text = if (value > upperBound) "$upperBound$limitSuffix" else value.toString()
        horizontalBadgeMargin = setHorizontalBadgeMargin(value)
        showText(text, true)
    }

    fun showText(label: CharSequence, animate: Boolean = true) {
        if (label == text && isShown) return
        applyLayoutParams()
        text = label
        if (isShown && animate) {
            animate()
                .scaleX(DEFAULT_UPDATE_SCALE)
                .scaleY(DEFAULT_UPDATE_SCALE)
                .withEndAction {
                    animate().scaleX(1f).scaleY(1f)
                        .start()
                }
                .start()
        } else {
            show(animate)
        }
    }

    fun toggle(
        animateShow: Boolean = true,
        animateHide: Boolean = true
    ) {
        if (isShown) {
            hide(animateHide)
        } else {
            show(animateShow)
        }
    }

    private fun applyLayoutParams() {
        val lp = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        when (badgePosition) {
            POSITION_TOP_LEFT -> {
                lp.gravity = Gravity.START or Gravity.TOP
                lp.setMargins(horizontalBadgeMargin, verticalBadgeMargin, 0, 0)
            }
            POSITION_TOP_RIGHT -> {
                lp.gravity = Gravity.END or Gravity.TOP
                lp.setMargins(0, verticalBadgeMargin, horizontalBadgeMargin, 0)
            }
            POSITION_BOTTOM_LEFT -> {
                lp.gravity = Gravity.START or Gravity.BOTTOM
                lp.setMargins(horizontalBadgeMargin, 0, 0, verticalBadgeMargin)
            }
            POSITION_BOTTOM_RIGHT -> {
                lp.gravity = Gravity.END or Gravity.BOTTOM
                lp.setMargins(0, 0, horizontalBadgeMargin, verticalBadgeMargin)
            }
            POSITION_CENTER -> {
                lp.gravity = Gravity.CENTER
                lp.setMargins(0, 0, 0, 0)
            }
            POSITION_BOTTOM_NAVIGATION -> {
                lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                lp.setMargins(
                    horizontalBadgeMargin,
                    verticalBadgeMargin,
                    0,
                    0
                )
            }
        }
        layoutParams = lp
    }

    private fun dipToPixels(dip: Int): Int {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip.toFloat(), resources.displayMetrics)
        return px.toInt()
    }

    /*
    * @method : set horizontal margin according to number
    *
    * */

    private fun setHorizontalBadgeMargin(value: Int): Int {
        return when {
            value >= DEFAULT_NUMBER -> BOTTOM_NAVIGATION_HORIZONTAL_MARGIN
            else -> DEFAULT_BOTTOM_NAVIGATION_MARGIN
        }
    }

    companion object {
        const val POSITION_TOP_LEFT = 1
        const val POSITION_TOP_RIGHT = 2
        const val POSITION_BOTTOM_LEFT = 3
        const val POSITION_BOTTOM_RIGHT = 4
        const val POSITION_CENTER = 5
        const val POSITION_BOTTOM_NAVIGATION = 6
        private const val DEFAULT_LR_PADDING_DIP = 6
        private const val DEFAULT_CORNER_RADIUS_DIP = 20
        private const val DEFAULT_TEXT_COLOR = Color.WHITE
        private const val DURATION = 300L
        private const val OVERSHOOT_TENSION = 4.5f
        private const val DEFAULT_UPPER_BOUND = 99
        private const val DEFAULT_UPPER_BOUND_SUFFIX = "+"
        private const val DEFAULT_BOTTOM_NAVIGATION_MARGIN = 8
        private const val BOTTOM_NAVIGATION_HORIZONTAL_MARGIN = 13
        private const val DEFAULT_UPDATE_SCALE = 1.3f
        private const val DEFAULT_NUMBER = 10
    }
}

fun BottomNavigationView.addBadge(@IdRes viewId: Int): BadgeView {
    return BadgeView(this.context, this, viewId)
}
