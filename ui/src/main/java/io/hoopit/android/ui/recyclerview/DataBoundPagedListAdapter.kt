package io.hoopit.android.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import io.hoopit.android.ui.NetworkState
import io.hoopit.android.ui.R
import io.hoopit.android.ui.databinding.NetworkStateItemBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * A generic RecyclerView adapter that uses Data Binding & DiffUtil.
 *
 * @param <T> Type of the resource in the list
 * @param <V> The type of the ViewDataBinding
</V></T> */
abstract class DataBoundPagedListAdapter<T, V : ViewDataBinding>(
    differ: DiffUtil.ItemCallback<T>,
    private val enableClicks: Boolean = true,
    private val enableLongClicks: Boolean = false,
    var lifecycleOwner: LifecycleOwner? = null,
    private val endLoadingIndicator: Boolean = true,
    private val frontLoadingIndicator: Boolean = false
) : PagedListAdapter<T, DataBoundViewHolder<*>>(differ), IClickAdapter<T> {

    @LayoutRes
    private val networkStateRes = R.layout.network_state_item

    private var endLoadingState: NetworkState? = null

    private var frontLoadingState: NetworkState? = null

    private val clickThrottle: Long = 500L

    companion object {
        const val DEFAULT_LAYOUT = 84740
        const val FRONT_LOADING_INDICATOR = 84741
        const val END_LOADING_INDICATOR = 84742
    }

    /**
     * An observable stream of click events.
     * Subscribe to this to receive click events.
     */
    private val clickSource = PublishSubject.create<T>()
    final override val clicks: Observable<T> = clickSource.throttleFirst(clickThrottle, TimeUnit.MILLISECONDS)

    private val longClickSource = PublishSubject.create<T>()
    override val longClicks: Observable<T> = longClickSource.throttleFirst(clickThrottle, TimeUnit.MILLISECONDS)

    private val retryClickSource = PublishSubject.create<NetworkState>()
    val retryClicks: Observable<NetworkState> = retryClickSource.throttleFirst(clickThrottle, TimeUnit.MILLISECONDS)

    override fun getItem(position: Int): T? {
        return if (position < super.getItemCount() && position >= 0) super.getItem(position)
        else null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBoundViewHolder<*> {
        return when (viewType) {
            DEFAULT_LAYOUT -> DataBoundViewHolder(createBinding(parent, defaultLayoutRes))
            FRONT_LOADING_INDICATOR, END_LOADING_INDICATOR -> DataBoundViewHolder(
                createNetworkBinding(parent)
            )
            else -> DataBoundViewHolder(createBinding(parent, getLayoutForViewType(viewType)))
        }
    }

    final override fun getItemViewType(position: Int): Int {
        return if (isLoadingAtEnd() && position == itemCount - getExtraRows()) {
            END_LOADING_INDICATOR
        } else if (isLoadingAtFront() && position == 0) {
            FRONT_LOADING_INDICATOR
        } else {
            getCustomItemViewType(position)
        }
    }

    protected open fun getCustomItemViewType(position: Int): Int {
        return DEFAULT_LAYOUT
    }

    open fun updateEndLoadingState(newPagingState: NetworkState?) {
        if (!endLoadingIndicator || isLoadingAtFront()) return
        val previousState = this.endLoadingState
        val hadExtraRow = isLoadingAtEnd()
        this.endLoadingState = newPagingState
        val hasExtraRow = isLoadingAtEnd()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(itemCount + 1)
            } else {
                notifyItemInserted(itemCount)
            }
        } else if (hasExtraRow && previousState != newPagingState) {
            notifyItemChanged(itemCount)
        }
    }

    private fun isLoadingAtEnd() =
        endLoadingState != null && endLoadingState != NetworkState.success

    private fun isLoadingAtFront() =
        frontLoadingState != null && frontLoadingState != NetworkState.success

    private fun getExtraRows(): Int {
        var count = 0
        if (isLoadingAtEnd()) ++count
        if (isLoadingAtFront()) ++count
        return count
    }

    override fun getItemCount(): Int {
        return super.getItemCount() + getExtraRows()
    }

    open fun updateFrontLoadingState(newPagingState: NetworkState?) {
        if (!frontLoadingIndicator || isLoadingAtEnd()) return
        val previousState = this.frontLoadingState
        val hadExtraRow = isLoadingAtFront()
        this.frontLoadingState = newPagingState
        val hasExtraRow = isLoadingAtFront()
        if (hadExtraRow != hasExtraRow) {
            if (hadExtraRow) {
                notifyItemRemoved(0)
            } else {
                notifyItemInserted(0)
            }
        } else if (hasExtraRow && previousState != newPagingState) {
            notifyItemChanged(0)
        }
    }

    /**
     * Override this to customize the view binding
     */
    protected open fun createBinding(parent: ViewGroup, @LayoutRes layoutRes: Int): V {
        val binding = DataBindingUtil.inflate<V>(
            LayoutInflater.from(parent.context),
            layoutRes,
            parent,
            false
        )
        if (enableClicks) {
            binding.root.setOnClickListener {
                map(binding)?.let(clickSource::onNext)
            }
        }
        if (enableLongClicks) {
            binding.root.setOnLongClickListener {
                map(binding)?.let(longClickSource::onNext)
                true
            }
        }
        return binding
    }

    private fun createNetworkBinding(parent: ViewGroup): NetworkStateItemBinding {
        val binding = NetworkStateItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.retryButton.setOnClickListener {
            binding.item?.let { retryClickSource.onNext(it) }
        }
        return binding
    }

    @LayoutRes
    protected open fun getLayoutForViewType(viewType: Int): Int {
        return defaultLayoutRes
    }

    @Suppress("UNCHECKED_CAST", "UnsafeCast")
    override fun onBindViewHolder(holder: DataBoundViewHolder<*>, position: Int) {
        holder.binding.setLifecycleOwner(lifecycleOwner)
        when (getItemViewType(position)) {
            FRONT_LOADING_INDICATOR -> {
                holder.binding as NetworkStateItemBinding
                holder.binding.item = frontLoadingState
            }
            END_LOADING_INDICATOR -> {
                holder.binding as NetworkStateItemBinding
                holder.binding.item = endLoadingState
            }
            else -> {
                val actualPosition = if (isLoadingAtFront()) position - 1 else position
                bind(holder.binding as V, getItem(actualPosition), position)
            }
        }
        holder.binding.executePendingBindings()
    }

    /**
     * Bind the item to the binding
     */
    protected abstract fun bind(binding: V, item: T?, position: Int)

    /**
     * The [LayoutRes] for the RecyclerView item
     * This is used to inflate the view.
     */
    protected abstract val defaultLayoutRes: Int
        @LayoutRes get

    /**
     * Should return the bound item from a binding.
     * This is used to attach a click listener
     */
    open fun map(binding: V): T? {
        throw NotImplementedError("map must be implemented or disableClicks must be set to false")
    }
}
