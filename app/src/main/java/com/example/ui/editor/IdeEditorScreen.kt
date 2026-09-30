package com.example.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CppFile
import com.example.ui.debug.VisualDebuggerPanel
import com.example.ui.dialogs.AboutDeveloperDialog
import com.example.ui.dialogs.CompilerSettingsDialog
import com.example.ui.dialogs.DeleteConfirmDialog
import com.example.ui.dialogs.NewFileDialog
import com.example.ui.dialogs.RenameFileDialog
import com.example.ui.dialogs.SnippetsDialog
import com.example.ui.theme.IdeAmberWarning
import com.example.ui.theme.IdeBackground
import com.example.ui.theme.IdeBlueLight
import com.example.ui.theme.IdeBluePrimary
import com.example.ui.theme.IdeBorder
import com.example.ui.theme.IdeCyanAccent
import com.example.ui.theme.IdeEmeraldGreen
import com.example.ui.theme.IdeRoseError
import com.example.ui.theme.IdeSurface
import com.example.ui.theme.IdeSurfaceVariant
import com.example.ui.theme.IdeTextMuted
import com.example.ui.theme.IdeTextPrimary
import com.example.ui.theme.IdeTextSecondary
import com.example.ui.theme.IdeWhite
import com.example.viewmodel.IdeUiState
import com.example.viewmodel.IdeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeEditorScreen(
  viewModel: IdeViewModel,
  uiState: IdeUiState
) {
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  val activeFile = viewModel.activeFile ?: return

  var textFieldValue by remember(activeFile.id) {
    mutableStateOf(TextFieldValue(activeFile.content, TextRange(activeFile.content.length)))
  }

  // Update text field if file content is modified externally (undo/redo/snippet)
  if (textFieldValue.text != activeFile.content) {
    val newCursor = minOf(textFieldValue.selection.start, activeFile.content.length)
    textFieldValue = TextFieldValue(activeFile.content, TextRange(newCursor))
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        modifier = Modifier.width(310.dp),
        drawerContainerColor = IdeWhite
      ) {
        DrawerContent(
          uiState = uiState,
          onSelectFile = {
            viewModel.selectFile(it)
            scope.launch { drawerState.close() }
          },
          onNewFile = {
            viewModel.setDialogState(newFile = true)
            scope.launch { drawerState.close() }
          },
          onRenameFile = { file ->
            viewModel.requestRenameFile(file)
            scope.launch { drawerState.close() }
          },
          onDeleteFile = { file ->
            viewModel.requestDeleteFile(file)
            scope.launch { drawerState.close() }
          },
          onOpenSnippets = {
            viewModel.setDialogState(snippets = true)
            scope.launch { drawerState.close() }
          },
          onOpenSettings = {
            viewModel.setDialogState(settings = true)
            scope.launch { drawerState.close() }
          },
          onOpenAbout = {
            viewModel.setDialogState(about = true)
            scope.launch { drawerState.close() }
          }
        )
      }
    }
  ) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(IdeBackground)
        .statusBarsPadding()
        .navigationBarsPadding(),
      topBar = {
        IdeTopBar(
          title = "GM'S c++",
          activeFileName = activeFile.name,
          onMenuClick = { scope.launch { drawerState.open() } },
          onCheckSyntax = { viewModel.checkSyntax() },
          onStartDebug = { viewModel.startDebugging() },
          onOpenTerminal = { viewModel.openTerminalScreen() }
        )
      },
      floatingActionButton = {
        if (!uiState.isDebugPanelOpen) {
          FloatingActionButton(
            onClick = { viewModel.runCode() },
            containerColor = IdeEmeraldGreen,
            contentColor = IdeWhite,
            shape = CircleShape,
            modifier = Modifier
              .size(62.dp)
              .shadow(12.dp, CircleShape)
              .testTag("pydroid_run_fab")
          ) {
            Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = "Run C++ Program in Full-Screen Terminal",
              modifier = Modifier.size(34.dp)
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // 1. Files Tab Bar (Pydroid style)
          FilesTabBar(
            files = uiState.files,
            activeFileId = uiState.activeFileId,
            onSelectFile = { viewModel.selectFile(it) },
            onCloseFile = { file -> viewModel.requestDeleteFile(file) },
            onNewFile = { viewModel.setDialogState(newFile = true) }
          )

          // 2. Code Editor Area
          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .background(IdeWhite)
          ) {
            CodeEditorView(
              file = activeFile,
              value = textFieldValue,
              onValueChange = {
                textFieldValue = it
                viewModel.updateActiveContent(it.text)
              },
              onToggleBreakpoint = { line -> viewModel.toggleBreakpoint(line) },
              debugCurrentLine = if (uiState.debugState.isActive) uiState.debugState.currentLine else -1
            )
          }

          // 3. Accessory Keyboard Toolbar
          CodeAccessoryBar(
            onInsertSymbol = { symbol ->
              val text = textFieldValue.text
              val selection = textFieldValue.selection
              val newText = text.replaceRange(selection.start, selection.end, symbol)
              val newCursor = selection.start + symbol.length
              textFieldValue = TextFieldValue(newText, TextRange(newCursor))
              viewModel.updateActiveContent(newText)
            },
            onUndo = { viewModel.undo() },
            onRedo = { viewModel.redo() },
            onFormat = { viewModel.formatCode() }
          )

          // 4. Status Bar
          IdeStatusBar(
            file = activeFile,
            standard = uiState.compilerConfig.standard.displayName,
            breakpointsCount = activeFile.breakpoints.size
          )
        }

        // 5. Visual Debugger Panel Overlay (when debugging)
        AnimatedVisibility(
          visible = uiState.isDebugPanelOpen,
          enter = slideInVertically { it } + fadeIn(),
          exit = slideOutVertically { it } + fadeOut(),
          modifier = Modifier.align(Alignment.BottomCenter)
        ) {
          VisualDebuggerPanel(
            debugState = uiState.debugState,
            onStepOver = { viewModel.stepOverDebug() },
            onContinue = { viewModel.continueDebug() },
            onStop = { viewModel.stopDebugging() }
          )
        }
      }
    }
  }

  // Dialogs
  if (uiState.showNewFileDialog) {
    NewFileDialog(
      onDismiss = { viewModel.setDialogState(newFile = false) },
      onConfirm = { name -> viewModel.createFile(name) }
    )
  }

  if (uiState.showRenameDialog && uiState.fileToRename != null) {
    RenameFileDialog(
      currentName = uiState.fileToRename.name,
      onDismiss = { viewModel.cancelRenameFile() },
      onConfirm = { newName -> viewModel.confirmRenameFile(newName) }
    )
  }

  if (uiState.fileToDelete != null) {
    DeleteConfirmDialog(
      fileName = uiState.fileToDelete.name,
      onDismiss = { viewModel.cancelDeleteFile() },
      onConfirm = { viewModel.confirmDeleteFile() }
    )
  }

  if (uiState.showSnippetsDialog) {
    SnippetsDialog(
      onDismiss = { viewModel.setDialogState(snippets = false) },
      onSelectSnippet = { snippet -> viewModel.loadSnippet(snippet) }
    )
  }

  if (uiState.showSettingsDialog) {
    CompilerSettingsDialog(
      currentConfig = uiState.compilerConfig,
      onDismiss = { viewModel.setDialogState(settings = false) },
      onSave = { newConfig -> viewModel.updateCompilerConfig(newConfig) }
    )
  }

  if (uiState.showAboutDialog) {
    AboutDeveloperDialog(
      onDismiss = { viewModel.setDialogState(about = false) },
      onReplaySplash = { viewModel.showSplash() }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdeTopBar(
  title: String,
  activeFileName: String,
  onMenuClick: () -> Unit,
  onCheckSyntax: () -> Unit,
  onStartDebug: () -> Unit,
  onOpenTerminal: () -> Unit
) {
  TopAppBar(
    title = {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = IdeBluePrimary
          )
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(IdeBlueLight.copy(alpha = 0.15f))
              .padding(horizontal = 5.dp, vertical = 1.dp)
          ) {
            Text(
              text = "Sir Ghulam Mustafa",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = IdeBluePrimary
            )
          }
        }
        Text(
          text = activeFileName,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          color = IdeTextSecondary
        )
      }
    },
    navigationIcon = {
      IconButton(onClick = onMenuClick, modifier = Modifier.testTag("ide_menu_button")) {
        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = IdeTextPrimary)
      }
    },
    actions = {
      IconButton(onClick = onCheckSyntax) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Check Syntax",
          tint = IdeCyanAccent,
          modifier = Modifier.size(20.dp)
        )
      }

      IconButton(onClick = onStartDebug, modifier = Modifier.testTag("start_debugger_top_btn")) {
        Icon(
          imageVector = Icons.Default.BugReport,
          contentDescription = "Debug",
          tint = IdeAmberWarning,
          modifier = Modifier.size(20.dp)
        )
      }

      IconButton(onClick = onOpenTerminal, modifier = Modifier.testTag("open_terminal_top_btn")) {
        Icon(
          imageVector = Icons.Default.Terminal,
          contentDescription = "Terminal Screen",
          tint = IdeBluePrimary,
          modifier = Modifier.size(20.dp)
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(containerColor = IdeWhite)
  )
}

