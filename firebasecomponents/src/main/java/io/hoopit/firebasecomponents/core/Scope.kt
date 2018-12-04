package io.hoopit.firebasecomponents.core

import android.os.Handler
import android.os.Looper
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class Scope(
    private val referenceManager: FirebaseReferenceManager
) {

    lateinit var cacheManager: CacheManager

    companion object {
        val defaultInstance = Scope(FirebaseReferenceManager()).also { it.cacheManager = CacheManager(it) }
    }

    private val scopes = ConcurrentHashMap<Query, Resource>()


    @Synchronized
    fun getScope(query: Query, defaultDisconnectDelay: Long = 10000): Resource {
        return scopes.getOrPut(query) { Resource(query, defaultDisconnectDelay) }
    }

    @Synchronized
    fun activate(query: Query) {
        getScope(query).dispatchActivate()
    }

    @Synchronized
    fun deactivate(query: Query, disconnectDelay: Long? = null) {
        if (disconnectDelay != null) scopes[query]?.dispatchDeactivate(disconnectDelay)
        else scopes[query]?.dispatchDeactivate()
    }

    inner class Resource constructor(
        private val rootQuery: Query,
        private val disconnectDelay: Long
    ) {
        private val childListeners = ConcurrentHashMap<Query, MutableList<ChildEventListener>>()
        private val valueListeners = ConcurrentHashMap<Query, MutableList<ValueEventListener>>()
        private var active = false


        private val handler = Handler(Looper.getMainLooper())

        private var pendingRemoval = false


        private val listener = Runnable {
            deactivate()
            pendingRemoval = false
        }

        fun dispatchActivate() {
            handler.removeCallbacks(listener)
            if (!pendingRemoval) activate()
            pendingRemoval = false
        }

        fun dispatchDeactivate(disconnectDelay: Long = this.disconnectDelay) {
            if (disconnectDelay > 0) {
                pendingRemoval = true
                handler.postDelayed(listener, disconnectDelay)
            } else {
                pendingRemoval = false
                listener.run()
            }
        }

        @Synchronized
        private fun activate() {
            if (!active) {
                Timber.d("Adding listeners for: ${rootQuery.spec}")
                active = true
                for ((query, listeners) in childListeners) {
                    referenceManager.getSubscription(query).subscribe(query, *listeners.toTypedArray())
                    activate(query)
                }
            }
        }

        @Synchronized
        private fun deactivate() {
            if (active) {
                Timber.d("Removing listeners for: ${rootQuery.spec}")
                active = false
                for ((query, listeners) in childListeners) {
                    referenceManager.getSubscription(query).unsubscribe(query, *listeners.toTypedArray())
                    deactivate(query)
                }
            }
        }

        @Synchronized
        fun addSubQuery(query: Query, listener: ChildEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            childListeners.getOrPut(query) { mutableListOf() }.add(listener).also {
                Timber.d("addQuery: activating listener immediately..")
                if (active) referenceManager.getSubscription(query).subscribe(query, listener)
            }
        }

        @Synchronized
        fun addSubQuery(query: Query, listener: ValueEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            valueListeners.getOrPut(query) { mutableListOf() }.add(listener).also {
                Timber.d("addQuery: activating listener immediately..")
                if (active) referenceManager.getSubscription(query).subscribe(query, listener)
            }
        }

        @Synchronized
        fun addListener(listener: ChildEventListener) {
            addSubQuery(rootQuery, listener)
        }

        @Synchronized
        fun addListener(listener: ValueEventListener) {
            addSubQuery(rootQuery, listener)
        }

//        @Synchronized
//        fun removeQuery(subQuery: Query): T? {
//            return subscriptions.remove(subQuery.spec)?.apply { unsubscribe() }
//        }

    }
}
