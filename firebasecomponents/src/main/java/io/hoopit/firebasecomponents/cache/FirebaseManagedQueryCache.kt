package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.core.FirebaseResource
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import io.hoopit.firebasecomponents.paging.QueryCacheChildrenListener
import io.hoopit.firebasecomponents.paging.QueryCacheValueListener
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

    fun getValueListener() = QueryCacheValueListener(this)
}

interface IManagedCache {
    fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun dispose()
}

class FirebaseValueCache<Type : Any>(
    private val scope: Scope,
    private val clazz: KClass<Type>
) : IManagedCache {

    override fun dispose() {
        TODO("not implemented")
    }

    private val liveData = mutableMapOf<QuerySpec, LiveData<Type?>>()

    fun get(query: Query): Type? {
        return liveData[query.spec]?.value
    }

    fun getLiveData(query: Query, disconnectDelay: Long, resource: Scope.Resource = scope.getResource(query)): LiveData<Type?> {
        return liveData.getOrPut(query.spec) {
            FirebaseCacheLiveData<Type?>(resource, query, this, disconnectDelay).also {
                if (resource.rootQuery == query)
                    resource.addListener(Listener(clazz, it))
            }
        }
    }

    override fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    override fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    private class Listener<T : Any>(private val clazz: KClass<T>, private val liveData: MutableLiveData<T?>) : ValueEventListener {

        override fun onCancelled(p0: DatabaseError) {
            TODO("not implemented")
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            liveData.postValue(snapshot.getValue(clazz.java))
        }

    }
}

