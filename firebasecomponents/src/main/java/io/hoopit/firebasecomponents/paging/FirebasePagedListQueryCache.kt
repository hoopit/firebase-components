package io.hoopit.firebasecomponents.paging

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.cache.FirebaseManagedQueryCache
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.core.Scope
import kotlin.reflect.KClass

class FirebasePagedListQueryCache<K : Comparable<K>, Type : ManagedFirebaseEntity>(
    val scope: Scope,
    query: Query,
    clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseManagedQueryCache<K, Type>(scope, query, clazz, orderKeyFunction) {

    private val invalidationListeners = mutableListOf<() -> Unit>()

    private val dataSourceFactory = FirebaseDataSourceFactory(this, query, orderKeyFunction)

    fun getDataSourceFactory(): FirebaseDataSourceFactory<K, Type> {
        return dataSourceFactory
    }

    fun getAround(key: K?, limit: Int): List<Type> {
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
