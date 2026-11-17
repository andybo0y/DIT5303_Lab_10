package com.example.lab_10.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SymbolTheme Enum defines available marker symbol combinations for Player X and Player O.
 */
enum class SymbolTheme(val displayName: String, val xSymbol: String, val oSymbol: String) {
    CLASSIC("Classic (❌ / ⭕)", "❌", "⭕"),
    ELEMENTS("Elements (🔥 / 🧊)", "🔥", "🧊"),
    SPACE("Space (⭐ / 🌙)", "⭐", "🌙"),
    EMOJI("Fun (🐶 / 🐱)", "🐶", "🐱")
}

/**
 * GameMode Enum defines supported gameplay modes (Player vs Player or Player vs AI Bot).
 */
enum class GameMode(val displayName: String) {
    PVP("Player vs Player"),
    AI("Player vs AI (Bot)")
}

/**
 * PreferencesManager handles persistent user settings and application state using Android SharedPreferences.
 *
 * Stores user login credentials, round counter, board configuration, marker symbol themes, and win statistics.
 *
 * @param context Android Application context required to instantiate SharedPreferences.
 */
class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "lab10_preferences"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGGED_IN_USER = "logged_in_user"

        private const val KEY_USER_PREFIX = "user_pass_" // stores username -> password mapping

        private const val KEY_PLAYER1_NAME = "player1_name"
        private const val KEY_PLAYER2_NAME = "player2_name"
        private const val KEY_GAME_MODE = "game_mode"
        private const val KEY_SYMBOL_THEME = "symbol_theme"
        private const val KEY_BOARD_SIZE = "board_size"
        private const val KEY_WIN_CONDITION = "win_condition"
        private const val KEY_ROUND_NUMBER = "round_number"

        private const val KEY_SCORE_P1 = "score_p1"
        private const val KEY_SCORE_P2 = "score_p2"
        private const val KEY_SCORE_DRAWS = "score_draws"
    }

    init {
        // Pre-register default account (ABC / 123)
        saveUserCredential("ABC", "123")
    }

    /**
     * Checks if a user is currently logged into the app.
     */
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    /**
     * Gets the currently active logged-in username.
     */
    fun getLoggedInUsername(): String = prefs.getString(KEY_LOGGED_IN_USER, "") ?: ""

    /**
     * Updates the logged-in session state and stores active username in SharedPreferences.
     */
    fun setLoggedIn(loggedIn: Boolean, username: String = "") {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            .putString(KEY_LOGGED_IN_USER, if (loggedIn) username else "")
            .apply()
    }

    /**
     * Registers or updates password credentials for a given username.
     */
    fun saveUserCredential(username: String, pass: String) {
        prefs.edit()
            .putString(KEY_USER_PREFIX + username.trim().lowercase(), pass)
            .apply()
    }

    /**
     * Validates user password credentials against stored SharedPreferences entries.
     */
    fun validateCredentials(username: String, pass: String): Boolean {
        val trimmedUser = username.trim().lowercase()

        // Always allow default user ABC with password 123
        if (trimmedUser == "abc" && pass == "123") {
            saveUserCredential("ABC", "123")
            return true
        }

        val savedPass = prefs.getString(KEY_USER_PREFIX + trimmedUser, null)
        return if (savedPass != null) {
            savedPass == pass
        } else {
            // First time login auto-registration for new usernames
            saveUserCredential(username.trim(), pass)
            true
        }
    }

    /**
     * Gets Player 1 display name.
     */
    fun getPlayer1Name(): String = prefs.getString(KEY_PLAYER1_NAME, "Player 1") ?: "Player 1"

    /**
     * Sets Player 1 display name.
     */
    fun setPlayer1Name(name: String) {
        prefs.edit().putString(KEY_PLAYER1_NAME, name.ifBlank { "Player 1" }).apply()
    }

    /**
     * Gets Player 2 display name.
     */
    fun getPlayer2Name(): String = prefs.getString(KEY_PLAYER2_NAME, "Player 2") ?: "Player 2"

    /**
     * Sets Player 2 display name.
     */
    fun setPlayer2Name(name: String) {
        prefs.edit().putString(KEY_PLAYER2_NAME, name.ifBlank { "Player 2" }).apply()
    }

    /**
     * Gets the selected GameMode (PVP vs AI).
     */
    fun getGameMode(): GameMode {
        val name = prefs.getString(KEY_GAME_MODE, GameMode.PVP.name) ?: GameMode.PVP.name
        return try {
            GameMode.valueOf(name)
        } catch (_: Exception) {
            GameMode.PVP
        }
    }

    /**
     * Sets the GameMode.
     */
    fun setGameMode(mode: GameMode) {
        prefs.edit().putString(KEY_GAME_MODE, mode.name).apply()
    }

    /**
     * Gets the active SymbolTheme.
     */
    fun getSymbolTheme(): SymbolTheme {
        val name = prefs.getString(KEY_SYMBOL_THEME, SymbolTheme.CLASSIC.name) ?: SymbolTheme.CLASSIC.name
        return try {
            SymbolTheme.valueOf(name)
        } catch (_: Exception) {
            SymbolTheme.CLASSIC
        }
    }

    /**
     * Sets the SymbolTheme.
     */
    fun setSymbolTheme(theme: SymbolTheme) {
        prefs.edit().putString(KEY_SYMBOL_THEME, theme.name).apply()
    }

    /**
     * Gets current board grid size (3 to 8).
     */
    fun getBoardSize(): Int = prefs.getInt(KEY_BOARD_SIZE, 3).coerceIn(3, 8)

    /**
     * Sets grid board size (3 to 8).
     */
    fun setBoardSize(size: Int) {
        val clampedSize = size.coerceIn(3, 8)
        val currentWinCondition = getWinCondition()
        val editor = prefs.edit().putInt(KEY_BOARD_SIZE, clampedSize)
        if (currentWinCondition > clampedSize) {
            editor.putInt(KEY_WIN_CONDITION, clampedSize)
        }
        editor.apply()
    }

    /**
     * Gets winning markers count (K needed in a row).
     */
    fun getWinCondition(): Int {
        val size = getBoardSize()
        return prefs.getInt(KEY_WIN_CONDITION, size).coerceIn(3, size)
    }

    /**
     * Sets winning markers count (K needed in a row).
     */
    fun setWinCondition(count: Int) {
        val size = getBoardSize()
        val clampedCount = count.coerceIn(3, size)
        prefs.edit().putInt(KEY_WIN_CONDITION, clampedCount).apply()
    }

    /**
     * Gets current round number of the game.
     */
    fun getRoundNumber(): Int = prefs.getInt(KEY_ROUND_NUMBER, 1).coerceAtLeast(1)

    /**
     * Increments game round number by 1 and returns updated value.
     */
    fun incrementRoundNumber(): Int {
        val nextRound = getRoundNumber() + 1
        prefs.edit().putInt(KEY_ROUND_NUMBER, nextRound).apply()
        return nextRound
    }

    /**
     * Resets round number back to 1 for starting a new game session.
     */
    fun resetRoundNumber() {
        prefs.edit().putInt(KEY_ROUND_NUMBER, 1).apply()
    }

    /**
     * Gets Player 1 win count.
     */
    fun getScoreP1(): Int = prefs.getInt(KEY_SCORE_P1, 0)

    /**
     * Gets Player 2 win count.
     */
    fun getScoreP2(): Int = prefs.getInt(KEY_SCORE_P2, 0)

    /**
     * Gets draw games count.
     */
    fun getScoreDraws(): Int = prefs.getInt(KEY_SCORE_DRAWS, 0)

    /**
     * Increments Player 1 score by 1.
     */
    fun incrementScoreP1() {
        prefs.edit().putInt(KEY_SCORE_P1, getScoreP1() + 1).apply()
    }

    /**
     * Increments Player 2 score by 1.
     */
    fun incrementScoreP2() {
        prefs.edit().putInt(KEY_SCORE_P2, getScoreP2() + 1).apply()
    }

    /**
     * Increments draw count by 1.
     */
    fun incrementScoreDraws() {
        prefs.edit().putInt(KEY_SCORE_DRAWS, getScoreDraws() + 1).apply()
    }

    /**
     * Resets all accumulated win/draw scores to 0.
     */
    fun resetScores() {
        prefs.edit()
            .putInt(KEY_SCORE_P1, 0)
            .putInt(KEY_SCORE_P2, 0)
            .putInt(KEY_SCORE_DRAWS, 0)
            .apply()
    }

    /**
     * Resets round number and scores for starting a fresh game series.
     */
    fun startNewGameSession() {
        resetRoundNumber()
        resetScores()
    }

    /**
     * Logs out current user session.
     */
    fun logout() {
        setLoggedIn(false, "")
    }
}
