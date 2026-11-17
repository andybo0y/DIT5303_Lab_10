package com.example.lab_10.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lab_10.data.GameDatabaseHelper
import com.example.lab_10.data.GameMode
import com.example.lab_10.data.PreferencesManager
import com.example.lab_10.data.SymbolTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Main Tic Tac Toe Game Composable Screen.
 *
 * Implements N x N grid gameplay, player turn management, AI bot move logic, win condition checking,
 * and records every move position entry into SQLiteDatabase using [GameDatabaseHelper].
 *
 * @param prefsManager Instance of [PreferencesManager] to access user preferences and session data.
 * @param onNavigateToConfig Callback triggered to navigate to Configuration screen.
 * @param onNavigateToRecords Callback triggered to navigate to Records & Statistics screen.
 * @param onLogout Callback triggered to log out active user session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicTacToeScreen(
    prefsManager: PreferencesManager,
    onNavigateToConfig: () -> Unit,
    onNavigateToRecords: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val dbHelper = remember(context) { GameDatabaseHelper(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Read stored user preferences
    val username = remember(prefsManager) { prefsManager.getLoggedInUsername().ifBlank { "Guest" } }
    val player1Name = remember(prefsManager) { prefsManager.getPlayer1Name() }
    val player2Name = remember(prefsManager) { prefsManager.getPlayer2Name() }
    val gameMode = remember(prefsManager) { prefsManager.getGameMode() }
    val symbolTheme = remember(prefsManager) { prefsManager.getSymbolTheme() }
    val boardSize = remember(prefsManager) { prefsManager.getBoardSize().coerceIn(3, 8) }
    val winCondition = remember(prefsManager) { prefsManager.getWinCondition().coerceIn(3, boardSize) }

    // Active Game Round Number counter stored in SharedPreferences
    var roundNumber by remember(prefsManager) { mutableIntStateOf(prefsManager.getRoundNumber()) }

    // Board state: N * N elements representing tile status ("X", "O", or null)
    var board by remember(boardSize, winCondition) {
        mutableStateOf(List<String?>(boardSize * boardSize) { null })
    }
    var currentTurn by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var isAiThinking by remember { mutableStateOf(false) }

    // Win/Draw score counters read from SharedPreferences
    var scoreP1 by remember { mutableIntStateOf(prefsManager.getScoreP1()) }
    var scoreP2 by remember { mutableIntStateOf(prefsManager.getScoreP2()) }
    var scoreDraws by remember { mutableIntStateOf(prefsManager.getScoreDraws()) }

    /**
     * Refreshes score counters from SharedPreferences.
     */
    fun refreshScoresFromPrefs() {
        scoreP1 = prefsManager.getScoreP1()
        scoreP2 = prefsManager.getScoreP2()
        scoreDraws = prefsManager.getScoreDraws()
    }

    /**
     * Generates all winning index sequences of length [winCount] on an N x N board.
     */
    fun generateWinPositions(size: Int, winCount: Int): List<List<Int>> {
        val winPositions = mutableListOf<List<Int>>()
        val k = winCount.coerceIn(3, size)

        // 1. Horizontal Rows
        for (r in 0 until size) {
            for (c in 0..(size - k)) {
                val line = mutableListOf<Int>()
                for (i in 0 until k) {
                    line.add(r * size + (c + i))
                }
                winPositions.add(line)
            }
        }

        // 2. Vertical Columns
        for (c in 0 until size) {
            for (r in 0..(size - k)) {
                val line = mutableListOf<Int>()
                for (i in 0 until k) {
                    line.add((r + i) * size + c)
                }
                winPositions.add(line)
            }
        }

        // 3. Main Diagonal (\)
        for (r in 0..(size - k)) {
            for (c in 0..(size - k)) {
                val line = mutableListOf<Int>()
                for (i in 0 until k) {
                    line.add((r + i) * size + (c + i))
                }
                winPositions.add(line)
            }
        }

        // 4. Anti-Diagonal (/)
        for (r in 0..(size - k)) {
            for (c in (k - 1) until size) {
                val line = mutableListOf<Int>()
                for (i in 0 until k) {
                    line.add((r + i) * size + (c - i))
                }
                winPositions.add(line)
            }
        }

        return winPositions
    }

    /**
     * Evaluates current board grid state to determine if a player won or if board resulted in a draw.
     */
    fun checkWinningCondition(currentBoard: List<String?>): Pair<String?, List<Int>?> {
        val winPositions = generateWinPositions(boardSize, winCondition)

        for (line in winPositions) {
            val firstSymbol = currentBoard[line[0]] ?: continue
            if (line.all { index -> currentBoard[index] == firstSymbol }) {
                return Pair(firstSymbol, line)
            }
        }

        if (currentBoard.none { it == null }) {
            return Pair("DRAW", null)
        }

        return Pair(null, null)
    }

    /**
     * Executes player input move action on tile [index] and records position into SQLiteDatabase.
     */
    fun makeMove(index: Int) {
        if (board[index] != null || winner != null || isAiThinking) return

        val newBoard = board.toMutableList()
        newBoard[index] = currentTurn
        board = newBoard

        val row = (index / boardSize) + 1
        val col = (index % boardSize) + 1
        val activePlayerName = if (currentTurn == "X") player1Name else player2Name
        val symbolIcon = if (currentTurn == "X") symbolTheme.xSymbol else symbolTheme.oSymbol
        val moveRecordStr = "$activePlayerName ($symbolIcon) placed at Row $row, Col $col (Grid $index)"

        // INSERT move input position record, username, round number, and system time into SQLiteDatabase
        dbHelper.insertMove(
            username = username,
            roundNumber = roundNumber,
            playerSymbol = currentTurn,
            positionRecord = moveRecordStr
        )

        val (foundWinner, foundLine) = checkWinningCondition(newBoard)
        if (foundWinner != null) {
            winner = foundWinner
            winningLine = foundLine
            when (foundWinner) {
                "X" -> {
                    prefsManager.incrementScoreP1()
                    refreshScoresFromPrefs()
                }
                "O" -> {
                    prefsManager.incrementScoreP2()
                    refreshScoresFromPrefs()
                }
                "DRAW" -> {
                    prefsManager.incrementScoreDraws()
                    refreshScoresFromPrefs()
                }
            }
        } else {
            currentTurn = if (currentTurn == "X") "O" else "X"
        }
    }

    /**
     * AI Bot player move coroutine effect.
     */
    LaunchedEffect(currentTurn, winner, gameMode, board) {
        if (gameMode == GameMode.AI && currentTurn == "O" && winner == null && !isAiThinking) {
            isAiThinking = true
            delay(500)

            val availableIndices = board.indices.filter { board[it] == null }
            if (availableIndices.isNotEmpty()) {
                var bestMove: Int? = null

                // 1. Check if AI can win immediately
                for (idx in availableIndices) {
                    val testBoard = board.toMutableList()
                    testBoard[idx] = "O"
                    if (checkWinningCondition(testBoard).first == "O") {
                        bestMove = idx
                        break
                    }
                }

                // 2. Block player X if player can win in next move
                if (bestMove == null) {
                    for (idx in availableIndices) {
                        val testBoard = board.toMutableList()
                        testBoard[idx] = "X"
                        if (checkWinningCondition(testBoard).first == "X") {
                            bestMove = idx
                            break
                        }
                    }
                }

                // 3. Take center grid if available
                val centerIdx = (boardSize * boardSize) / 2
                if (bestMove == null && centerIdx in availableIndices) {
                    bestMove = centerIdx
                }

                // 4. Take corners if available
                if (bestMove == null) {
                    val corners = listOf(
                        0,
                        boardSize - 1,
                        boardSize * (boardSize - 1),
                        boardSize * boardSize - 1
                    ).filter { it in availableIndices }
                    if (corners.isNotEmpty()) {
                        bestMove = corners.random()
                    }
                }

                // 5. Fallback random move
                if (bestMove == null) {
                    bestMove = availableIndices.random()
                }

                val newBoard = board.toMutableList()
                newBoard[bestMove] = "O"
                board = newBoard

                val row = (bestMove / boardSize) + 1
                val col = (bestMove % boardSize) + 1
                val moveRecordStr = "AI Bot (${symbolTheme.oSymbol}) placed at Row $row, Col $col (Grid $bestMove)"
                dbHelper.insertMove(
                    username = username,
                    roundNumber = roundNumber,
                    playerSymbol = "O",
                    positionRecord = moveRecordStr
                )

                val (foundWinner, foundLine) = checkWinningCondition(newBoard)
                if (foundWinner != null) {
                    winner = foundWinner
                    winningLine = foundLine
                    when (foundWinner) {
                        "X" -> {
                            prefsManager.incrementScoreP1()
                            refreshScoresFromPrefs()
                        }
                        "O" -> {
                            prefsManager.incrementScoreP2()
                            refreshScoresFromPrefs()
                        }
                        "DRAW" -> {
                            prefsManager.incrementScoreDraws()
                            refreshScoresFromPrefs()
                        }
                    }
                } else {
                    currentTurn = "X"
                }
            }
            isAiThinking = false
        }
    }

    /**
     * Resets game board grid and increments round number counter in SharedPreferences.
     */
    fun resetBoard() {
        roundNumber = prefsManager.incrementRoundNumber()
        board = List(boardSize * boardSize) { null }
        currentTurn = "X"
        winner = null
        winningLine = null
        isAiThinking = false
    }

    /**
     * Starts a New Game:
     * Saves previous game session summary into SQLite database, resets round number to 1,
     * resets total scores, and clears the game board grid.
     */
    fun startNewGame() {
        val winnerText = when {
            scoreP1 > scoreP2 -> "$player1Name (${symbolTheme.xSymbol}) Won ($scoreP1 - $scoreP2)"
            scoreP2 > scoreP1 -> "$player2Name (${symbolTheme.oSymbol}) Won ($scoreP2 - $scoreP1)"
            else -> "Game Series Tied ($scoreP1 - $scoreP2)"
        }

        // 1. Store previous game summary in SQLite Database
        dbHelper.insertGameSummary(
            username = username,
            totalRounds = roundNumber,
            p1Score = scoreP1,
            p2Score = scoreP2,
            drawsScore = scoreDraws,
            winnerSummary = winnerText
        )

        // 2. Reset preferences & state for new game
        prefsManager.startNewGameSession()
        roundNumber = 1
        board = List(boardSize * boardSize) { null }
        currentTurn = "X"
        winner = null
        winningLine = null
        isAiThinking = false
        refreshScoresFromPrefs()

        scope.launch {
            snackbarHostState.showSnackbar("Previous game saved to SQL database! Started New Game.")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Tic Tac Toe (Round $roundNumber | ${boardSize}x${boardSize})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "User: $username | Mode: ${gameMode.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToRecords) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = "Check Records"
                        )
                    }
                    IconButton(onClick = onNavigateToConfig) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configuration"
                        )
                    }
                    IconButton(onClick = {
                        prefsManager.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Score Board Card showing accumulated wins/draws stored in SharedPreferences
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScoreColumn(
                        label = "$player1Name (${symbolTheme.xSymbol})",
                        score = scoreP1,
                        isCurrentTurn = winner == null && currentTurn == "X"
                    )

                    VerticalDivider(modifier = Modifier.height(40.dp))

                    ScoreColumn(
                        label = "Draws",
                        score = scoreDraws,
                        isCurrentTurn = false
                    )

                    VerticalDivider(modifier = Modifier.height(40.dp))

                    ScoreColumn(
                        label = "$player2Name (${symbolTheme.oSymbol})",
                        score = scoreP2,
                        isCurrentTurn = winner == null && currentTurn == "O"
                    )
                }
            }

            // Game Turn & Winner Status Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (winner) {
                        "X" -> MaterialTheme.colorScheme.primaryContainer
                        "O" -> MaterialTheme.colorScheme.secondaryContainer
                        "DRAW" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        winner == "X" -> "🎉 $player1Name (${symbolTheme.xSymbol}) Wins Round $roundNumber!"
                        winner == "O" -> "🎉 $player2Name (${symbolTheme.oSymbol}) Wins Round $roundNumber!"
                        winner == "DRAW" -> "🤝 Round $roundNumber is a Draw!"
                        isAiThinking -> "🤖 $player2Name is thinking..."
                        currentTurn == "X" -> "👉 $player1Name (${symbolTheme.xSymbol})'s Turn"
                        else -> "👉 $player2Name (${symbolTheme.oSymbol})'s Turn"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            // Dynamic N x N Game Board Grid Box
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .padding(2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    val fontSize = when {
                        boardSize <= 3 -> 32.sp
                        boardSize == 4 -> 22.sp
                        boardSize == 5 -> 17.sp
                        boardSize == 6 -> 14.sp
                        else -> 11.sp
                    }
                    val paddingSize = when {
                        boardSize <= 3 -> 4.dp
                        boardSize <= 5 -> 2.dp
                        else -> 1.dp
                    }

                    for (row in 0 until boardSize) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (col in 0 until boardSize) {
                                val index = row * boardSize + col
                                val symbol = board[index]
                                val isWinningCell = winningLine?.contains(index) == true

                                GridTile(
                                    symbol = when (symbol) {
                                        "X" -> symbolTheme.xSymbol
                                        "O" -> symbolTheme.oSymbol
                                        else -> ""
                                    },
                                    isWinningCell = isWinningCell,
                                    fontSize = fontSize,
                                    onClick = { makeMove(index) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(paddingSize)
                                )
                            }
                        }
                    }
                }
            }

            // Action Buttons Section: 2 Rows of 2 Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: New Round & Start New Game (Saves previous data in SQL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { resetBoard() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Round",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Round", fontSize = 13.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { startNewGame() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Start New Game",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start New Game", fontSize = 13.sp, maxLines = 1)
                    }
                }

                // Row 2: Check Records (SQL & Stats) & Config / Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToRecords,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ListAlt,
                            contentDescription = "Check Records",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Check Records", fontSize = 13.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onNavigateToConfig,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Config",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Config", fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/**
 * Score Column displaying individual player wins/draw count.
 */
@Composable
private fun ScoreColumn(
    label: String,
    score: Int,
    isCurrentTurn: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isCurrentTurn) FontWeight.ExtraBold else FontWeight.Normal,
            color = if (isCurrentTurn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = score.toString(),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Grid Tile representing a single cell in the Tic Tac Toe board.
 */
@Composable
private fun GridTile(
    symbol: String,
    isWinningCell: Boolean,
    fontSize: TextUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isWinningCell) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
