package io.hoopit.firebasecomponents.lifecycle

import androidx.lifecycle.MediatorLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.IManagedCache
import io.hoopit.firebasecomponents.core.Scope

open class FirebaseCacheLiveData<Type>(
    private val resource: Scope.Resource,
    private val query: Query,
    private val cache: IManagedCache?,
    private val disconnectDelay: Long
) : MediatorLiveData<Type>() {

    override fun onInactive() {
        super.onInactive()
        cache?.onInactive(this, query)
        resource.dispatchDeactivate(disconnectDelay)

    }

    override fun onActive() {
        super.onActive()
        cache?.onActive(this, query)
        resource.dispatchActivate()
    }
}
