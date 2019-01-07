package io.ezet.android.ui

import androidx.annotation.StringRes

@Suppress("DataClassPrivateConstructor")
data class NetworkState private constructor(
    val status: Status,
    val msg: String? = null,
    @StringRes val stringRes: Int? = null
) {
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
