package io.hoopit.android.firebaserealtime.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import io.hoopit.android.common.switchMap
import java.util.TreeMap

inline fun <T, K, P : Comparable<P>> LiveData<List<T>>.orderByChildProperty(
    crossinline childGetter: (T) -> LiveData<K?>,
    crossinline childPropertyGetter: (K) -> P
): LiveData<List<T>> {
    return this.switchMap { list ->
        val liveData = MediatorLiveData<List<T>>()
        if (list.isEmpty()) {
            liveData.postValue(list)
            return@switchMap liveData
        }
        val map = TreeMap<P, T>()
        list.forEach { item ->
            val child = childGetter(item)
            liveData.removeSource(child)
            liveData.addSource(child) { subItem ->
                subItem?.let {
                    map.values.remove(item)
                    map[childPropertyGetter(it)] = item
                    liveData.postValue(map.values.reversed())
                }
            }
        }
        return@switchMap liveData
    }

//    liveData.addSource(this) { list ->
//        if (list.isEmpty()) {
//            liveData.postValue(mediatorLiveData(emptyList()))
//            return@addSource
//        }
//        val childLiveData = MediatorLiveData<List<T>>()
//        liveData.postValue(childLiveData)
//        list.forEach { item ->
//            val child = childGetter(item)
//            liveData.removeSource(child)
//            childLiveData.addSource(child) { subItem ->
//                subItem?.let {
//                    map.values.remove(item)
//                    map[childPropertyGetter(it)] = item
//                    childLiveData.postValue(map.values.reversed())
//                }
//            }
//        }
//    }
}
