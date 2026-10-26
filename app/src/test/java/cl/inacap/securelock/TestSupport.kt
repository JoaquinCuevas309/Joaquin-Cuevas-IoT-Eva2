package cl.inacap.securelock

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.model.Usuario
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.AuthResult
import cl.inacap.securelock.data.repository.LockerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = StandardTestDispatcher()) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

val usuarioDePrueba = Usuario("u1", "Joaquín", "joaquin@correo.cl", listOf("L1"))

class FakeAuthRepository(user: Usuario? = null) : AuthRepository {
    private val user = MutableStateFlow(user)
    override val currentUser: StateFlow<Usuario?> get() = user

    var loginResult: AuthResult = AuthResult.Success("u1")
    var registerResult: AuthResult = AuthResult.Success("u2")
    var loginCalls = 0
    var registerCalls = 0
    var resetCalls = 0

    override suspend fun login(email: String, password: String): AuthResult {
        loginCalls++
        return loginResult
    }

    override suspend fun register(nombre: String, email: String, password: String): AuthResult {
        registerCalls++
        return registerResult
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        resetCalls++
        return Result.success(Unit)
    }

    override fun logout() {
        user.value = null
    }
}

class FakeLockerRepository(locker: Locker? = Locker("L1", "Locker 1")) : LockerRepository {
    val locker = MutableStateFlow(locker)
    val accesos = MutableStateFlow<List<AccesoEvento>>(emptyList())
    var openCalls = 0
    var openResult: Result<Unit> = Result.success(Unit)

    override fun observeLockers(uid: String): Flow<List<Locker>> = locker.map { listOfNotNull(it) }
    override fun observeLocker(lockerId: String): Flow<Locker?> = locker
    override fun observeAccesos(lockerId: String): Flow<List<AccesoEvento>> = accesos
    override fun observeAccesosDeUsuario(uid: String): Flow<List<AccesoEvento>> = accesos

    override suspend fun requestOpen(lockerId: String, uid: String): Result<Unit> {
        openCalls++
        return openResult
    }
}
