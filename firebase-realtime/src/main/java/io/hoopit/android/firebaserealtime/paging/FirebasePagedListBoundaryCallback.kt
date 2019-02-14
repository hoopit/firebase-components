package io.hoopit.android.firebaserealtime.paging

import androidx.paging.PagedList
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseChildEventListener
import io.hoopit.android.firebaserealtime.core.FirebaseScope
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import io.hoopit.android.firebaserealtime.core.IFirebaseLinkedListCollection
import timber.log.Timber
import kotlin.reflect.KClass

/***
 * [PagedList.BoundaryCallback] for Firebase list resources
 */
abstract class FirebasePagedListBoundaryCallback<LocalType, Key>(
    private val query: Query,
    private val sortKey: (LocalType) -> Key
) : PagedList.BoundaryCallback<LocalType>() {

    override fun onZeroItemsLoaded() {
//        Timber.d("called: onZeroItemsLoaded: ${query.spec}")
        addInitialListener(query)
    }

    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        // FIXME: Enable paging
        return
        Timber.d("called: onItemAtEndLoaded: ${query.spec}")
        if (query.spec.loadsAllData()) {
            Timber.d("onItemAtEndLoaded: Ignored: Query loads all data.")
            return
        }
        // FIXME: Should we require isViewFromLeft?
        val q = if (query.spec.params.isViewFromLeft) startAt(query, itemAtEnd)
        else endAt(query, itemAtEnd)
        addEndListener(query, q)
    }

    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
        // FIXME: enable paging
        return
        // FIXME: Or is this never used ?
        Timber.d("called: onItemAtFrontLoaded: ${query.spec}")
        if (query.spec.loadsAllData()) {
            Timber.d("onItemAtFrontLoaded: Ignored: Query loads all data.")
            return
        }
        if (!query.spec.params.hasStart()) {
            Timber.d("onItemAtFrontLoaded: Ignored: No start specified.")
            return
        }

        val q = if (query.spec.params.isViewFromLeft) endAt(query, itemAtFront)
        else startAt(query, itemAtFront)
        addFrontListener(query, q)
    }

    private fun endAt(query: Query, item: LocalType): Query {
        val key = sortKey(item)
        return when (key) {
            is Number -> query.endAt(key.toDouble())
            is String -> query.endAt(key)
            is Boolean -> query.endAt(key)
            else -> throw IllegalArgumentException("Key must be a valid JSON value")
        }
    }

    private fun startAt(query: Query, item: LocalType): Query {
        val key = sortKey(item)
        return when (key) {
            is Number -> query.startAt(key.toDouble())
            is String -> query.startAt(key)
            is Boolean -> query.startAt(key)
            else -> throw IllegalArgumentException("Key must be a valid JSON value")
        }
    }

    protected abstract fun addInitialListener(query: Query)
    protected abstract fun addFrontListener(query: Query, subQuery: Query)
    protected abstract fun addEndListener(query: Query, subQuery: Query)
}
//
///***
// * [PagedList.BoundaryCallback] for Firebase list resources
// */
//class FirebaseManagedPagedListBoundaryCallback<LocalType, Key>(
//    query: Query,
//    sortKey: (LocalType) -> Key,
//    private val cache: FirebasePagedListCache<*, *>,
//    private val resource: io.hoopit.android.firebaserealtime.core.Scope.Resource,
//    private val pagedListConfig: PagedList.Config? = null
//) : FirebasePagedListBoundaryCallback<LocalType, Key>(cache.query, sortKey) {
//
//    private val limit = query.spec.params.limit
//
//    private var initialListener: QueryCacheChildrenListener<*>? = null
//    private var frontListener: QueryCacheChildrenListener<*>? = null
//    private var endListener: QueryCacheChildrenListener<*>? = null
//
//    private fun isInitialComplete(): Boolean {
//        return initialListener == null || initialListener?.getCount() == limit
//    }
//
//    private fun canAddFront(): Boolean {
//        return isInitialComplete() && (frontListener == null || frontListener?.getCount() == limit)
//    }
//
//    private fun canAddEnd(): Boolean {
//        return isInitialComplete() && (endListener == null || endListener?.getCount() == limit)
//    }
//
//    @Synchronized
//    override fun addInitialListener(query: Query) {
//        if (isInitialComplete()) {
//            Timber.d("Adding initial listener for ${query.spec}")
//            val listener = cache.getChildListener()
//            resource.addSubQuery(query, listener)
//            frontListener = listener
//        } else {
//            Timber.d("Denied initial listener for ${query.spec}")
//            assert(false)
//        }
//    }
//
//    @Synchronized
//    override fun addFrontListener(query: Query, subQuery: Query) {
//        if (canAddFront()) {
//            Timber.d("Adding front listener for ${subQuery.spec}")
//            val listener = cache.getChildListener()
//            resource.addSubQuery(subQuery, listener)
//            frontListener = listener
//        } else {
//            Timber.d("Denied front listener for ${subQuery.spec}")
//        }
//    }
//
//    @Synchronized
//    override fun addEndListener(query: Query, subQuery: Query) {
//        if (canAddEnd()) {
//            Timber.d("Adding end listener for ${subQuery.spec}")
//            val listener = cache.getChildListener()
//            resource.addSubQuery(subQuery, listener)
//            frontListener = listener
//        } else {
//            Timber.d("Denied end listener for ${subQuery.spec}")
//        }
//    }
//}

