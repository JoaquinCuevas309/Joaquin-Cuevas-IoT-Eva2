package cl.inacap.securelock.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.LockerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class RangoFecha(val etiqueta: String, val duracionMs: Long?) {
    TODO("Todo", null),
    ULTIMAS_24_H("24 h", 24 * 3_600_000L),
    ULTIMOS_7_DIAS("7 días", 7 * 24 * 3_600_000L),
}

data class HistoryUiState(
    val subtitulo: String = "",
    val eventos: List<AccesoEvento> = emptyList(),
    val nombresLocker: Map<String, String> = emptyMap(),
    val esGlobal: Boolean = false,
    val tipos: Set<TipoAcceso> = emptySet(),
    val rango: RangoFecha = RangoFecha.TODO,
    val cargando: Boolean = true,
)

private data class Fuente(val subtitulo: String, val eventos: List<AccesoEvento>, val nombres: Map<String, String>)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val lockerId: String?,
    auth: AuthRepository,
    lockers: LockerRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {

    private val tipos = MutableStateFlow<Set<TipoAcceso>>(emptySet())
    private val rango = MutableStateFlow(RangoFecha.TODO)

    private val fuente: Flow<Fuente> = auth.currentUser.flatMapLatest { user ->
        when {
            lockerId != null -> combine(lockers.observeLocker(lockerId), lockers.observeAccesos(lockerId)) { locker, eventos ->
                Fuente(locker?.nombre.orEmpty(), eventos, emptyMap())
            }
            user == null -> flowOf(Fuente("", emptyList(), emptyMap()))
            else -> combine(lockers.observeLockers(user.uid), lockers.observeAccesosDeUsuario(user.uid)) { lista, eventos ->
                Fuente("Todos mis lockers", eventos, lista.associate { it.id to it.nombre })
            }
        }
    }

    val state: StateFlow<HistoryUiState> = combine(fuente, tipos, rango) { origen, tiposActivos, rangoActivo ->
        val desde = rangoActivo.duracionMs?.let { clock() - it }
        val filtrados = origen.eventos.filter { evento ->
            (tiposActivos.isEmpty() || evento.tipo in tiposActivos) && (desde == null || evento.timestamp >= desde)
        }
        HistoryUiState(origen.subtitulo, filtrados, origen.nombres, lockerId == null, tiposActivos, rangoActivo, cargando = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState(esGlobal = lockerId == null))

    fun toggleTipo(tipo: TipoAcceso) = tipos.update { if (tipo in it) it - tipo else it + tipo }

    fun setRango(value: RangoFecha) {
        rango.value = value
    }
}
