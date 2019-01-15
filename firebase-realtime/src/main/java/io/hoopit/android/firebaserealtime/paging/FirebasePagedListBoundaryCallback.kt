package io.hoopit.android.firebaserealtime.paging

import androidx.paging.PagedList
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.Scope
import timber.log.Timber

/***
 * [PagedList.BoundaryCallback] for Firebase list resources
 */
abstract class FirebasePagedListBoundaryCallback<LocalType, Key>(
    private val query: Query,
    private val sortKey: (LocalType) -> Key
) : PagedList.BoundaryCallback<LocalType>() {

    override fun onZeroItemsLoaded() {
        Timber.d("called: onZeroItemsLoaded: ${query.spec}")
        addInitialListener(query)
    }

    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        Timber.d("called: onItemAtEndLoaded: ${query.spec}")
        return
        if (query.spec.loadsAllData()) {
            Timber.d("onItemAtEndLoaded: Ignored: Query loads all data.")
            return
        }
        val q = if (query.spec.params.isViewFromLeft) startAt(query, itemAtEnd)
        else endAt(query, itemAtEnd)
        addEndListener(query, q)
    }

    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
        Timber.d("called: onItemAtFrontLoaded: ${query.spec}")
        return
        if (query.spec.loadsAllData()) {
//            Timber.d("onItemAtFrontLoaded: Ignored: Query loads all data.")
            return
        }
        if (!query.spec.params.hasStart()) {
//            Timber.d("onItemAtFrontLoaded: Ignored: No start specified.")
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
//    private val cache: FirebasePagedListQueryCache<*, *>,
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

class FirebaseSimplePagedListBoundaryCallback<LocalType, Key>(
    query: Query,
    sortKey: (LocalType) -> Key,
    private val cache: FirebasePagedListQueryCache<*, *>,
    private val resource: Scope.Resource,
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
