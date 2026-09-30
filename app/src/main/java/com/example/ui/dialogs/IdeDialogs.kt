package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultProjects
import com.example.model.CompilerConfig
import com.example.model.CppStandard
import com.example.model.Snippet
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBlueLight
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
fun NewFileDialog(
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var fileName by remember { mutableStateOf("") }
  var isHeader by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(18.dp),
    containerColor = IdeWhite,
    title = {
      Text(
        text = "Create C++ File",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = IdeTextPrimary
      )
    },
    text = {
      Column {
        Text(
          text = "Enter file name with .cpp or .h extension:",
          fontSize = 13.sp,
          color = IdeTextSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
          value = fileName,
          onValueChange = { fileName = it },
          placeholder = { Text(if (isHeader) "my_header.h" else "utils.cpp", fontSize = 13.sp) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("new_file_input")
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          OutlinedButton(
            onClick = {
              isHeader = false
              if (fileName.isEmpty()) fileName = "module.cpp"
              else if (!fileName.endsWith(".cpp")) fileName = "${fileName.substringBefore(".")}.cpp"
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(".cpp Source", fontSize = 12.sp)
          }
          OutlinedButton(
            onClick = {
              isHeader = true
              if (fileName.isEmpty()) fileName = "module.h"
              else if (!fileName.endsWith(".h")) fileName = "${fileName.substringBefore(".")}.h"
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(".h Header", fontSize = 12.sp)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (fileName.isNotBlank()) {
            val extension = if (isHeader) ".h" else ".cpp"
            val finalName = if (!fileName.contains(".")) "$fileName$extension" else fileName
            onConfirm(finalName)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("confirm_create_file_btn")
      ) {
        Text("Create File")
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun RenameFileDialog(
  currentName: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit
) {
  var newName by remember { mutableStateOf(currentName) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(18.dp),
    containerColor = IdeWhite,
    title = {
      Text(
        text = "Rename File",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = IdeTextPrimary
      )
    },
    text = {
      OutlinedTextField(
        value = newName,
        onValueChange = { newName = it },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    },
    confirmButton = {
      Button(
        onClick = { if (newName.isNotBlank()) onConfirm(newName) },
        colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Rename")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun DeleteConfirmDialog(
  fileName: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(18.dp),
    containerColor = IdeWhite,
    title = {
      Text(
        text = "Delete File",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = IdeRoseError
      )
    },
    text = {
      Text(
        text = "Are you sure you want to permanently delete \"$fileName\" from device storage? This cannot be undone.",
        fontSize = 14.sp,
        color = IdeTextSecondary,
        lineHeight = 20.sp
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = IdeRoseError),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("confirm_delete_btn")
      ) {
        Text("Delete", color = IdeWhite)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun SnippetsDialog(
  onDismiss: () -> Unit,
  onSelectSnippet: (Snippet) -> Unit
) {
  var selectedCategory by remember { mutableStateOf("All") }
  val snippets = DefaultProjects.snippets
  val categories = listOf("All") + snippets.map { it.category }.distinct()

  val filtered = if (selectedCategory == "All") snippets else snippets.filter { it.category == selectedCategory }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = IdeWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Code,
          contentDescription = null,
          tint = IdeBluePrimary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "C++ Snippets & Examples",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextPrimary
        )
      }
    },
    text = {
      Column(modifier = Modifier.heightIn(max = 420.dp)) {
        // Category Pills
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          categories.take(4).forEach { cat ->
            val isSelected = selectedCategory == cat
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) IdeBluePrimary else IdeSurfaceVariant)
                .clickable { selectedCategory = cat }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = cat,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) IdeWhite else IdeTextSecondary
              )
            }
          }
        }

        // Snippets list
        LazyColumn(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(filtered) { snippet ->
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = IdeBackground),
              border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectSnippet(snippet) }
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = snippet.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = IdeTextPrimary
                  )
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(IdeBlueLight.copy(alpha = 0.15f))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = snippet.category,
                      fontSize = 10.sp,
                      color = IdeBluePrimary,
                      fontWeight = FontWeight.SemiBold
                    )
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = snippet.description,
                  fontSize = 12.sp,
                  color = IdeTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "File: ${snippet.fileName}",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = IdeCyanAccent
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Close")
      }
    }
  )
}

@Composable
fun CompilerSettingsDialog(
  currentConfig: CompilerConfig,
  onDismiss: () -> Unit,
  onSave: (CompilerConfig) -> Unit
) {
  var selectedStandard by remember { mutableStateOf(currentConfig.standard) }
  var optLevel by remember { mutableStateOf(currentConfig.optimizationLevel) }
  var enableThreads by remember { mutableStateOf(currentConfig.enableThreads) }
  var useOnlineCompiler by remember { mutableStateOf(currentConfig.useOnlineCompiler) }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(20.dp),
    containerColor = IdeWhite,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Settings,
          contentDescription = null,
          tint = IdeBluePrimary,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "C++ Compiler Settings",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextPrimary
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "C++ Standard Version",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextPrimary
        )
        Spacer(modifier = Modifier.height(6.dp))

        CppStandard.values().forEach { std ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedStandard = std }
              .padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            RadioButton(
              selected = selectedStandard == std,
              onClick = { selectedStandard = std },
              colors = RadioButtonDefaults.colors(selectedColor = IdeBluePrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${std.displayName} (${std.flag})",
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              color = IdeTextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = "Optimization Level",
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          color = IdeTextPrimary
        )
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf("-O0", "-O2", "-O3").forEach { opt ->
            val isSel = optLevel == opt
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSel) IdeBluePrimary else IdeSurfaceVariant)
                .clickable { optLevel = opt }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = opt,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = if (isSel) IdeWhite else IdeTextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = "Multithreading Support (-pthread)",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = IdeTextPrimary
            )
            Text(
              text = "Support for std::thread and POSIX threads",
              fontSize = 11.sp,
              color = IdeTextMuted
            )
          }
          Switch(
            checked = enableThreads,
            onCheckedChange = { enableThreads = it },
            colors = SwitchDefaults.colors(checkedThumbColor = IdeBluePrimary)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            currentConfig.copy(
              standard = selectedStandard,
              optimizationLevel = optLevel,
              enableThreads = enableThreads,
              useOnlineCompiler = useOnlineCompiler
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Apply Changes")
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun AboutDeveloperDialog(
  onDismiss: () -> Unit,
  onReplaySplash: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(22.dp),
    containerColor = IdeWhite,
    title = null,
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // App badge
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
              Brush.linearGradient(
                listOf(IdeBluePrimary, IdeCyanAccent)
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "C++",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = IdeWhite,
            fontFamily = FontFamily.Monospace
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "GM'S c++",
          fontSize = 24.sp,
          fontWeight = FontWeight.Black,
          color = IdeTextPrimary
        )

        Text(
          text = "Version 1.0.0 • Mobile C++ IDE",
          fontSize = 12.sp,
          color = IdeTextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Card
        Card(
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(containerColor = IdeBackground),
          border = androidx.compose.foundation.BorderStroke(1.dp, IdeBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(IdeBluePrimary),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Code,
                contentDescription = null,
                tint = IdeWhite,
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = "Developer",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = IdeCyanAccent
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                  imageVector = Icons.Default.Verified,
                  contentDescription = null,
                  tint = IdeBluePrimary,
                  modifier = Modifier.size(14.dp)
                )
              }
              Text(
                text = "Sir Ghulam Mustafa",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = IdeTextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Features full C++20 support, multi-file projects, custom header files, interactive stdin (cin), visual step debugging, and built-in snippets.",
          fontSize = 12.sp,
          color = IdeTextSecondary,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
          onClick = {
            onDismiss()
            onReplaySplash()
          },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "Replay Splash Screen", fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = IdeBluePrimary),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text("Done")
      }
    }
  )
}
