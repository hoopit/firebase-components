package io.hoopit.firebasecomponents.core

import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.cache.FirebaseValueCache
import io.hoopit.firebasecomponents.paging.FirebasePagedListQueryCache
import kotlin.reflect.KClass

class CacheManager(
    private val scopes: Scope
) {

    private val valueCaches = mutableMapOf<KClass<*>, FirebaseValueCache<*>>()
    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<*, *>>()
    private val listCache = mutableMapOf<QuerySpec, FirebaseListQueryCache<*, *>>()

    fun <T : Any> getCache(clazz: KClass<T>): FirebaseValueCache<T> {
        @Suppress("UNCHECKED_CAST")
        return valueCaches.getOrPut(clazz) {
            FirebaseValueCache(scopes, clazz)
        } as FirebaseValueCache<T>
    }

    fun <K : Comparable<K>, T : ManagedFirebaseEntity> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        disconnectDelay: Long,
        orderByKey: (T) -> K
    ): FirebasePagedListQueryCache<K, T> {
        // TODO: consider attaching initial listener immediately
        @Suppress("UNCHECKED_CAST")
        return pagedListCache.getOrPut(query.spec) {
            FirebasePagedListQueryCache(scopes, query, classModel, orderByKey)
        } as FirebasePagedListQueryCache<K, T>
    }

    fun <K : Comparable<K>, T : ManagedFirebaseEntity> getOrCreateListCache(
        query: Query,
        classModel: KClass<T>,
        disconnectDelay: Long,
        orderByKey: (T) -> K
    ): FirebaseListQueryCache<K, T> {
        @Suppress("UNCHECKED_CAST")
        return listCache.getOrElse(query.spec) {
            FirebaseListQueryCache(scopes, query, classModel, orderByKey)
        }.also { registerListQueryCache(it, query) } as FirebaseListQueryCache<K, T>
    }

    fun registerListQueryCache(cache: FirebaseListQueryCache<*, *>, query: Query) {
        listCache.getOrPut(query.spec) { cache }
        val scope = scopes.getScope(cache.query)
        return scope.addListener(cache.getListener())
    }
}
