package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

class FirebaseListQueryCache<K : Comparable<K>, T : ManagedFirebaseEntity>(
    scope: Scope,
    query: Query,
    clazz: KClass<T>,
    orderKeyFunction: (T) -> K
) : FirebaseManagedQueryCache<K, T>(scope, query, clazz, orderKeyFunction) {

    private val liveData = FirebaseCacheLiveData<List<T>>(scope.getScope(query), query, this)

    fun getLiveData() = liveData as LiveData<List<T>>

    override fun invalidate() = liveData.postValue(collection.toList())

}
