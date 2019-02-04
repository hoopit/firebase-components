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

fun Query.addChildEventListener(
    listener: ChildEventListener,
    firebaseScope: io.hoopit.android.firebaserealtime.core.FirebaseScope
) {
    firebaseScope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addValueEventListener(
    listener: ValueEventListener,
    firebaseScope: io.hoopit.android.firebaserealtime.core.FirebaseScope
) {
    firebaseScope.getResource(this).apply {
        addListener(listener)
        dispatchActivate()
    }
}

fun Query.addListenerForSingleValueEvent(
    listener: ValueEventListener,
    firebaseScope: io.hoopit.android.firebaserealtime.core.FirebaseScope
) {
    firebaseScope.getResource(this).apply {
        addListener(listener, once = true)
        dispatchActivate()
    }
}

inline fun <reified T : Any> Query.asLiveData(disconnectDelay: Long = 0): FirebaseValueLiveData<T> {
    return FirebaseValueLiveData(this, T::class, disconnectDelay)
}
