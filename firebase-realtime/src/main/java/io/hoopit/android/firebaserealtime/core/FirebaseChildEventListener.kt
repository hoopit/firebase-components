package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlin.reflect.KClass

abstract class FirebaseChildEventListener<T : io.hoopit.android.firebaserealtime.core.IFirebaseEntity>(
    private val classModel: KClass<out T>
) : ChildEventListener {

    final override fun onCancelled(error: DatabaseError) {
        cancelled(error)
    }

    final override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValue(classModel.java)?.let {
            it.entityId = requireNotNull(snapshot.key)
            childMoved(previousChildName, it)
        }
    }

    final override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValue(classModel.java)?.let {
            it.entityId = requireNotNull(snapshot.key)
            childChanged(previousChildName, it)
        }
    }

    final override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValue(classModel.java)?.let {
            it.entityId = requireNotNull(snapshot.key)
            childAdded(previousChildName, it)
        }
    }

    final override fun onChildRemoved(snapshot: DataSnapshot) {
        snapshot.getValue(classModel.java)?.let {
            it.entityId = requireNotNull(snapshot.key)
            childRemoved(it)
        }
    }

    abstract fun cancelled(error: DatabaseError)

    abstract fun childMoved(previousChildName: String?, child: T)

    abstract fun childChanged(previousChildName: String?, child: T)

    abstract fun childAdded(previousChildName: String?, child: T)

    abstract fun childRemoved(child: T)
}
