package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.firebaserealtime.paging.FirebasePagedListQueryCache
import kotlin.reflect.KClass

class FirebaseCache(
    private val scope: io.hoopit.android.firebaserealtime.core.Scope
) {

    private val valueCaches = mutableMapOf<KClass<*>, io.hoopit.android.firebaserealtime.cache.FirebaseValueCache<*>>()
    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<*, *>>()
    private val listCache =
        mutableMapOf<QuerySpec, io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache<*, *>>()

    fun dispose(querySpec: QuerySpec) {
        pagedListCache[querySpec]?.dispose()
    }

    fun <T : Any> getCache(clazz: KClass<T>): io.hoopit.android.firebaserealtime.cache.FirebaseValueCache<T> {
        @Suppress("UNCHECKED_CAST")
        return valueCaches.getOrPut(clazz) {
            io.hoopit.android.firebaserealtime.cache.FirebaseValueCache(scope, clazz)
        } as io.hoopit.android.firebaserealtime.cache.FirebaseValueCache<T>
    }

    fun <K : Comparable<K>, T : io.hoopit.android.firebaserealtime.core.FirebaseResource> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        orderByKey: (T) -> K
    ): FirebasePagedListQueryCache<K, T> {
        // TODO: consider attaching initial listener immediately
        @Suppress("UNCHECKED_CAST")
        return pagedListCache.getOrPut(query.spec) {
            FirebasePagedListQueryCache(scope, query, classModel, orderByKey)
        } as FirebasePagedListQueryCache<K, T>
    }

    fun <K : Comparable<K>, T : io.hoopit.android.firebaserealtime.core.FirebaseResource> getOrCreateListCache(
        query: Query,
        clazz: KClass<T>,
        orderByKey: (T) -> K
    ): io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache<K, T> {
        @Suppress("UNCHECKED_CAST")
        return listCache.getOrElse(query.spec) {
            io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache(
                scope,
                query,
                clazz,
                orderByKey
            )
        }.also {
            registerListQueryCache(
                it,
                query
            )
        } as io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache<K, T>
    }

    fun registerListQueryCache(
        cache: io.hoopit.android.firebaserealtime.cache.FirebaseListQueryCache<*, *>,
        query: Query
    ) {
        listCache.getOrPut(query.spec) { cache }
        val scope = scope.getResource(cache.query)
        return scope.addListener(cache.getChildListener())
    }
}
