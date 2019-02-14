package io.hoopit.android.firebaserealtime.core

import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.ConcurrentSkipListMap

class FirebaseCollection<K : Comparable<K>, V : IFirebaseEntity>
private constructor(
    private val map: ConcurrentSkipListMap<K, V>,
    private val orderKeyFunction: (V) -> K
) : Collection<V> by map.values {

    constructor(
        orderKeyFunction: (V) -> K,
        descending: Boolean
    ) : this(ConcurrentSkipListMap { o1, o2 ->
        if (!descending) o1.compareTo(o2)
        else o2.compareTo(o1)
    }, orderKeyFunction)

    data class InitialData<V>(val items: List<V>, val position: Int, val totalCount: Int)

    @Synchronized
    fun load(key: K?, limit: Int, clampLimit: Int): InitialData<V> {
        val adjustedKey = if (key != null && map.keys.indexOf(key) <= clampLimit) {
            Timber.d("load: Adjusted initial position to 0")
            null
        } else {
            key
        }
        val values = adjustedKey?.let { map.tailMap(it, true).values } ?: map.values
        val data = values.take(limit).toList()
        val position = adjustedKey?.let {
            map.keys.indexOf(it)
        } ?: 0
        return InitialData(items = data, position = position, totalCount = map.size)
    }

    @Synchronized
    fun getAfter(key: K, limit: Int): List<V> {
        val list = key.let { map.tailMap(it, false).values }
        return list.take(limit).toList()
    }

    @Synchronized
    fun getBefore(key: K, limit: Int): List<V> {
        val list = key.let { map.headMap(it, false).values }
        return list.toList().takeLast(limit)
    }

    fun load(start: Int, limit: Int, clampLimit: Int): InitialData<V> {
        return InitialData(map.values.toList().subList(start, start + limit), start, map.size)
    }

    fun getRange(startPosition: Int, loadSize: Int): List<V> {

        TODO("not implemented")
    }

    @Synchronized
    fun addAfter(id: String?, item: V) {
        map[orderKeyFunction(item)] = item
    }

    @Synchronized
    fun update(previousItemId: String?, item: V) {
//        val oldValue = map.replace(orderKeyFunction(item), item)
//        if (oldValue != null) return
        if (previousItemId == null) {
            map.remove(map.lastKey())
            map[orderKeyFunction(item)] = item
        } else {
            val currentKey = map.entries.first { (_, value) -> value.entityId == previousItemId }.key
            val nextKey = map.lowerKey(currentKey)
            if (nextKey == null) {
                Timber.e(IllegalStateException("nextKey cannot be null"))
                map[orderKeyFunction(item)] = item
            } else {
                map.remove(nextKey)
                map[orderKeyFunction(item)] = item
            }
        }
    }

    @Synchronized
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

    fun position(item: V): Int {
        return map.keys.indexOf(orderKeyFunction(item))
    }
}

abstract class FirebaseCollectionBase<V : IFirebaseEntity> : IFirebaseLinkedListCollection<V> {

    private var invalidationListener: (() -> Unit)? = null
    private var removeAfterInvalidate = false

    @Synchronized
    override fun setInvalidationListener(listener: (() -> Unit)?, removeAfterInvalidate: Boolean) {
        invalidationListener = listener
        this.removeAfterInvalidate = removeAfterInvalidate
    }

    @Synchronized
    private fun invalidate() {
        invalidationListener?.invoke()
        if (removeAfterInvalidate) invalidationListener = null
    }

    private val invalidationHandler = Handler(Looper.getMainLooper())

    private val invalidationTask = Runnable {
        invalidate()
    }

    protected fun dispatchInvalidate() {
        invalidationHandler.removeCallbacks(invalidationTask)
        invalidationHandler.postDelayed(invalidationTask, 100)
    }
}

interface IFirebaseLinkedListCollection<V : IFirebaseEntity> {
    val size: Int

    fun setInvalidationListener(listener: (() -> Unit)?, removeAfterInvalidate: Boolean = false)

    fun clear(invalidate: Boolean = true)

    fun addAfter(key: String?, child: V): Boolean

    fun addBefore(key: String?, child: V): Boolean

    fun updateBefore(key: String?, item: V)

    fun updateAfter(key: String?, item: V)

    fun moveAfter(key: String?, child: V): Boolean
    fun moveBefore(key: String?, child: V): Boolean
    fun remove(child: V): Boolean

    fun getInitial(startPosition: Int, loadSize: Int): InitialData<V>

    fun getRange(startPosition: Int, loadSize: Int): List<V>
    fun update(key: String?, item: V, before: Boolean)
    fun move(key: String?, child: V, before: Boolean)
    fun add(key: String?, child: V, before: Boolean)

