package io.hoopit.android.firebaserealtime.paging

import com.google.firebase.database.Query
import kotlin.reflect.KClass

class FirebasePagedListQueryCache<K : Comparable<K>, Type : io.hoopit.android.firebaserealtime.core.FirebaseResource>(
    val scope: io.hoopit.android.firebaserealtime.core.Scope,
    query: Query,
    clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : io.hoopit.android.firebaserealtime.cache.FirebaseManagedQueryCache<K, Type>(scope, query, clazz, orderKeyFunction) {

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

    fun addInvalidationListener(listener: () -> Unit) {
        invalidationListeners.add(listener)
    }

    override fun invalidate() {
        invalidationListeners.forEach { it() }
        invalidationListeners.clear()
    }
}
