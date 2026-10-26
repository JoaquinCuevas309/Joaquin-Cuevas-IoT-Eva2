package cl.inacap.securelock.ui.lockerdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.DemoControls
import cl.inacap.securelock.data.repository.LockerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LockerDetailUiState(
    val locker: Locker? = null,
    val cargando: Boolean = true,
    val ultimosAccesos: List<AccesoEvento> = emptyList(),
    val botonHabilitado: Boolean = true,
    val verificandoConexion: Boolean = false,
    val error: String? = null,
    val esDemo: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class LockerDetailViewModel(
    private val lockerId: String,
    private val auth: AuthRepository,
    private val lockers: LockerRepository,
    private val demo: DemoControls? = null,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val cooldownMs: Long = 3_000,
    private val avisoConexionMs: Long = 2_000,
) : ViewModel() {

    private val enEspera = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    private val locker = lockers.observeLocker(lockerId)

    private val ahora = locker.flatMapLatest { l ->
        if (l?.comandoPendienteDesde == null) {
            flowOf(clock())
        } else {
            flow {
                while (true) {
                    emit(clock())
                    delay(500)
                }
            }
        }
    }

    val state: StateFlow<LockerDetailUiState> = combine(
        locker,
        lockers.observeAccesos(lockerId),
        enEspera,
        error,
        ahora,
    ) { l, accesos, espera, err, now ->
        val pendiente = l?.comandoPendienteDesde
        LockerDetailUiState(
            locker = l,
            cargando = false,
            ultimosAccesos = accesos.take(5),
            botonHabilitado = !espera && pendiente == null && l?.conectado == true,
            verificandoConexion = pendiente != null && now - pendiente > avisoConexionMs,
            error = err,
            esDemo = demo != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LockerDetailUiState(esDemo = demo != null))

    // bloquea el botón mientras dura la espera
    fun onAbrirClick() {
        if (enEspera.value) return
        val uid = auth.currentUser.value?.uid ?: return
        enEspera.value = true
        error.value = null
        viewModelScope.launch {
            lockers.requestOpen(lockerId, uid).onFailure { e ->
                error.value = e.message ?: "No se pudo enviar el comando"
            }
            delay(cooldownMs)
            enEspera.value = false
        }
    }

    fun descartarError() = error.update { null }

    fun alternarConexion() {
        viewModelScope.launch { demo?.toggleConnection(lockerId) }
    }

    fun simularAlarma() {
        viewModelScope.launch { demo?.simulateAlarm(lockerId) }
    }

    fun simularTarjeta() {
        val uid = auth.currentUser.value?.uid ?: return
        viewModelScope.launch { demo?.simulateRfidAccess(lockerId, uid) }
    }
}
