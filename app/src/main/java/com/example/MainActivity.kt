package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.editor.IdeEditorScreen
import com.example.ui.splash.AnimatedSplashScreen
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.MyApplicationTheme
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

          Crossfade(
            targetState = uiState.showSplash,
            animationSpec = tween(400),
            label = "splash_to_editor_transition"
          ) { isSplashVisible ->
            if (isSplashVisible) {
              AnimatedSplashScreen(
                onDismiss = { ideViewModel.dismissSplash() }
              )
            } else {
              IdeEditorScreen(
                viewModel = ideViewModel,
                uiState = uiState
              )
            }
          }
        }
      }
    }
  }
}
