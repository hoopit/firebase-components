package io.hoopit.firebasecomponents.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MediatorLiveData

/**
 * A [MediatorLiveData] that delays it's transition to inactive state by a configurable amount.
 */
abstract class DelayedDisconnectLiveData<T>(private val disconnectDelay: Long) : MediatorLiveData<T>() {

    private val handler = Handler(Looper.getMainLooper())

    private var pendingRemoval = false

    final override fun onInactive() {
        super.onInactive()
        pendingRemoval = true
        handler.postDelayed(listener, disconnectDelay)
    }

    final override fun onActive() {
        super.onActive()
        handler.removeCallbacks(listener)
        if (!pendingRemoval) delayedOnActive()
        pendingRemoval = false
    }

    private val listener = Runnable {
        delayedOnInactive()
        pendingRemoval = false
    }

    /**
     * The delayed OnInactive call
     */
    abstract fun delayedOnInactive()

    /**
     * The delayed OnActive call
     */
    abstract fun delayedOnActive()
}
