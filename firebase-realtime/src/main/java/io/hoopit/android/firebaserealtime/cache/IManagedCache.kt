package io.hoopit.android.firebaserealtime.cache

import androidx.lifecycle.LiveData
import com.google.firebase.database.Query

interface IManagedCache {
    fun onInactive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun onActive(firebaseCacheLiveData: LiveData<*>, query: Query)
    fun dispose()
}