/**
 * Boundary callback that uses a child listener for the initial load, and value listeners for extended queries
 */
class FirebaseSimplePagedListBoundaryCallback<LocalType, Key>(
    query: Query,
    sortKey: (LocalType) -> Key,
    private val cache: FirebasePagedListCache<*, *>,
    private val resource: FirebaseScope.Resource,
    private val pagedListConfig: PagedList.Config? = null
) : FirebasePagedListBoundaryCallback<LocalType, Key>(cache.query, sortKey) {

    private val limit = query.spec.params.limit

    private var initialListener: IQueryCacheListener? = null
    private var frontListener: IQueryCacheListener? = null
    private var endListener: IQueryCacheListener? = null

    private fun isInitialComplete(): Boolean {
        return initialListener == null || initialListener?.getCount() == limit
    }

    private fun canAddFront(): Boolean {
        return isInitialComplete() && (frontListener == null || frontListener?.getCount() == limit)
    }

    private fun canAddEnd(): Boolean {
        return isInitialComplete() && (endListener == null || endListener?.getCount() == limit)
    }

    @Synchronized
    override fun addInitialListener(query: Query) {
        if (isInitialComplete()) {
            Timber.d("Adding initial listener for ${query.spec}")
            val listener = cache.getChildListener()
            resource.addSubQuery(query, listener)
            initialListener = listener
        } else {
            Timber.d("Denied initial listener for ${query.spec}")
        }
    }

    @Synchronized
    override fun addFrontListener(query: Query, subQuery: Query) {
        if (canAddFront()) {
            Timber.d("Adding front listener for ${subQuery.spec}")
            val listener = cache.getValueListener()
            resource.addSubQuery(subQuery, listener, true)
            frontListener = listener
        } else {
            Timber.d("Denied front listener for ${subQuery.spec}")
        }
    }

    @Synchronized
    override fun addEndListener(query: Query, subQuery: Query) {
        if (canAddEnd()) {
            Timber.d("Adding end listener for ${subQuery.spec}")
            val listener = cache.getValueListener()
            resource.addSubQuery(subQuery, listener, true)
            frontListener = listener
        } else {
            Timber.d("Denied end listener for ${subQuery.spec}")
        }
    }
}

/**
 * A [PagedList.BoundaryCallback] that increases the limit of its source [Query] to observe additional items
 * as they are requested.
 */
