package cl.inacap.securelock

import cl.inacap.securelock.data.repository.AuthResult
import cl.inacap.securelock.ui.login.AuthMode
import cl.inacap.securelock.ui.login.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private fun viewModel(auth: FakeAuthRepository = FakeAuthRepository()) = LoginViewModel(auth)

    @Test
    fun `un correo invalido no llama al repositorio`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.onEmailChange("no-es-un-correo")
        vm.onPasswordChange("12345678")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Ingresa un correo válido", vm.state.value.error)
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun `una contrasena corta muestra el error de largo minimo`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.onEmailChange("joaquin@correo.cl")
        vm.onPasswordChange("1234567")
        vm.submit()
        advanceUntilIdle()
        assertEquals("La contraseña debe tener al menos 8 caracteres", vm.state.value.error)
        assertEquals(0, auth.loginCalls)
    }

    @Test
    fun `credenciales incorrectas muestran el mensaje y vuelven a habilitar el formulario`() = runTest {
        val auth = FakeAuthRepository().apply { loginResult = AuthResult.Error("Correo o contraseña incorrectos") }
        val vm = viewModel(auth)
        vm.onEmailChange("joaquin@correo.cl")
        vm.onPasswordChange("12345678")
        vm.submit()
        assertTrue(vm.state.value.isLoading)
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertEquals("Correo o contraseña incorrectos", vm.state.value.error)
        assertEquals(1, auth.loginCalls)
    }

    @Test
    fun `un login valido termina sin error`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.onEmailChange(" joaquin@correo.cl ")
        vm.onPasswordChange("12345678")
        vm.submit()
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertNull(vm.state.value.error)
        assertEquals(1, auth.loginCalls)
    }

    @Test
    fun `el registro exige el nombre`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.toggleMode()
        assertEquals(AuthMode.REGISTER, vm.state.value.mode)
        vm.onEmailChange("nuevo@correo.cl")
        vm.onPasswordChange("12345678")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Ingresa tu nombre", vm.state.value.error)
        assertEquals(0, auth.registerCalls)
        vm.onNombreChange("Nuevo Usuario")
        vm.onConfirmPasswordChange("12345678")
        vm.submit()
        advanceUntilIdle()
        assertEquals(1, auth.registerCalls)
    }

    @Test
    fun `el registro exige repetir la misma contrasena`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.toggleMode()
        vm.onNombreChange("Nuevo Usuario")
        vm.onEmailChange("nuevo@correo.cl")
        vm.onPasswordChange("12345678")
        vm.onConfirmPasswordChange("12345679")
        vm.submit()
        advanceUntilIdle()
        assertEquals("Las contraseñas no coinciden", vm.state.value.error)
        assertEquals(0, auth.registerCalls)
    }

    @Test
    fun `recuperar contrasena no revela si el correo existe`() = runTest {
        val auth = FakeAuthRepository()
        val vm = viewModel(auth)
        vm.onEmailChange("cualquiera@correo.cl")
        vm.resetPassword()
        advanceUntilIdle()
        assertEquals(1, auth.resetCalls)
        assertNotNull(vm.state.value.info)
        assertNull(vm.state.value.error)
    }
}
