package io.hoopit.android.firebaserealtime.paging

import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.cache.FirebaseManagedCollectionCache
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import kotlin.reflect.KClass

class FirebasePagedListCache<K : Comparable<K>, Type : FirebaseResource>(
    val firebaseScope: FirebaseScope,
    query: Query,
    clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseManagedCollectionCache<K, Type>(firebaseScope, query, clazz, orderKeyFunction) {

    private val invalidationListeners = mutableListOf<() -> Unit>()

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    private var isInitialized = false

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    fun getInitial(key: K?, limit: Int): List<Type> {
//        if (!isInitialized) {
//            val listener = getListener()
//            scope.getResource(query).addListener(listener)
//            isInitialized = true
//        }
        return collection.getAround(key, limit)
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
