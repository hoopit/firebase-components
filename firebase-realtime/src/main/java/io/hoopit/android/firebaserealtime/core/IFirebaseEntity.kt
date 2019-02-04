package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
interface IFirebaseEntity {
    var entityId: String
}

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
abstract class FirebaseEntity : IFirebaseEntity {
    override lateinit var entityId: String
}

/**
 * An enhanced version of FirebaseEntity, which enables scopes and composite resources.
 */
abstract class FirebaseResource(val disconnectDelay: Long) : FirebaseEntity() {
    lateinit var firebaseScope: FirebaseScope
        private set
    lateinit var query: Query
        private set
    lateinit var ref: DatabaseReference
        private set

    fun init(firebaseScope: FirebaseScope, query: Query) {
        this.firebaseScope = firebaseScope
        this.query = query
        ref = if (query is DatabaseReference) query else query.ref.child(entityId)
    }
}

interface IFirebaseQuery {
    val disconnectDelay: Long
    val sourceQuery: Query
}