class FirebaseLivePagedListBoundaryCallback<LocalType>(
    private val query: Query,
    private val cache: FirebasePagedListCache<*, *>,
    private val resource: FirebaseScope.Resource,
    private val pagedListConfig: PagedList.Config,
    private val descending: Boolean = false
) : PagedList.BoundaryCallback<LocalType>() {

    private var activeQuery: Query? = null
    private var requestedLimit = 0

    private fun canAddListener(): Boolean {
        return cache.size >= requestedLimit
    }

    @Synchronized
    override fun onZeroItemsLoaded() {
        if (activeQuery != null) return
        createQuery()
    }

    private var activeListener: QueryCacheChildrenListener<*>? = null

    private fun createQuery() {
        cache.clear()
        requestedLimit += if (activeQuery == null)
            pagedListConfig.initialLoadSizeHint
        else
            pagedListConfig.pageSize

        val newQuery = if (descending) {
            query.limitToLast(requestedLimit)
        } else {
            query.limitToFirst(requestedLimit)
        }
        activeQuery = newQuery
        activeListener?.active = false
        activeListener = cache.getChildListener().also {
            resource.addSubQuery(newQuery, it)
        }
    }

    @Synchronized
    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        if (!canAddListener())
            return
        createQuery()
    }

    @Synchronized
    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
        return
        if (!canAddListener()) return
        createQuery()
    }
}

class FirebaseBoundaryCallback<LocalType : IFirebaseEntity>(
    private val query: Query,
    private val pagedListConfig: PagedList.Config,
    private val descending: Boolean,
    private val cache: IFirebaseLinkedListCollection<LocalType>,
    clazz: KClass<LocalType>

) : PagedList.BoundaryCallback<LocalType>() {

    private var activeQuery: Query? = null
    private var requestedLimit = 0

    private fun canAddListener(): Boolean {
        return cache.size >= requestedLimit
    }

    @Synchronized
    override fun onZeroItemsLoaded() {
        if (activeQuery != null) return
        createQuery()
    }

    private fun createQuery() {
        activeQuery?.removeEventListener(listener)

        requestedLimit += if (activeQuery == null)
            pagedListConfig.initialLoadSizeHint
        else
            pagedListConfig.pageSize * 5

        val newQuery = if (descending) {
            query.limitToLast(requestedLimit)
        } else {
            query.limitToFirst(requestedLimit)
        }
//        cache.clear(invalidate = false)

        activeQuery = newQuery
        newQuery.addChildEventListener(listener)
    }

    private val listener = QueryCacheChildrenListener2(cache, descending, clazz)

    @Synchronized
    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        if (!canAddListener())
            return
        createQuery()
    }

    @Synchronized
    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
        return
//        if (!canAddListener()) return
//        createQuery()
    }

    private class QueryCacheChildrenListener2<RemoteType : IFirebaseEntity>(
        private val cache: IFirebaseLinkedListCollection<RemoteType>,
        private val descending: Boolean,
        clazz: KClass<RemoteType>
    ) : FirebaseChildEventListener<RemoteType>(clazz) {

        override fun cancelled(error: DatabaseError) {
            Timber.e(error.toException())
        }

        override fun childMoved(previousChildName: String?, child: RemoteType) {
            Timber.d("called: childMoved($previousChildName, ${child.entityId})")
            cache.move(previousChildName, child, descending)
        }

        override fun childChanged(previousChildName: String?, child: RemoteType) {
            Timber.d("called: childChanged($previousChildName, ${child.entityId})")
            cache.update(previousChildName, child, descending)
        }

        @Synchronized
        override fun childAdded(previousChildName: String?, child: RemoteType) {
            Timber.d("called: childAdded($previousChildName, ${child.entityId})")
            cache.add(previousChildName, child, descending)
        }

        @Synchronized
        override fun childRemoved(child: RemoteType) {
            Timber.d("called: childRemoved(${child.entityId})")
            cache.remove(child)
        }
    }
}
