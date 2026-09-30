package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultProjects
import com.example.engine.CppDebuggerEngine
import com.example.engine.CppEngine
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.CppStandard
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

data class IdeUiState(
  val files: List<CppFile> = listOf(DefaultProjects.defaultMainFile, DefaultProjects.defaultHeaderFile),
  val activeFileId: String = "main_cpp",
  val compilerConfig: CompilerConfig = CompilerConfig(),
  val terminalLines: List<TerminalLine> = emptyList(),
  val isRunning: Boolean = false,
  val isTerminalOpen: Boolean = false,
  val isWaitingForInput: Boolean = false,
  val pendingInputPrompt: String = "",
  val debugState: DebugSessionState = DebugSessionState(),
  val isDebugPanelOpen: Boolean = false,
  val showSplash: Boolean = true,
  val showNewFileDialog: Boolean = false,
  val showRenameDialog: Boolean = false,
  val showSnippetsDialog: Boolean = false,
  val showSettingsDialog: Boolean = false,
  val showAboutDialog: Boolean = false,
  val syntaxErrors: List<String> = emptyList()
)

class IdeViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(IdeUiState())
  val uiState: StateFlow<IdeUiState> = _uiState.asStateFlow()

  private val engine = CppEngine()
  private val debugger = CppDebuggerEngine()
  private var inputDeferred: CompletableDeferred<String>? = null

  // Undo / Redo history
  private val undoStack = mutableListOf<String>()
  private val redoStack = mutableListOf<String>()

  val activeFile: CppFile?
    get() = _uiState.value.files.find { it.id == _uiState.value.activeFileId } ?: _uiState.value.files.firstOrNull()

  fun dismissSplash() {
    _uiState.update { it.copy(showSplash = false) }
  }

  fun showSplash() {
    _uiState.update { it.copy(showSplash = true) }
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

      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) {
            file.copy(content = newContent, isModified = true)
          } else file
        }
        state.copy(files = updated)
      }
    }
  }

  fun undo() {
    if (undoStack.isNotEmpty()) {
      val current = activeFile ?: return
      val prev = undoStack.removeAt(undoStack.lastIndex)
      redoStack.add(current.content)
      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) file.copy(content = prev, isModified = true) else file
        }
        state.copy(files = updated)
      }
    }
  }

  fun redo() {
    if (redoStack.isNotEmpty()) {
      val current = activeFile ?: return
      val next = redoStack.removeAt(redoStack.lastIndex)
      undoStack.add(current.content)
      _uiState.update { state ->
        val updated = state.files.map { file ->
          if (file.id == state.activeFileId) file.copy(content = next, isModified = true) else file
        }
        state.copy(files = updated)
      }
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
    val newFile = CppFile(
      id = UUID.randomUUID().toString(),
      name = cleanName,
      content = if (content.isEmpty()) {
        if (cleanName.endsWith(".h") || cleanName.endsWith(".hpp")) {
          "// $cleanName\n#pragma once\n\n"
        } else {
          "// $cleanName\n#include <iostream>\n\nint main() {\n    std::cout << \"Hello from $cleanName\" << std::endl;\n    return 0;\n}\n"
        }
      } else content,
      isMain = cleanName == "main.cpp"
    )

    _uiState.update { state ->
      state.copy(
        files = state.files + newFile,
        activeFileId = newFile.id,
        showNewFileDialog = false
      )
    }
  }

  fun deleteFile(id: String) {
    if (_uiState.value.files.size <= 1) return // Keep at least one file
    _uiState.update { state ->
      val newFiles = state.files.filter { it.id != id }
      val nextActive = if (state.activeFileId == id) newFiles.first().id else state.activeFileId
      state.copy(files = newFiles, activeFileId = nextActive)
    }
  }

  fun renameFile(id: String, newName: String) {
    _uiState.update { state ->
      val updated = state.files.map { file ->
        if (file.id == id) file.copy(name = newName) else file
      }
      state.copy(files = updated, showRenameDialog = false)
    }
  }

  fun loadSnippet(snippet: Snippet) {
    val current = activeFile
    if (current != null && current.name == snippet.fileName) {
      updateActiveContent(snippet.code)
    } else {
      createFile(snippet.fileName, snippet.code)
    }
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
        isTerminalOpen = true,
        syntaxErrors = errors
      )
    }
  }

  fun runCode() {
    val current = activeFile ?: return
    if (_uiState.value.isRunning) return

    _uiState.update {
      it.copy(
        isRunning = true,
        isTerminalOpen = true,
        terminalLines = listOf(
          TerminalLine("==========================================", OutputType.INFO),
          TerminalLine(" GM'S c++ IDE - Building & Executing ${current.name}", OutputType.SUCCESS),
          TerminalLine(" Developer: Sir Ghulam Mustafa", OutputType.INFO),
          TerminalLine("==========================================", OutputType.INFO)
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

  fun toggleTerminal(open: Boolean? = null) {
    _uiState.update { it.copy(isTerminalOpen = open ?: !it.isTerminalOpen) }
  }

  fun toggleDebugPanel(open: Boolean? = null) {
    _uiState.update { it.copy(isDebugPanelOpen = open ?: !it.isDebugPanelOpen) }
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
