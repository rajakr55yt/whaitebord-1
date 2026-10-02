package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.WhiteboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.RewardedAdManager
import com.example.viewmodel.WhiteboardViewModel

enum class AppScreen {
    HOME,
    WHITEBOARD,
    HISTORY
}

class MainActivity : ComponentActivity() {

    private val whiteboardViewModel: WhiteboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RewardedAdManager.initialize(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
                val savedBoards by whiteboardViewModel.savedBoards.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF141416)
                ) {
                    when (currentScreen) {
                        AppScreen.HOME -> {
                            HomeScreen(
                                onNewBoardClick = {
                                    whiteboardViewModel.newBoard(isDark = false)
                                    currentScreen = AppScreen.WHITEBOARD
                                },
                                onHistoryClick = {
                                    currentScreen = AppScreen.HISTORY
                                }
                            )
                        }
                        AppScreen.WHITEBOARD -> {
                            WhiteboardScreen(
                                viewModel = whiteboardViewModel,
                                onBackToHome = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }
                        AppScreen.HISTORY -> {
                            HistoryScreen(
                                savedBoards = savedBoards,
                                onSelectBoard = { board ->
                                    whiteboardViewModel.loadBoard(board)
                                    currentScreen = AppScreen.WHITEBOARD
                                },
                                onDeleteBoard = { id ->
                                    whiteboardViewModel.deleteBoard(id)
                                },
                                onBackClick = {
                                    currentScreen = AppScreen.HOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
