package io.hoopit.android.firebaserealtime.lifecycle

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import io.hoopit.android.common.livedata.DelayedDisconnectLiveData
import io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener
import io.hoopit.android.firebaserealtime.core.FirebaseCollection
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import kotlin.reflect.KClass

class FirebaseListLiveData<K : Comparable<K>, T : IFirebaseEntity>(
    private val query: Query,
    private val classModel: KClass<out T>,
    private val collection: FirebaseCollection<K, T>,
    disconnectDelay: Long
) : DelayedDisconnectLiveData<List<T>>(disconnectDelay) {

    constructor(
        query: Query,
        classModel: KClass<out T>,
        disconnectDelay: Long,
        orderKeyFunction: (T) -> K
    ) : this(
        query, classModel,
        FirebaseCollection<K, T>(
            orderKeyFunction,
            query.spec.params.isViewFromLeft
        ), disconnectDelay
    )

    private val listener = object : FirebaseChildEventListener<T>(classModel = classModel) {

        override fun cancelled(error: DatabaseError) {
            TODO("not implemented")
        }

        override fun childMoved(previousChildName: String?, child: T) {
            TODO("not implemented")
        }

        override fun childChanged(previousChildName: String?, child: T) {
            collection.update(previousChildName, child)
            postValue(collection.toList())
        }

        override fun childAdded(previousChildName: String?, child: T) {
            collection.addAfter(previousChildName, child)
            postValue(collection.toList())
        }

        override fun childRemoved(child: T) {
            collection.remove(child)
            postValue(collection.toList())
        }
    }

    override fun delayedOnActive() {
        query.addChildEventListener(listener)
    }

    override fun delayedOnInactive() {
        query.removeEventListener(listener)
    }
}
