package cl.inacap.securelock

import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.ui.lockerdetail.LockerDetailViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LockerDetailViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `abrir deshabilita el boton durante 3 segundos`() = runTest {
        val repo = FakeLockerRepository()
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertTrue(vm.state.value.botonHabilitado)

        vm.onAbrirClick()
        runCurrent()
        assertEquals(1, repo.openCalls)
        assertFalse(vm.state.value.botonHabilitado)

        advanceTimeBy(2_900)
        runCurrent()
        assertFalse(vm.state.value.botonHabilitado)

        advanceTimeBy(200)
        runCurrent()
        assertTrue(vm.state.value.botonHabilitado)
    }

    @Test
    fun `un segundo toque durante la espera no envia otro comando`() = runTest {
        val repo = FakeLockerRepository()
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()

        vm.onAbrirClick()
        vm.onAbrirClick()
        runCurrent()
        assertEquals(1, repo.openCalls)
    }

    @Test
    fun `un fallo del repositorio se muestra como error`() = runTest {
        val repo = FakeLockerRepository().apply { openResult = Result.failure(IllegalStateException("No tienes acceso a este locker")) }
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()

        vm.onAbrirClick()
        runCurrent()
        assertEquals("No tienes acceso a este locker", vm.state.value.error)
    }

    @Test
    fun `un comando pendiente por mas de 2 segundos muestra verificando conexion`() = runTest {
        val repo = FakeLockerRepository(Locker("L1", "Locker 1", comandoPendienteDesde = 0))
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertFalse(vm.state.value.verificandoConexion)

        advanceTimeBy(2_600)
        runCurrent()
        assertTrue(vm.state.value.verificandoConexion)
        assertFalse(vm.state.value.botonHabilitado)
    }

    @Test
    fun `un locker sin conexion no permite abrir`() = runTest {
        val repo = FakeLockerRepository(Locker("L1", "Locker 1", conectado = false))
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertFalse(vm.state.value.botonHabilitado)
    }

    @Test
    fun `solo se muestran los ultimos 5 accesos`() = runTest {
        val repo = FakeLockerRepository()
        repo.accesos.value = (1..8).map {
            cl.inacap.securelock.data.model.AccesoEvento("e$it", "L1", cl.inacap.securelock.data.model.TipoAcceso.APP, "Joaquín", it * 1_000L)
        }
        val vm = LockerDetailViewModel("L1", FakeAuthRepository(usuarioDePrueba), repo, clock = { testScheduler.currentTime })
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        runCurrent()
        assertEquals(5, vm.state.value.ultimosAccesos.size)
    }
}
