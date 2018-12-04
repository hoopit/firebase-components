package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.core.CacheManager
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.ext.liveData
import kotlin.reflect.KClass

abstract class FirebaseDaoBase<K : Comparable<K>, V : ManagedFirebaseEntity>(
    private val classModel: KClass<V>,
    private val disconnectDelay: Long,
    private val cacheManager: CacheManager = Scope.defaultInstance.cacheManager
) {

    private val pagedCacheMap = mutableMapOf<Query, FirebasePagedListQueryCache<K, V>>()
    private val listCacheMap = mutableMapOf<Query, FirebaseListQueryCache<K, V>>()

    private fun getPagedQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebasePagedListQueryCache<K, V> {
        return pagedCacheMap.getOrPut(query) {
            cacheManager.getOrCreatePagedCache(
                    query,
                    classModel,
                    disconnectDelay,
                    sortedKeyFunction
            )
        }
    }

    private fun getListQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebaseListQueryCache<K, V> {
        return listCacheMap.getOrPut(query) {
            cacheManager.getOrCreateListCache(
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
        // TODO: Improve
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
        TODO("Not implemented.") // Use value cache
    }
}



