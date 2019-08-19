package io.hoopit.android.ui

import android.content.Context
import androidx.annotation.StringRes
import io.hoopit.android.common.extensions.getStringOrDefault

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
    val status: Status,
    val msg: String? = null,
    @StringRes val stringRes: Int? = null,
    val data: Map<String, *>? = null
) {

    fun getErrorMessage(context: Context): String? {
        return stringRes?.let { context.getStringOrDefault(it, msg) } ?: msg
    }

    companion object {
        val success = NetworkState(Status.SUCCESS)
        val loading = NetworkState(Status.RUNNING)

        fun error(msg: String?, data: Map<String, *>? = null) = NetworkState(
            Status.FAILED,
            msg = msg,
            data = data
        )

        fun error(@StringRes stringRes: Int?, data: Map<String, *>? = null) = NetworkState(
            Status.FAILED,
            stringRes = stringRes,
            data = data
        )

        fun error(msg: String?, @StringRes stringRes: Int?, data: Map<String, *>? = null) =
            NetworkState(
                Status.FAILED,
                msg = msg,
                stringRes = stringRes,
                data = data
            )
    }

    enum class Status {
        RUNNING,
        SUCCESS,
        FAILED
    }
}
