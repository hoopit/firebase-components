package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.ext.noLiveData
import kotlin.reflect.KClass

abstract class FirebaseDaoBase<K : Comparable<K>, V : IFirebaseEntity>(
    private val classModel: KClass<V>
) {

    private val pagedCacheMap = mutableMapOf<Query, FirebasePagedListQueryCache<K, V>>()
    private val listCacheMap = mutableMapOf<Query, FirebaseListQueryCache<K, V>>()

//    private val listCacheMap = mutableMapOf<Query, FirebaseListLiveData<K, V>>()

    protected fun getPagedQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebasePagedListQueryCache<K, V> {
        return pagedCacheMap.getOrPut(query) { FirebasePagedListQueryCache(query, sortedKeyFunction) }
    }

    protected fun getListQueryCache(query: Query, sortedKeyFunction: (V) -> K): FirebaseListQueryCache<K, V> {
        return listCacheMap.getOrPut(query) { FirebaseListQueryCache(query, sortedKeyFunction, classModel) }
    }

    protected fun createList(query: Query, sortedKeyFunction: (V) -> K): LiveData<List<V>> {
        return getListQueryCache(query, sortedKeyFunction).getLiveData()
    }

    protected fun createPagedList(query: Query, sortedKeyFunction: (V) -> K): FirebaseDataSourceFactory<K, V> {
        return getPagedQueryCache(query, sortedKeyFunction).getDataSourceFactory()
    }

    protected fun getCachedItem(itemId: K): LiveData<V> {
        listCacheMap.values.forEach {
            val item = it.getLiveData(itemId)
            if (item != null) return item
        }
        pagedCacheMap.values.forEach {
            val item = it.getLiveData(itemId)
            if (item != null) return item
        }
        return noLiveData()
    }

    protected fun getOrFetchItem(itemId: K, query: Query, cacheOnly: Boolean = true, sortedKeyFunction: (V) -> K): LiveData<V> {
        TODO()
    }
}



