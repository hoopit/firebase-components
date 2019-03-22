package io.hoopit.android.firebaserealtime.core

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import io.hoopit.android.firebaserealtime.ext.getValueOrNull
import kotlin.reflect.KClass

abstract class FirebaseChildEventListener<T : Any>(
    private val classModel: KClass<T>
) : ChildEventListener {

    final override fun onCancelled(error: DatabaseError) {
        cancelled(error)
    }

    final override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValueOrNull(classModel)?.let {
            if (it is IFirebaseEntity) it.init(snapshot)
            childMoved(previousChildName, it)
        }
    }

    final override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValueOrNull(classModel)?.let {
            if (it is IFirebaseEntity) it.init(snapshot)
            childChanged(previousChildName, it)
        }
    }

    final override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
        snapshot.getValueOrNull(classModel)?.let {
            if (it is IFirebaseEntity) it.init(snapshot)
            childAdded(previousChildName, it)
        }
    }

    final override fun onChildRemoved(snapshot: DataSnapshot) {
        snapshot.getValueOrNull(classModel)?.let {
            if (it is IFirebaseEntity) it.init(snapshot)
            childRemoved(it)
        }
    }

    open fun cancelled(error: DatabaseError) {}

    abstract fun childMoved(previousChildName: String?, child: T)

    abstract fun childChanged(previousChildName: String?, child: T)

    abstract fun childAdded(previousChildName: String?, child: T)

    abstract fun childRemoved(child: T)
}
