package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: BandRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        database = AppDatabase.getDatabase(this)
        repository = BandRepository(database.bandDao())

        // 2. Pre-populate database asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            repository.prepopulateIfEmpty()
        }

        // 3. Define standard factories for ViewModels
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return when {
                    modelClass.isAssignableFrom(LoginViewModel::class.java) -> LoginViewModel(repository) as T
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
