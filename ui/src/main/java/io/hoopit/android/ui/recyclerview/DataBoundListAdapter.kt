package io.hoopit.android.ui.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.hoopit.android.ui.extensions.throttleClicks
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.subjects.PublishSubject

/**
 * A generic RecyclerView adapter that uses Data Binding & DiffUtil.
 *
 * @param <T> Type of the resource in the list
 * @param <V> The type of the ViewDataBinding
</V></T> */
abstract class DataBoundListAdapter<T, V : ViewDataBinding>(
    private val enableClicks: Boolean = true,
    private val enableLongClicks: Boolean = false,
    differ: DiffUtil.ItemCallback<T> = DefaultDiffUtilItemCallback()
) : ListAdapter<T, DataBoundViewHolder<V>>(differ) {

    var lifecycleOwner: LifecycleOwner? = null

    private var clickEmitter: ObservableEmitter<Observable<T?>>? = null

    private val clickSources = mutableListOf<Observable<T?>>()
    /**
     * An observable stream of click events.
     * Subscribe to this to receive click events.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    val clicks: Observable<T> = Observable.create<Observable<T?>> {
        clickEmitter = it
        it.onNext(Observable.merge(clickSources))
    }.flatMap {
        it
    }
    val longClicks = PublishSubject.create<T>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBoundViewHolder<V> {
        val binding = createBinding(parent, getLayoutForViewType(viewType))
        return DataBoundViewHolder(binding)
    }

    @LayoutRes
    protected open fun getLayoutForViewType(viewType: Int): Int {
        return defaultLayoutRes
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
            val observable = binding.root.throttleClicks().map {
                map(binding)
            }
            clickEmitter?.onNext(observable) ?: clickSources.add(observable)
        }
        if (enableLongClicks) {
            binding.root.setOnLongClickListener {
                map(binding)?.let(longClicks::onNext)
                true
            }
        }
        return binding
    }

    override fun onBindViewHolder(holder: DataBoundViewHolder<V>, position: Int) {
        holder.binding.setLifecycleOwner(lifecycleOwner)
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
        throw NotImplementedError("map must be implemented or enableClicks must be set to false")
    }
}

class DefaultDiffUtilItemCallback<T> : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
}
