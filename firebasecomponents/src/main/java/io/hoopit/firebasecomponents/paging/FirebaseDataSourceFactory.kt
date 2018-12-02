package io.hoopit.firebasecomponents.paging

import androidx.paging.DataSource
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.IFirebaseEntity

abstract class IFirebaseDataSourceFactory<Key : Comparable<Key>, StoreType : IFirebaseEntity, MappingType : Any> :
        DataSource.Factory<Pair<String, Key>, MappingType>() {
    abstract val store: FirebasePagedListQueryCache<Key, StoreType>
    abstract val query: Query
    abstract val keyFunction: (MappingType) -> Key
}

class FirebaseDataSourceFactory<Key : Comparable<Key>, Type : IFirebaseEntity>(
    override val store: FirebasePagedListQueryCache<Key, Type>,
    override val query: Query,
    override val keyFunction: (Type) -> Key
) : IFirebaseDataSourceFactory<Key, Type, Type>() {

    class FirebaseDataSourceFactoryMapper<Key : Comparable<Key>, RemoteType : IFirebaseEntity, MappingType : Any>(
        private val dataSourceFactory: FirebaseDataSourceFactory<Key, RemoteType>,
        private val mapper: (RemoteType) -> MappingType,
        override val keyFunction: (MappingType) -> Key
    ) : IFirebaseDataSourceFactory<Key, RemoteType, MappingType>() {
        override val store = dataSourceFactory.store
        override val query = dataSourceFactory.query

        override fun create(): DataSource<Pair<String, Key>, MappingType> {
            return dataSourceFactory.create().map(mapper)
        }
    }

    override fun create(): DataSource<Pair<String, Key>, Type> {
        return FirebaseDataSource(keyFunction, store).also {
            store.addInvalidationListener { it.invalidate() }
        }
    }

    @Suppress("unused")
    fun <ToValue : Any> map(
        mapper: (Type) -> ToValue,
        sortKeyFunc: (ToValue) -> Key
    ): IFirebaseDataSourceFactory<Key, Type, ToValue> {
        return FirebaseDataSourceFactoryMapper(this, mapper, sortKeyFunc)
    }
}
