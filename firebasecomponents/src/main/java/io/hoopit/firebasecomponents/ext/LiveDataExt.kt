package io.hoopit.firebasecomponents.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

fun <T> noLiveData(): LiveData<T> {
    return MutableLiveData<T>()
}

fun <T> liveData(singleValue: T): LiveData<T> {
    return MutableLiveData<T>().apply {
        postValue(singleValue)
    }
}

fun <T> mutableLiveData(initialValue: T): MutableLiveData<T> {
    return MutableLiveData<T>().apply {
        postValue(initialValue)
    }
}

fun <IN, OUT> LiveData<IN>.map(mapFunction: (IN) -> OUT): LiveData<OUT> {
    return Transformations.map(this, mapFunction)
}

/**
 * Extension wrapper for [Transformations.switchMap]
 */
fun <X, Y> LiveData<X>.switchMap(func: (X) -> LiveData<Y>): LiveData<Y> =
        Transformations.switchMap(this, func)


/**
 * Observes the first value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstForever(observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        observer.invoke(t)
        removeObserver(requireNotNull(observerWrapper))
    }
    observeForever(observerWrapper)
}
