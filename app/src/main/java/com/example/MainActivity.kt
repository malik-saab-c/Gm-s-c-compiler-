package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.editor.IdeEditorScreen
import com.example.ui.splash.AnimatedSplashScreen
import com.example.ui.terminal.TerminalScreen
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.IdeViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = IdeBackground
        ) {
          val ideViewModel: IdeViewModel = viewModel()
          val uiState by ideViewModel.uiState.collectAsStateWithLifecycle()

          AnimatedContent(
            targetState = uiState.currentScreen,
            transitionSpec = {
              if (targetState == AppScreen.TERMINAL) {
                (slideInHorizontally { width -> width } + fadeIn())
                  .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut())
              } else if (initialState == AppScreen.TERMINAL) {
                (slideInHorizontally { width -> -width / 3 } + fadeIn())
                  .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
              } else {
                fadeIn().togetherWith(fadeOut())
              }
            },
            label = "screen_transition"
          ) { screen ->
            when (screen) {
              AppScreen.SPLASH -> {
                AnimatedSplashScreen(
                  onDismiss = { ideViewModel.dismissSplash() }
                )
              }
              AppScreen.EDITOR -> {
                IdeEditorScreen(
                  viewModel = ideViewModel,
                  uiState = uiState
                )
              }
              AppScreen.TERMINAL -> {
                TerminalScreen(
                  fileName = ideViewModel.activeFile?.name ?: "main.cpp",
                  lines = uiState.terminalLines,
                  isRunning = uiState.isRunning,
                  isWaitingForInput = uiState.isWaitingForInput,
                  pendingInputPrompt = uiState.pendingInputPrompt,
                  onSubmitInput = { input -> ideViewModel.submitTerminalInput(input) },
                  onClear = { ideViewModel.clearTerminal() },
                  onRerun = { ideViewModel.runCode() },
                  onBackToEditor = { ideViewModel.closeTerminalScreen() }
                )
              }
            }
          }
        }
      }
    }
  }
}
