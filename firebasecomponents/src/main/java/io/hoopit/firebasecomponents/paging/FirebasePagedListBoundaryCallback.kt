package io.hoopit.firebasecomponents.paging

import androidx.paging.PagedList
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager
import timber.log.Timber

/***
 * [PagedList.BoundaryCallback] for Firebase list resources
 */
abstract class FirebasePagedListBoundaryCallback<LocalType, Key>(
    private val query: Query,
    private val sortKey: (LocalType) -> Key
//    private val initialCount: Int = 4,
//    private val nextCount: Int = 4,
//    private val previousCount: Int = 4
) : PagedList.BoundaryCallback<LocalType>() {

    override fun onZeroItemsLoaded() {
        addInitialListener(query)
    }

    override fun onItemAtEndLoaded(itemAtEnd: LocalType) {
        if (query.spec.loadsAllData()) return
        val q = if (query.spec.params.isViewFromLeft) startAt(query, itemAtEnd)
        else endAt(query, itemAtEnd)
        addEndListener(query, q)
    }

    override fun onItemAtFrontLoaded(itemAtFront: LocalType) {
        if (query.spec.loadsAllData()) return
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

/***
 * [PagedList.BoundaryCallback] for Firebase list resources
 */
class FirebaseManagedPagedListBoundaryCallback<LocalType, Key>(
    query: Query,
    sortKey: (LocalType) -> Key,
    private val firebaseConnectionManager: FirebaseConnectionManager,
    private val store: FirebasePagedListQueryCache<*, *>
        // TODO: Consider passing cache as argument, and use it to add listeners
) : FirebasePagedListBoundaryCallback<LocalType, Key>(query, sortKey) {

    private val limit = query.spec.params.limit

    private val lock = Any()

    private var initialListener: Listener<*>? = null
    private var frontListener: Listener<*>? = null
    private var endListener: Listener<*>? = null

    private fun isInitialComplete(): Boolean {
        return initialListener?.count == limit
    }

    private fun canAddFront(): Boolean {
        return isInitialComplete() && (frontListener == null || frontListener?.count == limit)
    }

    private fun canAddEnd(): Boolean {
        return isInitialComplete() && (endListener == null || endListener?.count == limit)
    }

    // TODO: add callback to disallow new listeners until current has fetched limit number of items
    override fun addInitialListener(query: Query) {
        synchronized(lock) {
            if (initialListener == null || isInitialComplete()) {
                Timber.d("Adding initial listener for ${query.spec}")
                initialListener = firebaseConnectionManager.addPagedListener(store, query)
            } else {
                Timber.d("Denied initial listener for ${query.spec}")
                assert(false)
            }
        }
    }

    override fun addFrontListener(query: Query, subQuery: Query) {
        synchronized(lock) {
            if (canAddFront()) {
                Timber.d("Adding front listener for ${query.spec}")
                frontListener = firebaseConnectionManager.addPagedListener(store, query, subQuery)
            } else {
                Timber.d("Denied front listener for ${query.spec}")
            }
        }
    }

    override fun addEndListener(query: Query, subQuery: Query) {
        synchronized(lock) {
            if (canAddEnd()) {
                Timber.d("Adding end listener for ${query.spec}")
                endListener = firebaseConnectionManager.addPagedListener(store, query, subQuery)
            } else {
                Timber.d("Denied end listener for ${query.spec}")
            }
        }
    }
}

