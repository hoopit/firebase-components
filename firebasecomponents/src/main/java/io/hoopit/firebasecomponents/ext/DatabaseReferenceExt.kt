package io.hoopit.firebasecomponents.ext

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query

public fun <V> updatesOf(vararg pairs: Pair<Query, V>): Map<String, V> {
    return mapOf(*pairs.map { (first, second) -> Pair(first.path.toString(), second) }.toTypedArray())
}

public fun <V> DatabaseReference.updateChildren(vararg pairs: Pair<Query, V>): Task<Void> {
    return this.updateChildren(updatesOf(*pairs))
}

