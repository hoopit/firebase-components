package io.hoopit.firebasecomponents.pagedlist

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.IFirebaseListEntity

class FirebasePagedListMemoryStore<K : Comparable<K>, Type : IFirebaseListEntity>(
    query: Query,
    orderKeyFunction: (Type) -> K
) {

    // TODO: disallow multiple stores attach the same query
    private val collection = FirebaseCollection(orderKeyFunction, query.spec.params.isViewFromLeft)

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    fun insert(previousId: String?, item: Type) {
        collection.addAfter(previousId, item)
        invalidate()
    }

    fun update(previousId: String?, item: Type) {
        collection.update(previousId, item)
        invalidate()
    }

    fun delete(item: Type) {
        val removed = collection.remove(item)
        if (removed) invalidate()
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

    private fun invalidate() {
        invalidationListeners.forEach { it() }
        invalidationListeners.clear()
    }

    fun get(it: K): Type? {
        return collection.get(it)
    }

}
