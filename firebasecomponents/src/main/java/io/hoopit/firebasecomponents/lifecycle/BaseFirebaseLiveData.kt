package io.hoopit.firebasecomponents.lifecycle

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MediatorLiveData

abstract class BaseFirebaseLiveData<T> : MediatorLiveData<T>() {

    private val handler = Handler(Looper.getMainLooper())

    private var pendingRemoval = false

    override fun onInactive() {
        super.onInactive()
        pendingRemoval = true
        handler.postDelayed(listener, 2000)
    }

    override fun onActive() {
        super.onActive()
        handler.removeCallbacks(listener)
        if (!pendingRemoval) addListener()
        pendingRemoval = false
    }

    private val listener = Runnable {
        removeListener()
        pendingRemoval = false
    }

    abstract fun removeListener()

    abstract fun addListener()
}
