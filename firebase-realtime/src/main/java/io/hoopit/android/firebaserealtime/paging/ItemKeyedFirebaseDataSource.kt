package io.hoopit.android.firebaserealtime.paging

import androidx.paging.ItemKeyedDataSource
import androidx.paging.PositionalDataSource
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import timber.log.Timber

class ItemKeyedFirebaseDataSource<Key : Comparable<Key>, StoreType : FirebaseResource>(
    private val keyFunction: (StoreType) -> Key,
    private val cache: FirebasePagedListCache<Key, StoreType>
) : ItemKeyedDataSource<ItemKeyedFirebaseDataSource.DataSourceKey<Key>, StoreType>() {

    override fun loadInitial(
        params: LoadInitialParams<DataSourceKey<Key>>,
        callback: LoadInitialCallback<StoreType>
    ) {
        Timber.d("called: loadInitial, initialKey: ${params.requestedInitialKey?.key}, requestedLoadSize: ${params.requestedLoadSize}")
        val data = cache.getInitial(
            key = params.requestedInitialKey?.key,
            limit = params.requestedLoadSize,
            clampToZero = 10
        )
        cache.addInvalidationListener { invalidate() }
        Timber.d("callback.onResult: LoadInitial, items: ${data.items.size}, storeSize: ${data.totalCount}, position: ${data.position}")
        try {
//            callback.onResult(data.items)
            callback.onResult(data.items, data.position, data.totalCount)
        } catch (e: IllegalArgumentException) {
            Timber.e(e)
        }
    }

    override fun loadAfter(
        params: LoadParams<DataSourceKey<Key>>,
        callback: LoadCallback<StoreType>
    ) {
        Timber.d("called: loadAfter")
        callback.onResult(cache.getAfter(params.key.key, params.requestedLoadSize))
    }

    override fun loadBefore(
        params: LoadParams<DataSourceKey<Key>>,
        callback: LoadCallback<StoreType>
    ) {
        Timber.d("called: loadBefore")
        callback.onResult(cache.getBefore(params.key.key, params.requestedLoadSize))
    }

    override fun getKey(item: StoreType) = DataSourceKey(requireNotNull(item.entityId), keyFunction(item))

    data class DataSourceKey<K>(val entityId: String, val key: K)
}

class PositionalFirebaseDataSource<Key : Comparable<Key>, StoreType : FirebaseResource>(
    private val keyFunction: (StoreType) -> Key,
    private val cache: FirebasePagedListCache<Key, StoreType>
) : PositionalDataSource<StoreType>() {

    override fun loadInitial(params: LoadInitialParams, callback: LoadInitialCallback<StoreType>) {
        val data = cache.getInitial(
            startPosition = params.requestedStartPosition,
            limit = params.requestedLoadSize,
            clampToZero = 10
        )
        cache.addInvalidationListener { invalidate() }
//        callback.onResult(data.items, data.position)
        callback.onResult(data.items, data.position, data.totalCount)
    }

    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<StoreType>) {
        val data = cache.getRange(params.startPosition, params.loadSize)
        callback.onResult(data)
    }
}
