package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.Scope
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

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

    fun getLiveData(
        query: Query,
        disconnectDelay: Long,
        resource: Scope.Resource = scope.getResource(query)
    ): LiveData<Type?> {
        return liveData.getOrPut(query.spec) {
            FirebaseCacheLiveData<Type?>(
                resource,
                query,
                this,
                disconnectDelay
            ).also {
                if (resource.rootQuery == query)
                    resource.addListener(Listener(query, it))
            }
        }
    }

    override fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    override fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query) {}

    private inner class Listener(
        private val query: Query,
        private val liveData: MutableLiveData<Type?>
    ) : ValueEventListener {

        override fun onCancelled(p0: DatabaseError) {
            TODO("not implemented")
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val item = snapshot.getValue(clazz.java)
            if (item is FirebaseResource) {
                item.entityId = requireNotNull(snapshot.key)
                item.scope = scope
                item.query = query
            }
            liveData.postValue(item)
        }
    }
}
