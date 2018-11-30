package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseCollection
import io.hoopit.firebasecomponents.core.IFirebaseEntity

abstract class FirebaseQueryCacheBase<K : Comparable<K>, Type : IFirebaseEntity>(
    query: Query,
    private val orderKeyFunction: (Type) -> K
) {
    // TODO: prevent multiple caches for the same query

    protected val collection = FirebaseCollection(orderKeyFunction, query.spec.params.isViewFromLeft)

    protected val items = mutableMapOf<K, MutableLiveData<Type>>()

    fun insert(previousId: String?, item: Type) {
        collection.addAfter(previousId, item)
        items[orderKeyFunction(item)]?.postValue(item)
        invalidate()
    }

    fun update(previousId: String?, item: Type) {
        collection.update(previousId, item)
        items[orderKeyFunction(item)]?.postValue(item)
        invalidate()
    }

    fun delete(item: Type) {
        val removed = collection.remove(item)
        items[orderKeyFunction(item)]?.postValue(null)
        if (removed) invalidate()
    }

    fun get(it: K): Type? {
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

    protected abstract fun invalidate()
}
