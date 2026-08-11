package com.shoppew.android.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shoppew.android.core.api.AuthUser
import com.shoppew.android.data.ShoppewException
import com.shoppew.android.data.ShoppewRepository
import com.shoppew.android.ui.common.ActionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val restoring: Boolean = true,
    val user: AuthUser? = null,
    val action: ActionState = ActionState.Idle,
)

@HiltViewModel
class SessionViewModel @Inject constructor(private val repository: ShoppewRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        restore()
    }

    fun restore() {
        if (!repository.hasRefreshSession) {
            _uiState.value = SessionUiState(restoring = false)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(restoring = true) }
            repository.restoreSession().fold(
                onSuccess = { user -> _uiState.value = SessionUiState(restoring = false, user = user) },
                onFailure = { _uiState.value = SessionUiState(restoring = false) },
            )
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(action = ActionState.Error("Vui lòng nhập email và mật khẩu.")) }
            return
        }
        authenticate({ repository.login(email, password) }, onSuccess)
    }

    fun register(email: String, password: String, displayName: String, phone: String?, onSuccess: () -> Unit) {
        val errors = buildMap {
            if (displayName.trim().length < 2) put("displayName", "Tên hiển thị cần ít nhất 2 ký tự")
            if (!email.contains('@')) put("email", "Email chưa đúng định dạng")
            if (password.length < 10) put("password", "Mật khẩu cần ít nhất 10 ký tự")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(action = ActionState.Error("Vui lòng kiểm tra thông tin.", errors)) }
            return
        }
        authenticate({ repository.register(email, password, displayName, phone) }, onSuccess)
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = SessionUiState(restoring = false)
            onComplete()
        }
    }

    fun clearAction() = _uiState.update { it.copy(action = ActionState.Idle) }

    private fun authenticate(call: suspend () -> Result<AuthUser>, onSuccess: () -> Unit) {
        if (_uiState.value.action == ActionState.Pending) return
        viewModelScope.launch {
            _uiState.update { it.copy(action = ActionState.Pending) }
            call().fold(
                onSuccess = { user ->
                    _uiState.value = SessionUiState(restoring = false, user = user, action = ActionState.Success("Đăng nhập thành công"))
                    onSuccess()
                },
                onFailure = { error ->
                    val exception = error as? ShoppewException
                    _uiState.update {
                        it.copy(action = ActionState.Error(error.message ?: "Đăng nhập không thành công", exception?.fieldErrors.orEmpty()))
                    }
                },
            )
        }
    }
}
