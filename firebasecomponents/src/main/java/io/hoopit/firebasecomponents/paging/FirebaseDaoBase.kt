package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.ext.liveData
import kotlin.reflect.KClass

abstract class FirebaseDaoBase<K : Comparable<K>, V : IFirebaseEntity>(
    private val classModel: KClass<V>,
    private val disconnectDelay: Long,
    private val firebaseConnectionManager: FirebaseConnectionManager = FirebaseConnectionManager.defaultInstance
) {

    private val pagedCacheMap = mutableMapOf<Query, FirebasePagedListQueryCache<K, V>>()
    private val listCacheMap = mutableMapOf<Query, FirebaseListQueryCache<K, V>>()

    protected fun getPagedQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebasePagedListQueryCache<K, V> {
        return pagedCacheMap.getOrPut(query) {
            firebaseConnectionManager.getOrCreatePagedCache(
                    query,
                    classModel,
                    disconnectDelay,
                    sortedKeyFunction
            )
        }
    }

    protected fun getListQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebaseListQueryCache<K, V> {
        return listCacheMap.getOrPut(query) {
            firebaseConnectionManager.getOrCreateListCache(
                    query,
                    classModel,
                    disconnectDelay,
                    sortedKeyFunction
            )
        }
    }

    protected fun createList(query: Query, sortedKeyFunction: (V) -> K): LiveData<List<V>> {
        return getListQueryCache(query, sortedKeyFunction).getLiveData()
    }

    protected fun createPagedList(query: Query, sortedKeyFunction: (V) -> K): FirebaseDataSourceFactory<K, V> {
        return getPagedQueryCache(query, sortedKeyFunction).getDataSourceFactory()
    }

    protected fun getCachedItem(itemId: K): LiveData<V> {
//        return liveData(firebaseConnectionManager.getCachedItem(itemId, classModel))
        listCacheMap.values.forEach {
            val item = it.getLiveData(itemId)
            if (item != null) return item
        }
//        pagedCacheMap.values.forEach {
//            val item = it.getLiveData(itemId)
//            if (item != null) return item
//        }
        return liveData(null)
    }

    protected fun getOrFetchItem(itemId: K, query: Query, cacheOnly: Boolean = true, sortedKeyFunction: (V) -> K): LiveData<V> {
        TODO()
    }
}



