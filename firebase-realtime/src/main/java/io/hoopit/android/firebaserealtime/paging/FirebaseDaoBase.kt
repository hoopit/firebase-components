package io.hoopit.android.firebaserealtime.paging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.common.liveData
import io.hoopit.android.firebaserealtime.cache.FirebaseListCache
import io.hoopit.android.firebaserealtime.core.FirebaseCache
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.Scope
import java.util.TreeMap
import kotlin.reflect.KClass

abstract class FirebaseDaoBase<K : Comparable<K>, V : FirebaseResource>(
    private val classModel: KClass<V>,
    private val disconnectDelay: Long,
    private val cacheManager: FirebaseCache = Scope.defaultInstance.cache
) {

    private val pagedCacheMap = mutableMapOf<QuerySpec, FirebasePagedListCache<K, V>>()
    private val listCacheMap = mutableMapOf<QuerySpec, FirebaseListCache<K, V>>()

    protected inline fun <T, K, P : Comparable<P>> LiveData<List<T>>.orderByChild(
        crossinline f: (T) -> LiveData<K?>,
        crossinline t: (K) -> P
    ): MediatorLiveData<List<T>> {
        val med = MediatorLiveData<List<T>>()
        med.addSource(this) { list ->
            val map = TreeMap<P, T>()
            list.forEach { item ->
                med.addSource(f(item)) { subItem ->
                    subItem?.let {
                        map.values.remove(item)
                        map[t(it)] = item
                        med.postValue(map.values.reversed())
                    }
                }
            }
        }
        return med
    }

    private fun getPagedQueryCache(
        query: Query,
        sortedKeyFunction: (V) -> K
    ): FirebasePagedListCache<K, V> {
        return pagedCacheMap.getOrPut(query.spec) {
            cacheManager.getOrCreatePagedCache(
                query,
                classModel,
                sortedKeyFunction
            )
        }
    }

    private fun getListQueryCache(
        query: Query,
        sortedKeyFunction: (V) -> K
    ): FirebaseListCache<K, V> {
        return listCacheMap.getOrPut(query.spec) {
            cacheManager.getOrCreateListCache(
                query,
                classModel,
                sortedKeyFunction
            )
        }
    }

    protected fun getList(query: Query, sortedKeyFunction: (V) -> K): LiveData<List<V>> {
        return getListQueryCache(query, sortedKeyFunction).getLiveData(disconnectDelay)
    }

    protected fun getPagedList(
        query: Query,
        sortedKeyFunction: (V) -> K
    ): FirebaseDataSourceFactory<K, V> {
        return getPagedQueryCache(query, sortedKeyFunction).getDataSourceFactory()
    }

    protected fun getItem(itemId: String, query: Query?): LiveData<V?> {
        // TODO: Implement get single item from list
        if (query == null) {
            listCacheMap.values.first {
                return it.getItem(itemId)
            }
            return liveData(null)
        } else {
            return cacheManager.getItemCache(classModel).getLiveData(query, disconnectDelay)
        }
    }
}
