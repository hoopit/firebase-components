package io.hoopit.android.firebaserealtime.model

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query
import io.hoopit.android.firebaserealtime.core.FirebaseResource
import io.hoopit.android.firebaserealtime.core.IFirebaseEntity
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseListLiveData
import io.hoopit.android.firebaserealtime.lifecycle.FirebaseValueLiveData
import io.hoopit.android.firebaserealtime.paging.FirebaseDataSourceFactory
import kotlin.reflect.KProperty

inline fun <reified T : Any> FirebaseResource.firebaseValue(
    disconnectDelay: Long = this.disconnectDelay,
    crossinline ref: () -> Query
): Lazy<LiveData<T?>> {
    return lazy {
        scope.cache.getItemCache(T::class).getLiveData(ref(), disconnectDelay)
    }
}

inline fun <K : Comparable<K>, reified T : FirebaseResource> FirebaseResource.firebaseList(
    noinline orderKeyFunction: (T) -> K,
    disconnectDelay: Long = this.disconnectDelay,
    crossinline query: () -> Query
): Lazy<LiveData<List<T>>> {
    return lazy {
        scope.cache.getOrCreateListCache(query(), T::class, orderKeyFunction).getLiveData(disconnectDelay)
    }
}

inline fun <K : Comparable<K>, reified T : FirebaseResource> FirebaseResource.firebaseCachedPagedList(
    disconnectDelay: Long,
    noinline orderKeyFunction: (T) -> K,
    crossinline query: () -> Query
): Lazy<FirebaseDataSourceFactory<K, T>> {
    return lazy {
        scope.cache.getOrCreatePagedCache(query(), T::class, orderKeyFunction).getDataSourceFactory()
    }
}

inline fun <reified T : Any> firebaseValue(
    disconnectDelay: Long,
    crossinline ref: () -> Query
): Lazy<LiveData<T?>> {
    return lazy {
        FirebaseValueLiveData(ref(), T::class, disconnectDelay)
    }
}

inline fun <K : Comparable<K>, reified T : IFirebaseEntity> firebaseList(
    disconnectDelay: Long,
    noinline orderKeyFunction: (T) -> K,
    crossinline query: () -> Query
): Lazy<FirebaseListLiveData<K, T>> {
    return lazy {
        FirebaseListLiveData(
            query(),
            T::class,
            orderKeyFunction = orderKeyFunction,
            disconnectDelay = disconnectDelay
        )
    }
}

fun <T, V> Lazy<T>.map(func: (T) -> V): Lazy<V> {
    return LazyMapperDelegate(this, func)
}

fun <T, V> Lazy<T>.map2(func: (T) -> V): LazyMapper<T, V> {
    return LazyMapper(this, func)
}

class LazyMapperDelegate<T, L>(private val prop: Lazy<L>, private val func: (L) -> T) : Lazy<T> {
    override val value: T
        get() = func(prop.value)

    override fun isInitialized() = prop.isInitialized()
}

class LazyMapper<T, V>(private val lazy: Lazy<T>, private val func: (T) -> V) {
    operator fun provideDelegate(thisRef: Any, prop: KProperty<*>): Lazy<V> {
        return lazy { func(lazy.value) }
    }
}
