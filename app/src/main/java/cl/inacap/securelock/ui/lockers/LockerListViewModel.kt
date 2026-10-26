package cl.inacap.securelock.ui.lockers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.LockerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LockerListUiState(
    val nombreUsuario: String = "",
    val lockers: List<Locker> = emptyList(),
    val cargando: Boolean = true,
    val refrescando: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LockerListViewModel(
    auth: AuthRepository,
    lockers: LockerRepository,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<LockerListUiState> = combine(
        auth.currentUser.flatMapLatest { user ->
            if (user == null) {
                flowOf(LockerListUiState(cargando = false))
            } else {
                lockers.observeLockers(user.uid).map { LockerListUiState(user.nombre, it, cargando = false) }
            }
        },
        refreshing,
    ) { base, isRefreshing -> base.copy(refrescando = isRefreshing) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockerListUiState())

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            delay(600)
            refreshing.value = false
        }
    }
}
