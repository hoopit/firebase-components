package io.hoopit.firebasecomponents.lifecycle

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.FirebaseConnectionManager

class FirebasePagedListLiveData<Type>(
    private val query: Query,
    private val firebaseConnectionManager: FirebaseConnectionManager
) : BaseFirebaseLiveData<Type>() {
    override fun removeListener() = firebaseConnectionManager.deactivate(query)

    override fun addListener() = firebaseConnectionManager.activate(query)
}
