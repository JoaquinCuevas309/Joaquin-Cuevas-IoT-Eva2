package cl.inacap.securelock.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.inacap.securelock.AppContainer
import cl.inacap.securelock.ui.account.AccountScreen
import cl.inacap.securelock.ui.account.AccountViewModel
import cl.inacap.securelock.ui.history.HistoryScreen
import cl.inacap.securelock.ui.history.HistoryViewModel
import cl.inacap.securelock.ui.lockerdetail.LockerDetailScreen
import cl.inacap.securelock.ui.lockerdetail.LockerDetailViewModel
import cl.inacap.securelock.ui.lockers.LockerListScreen
import cl.inacap.securelock.ui.lockers.LockerListViewModel
import cl.inacap.securelock.ui.login.LoginScreen
import cl.inacap.securelock.ui.login.LoginViewModel

object Routes {
    const val LOGIN = "login"
    const val LOCKERS = "lockers"
    const val HISTORY_ALL = "historial"
    const val ACCOUNT = "cuenta"
    const val DETAIL = "locker/{lockerId}"
    const val HISTORY = "history/{lockerId}"
    fun detail(lockerId: String) = "locker/$lockerId"
    fun history(lockerId: String) = "history/$lockerId"
}

private const val ARG_LOCKER = "lockerId"

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.LOCKERS, "Lockers", Icons.AutoMirrored.Filled.List),
    Tab(Routes.HISTORY_ALL, "Historial", Icons.Filled.DateRange),
    Tab(Routes.ACCOUNT, "Cuenta", Icons.Filled.Person),
)

@Composable
fun SecureLockNavHost(
    container: AppContainer,
    pendingLockerId: String?,
    onPendingConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val user by container.authRepository.currentUser.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val rutaActual = backStack?.destination?.route
    val logueado = user != null
    val inicio = if (logueado) Routes.LOCKERS else Routes.LOGIN

    LaunchedEffect(logueado) {
        val actual = navController.currentDestination?.route
        if (!logueado && actual != Routes.LOGIN) {
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
        } else if (logueado && actual == Routes.LOGIN) {
            navController.navigate(Routes.LOCKERS) { popUpTo(Routes.LOGIN) { inclusive = true } }
        }
    }

    LaunchedEffect(pendingLockerId, user) {
        val lockerId = pendingLockerId ?: return@LaunchedEffect
        val actual = user ?: return@LaunchedEffect
        // solo abre lockers asignados al usuario
        if (lockerId in actual.lockersAsignados) navController.navigate(Routes.detail(lockerId))
        onPendingConsumed()
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (logueado && tabs.any { it.route == rutaActual }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = rutaActual == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.LOCKERS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = inicio,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(factory = viewModelFactory {
                    initializer { LoginViewModel(container.authRepository, esDemo = !container.usaNube) }
                })
                LoginScreen(vm)
            }
            composable(Routes.LOCKERS) {
                val vm: LockerListViewModel = viewModel(factory = viewModelFactory {
                    initializer { LockerListViewModel(container.authRepository, container.lockerRepository) }
                })
                LockerListScreen(vm, onLockerClick = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.HISTORY_ALL) {
                val vm: HistoryViewModel = viewModel(key = "history-all", factory = viewModelFactory {
                    initializer { HistoryViewModel(null, container.authRepository, container.lockerRepository) }
                })
                HistoryScreen(vm)
            }
            composable(Routes.ACCOUNT) {
                val vm: AccountViewModel = viewModel(factory = viewModelFactory {
                    initializer { AccountViewModel(container.authRepository) }
                })
                AccountScreen(vm)
            }
            composable(Routes.DETAIL, arguments = listOf(navArgument(ARG_LOCKER) { type = NavType.StringType })) { entry ->
                val lockerId = entry.arguments?.getString(ARG_LOCKER).orEmpty()
                val vm: LockerDetailViewModel = viewModel(key = "detail-$lockerId", factory = viewModelFactory {
                    initializer {
                        LockerDetailViewModel(lockerId, container.authRepository, container.lockerRepository, container.demoControls)
                    }
                })
                LockerDetailScreen(
                    vm,
                    onBack = { navController.popBackStack() },
                    onVerHistorial = { navController.navigate(Routes.history(lockerId)) },
                )
            }
            composable(Routes.HISTORY, arguments = listOf(navArgument(ARG_LOCKER) { type = NavType.StringType })) { entry ->
                val lockerId = entry.arguments?.getString(ARG_LOCKER).orEmpty()
                val vm: HistoryViewModel = viewModel(key = "history-$lockerId", factory = viewModelFactory {
                    initializer { HistoryViewModel(lockerId, container.authRepository, container.lockerRepository) }
                })
                HistoryScreen(vm, onBack = { navController.popBackStack() })
            }
        }
    }
}
