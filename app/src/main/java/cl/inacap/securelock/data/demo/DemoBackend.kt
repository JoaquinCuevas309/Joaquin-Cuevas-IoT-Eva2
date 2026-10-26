package cl.inacap.securelock.data.demo

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.AlarmAlert
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.model.TipoAcceso
import cl.inacap.securelock.data.model.Usuario
import cl.inacap.securelock.data.repository.AuthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class DemoBackend(
    private val userStore: UserStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val latencyMs: Long = 600,
    private val pickupDelayMs: Long = 800,
    private val autoCloseMs: Long = 3000,
) {
    private val users = MutableStateFlow(userStore.loadUsers() ?: seedUsers().also { userStore.saveUsers(it) })
    private val session = MutableStateFlow(userStore.loadSession())
    private val lockers = MutableStateFlow(seedLockers())
    private val events = MutableStateFlow(seedEvents())
    private val eventCounter = AtomicInteger(events.value.size)
    private val alarmFlow = MutableSharedFlow<AlarmAlert>(extraBufferCapacity = 8)

    val alarms: SharedFlow<AlarmAlert> = alarmFlow.asSharedFlow()

    val currentUser: StateFlow<Usuario?> =
        combine(session, users) { uid, list -> resolveUser(uid, list) }
            .stateIn(scope, SharingStarted.Eagerly, resolveUser(session.value, users.value))

    suspend fun login(email: String, password: String): AuthResult {
        delay(latencyMs)
        val user = users.value.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        val valid = user != null &&
            MessageDigest.isEqual(hash(password, user.salt).toByteArray(), user.hash.toByteArray())
        if (user == null || !valid) return AuthResult.Error("Correo o contraseña incorrectos")
        setSession(user.uid)
        return AuthResult.Success(user.uid)
    }

    suspend fun register(nombre: String, email: String, password: String): AuthResult {
        delay(latencyMs)
        val mail = email.trim().lowercase()
        if (users.value.any { it.email.equals(mail, ignoreCase = true) }) {
            return AuthResult.Error("Ya existe una cuenta con ese correo")
        }
        val salt = newSalt()
        val user = StoredUser(UUID.randomUUID().toString(), nombre.trim(), mail, emptyList(), salt, hash(password, salt))
        val updated = users.value + user
        users.value = updated
        userStore.saveUsers(updated)
        setSession(user.uid)
        return AuthResult.Success(user.uid)
    }

    suspend fun sendPasswordReset(@Suppress("UNUSED_PARAMETER") email: String): Result<Unit> {
        delay(latencyMs)
        return Result.success(Unit)
    }

    fun logout() = setSession(null)

    fun observeLockers(uid: String): Flow<List<Locker>> =
        combine(users, lockers) { userList, lockerList ->
            val assigned = userList.firstOrNull { it.uid == uid }?.lockers.orEmpty()
            assigned.mapNotNull { id -> lockerList.firstOrNull { it.id == id } }
        }

    fun observeAlarmas(uid: String): Flow<AlarmAlert> =
        alarms.filter { alert -> alert.lockerId in users.value.firstOrNull { it.uid == uid }?.lockers.orEmpty() }

    fun observeLocker(lockerId: String): Flow<Locker?> =
        lockers.map { list -> list.firstOrNull { it.id == lockerId } }

    fun observeAccesos(lockerId: String): Flow<List<AccesoEvento>> =
        events.map { list -> list.filter { it.lockerId == lockerId }.sortedByDescending { it.timestamp } }

    fun observeAccesosDeUsuario(uid: String): Flow<List<AccesoEvento>> =
        combine(users, events) { userList, eventList ->
            val assigned = userList.firstOrNull { it.uid == uid }?.lockers.orEmpty()
            eventList.filter { it.lockerId in assigned }.sortedByDescending { it.timestamp }
        }

    suspend fun toggleConnection(lockerId: String) {
        updateLocker(lockerId) { it.copy(conectado = !it.conectado) }
    }

    // comprueba acceso y conexión antes de abrir
    fun requestOpen(lockerId: String, uid: String): Result<Unit> {
        val user = users.value.firstOrNull { it.uid == uid }
            ?: return Result.failure(IllegalStateException("Sesión no válida"))
        if (lockerId !in user.lockers) return Result.failure(IllegalStateException("No tienes acceso a este locker"))
        val locker = lockers.value.firstOrNull { it.id == lockerId }
            ?: return Result.failure(IllegalStateException("Locker no encontrado"))
        if (!locker.conectado) return Result.failure(IllegalStateException("El locker no está conectado"))
        if (locker.estado == EstadoLocker.ABIERTO || locker.comandoPendienteDesde != null) {
            return Result.failure(IllegalStateException("El locker ya está abierto"))
        }
        updateLocker(lockerId) { it.copy(comandoPendienteDesde = clock()) }
        scope.launch {
            delay(pickupDelayMs)
            runOpenCycle(lockerId, TipoAcceso.APP, user.nombre)
        }
        return Result.success(Unit)
    }

    suspend fun simulateRfidAccess(lockerId: String, uid: String) {
        val locker = lockers.value.firstOrNull { it.id == lockerId } ?: return
        if (locker.estado == EstadoLocker.ABIERTO || !locker.conectado) return
        val nombre = users.value.firstOrNull { it.uid == uid }?.nombre
        scope.launch { runOpenCycle(lockerId, TipoAcceso.RFID, nombre) }
    }

    suspend fun simulateAlarm(lockerId: String) {
        val locker = lockers.value.firstOrNull { it.id == lockerId } ?: return
        addEvent(lockerId, TipoAcceso.ALARMA, null)
        updateLocker(lockerId) { it.copy(ultimoEvento = clock()) }
        alarmFlow.tryEmit(AlarmAlert(lockerId, locker.nombre))
    }

    // abre, espera y cierra solo
    private suspend fun runOpenCycle(lockerId: String, tipo: TipoAcceso, usuario: String?) {
        updateLocker(lockerId) {
            it.copy(estado = EstadoLocker.ABIERTO, comandoPendienteDesde = null, ultimoEvento = clock())
        }
        addEvent(lockerId, tipo, usuario)
        delay(autoCloseMs)
        updateLocker(lockerId) { it.copy(estado = EstadoLocker.CERRADO) }
    }

    private fun updateLocker(lockerId: String, change: (Locker) -> Locker) {
        lockers.update { list -> list.map { if (it.id == lockerId) change(it) else it } }
    }

    private fun addEvent(lockerId: String, tipo: TipoAcceso, usuario: String?) {
        val event = AccesoEvento("e${eventCounter.incrementAndGet()}", lockerId, tipo, usuario, clock())
        events.update { listOf(event) + it }
    }

    private fun setSession(uid: String?) {
        session.value = uid
        userStore.saveSession(uid)
    }

    private fun resolveUser(uid: String?, list: List<StoredUser>): Usuario? =
        list.firstOrNull { it.uid == uid }?.toUsuario()

    private fun seedUsers(): List<StoredUser> {
        fun user(uid: String, nombre: String, email: String, password: String, lockerIds: List<String>): StoredUser {
            val salt = newSalt()
            return StoredUser(uid, nombre, email, lockerIds, salt, hash(password, salt))
        }
        return listOf(
            user("demo-1", "Joaquín Demo", "demo@securelock.cl", "demo12345", listOf("L1", "L2")),
            user("demo-2", "Administrador", "admin@securelock.cl", "admin12345", listOf("L1", "L2", "L3")),
        )
    }

    private fun seedLockers(): List<Locker> {
        val now = clock()
        return listOf(
            Locker("L1", "Locker 12 · Gimnasio Centro", ultimoEvento = now - 2 * HOUR),
            Locker("L2", "Locker 07 · Gimnasio Centro", conectado = false, ultimoEvento = now - 5 * HOUR),
            Locker("L3", "Locker 03 · Gimnasio Norte", ultimoEvento = now - 2 * DAY),
        )
    }

    private fun seedEvents(): List<AccesoEvento> {
        val now = clock()
        return listOf(
            AccesoEvento("e1", "L1", TipoAcceso.RFID, "Joaquín Demo", now - 2 * HOUR),
            AccesoEvento("e2", "L1", TipoAcceso.APP, "Joaquín Demo", now - 1 * DAY),
            AccesoEvento("e3", "L1", TipoAcceso.ALARMA, null, now - 3 * DAY),
            AccesoEvento("e4", "L2", TipoAcceso.RFID, "Joaquín Demo", now - 5 * HOUR),
            AccesoEvento("e5", "L3", TipoAcceso.APP, "Administrador", now - 2 * DAY),
        )
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY = 24 * HOUR

        // SHA-256 con sal (solo modo demo)
        fun hash(password: String, salt: String): String =
            MessageDigest.getInstance("SHA-256").digest((salt + password).toByteArray())
                .joinToString("") { "%02x".format(it) }

        fun newSalt(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
