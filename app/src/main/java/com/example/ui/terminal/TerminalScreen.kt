package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OutputType
import com.example.model.TerminalLine
import com.example.ui.theme.IdeAmberWarning
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeEmeraldGreen
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalError
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalSuccess
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
  fileName: String,
  lines: List<TerminalLine>,
  isRunning: Boolean,
  isWaitingForInput: Boolean,
  pendingInputPrompt: String,
  onSubmitInput: (String) -> Unit,
  onClear: () -> Unit,
  onRerun: () -> Unit,
  onBackToEditor: () -> Unit
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  var inputQuery by remember { mutableStateOf("") }

  // Intercept system Back button to return to code editor
  BackHandler {
    onBackToEditor()
  }

  // Auto scroll to bottom when new line arrives or input prompt appears
  LaunchedEffect(lines.size, isWaitingForInput) {
    if (lines.isNotEmpty()) {
      listState.animateScrollToItem(lines.size - 1)
    }
  }

  // Pulsing dot for running state
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseDot by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot"
  )

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(TerminalBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .testTag("pydroid_terminal_screen"),
    topBar = {
      TopAppBar(
        title = {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "Terminal Output",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TerminalText,
                fontFamily = FontFamily.Monospace
              )
              Spacer(modifier = Modifier.width(10.dp))
              // Running or Exited badge
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isRunning) IdeEmeraldGreen.copy(alpha = 0.2f) else Color(0xFF334155))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(7.dp)
                      .scale(if (isRunning) pulseDot else 1f)
                      .clip(CircleShape)
                      .background(if (isRunning) IdeEmeraldGreen else Color(0xFF94A3B8))
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isRunning) "RUNNING" else "EXITED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isRunning) IdeEmeraldGreen else Color(0xFF94A3B8)
                  )
                }
              }
            }
            Text(
              text = fileName,
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              color = TerminalPrompt
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onBackToEditor,
            modifier = Modifier.testTag("terminal_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Code Editor",
              tint = TerminalText,
              modifier = Modifier.size(24.dp)
            )
          }
        },
        actions = {
          // Re-run button
          IconButton(onClick = onRerun, modifier = Modifier.testTag("terminal_rerun_button")) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Re-run Program",
              tint = TerminalPrompt,
              modifier = Modifier.size(22.dp)
            )
          }

          // Copy button
          IconButton(
            onClick = {
              val fullOutput = lines.joinToString("\n") { it.text }
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("C++ Output", fullOutput))
              Toast.makeText(context, "Terminal output copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.testTag("terminal_copy_button")
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Output",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(20.dp)
            )
          }

          // Clear button
          IconButton(onClick = onClear, modifier = Modifier.testTag("terminal_clear_button")) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear Terminal",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(22.dp)
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = TerminalSurface)
      )
    },
    bottomBar = {
      // Interactive Input Bar or Status Bar
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(TerminalSurface)
      ) {
        if (isWaitingForInput) {
          Surface(
            color = TerminalSurface,
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, IdeCyanAccent.copy(alpha = 0.5f), RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = pendingInputPrompt.ifEmpty { "cin >> " },
                color = TerminalPrompt,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              )

              Spacer(modifier = Modifier.width(6.dp))

              BasicTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                textStyle = TextStyle(
                  color = TerminalText,
                  fontSize = 14.sp,
                  fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(TerminalPrompt),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                  onSend = {
                    if (inputQuery.isNotEmpty()) {
                      onSubmitInput(inputQuery)
                      inputQuery = ""
                    }
                  }
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("terminal_stdin_input_field"),
                decorationBox = { innerTextField ->
                  if (inputQuery.isEmpty()) {
                    Text(
                      text = "Enter input...",
                      color = Color(0xFF64748B),
                      fontSize = 14.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                  innerTextField()
                }
              )

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = {
                  onSubmitInput(inputQuery)
                  inputQuery = ""
                },
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(IdeBluePrimary)
                  .testTag("terminal_stdin_send_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Send,
                  contentDescription = "Submit stdin",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        } else {
          // Bottom hint bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onBackToEditor() }
              .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = null,
              tint = IdeCyanAccent,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isRunning) "Running in background... Tap to return to code" else "Execution complete. Tap here or ← to return to code",
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium,
              color = IdeCyanAccent,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(TerminalBackground)
        .padding(innerPadding)
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 14.dp, vertical = 12.dp)
      ) {
        if (lines.isEmpty()) {
          item {
            Text(
              text = "Initializing mobile execution container...\n",
              color = Color(0xFF64748B),
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace
            )
          }
        } else {
          items(lines) { line ->
            val color = when (line.type) {
              OutputType.STDOUT -> TerminalText
              OutputType.STDIN -> TerminalPrompt
              OutputType.STDERR -> IdeAmberWarning
              OutputType.INFO -> Color(0xFF94A3B8)
              OutputType.SUCCESS -> TerminalSuccess
              OutputType.ERROR -> TerminalError
            }

            Text(
              text = line.text,
              color = color,
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              lineHeight = 19.sp
            )
          }
        }
      }
    }
  }
}
