package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.data.BandRepository
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight

class MainActivity : FragmentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: BandRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        database = AppDatabase.getDatabase(this)
        repository = BandRepository(database.bandDao())

        // 2. Define standard factories for ViewModels
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(repository, applicationContext) as T
                    modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(repository) as T
                    modelClass.isAssignableFrom(CommitmentsViewModel::class.java) -> CommitmentsViewModel(repository) as T
                    modelClass.isAssignableFrom(MembersViewModel::class.java) -> MembersViewModel(repository) as T
                    modelClass.isAssignableFrom(PaymentDetailViewModel::class.java) -> PaymentDetailViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                // Request view model references using our custom factory
                val loginViewModel: LoginViewModel = viewModel(factory = factory)
                val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
                val commitmentsViewModel: CommitmentsViewModel = viewModel(factory = factory)
                val membersViewModel: MembersViewModel = viewModel(factory = factory)
                val paymentDetailViewModel: PaymentDetailViewModel = viewModel(factory = factory)

                NavHost(
                    navController = navController,
                    startDestination = "login",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Login view destination
                    composable("login") {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onBiometricLoginRequested = {
                                authenticateWithFingerprint(
                                    onSuccess = { loginViewModel.loginWithFingerprint() },
                                    onError = { message -> loginViewModel.setLoginError(message) }
                                )
                            },
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Main App Shell containing Bottom Navigation
                    composable("main") {
                        MainAppShell(
                            navController = navController,
                            dashboardViewModel = dashboardViewModel,
                            commitmentsViewModel = commitmentsViewModel,
                            membersViewModel = membersViewModel,
                            onLogout = {
                                loginViewModel.logout()
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Detail view destination (receives event ID parameter)
                    composable(
                        route = "detail/{commitmentId}",
                        arguments = listOf(navArgument("commitmentId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val commitmentId = backStackEntry.arguments?.getInt("commitmentId") ?: 0
                        PaymentDetailScreen(
                            viewModel = paymentDetailViewModel,
                            commitmentId = commitmentId,
                            onNavigateBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun authenticateWithFingerprint(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                onError("Este celular no tiene sensor biometrico disponible")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                onError("El sensor biometrico no esta disponible en este momento")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("Registra una huella en los ajustes del celular para usar este acceso")
                return
            }
            else -> {
                onError("No se puede usar la huella en este dispositivo")
                return
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Confirma tu huella digital")
            .setSubtitle("Usa el sensor de tu celular para iniciar sesion")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Huella no reconocida. Intentalo nuevamente")
                }
            }
        )

        prompt.authenticate(promptInfo)
    }
}

// Scaffold with BottomNavigationBar that manages Dashboard, Commitments and Members screens
@Composable
fun MainAppShell(
    navController: androidx.navigation.NavController,
    dashboardViewModel: DashboardViewModel,
    commitmentsViewModel: CommitmentsViewModel,
    membersViewModel: MembersViewModel,
    onLogout: () -> Unit
) {
    val shellNavController = rememberNavController()
    val navBackStackEntry by shellNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

    val items = listOf(
        NavigationItem("dashboard", "Inicio", Icons.Default.Dashboard),
        NavigationItem("commitments", "Eventos", Icons.Default.EventNote),
        NavigationItem("members", "Integrantes", Icons.Default.People)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, BrandPurpleLight, RoundedCornerShape(20.dp))
                    .testTag("bottom_nav_bar")
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        selected = isSelected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandPurple,
                            selectedTextColor = BrandPurple,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextGray,
                            indicatorColor = BrandPurpleLight
                        ),
                        onClick = {
                            if (currentRoute != item.route) {
                                shellNavController.navigate(item.route) {
                                    popUpTo(shellNavController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        modifier = Modifier.testTag("nav_tab_${item.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = shellNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToCommitments = {
                        shellNavController.navigate("commitments") {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToMembers = {
                        shellNavController.navigate("members") {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("commitments") {
                CommitmentsScreen(
                    viewModel = commitmentsViewModel,
                    onNavigateToDetail = { commitmentId ->
                        // Navigate up to parent NavHost detail destination
                        navController.navigate("detail/$commitmentId")
                    }
                )
            }
            composable("members") {
                MembersScreen(
                    viewModel = membersViewModel
                )
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
