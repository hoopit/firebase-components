package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebaseListCache<K : Comparable<K>, T : FirebaseScopedResource>(
    private val firebaseScope: FirebaseScope,
    query: Query,
    clazz: KClass<T>,
    orderKeyFunction: (T) -> K
) : FirebaseManagedCollectionCache<K, T>(
    firebaseScope,
    query,
    !query.spec.params.isViewFromLeft,
    clazz,
    orderKeyFunction
) {

    private var liveData: MutableLiveData<List<T>>? = null

    fun getLiveData(
        disconnectDelay: Long,
        resource: FirebaseScope.Resource = firebaseScope.getResource(query)
    ): LiveData<List<T>> {
        if (liveData == null) {
            liveData =
                FirebaseCacheLiveData<List<T>>(
                    firebaseScope.getResource(query), query, this, disconnectDelay
                ).also {
                    if (resource.rootQuery == query) resource.addListener(getChildListener())
                }
        }
        return requireNotNull(liveData)
    }

    override fun invalidate() {
        liveData?.postValue(collection.toList())
    }
}