@Composable
private fun FilesTabBar(
  files: List<CppFile>,
  activeFileId: String,
  onSelectFile: (String) -> Unit,
  onCloseFile: (CppFile) -> Unit,
  onNewFile: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(44.dp)
      .background(IdeSurfaceVariant)
      .horizontalScroll(rememberScrollState())
      .padding(horizontal = 6.dp, vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    files.forEach { file ->
      val isActive = file.id == activeFileId
      Surface(
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        color = if (isActive) IdeWhite else IdeSurfaceVariant,
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.dp, IdeBorder) else null,
        modifier = Modifier.padding(end = 4.dp)
      ) {
        Row(
          modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier
              .clickable { onSelectFile(file.id) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Code,
              contentDescription = null,
              tint = if (file.isHeader) IdeAmberWarning else IdeBluePrimary,
              modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = file.name,
              fontSize = 12.sp,
              fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
              color = if (isActive) IdeTextPrimary else IdeTextSecondary
            )
          }

          if (files.size > 1) {
            Spacer(modifier = Modifier.width(2.dp))
            IconButton(
              onClick = { onCloseFile(file) },
              modifier = Modifier.size(26.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close ${file.name}",
                tint = IdeTextMuted,
                modifier = Modifier.size(14.dp)
              )
            }
          } else {
            Spacer(modifier = Modifier.width(6.dp))
          }
        }
      }
    }

    IconButton(
      onClick = onNewFile,
      modifier = Modifier
        .size(32.dp)
        .testTag("add_tab_button")
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "New File",
        tint = IdeBluePrimary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun CodeEditorView(
  file: CppFile,
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  onToggleBreakpoint: (Int) -> Unit,
  debugCurrentLine: Int
) {
  val lines = value.text.lines()
  val verticalScroll = rememberScrollState()
  val horizontalScroll = rememberScrollState()

  Row(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(verticalScroll)
  ) {
    // 1. Line Numbers Column with Breakpoints
    Column(
      modifier = Modifier
        .width(46.dp)
        .background(IdeSurfaceVariant)
        .padding(vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      lines.indices.forEach { idx ->
        val lineNum = idx + 1
        val hasBreakpoint = file.breakpoints.contains(lineNum)
        val isDebugLine = debugCurrentLine == lineNum

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(if (isDebugLine) IdeAmberWarning.copy(alpha = 0.25f) else Color.Transparent)
            .clickable { onToggleBreakpoint(lineNum) },
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasBreakpoint) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(IdeRoseError)
              )
              Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
              text = "$lineNum",
              fontSize = 11.5.sp,
              fontFamily = FontFamily.Monospace,
              color = if (isDebugLine) IdeAmberWarning else if (hasBreakpoint) IdeRoseError else IdeTextMuted
            )
          }
        }
      }
    }

    // 2. Code Input Field
    Box(
      modifier = Modifier
        .weight(1f)
        .horizontalScroll(horizontalScroll)
        .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
      BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
          color = IdeTextPrimary,
          fontSize = 13.sp,
          fontFamily = FontFamily.Monospace,
          lineHeight = 22.sp
        ),
        cursorBrush = SolidColor(IdeBluePrimary),
        visualTransformation = VisualTransformation { text ->
          androidx.compose.ui.text.input.TransformedText(
            com.example.ui.editor.CppSyntaxHighlighter.highlight(text.text),
            androidx.compose.ui.text.input.OffsetMapping.Identity
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .testTag("cpp_code_editor_field")
      )
    }
  }
}

