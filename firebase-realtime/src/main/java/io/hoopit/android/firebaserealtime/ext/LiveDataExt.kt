package io.hoopit.android.firebaserealtime.ext

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.util.TreeMap

inline fun <T, K, P : Comparable<P>> LiveData<List<T>>.orderByChild(
    crossinline f: (T) -> LiveData<K?>,
    crossinline t: (K) -> P
): MediatorLiveData<List<T>> {
    val med = MediatorLiveData<List<T>>()
    med.addSource(this) { list ->
        val map = TreeMap<P, T>()
        list.forEach { item ->
            val asd = f(item)
            med.removeSource(asd)
            med.addSource(asd) { subItem ->
                subItem?.let {
                    map.values.remove(item)
                    map[t(it)] = item
                    med.postValue(map.values.reversed())
                }
            }
        }
    }
    return med
}
