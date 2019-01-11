package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : io.hoopit.android.firebaserealtime.core.FirebaseResource>(
    private val scope: io.hoopit.android.firebaserealtime.core.Scope,
    query: Query,
    clazz: KClass<T>,
    orderKeyFunction: (T) -> K
) : io.hoopit.android.firebaserealtime.cache.FirebaseManagedQueryCache<K, T>(scope, query, clazz, orderKeyFunction) {

    private var liveData: MutableLiveData<List<T>>? = null

    fun getLiveData(
        query: Query,
        disconnectDelay: Long,
        resource: io.hoopit.android.firebaserealtime.core.Scope.Resource = scope.getResource(query)
    ): LiveData<List<T>> {
        if (liveData == null) {
            liveData = FirebaseCacheLiveData<List<T>>(scope.getResource(query), query, this, disconnectDelay).also {
                if (resource.rootQuery == query) resource.addListener(getChildListener())
            }
        }
        return requireNotNull(liveData)
    }

    override fun invalidate() {
        liveData?.postValue(collection.toList())
    }
}
