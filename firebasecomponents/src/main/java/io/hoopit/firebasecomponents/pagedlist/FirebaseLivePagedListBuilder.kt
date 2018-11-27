package io.hoopit.firebasecomponents.pagedlist

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseError
import io.hoopit.firebasecomponents.FirebaseChildEventListener
import io.hoopit.firebasecomponents.FirebaseConnectionManager
import io.hoopit.firebasecomponents.IFirebaseListEntity
import io.hoopit.firebasecomponents.livedata.FirebasePagedListLiveData
import kotlin.reflect.KClass

@Suppress("unused")
class FirebaseLivePagedListBuilder<Key : Comparable<Key>, LocalType : Any, RemoteType : IFirebaseListEntity>(
    private val factory: IFirebaseDataSourceFactory<Key, RemoteType, LocalType>,
    config: PagedList.Config,
    classModel: KClass<RemoteType>
) {

    private val listener = Listener(classModel, factory.store)

    private val config = Config(listener, listener, listener)

    enum class FirebaseReferenceMode {
        LIVE,
        SINGLE
    }

    data class Config(
        val initialListener: ChildEventListener? = null,
        val frontListener: ChildEventListener? = null,
        val endListener: ChildEventListener? = null,
        val initialCount: Int = 30,
        val nextCount: Int = -1,
        val previousCount: Int = -1,
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
        val liveData = FirebasePagedListLiveData<PagedList<LocalType>>(
            factory.query,
            firebaseConnectionManager = FirebaseConnectionManager
        )
        liveData.addSource(livePagedListBuilder.setBoundaryCallback(buildBoundaryCallback()).build()) {
            liveData.value = it
        }
        return liveData
    }

    private fun buildBoundaryCallback(): FirebasePagedListBoundaryCallback<LocalType, Key> {
        return FirebasePagedListChildBoundaryCallback(
            factory.query,
            factory.keyFunction,
            config.initialListener,
            config.frontListener,
            config.endListener,
            FirebaseConnectionManager
        )
    }

    class Listener<RemoteType : IFirebaseListEntity>(
        classModel: KClass<RemoteType>,
        private val store: FirebasePagedListMemoryStore<*, RemoteType>
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
