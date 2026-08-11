package com.shoppew.android.ui.common

sealed interface LoadState<out T> {
    data object Idle : LoadState<Nothing>
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

sealed interface ActionState {
    data object Idle : ActionState
    data object Pending : ActionState
    data class Success(val message: String) : ActionState
    data class Error(val message: String, val fieldErrors: Map<String, String> = emptyMap()) : ActionState
}
