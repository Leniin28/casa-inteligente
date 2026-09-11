package com.ecore.demo2.feature.auth.login

import com.ecore.demo2.core.util.AppException
import com.ecore.demo2.testutil.FakeAuthRepository
import com.ecore.demo2.testutil.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = FakeAuthRepository()
    // lazy: el ViewModel debe crearse después de que la regla instale el dispatcher de test.
    private val viewModel by lazy { LoginViewModel(authRepository) }

    @Test
    fun `invalid email is rejected without calling the repository`() {
        viewModel.onEmailChange("no-es-un-email")
        viewModel.onPasswordChange("demo1234")
        viewModel.login()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(0, authRepository.loginCalls)
    }

    @Test
    fun `short password is rejected`() {
        viewModel.onEmailChange("demo@casa.local")
        viewModel.onPasswordChange("123")
        viewModel.login()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals(0, authRepository.loginCalls)
    }

    @Test
    fun `valid credentials create a session`() {
        viewModel.onEmailChange("demo@casa.local")
        viewModel.onPasswordChange("demo1234")
        viewModel.login()

        val state = viewModel.uiState.value
        assertEquals(1, authRepository.loginCalls)
        assertNull(state.error)
        assertFalse(state.isLoading)
        assertEquals("demo@casa.local", authRepository.sessionFlow.value?.user?.email)
    }

    @Test
    fun `repository errors are shown to the user`() {
        authRepository.error = AppException("Email o contraseña incorrectos.")
        viewModel.onEmailChange("demo@casa.local")
        viewModel.onPasswordChange("demo1234")
        viewModel.login()

        assertEquals("Email o contraseña incorrectos.", viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
