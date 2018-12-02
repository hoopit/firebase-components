package io.hoopit.firebasecomponents.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.cache.FirebaseListQueryCache
import io.hoopit.firebasecomponents.paging.FirebasePagedListQueryCache
import io.hoopit.firebasecomponents.paging.Listener
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class CacheManager {

}

class ScopeManager(private val getSubscription: (Query) -> FirebaseConnectionManager.IFirebaseReference) {

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
        private val listeners = ConcurrentHashMap<Query, MutableList<ChildEventListener>>()
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
                for ((query, listeners) in listeners) {
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
                for ((query, listeners) in listeners) {
                    getSubscription(query).unsubscribe(query, *listeners.toTypedArray())
                    deactivate(query)
                }
            }
        }

        @Synchronized
        fun addQuery(query: Query, listener: ChildEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            listeners.getOrPut(query) { mutableListOf() }.add(listener).also {
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

    private val subscriptions = ConcurrentHashMap<QuerySpec, IFirebaseReference>()

    private val pagedListScopes = ScopeManager(::getSubscription)
    private val scopes = ScopeManager(::getSubscription)

    private fun getSubscription(query: Query): IFirebaseReference {
        return subscriptions.getOrPut(query.spec) { FirebaseReference(querySpec = query.spec) }
    }

    // TODO: infinite depth sub queries

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

    fun registerListQueryCache(cache: FirebaseListQueryCache<*, *>, query: Query) {
        listCache.getOrPut(query.spec) { cache }
        val scope = scopes.getScope(cache.query)
        return scope.addQuery(cache.query, cache.getListener())
    }

    fun addPagedListener(cache: FirebasePagedListQueryCache<*, *>, scope: Query, subQuery: Query = scope): Listener<out IFirebaseEntity> {
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

    interface IFirebaseReference {
        fun subscribe(query: Query, vararg listeners: ChildEventListener)
        fun unsubscribe(query: Query, vararg listeners: ChildEventListener)
        val querySpec: QuerySpec
    }

    class FirebaseReference(override val querySpec: QuerySpec) : IFirebaseReference, ChildEventListener {

        override fun onCancelled(error: DatabaseError) {
            subscribers.values.forEach { it.forEach { it.onCancelled(error) } }
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {
            subscribers.values.forEach { it.forEach { it.onChildMoved(snapshot, previousChildKey) } }
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
            subscribers.values.forEach { it.forEach { it.onChildChanged(snapshot, previousChildKey) } }
        }

        override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
            subscribers.values.forEach { it.forEach { it.onChildAdded(snapshot, previousChildKey) } }
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            subscribers.values.forEach { it.forEach { it.onChildRemoved(snapshot) } }
        }

        private val subscribers = mutableMapOf<Query, MutableList<ChildEventListener>>()

        private var subscriptions = 0

        @Synchronized
        override fun subscribe(query: Query, vararg listeners: ChildEventListener) {
            assert(querySpec == query.spec) { "Can not subscribe to a Query with different QuerySpec." }
            assert(!subscribers.contains(query)) { "Adding multiple listeners on the same Query is not yet supported. Consider creating a new Query." }
            val list = subscribers.getOrPut(query) { mutableListOf() }
            val added = list.addAll(listeners)
            if (added && subscriptions == 0) query.addChildEventListener(this)
            subscriptions += listeners.size

        }

        @Synchronized
        override fun unsubscribe(query: Query, vararg listeners: ChildEventListener) {
            assert(querySpec == query.spec) { "Can not unsubscribe from a Query with different QuerySpec." }
            listeners.forEach {
                val removed = subscribers[query]?.remove(it)
                if (removed == true) --subscriptions
            }
            if (subscriptions == 0) query.removeEventListener(this)
            else if (subscriptions < 0) throw IllegalStateException("Attempting to unsub with 0 subs")
        }
    }


}
