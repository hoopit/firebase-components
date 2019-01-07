package io.ezet.android.ui.widgets

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.ezet.android.ui.NetworkState

open class NetworkStateToastNotifier(
    private val networkState: LiveData<NetworkState>,
    private val context: Context,
    private val success: Int? = null,
    private val error: Int? = null,
    private val loadingIndicator: View? = null
) : Observer<NetworkState> {

    override fun onChanged(it: NetworkState?) {
        it?.let {
            when (it.status) {
                NetworkState.Status.SUCCESS -> {
                    success?.let { showSuccess(it) }
                    networkState.removeObserver(this)
                }
                NetworkState.Status.FAILED -> {
                    val errorRes = error ?: it.stringRes
                    if (errorRes != null)
                        showError(errorRes)
                    loadingIndicator?.visibility = View.GONE
                    networkState.removeObserver(this)
                }
                NetworkState.Status.RUNNING -> {
                    showLoading()
                }
            }
        }
    }

    fun observe(lifecycleOwner: LifecycleOwner) {
        networkState.observe(lifecycleOwner, this)
    }

    protected open fun showError(@StringRes resId: Int) {
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    protected open fun showSuccess(@StringRes resId: Int) {
        loadingIndicator?.visibility = View.GONE
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    protected open fun showLoading() {
        loadingIndicator?.visibility = View.VISIBLE
    }
}
