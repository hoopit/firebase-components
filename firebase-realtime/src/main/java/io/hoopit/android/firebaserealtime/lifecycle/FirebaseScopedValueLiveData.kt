package io.hoopit.android.firebaserealtime.lifecycle

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.firebaserealtime.cache.IManagedCache
import io.hoopit.android.firebaserealtime.core.Scope
import kotlin.reflect.KClass

class FirebaseScopedValueLiveData<T : Any>(
    query: Query,
    resource: Scope.Resource,
    private val clazz: KClass<T>,
    cache: IManagedCache? = null,
    disconnectDelay: Long
) : FirebaseCacheLiveData<T>(resource, query, cache, disconnectDelay), ValueEventListener {

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
