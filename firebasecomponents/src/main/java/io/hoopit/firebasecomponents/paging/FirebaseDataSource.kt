package io.hoopit.firebasecomponents.paging

import androidx.paging.ItemKeyedDataSource
import io.hoopit.firebasecomponents.core.IFirebaseEntity

class FirebaseDataSource<Key : Comparable<Key>, StoreType : IFirebaseEntity>(
    private val keyFunction: (StoreType) -> Key,
    private val store: FirebasePagedListQueryCache<Key, StoreType>
) : ItemKeyedDataSource<Pair<String, Key>, StoreType>() {

    override fun loadInitial(
        params: LoadInitialParams<Pair<String, Key>>,
        callback: LoadInitialCallback<StoreType>
    ) {
        callback.onResult(
                store.getAfter(
                        params.requestedInitialKey?.second,
                        params.requestedLoadSize
                )
        )
    }

    override fun loadAfter(
        params: LoadParams<Pair<String, Key>>,
        callback: LoadCallback<StoreType>
    ) {
        callback.onResult(store.getAfter(params.key.second, params.requestedLoadSize))
    }

    override fun loadBefore(
        params: LoadParams<Pair<String, Key>>,
        callback: LoadCallback<StoreType>
    ) {
        callback.onResult(store.getBefore(params.key.second, params.requestedLoadSize))
    }

    override fun getKey(item: StoreType) = Pair(requireNotNull(item.entityId), keyFunction(item))
}
