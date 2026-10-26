package cl.inacap.securelock.data.demo

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.AlarmAlert
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.model.Usuario
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.AuthResult
import cl.inacap.securelock.data.repository.DemoControls
import cl.inacap.securelock.data.repository.LockerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DemoAuthRepository(private val backend: DemoBackend) : AuthRepository {
    override val currentUser: StateFlow<Usuario?> get() = backend.currentUser
    override suspend fun login(email: String, password: String): AuthResult = backend.login(email, password)
    override suspend fun register(nombre: String, email: String, password: String): AuthResult =
        backend.register(nombre, email, password)

    override suspend fun sendPasswordReset(email: String): Result<Unit> = backend.sendPasswordReset(email)
    override fun logout() = backend.logout()
}

class DemoLockerRepository(private val backend: DemoBackend) : LockerRepository, DemoControls {
    override fun observeLockers(uid: String): Flow<List<Locker>> = backend.observeLockers(uid)
    override fun observeLocker(lockerId: String): Flow<Locker?> = backend.observeLocker(lockerId)
    override fun observeAccesos(lockerId: String): Flow<List<AccesoEvento>> = backend.observeAccesos(lockerId)
    override fun observeAccesosDeUsuario(uid: String): Flow<List<AccesoEvento>> = backend.observeAccesosDeUsuario(uid)
    override fun observeAlarmas(uid: String): Flow<AlarmAlert> = backend.observeAlarmas(uid)
    override suspend fun requestOpen(lockerId: String, uid: String): Result<Unit> = backend.requestOpen(lockerId, uid)
    override suspend fun toggleConnection(lockerId: String) = backend.toggleConnection(lockerId)
    override suspend fun simulateAlarm(lockerId: String) = backend.simulateAlarm(lockerId)
    override suspend fun simulateRfidAccess(lockerId: String, uid: String) = backend.simulateRfidAccess(lockerId, uid)
}
