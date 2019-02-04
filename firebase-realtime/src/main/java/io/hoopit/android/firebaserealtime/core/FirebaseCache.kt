package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.Query
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.firebaserealtime.cache.FirebaseListCache
import io.hoopit.android.firebaserealtime.cache.FirebaseValueCache
import io.hoopit.android.firebaserealtime.paging.FirebasePagedListCache
import kotlin.reflect.KClass

class FirebaseCache(
    private val firebaseScope: FirebaseScope
) {

    private val itemCaches = mutableMapOf<KClass<*>, FirebaseValueCache<*>>()
    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListCache<*, *>>()
    private val listCache = mutableMapOf<QuerySpec, FirebaseListCache<*, *>>()

    fun dispose(querySpec: QuerySpec) {
        pagedListCache[querySpec]?.dispose()
        pagedListCache.remove(querySpec)
    }

    fun <T : Any> getItemCache(clazz: KClass<T>): FirebaseValueCache<T> {
        @Suppress("UNCHECKED_CAST")
        return itemCaches.getOrPut(clazz) {
            FirebaseValueCache(firebaseScope, clazz)
        } as FirebaseValueCache<T>
    }

    fun <K : Comparable<K>, T : FirebaseResource> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        orderByKey: (T) -> K
    ): FirebasePagedListCache<K, T> {
        // TODO: consider attaching initial listener immediately
        @Suppress("UNCHECKED_CAST")
        return pagedListCache.getOrPut(query.spec) {
            FirebasePagedListCache(firebaseScope, query, classModel, orderByKey)
        } as FirebasePagedListCache<K, T>
    }

    fun <K : Comparable<K>, T : FirebaseResource> getOrCreateListCache(
        query: Query,
        clazz: KClass<T>,
        orderByKey: (T) -> K
    ): FirebaseListCache<K, T> {
        @Suppress("UNCHECKED_CAST")
        return listCache.getOrElse(query.spec) {
            FirebaseListCache(
                firebaseScope,
                query,
                clazz,
                orderByKey
            )
        }.also {
            registerListQueryCache(
                it,
                query
            )
        } as FirebaseListCache<K, T>
    }

    fun registerListQueryCache(
        cache: FirebaseListCache<*, *>,
        query: Query
    ) {
        listCache.getOrPut(query.spec) { cache }
        val scope = firebaseScope.getResource(cache.query)
        return scope.addListener(cache.getChildListener())
    }
}
