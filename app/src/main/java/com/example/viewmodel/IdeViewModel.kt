package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileManager
import com.example.engine.CppDebuggerEngine
import com.example.engine.CppEngine
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.DebugSessionState
import com.example.model.OutputType
import com.example.model.Snippet
import com.example.model.TerminalLine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
  SPLASH,
  EDITOR,
  TERMINAL
}

data class IdeUiState(
  val currentScreen: AppScreen = AppScreen.SPLASH,
  val files: List<CppFile> = emptyList(),
  val activeFileId: String = "",
  val compilerConfig: CompilerConfig = CompilerConfig(),
  val terminalLines: List<TerminalLine> = emptyList(),
  val isRunning: Boolean = false,
  val isWaitingForInput: Boolean = false,
  val pendingInputPrompt: String = "",
  val debugState: DebugSessionState = DebugSessionState(),
  val isDebugPanelOpen: Boolean = false,
  val showNewFileDialog: Boolean = false,
  val showRenameDialog: Boolean = false,
  val fileToRename: CppFile? = null,
  val fileToDelete: CppFile? = null,
  val showSnippetsDialog: Boolean = false,
  val showSettingsDialog: Boolean = false,
  val showAboutDialog: Boolean = false,
  val syntaxErrors: List<String> = emptyList()
)

class IdeViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(IdeUiState())
  val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

  private val engine = CppEngine()
  private val debugger = CppDebuggerEngine()
  private var inputDeferred: CompletableDeferred<String>? = null

  // Undo / Redo history
  private val undoStack = mutableListOf<String>()
  private val redoStack = mutableListOf<String>()

  init {
    loadFilesFromDisk()
  }

  private fun loadFilesFromDisk() {
    val context = getApplication<Application>().applicationContext
    val loadedFiles = FileManager.loadFiles(context)
    val initialActiveId = loadedFiles.firstOrNull()?.id.orEmpty()
    _uiState.update {
      it.copy(
        files = loadedFiles,
        activeFileId = initialActiveId
      )
    }
  }

  val activeFile: CppFile?
    get() = _uiState.value.files.find { it.id == _uiState.value.activeFileId } ?: _uiState.value.files.firstOrNull()

  fun dismissSplash() {
    _uiState.update { it.copy(currentScreen = AppScreen.EDITOR) }
  }

  fun showSplash() {
    _uiState.update { it.copy(currentScreen = AppScreen.SPLASH) }
  }

  fun openTerminalScreen() {
    _uiState.update { it.copy(currentScreen = AppScreen.TERMINAL) }
  }

  fun closeTerminalScreen() {
    _uiState.update { it.copy(currentScreen = AppScreen.EDITOR) }
  }

  fun selectFile(id: String) {
    _uiState.update { it.copy(activeFileId = id) }
    undoStack.clear()
    redoStack.clear()
  }

  fun updateActiveContent(newContent: String) {
    val current = activeFile ?: return
    if (current.content != newContent) {
      undoStack.add(current.content)
      redoStack.clear()
      if (undoStack.size > 50) undoStack.removeAt(0)

      val updatedFile = current.copy(content = newContent, isModified = true)

      // 1. Update in-memory state
      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) updatedFile else file
        }
        state.copy(files = updated)
      }

      // 2. Persist to disk immediately
      val context = getApplication<Application>().applicationContext
      FileManager.saveFile(context, updatedFile)
    }
  }

  fun undo() {
    if (undoStack.isNotEmpty()) {
      val current = activeFile ?: return
      val prev = undoStack.removeAt(undoStack.lastIndex)
      redoStack.add(current.content)
      val updatedFile = current.copy(content = prev, isModified = true)
      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) updatedFile else file
        }
        state.copy(files = updated)
      }
      val context = getApplication<Application>().applicationContext
      FileManager.saveFile(context, updatedFile)
    }
  }

  fun redo() {
    if (redoStack.isNotEmpty()) {
      val current = activeFile ?: return
      val next = redoStack.removeAt(redoStack.lastIndex)
      undoStack.add(current.content)
      val updatedFile = current.copy(content = next, isModified = true)
      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) updatedFile else file
        }
        state.copy(files = updated)
      }
      val context = getApplication<Application>().applicationContext
      FileManager.saveFile(context, updatedFile)
    }
  }

  fun toggleBreakpoint(line: Int) {
    _uiState.update { state ->
      val updated = state.files.map { file ->
        if (file.id == state.activeFileId) {
          val newBps = if (file.breakpoints.contains(line)) {
            file.breakpoints - line
          } else {
            file.breakpoints + line
          }
          file.copy(breakpoints = newBps)
        } else file
      }
      state.copy(files = updated)
    }
  }

  fun createFile(name: String, content: String = "") {
    val cleanName = if (!name.contains(".")) "$name.cpp" else name
    val initialCode = if (content.isEmpty()) {
      if (cleanName.endsWith(".h") || cleanName.endsWith(".hpp")) {
        "// $cleanName\n#pragma once\n\n"
      } else {
        "// $cleanName\n#include <iostream>\nusing namespace std;\n\nint main() {\n    cout << \"Hello from $cleanName!\" << endl;\n    return 0;\n}\n"
      }
    } else content

    val newFile = CppFile(
      id = cleanName,
      name = cleanName,
      content = initialCode,
      isMain = cleanName == "main.cpp"
    )

    // Save to disk
    val context = getApplication<Application>().applicationContext
    FileManager.saveFile(context, newFile)

    _uiState.update { state ->
      // Replace if file with same name exists, else append
      val filtered = state.files.filter { it.name != cleanName }
      state.copy(
        files = filtered + newFile,
        activeFileId = newFile.id,
        showNewFileDialog = false
      )
    }
  }

  fun requestDeleteFile(file: CppFile) {
    _uiState.update { it.copy(fileToDelete = file) }
  }

  fun cancelDeleteFile() {
    _uiState.update { it.copy(fileToDelete = null) }
  }

  fun confirmDeleteFile() {
    val target = _uiState.value.fileToDelete ?: return
    val context = getApplication<Application>().applicationContext

    // Delete from disk
    FileManager.deleteFile(context, target.name)

    _uiState.update { state ->
      val newFiles = state.files.filter { it.id != target.id }
      val fallbackFiles = if (newFiles.isEmpty()) {
        val defaultFile = CppFile("main.cpp", "main.cpp", "// main.cpp\n#include <iostream>\n\nint main() {\n    return 0;\n}\n", isMain = true)
        FileManager.saveFile(context, defaultFile)
        listOf(defaultFile)
      } else newFiles

      val nextActive = if (state.activeFileId == target.id) fallbackFiles.first().id else state.activeFileId
      state.copy(
        files = fallbackFiles,
        activeFileId = nextActive,
        fileToDelete = null
      )
    }
  }

  fun requestRenameFile(file: CppFile) {
    _uiState.update { it.copy(showRenameDialog = true, fileToRename = file) }
  }

  fun cancelRenameFile() {
    _uiState.update { it.copy(showRenameDialog = false, fileToRename = null) }
  }

  fun confirmRenameFile(newName: String) {
    val target = _uiState.value.fileToRename ?: return
    val cleanName = if (!newName.contains(".")) {
      if (target.isHeader) "$newName.h" else "$newName.cpp"
    } else newName

    val context = getApplication<Application>().applicationContext
    FileManager.renameFile(context, target.name, cleanName, target.content)

    val renamedFile = target.copy(id = cleanName, name = cleanName, isMain = cleanName == "main.cpp")

    _uiState.update { state ->
      val updated = state.files.map { if (it.id == target.id) renamedFile else it }
      state.copy(
        files = updated,
        activeFileId = if (state.activeFileId == target.id) renamedFile.id else state.activeFileId,
        showRenameDialog = false,
        fileToRename = null
      )
    }
  }

  fun loadSnippet(snippet: Snippet) {
    createFile(snippet.fileName, snippet.code)
    _uiState.update { it.copy(showSnippetsDialog = false) }
  }

  fun formatCode() {
    val current = activeFile ?: return
    val lines = current.content.lines()
    val sb = StringBuilder()
    var indent = 0

    for (raw in lines) {
      val trimmed = raw.trim()
      if (trimmed.isEmpty()) {
        sb.append("\n")
        continue
      }

      if (trimmed.startsWith("}") || trimmed.startsWith("};")) {
        indent = maxOf(0, indent - 1)
      }

      val spaces = "    ".repeat(indent)
      sb.append(spaces).append(trimmed).append("\n")

      if (trimmed.endsWith("{") && !trimmed.startsWith("//")) {
        indent++
      }
    }

    updateActiveContent(sb.toString().trimEnd() + "\n")
  }

  fun checkSyntax() {
    val current = activeFile ?: return
    val errors = engine.checkSyntax(current.content, _uiState.value.files)
    _uiState.update { state ->
      val lines = state.terminalLines.toMutableList()
      lines.add(TerminalLine("[Diagnostic] Syntax Analysis for ${current.name}...", OutputType.INFO))
      if (errors.isEmpty()) {
        lines.add(TerminalLine("✓ No syntax errors found! Ready to compile.", OutputType.SUCCESS))
      } else {
        errors.forEach { lines.add(TerminalLine(it, OutputType.ERROR)) }
      }
      state.copy(
        terminalLines = lines,
        currentScreen = AppScreen.TERMINAL,
        syntaxErrors = errors
      )
    }
  }

  fun runCode() {
    val current = activeFile ?: return
    if (_uiState.value.isRunning) {
      _uiState.update { it.copy(currentScreen = AppScreen.TERMINAL) }
      return
    }

    // Switch to separate terminal screen Pydroid style!
    _uiState.update {
      it.copy(
        currentScreen = AppScreen.TERMINAL,
        isRunning = true,
        terminalLines = listOf(
          TerminalLine("==========================================", OutputType.INFO),
          TerminalLine(" GM'S c++ IDE - Executing ${current.name}", OutputType.SUCCESS),
          TerminalLine(" Developer: Sir Ghulam Mustafa", OutputType.INFO),
          TerminalLine("==========================================\n", OutputType.INFO)
        )
      )
    }

    viewModelScope.launch {
      engine.execute(
        code = current.content,
        projectFiles = _uiState.value.files,
        config = _uiState.value.compilerConfig,
        inputProvider = {
          _uiState.update { it.copy(isWaitingForInput = true, pendingInputPrompt = "cin >> ") }
          val deferred = CompletableDeferred<String>()
          inputDeferred = deferred
          val result = deferred.await()
          _uiState.update { it.copy(isWaitingForInput = false, pendingInputPrompt = "") }
          result
        },
        onOutput = { line ->
          _uiState.update { state ->
            state.copy(terminalLines = state.terminalLines + line)
          }
        }
      )
      _uiState.update { it.copy(isRunning = false, isWaitingForInput = false) }
    }
  }

  fun submitTerminalInput(input: String) {
    inputDeferred?.complete(input)
    inputDeferred = null
  }

  fun startDebugging() {
    val current = activeFile ?: return
    val debugSession = debugger.startSession(current)
    _uiState.update {
      it.copy(
        debugState = debugSession,
        isDebugPanelOpen = true
      )
    }
  }

  fun stepOverDebug() {
    val current = activeFile ?: return
    val newState = debugger.stepOver(current)
    _uiState.update { it.copy(debugState = newState) }
  }

  fun continueDebug() {
    val current = activeFile ?: return
    val newState = debugger.continueExecution(current)
    _uiState.update { it.copy(debugState = newState) }
  }

  fun stopDebugging() {
    val newState = debugger.stopSession()
    _uiState.update {
      it.copy(
        debugState = newState,
        isDebugPanelOpen = false
      )
    }
  }

  fun clearTerminal() {
    _uiState.update { it.copy(terminalLines = emptyList()) }
  }

  fun updateCompilerConfig(config: CompilerConfig) {
    _uiState.update { it.copy(compilerConfig = config, showSettingsDialog = false) }
  }

  fun setDialogState(
    newFile: Boolean = false,
    rename: Boolean = false,
    snippets: Boolean = false,
    settings: Boolean = false,
    about: Boolean = false
  ) {
    _uiState.update {
      it.copy(
        showNewFileDialog = newFile,
        showRenameDialog = rename,
        showSnippetsDialog = snippets,
        showSettingsDialog = settings,
        showAboutDialog = about
      )
    }
  }
}
