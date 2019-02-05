package io.hoopit.android.firebaserealtime.cache

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.android.common.liveData
import io.hoopit.android.firebaserealtime.core.FirebaseCollection
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import timber.log.Timber

/**
 * Base class for Firebase collection caches.
 */
abstract class FirebaseCollectionCacheBase<K : Comparable<K>, Type : IFirebaseEntity>(
    private val query: Query,
    orderKeyFunction: (Type) -> K
) {
    private val invalidationHandler = Handler(Looper.getMainLooper())

    private val invalidationTask = Runnable {
        Timber.d("dispatchInvalidate: executing delayed invalidate for ${query.spec}")
        invalidate()
    }

    protected val collection = FirebaseCollection(
        orderKeyFunction,
        query.spec.params.isViewFromLeft
    )

    val size: Int
        get() = collection.size

    protected val items = mutableMapOf<String, MutableLiveData<Type>>()

    open fun insert(previousId: String?, item: Type) {
        collection.addAfter(previousId, item)
        items[item.entityId]?.postValue(item)
        dispatchInvalidate()
    }

    open fun insertAll(items: Collection<Type>) {
        collection.addAll(items)
        items.forEach { this.items[it.entityId]?.postValue(it) }
        dispatchInvalidate()
    }

    fun move(previousChildName: String?, child: Type) {
        if (collection.move(previousChildName, child)) dispatchInvalidate()
    }

    open fun update(previousId: String?, item: Type) {
        collection.update(previousId, item)
        items[item.entityId]?.postValue(item)
        dispatchInvalidate()
    }

    /**
     * Whether remove requests should be ignored. Can be used to handle cases where the query has a limit,
     * and items are removed due to being pushed outside the limits of the query due to new entries being added.
     */
    protected open fun shouldIgnoreRemove(item: Type): Boolean {
        if (!query.spec.params.hasAnchoredLimit()) return false
        val pos = collection.position(item) + 1
        return (pos % query.spec.params.limit == 0 || (pos == collection.size && pos > query.spec.params.limit))
    }

    open fun delete(item: Type) {
        if (shouldIgnoreRemove(item)) return
        val removed = collection.remove(item)
        items[item.entityId]?.postValue(null)
        if (removed) dispatchInvalidate()
    }

    open fun get(it: K): Type? {
        return collection.get(it)
    }

    fun getItem(entityId: String): LiveData<Type?> {
        return items.getOrPut(entityId) {
            return liveData(collection.singleOrNull { it.entityId == entityId })
        }
    }

    private fun dispatchInvalidate() {
        invalidationHandler.removeCallbacks(invalidationTask)
        invalidationHandler.postDelayed(invalidationTask, 250)
        return
        if (query.spec.params.hasLimit() && collection.size == query.spec.params.limit) {
            // TODO: Dispatch immediately if page size is reached?
            Timber.d("dispatchInvalidate: Limit reached, invalidating immediately...")
//            invalidate()
            invalidationHandler.postDelayed(invalidationTask, 100)
        } else {
        }
    }

    protected abstract fun invalidate()
}
