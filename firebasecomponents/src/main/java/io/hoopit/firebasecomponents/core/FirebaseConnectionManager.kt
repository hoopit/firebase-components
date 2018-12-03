package io.hoopit.firebasecomponents.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.paging.FirebasePagedListQueryCache
import io.hoopit.firebasecomponents.paging.QueryCacheListener
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class CacheManager {

}

class ScopeManager(private val getSubscription: (Query) -> FirebaseConnectionManager.FirebaseReference) {

    private val scopes = ConcurrentHashMap<Query, SubscriptionScope>()

    @Synchronized
    fun getScope(query: Query): SubscriptionScope {
        return scopes.getOrPut(query) { SubscriptionScope(query) }
    }

    @Synchronized
    fun activate(query: Query) {
        getScope(query).activate()
    }

    @Synchronized
    fun deactivate(query: Query) {
        scopes[query]?.deactivate()
    }

    inner class SubscriptionScope constructor(
        private val scope: Query
    ) {
        private val childListeners = ConcurrentHashMap<Query, MutableList<ChildEventListener>>()
        private val valueListeners = ConcurrentHashMap<Query, MutableList<ValueEventListener>>()
        private var active = false

//        @Synchronized
//        fun removeListener(subQuery: Query): T? {
//            return subscriptions.remove(subQuery.spec)?.apply { unsubscribe() }
//        }

        @Synchronized
        fun activate() {
            if (!active) {
                Timber.d("Adding listeners for: ${scope.spec}")
                active = true
                for ((query, listeners) in childListeners) {
                    getSubscription(query).subscribe(query, *listeners.toTypedArray())
                    activate(query)
                }
            }
        }

        @Synchronized
        fun deactivate() {
            if (active) {
                Timber.d("Removing listeners for: ${scope.spec}")
                active = false
                for ((query, listeners) in childListeners) {
                    getSubscription(query).unsubscribe(query, *listeners.toTypedArray())
                    deactivate(query)
                }
            }
        }

        @Synchronized
        fun addQuery(query: Query, listener: ChildEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            childListeners.getOrPut(query) { mutableListOf() }.add(listener).also {
                Timber.d("addQuery: activating listener immediately..")
                if (active) getSubscription(query).subscribe(query, listener)
            }
        }

        @Synchronized
        fun addQuery(query: Query, listener: ValueEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            valueListeners.getOrPut(query) { mutableListOf() }.add(listener).also {
                Timber.d("addQuery: activating listener immediately..")
                if (active) getSubscription(query).subscribe(query, listener)
            }
        }
    }
}

class FirebaseConnectionManager {

    companion object {
        val defaultInstance = FirebaseConnectionManager()
    }

    private val pagedListCache = mutableMapOf<QuerySpec, FirebasePagedListQueryCache<*, *>>()
    private val listCache = mutableMapOf<QuerySpec, FirebaseListQueryCache<*, *>>()

    private val subscriptions = ConcurrentHashMap<QuerySpec, FirebaseReference>()

    private val pagedListScopes = ScopeManager(::getSubscription)
    private val scopes = ScopeManager(::getSubscription)
    val valueScopes = ScopeManager(::getSubscription)

    private fun getSubscription(query: Query): FirebaseReference {
        return subscriptions.getOrPut(query.spec) { FirebaseReference(querySpec = query.spec) }
    }

    fun activatePaging(query: Query) {
        pagedListScopes.activate(query)
    }

    fun deactivatePaging(query: Query) {
        pagedListScopes.deactivate(query)
    }

    fun activate(query: Query) {
        scopes.activate(query)
    }

    fun deactivate(query: Query, dispose: Boolean = false) {
        scopes.deactivate(query)
    }

    fun <K : Comparable<K>, T : IFirebaseEntity> getOrCreatePagedCache(
        query: Query,
        classModel: KClass<T>,
        disconnectDelay: Long,
        orderByKey: (T) -> K
    ): FirebasePagedListQueryCache<K, T> {
        // TODO: consider attaching initial listener immediately
        @Suppress("UNCHECKED_CAST")
        return pagedListCache.getOrPut(query.spec) {
            FirebasePagedListQueryCache(this, query, classModel, orderByKey)
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
            FirebaseListQueryCache(this, query, classModel, disconnectDelay, orderByKey)
        }.also { registerListQueryCache(it, query) } as FirebaseListQueryCache<K, T>
    }