    data class InitialData<V>(val items: List<V>, val position: Int, val totalCount: Int)
}

class CustomFirebaseCollection<V : IFirebaseEntity> : FirebaseCollectionBase<V>() {

    private inner class Node(var item: V? = null) {
        var next: Node? = null
        var prev: Node? = null
    }

    private var head: Node = Node()
    private val descendingHead: Node = Node()
    private val map = HashMap<String?, Node>().apply { this[null] = head }

    init {

    }

    override val size: Int
        get() = map.size - 1

    override fun update(key: String?, item: V, before: Boolean) {
        Timber.d("update() called with: key = [$key], item = [$item], before = [$before]")
        if (before) updateBefore(key, item) else updateAfter(key, item)
        dispatchInvalidate()
    }

    override fun updateAfter(key: String?, item: V) {
        requireNotNull(get(key).next).item = item
    }

    override fun updateBefore(key: String?, item: V) {
        requireNotNull(get(key).prev).item = item
    }

    private fun get(key: String?) = requireNotNull(map[key])

    override fun clear(invalidate: Boolean) {
        map.clear()
        map[null] = head
        if (invalidate) dispatchInvalidate()
    }

    override fun move(key: String?, child: V, before: Boolean) {
        Timber.d("move() called with: key = [$key], child = [$child], before = [$before]")
        val moved = if (before) moveBefore(key, child) else moveAfter(key, child)
        if (moved) dispatchInvalidate()
    }

    override fun moveAfter(key: String?, child: V): Boolean {
        val node = get(child.entityId)
        if (node.prev?.item?.entityId == key) return false
        remove(child)
        addAfter(key, child)
        return true
    }

    override fun moveBefore(key: String?, child: V): Boolean {
        val node = get(child.entityId)
        if (node.next?.item?.entityId == key) return false
        remove(child)
        addBefore(key, child)
        return true
    }

    override fun add(key: String?, child: V, before: Boolean) {
        Timber.d("add() called with: key = [$key], child = [$child], before = [$before]")
        val added = if (before) addBefore(key, child) else addAfter(key, child)
        if (added) dispatchInvalidate()
    }

    override fun addBefore(key: String?, child: V): Boolean {
        map[child.entityId]?.let {
            return if (it.next?.item?.entityId == key) {
                false
            } else {
                // This shouldn't happen
                check(false)
                moveBefore(key, child)
                true
            }
        }
        val newNode = Node(child)
        map[child.entityId] = newNode
        val nextNode = get(key)
        newNode.next = nextNode
        newNode.prev = nextNode.prev
        nextNode.prev?.next = newNode
        nextNode.prev = newNode
        if (head.item?.entityId == key) head = newNode
        return true
    }

    override fun addAfter(key: String?, child: V): Boolean {
        map[child.entityId]?.let {
            return if (it.prev?.item?.entityId == key) {
                false
            } else {
                // This shouldn't happen
                check(false)
                moveAfter(key, child)
                true
            }
        }
        val newNode = Node(child)
        map[child.entityId] = newNode
        val prevNode = get(key)
        newNode.prev = prevNode
        newNode.next = prevNode.next
        prevNode.next?.prev = newNode
        prevNode.next = newNode
//        if (tail == prevNode) tail = newNode
        return true
    }

    override fun remove(child: V): Boolean {
        Timber.d("remove() called with: child = [$child]")
        val node = get(child.entityId)
//        if (tail.item?.entityId == child.entityId) {
//            tail = requireNotNull(node.prev)
//        }
        node.prev?.next = node.next
        node.next?.prev = node.prev
        map.remove(child.entityId)
        if (head.item?.entityId == child.entityId) head = requireNotNull(node.next)
        dispatchInvalidate()
        return true
    }

    override fun getInitial(startPosition: Int, loadSize: Int): IFirebaseLinkedListCollection.InitialData<V> {
        val items = getRange(startPosition, loadSize)
        return IFirebaseLinkedListCollection.InitialData(items, startPosition, size)
    }

