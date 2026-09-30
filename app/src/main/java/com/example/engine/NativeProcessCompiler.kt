package com.example.engine

import android.content.Context
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.OutputType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class NativeProcessCompiler(private val context: Context) {

  private val workspaceDir: File
    get() = File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() }

  private val binDir: File
    get() = File(context.filesDir, "bin").apply { if (!exists()) mkdirs() }

  /**
   * Searches for available native C++ compiler CLI binaries on Android OS
   */
  fun findNativeCompilerBinary(): String? {
    val potentialPaths = arrayOf(
      "/system/bin/clang++",
      "/system/bin/g++",
      "/system/xbin/clang++",
      "/data/data/com.termux/files/usr/bin/clang++",
      "/data/data/com.termux/files/usr/bin/g++",
      File(binDir, "clang++").absolutePath,
      File(binDir, "g++").absolutePath,
      File(context.applicationInfo.nativeLibraryDir, "libclang.so").absolutePath
    )

    for (path in potentialPaths) {
      val file = File(path)
      if (file.exists() && file.canExecute()) {
        return path
      }
    }
    return null
  }

  /**
   * Compiles and executes C++ source using native ProcessBuilder / Runtime.getRuntime().exec()
   */
  suspend fun compileAndExecuteNative(
    mainCode: String,
    projectFiles: List<CppFile>,
    config: CompilerConfig,
    inputProvider: suspend () -> String,
    onOutput: (TerminalLine) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      // 1. File & Directory Management: Write source files to context.filesDir
      val mainCppFile = File(workspaceDir, "main.cpp")
      mainCppFile.writeText(mainCode)

      projectFiles.forEach { file ->
        if (file.name != "main.cpp") {
          File(workspaceDir, file.name).writeText(file.content)
        }
      }

      onOutput(TerminalLine("Preparing workspace at ${workspaceDir.absolutePath}...", OutputType.INFO))

      // 2. Compiler Search
      val compilerPath = findNativeCompilerBinary()

      if (compilerPath != null) {
        onOutput(TerminalLine("Found Native Compiler: $compilerPath", OutputType.INFO))
        onOutput(TerminalLine("Executing: $compilerPath -O2 main.cpp -o main_exec", OutputType.INFO))

        val outputExecutable = File(workspaceDir, "main_exec")
        if (outputExecutable.exists()) {
          outputExecutable.delete()
        }

        // 3. Compilation Process via ProcessBuilder
        val compilePb = ProcessBuilder(
          compilerPath,
          "-O2",
          if (config.standard.displayName.contains("20")) "-std=c++20" else "-std=c++17",
          "main.cpp",
          "-o",
          "main_exec"
        ).apply {
          directory(workspaceDir)
          environment()["PATH"] = "${binDir.absolutePath}:${System.getenv("PATH") ?: ""}"
          environment()["LD_LIBRARY_PATH"] = "${context.applicationInfo.nativeLibraryDir}:${System.getenv("LD_LIBRARY_PATH") ?: ""}"
        }

        val compileProcess = compilePb.start()

        // Read compilation stderr
        val compileErrReader = BufferedReader(InputStreamReader(compileProcess.errorStream, Charsets.UTF_8))
        var compileErrLine: String?
        var hasCompileErrors = false

        while (compileErrReader.readLine().also { compileErrLine = it } != null) {
          hasCompileErrors = true
          onOutput(TerminalLine(compileErrLine.orEmpty(), OutputType.ERROR))
        }

        val compileExitCode = compileProcess.waitFor()

        if (compileExitCode != 0 || !outputExecutable.exists()) {
          onOutput(TerminalLine("\nCompilation failed with exit code $compileExitCode.", OutputType.ERROR))
          return@withContext true
        }

        onOutput(TerminalLine("Compilation successful! Setting binary permissions...", OutputType.SUCCESS))

        // Set executable permissions
        outputExecutable.setExecutable(true, false)
        try {
          Runtime.getRuntime().exec("chmod 755 ${outputExecutable.absolutePath}").waitFor()
        } catch (_: Exception) {}

        // 4. Execution & Real-time Stream Capture
        onOutput(TerminalLine("Executing ./main_exec...\n----------------------------------------", OutputType.SUCCESS))

        val execPb = ProcessBuilder(outputExecutable.absolutePath).apply {
          directory(workspaceDir)
          environment()["LD_LIBRARY_PATH"] = "${context.applicationInfo.nativeLibraryDir}:${System.getenv("LD_LIBRARY_PATH") ?: ""}"
        }

        val execProcess = execPb.start()

        val stdoutReader = BufferedReader(InputStreamReader(execProcess.inputStream, Charsets.UTF_8), 1024)
        val stderrReader = BufferedReader(InputStreamReader(execProcess.errorStream, Charsets.UTF_8), 1024)
        val processWriter = BufferedWriter(OutputStreamWriter(execProcess.outputStream, Charsets.UTF_8))

        // Handle interactive cin input if code contains cin or input prompts
        if (mainCode.contains("cin") || mainCode.contains("scanf") || mainCode.contains("getchar")) {
          val userInput = inputProvider()
          processWriter.write(userInput)
          processWriter.newLine()
          processWriter.flush()
        }

        // Unbuffered stream capture
        var stdoutLine: String?
        while (stdoutReader.readLine().also { stdoutLine = it } != null) {
          onOutput(TerminalLine(stdoutLine.orEmpty(), OutputType.STDOUT))
        }

        var stderrLine: String?
        while (stderrReader.readLine().also { stderrLine = it } != null) {
          onOutput(TerminalLine(stderrLine.orEmpty(), OutputType.STDERR))
        }

        val exitCode = execProcess.waitFor()
        onOutput(TerminalLine("\n----------------------------------------", OutputType.INFO))
        if (exitCode == 0) {
          onOutput(TerminalLine("Process finished with exit code 0", OutputType.SUCCESS))
        } else {
          onOutput(TerminalLine("Process finished with exit code $exitCode", OutputType.ERROR))
        }

        return@withContext true
      } else {
        onOutput(TerminalLine("[Offline Process System] Initialized Workspace: ${workspaceDir.absolutePath}", OutputType.INFO))
        return@withContext false
      }
    } catch (e: Exception) {
      onOutput(TerminalLine("Native Process Engine notice: ${e.message}", OutputType.INFO))
      return@withContext false
    }
  }
}