    fun registerListQueryCache(cache: FirebaseListQueryCache<*, *>, query: Query) {
        listCache.getOrPut(query.spec) { cache }
        val scope = scopes.getScope(cache.query)
        return scope.addQuery(cache.query, cache.getListener())
    }

    fun addPagedListener(cache: FirebasePagedListQueryCache<*, *>, scope: Query, subQuery: Query = scope): QueryCacheListener<out IFirebaseEntity> {
        pagedListCache[scope.spec] = cache
        val listener = cache.getListener()
        pagedListScopes.getScope(scope).addQuery(subQuery, listener)
        return listener
    }

    fun createListener(query: Query, listener: ValueEventListener, once: Boolean = false) {

    }

    fun <K : Comparable<K>, T : IFirebaseEntity> getCachedItem(itemId: K, classModel: KClass<T>): T {
        TODO()
    }


    class FirebaseReference(private val querySpec: QuerySpec) {

        // TODO: refactor
        private val childEventSubs = mutableMapOf<Query, MutableList<ChildEventListener>>()
        private var numChildEventSubs = 0

        private val valueEventSubs = mutableMapOf<Query, MutableList<ValueEventListener>>()
        private var numValueEventSubs = 0

        @Synchronized
        fun subscribe(query: Query, vararg listeners: ChildEventListener) {
            numChildEventSubs = subscribeInternal(query, childEventSubs, numChildEventSubs, *listeners) { query.addChildEventListener(childListener) }
        }

        @Synchronized
        fun subscribe(query: Query, vararg listeners: ValueEventListener) {
            // TODO: add support for Once values
            numValueEventSubs = subscribeInternal(query, valueEventSubs, numValueEventSubs, *listeners) { query.addValueEventListener(valueListener) }
        }

        @Synchronized
        fun unsubscribe(query: Query, vararg listeners: ValueEventListener) {
            numValueEventSubs = unsubscribeInternal(query, valueEventSubs, numValueEventSubs, *listeners) { query.removeEventListener(valueListener) }
        }

        @Synchronized
        fun unsubscribe(query: Query, vararg listeners: ChildEventListener) {
            numChildEventSubs = unsubscribeInternal(query, childEventSubs, numChildEventSubs, *listeners) { query.removeEventListener(childListener) }
        }

        private inline fun <T> subscribeInternal(query: Query, map: MutableMap<Query, MutableList<T>>, count: Int, vararg listeners: T, activate: () -> Unit): Int {
            assert(querySpec == query.spec) { "Can not subscribe to a Query with different QuerySpec." }
            assert(!map.contains(query)) { "Adding multiple listeners on the same Query is not yet supported. Consider creating a new Query." }
            val list = map.getOrPut(query) { mutableListOf() }
            val added = list.addAll(listeners)
            if (added && numChildEventSubs == 0) activate()
            return count + listeners.size
        }

        private inline fun <T> unsubscribeInternal(query: Query, map: MutableMap<Query, MutableList<T>>, count: Int, vararg listeners: T, deactivate: () -> Unit): Int {
            assert(querySpec == query.spec) { "Can not unsubscribe from a Query with different QuerySpec." }
            var newCount = count
            listeners.forEach {
                val removed = map[query]?.remove(it)
                if (removed == true) --newCount
            }
            if (newCount == 0) query.removeEventListener(childListener)
            else if (newCount < 0) throw IllegalStateException("Attempting to unsub with 0 subs")
            return count - newCount
        }

        private val childListener = object : ChildEventListener {

            override fun onCancelled(error: DatabaseError) {
                childEventSubs.values.forEach { it.forEach { it.onCancelled(error) } }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { it.forEach { it.onChildMoved(snapshot, previousChildKey) } }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { it.forEach { it.onChildChanged(snapshot, previousChildKey) } }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { it.forEach { it.onChildAdded(snapshot, previousChildKey) } }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                childEventSubs.values.forEach { it.forEach { it.onChildRemoved(snapshot) } }
            }
        }

        private val valueListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                valueEventSubs.values.forEach { it.forEach { it.onCancelled(p0) } }
            }

            override fun onDataChange(p0: DataSnapshot) {
                valueEventSubs.values.forEach { it.forEach { it.onDataChange(p0) } }
            }
        }
    }
}
