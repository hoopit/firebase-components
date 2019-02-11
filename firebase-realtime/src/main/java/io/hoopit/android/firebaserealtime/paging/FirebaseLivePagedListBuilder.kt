package io.hoopit.android.firebaserealtime.paging

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.database.ChildEventListener
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseCacheLiveData

class FirebaseLivePagedListBuilder<Key : Comparable<Key>, LocalType : Any, RemoteType : FirebaseResource>(
    private val factory: IFirebaseDataSourceFactory<Key, RemoteType, LocalType>,
    pagedListConfig: PagedList.Config,
    var disconnectDelay: Long
) {

//    private val listener = Listener(classModel, factory.store)
//    private val callbackConfig = BoundaryCallbackConfig(listener, null, listener)

    enum class FirebaseReferenceMode {
        LIVE,
        SINGLE
    }

    data class BoundaryCallbackConfig(
        // TODO: Consider allowing custom page size, but requires custom Query object to hold sort order
        val initialListener: ChildEventListener? = null,
        val frontListener: ChildEventListener? = null,
        val endListener: ChildEventListener? = null,
        val ignoreRemove: Boolean = false,
        val initialMode: FirebaseReferenceMode = FirebaseReferenceMode.LIVE,
        val previousPagesMode: FirebaseReferenceMode = FirebaseReferenceMode.SINGLE,
        val nextPagesMode: FirebaseReferenceMode = FirebaseReferenceMode.SINGLE
    )

    private val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig)

    /**
     * Builds the PagedList LiveData. Boundary callback is overwritten.
     */
    fun build(): LiveData<PagedList<LocalType>> {
        val liveData = FirebaseCacheLiveData<PagedList<LocalType>>(
            factory.cache.firebaseScope.getResource(factory.query),
            factory.query,
            factory.cache,
            disconnectDelay
        )
        liveData.addSource(livePagedListBuilder.build()) {
            liveData.value = it
        }
        return liveData
    }

    fun setBoundaryCallback(boundaryCallback: PagedList.BoundaryCallback<LocalType>): LivePagedListBuilder<ItemKeyedFirebaseDataSource.DataSourceKey<Key>, LocalType> {
        return livePagedListBuilder.setBoundaryCallback(boundaryCallback)
    }
}
