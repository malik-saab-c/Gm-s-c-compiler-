package com.example.engine

import android.content.Context
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.CppStandard
import com.example.model.OutputType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Native GCC / Clang execution engine based on the open-source
 * tranleduy2000/c_cpp_compiler (CPP N-IDE) architecture for Android.
 *
 * Implements:
 * 1. GccArgumentBuilder for standard compilation flags (-O2, -std=c++20, -lm, -llog)
 * 2. CCTools / Termux / Android POSIX environment setup (PATH, LD_LIBRARY_PATH, TMPDIR)
 * 3. Subprocess execution with unbuffered stream reading and interactive stdin
 */
class NativeProcessCompiler(private val context: Context) {

  private val workspaceDir: File
    get() = File(context.filesDir, "workspace").apply { if (!exists()) mkdirs() }

  private val binDir: File
    get() = File(context.filesDir, "bin").apply { if (!exists()) mkdirs() }

  private val cctoolsDir: File
    get() = context.getDir("cctools", Context.MODE_PRIVATE)

  /**
   * Searches for available native C++ compiler CLI binaries on Android OS,
   * checking CCTools (c_cpp_compiler), Termux, /system/bin, and app-native directories.
   */
  fun findNativeCompilerBinary(): String? {
    val potentialPaths = arrayOf(
      File(cctoolsDir, "bin/g++").absolutePath,
      File(cctoolsDir, "bin/gcc").absolutePath,
      File(cctoolsDir, "bin/clang++").absolutePath,
      File(context.filesDir, "cctools/bin/g++").absolutePath,
      File(context.filesDir, "cctools/bin/gcc").absolutePath,
      "/data/data/com.termux/files/usr/bin/clang++",
      "/data/data/com.termux/files/usr/bin/g++",
      "/system/bin/clang++",
      "/system/bin/g++",
      "/system/xbin/clang++",
      "/system/xbin/g++",
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
   * Builds the GCC/G++ compilation arguments following tranleduy2000/c_cpp_compiler's GccArgumentBuilder
   */
  fun buildGccArguments(
    compilerPath: String,
    sourceFileName: String,
    outputBinaryName: String,
    config: CompilerConfig
  ): List<String> {
    val args = mutableListOf<String>()
    args.add(compilerPath)

    // Optimization flags
    args.add(config.optimizationLevel)

    // C++ Standard flags
    val stdFlag = when (config.standard) {
      CppStandard.CPP11 -> "-std=c++11"
      CppStandard.CPP14 -> "-std=c++14"
      CppStandard.CPP17 -> "-std=c++17"
      CppStandard.CPP20 -> "-std=c++20"
      CppStandard.CPP23 -> "-std=c++2b"
    }
    args.add(stdFlag)

    // Warnings
    config.warnings.split(" ").filter { it.isNotBlank() }.forEach {
      args.add(it)
    }

    // System and local includes (CCTools include structure from c_cpp_compiler)
    val cctoolsInclude = File(cctoolsDir, "include")
    if (cctoolsInclude.exists()) {
      args.add("-isystem")
      args.add(cctoolsInclude.absolutePath)
    }
    args.add("-I.")

    // Source file
    args.add(sourceFileName)

    // Output binary
    args.add("-o")
    args.add(outputBinaryName)

    // Standard libraries from tranleduy2000 NativeCompileImpl: -lm, -llog
    args.add("-lm")
    args.add("-llog")

    return args
  }

  /**
   * Compiles and executes C++ source using native ProcessBuilder following c_cpp_compiler
   */
  suspend fun compileAndExecuteNative(
    mainCode: String,
    projectFiles: List<CppFile>,
    config: CompilerConfig,
    inputProvider: suspend () -> String,
    onOutput: (TerminalLine) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      // 1. Write source files into workspace
      val mainCppFile = File(workspaceDir, "main.cpp")
      mainCppFile.writeText(mainCode)

      projectFiles.forEach { file ->
        if (file.name != "main.cpp") {
          File(workspaceDir, file.name).writeText(file.content)
        }
      }

      onOutput(TerminalLine("Preparing workspace at ${workspaceDir.absolutePath}...", OutputType.INFO))

      // 2. Discover Native Compiler Toolchain
      val compilerPath = findNativeCompilerBinary()

      if (compilerPath != null) {
        onOutput(TerminalLine("Found Native Toolchain: $compilerPath", OutputType.INFO))

        val outputExecutable = File(workspaceDir, "main_exec")
        if (outputExecutable.exists()) {
          outputExecutable.delete()
        }

        // 3. Build arguments using GccArgumentBuilder logic
        val compileArgs = buildGccArguments(
          compilerPath = compilerPath,
          sourceFileName = "main.cpp",
          outputBinaryName = "main_exec",
          config = config
        )

        onOutput(TerminalLine("Running: ${compileArgs.joinToString(" ")}", OutputType.INFO))

        // Build Environment variables following c_cpp_compiler Environment.buildDefaultEnv
        val pathEnv = listOfNotNull(
          File(cctoolsDir, "bin").takeIf { it.exists() }?.absolutePath,
          File(cctoolsDir, "sbin").takeIf { it.exists() }?.absolutePath,
          binDir.absolutePath,
          System.getenv("PATH")
        ).joinToString(File.pathSeparator)

        val ldPathEnv = listOfNotNull(
          File(cctoolsDir, "lib").takeIf { it.exists() }?.absolutePath,
          context.applicationInfo.nativeLibraryDir,
          System.getenv("LD_LIBRARY_PATH")
        ).joinToString(File.pathSeparator)

        val compilePb = ProcessBuilder(compileArgs).apply {
          directory(workspaceDir)
          environment()["PATH"] = pathEnv
          environment()["LD_LIBRARY_PATH"] = ldPathEnv
          environment()["TMPDIR"] = workspaceDir.absolutePath
          environment()["HOME"] = context.filesDir.absolutePath
        }

        val compileProcess = compilePb.start()

        // Stream compiler stderr/stdout in real-time
        val compileErrReader = BufferedReader(InputStreamReader(compileProcess.errorStream, Charsets.UTF_8))
        var compileErrLine: String?
        while (compileErrReader.readLine().also { compileErrLine = it } != null) {
          onOutput(TerminalLine(compileErrLine.orEmpty(), OutputType.ERROR))
        }

        val compileOutReader = BufferedReader(InputStreamReader(compileProcess.inputStream, Charsets.UTF_8))
        var compileOutLine: String?
        while (compileOutReader.readLine().also { compileOutLine = it } != null) {
          onOutput(TerminalLine(compileOutLine.orEmpty(), OutputType.INFO))
        }

        val compileExitCode = compileProcess.waitFor()

        if (compileExitCode != 0 || !outputExecutable.exists()) {
          onOutput(TerminalLine("\nCompilation failed with exit code $compileExitCode.", OutputType.ERROR))
          return@withContext true
        }

        onOutput(TerminalLine("Compilation successful! Setting executable permissions...", OutputType.SUCCESS))

        // Set executable permissions (chmod 755 as in Shell.java)
        outputExecutable.setExecutable(true, false)
        try {
          Runtime.getRuntime().exec(arrayOf("chmod", "755", outputExecutable.absolutePath)).waitFor()
        } catch (_: Exception) {}

        // 4. Execution & Real-time Stream Capture
        onOutput(TerminalLine("Executing ./main_exec...\n----------------------------------------", OutputType.SUCCESS))

        val execPb = ProcessBuilder(outputExecutable.absolutePath).apply {
          directory(workspaceDir)
          environment()["LD_LIBRARY_PATH"] = ldPathEnv
          environment()["TMPDIR"] = workspaceDir.absolutePath
          environment()["HOME"] = context.filesDir.absolutePath
        }

        val execProcess = execPb.start()

        val stdoutReader = BufferedReader(InputStreamReader(execProcess.inputStream, Charsets.UTF_8), 1024)
        val stderrReader = BufferedReader(InputStreamReader(execProcess.errorStream, Charsets.UTF_8), 1024)
        val processWriter = BufferedWriter(OutputStreamWriter(execProcess.outputStream, Charsets.UTF_8))

        // Interactive cin / stdin handling
        if (mainCode.contains("cin") || mainCode.contains("scanf") || mainCode.contains("getchar") || mainCode.contains("getline")) {
          val userInput = inputProvider()
          processWriter.write(userInput)
          processWriter.newLine()
          processWriter.flush()
        }

        // Unbuffered stream capture line by line
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
        onOutput(TerminalLine("[Toolchain Architecture] Initialized Workspace: ${workspaceDir.absolutePath}", OutputType.INFO))
        return@withContext false
      }
    } catch (e: Exception) {
      onOutput(TerminalLine("Native Process Engine notice: ${e.message}", OutputType.INFO))
      return@withContext false
    }
  }
}
