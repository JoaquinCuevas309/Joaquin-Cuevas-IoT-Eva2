package cl.inacap.securelock.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class LoginUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val esDemo: Boolean = true,
)

class LoginViewModel(private val auth: AuthRepository, esDemo: Boolean = true) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState(esDemo = esDemo))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onNombreChange(value: String) = _state.update { it.copy(nombre = value, error = null, info = null) }
    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null, info = null) }
    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null, info = null) }
    fun onConfirmPasswordChange(value: String) = _state.update { it.copy(confirmPassword = value, error = null, info = null) }

    fun toggleMode() = _state.update {
        it.copy(mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN, confirmPassword = "", error = null, info = null)
    }

    fun fillDemoCredentials() = _state.update {
        it.copy(mode = AuthMode.LOGIN, email = DEMO_EMAIL, password = DEMO_PASSWORD, error = null, info = null)
    }

    fun submit() {
        val current = _state.value
        if (current.isLoading) return
        val problem = validate(current)
        if (problem != null) {
            _state.update { it.copy(error = problem) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null, info = null) }
        viewModelScope.launch {
            val result = when (current.mode) {
                AuthMode.LOGIN -> auth.login(current.email.trim(), current.password)
                AuthMode.REGISTER -> auth.register(current.nombre, current.email.trim(), current.password)
            }
            _state.update {
                when (result) {
                    is AuthResult.Success -> it.copy(isLoading = false)
                    is AuthResult.Error -> it.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun resetPassword() {
        val current = _state.value
        if (current.isLoading) return
        if (!isValidEmail(current.email)) {
            _state.update { it.copy(error = MSG_EMAIL) }
            return
        }
        _state.update { it.copy(isLoading = true, error = null, info = null) }
        viewModelScope.launch {
            val result = auth.sendPasswordReset(current.email.trim())
            _state.update {
                if (result.isSuccess) {
                    it.copy(isLoading = false, info = "Si el correo está registrado, te enviamos un enlace para restablecer la contraseña")
                } else {
                    it.copy(isLoading = false, error = "No pudimos enviar el correo. Intenta nuevamente")
                }
            }
        }
    }

    // largo mínimo y confirmación al registrarse
    private fun validate(s: LoginUiState): String? = when {
        s.mode == AuthMode.REGISTER && s.nombre.isBlank() -> "Ingresa tu nombre"
        !isValidEmail(s.email) -> MSG_EMAIL
        s.password.length < MIN_PASSWORD -> "La contraseña debe tener al menos $MIN_PASSWORD caracteres"
        s.mode == AuthMode.REGISTER && s.password != s.confirmPassword -> "Las contraseñas no coinciden"
        else -> null
    }

    companion object {
        const val MIN_PASSWORD = 8
        const val DEMO_EMAIL = "demo@securelock.cl"
        const val DEMO_PASSWORD = "demo12345"
        private const val MSG_EMAIL = "Ingresa un correo válido"
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun isValidEmail(email: String) = EMAIL_REGEX.matches(email.trim())
    }
}
