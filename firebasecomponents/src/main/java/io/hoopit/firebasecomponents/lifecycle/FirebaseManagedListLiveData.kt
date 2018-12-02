package io.hoopit.firebasecomponents.lifecycle

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseQueryCacheBase

/**
 * [DelayedDisconnectLiveData] that manages its connections through a FirebaseConnectionManager.
 * Useful for resources that attach to multiple sub-queries.
 * It will manage connection and disconnection for itself and all related sub-queries that have been
 * added to the manager.
 */
open class FirebaseCacheLiveData<Type>(
    private val cache: FirebaseQueryCacheBase<*, *>,
    private val query: Query,
    disconnectDelay: Long
) : DelayedDisconnectLiveData<Type>(disconnectDelay) {
    final override fun delayedOnInactive() = cache.onInActive(this, query)

    final override fun delayedOnActive() = cache.onActive(this, query)
}
