package cl.inacap.securelock

import cl.inacap.securelock.data.firebase.FirebaseMappers
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.TipoAcceso
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseMappersTest {

    private val ahora = 1_000_000L

    private fun raw(latido: Long? = ahora - 5_000, estado: String = "CERRADO", pendiente: Long? = null): Map<String, Any?> =
        buildMap {
            put("nombre", "Locker 12")
            put("estado", estado)
            if (latido != null) put("ultimoLatido", latido)
            if (pendiente != null) put("comando", mapOf("pendienteDesde" to pendiente, "por" to "u1"))
        }

    @Test
    fun `un latido reciente marca el locker como conectado`() {
        val locker = FirebaseMappers.locker("L1", raw(), ahora)
        assertTrue(locker!!.conectado)
        assertEquals("Locker 12", locker.nombre)
        assertEquals(EstadoLocker.CERRADO, locker.estado)
    }

    @Test
    fun `un latido antiguo o ausente marca el locker sin conexion`() {
        assertFalse(FirebaseMappers.locker("L1", raw(latido = ahora - 31_000), ahora)!!.conectado)
        assertFalse(FirebaseMappers.locker("L1", raw(latido = null), ahora)!!.conectado)
    }

    @Test
    fun `un comando pendiente viejo se ignora`() {
        assertEquals(ahora - 3_000, FirebaseMappers.locker("L1", raw(pendiente = ahora - 3_000), ahora)!!.comandoPendienteDesde)
        assertNull(FirebaseMappers.locker("L1", raw(pendiente = ahora - 25_000), ahora)!!.comandoPendienteDesde)
    }

    @Test
    fun `el estado abierto se reconoce y un locker inexistente da nulo`() {
        assertEquals(EstadoLocker.ABIERTO, FirebaseMappers.locker("L1", raw(estado = "ABIERTO"), ahora)!!.estado)
        assertNull(FirebaseMappers.locker("L9", null, ahora))
    }

    @Test
    fun `los accesos se ordenan del mas nuevo al mas antiguo y se descartan los invalidos`() {
        val datos = mapOf(
            "a" to mapOf("tipo" to "APP", "usuario" to "Joaquin", "timestamp" to 100L),
            "b" to mapOf("tipo" to "ALARMA", "timestamp" to 300L),
            "c" to mapOf("tipo" to "OTRO", "timestamp" to 200L),
            "d" to mapOf("tipo" to "RFID"),
        )
        val lista = FirebaseMappers.listaAccesos("L1", datos)
        assertEquals(listOf("b", "a"), lista.map { it.id })
        assertEquals(TipoAcceso.ALARMA, lista.first().tipo)
        assertNull(lista.first().usuario)
    }

    @Test
    fun `solo cuentan los lockers asignados con valor verdadero`() {
        val raw = mapOf("lockers" to mapOf("L2" to true, "L1" to true, "L3" to false))
        val usuario = FirebaseMappers.usuario("u1", raw + mapOf("nombre" to "Ana", "email" to "ana@correo.cl"), null, null)
        assertEquals(listOf("L1", "L2"), usuario.lockersAsignados)
        assertEquals("Ana", usuario.nombre)
    }

    @Test
    fun `un usuario sin registro en la base usa los datos de la cuenta`() {
        val usuario = FirebaseMappers.usuario("u1", null, "Luis", "luis@correo.cl")
        assertEquals("Luis", usuario.nombre)
        assertEquals("luis@correo.cl", usuario.email)
        assertEquals(emptyList<String>(), usuario.lockersAsignados)
    }
}
