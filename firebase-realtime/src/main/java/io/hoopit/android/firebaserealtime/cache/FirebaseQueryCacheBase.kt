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

abstract class FirebaseQueryCacheBase<K : Comparable<K>, Type : IFirebaseEntity>(
    private val query: Query,
    orderKeyFunction: (Type) -> K
) : IManagedCache {

    override fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query) {
//        scope.dispatchDeactivate()
    }

    override fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query) {
//        scope.dispatchActivate()
    }

    override fun dispose() {
        collection.clear()
        invalidate()
//        TODO("not implemented")
    }

    private val invalidationHandler = Handler(Looper.getMainLooper())

    private val invalidationTask = Runnable {
        Timber.d("dispatchInvalidate: executing delayed invalidate")
        invalidate()
    }

    protected val collection = FirebaseCollection(
        orderKeyFunction,
        query.spec.params.isViewFromLeft
    )

    val size: Int
        get() = collection.size

    fun indexOf(item: Type): Int {
        return collection.indexOf(item)
    }

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
//        collection.move(previousChildName, child)
        dispatchInvalidate()
    }

    open fun update(previousId: String?, item: Type) {
        collection.update(previousId, item)
        items[item.entityId]?.postValue(item)
        dispatchInvalidate()
    }

    open fun delete(item: Type) {
        val removed = collection.remove(item)
        items[item.entityId]?.postValue(null)
        if (removed) dispatchInvalidate()
    }

    open fun get(it: K): Type? {
        return collection.get(it)
    }

    fun getLiveData(entityId: String): LiveData<Type?> {
        return items.getOrPut(entityId) {
            return liveData(collection.singleOrNull { it.entityId == entityId })
//            return liveData(collection.get(it))
        }
    }

    private fun dispatchInvalidate() {
        invalidationHandler.removeCallbacks(invalidationTask)
        invalidationHandler.postDelayed(invalidationTask, 100)
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
