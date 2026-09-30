package com.example.engine

import com.example.model.CppFile
import com.example.model.DebugFrame
import com.example.model.DebugSessionState
import com.example.model.DebugVariable

class CppDebuggerEngine {

  private var state = DebugSessionState()
  private var codeLines: List<String> = emptyList()
  private var currentIdx: Int = 0
  private val variablesMap = mutableMapOf<String, DebugVariable>()

  fun startSession(file: CppFile): DebugSessionState {
    codeLines = file.content.lines()
    currentIdx = 0
    variablesMap.clear()

    // Find first executable line inside main
    var mainStart = -1
    for (i in codeLines.indices) {
      if (codeLines[i].contains("int main") || codeLines[i].contains("void main")) {
        mainStart = i + 1
        break
      }
    }
    if (mainStart == -1) mainStart = 0

    currentIdx = mainStart
    // Advance to first non-empty, non-comment line
    advanceToExecutableLine()

    state = DebugSessionState(
      isActive = true,
      isPaused = true,
      currentLine = currentIdx + 1,
      currentFile = file.name,
      callStack = listOf(DebugFrame("main()", file.name, currentIdx + 1)),
      variables = getVariableList()
    )
    return state
  }

  fun stepOver(file: CppFile): DebugSessionState {
    if (!state.isActive || currentIdx >= codeLines.size) {
      return stopSession()
    }

    // Execute current line inspection
    val line = codeLines.getOrNull(currentIdx)?.trim().orEmpty()
    inspectLine(line)

    currentIdx++
    advanceToExecutableLine()

    if (currentIdx >= codeLines.size || (line.startsWith("return ") && !line.startsWith("return 0; /*"))) {
      return stopSession()
    }

    state = state.copy(
      currentLine = currentIdx + 1,
      callStack = listOf(DebugFrame("main()", file.name, currentIdx + 1)),
      variables = getVariableList()
    )
    return state
  }

  fun continueExecution(file: CppFile): DebugSessionState {
    if (!state.isActive) return state
    val breakpoints = file.breakpoints

    // Run until next breakpoint or end
    while (currentIdx < codeLines.size) {
      val line = codeLines.getOrNull(currentIdx)?.trim().orEmpty()
      inspectLine(line)
      currentIdx++
      advanceToExecutableLine()

      val lineNumber = currentIdx + 1
      if (breakpoints.contains(lineNumber)) {
        // Hit breakpoint!
        state = state.copy(
          currentLine = lineNumber,
          isPaused = true,
          callStack = listOf(DebugFrame("main()", file.name, lineNumber)),
          variables = getVariableList()
        )
        return state
      }
    }

    return stopSession()
  }

  fun stopSession(): DebugSessionState {
    state = DebugSessionState(isActive = false, isPaused = false, currentLine = -1)
    variablesMap.clear()
    return state
  }

  private fun advanceToExecutableLine() {
    while (currentIdx < codeLines.size) {
      val line = codeLines[currentIdx].trim()
      if (line.isNotEmpty() && !line.startsWith("//") && !line.startsWith("/*") && line != "{" && line != "}") {
        break
      }
      currentIdx++
    }
  }

  private fun inspectLine(line: String) {
    // Detect vector
    if (line.contains("vector<") || line.contains("std::vector")) {
      val nameMatch = Regex("""vector\s*<([^>]+)>\s*(\w+)""").find(line)
      if (nameMatch != null) {
        val type = nameMatch.groupValues[1]
        val name = nameMatch.groupValues[2]
        val elements = if (line.contains("{") && line.contains("}")) {
          line.substringAfter("{").substringBefore("}").trim()
        } else ""
        val count = if (elements.isEmpty()) 0 else elements.split(",").size
        variablesMap[name] = DebugVariable(
          name = name,
          type = "std::vector<$type>",
          value = "{$elements} [size: $count]",
          isUpdated = true
        )
      }
    }

    // Detect sort
    if (line.contains("sort(") || line.contains("std::sort")) {
      val match = Regex("""sort\s*\(\s*(\w+)""").find(line)
      if (match != null) {
        val name = match.groupValues[1]
        val existing = variablesMap[name]
        if (existing != null) {
          variablesMap[name] = existing.copy(
            value = existing.value + " (sorted)",
            isUpdated = true
          )
        }
      }
    }

    // Detect variable declarations (int, double, string, etc.)
    val varDecl = Regex("""(int|long|double|float|bool|string|std::string|char)\s+(\w+)\s*=\s*([^;]+);""").find(line)
    if (varDecl != null) {
      val type = varDecl.groupValues[1]
      val name = varDecl.groupValues[2]
      val value = varDecl.groupValues[3].trim()
      variablesMap[name] = DebugVariable(name = name, type = type, value = value, isUpdated = true)
      return
    }

    // Detect assignments (e.g., sum += s; or x = 42;)
    val assign = Regex("""(\w+)\s*([+\-*/]?=)\s*([^;]+);""").find(line)
    if (assign != null) {
      val name = assign.groupValues[1]
      val op = assign.groupValues[2]
      val value = assign.groupValues[3].trim()
      val existing = variablesMap[name]
      if (existing != null) {
        variablesMap[name] = existing.copy(
          value = if (op == "=") value else "${existing.value} $op $value",
          isUpdated = true
        )
      }
    }

    // Detect for loop variable: for (int i = 0; ...)
    val forVar = Regex("""for\s*\(\s*(?:int|auto)?\s*(\w+)\s*=\s*(\d+)""").find(line)
    if (forVar != null) {
      val name = forVar.groupValues[1]
      val initVal = forVar.groupValues[2]
      variablesMap[name] = DebugVariable(name = name, type = "int", value = initVal, isUpdated = true)
    }

    // Detect range for: for (int num : numbers)
    val rangeFor = Regex("""for\s*\(\s*(?:auto|int)?\s*(\w+)\s*:\s*(\w+)""").find(line)
    if (rangeFor != null) {
      val loopVar = rangeFor.groupValues[1]
      val container = rangeFor.groupValues[2]
      variablesMap[loopVar] = DebugVariable(name = loopVar, type = "int", value = "item of $container", isUpdated = true)
    }
  }

  private fun getVariableList(): List<DebugVariable> {
    return variablesMap.values.sortedBy { it.name }
  }
}
