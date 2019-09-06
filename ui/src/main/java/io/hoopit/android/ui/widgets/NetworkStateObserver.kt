package io.hoopit.android.ui.widgets

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.hoopit.android.ui.NetworkState

abstract class NetworkStateObserver(
    private val networkState: LiveData<NetworkState>,
    private val removeOnFailed: Boolean = true
) : Observer<NetworkState> {

    final override fun onChanged(it: NetworkState?) {
        if (it == null) return
        when (it.status) {
            NetworkState.Status.SUCCESS -> {
                networkState.removeObserver(this)
                onSuccess(it)
            }
            NetworkState.Status.FAILED -> {
                if (removeOnFailed) networkState.removeObserver(this)
                onError(it)
            }
            NetworkState.Status.RUNNING -> {
                onLoading(it)
            }
        }
    }

    open fun onSuccess(networkState: NetworkState) {}
    open fun onError(networkState: NetworkState) {}
    open fun onLoading(networkState: NetworkState) {}

    fun observe(lifecycleOwner: LifecycleOwner) {
        networkState.observe(lifecycleOwner, this)
    }
}
