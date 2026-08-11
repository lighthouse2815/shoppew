package com.shoppew.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.shoppew.android.ui.common.ActionState
import com.shoppew.android.ui.common.InlineMessage
import com.shoppew.android.ui.state.SessionViewModel

@Composable
fun AuthRequiredScreen(message: String, onLogin: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("Phiên đăng nhập giúp bảo vệ giỏ hàng và dữ liệu cá nhân của bạn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Đăng nhập") }
    }
}

@Composable
fun LoginScreen(viewModel: SessionViewModel, onBack: () -> Unit, onRegister: () -> Unit, onSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.clearAction() }
    AuthContainer("Đăng nhập", "Tiếp tục mua sắm và theo dõi đơn hàng.", onBack) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().testTag("login-email").semantics { contentType = ContentType.EmailAddress },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            singleLine = true,
            isError = state.action is ActionState.Error && email.isBlank(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            modifier = Modifier.fillMaxWidth().testTag("login-password").semantics { contentType = ContentType.Password },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            singleLine = true,
        )
        ActionFeedback(state.action)
        Button(
            onClick = { viewModel.login(email, password, onSuccess) },
            enabled = state.action != ActionState.Pending && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("login-submit"),
        ) {
            if (state.action == ActionState.Pending) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp) else Text("Đăng nhập")
        }
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Chưa có tài khoản? Đăng ký") }
    }
}

@Composable
fun RegisterScreen(viewModel: SessionViewModel, onBack: () -> Unit, onLogin: () -> Unit, onSuccess: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    val passwordMismatch = confirm.isNotEmpty() && password != confirm
    LaunchedEffect(Unit) { viewModel.clearAction() }
    AuthContainer("Tạo tài khoản", "Thông tin này dùng cho giao hàng và hỗ trợ đơn mua.", onBack) {
        OutlinedTextField(displayName, { displayName = it }, label = { Text("Tên hiển thị *") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(email, { email = it }, label = { Text("Email *") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("Số điện thoại") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
        OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu *") }, supportingText = { Text("Ít nhất 10 ký tự, nên gồm chữ hoa, chữ thường, số và ký hiệu.") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(confirm, { confirm = it }, label = { Text("Nhập lại mật khẩu *") }, isError = passwordMismatch, supportingText = { if (passwordMismatch) Text("Mật khẩu chưa khớp") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
        ActionFeedback(state.action)
        Button(
            onClick = { viewModel.register(email, password, displayName, phone.takeIf(String::isNotBlank), onSuccess) },
            enabled = state.action != ActionState.Pending && !passwordMismatch && displayName.isNotBlank() && email.isNotBlank() && password.length >= 10,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.action == ActionState.Pending) CircularProgressIndicator(Modifier.height(22.dp), strokeWidth = 2.dp) else Text("Tạo tài khoản")
        }
        TextButton(onClick = onLogin, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Đã có tài khoản? Đăng nhập") }
    }
}

@Composable
private fun AuthContainer(title: String, subtitle: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") }
            Text("shoppew", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
    }
}

@Composable
private fun ActionFeedback(action: ActionState) {
    when (action) {
        is ActionState.Error -> InlineMessage(action.message, true)
        is ActionState.Success -> InlineMessage(action.message, false)
        else -> Unit
    }
}
