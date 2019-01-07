package io.hoopit.firebasecomponents.ext

import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.hoopit.firebasecomponents.core.Scope
import io.hoopit.firebasecomponents.lifecycle.FirebaseListLiveData
import io.hoopit.firebasecomponents.lifecycle.FirebaseValueLiveData

public fun <V> updatesOf(vararg pairs: Pair<Query, V>): Map<String, V> {
    return mapOf(*pairs.map { (first, second) -> Pair(first.path.toString(), second) }.toTypedArray())
}

public fun <V> DatabaseReference.updateChildren(vararg pairs: Pair<Query, V>): Task<Void> {
    return this.updateChildren(updatesOf(*pairs))
}


fun Query.addChildEventListener(listener: ChildEventListener, scope: Scope) {
    scope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addValueEventListener(listener: ValueEventListener, scope: Scope) {
    scope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addListenerForSingleValueEvent(listener: ValueEventListener, scope: Scope) {
    scope.getResource(this).apply {
        addListener(listener, once = true)
        dispatchActivate()
    }
}

inline fun <reified T : Any> Query.liveData(disconnectDelay: Long = 0): FirebaseValueLiveData<T> {
    return FirebaseValueLiveData(this, T::class, disconnectDelay)
}
