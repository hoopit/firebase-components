package io.hoopit.android.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * A generic RecyclerView adapter that uses Data Binding & DiffUtil.
 *
 * @param <T> Type of the resource in the list
 * @param <V> The type of the ViewDataBinding
</V></T> */
abstract class DataBoundListAdapter<T, V : ViewDataBinding>(
    differ: DiffUtil.ItemCallback<T>,
    var lifecycleOwner: LifecycleOwner? = null,
    private val enableClicks: Boolean = true,
    private val enableLongClicks: Boolean = false,
    hasStableIds: Boolean = false
) : ListAdapter<T, DataBoundViewHolder<V>>(differ), IAdapter<T> {

    init {
        setHasStableIds(hasStableIds)
    }

    override fun submit(list: List<T>?, callback: Runnable?) = submitList(list)

    final override fun setHasStableIds(hasStableIds: Boolean) {
        super.setHasStableIds(hasStableIds)
    }

    override fun getItemId(position: Int): Long {
        check(!hasStableIds()) { "You need to override getItemId when hasStableIds = true" }
        return 0
    }

    private val clickSource = PublishSubject.create<T>()
    override val clicks: Observable<T> = clickSource.throttleFirst(500, TimeUnit.MILLISECONDS)

    private val longClickSource = PublishSubject.create<T>()
    override val longClicks: Observable<T> = longClickSource.throttleFirst(500, TimeUnit.MILLISECONDS)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBoundViewHolder<V> {
        val binding = createBinding(parent, getLayoutForViewType(viewType))
        return DataBoundViewHolder(binding)
    }

    @LayoutRes
    protected open fun getLayoutForViewType(viewType: Int): Int {
        return defaultLayoutRes
    }

    var onClickListener: (T) -> Unit = clickSource::onNext

    var onLongClickListener: (T) -> Unit = longClickSource::onNext

    protected fun onClick(binding: V) {
        map(binding)?.let { onClickListener(it) }
    }

    protected fun onLongClick(binding: V) {
        map(binding)?.let { onLongClickListener(it) }
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
                onClick(binding)
            }
        }
        if (enableLongClicks) {
            binding.root.setOnLongClickListener {
                onLongClick(binding)
                true
            }
        }
        return binding
    }

    override fun onBindViewHolder(holder: DataBoundViewHolder<V>, position: Int) {
        holder.binding.lifecycleOwner = lifecycleOwner
        bind(holder.binding, getItem(position), position)
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
        // TODO: log
        return null
    }

    class DefaultDiffUtilItemCallback<T> : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
        override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    }
}


