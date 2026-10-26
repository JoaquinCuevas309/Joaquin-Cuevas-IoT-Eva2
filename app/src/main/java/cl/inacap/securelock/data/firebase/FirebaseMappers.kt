package cl.inacap.securelock.data.firebase

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.data.model.Usuario

object FirebaseMappers {
    // sin latido en 30 s el locker figura sin conexión
    const val HEARTBEAT_TIMEOUT_MS = 30_000L
    // un comando pendiente deja de valer a los 20 s
    const val COMMAND_TTL_MS = 20_000L

    fun locker(id: String, raw: Map<*, *>?, now: Long): Locker? {
        raw ?: return null
        val latido = (raw["ultimoLatido"] as? Number)?.toLong()
        val pendiente = ((raw["comando"] as? Map<*, *>)?.get("pendienteDesde") as? Number)?.toLong()
        return Locker(
            id = id,
            nombre = raw["nombre"] as? String ?: id,
            estado = if ((raw["estado"] as? String).equals("ABIERTO", ignoreCase = true)) EstadoLocker.ABIERTO else EstadoLocker.CERRADO,
            conectado = latido != null && now - latido <= HEARTBEAT_TIMEOUT_MS,
            comandoPendienteDesde = pendiente?.takeIf { now - it <= COMMAND_TTL_MS },
            ultimoEvento = (raw["ultimoEvento"] as? Number)?.toLong(),
        )
    }

    fun acceso(id: String, lockerId: String, raw: Map<*, *>?): AccesoEvento? {
        raw ?: return null
        val tipo = runCatching { TipoAcceso.valueOf(raw["tipo"] as String) }.getOrNull() ?: return null
        val timestamp = (raw["timestamp"] as? Number)?.toLong() ?: return null
        return AccesoEvento(id, lockerId, tipo, raw["usuario"] as? String, timestamp)
    }

    fun lockersAsignados(raw: Any?): List<String> =
        (raw as? Map<*, *>)
            ?.filter { it.value == true }
            ?.keys
            ?.map { it.toString() }
            ?.sorted()
            .orEmpty()

    fun usuario(uid: String, raw: Map<*, *>?, nombreAuth: String?, emailAuth: String?): Usuario =
        Usuario(
            uid = uid,
            nombre = raw?.get("nombre") as? String ?: nombreAuth ?: emailAuth.orEmpty(),
            email = raw?.get("email") as? String ?: emailAuth.orEmpty(),
            lockersAsignados = lockersAsignados(raw?.get("lockers")),
        )

    fun listaAccesos(lockerId: String, raw: Any?): List<AccesoEvento> =
        (raw as? Map<*, *>)
            ?.mapNotNull { (key, value) -> acceso(key.toString(), lockerId, value as? Map<*, *>) }
            ?.sortedByDescending { it.timestamp }
            .orEmpty()
}
