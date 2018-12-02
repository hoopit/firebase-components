package io.hoopit.firebasecomponents.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.paging.FirebasePagedListQueryCache
import io.hoopit.firebasecomponents.paging.Listener
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class FirebaseConnectionManager() {

    companion object {
        val defaultInstance = FirebaseConnectionManager()
    }

    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<*, *>>()

    private val listCache = mutableMapOf<QuerySpec, FirebaseListQueryCache<*, *>>()


    private val pagedListScopes = ConcurrentHashMap<QuerySpec, SubscriptionScope<PagedListSubscription>>()
    private val basicListScopes = ConcurrentHashMap<QuerySpec, SubscriptionScope<ListSubscription>>()

    fun activatePaging(query: Query) {
        synchronized(pagedListScopes) {
            pagedListScopes.getOrPut(query.spec) { SubscriptionScope(query) }.subscribe()
        }
    }

    fun deactivatePaging(query: Query) {
        synchronized(pagedListScopes) {
            pagedListScopes[query.spec]?.unsubscribe()
        }
    }

    fun activate(query: Query) {
        synchronized(basicListScopes) {
            basicListScopes.getOrPut(query.spec) { SubscriptionScope(query) }.subscribe()
        }
    }

    fun deactivate(query: Query, dispose: Boolean = false) {
        synchronized(basicListScopes) {
            basicListScopes[query.spec]?.unsubscribe()
        }
    }

    fun <K : Comparable<K>, T : IFirebaseEntity> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        disconnectDelay: Long,
        orderByKey: (T) -> K
    ): FirebasePagedListQueryCache<K, T> {
        // TODO: consider attaching initial listener immediately
        synchronized(pagedListCache) {
            @Suppress("UNCHECKED_CAST")
            return pagedListCache.getOrPut(query.spec) { FirebasePagedListQueryCache(this, query, classModel, orderByKey) } as FirebasePagedListQueryCache<K, T>
        }
    }

    fun <K : Comparable<K>, T : IFirebaseEntity> getOrCreateListCache(
        query: Query,
        classModel: KClass<T>,
        disconnectDelay: Long,
        orderByKey: (T) -> K
    ): FirebaseListQueryCache<K, T> {
        @Suppress("UNCHECKED_CAST")
        return listCache.getOrElse(query.spec) {
            FirebaseListQueryCache(this, query, classModel, disconnectDelay, orderByKey)
        }.also { registerListQueryCache(it, query) } as FirebaseListQueryCache<K, T>
    }

    fun registerListQueryCache(cache: FirebaseListQueryCache<*, *>, query: Query): ChildEventListener {
        listCache.getOrPut(query.spec) { cache }
        synchronized(basicListScopes) {
            val subs = basicListScopes.getOrPut(cache.query.spec) { SubscriptionScope(cache.query) }
            return subs.getOrPut(cache.query) { ListSubscription(cache.query, cache.getListener()) }.childEventListener
        }
    }


    fun addPagedListener(cache: FirebasePagedListQueryCache<*, *>, baseQuery: Query, subQuery: Query = baseQuery): Listener<out IFirebaseEntity> {
        pagedListCache[baseQuery.spec] = cache
        synchronized(pagedListScopes) {
            val subs = pagedListScopes.getOrPut(baseQuery.spec) { SubscriptionScope(baseQuery) }
            return subs.getOrPut(subQuery) { PagedListSubscription(subQuery, cache.getListener()) }.childEventListener
        }
    }

    fun createListener(query: Query, listener: ValueEventListener, once: Boolean = false) {

    }

    fun <K : Comparable<K>, T : IFirebaseEntity> getCachedItem(itemId: K, classModel: KClass<T>): T {
        TODO()
    }

    interface ISubscription {
        fun on()
        fun off()

    }

    data class ListSubscription(
        val query: Query,
        val childEventListener: ChildEventListener
    ) : ISubscription {

        override fun on() {
            query.addChildEventListener(childEventListener)
        }

        override fun off() = query.removeEventListener(childEventListener)
    }

    data class PagedListSubscription(
        val query: Query,
        val childEventListener: Listener<out IFirebaseEntity>
    ) : ISubscription {

        override fun on() {
            query.addChildEventListener(childEventListener)
        }

        override fun off() = query.removeEventListener(childEventListener)
    }

    private data class SubscriptionScope<T : ISubscription> constructor(
        private val baseQuery: Query
    ) {
        private val subscriptions = ConcurrentHashMap<QuerySpec, T>()

        private var active = false

        @Synchronized
        fun removeListener(subQuery: Query): T? {
            return subscriptions.remove(subQuery.spec)?.apply { off() }
        }

        @Synchronized
        fun subscribe() {
            if (!active) {
                Timber.d("Adding listeners for: ${baseQuery.spec}")
                active = true
                subscriptions.values.forEach { it.on() }
            }
        }

        @Synchronized
        fun unsubscribe() {
            if (active) {
                Timber.d("Removing listeners for: ${baseQuery.spec}")
                active = false
                subscriptions.values.forEach { it.off() }
            }
        }

        @Synchronized
        fun getOrPut(subQuery: Query, subscription: () -> T): T {
            return subscriptions.getOrPut(subQuery.spec) {
                subscription().also { if (active) it.on() }
            }
        }
    }


}
