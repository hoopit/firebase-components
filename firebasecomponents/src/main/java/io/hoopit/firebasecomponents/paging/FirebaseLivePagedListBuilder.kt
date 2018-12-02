package io.hoopit.firebasecomponents.paging

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import io.hoopit.firebasecomponents.core.FirebaseChildEventListener
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseCacheLiveData
import kotlin.reflect.KClass

@Suppress("unused")
class FirebaseLivePagedListBuilder<Key : Comparable<Key>, LocalType : Any, RemoteType : IFirebaseEntity>(
    private val factory: IFirebaseDataSourceFactory<Key, RemoteType, LocalType>,
    config: PagedList.Config,
    classModel: KClass<RemoteType>,
    var disconnectDelay: Long = 2000
) {

    private val listener = Listener(classModel, factory.store)

    private val config = BoundaryCallbackConfig(listener, null, listener)

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

    @Suppress("MemberVisibilityCanBePrivate")
    val livePagedListBuilder = LivePagedListBuilder(factory, config)

    /**
     * Builds the PagedList LiveData. Boundary callback is overwritten.
     */
    fun build(): LiveData<PagedList<LocalType>> {
        val liveData = FirebaseCacheLiveData<PagedList<LocalType>>(
                factory.store,
                factory.query,
                disconnectDelay
        )
        liveData.addSource(livePagedListBuilder.setBoundaryCallback(buildBoundaryCallback()).build()) {
            liveData.value = it
        }
        return liveData
    }

    private fun buildBoundaryCallback(): FirebasePagedListBoundaryCallback<LocalType, Key> {
        return FirebaseManagedPagedListBoundaryCallback(
                factory.query,
                factory.keyFunction,
                factory.store.firebaseConnectionManager,
                factory.store
        )
    }

    interface IFirebaseCallbackListener {

        fun onMoved()

        fun onAdded()

        fun onRemoved()

    }

    class Listener<RemoteType : IFirebaseEntity>(
        classModel: KClass<RemoteType>,
        private val store: FirebasePagedListQueryCache<*, RemoteType>
    ) : FirebaseChildEventListener<RemoteType>(classModel) {

        override fun cancelled(error: DatabaseError) {
            TODO("not implemented")
        }

        override fun childMoved(previousChildName: String?, child: RemoteType) {
            TODO("not implemented")
        }

        override fun childChanged(previousChildName: String?, child: RemoteType) {
            store.update(previousChildName, child)
        }

        override fun childAdded(previousChildName: String?, child: RemoteType) {
            store.insert(previousChildName, child)
        }

        override fun childRemoved(child: RemoteType) {
            store.delete(child)
        }
    }
}
