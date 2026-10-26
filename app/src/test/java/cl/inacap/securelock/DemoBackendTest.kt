package cl.inacap.securelock

import cl.inacap.securelock.data.demo.DemoBackend
import cl.inacap.securelock.data.demo.InMemoryUserStore
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.data.repository.AuthResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoBackendTest {

    private fun TestScope.backend(store: InMemoryUserStore = InMemoryUserStore()) =
        DemoBackend(store, backgroundScope, clock = { testScheduler.currentTime + 10_000_000 }, latencyMs = 0)

    @Test
    fun `iniciar sesion con la cuenta demo entrega el usuario`() = runTest {
        val backend = backend()
        val result = backend.login("DEMO@securelock.cl", "demo12345")
        assertTrue(result is AuthResult.Success)
        runCurrent()
        assertEquals("Joaquín Demo", backend.currentUser.value?.nombre)
    }

    @Test
    fun `una contrasena incorrecta no abre sesion`() = runTest {
        val backend = backend()
        val result = backend.login("demo@securelock.cl", "otra-clave-1")
        assertEquals(AuthResult.Error("Correo o contraseña incorrectos"), result)
        runCurrent()
        assertNull(backend.currentUser.value)
    }

    @Test
    fun `no se puede registrar un correo que ya existe`() = runTest {
        val backend = backend()
        val result = backend.register("Otro", "demo@securelock.cl", "clave-larga-1")
        assertEquals(AuthResult.Error("Ya existe una cuenta con ese correo"), result)
    }

    @Test
    fun `un usuario nuevo parte sin lockers asignados`() = runTest {
        val backend = backend()
        backend.register("Nuevo", "nuevo@correo.cl", "clave-larga-1")
        runCurrent()
        assertEquals(emptyList<String>(), backend.currentUser.value?.lockersAsignados)
    }

    @Test
    fun `la sesion persiste entre instancias`() = runTest {
        val store = InMemoryUserStore()
        backend(store).login("demo@securelock.cl", "demo12345")
        val second = backend(store)
        assertNotNull(second.currentUser.value)
    }

    @Test
    fun `abrir un locker pasa por pendiente, abierto y vuelve a cerrado`() = runTest {
        val backend = backend()
        backend.login("demo@securelock.cl", "demo12345")
        assertTrue(backend.requestOpen("L1", "demo-1").isSuccess)
        assertNotNull(backend.observeLocker("L1").first()?.comandoPendienteDesde)

        advanceTimeBy(900)
        runCurrent()
        val abierto = backend.observeLocker("L1").first()
        assertEquals(EstadoLocker.ABIERTO, abierto?.estado)
        assertNull(abierto?.comandoPendienteDesde)
        assertEquals(TipoAcceso.APP, backend.observeAccesos("L1").first().first().tipo)

        advanceTimeBy(3_100)
        runCurrent()
        assertEquals(EstadoLocker.CERRADO, backend.observeLocker("L1").first()?.estado)
    }

    @Test
    fun `un locker sin conexion no se puede abrir hasta que se reconecte`() = runTest {
        val backend = backend()
        backend.login("demo@securelock.cl", "demo12345")
        assertEquals(false, backend.observeLocker("L2").first()?.conectado)
        assertTrue(backend.requestOpen("L2", "demo-1").isFailure)

        backend.toggleConnection("L2")
        runCurrent()
        assertEquals(true, backend.observeLocker("L2").first()?.conectado)
        assertTrue(backend.requestOpen("L2", "demo-1").isSuccess)
    }

    @Test
    fun `el historial general solo incluye los lockers del usuario`() = runTest {
        val backend = backend()
        val ids = backend.observeAccesosDeUsuario("demo-1").first().map { it.lockerId }.toSet()
        assertEquals(setOf("L1", "L2"), ids)
    }

    @Test
    fun `no se puede abrir un locker ajeno`() = runTest {
        val backend = backend()
        assertTrue(backend.requestOpen("L3", "demo-1").isFailure)
    }

    @Test
    fun `la alarma queda registrada y se emite el aviso`() = runTest {
        val backend = backend()
        var avisos = 0
        backgroundScope.launch { backend.alarms.collect { avisos++ } }
        runCurrent()
        backend.simulateAlarm("L1")
        runCurrent()
        assertEquals(1, avisos)
        assertEquals(TipoAcceso.ALARMA, backend.observeAccesos("L1").first().first().tipo)
    }
}
