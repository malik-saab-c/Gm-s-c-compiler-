package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.example.ui.theme.IdeBlueLight
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeEmeraldGreen
import com.example.ui.theme.IdeRoseError
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalError
import com.example.ui.theme.TerminalPrompt
import com.example.ui.theme.TerminalSuccess
import com.example.ui.theme.TerminalSurface
import com.example.ui.theme.TerminalText

@Composable
fun InteractiveTerminalSheet(
  lines: List<TerminalLine>,
  isRunning: Boolean,
  isWaitingForInput: Boolean,
  pendingInputPrompt: String,
  onSubmitInput: (String) -> Unit,
  onClear: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val listState = rememberLazyListState()
  var inputQuery by remember { mutableStateOf("") }

  // Auto scroll to bottom on new output
  LaunchedEffect(lines.size, isWaitingForInput) {
    if (lines.isNotEmpty()) {
      listState.animateScrollToItem(lines.size - 1)
    }
  }

  // Pulsing dot for running status
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseDot by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.3f,
    animationSpec = infiniteRepeatable(
      animation = tween(600, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "dot"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .imePadding()
      .testTag("interactive_terminal_sheet"),
    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    color = TerminalBackground,
    shadowElevation = 16.dp
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Terminal Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(TerminalSurface)
          .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = "Terminal Icon",
            tint = TerminalPrompt,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Terminal Output",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TerminalText,
            fontFamily = FontFamily.Monospace
          )

          Spacer(modifier = Modifier.width(10.dp))

          // Status indicator
          Box(
            modifier = Modifier
              .size(8.dp)
              .scale(if (isRunning) pulseDot else 1f)
              .clip(CircleShape)
              .background(if (isRunning) IdeEmeraldGreen else Color(0xFF64748B))
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

        // Action buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              val fullOutput = lines.joinToString("\n") { it.text }
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", fullOutput))
              Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = "Copy Output",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(16.dp)
            )
          }

          IconButton(
            onClick = onClear,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear Terminal",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(18.dp)
            )
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier.size(34.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Terminal",
              tint = Color(0xFF94A3B8),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // 2. Output Console List
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        if (lines.isEmpty()) {
          item {
            Text(
              text = "No output yet. Tap RUN (▶) to compile and execute.",
              color = Color(0xFF64748B),
              fontSize = 12.sp,
              fontFamily = FontFamily.Monospace,
              modifier = Modifier.padding(top = 16.dp)
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
              fontSize = 12.5.sp,
              fontFamily = FontFamily.Monospace,
              lineHeight = 18.sp
            )
          }
        }
      }

      // 3. Interactive Input Bar (cin >>)
      AnimatedVisibility(
        visible = isWaitingForInput,
        enter = fadeIn() + slideInVertically { 40 }
      ) {
        Surface(
          color = TerminalSurface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, IdeCyanAccent.copy(alpha = 0.4f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = pendingInputPrompt.ifEmpty { "cin >> " },
              color = TerminalPrompt,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            BasicTextField(
              value = inputQuery,
              onValueChange = { inputQuery = it },
              textStyle = TextStyle(
                color = TerminalText,
                fontSize = 13.sp,
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
                .testTag("cin_input_field"),
              decorationBox = { innerTextField ->
                if (inputQuery.isEmpty()) {
                  Text(
                    text = "Type stdin input...",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                  )
                }
                innerTextField()
              }
            )

            IconButton(
              onClick = {
                onSubmitInput(inputQuery)
                inputQuery = ""
              },
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(IdeBluePrimary)
                .testTag("cin_send_button")
            ) {
              Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send cin input",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }
      }
    }
  }
}
