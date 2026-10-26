package cl.inacap.securelock.data.repository

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.AlarmAlert
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

sealed interface AuthResult {
    data class Success(val uid: String) : AuthResult
    data class Error(val message: String) : AuthResult
}

interface AuthRepository {
    val currentUser: StateFlow<Usuario?>
    suspend fun login(email: String, password: String): AuthResult
    suspend fun register(nombre: String, email: String, password: String): AuthResult
    suspend fun sendPasswordReset(email: String): Result<Unit>
    fun logout()
}

interface LockerRepository {
    fun observeLockers(uid: String): Flow<List<Locker>>
    fun observeLocker(lockerId: String): Flow<Locker?>
    fun observeAccesos(lockerId: String): Flow<List<AccesoEvento>>
    fun observeAccesosDeUsuario(uid: String): Flow<List<AccesoEvento>>
    fun observeAlarmas(uid: String): Flow<AlarmAlert> = emptyFlow()
    suspend fun requestOpen(lockerId: String, uid: String): Result<Unit>
}

interface DemoControls {
    suspend fun toggleConnection(lockerId: String)
    suspend fun simulateAlarm(lockerId: String)
    suspend fun simulateRfidAccess(lockerId: String, uid: String)
}
