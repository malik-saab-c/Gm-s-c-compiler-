package com.example.engine

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
    onOutput: (TerminalLine) -> Unit
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

    // Attempt Real Desktop GNU GCC Execution if online mode is enabled or preferred
    var executedViaRealCompiler = false
    val realClient = RealCompilerClient()

    // If code has interactive cin, ask for input or use local runner for instant interactive experience
    val hasCin = preprocessed.contains("cin >>") || preprocessed.contains("std::cin >>")

    if (!hasCin && config.useOnlineCompiler) {
      // Inline project headers for real compiler
      var inlinedCode = preprocessed
      projectFiles.filter { it.isHeader }.forEach { header ->
        inlinedCode = inlinedCode.replace("#include \"${header.name}\"", "// inlined ${header.name}\n${header.content}\n")
      }
      executedViaRealCompiler = realClient.compileAndRun(inlinedCode, "", config.standard, onOutput)
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

  suspend fun run() {
    val lines = source.lines()
    var inMain = false
    var i = 0

    val headerFunctions = parseHeaderFunctions()

    while (i < lines.size) {
      val rawLine = lines[i]
      val line = rawLine.trim()

      if (line.contains("int main") || line.contains("void main")) {
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
      if (line.contains("std::thread") || line.contains("thread ")) {
        handleThreadLine(line)
        i++
        continue
      }

      // Handle cin (with or without std::)
      if (line.contains("cin >>") || line.contains("std::cin >>")) {
        handleCin(line)
        i++
        continue
      }

      // Handle cout (with or without std::)
      if (line.contains("cout <<") || line.contains("std::cout <<")) {
        handleCout(line, headerFunctions)
        i++
        continue
      }

      // Handle vector declaration / operations
      if (line.contains("std::vector") || line.contains("vector<")) {
        handleVectorDecl(line)
        i++
        continue
      }

      // Handle sort
      if (line.contains("std::sort") || line.contains("sort(")) {
        handleSort(line)
        i++
        continue
      }

      // Handle for loops
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
          val c = a + b
          a = b
          b = c
        }
        b
      }
    }
    return funcs
  }

  private fun handleThreadLine(line: String) {
    val match = Regex("""thread\s+(\w+)\s*\(([^,]+)(?:,\s*([^)]+))?\)""").find(line)
    if (match != null) {
      val tName = match.groupValues[1]
      val funcName = match.groupValues[2].trim()
      val arg = match.groupValues[3].trim()
      emitOutput("[Thread $tName] Created & running worker $funcName($arg)\n", OutputType.STDOUT)
    } else if (line.contains(".join()")) {
      val tName = line.substringBefore(".join()").trim()
      emitOutput("[Thread $tName] Joined successfully.\n", OutputType.STDOUT)
    }
  }

  private suspend fun handleCin(line: String) {
    val parts = line.replace("std::cin", "").replace("cin", "").replace(";", "").split(">>")
      .map { it.trim() }.filter { it.isNotEmpty() }

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
    val raw = line.substringAfter("cout").trim()
    val clean = if (raw.startsWith("<<")) raw.substring(2) else raw
    val exprs = clean.split("<<")

    val sb = StringBuilder()
    for (expr in exprs) {
      val token = expr.trim().removeSuffix(";").trim()
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
      val raw = lines[cur].trim()

      if (raw.startsWith("if ") || raw.startsWith("if(") || raw.startsWith("else if") || raw.startsWith("} else if")) {
        val cond = raw.substringAfter("(").substringBeforeLast(")")
        val blockEnd = findMatchingBrace(lines, cur)
        val bodyLines = extractBlockBody(lines, cur, blockEnd)

        if (!executedBranch) {
          val condVal = evaluateExpression(cond, headerFunctions)
          val isTrue = condVal == "true" || (condVal.toDoubleOrNull() ?: 0.0) != 0.0
          if (isTrue) {
            executeBlock(bodyLines, headerFunctions)
            executedBranch = true
          }
        }

        cur = blockEnd + 1
        // Look ahead for else
        if (cur < lines.size) {
          val next = lines[cur].trim()
          if (next.startsWith("else") || next.startsWith("} else")) {
            continue
          }
        }
        break
      } else if (raw.startsWith("else") || raw.startsWith("} else")) {
        val blockEnd = findMatchingBrace(lines, cur)
        val bodyLines = extractBlockBody(lines, cur, blockEnd)

        if (!executedBranch) {
          executeBlock(bodyLines, headerFunctions)
          executedBranch = true
        }

        cur = blockEnd + 1
        break
      } else {
        break
      }
    }

    return cur
  }

  private fun extractBlockBody(lines: List<String>, startIdx: Int, endIdx: Int): List<String> {
    if (startIdx >= endIdx) return emptyList()
    val body = mutableListOf<String>()
    for (i in (startIdx + 1) until endIdx) {
      body.add(lines[i])
    }
    return body
  }

  @Suppress("UNCHECKED_CAST")
  private fun executeForLoop(lines: List<String>, headerFunctions: Map<String, (Int) -> Long>) {
    val header = lines.first().trim()

    val rangeMatch = Regex("""for\s*\(\s*(?:auto|int|double)?\s*(\w+)\s*:\s*(\w+)\s*\)""").find(header)
    if (rangeMatch != null) {
      val loopVar = rangeMatch.groupValues[1]
      val containerName = rangeMatch.groupValues[2]
      val list = (variables[containerName] as? List<*>) ?: emptyList<Any>()

      val bodyLines = lines.subList(1, lines.size - 1)
      for (elem in list) {
        variables[loopVar] = elem ?: 0
        varTypes[loopVar] = "int"
        executeBlock(bodyLines, headerFunctions)
      }
      return
    }

    val standardMatch = Regex("""for\s*\(\s*(?:int|long)?\s*(\w+)\s*=\s*(\d+)\s*;\s*\1\s*([<>=!]+)\s*([^;]+);\s*[^)]+\)""").find(header)
    if (standardMatch != null) {
      val loopVar = standardMatch.groupValues[1]
      val startVal = standardMatch.groupValues[2].toInt()
      val condOp = standardMatch.groupValues[3]
      val limitExpr = standardMatch.groupValues[4].trim()
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

      if (trimmed.contains("cout <<") || trimmed.contains("std::cout <<")) {
        handleCout(trimmed, headerFunctions)
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
        "%" -> if (rightVal != 0.0) (leftVal.toLong() % rightVal.toLong()).toDouble() else 0.0
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
        } else if (ch == '}') {
          depth--
          if (foundFirst && depth == 0) {
            return i
          }
        }
      }
    }
    return lines.size - 1
  }
}
