package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.Query

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
interface IFirebaseEntity {
    var entityId: String
}

abstract class FirebaseResource(val disconnectDelay: Long) : IFirebaseEntity {
    override lateinit var entityId: String
    lateinit var scope: Scope
    lateinit var query: Query

    fun init(scope: Scope, query: Query) {
        this.scope = scope
        this.query = query
    }
}

interface IFirebaseQuery {
    val disconnectDelay: Long
    val sourceQuery: Query
}
