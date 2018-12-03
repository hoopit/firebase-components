package io.hoopit.firebasecomponents.paging

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseManagedQueryCache
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebasePagedListQueryCache<K : Comparable<K>, Type : IFirebaseEntity>(
    val firebaseConnectionManager: FirebaseConnectionManager,
    query: Query,
    private val classModel: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseManagedQueryCache<K, Type>(firebaseConnectionManager, query, orderKeyFunction) {

    override fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        firebaseConnectionManager.deactivatePaging(this.query)
    }

    override fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        firebaseConnectionManager.activatePaging(this.query)
    }

    private val invalidationListeners = mutableListOf<() -> Unit>()

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getListener(): QueryCacheListener<Type> {
        return QueryCacheListener(classModel, this)
    }

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    fun getAround(key: K?, limit: Int): List<Type> {
        return collection.getAround(key, limit)
    }

    fun getAfter(key: K, limit: Int): List<Type> {
        return collection.getAfter(key, limit)
    }

    fun getBefore(key: K, limit: Int): List<Type> {
        return collection.getBefore(key, limit)
    }


    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    override fun invalidate() {
        invalidationListeners.forEach { it() }
        invalidationListeners.clear()
    }

}
