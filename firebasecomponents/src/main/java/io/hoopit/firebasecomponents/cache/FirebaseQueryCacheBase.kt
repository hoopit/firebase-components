package io.hoopit.firebasecomponents.cache

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseCollection
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import timber.log.Timber

abstract class FirebaseQueryCacheBase<K : Comparable<K>, Type : IFirebaseEntity>(
    private val query: Query,
    private val orderKeyFunction: (Type) -> K
) {

    private val handler = Handler(Looper.getMainLooper())

    private var pendingInvalidate = false

    private val listener = Runnable {
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

    fun getLiveData(it: K): LiveData<Type>? {
        return items.getOrPut(it) {
            val liveData = MutableLiveData<Type>()
            collection.get(it)?.let {
                liveData.postValue(it)
            }
            liveData
        }
    }

    private fun dispatchInvalidate() {
        handler.removeCallbacks(listener)
        if (query.spec.params.hasLimit() && collection.size == query.spec.params.limit) {
            // TODO: Dispatch immediately if requested initial size or page size is reached?
            Timber.d("dispatchInvalidate: Limit reached, invalidating immediately...")
            invalidate()
        } else {
            handler.postDelayed(listener, 100)
        }
    }

    protected abstract fun invalidate()

}
