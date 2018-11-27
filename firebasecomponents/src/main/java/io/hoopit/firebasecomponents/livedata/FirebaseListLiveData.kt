package io.hoopit.firebasecomponents.livedata

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.FirebaseChildEventListener
import io.hoopit.firebasecomponents.IFirebaseListEntity
import io.hoopit.firebasecomponents.pagedlist.FirebaseCollection
import kotlin.reflect.KClass

@Suppress("unused")
class FirebaseListLiveData<K : Comparable<K>, T : IFirebaseListEntity>(
    private val query: Query,
    private val classModel: KClass<out T>,
    orderKeyFunction: (T) -> K
) : FirebaseLiveData<Collection<T>>() {

    private val collection = FirebaseCollection(orderKeyFunction, query.spec.params.isViewFromLeft)

    val listener = object : FirebaseChildEventListener<T>(classModel = classModel) {

        override fun cancelled(error: DatabaseError) {
            TODO("not implemented")
        }

        override fun childMoved(previousChildName: String?, child: T) {
            TODO("not implemented")
        }

        override fun childChanged(previousChildName: String?, child: T) {
            collection.update(previousChildName, child)
        }

        override fun childAdded(previousChildName: String?, child: T) {
            collection.addAfter(previousChildName, child)
            postValue(collection)
        }

        override fun childRemoved(child: T) {
            collection.remove(child)
            postValue(collection)
        }
    }

    override fun addListener() {
        query.addChildEventListener(listener)
    }

    override fun removeListener() {
        query.removeEventListener(listener)
    }
}
