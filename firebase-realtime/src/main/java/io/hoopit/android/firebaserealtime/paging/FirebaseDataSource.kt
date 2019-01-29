package io.hoopit.android.firebaserealtime.paging

import androidx.paging.ItemKeyedDataSource
import timber.log.Timber
import kotlin.math.max

class FirebaseDataSource<Key : Comparable<Key>, StoreType : io.hoopit.android.firebaserealtime.core.FirebaseResource>(
    private val keyFunction: (StoreType) -> Key,
    private val store: FirebasePagedListCache<Key, StoreType>
) : ItemKeyedDataSource<Pair<String, Key>, StoreType>() {

    override fun loadInitial(
        params: LoadInitialParams<Pair<String, Key>>,
        callback: LoadInitialCallback<StoreType>
    ) {
        Timber.d("called: loadInitial, initialKey: ${params.requestedInitialKey?.second}, requestedLoadSize: ${params.requestedLoadSize}")
        val items = store.getInitial(
            params.requestedInitialKey?.second,
            params.requestedLoadSize
        )
        store.addInvalidationListener { invalidate() }
//        callback.onResult(items)
        // TODO: required for placeholder support
        // Position is sometimes -1, which means the store has been modified after store.getInitial was returned
        // Maybe return position as part of store.getInitial(), or find another solution
        val position = items.firstOrNull()?.let { store.indexOf(it) } ?: 0
        val storeSize = if (items.isEmpty()) 0 else store.size
        Timber.d("callback.onResult: LoadInitial, items: ${items.size}, storeSize: $storeSize, position: $position")
        try {
            callback.onResult(items, max(position, 0), storeSize)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun loadAfter(
        params: LoadParams<Pair<String, Key>>,
        callback: LoadCallback<StoreType>
    ) {
        Timber.d("called: loadAfter")
        callback.onResult(store.getAfter(params.key.second, params.requestedLoadSize))
    }

    override fun loadBefore(
        params: LoadParams<Pair<String, Key>>,
        callback: LoadCallback<StoreType>
    ) {
        Timber.d("called: loadBefore")
        callback.onResult(store.getBefore(params.key.second, params.requestedLoadSize))
    }

    override fun getKey(item: StoreType) = Pair(requireNotNull(item.entityId), keyFunction(item))
}
