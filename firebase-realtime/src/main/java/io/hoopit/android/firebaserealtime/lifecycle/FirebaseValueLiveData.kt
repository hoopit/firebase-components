package io.hoopit.android.firebaserealtime.lifecycle

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import io.hoopit.android.common.livedata.DelayedDisconnectLiveData
import kotlin.reflect.KClass

class FirebaseValueLiveData<T : Any>(
    private val reference: Query,
    private val classModel: KClass<T>,
    disconnectDelay: Long = 2000
) : DelayedDisconnectLiveData<T>(disconnectDelay), ValueEventListener {
    override fun delayedOnActive() {
        reference.addValueEventListener(this)
    }

    override fun delayedOnInactive() = reference.removeEventListener(this)

    override fun onCancelled(error: DatabaseError) {
        TODO("not implemented")
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        value = snapshot.getValue(classModel.java)
    }
}

