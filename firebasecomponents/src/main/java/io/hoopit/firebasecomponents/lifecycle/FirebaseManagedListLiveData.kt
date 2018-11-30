package io.hoopit.firebasecomponents.lifecycle

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager

/**
 * [DelayedTransitionLiveData] that manages its connections through a FirebaseConnectionManager.
 * Useful for resources that attach to multiple sub-queries.
 * It will manage connection and disconnection for all related sub-queries that have been
 * added to the manager.
 */
open class FirebaseManagedWrapperLiveData<Type>(
    private val query: Query,
    private val firebaseConnectionManager: FirebaseConnectionManager,
    disconnectDelay: Long
) : DelayedTransitionLiveData<Type>(disconnectDelay) {
    final override fun delayedOnInactive() = firebaseConnectionManager.deactivate(query)

    final override fun delayedOnActive() = firebaseConnectionManager.activate(query)
}
