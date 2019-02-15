package io.hoopit.android.ui.widgets

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import io.hoopit.android.common.extensions.getStringOrDefault
import io.hoopit.android.ui.NetworkState

open class NetworkStateToastNotifier(
    private val networkState: LiveData<NetworkState>,
    private val context: Context,
    private val success: Int? = null,
    private val errorRes: Int? = null,
    private val loadingIndicator: View? = null,
    private val view: View? = null
) : Observer<NetworkState> {

    override fun onChanged(it: NetworkState?) {
        it?.let { (status, msg, stringRes) ->
            when (status) {
                NetworkState.Status.SUCCESS -> {
                    success?.let { showSuccess(it) }
                    networkState.removeObserver(this)
                }
                NetworkState.Status.FAILED -> {

                    val errorMsg = (errorRes ?: stringRes)?.let { context.getStringOrDefault(it, msg) }
                    showError(errorMsg)
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

    protected open fun showError(error: String?) {
        if (error == null) return
        if (view != null) Snackbar.make(view, error, Snackbar.LENGTH_LONG).show()
        else Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
    }

    protected open fun showSuccess(@StringRes resId: Int) {
        loadingIndicator?.visibility = View.GONE
        Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
    }

    protected open fun showLoading() {
        loadingIndicator?.visibility = View.VISIBLE
    }
}
