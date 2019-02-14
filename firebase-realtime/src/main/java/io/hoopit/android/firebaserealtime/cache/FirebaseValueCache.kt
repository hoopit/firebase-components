package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.core.view.QuerySpec
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import io.hoopit.android.firebaserealtime.core.FirebaseScopedResource
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseCacheLiveData
import timber.log.Timber
import kotlin.reflect.KClass

class FirebaseValueCache<Type : Any>(
    private val firebaseScope: FirebaseScope,
    private val clazz: KClass<Type>
) : IManagedCache {

    override fun dispose() {
        liveData.clear()
        // TODO: Do we need to do more cleanup?
    }

    private val liveData = mutableMapOf<QuerySpec, LiveData<Type?>>()

    fun get(query: Query): Type? {
        return liveData[query.spec]?.value
    }

    fun getLiveData(
        query: Query,
        disconnectDelay: Long,
        resource: FirebaseScope.Resource = firebaseScope.getResource(query)
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

        override fun onCancelled(error: DatabaseError) {
            Timber.w(error.toException())
        }

        override fun onDataChange(snapshot: DataSnapshot) {
            val item = snapshot.getValue(clazz.java)
            if (item is FirebaseScopedResource) {
                item.entityId = requireNotNull(snapshot.key)
                item.init(firebaseScope, query)
            }
            liveData.postValue(item)
        }
    }
}
