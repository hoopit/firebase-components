package io.hoopit.firebasecomponents.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

fun <IN, OUT> LiveData<IN>.switchMap(mapFunction: (IN) -> LiveData<OUT>): LiveData<OUT> {
    return Transformations.switchMap(this, mapFunction)
}
