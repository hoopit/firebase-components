package io.hoopit.firebasecomponents.model

import androidx.lifecycle.LiveData
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.Query
import io.hoopit.firebasecomponents.core.IFirebaseEntity
import io.hoopit.firebasecomponents.core.ManagedFirebaseEntity
import io.hoopit.firebasecomponents.lifecycle.FirebaseListLiveData
import kotlin.reflect.KClass

fun <K : Comparable<K>, T : IFirebaseEntity> ManagedFirebaseEntity.firebaseList(
    classModel: KClass<T>,
    orderKeyFunction: (T) -> K,
    query: () -> Query
): Lazy<FirebaseListLiveData<K, T>> {
    return lazy {
        FirebaseListLiveData(
                query(),
                classModel,
                orderKeyFunction = orderKeyFunction
        )
    }
}

fun <T : Any> ManagedFirebaseEntity.fbLiveValue(
    clazz: KClass<T>,
    disconnectDelay: Long,
    ref: () -> DatabaseReference
): Lazy<LiveData<T>> {
    TODO()
//    return lazy {
//        referenceManager.cacheManager.getCache(clazz, disconnectDelay).getLiveData(ref())
//    }
}

fun <T : Any> ManagedFirebaseEntity.fbScopedValue(
    clazz: KClass<T>,
    ref: () -> DatabaseReference
): Lazy<LiveData<T>> {
    return lazy {
        cache.getCache(clazz).getLiveData(ref(), resource)
    }
}


inline fun <reified T : Any> ManagedFirebaseEntity.firebaseValue(
    crossinline ref: () -> DatabaseReference
): Lazy<LiveData<T>> {
    return lazy {
        cache.getCache(T::class).getLiveData(ref())
    }
}
