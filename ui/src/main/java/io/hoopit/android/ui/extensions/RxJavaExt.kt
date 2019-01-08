package io.hoopit.android.ui.extensions

import android.view.View
import com.jakewharton.rxbinding3.view.clicks
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

const val DEFAULT_THROTTLE_MS = 500L
const val DEFAULT_SHORT_THROTTLE_MS = 100L

/**
 * Throttles clicks by disabling interaction for a duration specified in ms
 * @return: an observable stream of clicks
 */
fun View.throttleClicks(ms: Long = DEFAULT_THROTTLE_MS): Observable<Unit> {
    return this.clicks().throttleFirst(ms, TimeUnit.MILLISECONDS)
}

/**
 * Throttles clicks by disabling interaction for a duration specified in ms
 * @return: an observable stream of clicks
 */
fun <T> Observable<T>.throttle(ms: Long = DEFAULT_THROTTLE_MS): Observable<T> {
    return this.throttleFirst(ms, TimeUnit.MILLISECONDS)
}
