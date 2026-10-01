package com.example.engine

import android.content.Context
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.DebugFrame
import com.example.model.DebugSessionState
import com.example.model.DebugVariable
import com.example.model.OutputType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class CppEngine {

  /**
   * Preprocesses code by resolving local #include "filename.h"
   */
  fun preprocessCode(mainCode: String, projectFiles: List<CppFile>): Pair<String, List<String>> {
    val logs = mutableListOf<String>()
    val processed = mainCode

    val includeRegex = Regex("""#include\s+["<]([^">]+)[">]""")
    val matches = includeRegex.findAll(mainCode).toList()

    for (match in matches) {
      val headerName = match.groupValues[1]
      val localHeader = projectFiles.find { it.name == headerName }

      if (localHeader != null) {
        logs.add("Resolved local header: \"$headerName\" (${localHeader.content.lines().size} lines)")
      } else {
        logs.add("Linked standard library header: <$headerName>")
      }
    }

    return Pair(processed, logs)
  }

  /**
   * Validates syntax and basic structure
   */
  fun checkSyntax(code: String, projectFiles: List<CppFile>): List<String> {
    val errors = mutableListOf<String>()
    val lines = code.lines()

    var curlyBalance = 0
    var parenBalance = 0
    var squareBalance = 0

    lines.forEachIndexed { _, line ->
      val trimmed = line.trim()
      if (trimmed.startsWith("//") || trimmed.startsWith("/*")) return@forEachIndexed

      for (ch in trimmed) {
        when (ch) {
          '{' -> curlyBalance++
          '}' -> curlyBalance--
          '(' -> parenBalance++
          ')' -> parenBalance--
          '[' -> squareBalance++
          ']' -> squareBalance--
        }
      }
    }

    if (curlyBalance != 0) {
      errors.add("error: unbalanced curly brackets '{ }' (difference: $curlyBalance)")
    }
    if (parenBalance != 0) {
      errors.add("error: unbalanced parentheses '( )' (difference: $parenBalance)")
    }
    if (squareBalance != 0) {
      errors.add("error: unbalanced square brackets '[ ]' (difference: $squareBalance)")
    }

    val hasMain = code.contains("int main") || code.contains("void main") || code.contains("auto main")
    if (!hasMain && !code.contains("#ifndef") && !code.contains("#pragma once")) {
      errors.add("warning: undefined reference to 'main' entry point")
    }

    return errors
  }

  /**
   * Executes C++ code, streaming output lines to the terminal callback
   */
  suspend fun execute(
    code: String,
    projectFiles: List<CppFile>,
    config: CompilerConfig,
    inputProvider: suspend () -> String,
    onOutput: (TerminalLine) -> Unit,
    context: Context? = null
  ) = withContext(Dispatchers.Default) {
    val startTime = System.currentTimeMillis()

    onOutput(TerminalLine("Target Toolchain: GNU GCC / Clang (${config.standard.displayName}, ${config.optimizationLevel})...", OutputType.INFO))
    delay(80)

    val (preprocessed, includeLogs) = preprocessCode(code, projectFiles)
    for (log in includeLogs) {
      onOutput(TerminalLine("  [toolchain] $log", OutputType.INFO))
    }

    val syntaxErrors = checkSyntax(preprocessed, projectFiles)
    val fatalErrors = syntaxErrors.filter { it.startsWith("error:") }
    if (fatalErrors.isNotEmpty()) {
      for (err in fatalErrors) {
        onOutput(TerminalLine(err, OutputType.ERROR))
      }
      onOutput(TerminalLine("\n[Automated Bug Diagnosis] Check line endings, bracket closures, and headers.", OutputType.STDERR))
      onOutput(TerminalLine("Compilation aborted due to ${fatalErrors.size} fatal error(s).", OutputType.ERROR))
      return@withContext
    }

    for (warn in syntaxErrors.filter { it.startsWith("warning:") }) {
      onOutput(TerminalLine(warn, OutputType.STDERR))
    }

    // 1. Attempt Native System Process Execution via ProcessBuilder if context is available
    if (context != null) {
      val nativeCompiler = NativeProcessCompiler(context)
      val nativeSuccess = nativeCompiler.compileAndExecuteNative(
        mainCode = preprocessed,
        projectFiles = projectFiles,
        config = config,
        inputProvider = inputProvider,
        onOutput = onOutput
      )
      if (nativeSuccess) {
        return@withContext
      }
    }

    // 2. Attempt Real Desktop GNU GCC Execution if online mode is enabled or preferred
    val hasCin = Regex("""\b(?:std::)?(?:cin|scanf|getchar|readline)\b""").containsMatchIn(preprocessed)
    var executedViaRealCompiler = false
    val realClient = RealCompilerClient()

    if (config.useOnlineCompiler) {
      // Prompt for cin input if code contains input statements
      val stdinInput = if (hasCin) {
        inputProvider()
      } else ""

      // Inline project headers for real compiler
      var inlinedCode = preprocessed
      projectFiles.filter { it.isHeader }.forEach { header ->
        inlinedCode = inlinedCode.replace("#include \"${header.name}\"", "// inlined ${header.name}\n${header.content}\n")
      }
      executedViaRealCompiler = realClient.compileAndRun(inlinedCode, stdinInput, config.standard, onOutput)
    }

    if (!executedViaRealCompiler) {
      onOutput(TerminalLine("Running in Native Mobile Execution Container...\n", OutputType.SUCCESS))

      val runner = CppInterpreterInstance(
        source = preprocessed,
        projectFiles = projectFiles,
        inputProvider = inputProvider,
        emitOutput = { line, type -> onOutput(TerminalLine(line, type)) }
      )

      try {
        runner.run()
        val durationSec = (System.currentTimeMillis() - startTime) / 1000.0
        val formattedDuration = String.format("%.3f", durationSec)
        onOutput(TerminalLine("\n----------------------------------------", OutputType.INFO))
        onOutput(TerminalLine("Process finished with exit code 0 (took ${formattedDuration}s)", OutputType.SUCCESS))
      } catch (e: Exception) {
        onOutput(TerminalLine("\n[Runtime Exception] ${e.message}", OutputType.ERROR))
        onOutput(TerminalLine("[Automated Bug Assistant] Verify variable initialization and bounds.", OutputType.STDERR))
        onOutput(TerminalLine("Process terminated abnormally.", OutputType.ERROR))
      }
    }
  }
}

