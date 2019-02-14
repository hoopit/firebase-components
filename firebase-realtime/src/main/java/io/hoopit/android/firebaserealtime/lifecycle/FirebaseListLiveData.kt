package io.hoopit.android.firebaserealtime.lifecycle

import com.google.firebase.database.Query
import io.hoopit.android.common.livedata.DelayedDisconnectLiveData
import io.hoopit.android.firebaserealtime.core.CustomFirebaseCollection
import io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import kotlin.reflect.KClass

class FirebaseListLiveData<T : IFirebaseEntity>(
    private val query: Query,
    private val clazz: KClass<T>,
    disconnectDelay: Long
) : DelayedDisconnectLiveData<List<T>>(disconnectDelay) {

    private val collection = CustomFirebaseCollection<T>()

    private val descending = query.spec.params.hasAnchoredLimit() && !query.spec.params.isViewFromLeft

    private fun invalidated() {
        val items = collection.getRange(0, collection.size)
        postValue(items)
    }

    init {
        collection.setInvalidationListener(this::invalidated, removeAfterInvalidate = false)
    }

    private val listener = object : FirebaseChildEventListener<T>(clazz) {

        override fun childMoved(previousChildName: String?, child: T) {
            collection.move(previousChildName, child, descending)
        }

        override fun childChanged(previousChildName: String?, child: T) {
            collection.update(previousChildName, child, descending)
        }

        override fun childAdded(previousChildName: String?, child: T) {
            collection.add(previousChildName, child, descending)
        }

        override fun childRemoved(child: T) {
            collection.remove(child)
        }
    }

    override fun delayedOnActive() {
        query.addChildEventListener(listener)
    }

    override fun delayedOnInactive() {
        query.removeEventListener(listener)
        collection.clear(invalidate = false)
    }
}
