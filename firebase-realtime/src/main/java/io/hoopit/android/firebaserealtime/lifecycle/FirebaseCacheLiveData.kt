package io.hoopit.android.firebaserealtime.lifecycle

import androidx.lifecycle.MediatorLiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.cache.IManagedCache
import io.hoopit.android.firebaserealtime.core.FirebaseScope

open class FirebaseCacheLiveData<Type>(
    private val resource: FirebaseScope.Resource,
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
