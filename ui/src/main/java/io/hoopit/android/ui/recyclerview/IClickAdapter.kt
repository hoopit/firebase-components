package io.hoopit.android.ui.recyclerview

import io.reactivex.Observable

interface IClickAdapter<T> {
    val clicks: Observable<T>
    val longClicks: Observable<T>
}
