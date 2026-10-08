package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.AttendanceRepository
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.OfficeQrScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryBlue

enum class Screen {
    LOGIN,
    HOME,
    HISTORY,
    SCANNER,
    OFFICE_QR
}

class MainActivity : ComponentActivity() {

    private val repository = AttendanceRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(repository = repository)
            }
        }
    }
}

@Composable
fun MainApp(repository: AttendanceRepository) {
    val currentUser by repository.currentUser.collectAsStateWithLifecycle()
    val todayRecord by repository.todayRecord.collectAsStateWithLifecycle()
    val history by repository.history.collectAsStateWithLifecycle()
    val isUserInsideOffice by repository.isUserInsideOffice.collectAsStateWithLifecycle()

    var currentScreen by remember {
        mutableStateOf(if (currentUser != null) Screen.HOME else Screen.LOGIN)
    }
    var isCheckInMode by remember { mutableStateOf(true) }

    if (currentUser == null) {
        LoginScreen(
            onLoginSuccess = { email, role ->
                repository.login(email, role)
                currentScreen = Screen.HOME
            }
        )
        return
    }

    val showBottomBar = currentScreen == Screen.HOME || currentScreen == Screen.HISTORY

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.HOME,
                        onClick = { currentScreen = Screen.HOME },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Beranda") },
                        label = { Text("Beranda") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.HISTORY,
                        onClick = { currentScreen = Screen.HISTORY },
                        icon = { Icon(Icons.Default.History, contentDescription = "Riwayat") },
                        label = { Text("Riwayat") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                Screen.LOGIN -> {
                    LoginScreen(
                        onLoginSuccess = { email, role ->
                            repository.login(email, role)
                            currentScreen = Screen.HOME
                        }
                    )
                }

                Screen.HOME -> {
                    HomeScreen(
                        user = currentUser!!,
                        todayRecord = todayRecord,
                        repository = repository,
                        isUserInsideOffice = isUserInsideOffice,
                        onNavigateToScan = { checkIn ->
                            isCheckInMode = checkIn
                            currentScreen = Screen.SCANNER
                        },
                        onNavigateToOfficeQr = {
                            currentScreen = Screen.OFFICE_QR
                        },
                        onLogout = {
                            repository.logout()
                            currentScreen = Screen.LOGIN
                        }
                    )
                }

                Screen.HISTORY -> {
                    BackHandler {
                        currentScreen = Screen.HOME
                    }
                    HistoryScreen(records = history)
                }

                Screen.SCANNER -> {
                    BackHandler {
                        currentScreen = Screen.HOME
                    }
                    ScannerScreen(
                        isCheckIn = isCheckInMode,
                        repository = repository,
                        onBack = { currentScreen = Screen.HOME }
                    )
                }

                Screen.OFFICE_QR -> {
                    BackHandler {
                        currentScreen = Screen.HOME
                    }
                    OfficeQrScreen(
                        repository = repository,
                        onBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }
}
