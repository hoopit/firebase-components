package io.hoopit.android.ui.recyclerview

import io.reactivex.Observable

interface IAdapter<T> {
    val clicks: Observable<T>
    val longClicks: Observable<T>
    fun submit(list: List<T>?, callback: Runnable? = null)
}
