package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

class FirebaseReferenceManager {

    companion object {
        val defaultInstance = FirebaseReferenceManager()
    }

    private val references = ConcurrentHashMap<QuerySpec, FirebaseReference>()

    fun getReference(query: Query): FirebaseReference {
        return references.getOrPut(query.spec) { FirebaseReference(querySpec = query.spec) }
    }

    class FirebaseReference(private val querySpec: QuerySpec) {

        // TODO: refactor
        private val childEventSubs = mutableMapOf<Query, MutableList<ChildEventListener>>()
        private var numChildEventSubs = 0

        private val valueEventSubs = mutableMapOf<Query, MutableList<ValueEventListener>>()
        private val singleValueEventSubs = mutableMapOf<Query, MutableList<ValueEventListener>>()
        private var numValueEventSubs = 0

        @Synchronized
        fun subscribe(query: Query, vararg listeners: ChildEventListener) {
            Timber.d("called: subscribe ChildEventListener: ${query.spec}")
            numChildEventSubs = subscribeInternal(query, childEventSubs, numChildEventSubs, *listeners) {
                query.addChildEventListener(childListener)
            }
        }

        @Synchronized
        fun subscribe(query: Query, vararg listeners: ValueEventListener) {
            Timber.d("called: subscribe ValueEventListener: ${query.spec}")
            numValueEventSubs = subscribeInternal(query, valueEventSubs, numValueEventSubs, *listeners) {
                query.addValueEventListener(valueListener)
            }
        }

        @Synchronized
        fun subscribeSingle(query: Query, vararg listeners: ValueEventListener) {
            Timber.d("called: subscribeSingle: ${query.spec}")
            numValueEventSubs = subscribeInternal(query, singleValueEventSubs, numValueEventSubs, *listeners) {
                query.addListenerForSingleValueEvent(valueListener)
            }
        }

        @Synchronized
        fun unsubscribe(query: Query, vararg listeners: ValueEventListener) {
            numValueEventSubs = unsubscribeInternal(query, valueEventSubs, numValueEventSubs, *listeners) {
                query.removeEventListener(valueListener)
            }
            numValueEventSubs = unsubscribeInternal(query, singleValueEventSubs, numValueEventSubs, *listeners) {
                query.removeEventListener(valueListener)
            }
        }

        @Synchronized
        fun unsubscribe(query: Query, vararg listeners: ChildEventListener) {
            numChildEventSubs = unsubscribeInternal(query, childEventSubs, numChildEventSubs, *listeners) {
                query.removeEventListener(childListener)
            }
        }

        private fun unsubscribleSingleValueListener() {
        }

        @Synchronized
        private inline fun <T> subscribeInternal(
            query: Query,
            map: MutableMap<Query, MutableList<T>>,
            currentSubs: Int,
            vararg listeners: T,
            activate: () -> Unit
        ): Int {
            assert(querySpec == query.spec) { "Can not subscribe to a Query with different QuerySpec." }
            assert(!map.contains(query)) { "Adding multiple listeners on the same Query is not yet supported. Consider creating a new Query." }
            val list = map.getOrPut(query) { mutableListOf() }
            val added = list.addAll(listeners)
            if (added && currentSubs == 0) {
                Timber.d("called: subscribeInternal: activating: $querySpec")
                activate()
            }
            return currentSubs + listeners.size
        }

        @Synchronized
        private inline fun <T> unsubscribeInternal(
            query: Query,
            map: MutableMap<Query, MutableList<T>>,
            count: Int,
            vararg listeners: T,
            deactivate: () -> Unit
        ): Int {
            if (count == 0) return count
            assert(querySpec == query.spec) { "Can not unsubscribe from a Query with different QuerySpec." }
            var newCount = count
            listeners.forEach {
                val removed = map[query]?.remove(it)
                if (removed == true) --newCount
            }
            if (newCount == 0) {
//                Timber.d("called: unsubscribeInternal: deactivating: $querySpec")
                deactivate()
            } else if (newCount < 0)
                throw IllegalStateException("Attempting to unsub with 0 subs: $querySpec")
            return newCount
        }

        private val childListener = object : ChildEventListener {

            override fun onCancelled(error: DatabaseError) {
                childEventSubs.values.forEach { list -> list.forEach { it.onCancelled(error) } }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { list -> list.forEach { it.onChildMoved(snapshot, previousChildKey) } }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { list -> list.forEach { it.onChildChanged(snapshot, previousChildKey) } }
            }

            override fun onChildAdded(snapshot: DataSnapshot, previousChildKey: String?) {
                childEventSubs.values.forEach { list -> list.forEach { it.onChildAdded(snapshot, previousChildKey) } }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                childEventSubs.values.forEach { list -> list.forEach { it.onChildRemoved(snapshot) } }
            }
        }

        private val valueListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                valueEventSubs.values.forEach { it.forEach { it.onCancelled(p0) } }
            }

            override fun onDataChange(p0: DataSnapshot) {
                valueEventSubs.values.forEach { it.forEach { it.onDataChange(p0) } }
                for ((key, it) in singleValueEventSubs) {
                    it.forEach {
                        it.onDataChange(p0)
                        numValueEventSubs = unsubscribeInternal(
                            key,
                            singleValueEventSubs,
                            numValueEventSubs,
                            it
                        ) { key.removeEventListener(this) }
                    }
                }
            }
        }
    }
}
