package io.hoopit.firebasecomponents.paging

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseQueryCacheBase
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebasePagedListQueryCache<K : Comparable<K>, Type : IFirebaseEntity>(
    val firebaseConnectionManager: FirebaseConnectionManager,
    private val query: Query,
    private val classModel: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseQueryCacheBase<K, Type>(query, orderKeyFunction) {

    override fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        firebaseConnectionManager.deactivatePaging(this.query)
    }

    override fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        firebaseConnectionManager.activatePaging(this.query)
    }

    private val invalidationListeners = mutableListOf<() -> Unit>()

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getListener(): Listener<Type> {
        return Listener(classModel, this)
    }

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    fun getAfter(requestedInitialKey: K?, limit: Int): List<Type> {
        return collection.getAfter(requestedInitialKey, limit)
    }

    fun getBefore(requestedInitialKey: K?, limit: Int): List<Type> {
        return collection.getBefore(requestedInitialKey, limit)
    }


    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    override fun invalidate() {
        invalidationListeners.forEach { it() }
        invalidationListeners.clear()
    }

}
