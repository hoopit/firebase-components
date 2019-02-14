package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
interface IFirebaseEntity {
    var entityId: String
    val ref: DatabaseReference
    fun init(snapshot: DataSnapshot)
}

/**
 * Interface for firebase object.
 * [entityId] holds the firebase key of the object.
 */
abstract class FirebaseEntity : IFirebaseEntity {
    override lateinit var entityId: String
    override lateinit var ref: DatabaseReference

    override fun init(snapshot: DataSnapshot) {
        entityId = snapshot.key ?: ""
        ref = snapshot.ref
    }

    override fun toString(): String = entityId
}

interface IFirebaseResource {
    val disconnectDelay: Long
}

/**
 * A resource that is composed of one or more child resources.
 */
abstract class FirebaseCompositeResource(
    override val disconnectDelay: Long
) : IFirebaseResource, FirebaseEntity() {

    fun with(ref: DatabaseReference) {
        this.entityId = ref.key ?: ""
        this.ref = ref
    }
}

/**
 * An enhanced version of FirebaseEntity, which enables scopes and composite resources.
 */
abstract class FirebaseScopedResource(override val disconnectDelay: Long) : FirebaseEntity(), IFirebaseResource {
    lateinit var firebaseScope: FirebaseScope
        private set
    lateinit var query: Query
        private set
    override lateinit var ref: DatabaseReference

    fun init(firebaseScope: FirebaseScope, query: Query) {
        this.firebaseScope = firebaseScope
        this.query = query
        ref = if (query is DatabaseReference) query else query.ref.child(entityId)
    }
}
