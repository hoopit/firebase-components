package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseChildEventListener
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseManagedWrapperLiveData
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : IFirebaseEntity>(
    query: Query,
    classModel: KClass<T>,
    disconnectDelay: Long,
    connectionManager: FirebaseConnectionManager,
    orderKeyFunction: (T) -> K
) : FirebaseQueryCacheBase<K, T>(query, orderKeyFunction) {

    private val listener = object : FirebaseChildEventListener<T>(classModel = classModel) {

        override fun cancelled(error: DatabaseError) {
            TODO("not implemented")
        }

        override fun childMoved(previousChildName: String?, child: T) {
            TODO("not implemented")
        }

        override fun childChanged(previousChildName: String?, child: T) {
            collection.update(previousChildName, child)
            invalidate()
        }

        override fun childAdded(previousChildName: String?, child: T) {
            collection.addAfter(previousChildName, child)
            invalidate()
        }

        override fun childRemoved(child: T) {
            collection.remove(child)
            invalidate()
        }
    }

    init {
        connectionManager.addListener(query, listener)
    }

    // TODO: Maybe use a simpler/faster backing collection type?

    private val liveData = FirebaseManagedWrapperLiveData<List<T>>(query, connectionManager, disconnectDelay)

    fun getLiveData(): LiveData<List<T>> {
        return liveData
    }

    override fun invalidate() {
        liveData.postValue(collection.toList())
    }

}
