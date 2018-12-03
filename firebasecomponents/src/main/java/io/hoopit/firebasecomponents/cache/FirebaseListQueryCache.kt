package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import io.hoopit.firebasecomponents.paging.QueryCacheListener
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : ManagedFirebaseEntity>(
    connectionManager: FirebaseConnectionManager,
    query: Query,
    private val clazz: KClass<T>,
    disconnectDelay: Long,
    orderKeyFunction: (T) -> K
) : FirebaseManagedQueryCache<K, T>(connectionManager, query, orderKeyFunction) {

    private val liveData = FirebaseCacheLiveData<List<T>>(this, query, disconnectDelay)

    fun getLiveData() = liveData as LiveData<List<T>>

    fun getListener() = QueryCacheListener(clazz, this)

    override fun invalidate() = liveData.postValue(collection.toList())

}
