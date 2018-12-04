package io.hoopit.firebasecomponents.lifecycle

import androidx.lifecycle.MediatorLiveData
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.IManagedCache
import io.hoopit.firebasecomponents.core.Scope

open class FirebaseCacheLiveData<Type>(
    private val resource: Scope.Resource,
    private val query: Query,
    private val cache: IManagedCache?
) : MediatorLiveData<Type>() {

    override fun onInactive() {
        super.onInactive()
        cache?.onInactive(this, query)
        resource.dispatchDeactivate()

    }

    override fun onActive() {
        super.onActive()
        cache?.onActive(this, query)
        resource.dispatchActivate()
    }
}
