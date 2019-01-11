package io.hoopit.android.firebaserealtime.paging

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.firebaserealtime.cache.FirebaseQueryCacheBase
import io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

interface IQueryCacheListener {
    fun getCount(): Int
}

class QueryCacheChildrenListener<RemoteType : io.hoopit.android.firebaserealtime.core.IFirebaseEntity>(
    clazz: KClass<RemoteType>,
    private val cache: io.hoopit.android.firebaserealtime.cache.FirebaseQueryCacheBase<*, RemoteType>
) : io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener<RemoteType>(clazz), IQueryCacheListener {

    private val count = AtomicInteger()

    override fun cancelled(error: DatabaseError) {
        TODO("not implemented")
    }

    override fun childMoved(previousChildName: String?, child: RemoteType) {
        TODO("not implemented")
    }

    override fun childChanged(previousChildName: String?, child: RemoteType) {
        cache.update(previousChildName, child)
    }

    @Synchronized
    override fun childAdded(previousChildName: String?, child: RemoteType) {
        cache.insert(previousChildName, child)
        count.incrementAndGet()
    }

    @Synchronized
    override fun childRemoved(child: RemoteType) {
        cache.delete(child)
        count.decrementAndGet()
    }

    @Synchronized
    override fun getCount(): Int {
        return count.get()
    }
}

class QueryCacheValueListener<RemoteType : io.hoopit.android.firebaserealtime.core.IFirebaseEntity>(
    private val cache: io.hoopit.android.firebaserealtime.cache.FirebaseQueryCacheBase<*, RemoteType>,
    private val clazz: KClass<RemoteType>
) : ValueEventListener, IQueryCacheListener {

    override fun onCancelled(p0: DatabaseError) {
        TODO("not implemented")
    }

    @Synchronized
    override fun onDataChange(snapshot: DataSnapshot) {
        val items = snapshot.children.map { requireNotNull(it.getValue(clazz.java)?.apply { entityId = requireNotNull(it.key) }) }
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
