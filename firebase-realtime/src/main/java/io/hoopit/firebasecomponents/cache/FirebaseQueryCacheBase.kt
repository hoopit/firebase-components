package io.hoopit.firebasecomponents.cache

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseCollection
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.ext.liveData
import timber.log.Timber

abstract class FirebaseQueryCacheBase<K : Comparable<K>, Type : IFirebaseEntity>(
    private val query: Query,
    private val orderKeyFunction: (Type) -> K
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

    private var isInvalidatePending = false

    private val invalidationTask = Runnable {
        Timber.d("dispatchInvalidate: executing delayed invalidate")
        invalidate()
    }

    protected val collection = FirebaseCollection(orderKeyFunction, query.spec.params.isViewFromLeft)

    val size: Int
        get() = collection.size

    fun indexOf(item: Type): Int {
        return collection.indexOf(item)
    }

    protected val items = mutableMapOf<K, MutableLiveData<Type>>()

    open fun insert(previousId: String?, item: Type) {
        collection.addAfter(previousId, item)
        items[orderKeyFunction(item)]?.postValue(item)
        dispatchInvalidate()
    }

    open fun insertAll(items: Collection<Type>) {
        collection.addAll(items)
        items.forEach { this.items[orderKeyFunction(it)]?.postValue(it) }
        dispatchInvalidate()
    }

    open fun update(previousId: String?, item: Type) {
        collection.update(previousId, item)
        items[orderKeyFunction(item)]?.postValue(item)
        dispatchInvalidate()
    }

    open fun delete(item: Type) {
        val removed = collection.remove(item)
        items[orderKeyFunction(item)]?.postValue(null)
        if (removed) dispatchInvalidate()
    }

    open fun get(it: K): Type? {
        return collection.get(it)
    }

    fun getLiveData(it: K): LiveData<Type?> {
        return items.getOrPut(it) {
            return liveData(collection.get(it))
        }
    }

    private fun dispatchInvalidate() {
        invalidationHandler.removeCallbacks(invalidationTask)
        if (query.spec.params.hasLimit() && collection.size == query.spec.params.limit) {
            // TODO: Dispatch immediately if requested initial size or page size is reached?
            Timber.d("dispatchInvalidate: Limit reached, invalidating immediately...")
            invalidate()
        } else {
            invalidationHandler.postDelayed(invalidationTask, 100)
        }
    }

    protected abstract fun invalidate()

}
