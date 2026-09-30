package com.example.ui.debug

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DebugSessionState
import com.example.ui.theme.IdeAmberWarning
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeEmeraldGreen
import com.example.ui.theme.IdeRoseError
import com.example.ui.theme.IdeSurfaceVariant
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.IdeWhite

@Composable
fun VisualDebuggerPanel(
  debugState: DebugSessionState,
  onStepOver: () -> Unit,
  onContinue: () -> Unit,
  onStop: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    modifier = modifier
      .fillMaxWidth()
      .testTag("visual_debugger_panel"),
    color = IdeWhite,
    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    shadowElevation = 10.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(IdeAmberWarning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.BugReport,
              contentDescription = "Debugger",
              tint = IdeAmberWarning,
              modifier = Modifier.size(16.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Column {
            Text(
              text = "Visual C++ Debugger",
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = IdeTextPrimary
            )
            Text(
              text = if (debugState.isActive) "Paused at line ${debugState.currentLine} (${debugState.currentFile})" else "Session stopped",
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              color = if (debugState.isActive) IdeBluePrimary else IdeTextMuted
            )
          }
        }

        // Stop button
        IconButton(
          onClick = onStop,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close Debugger",
            tint = IdeTextMuted,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Control Buttons (Step Over, Continue, Stop)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onStepOver,
          enabled = debugState.isActive,
          colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .testTag("step_over_button")
        ) {
          Icon(
            imageVector = Icons.Default.SkipNext,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Step Over", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
          onClick = onContinue,
          enabled = debugState.isActive,
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .height(38.dp)
            .testTag("continue_debug_button")
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = IdeEmeraldGreen,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Continue", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IdeTextPrimary)
        }

        IconButton(
          onClick = onStop,
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(IdeRoseError.copy(alpha = 0.1f))
            .testTag("stop_debug_button")
        ) {
          Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = "Stop",
            tint = IdeRoseError,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Variables Watch List Header
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(IdeSurfaceVariant)
          .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "VARIABLE",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextSecondary,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "TYPE",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextSecondary,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1f)
        )
        Text(
          text = "VALUE",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextSecondary,
          fontFamily = FontFamily.Monospace,
          modifier = Modifier.weight(1.2f)
        )
      }

      // Variables List
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(120.dp)
      ) {
        if (debugState.variables.isEmpty()) {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No variables in scope yet. Tap 'Step Over' to evaluate.",
                fontSize = 11.sp,
                color = IdeTextMuted,
                fontFamily = FontFamily.Monospace
              )
            }
          }
        } else {
          items(debugState.variables) { v ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, IdeBorder)
                .background(if (v.isUpdated) IdeCyanAccent.copy(alpha = 0.08f) else IdeWhite)
                .padding(horizontal = 10.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = v.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = IdeBluePrimary,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = v.type,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = IdeCyanAccent,
                modifier = Modifier.weight(1f)
              )
              Text(
                text = v.value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = IdeTextPrimary,
                modifier = Modifier.weight(1.2f)
              )
            }
          }
        }
      }
    }
  }
}
