package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.connectivity.ConnectivityObserver
import com.shoppew.android.core.connectivity.ConnectivityStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ConnectivityViewModel @Inject constructor(
    observer: ConnectivityObserver,
) : ViewModel() {
    val status: StateFlow<ConnectivityStatus> = observer.status.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = observer.status.value,
    )
}
