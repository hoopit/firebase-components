package io.hoopit.android.firebaserealtime.core

import java.util.concurrent.ConcurrentSkipListMap

class FirebaseCollection<K : Comparable<K>, V : IFirebaseEntity>
private constructor(
    private val map: ConcurrentSkipListMap<K, V>,
    private val ascending: Boolean,
    private val orderKeyFunction: (V) -> K
) : Collection<V> by map.values {

    constructor(
        orderKeyFunction: (V) -> K,
        ascending: Boolean
    ) : this(ConcurrentSkipListMap { o1, o2 ->
        if (ascending) o1.compareTo(o2)
        else o2.compareTo(o1)
    }, ascending, orderKeyFunction)

    fun getAround(key: K?, limit: Int): List<V> {
        val list = key?.let { map.tailMap(it, true).values } ?: map.values
        return list.take(limit).toList()
    }

    fun getAfter(key: K, limit: Int): List<V> {
        val list = key.let { map.tailMap(it, false).values }
        return list.take(limit).toList()
    }

    fun getBefore(key: K, limit: Int): List<V> {
        val list = key.let { map.headMap(it, false).values }
        return list.toList().takeLast(limit)
    }

    fun addAfter(id: String?, item: V) {
        map[orderKeyFunction(item)] = item
    }

    fun update(previousItemId: String?, item: V) {
//        val oldValue = map.replace(orderKeyFunction(item), item)
//        if (oldValue != null) return
        if (previousItemId == null) {
            map.remove(map.firstKey())
            map[orderKeyFunction(item)] = item
        } else {
            val currentKey = map.entries.first { (_, value) -> value.entityId == previousItemId }.key
            val nextKey = map.lowerKey(currentKey)
            if (nextKey == null) {
                throw IllegalStateException("nextKey cannot be null")
            } else {
                map.remove(nextKey)
                map[orderKeyFunction(item)] = item
            }
        }
    }

    fun remove(item: V): Boolean {
        return map.remove(orderKeyFunction(item)) != null
    }

    fun get(it: K): V? {
        return map[it]
    }

    fun clear() {
        map.clear()
    }

    fun addAll(items: Collection<V>) {
        items.forEach { map[orderKeyFunction(it)] = it }
    }

    fun move(previousChildName: String?, child: V): Boolean {
        // Do nothing, because the move is handled by the subsequent change event
        return false
    }
}
//
//class FirebaseCollection<K : Comparable<K>, V : IFirebaseEntity>
//private constructor(
//    private val list: MutableList<V>,
//    private val ascending: Boolean,
//    private val orderKeyFunction: (V) -> K
//) : Collection<V> by list {
//
//    constructor(
//        orderKeyFunction: (V) -> K,
//        ascending: Boolean
//    ) : this(Collections.synchronizedList(LinkedList()), ascending, orderKeyFunction)
//
//    private val map = HashMap<String, Int>()
//
//    fun getAround(requestedInitialKey: K?, limit: Int): List<V> {
//        return list.subList(0, limit)
//        // TODO: reverse this if query is reversed
//        val list = requestedInitialKey?.let { key ->
//            list.dropWhile { orderKeyFunction(it) != key }.drop(1)
//        }
//            ?: list
//        val end = Math.min(limit, list.size)
//        return list.subList(0, end)
//    }
//
//    fun addAfter(id: String?, item: V) {
//
//        val insertIndex = id?.let { letId -> list.indexOfFirst { it.entityId == letId } + 1 }
//            ?: 0
//        list.add(insertIndex, item)
//    }
//
//    fun update(previousId: String?, item: V) {
//        val index = list.indexOfFirst { it.entityId == item.entityId }
//        list.removeAt(index)
//        list.add(index, item)
//    }
//
//    fun remove(item: V): Boolean {
//        return list.remove(item)
//    }
//
//    fun getAfter(key: K, limit: Int): List<V> {
//        return list.dropWhile { orderKeyFunction(it) < key }.take(limit)
//    }
//
//    fun getBefore(key: K, limit: Int): List<V> {
//        return list.takeWhile { orderKeyFunction(it) < key }.takeLast(limit)
//    }
//
//    fun get(it: K): V? {
//        return list.find { item -> orderKeyFunction(item) == it }
//    }
//
//    fun clear() {
//        list.clear()
//    }
//
//    fun addAll(items: Collection<V>) {
//        list.addAll(items)
//    }
//
//    fun move(previousChildName: String?, child: V) {
//        TODO("not implemented")
//    }
//}


