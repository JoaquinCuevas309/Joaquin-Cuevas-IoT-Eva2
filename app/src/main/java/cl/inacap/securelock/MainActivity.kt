package cl.inacap.securelock

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.inacap.securelock.navigation.SecureLockNavHost
import cl.inacap.securelock.ui.theme.SecureLockTheme

class MainActivity : ComponentActivity() {

    private var pendingLockerId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingLockerId = intent?.getStringExtra(EXTRA_LOCKER_ID)
        val container = (application as SecureLockApp).container
        setContent {
            SecureLockTheme {
                SecureLockNavHost(
                    container = container,
                    pendingLockerId = pendingLockerId,
                    onPendingConsumed = { pendingLockerId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_LOCKER_ID)?.let { pendingLockerId = it }
    }

    companion object {
        const val EXTRA_LOCKER_ID = "lockerId"
    }
}
