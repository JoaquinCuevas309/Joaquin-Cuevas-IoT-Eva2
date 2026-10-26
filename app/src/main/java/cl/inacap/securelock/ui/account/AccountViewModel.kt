package cl.inacap.securelock.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.securelock.data.model.Usuario
import cl.inacap.securelock.data.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AccountViewModel(private val auth: AuthRepository) : ViewModel() {

    val usuario: StateFlow<Usuario?> = auth.currentUser
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), auth.currentUser.value)

    fun logout() = auth.logout()
}
