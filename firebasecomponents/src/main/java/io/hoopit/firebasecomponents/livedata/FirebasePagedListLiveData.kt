package io.hoopit.firebasecomponents.livedata

import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.FirebaseConnectionManager

class FirebasePagedListLiveData<Type>(
    private val query: Query,
    private val firebaseConnectionManager: FirebaseConnectionManager
) : FirebaseLiveData<Type>() {
    override fun removeListener() = firebaseConnectionManager.deactivate(query)

    override fun addListener() = firebaseConnectionManager.activate(query)
}
