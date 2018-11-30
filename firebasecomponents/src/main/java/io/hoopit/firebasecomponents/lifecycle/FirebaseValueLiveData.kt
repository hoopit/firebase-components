package io.hoopit.firebasecomponents.lifecycle

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlin.reflect.KClass

class FirebaseValueLiveData<T : Any>(
    private val reference: DatabaseReference,
    private val classModel: KClass<T>,
    disconnectDelay: Long = 2000
) : DelayedTransitionLiveData<T>(disconnectDelay), ValueEventListener {
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
