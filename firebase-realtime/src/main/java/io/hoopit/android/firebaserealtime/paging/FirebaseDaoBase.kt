package io.hoopit.android.firebaserealtime.paging

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.common.liveData
import io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache
import io.hoopit.android.firebaserealtime.core.FirebaseCache
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.Scope
import kotlin.reflect.KClass

abstract class FirebaseDaoBase<K : Comparable<K>, V : FirebaseResource>(
    private val classModel: KClass<V>,
    private val disconnectDelay: Long,
    private val cacheManager: FirebaseCache = Scope.defaultInstance.cache
) {

    private val pagedCacheMap = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<K, V>>()
    private val listCacheMap = mutableMapOf<QuerySpec, FirebaseListQueryCache<K, V>>()

    private fun getPagedQueryCache(
        query: Query,
        sortedKeyFunction: (V) -> K
    ): FirebasePagedListQueryCache<K, V> {
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
    ): FirebaseListQueryCache<K, V> {
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
        // TODO: Improve
        if (query == null) {
            listCacheMap.values.first {
                return it.getLiveData(itemId)
            }
            return liveData(null)
        } else {
            return cacheManager.getItemCache(classModel).getLiveData(query, disconnectDelay)
        }
    }
}
