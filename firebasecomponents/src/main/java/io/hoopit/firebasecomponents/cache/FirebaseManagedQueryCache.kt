package io.hoopit.firebasecomponents.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

abstract class FirebaseManagedQueryCache<K : Comparable<K>, Type : ManagedFirebaseEntity>(
    private val connectionManager: FirebaseConnectionManager,
    val query: Query,
    orderKeyFunction: (Type) -> K
) : FirebaseQueryCacheBase<K, Type>(query, orderKeyFunction), IManagedCache {

    override fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        connectionManager.deactivate(query)
    }

    override fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        connectionManager.activate(query)
    }

    override fun insert(previousId: String?, item: Type) {
        item.scope = query
        super.insert(previousId, item)
    }

    override fun update(previousId: String?, item: Type) {
        item.scope = query
        super.update(previousId, item)
    }

    override fun delete(item: Type) {
        item.scope = query
        super.delete(item)
    }
}

interface IManagedCache {
    fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query)
    fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query)
}

class FirebaseValueCache<Type : Any>(
    private val connectionManager: FirebaseConnectionManager,
    private val clazz: KClass<Type>
) : IManagedCache {

    fun getListener(query: Query): ValueEventListener {
        return liveData[query.spec] ?: throw IllegalArgumentException("")
    }

    val liveData = mutableMapOf<QuerySpec, FirebaseCachedValueLiveData<Type>>()

    fun get(query: Query): Type? {
        return liveData[query.spec]?.value
    }

    fun getLiveData(query: Query): LiveData<Type> {
        return liveData.getOrPut(query.spec) {
            FirebaseCachedValueLiveData(this, query, clazz, 2000).also {
                connectionManager.valueScopes.getScope(query).addQuery(query, it)
            }
        }
    }

    override fun onInActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        // TODO: refactor
        connectionManager.valueScopes.deactivate(query)
    }

    override fun onActive(firebaseCacheLiveData: FirebaseCacheLiveData<*>, query: Query) {
        // TODO: refactor
        connectionManager.valueScopes.activate(query)
    }

}

class FirebaseCachedValueLiveData<T : Any>(
    cache: FirebaseValueCache<T>,
    query: Query,
    private val classModel: KClass<T>,
    disconnectDelay: Long = 2000
) : FirebaseCacheLiveData<T>(cache, query, disconnectDelay), ValueEventListener {

    override fun onCancelled(error: DatabaseError) {
        TODO("not implemented")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        value = snapshot.getValue(classModel.java)
    }
}
