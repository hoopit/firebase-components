package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.paging.QueryCacheChildrenListener
import io.hoopit.android.firebaserealtime.paging.QueryCacheValueListener
import kotlin.reflect.KClass

abstract class FirebaseManagedCollectionCache<K : Comparable<K>, Type : FirebaseScopedResource>(
    private val firebaseScope: FirebaseScope,
    val query: Query,
    descending: Boolean,
    private val clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseCollectionCacheBase<K, Type>(query, descending, orderKeyFunction), IManagedCache {

    override fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query) {
//        scope.dispatchDeactivate()
    }

    override fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query) {
//        scope.dispatchActivate()
    }

    override fun dispose() {
        //TODO: Improve
        collection.clear()
        invalidate()
    }

    override fun insert(previousId: String?, item: Type) {
        item.init(firebaseScope, query)
        super.insert(previousId, item)
    }

    override fun insertAll(items: Collection<Type>) {
        items.forEach {
            it.init(firebaseScope, query)
        }
        super.insertAll(items)
    }

    override fun update(previousId: String?, item: Type) {
        item.init(firebaseScope, query)
        super.update(previousId, item)
    }

    override fun delete(item: Type) {
        item.init(firebaseScope, query)
        super.delete(item)
    }

    fun getChildListener() = QueryCacheChildrenListener(clazz, this)

    fun getValueListener() = QueryCacheValueListener(this, clazz)
}
