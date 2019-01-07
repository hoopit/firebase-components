package io.ezet.android.ui.widgets

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import io.ezet.android.ui.R

class EmptyRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    private var mHandler: Handler = Handler()
    private var emptyView: View? = null
    private val check = Runnable { checkIfEmpty() }

    companion object {
        const val DEFER_CHECK_DELAY = 500L
    }

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
//            deferCheck()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            deferCheck()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            deferCheck()
        }
    }

    override fun onAttachedToWindow() {
        if (emptyView == null) {
            val emptyView = emptyViewId?.let { (parent as? View)?.findViewById<View>(it) }
            emptyView?.let { setEmptyView(it) } ?: setDefaultEmptyView()
        }
        super.onAttachedToWindow()
    }

    private var emptyViewId: Int?

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.EmptyRecyclerView)
        emptyViewId = typedArray.getResourceId(R.styleable.EmptyRecyclerView_emptyView, 0)
        emptyViewId = if (emptyViewId != 0) emptyViewId else null
        typedArray.recycle()
    }

    private fun deferCheck() {
        mHandler.removeCallbacks(check)
        mHandler.postDelayed(check, DEFER_CHECK_DELAY)
    }

    private fun checkIfEmpty() {
        if (emptyView == null) return
        val emptyViewVisible = adapter?.itemCount == 0
        emptyView?.visibility = if (emptyViewVisible) View.VISIBLE else View.GONE
        visibility = if (emptyViewVisible) View.GONE else View.VISIBLE
    }

    override fun setAdapter(adapter: RecyclerView.Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        deferCheck()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setEmptyView(emptyView: View) {
        this.emptyView?.visibility = View.GONE
        emptyView.visibility = View.GONE
        this.emptyView = emptyView
        deferCheck()
    }

    private fun setDefaultEmptyView(@StringRes stringRes: Int = R.string.EmptyRecyclerView_defaultText) {
//        val tv = TextView(context, null, R.attr.empty_list_placeholder)
        val tv = TextView(context, null)
        tv.setText(stringRes)
        tv.visibility = View.GONE
        setEmptyView(tv)
        val ll = LinearLayoutCompat(context)
        ll.orientation = LinearLayoutCompat.VERTICAL
        val parent = parent as? ViewGroup ?: return

        parent.removeView(this)
        ll.addView(
            this, 0, LinearLayoutCompat.LayoutParams(
                LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.MATCH_PARENT
            )
        )

        ll.addView(tv, 1, LinearLayoutCompat.LayoutParams(
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT,
            LinearLayoutCompat.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = resources.getDimensionPixelSize(R.dimen.emptyRecyclerView_margin)
            gravity = Gravity.CENTER
        })
        parent.addView(ll, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}
