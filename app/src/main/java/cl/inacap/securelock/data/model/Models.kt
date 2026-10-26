package cl.inacap.securelock.data.model

data class Usuario(
    val uid: String,
    val nombre: String,
    val email: String,
    val lockersAsignados: List<String>,
)

enum class EstadoLocker { ABIERTO, CERRADO }

data class Locker(
    val id: String,
    val nombre: String,
    val estado: EstadoLocker = EstadoLocker.CERRADO,
    val conectado: Boolean = true,
    val comandoPendienteDesde: Long? = null,
    val ultimoEvento: Long? = null,
)

enum class TipoAcceso { RFID, APP, ALARMA }

data class AccesoEvento(
    val id: String,
    val lockerId: String,
    val tipo: TipoAcceso,
    val usuario: String?,
    val timestamp: Long,
)

data class AlarmAlert(val lockerId: String, val lockerNombre: String)
