package io.hoopit.android.ui

import android.content.Context
import androidx.annotation.StringRes
import io.hoopit.android.common.extensions.getStringOrDefault

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
    val status: Status,
    val msg: String? = null,
    @StringRes val stringRes: Int? = null
) {

    fun getErrorMessage(context: Context): String? {
        return stringRes?.let { context.getStringOrDefault(it, msg) } ?: msg
    }

    companion object {
        val success = NetworkState(Status.SUCCESS)
        val loading = NetworkState(Status.RUNNING)

        fun error(msg: String?) = NetworkState(
            Status.FAILED,
            msg = msg
        )

        fun error(@StringRes stringRes: Int?) = NetworkState(
            Status.FAILED,
            stringRes = stringRes
        )

        fun error(msg: String?, @StringRes stringRes: Int?) = NetworkState(
            Status.FAILED,
            msg = msg,
            stringRes = stringRes

        )
    }

    enum class Status {
        RUNNING,
        SUCCESS,
        FAILED
    }
}
