@file:Suppress("TooManyFunctions")

package io.hoopit.android.common

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations

/**
 * Extension wrapper for [LiveData.observe]
 */
fun <T> LiveData<T>.observe(owner: LifecycleOwner, observer: (T) -> Unit) =
    observe(owner, Observer<T> { v -> observer.invoke(v) })

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstNotNull(owner: LifecycleOwner, observer: (T) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        if (t != null) {
            observer.invoke(t)
            removeObserver(requireNotNull(observerWrapper))
        }
    }
    observe(owner, observerWrapper)
}

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirst(owner: LifecycleOwner, observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        observer.invoke(t)
        removeObserver(requireNotNull(observerWrapper))
    }
    observe(owner, observerWrapper)
}

/**
 * Observes the first non-null value and then removes the observer
 */
fun <T> LiveData<T>.observeFirstNotNullForever(observer: (T?) -> Unit) {
    var observerWrapper: Observer<T>? = null
    observerWrapper = Observer { t ->
        if (t != null) {
            observer.invoke(t)
            removeObserver(requireNotNull(observerWrapper))
        }
    }
    observeForever(observerWrapper)
}

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

/**
 * Extension wrapper for [Transformations.switchMap]
 */
fun <X, Y> LiveData<X>.switchMap(func: (X) -> LiveData<Y>?): LiveData<Y> = Transformations.switchMap(this, func)

/**
 * Extension wrapper for [Transformations.map]
 */
fun <X, Y> LiveData<X>.map(func: (X) -> Y): LiveData<Y> = Transformations.map(this, func)

/**
 * Extension wrapper for [Transformations.map]
 */
inline fun <X, Y> LiveData<X>.mapUpdate(crossinline func: (X) -> Y): LiveData<Y> {
    val result = MediatorLiveData<Y>()
    result.addSource(this) { x -> result.update(func(x)) }
    return result
}

/**
 * Extension wrapper for [LiveDataReactiveStreams.toPublisher]
 */
//fun <T> LiveData<T>.toPublisher(lifecycleOwner: LifecycleOwner) = LiveDataReactiveStreams.toPublisher(lifecycleOwner, this)

fun <T> MutableLiveData<T>.update(newValue: T) {
    if (this.value != newValue)
        this.value = newValue
}

fun <T> MutableLiveData<T>.postUpdate(newValue: T) {
    if (this.value != newValue)
        this.postValue(newValue)
}

fun <T> liveData(value: T?): LiveData<T> {
    return mutableLiveData(value)
}

fun <T> noLiveData(): LiveData<T> {
    return NoLiveData()
}

fun <T> mutableLiveData(newValue: T): MutableLiveData<T> {
    val data = MutableLiveData<T>()
    data.value = newValue
    return data
}

fun <T> mediatorLiveData(newValue: T?): MediatorLiveData<T> {
    val data = MediatorLiveData<T>()
    data.postValue(newValue)
    return data
}

fun <TSOURCE, TOUT> mediatorLiveDataUpdate(
    source: LiveData<TSOURCE>,
    onChanged: (TSOURCE?) -> TOUT
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    liveData.addSource(source) {
        liveData.postValue(onChanged(it))
    }
    return liveData
}

fun <TSOURCE, TOUT> mediatorLiveData(
    source: LiveData<TSOURCE>,
    onChanged: MediatorLiveData<TOUT>.(TSOURCE?) -> Unit
): MediatorLiveData<TOUT> {
    val liveData = MediatorLiveData<TOUT>()
    liveData.addSource(source) { onChanged(liveData, it) }
    return liveData
}

fun <T> MediatorLiveData<T>.reObserveFirst(src: LiveData<T?>, onChanged: (T?) -> Unit) {
    this.removeSource(src)
    this.addSource(src) {
        this.removeSource(src)
        onChanged(it)
    }
}

fun <T> LiveData<T>.filter(predicate: (T?) -> Boolean): MediatorLiveData<T> {
    return mediatorLiveData(this) {
        if (predicate(it)) {
            this.postValue(it)
        }
    }
}
