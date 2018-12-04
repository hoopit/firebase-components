package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import io.hoopit.firebasecomponents.paging.QueryCacheListener
import kotlin.reflect.KClass

abstract class FirebaseManagedQueryCache<K : Comparable<K>, Type : ManagedFirebaseEntity>(
    private val scope: Scope,
    val query: Query,
    private val clazz: KClass<Type>,
    orderKeyFunction: (Type) -> K
) : FirebaseQueryCacheBase<K, Type>(query, orderKeyFunction) {

    override fun insert(previousId: String?, item: Type) {
        item.resource = scope.getScope(query)
        item.cache = scope.cacheManager
        super.insert(previousId, item)
    }

    override fun update(previousId: String?, item: Type) {
        item.resource = scope.getScope(query)
        item.cache = scope.cacheManager
        super.update(previousId, item)
    }

    override fun delete(item: Type) {
        item.resource = scope.getScope(query)
        item.cache = scope.cacheManager
        super.delete(item)
    }

    fun getListener() = QueryCacheListener(clazz, this)
}

interface IManagedCache {
    fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query)
}

class FirebaseValueCache<Type : Any>(
    private val scope: Scope,
    private val clazz: KClass<Type>
) : IManagedCache {

    private val liveData = mutableMapOf<QuerySpec, LiveData<Type>>()
    private val scopedLiveData = mutableMapOf<QuerySpec, LiveData<Type>>()

    fun get(query: Query): Type? {
        return liveData[query.spec]?.value
    }

    fun getLiveData(query: Query, resource: Scope.Resource = scope.getScope(query)): LiveData<Type> {
        return scopedLiveData.getOrPut(query.spec) {
            FirebaseCacheLiveData<Type>(resource, query, this).also {
                resource.addListener(Listener(clazz, it))
            }
        }
    }

    override fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    override fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    private class Listener<T : Any>(private val clazz: KClass<T>, private val liveData: MutableLiveData<T>) : ValueEventListener {

        override fun onCancelled(p0: DatabaseError) {
            TODO("not implemented")
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            liveData.value = snapshot.getValue(clazz.java)
        }

    }
}

class FirebaseScopedValueLiveData<T : Any>(
    query: Query,
    resource: Scope.Resource,
    private val clazz: KClass<T>,
    cache: IManagedCache? = null
) : FirebaseCacheLiveData<T>(resource, query, cache), ValueEventListener {

    init {
        resource.addListener(this)
    }

    override fun onCancelled(p0: DatabaseError) {
        TODO("not implemented")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        value = snapshot.getValue(clazz.java)
    }

}
