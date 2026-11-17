package com.example.lab_10

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.lab_10.data.PreferencesManager
import com.example.lab_10.ui.ConfigScreen
import com.example.lab_10.ui.LoginScreen
import com.example.lab_10.ui.RecordsScreen
import com.example.lab_10.ui.TicTacToeScreen
import com.example.lab_10.ui.theme.Lab_10Theme

/**
 * Navigation screen enumeration representing the 4 main views in the application.
 */
enum class AppScreen {
    LOGIN,   // Authentication / Login View
    GAME,    // Tic Tac Toe Gameplay View
    CONFIG,  // Configuration & Settings View
    RECORDS  // SQLite Database Records & Total Statistics View
}

/**
 * MainActivity serves as the primary entry point for the Android application.
 *
 * Initializes [PreferencesManager], enables edge-to-edge layout, and sets up Jetpack Compose UI with screen navigation state.
 */
class MainActivity : ComponentActivity() {

    private lateinit var prefsManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefsManager = PreferencesManager(applicationContext)
        enableEdgeToEdge()

        setContent {
            Lab_10Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember {
                        mutableStateOf(
                            if (prefsManager.isLoggedIn()) AppScreen.GAME else AppScreen.LOGIN
                        )
                    }

                    when (currentScreen) {
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                prefsManager = prefsManager,
                                onLoginSuccess = {
                                    currentScreen = AppScreen.GAME
                                }
                            )
                        }
                        AppScreen.GAME -> {
                            TicTacToeScreen(
                                prefsManager = prefsManager,
                                onNavigateToConfig = {
                                    currentScreen = AppScreen.CONFIG
                                },
                                onNavigateToRecords = {
                                    currentScreen = AppScreen.RECORDS
                                },
                                onLogout = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                        AppScreen.CONFIG -> {
                            ConfigScreen(
                                prefsManager = prefsManager,
                                onNavigateBack = {
                                    currentScreen = AppScreen.GAME
                                },
                                onLogout = {
                                    currentScreen = AppScreen.LOGIN
                                }
                            )
                        }
                        AppScreen.RECORDS -> {
                            RecordsScreen(
                                prefsManager = prefsManager,
                                onNavigateBack = {
                                    currentScreen = AppScreen.GAME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
