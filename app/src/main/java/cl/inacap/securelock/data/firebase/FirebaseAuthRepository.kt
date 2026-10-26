package cl.inacap.securelock.data.firebase

import cl.inacap.securelock.data.model.Usuario
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.AuthResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthRepository(
    scope: CoroutineScope,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
) : AuthRepository {

    private val authUid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override val currentUser: StateFlow<Usuario?> = authUid
        .flatMapLatest { uid ->
            if (uid == null) {
                flowOf(null)
            } else {
                db.getReference("usuarios/$uid").valueFlow().map { raw ->
                    val user = auth.currentUser
                    FirebaseMappers.usuario(uid, raw as? Map<*, *>, user?.displayName, user?.email)
                }
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun login(email: String, password: String): AuthResult =
        try {
            val uid = auth.signInWithEmailAndPassword(email.trim(), password).await().user?.uid
            if (uid == null) AuthResult.Error(MSG_GENERICO) else AuthResult.Success(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(mensaje(e))
        }

    // crea la cuenta y su perfil en la base
    override suspend fun register(nombre: String, email: String, password: String): AuthResult =
        try {
            val mail = email.trim().lowercase()
            val user = auth.createUserWithEmailAndPassword(mail, password).await().user
                ?: return AuthResult.Error(MSG_GENERICO)
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(nombre.trim()).build()).await()
            db.getReference("usuarios/${user.uid}")
                .setValue(mapOf("nombre" to nombre.trim(), "email" to mail))
                .await()
            AuthResult.Success(user.uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(mensaje(e))
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> =
        try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }

    override fun logout() = auth.signOut()

    // traduce errores de Firebase a mensajes de la app
    private fun mensaje(e: Exception): String = when (e) {
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> MSG_GENERICO
    }

    private companion object {
        const val MSG_GENERICO = "No se pudo completar la operación. Intenta nuevamente"
    }
}
