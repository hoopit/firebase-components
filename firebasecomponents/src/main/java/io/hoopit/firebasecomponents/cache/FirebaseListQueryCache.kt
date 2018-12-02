package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import io.hoopit.firebasecomponents.paging.Listener
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : IFirebaseEntity>(
    private val connectionManager: FirebaseConnectionManager,
    val query: Query,
    private val classModel: KClass<T>,
    disconnectDelay: Long,
    orderKeyFunction: (T) -> K
) : FirebaseQueryCacheBase<K, T>(query, orderKeyFunction) {

    override fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        connectionManager.deactivate(query)
    }

    override fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        connectionManager.activate(query)
    }

    fun getListener(): Listener<T> {
        return Listener(classModel, this)
    }

    private val liveData = FirebaseCacheLiveData<List<T>>(this, query, disconnectDelay)

    fun getLiveData(): LiveData<List<T>> {
        return liveData
    }

    override fun invalidate() {
        liveData.postValue(collection.toList())
    }

}
