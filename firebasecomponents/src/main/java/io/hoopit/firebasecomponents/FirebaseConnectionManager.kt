package io.hoopit.firebasecomponents

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener

object FirebaseConnectionManager {

    private data class FirebaseListener private constructor(
        private val query: Query,
        private val childEventListener: ChildEventListener?,
        private val valueEventListener: ValueEventListener?,
        private val single: Boolean
    ) {

        constructor(baseQuery: Query, childEventListener: ChildEventListener) : this(
            baseQuery,
            childEventListener,
            null,
            false
        )

        constructor(
            baseQuery: Query,
            childEventListener: ValueEventListener,
            single: Boolean = false
        ) : this(baseQuery, null, childEventListener, single)

        fun on() {
            childEventListener?.let { query.addChildEventListener(it) }
            valueEventListener?.let {
                if (single) query.addListenerForSingleValueEvent(it)
                else query.addValueEventListener(it)
            }
        }

        fun off() {
            childEventListener?.let { query.removeEventListener(it) }
            valueEventListener?.let { query.removeEventListener(it) }
        }
    }

    private data class FirebaseSubscription constructor(
        private val baseQuery: Query
    ) {

        private val listeners = mutableListOf<FirebaseListener>()

        private var active = false

        fun addListener(listener: FirebaseListener) {
            listeners.add(listener)
            if (active) listener.on()
        }

        fun removeListener(listener: FirebaseListener) {
            listeners.remove(listener)
            listener.off()
        }

        fun subscribe() {
            active = true
            attachListeners()
        }

        fun unsubscribe() {
            active = false
            detachListeners()
        }

        private fun attachListeners() {
            listeners.forEach { it.on() }
        }

        private fun detachListeners() {
            listeners.forEach { it.off() }
        }
    }

    private val eventListeners = mutableMapOf<Query, FirebaseSubscription>()

    fun addListener(query: Query, subQuery: Query, listener: ChildEventListener) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }
            .addListener(FirebaseListener(subQuery, listener))
    }

    fun addListener(query: Query, subQuery: Query, listener: ValueEventListener) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }
            .addListener(FirebaseListener(subQuery, listener))
    }

    fun addListener(query: Query, listener: ChildEventListener) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }
            .addListener(FirebaseListener(query, listener))
    }

    fun addListener(query: Query, listener: ValueEventListener, once: Boolean = false) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }
            .addListener(FirebaseListener(query, listener, once))
    }

    fun activate(query: Query) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }.subscribe()
    }

    fun deactivate(query: Query) {
        eventListeners.getOrPut(query) { FirebaseSubscription(query) }.unsubscribe()
    }
}
