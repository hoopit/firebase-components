package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.Scope
import io.hoopit.android.firebaserealtime.paging.QueryCacheChildrenListener
import io.hoopit.android.firebaserealtime.paging.QueryCacheValueListener
import kotlin.reflect.KClass

abstract class FirebaseManagedQueryCache<K : Comparable<K>, Type : FirebaseResource>(
    private val scope: Scope,
    val query: Query,
    private val clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseQueryCacheBase<K, Type>(query, orderKeyFunction) {

    override fun insert(previousId: String?, item: Type) {
        item.scope = scope
        item.query = query
        super.insert(previousId, item)
    }

    override fun insertAll(items: Collection<Type>) {
        items.forEach {
            it.scope = scope
            it.query = query
        }
        super.insertAll(items)
    }

    override fun update(previousId: String?, item: Type) {
        item.scope = scope
        item.query = query
        super.update(previousId, item)
    }

    override fun delete(item: Type) {
        item.scope = scope
        item.query = query
        super.delete(item)
    }

    fun getChildListener() = QueryCacheChildrenListener(clazz, this)

    fun getValueListener() = QueryCacheValueListener(this, clazz)
}

interface IManagedCache {
    fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun dispose()
}

