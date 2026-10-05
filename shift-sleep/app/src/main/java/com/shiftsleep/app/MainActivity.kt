package com.shiftsleep.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiftsleep.app.ui.home.HomeScreen
import com.shiftsleep.app.ui.home.HomeViewModel
import com.shiftsleep.app.ui.onboarding.OnboardingScreen
import com.shiftsleep.app.ui.schedule.ScheduleScreen
import com.shiftsleep.app.ui.settings.SettingsScreen
import com.shiftsleep.app.ui.theme.ShiftSleepTheme

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val app = application as ShiftSleepApp
        setContent {
            ShiftSleepTheme {
                ShiftSleepRoot(app)
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun ShiftSleepRoot(app: ShiftSleepApp) {
    val prefsFlow = remember { app.repository.observePrefs() }
    val prefs by prefsFlow.collectAsState(initial = null)
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        app.repository.ensurePrefs()
    }

    if (prefs == null) return

    if (!prefs!!.onboardingDone) {
        OnboardingScreen(
            onFinished = { preset ->
                // handled inside screen via repo; navigate by recomposition
            },
            repository = app.repository,
            scheduler = app.notificationScheduler,
        )
        return
    }

    val tabs = listOf(
        Triple("home", "오늘", Icons.Outlined.Home),
        Triple("schedule", "근무표", Icons.Outlined.CalendarMonth),
        Triple("settings", "설정", Icons.Outlined.Settings),
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: "home"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding),
        ) {
            composable("home") {
                val vm: HomeViewModel = viewModel(
                    factory = HomeViewModel.factory(app.repository),
                )
                HomeScreen(vm)
            }
            composable("schedule") {
                ScheduleScreen(
                    repository = app.repository,
                    scheduler = app.notificationScheduler,
                )
            }
            composable("settings") {
                SettingsScreen(
                    repository = app.repository,
                    scheduler = app.notificationScheduler,
                )
            }
        }
    }
}
