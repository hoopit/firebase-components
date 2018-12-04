package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.database.ChildEventListener
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData

@Suppress("unused")
class FirebaseLivePagedListBuilder<Key : Comparable<Key>, LocalType : Any, RemoteType : ManagedFirebaseEntity>(
    private val factory: IFirebaseDataSourceFactory<Key, RemoteType, LocalType>,
    private val pagedListConfig: PagedList.Config.Builder,
    var disconnectDelay: Long = 2000
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

    private val livePagedListBuilder = LivePagedListBuilder(factory, pagedListConfig.build())

    /**
     * Builds the PagedList LiveData. Boundary callback is overwritten.
     */
    fun build(): LiveData<PagedList<LocalType>> {
        val liveData = FirebaseCacheLiveData<PagedList<LocalType>>(
                factory.cache.scope.getScope(factory.query),
                factory.query,
                factory.cache
        )
        val callback = buildBoundaryCallback()
        liveData.addSource(livePagedListBuilder.setBoundaryCallback(callback).build()) {
            liveData.value = it
        }
        return liveData
    }

    private fun buildBoundaryCallback(): FirebasePagedListBoundaryCallback<LocalType, Key> {
        return FirebaseManagedPagedListBoundaryCallback(
                factory.query,
                factory.keyFunction,
                factory.cache,
                factory.cache.scope.getScope(factory.query),
                pagedListConfig.build()
        )
    }
}
