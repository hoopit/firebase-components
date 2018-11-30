package io.hoopit.firebasecomponents.paging

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseQueryCacheBase
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity

class FirebasePagedListQueryCache<K : Comparable<K>, Type : IFirebaseEntity>(
    val firebaseConnectionManager: FirebaseConnectionManager,
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
