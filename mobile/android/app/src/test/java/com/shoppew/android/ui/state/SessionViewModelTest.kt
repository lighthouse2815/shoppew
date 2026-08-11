package com.shoppew.android.ui.state

import com.shoppew.android.core.api.AuthUser
import com.shoppew.android.test.MainDispatcherRule
import com.shoppew.android.test.TestShoppewRepository
import com.shoppew.android.ui.common.ActionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `login exposes authenticated user and duplicate safe success state`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository().apply {
            loginResult = Result.success(AuthUser(id = "user-1", email = "buyer@example.test", displayName = "Người mua"))
        }
        val viewModel = SessionViewModel(repository)

        viewModel.login("buyer@example.test", "correct-password") {}
        advanceUntilIdle()

        assertEquals("user-1", viewModel.uiState.value.user?.id)
        assertTrue(viewModel.uiState.value.action is ActionState.Success)
        assertEquals(1, repository.loginCalls)
        assertFalse(viewModel.uiState.value.restoring)
    }

    @Test
    fun `invalid registration is rejected locally with field errors`() = runTest(mainDispatcherRule.dispatcher) {
        val repository = TestShoppewRepository()
        val viewModel = SessionViewModel(repository)

        viewModel.register("invalid", "short", "A", null) {}

        val error = viewModel.uiState.value.action as ActionState.Error
        assertEquals(setOf("displayName", "email", "password"), error.fieldErrors.keys)
        assertEquals(0, repository.loginCalls)
    }
}
