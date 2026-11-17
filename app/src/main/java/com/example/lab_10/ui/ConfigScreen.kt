package com.example.lab_10.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab_10.data.GameMode
import com.example.lab_10.data.PreferencesManager
import com.example.lab_10.data.SymbolTheme
import kotlinx.coroutines.launch

/**
 * Configuration Composable Screen.
 *
 * Allows users to configure Player names, Game Mode, custom N x N board size, win condition K,
 * and marker symbol themes.
 *
 * @param prefsManager Instance of [PreferencesManager] for persistent settings management.
 * @param onNavigateBack Callback triggered to return to TicTacToe game screen.
 * @param onLogout Callback triggered to log out user session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    prefsManager: PreferencesManager,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    // Read initial preference values
    var player1Name by remember { mutableStateOf(prefsManager.getPlayer1Name()) }
    var player2Name by remember { mutableStateOf(prefsManager.getPlayer2Name()) }
    var gameMode by remember { mutableStateOf(prefsManager.getGameMode()) }
    var symbolTheme by remember { mutableStateOf(prefsManager.getSymbolTheme()) }
    var boardSize by remember { mutableIntStateOf(prefsManager.getBoardSize()) }
    var winCondition by remember { mutableIntStateOf(prefsManager.getWinCondition()) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuration", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Game"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Player Names Configuration Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "👤 Player Profiles",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = player1Name,
                        onValueChange = {
                            player1Name = it
                            prefsManager.setPlayer1Name(it)
                        },
                        label = { Text("Player 1 Name (X)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = player2Name,
                        onValueChange = {
                            player2Name = it
                            prefsManager.setPlayer2Name(it)
                        },
                        label = { Text("Player 2 Name (O)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        enabled = gameMode == GameMode.PVP,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Game Mode Selection Section (PVP vs AI Bot)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎮 Game Mode",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    GameMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    gameMode = mode
                                    prefsManager.setGameMode(mode)
                                    if (mode == GameMode.AI) {
                                        player2Name = "AI Bot"
                                        prefsManager.setPlayer2Name("AI Bot")
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Game mode set to ${mode.displayName}")
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (gameMode == mode),
                                onClick = {
                                    gameMode = mode
                                    prefsManager.setGameMode(mode)
                                    if (mode == GameMode.AI) {
                                        player2Name = "AI Bot"
                                        prefsManager.setPlayer2Name("AI Bot")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = mode.displayName, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Custom Board Size & Win Condition Stepper Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "📐 Custom Board & Win Condition",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Board Size Stepper (N x N)
                    Column {
                        Text(
                            text = "Grid Board Size (N x N)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (boardSize > 3) {
                                        boardSize--
                                        prefsManager.setBoardSize(boardSize)
                                        if (winCondition > boardSize) {
                                            winCondition = boardSize
                                            prefsManager.setWinCondition(winCondition)
                                        }
                                    }
                                },
                                enabled = boardSize > 3,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "$boardSize x $boardSize",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    if (boardSize < 8) {
                                        boardSize++
                                        prefsManager.setBoardSize(boardSize)
                                    }
                                },
                                enabled = boardSize < 8,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick Presets
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 4, 5, 6, 8).forEach { preset ->
                                FilterChip(
                                    selected = boardSize == preset,
                                    onClick = {
                                        boardSize = preset
                                        prefsManager.setBoardSize(preset)
                                        if (winCondition > preset) {
                                            winCondition = preset
                                            prefsManager.setWinCondition(winCondition)
                                        }
                                    },
                                    label = { Text("${preset}x${preset}") }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    // Win Condition Stepper (K connected)
                    Column {
                        Text(
                            text = "Connected Markers Needed to Win (K)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Connect $winCondition markers in a row, column, or diagonal on a ${boardSize}x${boardSize} grid to win.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (winCondition > 3) {
                                        winCondition--
                                        prefsManager.setWinCondition(winCondition)
                                    }
                                },
                                enabled = winCondition > 3,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "Connect $winCondition",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    if (winCondition < boardSize) {
                                        winCondition++
                                        prefsManager.setWinCondition(winCondition)
                                    }
                                },
                                enabled = winCondition < boardSize,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Symbol Theme Selection Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "🎨 Marker Symbols & Theme",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SymbolTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    symbolTheme = theme
                                    prefsManager.setSymbolTheme(theme)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Symbol theme updated!")
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (symbolTheme == theme),
                                onClick = {
                                    symbolTheme = theme
                                    prefsManager.setSymbolTheme(theme)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = theme.displayName, fontSize = 16.sp)
                        }
                    }
                }
            }

            // Logout Session Button
            Button(
                onClick = {
                    prefsManager.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Log Out Active Session",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