    override fun getRange(startPosition: Int, loadSize: Int): List<V> {
        // FIXME: handle NULL node
        val items = mutableListOf<V>()
        var node: Node? = if (head.item == null) head.next else head
        for (i in 1..startPosition) node = node?.next ?: return emptyList()
        for (i in 1..loadSize) {
            node?.item?.let { items.add(it) }
            node = node?.next ?: return items
        }

        return items
    }
}
//
//class FirebaseLinkedListCollection<V : IFirebaseEntity>
//private constructor(
//    private val list: LinkedList<V>,
//    private val descending: Boolean
//) : FirebaseCollectionBase<V>(), Collection<V> by list {
//
//    private class DescendingListIterator<V>(list: MutableList<V>) : MutableListIterator<V> {
//
//        private val iterator = list.listIterator(list.size)
//
//        override fun hasPrevious(): Boolean = iterator.hasNext()
//
//        override fun nextIndex(): Int = iterator.previousIndex()
//
//        override fun previous(): V = iterator.next()
//
//        override fun previousIndex(): Int = iterator.nextIndex()
//
//        override fun add(element: V) {
//            val hasNext = iterator.hasNext()
//            if (hasNext) iterator.next()
//            if (iterator.nextIndex() == 0) {
//                // switch items
//            }
//            iterator.add(element)
//            if (hasNext) iterator.previous()
//        }
//
//        override fun hasNext(): Boolean = iterator.hasPrevious()
//
//        override fun next(): V = iterator.previous()
//
//        override fun remove() = iterator.remove()
//
//        override fun set(element: V) = iterator.set(element)
//    }
//
//    constructor(descending: Boolean) : this(LinkedList(), descending)
//
//    private fun getIterator(): MutableListIterator<V> =
//        if (descending) DescendingListIterator(list) else list.listIterator()
//
//    private fun getReverseIterator(): MutableListIterator<V> =
//        if (!descending) DescendingListIterator(list) else list.listIterator()
//
//    override fun updateAfter(previousId: String?, item: V) {
//        val iterator = getIterator()
//        while (iterator.hasNext()) {
//            val element = iterator.next()
//            if (previousId == null || element.entityId == previousId) {
//                if (previousId != null) iterator.next()
//                iterator.set(item)
//            }
//        }
//        dispatchInvalidate()
//    }
//
//    @Synchronized
//    override fun clear(invalidate: Boolean) {
//        list.clear()
//        if (invalidate) dispatchInvalidate()
//    }
//
//    @Synchronized
//    override fun moveAfter(previousChildName: String?, child: V): Boolean {
//        // TODO: combine into a single iteration
//        val iterator = getIterator()
//        var previous: V? = null
//        var moved = false
//        while (iterator.hasNext()) {
//
//            val item = iterator.next()
//            if (item.entityId == previousChildName) {
//                // found new position
//                val hasNext = iterator.hasNext()
//                if (hasNext && iterator.next().entityId == child.entityId)
//                // item is already in correct position
//                    return false
//                else if (hasNext) iterator.previous()
//                iterator.add(child)
//                if (moved) break
//                moved = true
//            } else if (item.entityId == child.entityId) {
//                // found existing position
//                if (previousChildName == previous?.entityId) {
//                    // item is already in correct position
//                    return false
//                } else {
//                    iterator.remove()
//                    if (moved) break
//                    moved = true
//                }
//            }
//            previous = item
//        }
//
////        removeFirst(child.entityId)
////        addAfter(previousChildName, child)
//        dispatchInvalidate()
//        return true
//    }
//
//    private fun List<V>.reverseIfDescending(): List<V> {
//        return if (descending) this.reversed() else this
//    }
//
//    @Synchronized
//    override fun getInitial(startPosition: Int, loadSize: Int): IFirebaseLinkedListCollection.InitialData<V> {
//        val subList = list.subList(startPosition, min(startPosition + loadSize, size)).toList()
//        return IFirebaseLinkedListCollection.InitialData(subList, startPosition, list.size)
//    }
//
//    @Synchronized
//    override fun getRange(startPosition: Int, loadSize: Int): List<V> {
//        return list.subList(startPosition, min(startPosition + loadSize, size)).toList()
//    }
//
//    @Synchronized
//    override fun addAfter(previousChildName: String?, child: V) {
//        val added = addAfter1(previousChildName, child)
//        if (added) dispatchInvalidate()
//    }
//
//    @Synchronized
//    override fun remove(child: V): Boolean {
//        val removeFirst = removeFirst(child.entityId)
//        dispatchInvalidate()
//        return removeFirst
//    }
//
//    private fun addAfter1(previousChildName: String?, child: V): Boolean {
//        val iterator = getIterator()
//        while (previousChildName != null && iterator.hasNext()) {
//            val item = iterator.next()
//            if (item.entityId == previousChildName) {
//                if (iterator.hasNext() && iterator.next().entityId == child.entityId)
//                    return false
//                break
//            }
//        }
//        iterator.add(child)
//        return true
//    }
//
//    private fun removeFirst(entityId: String): Boolean {
//        val iterator = getReverseIterator()
//        while (iterator.hasNext()) {
//            val item = iterator.next()
//            if (item.entityId == entityId) {
//                iterator.remove()
//                return true
//            }
//        }
//        return false
//    }
//}


