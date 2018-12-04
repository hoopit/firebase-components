package io.hoopit.firebasecomponents.core

import com.google.firebase.database.Query

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
interface IFirebaseEntity {
    var entityId: String

    object orderBy {

        fun key(item: IFirebaseEntity) = item.entityId

    }
}

abstract class ManagedFirebaseEntity : IFirebaseEntity {
    lateinit var resource: Scope.Resource
    lateinit var cache: CacheManager
}

interface IFirebaseQuery {
    val disconnectDelay: Long
    val sourceQuery: Query
}