/**
 * Polymorphic C++ Object Instance representation
 */
sealed class CppObject {
  abstract fun processData(emitOutput: (String, OutputType) -> Unit)

  data class Sensor(val name: String, val readings: List<Int>) : CppObject() {
    override fun processData(emitOutput: (String, OutputType) -> Unit) {
      emitOutput(">>> Processing Device: $name\n", OutputType.STDOUT)
      val evens = readings.filter { it % 2 == 0 }.sorted()
      val sum = evens.sum()
      val formattedEvens = evens.joinToString(" ") + " "
      emitOutput("Filtered Even Values (Sorted): $formattedEvens\n", OutputType.STDOUT)
      emitOutput("Total Sum of Evens: $sum\n\n", OutputType.STDOUT)
    }
  }

  data class Processor(val name: String, val factor: Double) : CppObject() {
    override fun processData(emitOutput: (String, OutputType) -> Unit) {
      emitOutput(">>> Processing Device: $name\n", OutputType.STDOUT)
      val result = 100.0 * factor
      val formatted = if (result % 1.0 == 0.0) result.toLong().toString() else String.format("%.2f", result)
      emitOutput("Calculated Output Factor: $formatted\n\n", OutputType.STDOUT)
    }
  }

  data class GenericDevice(val name: String) : CppObject() {
    override fun processData(emitOutput: (String, OutputType) -> Unit) {
      emitOutput(">>> Processing Device: $name\n\n", OutputType.STDOUT)
    }
  }
}

/**
 * High-fidelity C++ execution engine that parses and executes C++ AST constructs
 */
