package cl.inacap.securelock

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.ui.history.HistoryViewModel
import cl.inacap.securelock.ui.history.RangoFecha
import cl.inacap.securelock.util.relativeTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryAndTimeTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    private val hora = 3_600_000L
    private val ahora = 100 * 24 * hora

    private val eventos = listOf(
        AccesoEvento("e1", "L1", TipoAcceso.RFID, "Joaquín", ahora - 2 * hora),
        AccesoEvento("e2", "L1", TipoAcceso.APP, "Joaquín", ahora - 30 * hora),
        AccesoEvento("e3", "L1", TipoAcceso.ALARMA, null, ahora - 10 * 24 * hora),
    )

    @Test
    fun `el historial filtra por tipo y por rango`() = runTest {
        val repo = FakeLockerRepository().apply { accesos.value = eventos }
        val vm = HistoryViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { ahora })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(3, vm.state.value.eventos.size)

        vm.toggleTipo(TipoAcceso.ALARMA)
        runCurrent()
        assertEquals(listOf("e3"), vm.state.value.eventos.map { it.id })

        vm.toggleTipo(TipoAcceso.ALARMA)
        vm.setRango(RangoFecha.ULTIMAS_24_H)
        runCurrent()
        assertEquals(listOf("e1"), vm.state.value.eventos.map { it.id })

        vm.setRango(RangoFecha.ULTIMOS_7_DIAS)
        runCurrent()
        assertEquals(listOf("e1", "e2"), vm.state.value.eventos.map { it.id })
    }

    @Test
    fun `el historial general reune los accesos de todos los lockers del usuario`() = runTest {
        val repo = FakeLockerRepository().apply { accesos.value = eventos }
        val vm = HistoryViewModel(null, FakeAuthRepository(usuarioDePrueba), repo, clock = { ahora })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(true, vm.state.value.esGlobal)
        assertEquals("Todos mis lockers", vm.state.value.subtitulo)
        assertEquals(3, vm.state.value.eventos.size)
        assertEquals("Locker 1", vm.state.value.nombresLocker["L1"])
    }

    @Test
    fun `el tiempo relativo usa unidades legibles`() {
        val base = 1_000_000_000L
        assertEquals("justo ahora", relativeTime(base - 30_000, base))
        assertEquals("hace 5 min", relativeTime(base - 5 * 60_000, base))
        assertEquals("hace 3 h", relativeTime(base - 3 * hora, base))
        assertEquals("hace 2 d", relativeTime(base - 2 * 24 * hora, base))
    }
}
