package io.hoopit.firebasecomponents.paging

import androidx.paging.PagedList
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager

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
    private val initialListener: ChildEventListener?,
    private val frontListener: ChildEventListener?,
    private val endListener: ChildEventListener?,
    private val firebaseConnectionManager: FirebaseConnectionManager
) : FirebasePagedListBoundaryCallback<LocalType, Key>(query, sortKey) {
    override fun addInitialListener(query: Query) {
        if (initialListener == null) return
        firebaseConnectionManager.addListener(query, initialListener)
    }

    override fun addFrontListener(query: Query, subQuery: Query) {
        if (frontListener == null) return
        firebaseConnectionManager.addListener(query, subQuery, frontListener)
    }

    override fun addEndListener(query: Query, subQuery: Query) {
        if (endListener == null) return
        firebaseConnectionManager.addListener(query, subQuery, endListener)
    }
}
