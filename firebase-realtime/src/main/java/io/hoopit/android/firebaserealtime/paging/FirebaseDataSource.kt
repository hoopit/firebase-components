package io.hoopit.android.firebaserealtime.paging

import androidx.paging.ItemKeyedDataSource
import timber.log.Timber
import kotlin.math.max

class FirebaseDataSource<Key : Comparable<Key>, StoreType : io.hoopit.android.firebaserealtime.core.FirebaseResource>(
    private val keyFunction: (StoreType) -> Key,
    private val store: FirebasePagedListQueryCache<Key, StoreType>
) : ItemKeyedDataSource<Pair<String, Key>, StoreType>() {

    override fun loadInitial(
        params: LoadInitialParams<Pair<String, Key>>,
        callback: LoadInitialCallback<StoreType>
    ) {
        val items = store.getInitial(
            params.requestedInitialKey?.second,
            params.requestedLoadSize
        )
//        callback.onResult(items)
        // TODO: required for placeholder support
        // Position is sometimes -1, which means the store has been modified after store.getInitial was returned
        // Maybe return position as part of store.getInitial(), or find another solution
        val position = items.firstOrNull()?.let { store.indexOf(it) } ?: 0
        callback.onResult(
            items,
            max(position, 0),
            store.size
        )
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
