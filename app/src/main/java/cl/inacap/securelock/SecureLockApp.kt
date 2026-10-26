package cl.inacap.securelock

import android.app.Application
import android.content.Context
import cl.inacap.securelock.data.demo.DemoAuthRepository
import cl.inacap.securelock.data.demo.DemoBackend
import cl.inacap.securelock.data.demo.DemoLockerRepository
import cl.inacap.securelock.data.demo.PrefsUserStore
import cl.inacap.securelock.data.firebase.FirebaseAuthRepository
import cl.inacap.securelock.data.firebase.FirebaseLockerRepository
import cl.inacap.securelock.data.repository.AuthRepository
import cl.inacap.securelock.data.repository.DemoControls
import cl.inacap.securelock.data.repository.LockerRepository
import cl.inacap.securelock.util.AlertNotifier
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notifier = AlertNotifier(context)

    // Firebase si existe google-services.json; si no, modo demo
    val usaNube: Boolean = FirebaseApp.getApps(context).isNotEmpty()
    val authRepository: AuthRepository
    val lockerRepository: LockerRepository
    val demoControls: DemoControls?

    init {
        if (usaNube) {
            authRepository = FirebaseAuthRepository(appScope)
            lockerRepository = FirebaseLockerRepository()
            demoControls = null
        } else {
            val backend = DemoBackend(PrefsUserStore(context), appScope)
            val demoLockers = DemoLockerRepository(backend)
            authRepository = DemoAuthRepository(backend)
            lockerRepository = demoLockers
            demoControls = demoLockers
        }
        // notifica las alarmas de los lockers del usuario
        appScope.launch {
            authRepository.currentUser
                .map { it?.uid }
                .distinctUntilChanged()
                .flatMapLatest { uid -> if (uid == null) emptyFlow() else lockerRepository.observeAlarmas(uid) }
                .collect { alert -> notifier.notifyAlarm(alert.lockerId, alert.lockerNombre) }
        }
    }
}

class SecureLockApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
