package io.hoopit.android.firebaserealtime.paging

import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.cache.FirebaseManagedCollectionCache
import io.hoopit.android.firebaserealtime.core.FirebaseCollection
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import kotlin.reflect.KClass

class FirebasePagedListCache<K : Comparable<K>, Type : FirebaseResource>(
    val firebaseScope: FirebaseScope,
    query: Query,
    descending: Boolean,
    clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseManagedCollectionCache<K, Type>(firebaseScope, query, descending, clazz, orderKeyFunction) {

    private val invalidationListeners = mutableListOf<() -> Unit>()

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    /**
     * Used by PositionalDataSource
     */
    fun getInitial(startPosition: Int, limit: Int, clampToZero: Int): FirebaseCollection.InitialData<Type> {
        return collection.load(startPosition, limit, clampToZero)
    }

    /**
     * Used by [PositionalDataSource]
     */
    fun getRange(startPosition: Int, loadSize: Int): List<Type> {
        return collection.getRange(startPosition, loadSize)
    }

    /**
     * Used by ItemKeyedDataSource
     */
    fun getInitial(key: K?, limit: Int, clampToZero: Int): FirebaseCollection.InitialData<Type> {
        return collection.load(key, limit, clampToZero)
    }

    fun getAfter(key: K, limit: Int): List<Type> {
        return collection.getAfter(key, limit)
    }

    fun getBefore(key: K, limit: Int): List<Type> {
        return collection.getBefore(key, limit)
    }

    @Synchronized
    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    @Synchronized
    override fun invalidate() {
        invalidationListeners.forEach { function ->
            function()
        }
        invalidationListeners.clear()
    }
}
