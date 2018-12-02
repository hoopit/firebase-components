package io.hoopit.firebasecomponents.paging

import com.google.firebase.database.DatabaseError
import io.hoopit.firebasecomponents.cache.FirebaseQueryCacheBase
import io.hoopit.firebasecomponents.core.FirebaseChildEventListener
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

class Listener<RemoteType : IFirebaseEntity>(
    classModel: KClass<RemoteType>,
    private val store: FirebaseQueryCacheBase<*, RemoteType>
) : FirebaseChildEventListener<RemoteType>(classModel) {

    private val count = AtomicInteger()

    override fun cancelled(error: DatabaseError) {
        TODO("not implemented")
    }

    override fun childMoved(previousChildName: String?, child: RemoteType) {
        TODO("not implemented")
    }

    override fun childChanged(previousChildName: String?, child: RemoteType) {
        store.update(previousChildName, child)
    }

    @Synchronized
    override fun childAdded(previousChildName: String?, child: RemoteType) {
        store.insert(previousChildName, child)
        count.incrementAndGet()
    }

    @Synchronized
    override fun childRemoved(child: RemoteType) {
        store.delete(child)
        count.decrementAndGet()
    }

    @Synchronized
    fun getCount(): Int {
        return count.get()
    }


}