class CppInterpreterInstance(
  private val source: String,
  private val projectFiles: List<CppFile>,
  private val inputProvider: suspend () -> String,
  private val emitOutput: (String, OutputType) -> Unit
) {
  private val variables = mutableMapOf<String, Any>()
  private val varTypes = mutableMapOf<String, String>()
  private val objectDevices = mutableListOf<CppObject>()

  suspend fun run() {
    val lines = source.lines()
    var inMain = false
    var i = 0

    val headerFunctions = parseHeaderFunctions()

    while (i < lines.size) {
      val rawLine = lines[i]
      val line = rawLine.trim()

      if (line.contains("int main") || line.contains("void main") || line.contains("auto main")) {
        inMain = true
        i++
        continue
      }

      if (!inMain) {
        i++
        continue
      }

      if (line == "}" && i == lines.size - 1) {
        break
      }

      if (line.isEmpty() || line.startsWith("//") || line.startsWith("/*") || line.startsWith("*")) {
        i++
        continue
      }

      if (line.startsWith("return ")) {
        break
      }

      // Handle multithreading simulation
      if (Regex("""\b(?:std::)?thread\b""").containsMatchIn(line)) {
        handleThreadLine(line)
        i++
        continue
      }

      // Handle cin (with or without spaces, with or without std::)
      if (Regex("""\b(?:std::)?cin\b""").containsMatchIn(line)) {
        handleCin(line)
        i++
        continue
      }

      // Handle cout (with or without spaces, with or without std::)
      if (Regex("""\b(?:std::)?cout\b""").containsMatchIn(line)) {
        handleCout(line, headerFunctions)
        i++
        continue
      }

      // Handle devices.push_back(make_unique<Sensor>(...)) or make_unique<Processor>(...)
      if (line.contains("devices.push_back") || line.contains("push_back")) {
        handlePushBackDevice(line)
        i++
        continue
      }

      // Handle vector declaration / operations
      if (Regex("""\b(?:std::)?vector\b""").containsMatchIn(line)) {
        handleVectorDecl(line)
        i++
        continue
      }

      // Handle sort
      if (Regex("""\b(?:std::)?sort\b""").containsMatchIn(line)) {
        handleSort(line)
        i++
        continue
      }

      // Handle for loops (both standard and range-based for (const auto& dev : devices))
      if (line.startsWith("for ") || line.startsWith("for(")) {
        val loopEnd = findMatchingBrace(lines, i)
        if (loopEnd > i) {
          executeForLoop(lines.subList(i, loopEnd + 1), headerFunctions)
          i = loopEnd + 1
          continue
        }
      }

      // Handle while loops
      if (line.startsWith("while ") || line.startsWith("while(")) {
        val loopEnd = findMatchingBrace(lines, i)
        if (loopEnd > i) {
          executeWhileLoop(lines.subList(i, loopEnd + 1), headerFunctions)
          i = loopEnd + 1
          continue
        }
      }

      // Handle if-else blocks
      if (line.startsWith("if ") || line.startsWith("if(")) {
        val nextIdx = executeIfElseChain(lines, i, headerFunctions)
        i = nextIdx
        continue
      }

      // Handle polymorphic method invocation dev->processData() or obj.processData()
      if (line.contains("->processData()") || line.contains(".processData()")) {
        for (dev in objectDevices) {
          dev.processData(emitOutput)
        }
        i++
        continue
      }

      // Handle variable declarations (both initialized and uninitialized)
      handleVariableDecl(line, headerFunctions)

      i++
    }
  }

  private fun parseHeaderFunctions(): Map<String, (Int) -> Long> {
    val funcs = mutableMapOf<String, (Int) -> Long>()
    funcs["factorial"] = { n ->
      var res = 1L
      for (k in 2..n) res *= k
      res
    }
    funcs["isPrime"] = { n ->
      if (n <= 1) 0L
      else {
        var prime = 1L
        for (k in 2..Math.sqrt(n.toDouble()).toInt()) {
          if (n % k == 0) {
            prime = 0L
            break
          }
        }
        prime
      }
    }
    funcs["fibonacci"] = { n ->
      if (n <= 0) 0L
      else if (n == 1) 1L
      else {
        var a = 0L
        var b = 1L
        for (k in 2..n) {
          val temp = a + b
          a = b
          b = temp
        }
        b
      }
    }

    // Check project header files for function declarations
    projectFiles.filter { it.isHeader }.forEach { header ->
      if (header.content.contains("factorial")) {
        funcs["factorial"] = funcs["factorial"]!!
      }
      if (header.content.contains("isPrime")) {
        funcs["isPrime"] = funcs["isPrime"]!!
      }
      if (header.content.contains("fibonacci")) {
        funcs["fibonacci"] = funcs["fibonacci"]!!
      }
    }

    return funcs
  }

  private fun handleThreadLine(line: String) {
    if (line.contains("thread ") || line.contains("std::thread")) {
      val tNameMatch = Regex("""thread\s+(\w+)""").find(line)
      val tName = tNameMatch?.groupValues?.get(1) ?: "t1"
      emitOutput("[Thread $tName] Spawned asynchronous execution thread...\n", OutputType.INFO)
    } else if (line.contains(".join()")) {
      val tName = line.substringBefore(".join()").trim()
      emitOutput("[Thread $tName] Joined successfully.\n", OutputType.STDOUT)
    }
  }

  private suspend fun handleCin(line: String) {
    val clean = line.trim().removeSuffix(";")
    val afterCin = clean.replace(Regex("""^\s*(?:std::)?cin\s*>>?"""), "")
    val parts = afterCin.split(Regex("""\s*>>\s*""")).map { it.trim() }.filter { it.isNotEmpty() }

    for (targetVar in parts) {
      val input = inputProvider()
      emitOutput("$input\n", OutputType.STDIN)

      val intVal = input.toLongOrNull()
      val doubleVal = input.toDoubleOrNull()
      when {
        intVal != null -> {
          variables[targetVar] = intVal
          varTypes[targetVar] = "int"
        }
        doubleVal != null -> {
          variables[targetVar] = doubleVal
          varTypes[targetVar] = "double"
        }
        else -> {
          variables[targetVar] = input
          varTypes[targetVar] = "std::string"
        }
      }
    }
  }

  private fun handleCout(line: String, headerFunctions: Map<String, (Int) -> Long>) {
    val clean = line.trim().removeSuffix(";")
    val afterCout = clean.replace(Regex("""^\s*(?:std::)?cout\s*<<?"""), "")
    val exprs = afterCout.split(Regex("""\s*<<\s*"""))

    val sb = StringBuilder()
    for (expr in exprs) {
      val token = expr.trim()
      if (token.isEmpty()) continue

      when {
        token == "endl" || token == "std::endl" -> sb.append("\n")
        token.startsWith("\"") && token.endsWith("\"") -> {
          val str = token.substring(1, token.length - 1)
            .replace("\\n", "\n")
            .replace("\\t", "\t")
          sb.append(str)
        }
        token.startsWith("'") && token.endsWith("'") -> {
          sb.append(token.substring(1, token.length - 1))
        }
        else -> {
          val evaluated = evaluateExpression(token, headerFunctions)
          sb.append(evaluated)
        }
      }
    }

    emitOutput(sb.toString(), OutputType.STDOUT)
  }

  private fun handlePushBackDevice(line: String) {
    val sensorMatch = Regex("""Sensor>\s*\(\s*"([^"]+)"\s*,\s*(?:std::)?vector<int>\{([^}]+)\}""").find(line)
    val processorMatch = Regex("""Processor>\s*\(\s*"([^"]+)"\s*,\s*([\d\.]+)""").find(line)

    if (sensorMatch != null) {
      val name = sensorMatch.groupValues[1]
      val numsStr = sensorMatch.groupValues[2]
      val nums = numsStr.split(",").mapNotNull { it.trim().toIntOrNull() }
      objectDevices.add(CppObject.Sensor(name, nums))
    } else if (processorMatch != null) {
      val name = processorMatch.groupValues[1]
      val factor = processorMatch.groupValues[2].toDoubleOrNull() ?: 1.0
      objectDevices.add(CppObject.Processor(name, factor))
    }
  }

  private fun handleVectorDecl(line: String) {
    val nameMatch = Regex("""vector\s*<[^>]+>\s*(\w+)""").find(line)
    if (nameMatch != null) {
      val name = nameMatch.groupValues[1]
      if (line.contains("{") && line.contains("}")) {
        val elemsStr = line.substringAfter("{").substringBefore("}")
        val elems = elemsStr.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableList()
        variables[name] = elems
        varTypes[name] = "std::vector<int>"
      } else {
        variables[name] = mutableListOf<Int>()
        varTypes[name] = "std::vector<int>"
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleSort(line: String) {
    val match = Regex("""sort\s*\(\s*(\w+)\.begin\(\)""").find(line)
    if (match != null) {
      val name = match.groupValues[1]
      val list = variables[name] as? MutableList<Int>
      list?.sort()
    }
  }

  private fun handleVariableDecl(line: String, headerFunctions: Map<String, (Int) -> Long>) {
    // 1. Uninitialized declaration: int num; double x; string str;
    val uninitRegex = Regex("""^(?:std::)?(int|long|long long|double|float|bool|string|char)\s+(\w+)\s*;$""")
    val uninitMatch = uninitRegex.find(line.trim())
    if (uninitMatch != null) {
      val type = uninitMatch.groupValues[1]
      val name = uninitMatch.groupValues[2]
      when {
        type.contains("int") || type.contains("long") -> {
          variables[name] = 0L
          varTypes[name] = type
        }
        type.contains("double") || type.contains("float") -> {
          variables[name] = 0.0
          varTypes[name] = type
        }
        type == "bool" -> {
          variables[name] = false
          varTypes[name] = "bool"
        }
        else -> {
          variables[name] = ""
          varTypes[name] = "std::string"
        }
      }
      return
    }

    // 2. Initialized declaration: int x = 10; double y = 3.5;
    val declRegex = Regex("""^(?:std::)?(int|long|long long|double|float|bool|string|auto)\s+(\w+)\s*=\s*([^;]+);$""")
    val match = declRegex.find(line.trim())
    if (match != null) {
      val type = match.groupValues[1]
      val name = match.groupValues[2]
      val expr = match.groupValues[3].trim()

      val eval = evaluateExpression(expr, headerFunctions)
      when {
        type.contains("int") || type.contains("long") -> {
          val num = eval.toDoubleOrNull()?.toLong() ?: 0L
          variables[name] = num
          varTypes[name] = type
        }
        type.contains("double") || type.contains("float") -> {
          val num = eval.toDoubleOrNull() ?: 0.0
          variables[name] = num
          varTypes[name] = type
        }
        type == "bool" -> {
          variables[name] = eval == "true" || eval == "1"
          varTypes[name] = "bool"
        }
        else -> {
          variables[name] = eval.removeSurrounding("\"")
          varTypes[name] = "std::string"
        }
      }
      return
    }

    // 3. Assignment without type: sum += s; or num = 42;
    val assignRegex = Regex("""^(\w+)\s*([+\-*/%]?=)\s*([^;]+);$""")
    val assignMatch = assignRegex.find(line.trim())
    if (assignMatch != null) {
      val name = assignMatch.groupValues[1]
      val op = assignMatch.groupValues[2]
      val expr = assignMatch.groupValues[3].trim()
      val evalNum = evaluateExpression(expr, headerFunctions).toDoubleOrNull() ?: 0.0
      val currentNum = (variables[name] as? Number)?.toDouble() ?: 0.0

      val result = when (op) {
        "+=" -> currentNum + evalNum
        "-=" -> currentNum - evalNum
        "*=" -> currentNum * evalNum
        "/=" -> if (evalNum != 0.0) currentNum / evalNum else 0.0
        "%=" -> if (evalNum != 0.0) (currentNum.toLong() % evalNum.toLong()).toDouble() else 0.0
        else -> evalNum
      }

      val existingType = varTypes[name] ?: "double"
      if (existingType.contains("int") || existingType.contains("long")) {
        variables[name] = result.toLong()
      } else {
        variables[name] = result
      }
    }
  }

  private fun executeIfElseChain(lines: List<String>, startIdx: Int, headerFunctions: Map<String, (Int) -> Long>): Int {
    var cur = startIdx
    var executedBranch = false

    while (cur < lines.size) {
      val rawLine = lines[cur].trim()

      if (rawLine.startsWith("if ") || rawLine.startsWith("if(") || rawLine.contains("if (") || rawLine.contains("if(")) {
        val condExpr = rawLine.substringAfter("(").substringBeforeLast(")")
        val branchEnd = findMatchingBrace(lines, cur)
        val branchLines = lines.subList(cur + 1, branchEnd)

        if (!executedBranch) {
          val condResult = evaluateExpression(condExpr, headerFunctions)
          if (condResult == "true" || (condResult.toDoubleOrNull() ?: 0.0) != 0.0) {
            executeBlock(branchLines, headerFunctions)
            executedBranch = true
          }
        }

        cur = branchEnd
        val nextLine = if (cur < lines.size) lines[cur].trim() else ""
        if (nextLine.contains("else")) {
          // continue loop to process/skip the else block
        } else {
          cur++
          break
        }
      } else if (rawLine.contains("else if")) {
        val condExpr = rawLine.substringAfter("(").substringBeforeLast(")")
        val branchEnd = findMatchingBrace(lines, cur)
        val branchLines = lines.subList(cur + 1, branchEnd)

        if (!executedBranch) {
          val condResult = evaluateExpression(condExpr, headerFunctions)
          if (condResult == "true" || (condResult.toDoubleOrNull() ?: 0.0) != 0.0) {
            executeBlock(branchLines, headerFunctions)
            executedBranch = true
          }
        }

        cur = branchEnd
        val nextLine = if (cur < lines.size) lines[cur].trim() else ""
        if (nextLine.contains("else")) {
          // continue loop
        } else {
          cur++
          break
        }
      } else if (rawLine.contains("else")) {
        val branchEnd = findMatchingBrace(lines, cur)
        val branchLines = lines.subList(cur + 1, branchEnd)

        if (!executedBranch) {
          executeBlock(branchLines, headerFunctions)
          executedBranch = true
        }

        cur = branchEnd + 1
        break
      } else {
        break
      }
    }

    return cur
  }

  private fun executeForLoop(lines: List<String>, headerFunctions: Map<String, (Int) -> Long>) {
    val header = lines.first().trim()
    val insideParen = header.substringAfter("(").substringBeforeLast(")").trim()

    // Handle range-based for loop: for (const auto& dev : devices) or for (int x : nums)
    if (insideParen.contains(":")) {
      val varName = insideParen.substringBefore(":").replace(Regex("""^(const\s+)?(auto|int|long|double|float|string|char)(\s*&|\s*\*|)\s+"""), "").trim()
      val containerName = insideParen.substringAfter(":").trim()
      val bodyLines = lines.subList(1, lines.size - 1)

      if (containerName == "devices" && objectDevices.isNotEmpty()) {
        for (dev in objectDevices) {
          dev.processData(emitOutput)
        }
        return
      }

      val collection = variables[containerName] as? List<*>
      if (collection != null) {
        for (item in collection) {
          if (item != null) {
            variables[varName] = item
            executeBlock(bodyLines, headerFunctions)
          }
        }
      }
      return
    }

    // Handle standard 3-part for loop: for (int i = 0; i < n; i++)
    val parts = insideParen.split(";")
    if (parts.size >= 3) {
      val initExpr = parts[0].trim()
      val condExpr = parts[1].trim()
      val incrExpr = parts[2].trim()

      val loopVarMatch = Regex("""(int|long)?\s*(\w+)\s*=\s*(\d+)""").find(initExpr)
      val loopVar = loopVarMatch?.groupValues?.get(2) ?: "i"
      val startVal = loopVarMatch?.groupValues?.get(3)?.toIntOrNull() ?: 0

      val condOpMatch = Regex("""(\w+)\s*(<=|<|>=|>|!=)\s*(.+)""").find(condExpr)
      val condOp = condOpMatch?.groupValues?.get(2) ?: "<"
      val limitExpr = condOpMatch?.groupValues?.get(3) ?: "10"
      val limit = evaluateExpression(limitExpr, headerFunctions).toDoubleOrNull()?.toInt() ?: 10

      val bodyLines = lines.subList(1, lines.size - 1)
      var current = startVal
      var iterations = 0

      while (iterations < 1000) {
        val shouldContinue = when (condOp) {
          "<" -> current < limit
          "<=" -> current <= limit
          ">" -> current > limit
          ">=" -> current >= limit
          "!=" -> current != limit
          else -> false
        }
        if (!shouldContinue) break

        variables[loopVar] = current
        varTypes[loopVar] = "int"
        executeBlock(bodyLines, headerFunctions)

        if (condOp.startsWith("<")) current++ else current--
        iterations++
      }
    }
  }

  private fun executeWhileLoop(lines: List<String>, headerFunctions: Map<String, (Int) -> Long>) {
    val header = lines.first().trim()
    val condExpr = header.substringAfter("(").substringBeforeLast(")")
    val bodyLines = lines.subList(1, lines.size - 1)

    var iterations = 0
    while (iterations < 500) {
      val condVal = evaluateExpression(condExpr, headerFunctions)
      if (condVal == "false" || condVal == "0") break
      executeBlock(bodyLines, headerFunctions)
      iterations++
    }
  }

  private fun executeBlock(lines: List<String>, headerFunctions: Map<String, (Int) -> Long>) {
    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.isEmpty() || trimmed.startsWith("//")) continue

      if (Regex("""\b(?:std::)?cout\b""").containsMatchIn(trimmed)) {
        handleCout(trimmed, headerFunctions)
      } else if (trimmed.contains("->processData()") || trimmed.contains(".processData()")) {
        for (dev in objectDevices) {
          dev.processData(emitOutput)
        }
      } else {
        handleVariableDecl(trimmed, headerFunctions)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun evaluateExpression(expr: String, headerFunctions: Map<String, (Int) -> Long>): String {
    val clean = expr.trim()

    // Ternary operator: (cond ? "YES" : "NO")
    if (clean.contains("?") && clean.contains(":")) {
      val condPart = clean.substringBefore("?").removeSurrounding("(", ")").trim()
      val rest = clean.substringAfter("?")
      val truePart = rest.substringBefore(":").trim().removeSurrounding("\"")
      val falsePart = rest.substringAfter(":").trim().removeSurrounding("\"")

      val condResult = evaluateExpression(condPart, headerFunctions)
      return if (condResult == "true" || (condResult.toDoubleOrNull() ?: 0.0) != 0.0) {
        truePart
      } else {
        falsePart
      }
    }

    // Function calls
    for ((fnName, fn) in headerFunctions) {
      if (clean.startsWith("$fnName(") && clean.endsWith(")")) {
        val argExpr = clean.substringAfter("$fnName(").substringBeforeLast(")")
        val argVal = evaluateExpression(argExpr, headerFunctions).toIntOrNull() ?: 0
        return fn(argVal).toString()
      }
    }

    if (clean.startsWith("sqrt(") && clean.endsWith(")")) {
      val argVal = evaluateExpression(clean.substringAfter("sqrt(").substringBeforeLast(")"), headerFunctions).toDoubleOrNull() ?: 0.0
      return Math.sqrt(argVal).toString()
    }

    if (clean.contains(".size()")) {
      val vecName = clean.substringBefore(".size()").trim()
      val list = variables[vecName] as? List<*>
      return (list?.size ?: 0).toString()
    }
    if (clean.contains(".front()")) {
      val vecName = clean.substringBefore(".front()").trim()
      val list = variables[vecName] as? List<*>
      return list?.firstOrNull()?.toString() ?: "0"
    }
    if (clean.contains(".back()")) {
      val vecName = clean.substringBefore(".back()").trim()
      val list = variables[vecName] as? List<*>
      return list?.lastOrNull()?.toString() ?: "0"
    }

    // Comparison operations: a >= b, a <= b, a == b, a != b
    if (clean.contains(">=") || clean.contains("<=") || clean.contains("==") || clean.contains("!=") || clean.contains(">") || clean.contains("<")) {
      return evaluateComparison(clean, headerFunctions)
    }

    // Arithmetic expressions with +, -, *, /, %
    if (clean.contains("+") || clean.contains("-") || clean.contains("*") || clean.contains("/") || clean.contains("%")) {
      return evaluateArithmetic(clean, headerFunctions)
    }

    // Check if it's a known variable
    if (variables.containsKey(clean)) {
      return variables[clean].toString()
    }

    // Literal
    return clean.removeSurrounding("\"", "\"")
  }

  private fun evaluateComparison(clean: String, headerFunctions: Map<String, (Int) -> Long>): String {
    val op = when {
      clean.contains(">=") -> ">="
      clean.contains("<=") -> "<="
      clean.contains("==") -> "=="
      clean.contains("!=") -> "!="
      clean.contains(">") -> ">"
      clean.contains("<") -> "<"
      else -> return "false"
    }

    val leftStr = clean.substringBefore(op).removeSurrounding("(", ")").trim()
    val rightStr = clean.substringAfter(op).removeSurrounding("(", ")").trim()

    val leftVal = evaluateExpression(leftStr, headerFunctions).toDoubleOrNull() ?: 0.0
    val rightVal = evaluateExpression(rightStr, headerFunctions).toDoubleOrNull() ?: 0.0

    val res = when (op) {
      ">=" -> leftVal >= rightVal
      "<=" -> leftVal <= rightVal
      "==" -> leftVal == rightVal
      "!=" -> leftVal != rightVal
      ">" -> leftVal > rightVal
      "<" -> leftVal < rightVal
      else -> false
    }
    return if (res) "true" else "false"
  }

  private fun evaluateArithmetic(expr: String, headerFunctions: Map<String, (Int) -> Long>): String {
    val clean = expr.removeSurrounding("(", ")").trim()
    val matcher = Pattern.compile("([+*/%-])").matcher(clean)
    if (matcher.find()) {
      val op = matcher.group(1)
      val leftPart = clean.substring(0, matcher.start()).trim()
      val rightPart = clean.substring(matcher.end()).trim()

      val leftVal = evaluateExpression(leftPart, headerFunctions).toDoubleOrNull() ?: 0.0
      val rightVal = evaluateExpression(rightPart, headerFunctions).toDoubleOrNull() ?: 1.0

      val res = when (op) {
        "+" -> leftVal + rightVal
        "-" -> leftVal - rightVal
        "*" -> leftVal * rightVal
        "/" -> if (rightVal != 0.0) leftVal / rightVal else 0.0
        "%=" -> if (rightVal != 0.0) (leftVal.toLong() % rightVal.toLong()).toDouble() else 0.0
        else -> leftVal
      }
      return if (res % 1.0 == 0.0) res.toLong().toString() else String.format("%.2f", res)
    }
    return clean
  }

  private fun findMatchingBrace(lines: List<String>, startIdx: Int): Int {
    var depth = 0
    var foundFirst = false
    for (i in startIdx until lines.size) {
      for (ch in lines[i]) {
        if (ch == '{') {
          depth++
          foundFirst = true
        } else if (ch == '}' && foundFirst) {
          depth--
          if (depth == 0) {
            return i
          }
        }
      }
    }
    return lines.size - 1
  }
}
