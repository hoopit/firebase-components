package io.hoopit.firebasecomponents.core

import com.google.firebase.database.Query

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
interface IFirebaseEntity {
    var entityId: String

    object OrderBy {

        fun key(item: IFirebaseEntity) = item.entityId

    }
}

abstract class FirebaseResource(val disconnectDelay: Long) : IFirebaseEntity {
    override lateinit var entityId: String
    lateinit var scope: Scope
    lateinit var query: Query
}

interface IFirebaseQuery {
    val disconnectDelay: Long
    val sourceQuery: Query
}
