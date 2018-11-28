package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseCollection
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.ext.map
import io.hoopit.firebasecomponents.lifecycle.FirebaseListLiveData
import kotlin.reflect.KClass

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

class FirebaseListQueryCache<K : Comparable<K>, T : IFirebaseEntity>(
    query: Query,
    orderKeyFunction: (T) -> K,
    classModel: KClass<T>
) : FirebaseQueryCacheBase<K, T>(query, orderKeyFunction) {

    // TODO: Maybe use a simpler/faster backing collection type?

    private val liveData = FirebaseListLiveData(query, classModel, collection)

    fun getLiveData(): LiveData<List<T>> {
        return liveData.map {
            // TODO: remove
            it
        }
    }

    override fun invalidate() {
        liveData.postValue(collection.toList())
    }

}

class FirebasePagedListQueryCache<K : Comparable<K>, Type : IFirebaseEntity>(
    query: Query,
    orderKeyFunction: (Type) -> K
) : FirebaseQueryCacheBase<K, Type>(query, orderKeyFunction) {

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }


    fun getAfter(requestedInitialKey: K?, limit: Int): List<Type> {
        return collection.getAfter(requestedInitialKey, limit)
    }

    fun getBefore(requestedInitialKey: K?, limit: Int): List<Type> {
        return collection.getBefore(requestedInitialKey, limit)
    }

    private val invalidationListeners = mutableListOf<() -> Unit>()

    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    override fun invalidate() {
        invalidationListeners.forEach { it() }
        invalidationListeners.clear()
    }
}