@Composable
private fun IdeStatusBar(
  file: CppFile,
  standard: String,
  breakpointsCount: Int
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(26.dp)
      .background(IdeSurfaceVariant)
      .border(0.5.dp, IdeBorder)
      .padding(horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "Lines: ${file.content.lines().size}",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = IdeTextSecondary
      )
      Spacer(modifier = Modifier.width(12.dp))
      if (breakpointsCount > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(IdeRoseError)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "$breakpointsCount BPs",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = IdeRoseError
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
      }
      Text(
        text = "UTF-8",
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        color = IdeTextMuted
      )
    }

    Text(
      text = standard,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      fontFamily = FontFamily.Monospace,
      color = IdeBluePrimary
    )
  }
}

@Composable
private fun DrawerContent(
  uiState: IdeUiState,
  onSelectFile: (String) -> Unit,
  onNewFile: () -> Unit,
  onRenameFile: (CppFile) -> Unit,
  onDeleteFile: (CppFile) -> Unit,
  onOpenSnippets: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAbout: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxHeight()
      .padding(16.dp)
      .verticalScroll(rememberScrollState())
  ) {
    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(Brush.linearGradient(listOf(IdeBluePrimary, IdeCyanAccent))),
        contentAlignment = Alignment.Center
      ) {
        Text("C++", color = IdeWhite, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column {
        Text("GM'S c++", fontSize = 18.sp, fontWeight = FontWeight.Black, color = IdeTextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Sir Ghulam Mustafa", fontSize = 11.sp, color = IdeBluePrimary, fontWeight = FontWeight.SemiBold)
          Spacer(modifier = Modifier.width(3.dp))
          Icon(Icons.Default.Verified, contentDescription = null, tint = IdeBluePrimary, modifier = Modifier.size(12.dp))
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = IdeBorder)
    Spacer(modifier = Modifier.height(14.dp))

    // Project Files Section Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "PROJECT FILES",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = IdeTextMuted
      )
      IconButton(onClick = onNewFile, modifier = Modifier.size(28.dp)) {
        Icon(Icons.Default.Add, contentDescription = "New File", tint = IdeBluePrimary, modifier = Modifier.size(20.dp))
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Files list
    uiState.files.forEach { file ->
      val isSelected = file.id == uiState.activeFileId
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) IdeBlueLight.copy(alpha = 0.12f) else Color.Transparent,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier
              .weight(1f)
              .clickable { onSelectFile(file.id) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Code,
              contentDescription = null,
              tint = if (file.isHeader) IdeAmberWarning else IdeBluePrimary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = file.name,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              fontFamily = FontFamily.Monospace,
              color = if (isSelected) IdeBluePrimary else IdeTextPrimary
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onRenameFile(file) }, modifier = Modifier.size(34.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Rename ${file.name}", tint = IdeTextMuted, modifier = Modifier.size(16.dp))
            }
            if (uiState.files.size > 1) {
              IconButton(onClick = { onDeleteFile(file) }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete ${file.name}", tint = IdeRoseError, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))
    HorizontalDivider(color = IdeBorder)
    Spacer(modifier = Modifier.height(14.dp))

    // Tools & Navigation Items
    NavigationDrawerItem(
      label = { Text("Code Snippets & Examples", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
      icon = { Icon(Icons.Default.Code, contentDescription = null, tint = IdeBluePrimary) },
      selected = false,
      onClick = onOpenSnippets,
      colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )

    NavigationDrawerItem(
      label = { Text("Compiler Settings", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
      icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = IdeCyanAccent) },
      selected = false,
      onClick = onOpenSettings,
      colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )

    NavigationDrawerItem(
      label = { Text("About & Developer Info", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
      icon = { Icon(Icons.Default.Info, contentDescription = null, tint = IdeEmeraldGreen) },
      selected = false,
      onClick = onOpenAbout,
      colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
    )
  }
}
