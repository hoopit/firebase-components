package io.hoopit.android.firebaserealtime.ext

import com.google.android.gms.tasks.Task
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseValueLiveData

fun <V> updatesOf(vararg pairs: Pair<Query, V>): Map<String, V> {
    return mapOf(*pairs.map { (first, second) -> Pair(first.path.toString(), second) }.toTypedArray())
}

fun <V> DatabaseReference.updateChildren(vararg pairs: Pair<Query, V>): Task<Void> {
    return this.updateChildren(updatesOf(*pairs))
}

fun Query.addChildEventListener(listener: ChildEventListener, scope: io.hoopit.android.firebaserealtime.core.Scope) {
    scope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addValueEventListener(listener: ValueEventListener, scope: io.hoopit.android.firebaserealtime.core.Scope) {
    scope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addListenerForSingleValueEvent(
    listener: ValueEventListener,
    scope: io.hoopit.android.firebaserealtime.core.Scope
) {
    scope.getResource(this).apply {
        addListener(listener, once = true)
        dispatchActivate()
    }
}

inline fun <reified T : Any> Query.liveData(disconnectDelay: Long = 0): FirebaseValueLiveData<T> {
    return FirebaseValueLiveData(this, T::class, disconnectDelay)
}
