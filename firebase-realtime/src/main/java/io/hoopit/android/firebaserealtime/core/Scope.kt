package io.hoopit.android.firebaserealtime.core

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

    lateinit var cache: FirebaseCache

    companion object {
        val defaultInstance = Scope(FirebaseReferenceManager()).also {
            it.cache = FirebaseCache(it)
        }
    }

    private val scopes = ConcurrentHashMap<Query, Resource>()

    @Synchronized
    fun getResource(query: Query, defaultDisconnectDelay: Long = 10000): Resource {
        return scopes.getOrPut(query) { Resource(query, defaultDisconnectDelay) }
    }

    @Synchronized
    private fun activate(query: Query) {
        getResource(query).dispatchActivate()
    }

    @Synchronized
    private fun deactivate(query: Query, disconnectDelay: Long? = null) {
        if (disconnectDelay != null) scopes[query]?.dispatchDeactivate(disconnectDelay)
        else scopes[query]?.dispatchDeactivate()
    }

    private fun onDeactivate(query: Query) {
        cache.dispose(query.spec)
    }

    inner class Resource constructor(
        val rootQuery: Query,
        private val disconnectDelay: Long
    ) {
        private val childListeners = ConcurrentHashMap<Query, MutableList<ChildEventListener>>()
        private val valueListeners = ConcurrentHashMap<Query, MutableList<ValueEventListener>>()
        private val singleValueListeners = ConcurrentHashMap<Query, MutableList<ValueEventListener>>()
        private var active = false

        private val handler = Handler(Looper.getMainLooper())

        private var pendingRemoval = false

        private val listener = Runnable {
            deactivate()
            pendingRemoval = false
        }

        @Synchronized
        fun dispatchActivate() {
            handler.removeCallbacks(listener)
            if (!pendingRemoval) activate()
            pendingRemoval = false
        }

        @Synchronized
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
//                Timber.d("Adding listeners for: ${rootQuery.spec}")
                active = true
                for ((query, listeners) in childListeners) {
                    referenceManager.getReference(query).subscribe(query, *listeners.toTypedArray())
                    if (rootQuery != query) activate(query)
                }
                for ((query, listeners) in valueListeners) {
                    referenceManager.getReference(query).subscribe(query, *listeners.toTypedArray())
                    if (rootQuery != query) activate(query)
                }
                for ((query, listeners) in singleValueListeners) {
                    referenceManager.getReference(query).subscribeSingle(query, *listeners.toTypedArray())
                    if (rootQuery != query) activate(query)
                }
            }
        }

        @Synchronized
        private fun deactivate() {
            onDeactivate(rootQuery)
            if (active) {
//                Timber.d("Removing listeners for: ${rootQuery.spec}")
                active = false
                for ((query, listeners) in childListeners) {
                    referenceManager.getReference(query).unsubscribe(query, *listeners.toTypedArray())
                    if (rootQuery != query) deactivate(query)
                }
                for ((query, listeners) in valueListeners) {
                    referenceManager.getReference(query).unsubscribe(query, *listeners.toTypedArray())
                    if (rootQuery != query) deactivate(query)
                }
                for ((query, listeners) in singleValueListeners) {
                    referenceManager.getReference(query).unsubscribe(query, *listeners.toTypedArray())
                    if (rootQuery != query) deactivate(query)
                }
            }
        }

        @Synchronized
        fun addSubQuery(query: Query, listener: ChildEventListener) {
            Timber.d("called: addQuery: ${query.spec}")
            childListeners.getOrPut(query) { mutableListOf() }.add(listener).also {
                if (active) {
                    Timber.d("addQuery: activating listener immediately..")
                    referenceManager.getReference(query).subscribe(query, listener)
                    activate(query)
                }
            }
        }

        @Synchronized
        fun addSubQuery(query: Query, listener: ValueEventListener, once: Boolean = false) {
            Timber.d("called: addQuery: ${query.spec}")
            if (once) singleValueListeners.getOrPut(query) { mutableListOf() }.add(listener)
            else valueListeners.getOrPut(query) { mutableListOf() }.add(listener)
            if (active) {
                Timber.d("addQuery: activating listener immediately..")
                if (once)
                    referenceManager.getReference(query).subscribeSingle(query, listener)
                else
                    referenceManager.getReference(query).subscribe(query, listener)
                activate(query)
            }
        }

        fun addListener(listener: ChildEventListener) {
            addSubQuery(rootQuery, listener)
        }

        fun addListener(listener: ValueEventListener, once: Boolean = false) {
            addSubQuery(rootQuery, listener, once)
        }

//        @Synchronized
//        fun removeQuery(subQuery: Query): T? {
//            return subscriptions.remove(subQuery.spec)?.apply { unsubscribe() }
//        }

    }
}
