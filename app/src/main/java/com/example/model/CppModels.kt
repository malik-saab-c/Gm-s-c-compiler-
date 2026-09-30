package com.example.model

enum class CppStandard(val displayName: String, val flag: String) {
  CPP11("C++11", "-std=c++11"),
  CPP14("C++14", "-std=c++14"),
  CPP17("C++17", "-std=c++17"),
  CPP20("C++20 (Modern)", "-std=c++20"),
  CPP23("C++23 (Latest)", "-std=c++23")
}

data class CppFile(
  val id: String,
  val name: String,
  val content: String,
  val isMain: Boolean = false,
  val isModified: Boolean = false,
  val breakpoints: Set<Int> = emptySet()
) {
  val isHeader: Boolean
    get() = name.endsWith(".h") || name.endsWith(".hpp")
}

data class CompilerConfig(
  val standard: CppStandard = CppStandard.CPP20,
  val optimizationLevel: String = "-O2",
  val warnings: String = "-Wall -Wextra",
  val enableThreads: Boolean = true,
  val useOnlineCompiler: Boolean = false
)

enum class OutputType {
  STDOUT,
  STDIN,
  STDERR,
  INFO,
  SUCCESS,
  ERROR
}

data class TerminalLine(
  val text: String,
  val type: OutputType = OutputType.STDOUT
)

data class Snippet(
  val id: String,
  val title: String,
  val category: String,
  val description: String,
  val fileName: String,
  val code: String
)

data class DebugVariable(
  val name: String,
  val type: String,
  val value: String,
  val isUpdated: Boolean = false
)

data class DebugFrame(
  val functionName: String,
  val fileName: String,
  val line: Int
)

data class DebugSessionState(
  val isActive: Boolean = false,
  val isPaused: Boolean = false,
  val currentLine: Int = -1,
  val currentFile: String = "",
  val callStack: List<DebugFrame> = emptyList(),
  val variables: List<DebugVariable> = emptyList()
)
