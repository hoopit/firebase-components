package io.hoopit.android.firebaserealtime.paging

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.firebaserealtime.cache.FirebaseCollectionCacheBase
import io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

interface IQueryCacheListener {
    fun getCount(): Int
}

class QueryCacheChildrenListener<RemoteType : IFirebaseEntity>(
    clazz: KClass<RemoteType>,
    private val cache: FirebaseCollectionCacheBase<*, RemoteType>
) : FirebaseChildEventListener<RemoteType>(clazz), IQueryCacheListener {

    private val count = AtomicInteger()

    override fun cancelled(error: DatabaseError) {
        Timber.e(error.toException())
    }

    override fun childMoved(previousChildName: String?, child: RemoteType) {
        Timber.d("called: childMoved($previousChildName, ${child.entityId})")
        cache.move(previousChildName, child)
    }

    override fun childChanged(previousChildName: String?, child: RemoteType) {
        Timber.d("called: childChanged($previousChildName, ${child.entityId})")
        cache.update(previousChildName, child)
    }

    @Synchronized
    override fun childAdded(previousChildName: String?, child: RemoteType) {
        Timber.d("called: childAdded($previousChildName, ${child.entityId})")
        cache.insert(previousChildName, child)
        count.incrementAndGet()
    }

    @Synchronized
    override fun childRemoved(child: RemoteType) {
        Timber.d("called: childRemoved(${child.entityId})")
        cache.delete(child)
        count.decrementAndGet()
    }

    @Synchronized
    override fun getCount(): Int {
        return count.get()
    }
}

class QueryCacheValueListener<RemoteType : IFirebaseEntity>(
    private val cache: FirebaseCollectionCacheBase<*, RemoteType>,
    private val clazz: KClass<RemoteType>
) : ValueEventListener, IQueryCacheListener {

    override fun onCancelled(error: DatabaseError) {
        Timber.e(error.toException())
    }

    @Synchronized
    override fun onDataChange(snapshot: DataSnapshot) {
        val items = snapshot.children.map {
            requireNotNull(it.getValue(clazz.java)?.apply {
                entityId = requireNotNull(it.key)
            })
        }
//        val items = snapshot.getValue(object : GenericTypeIndicator<HashMap<String, RemoteType>>() {}) ?: return
        items.let {
            count.set(it.size)
            cache.insertAll(items)
        }
    }

    private val count = AtomicInteger()

    @Synchronized
    override fun getCount(): Int {
        return count.get()
    }
}
