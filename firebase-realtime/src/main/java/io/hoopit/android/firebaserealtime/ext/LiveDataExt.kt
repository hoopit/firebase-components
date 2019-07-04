package io.hoopit.android.firebaserealtime.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.util.TreeMap

inline fun <T, K, P : Comparable<P>> LiveData<List<T>>.orderByChildProperty(
    crossinline childGetter: (T) -> LiveData<K?>,
    crossinline propertyGetter: (K) -> P
): MediatorLiveData<List<T>> {
    val med = MediatorLiveData<List<T>>()
    med.addSource(this) { list ->
        if (list.isEmpty()) med.postValue(emptyList())
        val map = TreeMap<P, T>()
        list.forEach { item ->
            val asd = childGetter(item)
            med.removeSource(asd)
            med.addSource(asd) { subItem ->
                subItem?.let {
                    map.values.remove(item)
                    map[propertyGetter(it)] = item
                    med.postValue(map.values.reversed())
                }
            }
        }
    }
    return med
}
