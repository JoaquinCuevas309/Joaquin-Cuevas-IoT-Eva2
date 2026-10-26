package cl.inacap.securelock.data.firebase

import cl.inacap.securelock.data.model.AccesoEvento
import cl.inacap.securelock.data.model.AlarmAlert
import cl.inacap.securelock.data.model.EstadoLocker
import cl.inacap.securelock.data.model.Locker
import cl.inacap.securelock.data.repository.LockerRepository
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.tasks.await

// escucha en tiempo real como Flow
internal fun Query.valueFlow(): Flow<Any?> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot.value)
        }

        override fun onCancelled(error: DatabaseError) {
            trySend(null)
            close()
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseLockerRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val clock: () -> Long = { System.currentTimeMillis() },
) : LockerRepository {

    private val ticker: Flow<Unit> = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }

    private fun lockersAsignados(uid: String): Flow<List<String>> =
        db.getReference("usuarios/$uid/lockers").valueFlow()
            .map { FirebaseMappers.lockersAsignados(it) }
            .distinctUntilChanged()

    override fun observeLockers(uid: String): Flow<List<Locker>> =
        lockersAsignados(uid).flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else combine(ids.map { observeLocker(it) }) { list -> list.filterNotNull() }
        }

    // el ticker reevalúa la conexión aunque no lleguen cambios
    override fun observeLocker(lockerId: String): Flow<Locker?> =
        combine(db.getReference("lockers/$lockerId").valueFlow(), ticker) { raw, _ ->
            FirebaseMappers.locker(lockerId, raw as? Map<*, *>, clock())
        }

    override fun observeAccesos(lockerId: String): Flow<List<AccesoEvento>> =
        db.getReference("accesos/$lockerId").orderByChild("timestamp").limitToLast(HISTORY_LIMIT).valueFlow()
            .map { FirebaseMappers.listaAccesos(lockerId, it) }

    override fun observeAccesosDeUsuario(uid: String): Flow<List<AccesoEvento>> =
        lockersAsignados(uid).flatMapLatest { ids ->
            if (ids.isEmpty()) flowOf(emptyList())
            else combine(ids.map { observeAccesos(it) }) { listas ->
                listas.toList().flatten().sortedByDescending { it.timestamp }
            }
        }

    override fun observeAlarmas(uid: String): Flow<AlarmAlert> =
        lockersAsignados(uid).flatMapLatest { ids -> ids.map { alarmasDe(it) }.merge() }

    // solo alarmas nuevas
    private fun alarmasDe(lockerId: String): Flow<AlarmAlert> = callbackFlow {
        val nombre = runCatching {
            db.getReference("lockers/$lockerId/nombre").get().await().getValue(String::class.java)
        }.getOrNull() ?: lockerId
        val desde = clock() - ALARM_REPLAY_MS
        val query = db.getReference("accesos/$lockerId").orderByChild("timestamp").startAt(desde.toDouble())
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val raw = snapshot.value as? Map<*, *>
                if (raw?.get("tipo") == "ALARMA") trySend(AlarmAlert(lockerId, nombre))
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) = Unit
            override fun onCancelled(error: DatabaseError) {
                close()
            }
        }
        query.addChildEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    // valida el estado y escribe el comando con hora del servidor
    override suspend fun requestOpen(lockerId: String, uid: String): Result<Unit> =
        try {
            val snapshot = db.getReference("lockers/$lockerId").get().await()
            val locker = FirebaseMappers.locker(lockerId, snapshot.value as? Map<*, *>, clock())
                ?: throw IllegalStateException("Locker no encontrado")
            if (!locker.conectado) throw IllegalStateException("El locker no está conectado")
            if (locker.estado == EstadoLocker.ABIERTO || locker.comandoPendienteDesde != null) {
                throw IllegalStateException("El locker ya está abierto")
            }
            db.getReference("lockers/$lockerId/comando")
                .setValue(mapOf("pendienteDesde" to ServerValue.TIMESTAMP, "por" to uid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            Result.failure(e)
        } catch (e: Exception) {
            val permiso = e.message.orEmpty().contains("permission", ignoreCase = true)
            Result.failure(IllegalStateException(if (permiso) "No tienes acceso a este locker" else "No se pudo enviar el comando. Revisa tu conexión"))
        }

    private companion object {
        const val TICK_MS = 5_000L
        const val HISTORY_LIMIT = 100
        const val ALARM_REPLAY_MS = 5_000L
    }
}
