package io.hoopit.android.firebaserealtime.paging

import androidx.paging.DataSource
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseResource

abstract class IFirebaseDataSourceFactory<Key : Comparable<Key>, StoreType : FirebaseResource, MappingType : Any> :
    DataSource.Factory<ItemKeyedFirebaseDataSource.DataSourceKey<Key>, MappingType>() {
    abstract val cache: FirebasePagedListCache<Key, StoreType>
    abstract val query: Query
    abstract val keyFunction: (MappingType) -> Key
}

class FirebaseDataSourceFactory<Key : Comparable<Key>, Type : FirebaseResource>(
    override val cache: FirebasePagedListCache<Key, Type>,
    override val query: Query,
    override val keyFunction: (Type) -> Key
) : IFirebaseDataSourceFactory<Key, Type, Type>() {

    class FirebaseDataSourceFactoryMapper<Key : Comparable<Key>, RemoteType : FirebaseResource, MappingType : Any>(
        private val dataSourceFactory: FirebaseDataSourceFactory<Key, RemoteType>,
        private val mapper: (RemoteType) -> MappingType,
        override val keyFunction: (MappingType) -> Key
    ) : IFirebaseDataSourceFactory<Key, RemoteType, MappingType>() {
        override val cache = dataSourceFactory.cache
        override val query = dataSourceFactory.query

        override fun create(): DataSource<ItemKeyedFirebaseDataSource.DataSourceKey<Key>, MappingType> {
            return dataSourceFactory.create().map(mapper)
        }
    }

    override fun create(): DataSource<ItemKeyedFirebaseDataSource.DataSourceKey<Key>, Type> {
        return ItemKeyedFirebaseDataSource(keyFunction, cache)
    }

    @Suppress("unused")
    fun <ToValue : Any> map(
        mapper: (Type) -> ToValue,
        sortKeyFunc: (ToValue) -> Key
    ): IFirebaseDataSourceFactory<Key, Type, ToValue> {
        return FirebaseDataSourceFactoryMapper(this, mapper, sortKeyFunc)
    }
}

