package com.example.engine

import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.CppStandard
import com.example.model.OutputType
import com.example.model.TerminalLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class RealCompilerClient {
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  suspend fun compileAndRun(
    code: String,
    stdin: String,
    standard: CppStandard,
    onOutput: (TerminalLine) -> Unit
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      onOutput(TerminalLine("Connecting to GNU GCC Toolchain (x86_64 Linux)...", OutputType.INFO))

      val compilerName = when (standard) {
        CppStandard.CPP11 -> "gcc-head"
        CppStandard.CPP14 -> "gcc-head"
        CppStandard.CPP17 -> "gcc-head"
        CppStandard.CPP20 -> "gcc-head"
        CppStandard.CPP23 -> "gcc-head"
      }

      val options = when (standard) {
        CppStandard.CPP11 -> "c++11,warning,optimize"
        CppStandard.CPP14 -> "c++14,warning,optimize"
        CppStandard.CPP17 -> "c++17,warning,optimize"
        CppStandard.CPP20 -> "c++20,warning,optimize"
        CppStandard.CPP23 -> "c++2b,warning,optimize"
      }

      val payload = JSONObject().apply {
        put("compiler", compilerName)
        put("code", code)
        put("options", options)
        put("stdin", stdin)
      }

      val body = payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder()
        .url("https://wandbox.org/api/compile.json")
        .post(body)
        .build()

      val response = client.newCall(request).execute()
      if (!response.isSuccessful) {
        onOutput(TerminalLine("Cloud server returned HTTP ${response.code}. Switching to local runner...", OutputType.STDERR))
        return@withContext false
      }

      val respStr = response.body?.string().orEmpty()
      if (respStr.isBlank()) {
        onOutput(TerminalLine("Empty response from compiler service. Switching to local runner...", OutputType.STDERR))
        return@withContext false
      }

      val json = JSONObject(respStr)

      // 1. Compiler Output (Errors / Warnings)
      val compilerOutput = json.optString("compiler_output", "").trim()
      val compilerError = json.optString("compiler_error", "").trim()
      if (compilerOutput.isNotEmpty()) {
        onOutput(TerminalLine(compilerOutput, OutputType.INFO))
      }
      if (compilerError.isNotEmpty()) {
        onOutput(TerminalLine(compilerError, OutputType.ERROR))
      }

      // 2. Program Output (stdout)
      val programOutput = json.optString("program_output", "")
      if (programOutput.isNotEmpty()) {
        onOutput(TerminalLine(programOutput, OutputType.STDOUT))
      }

      // 3. Program Error (stderr)
      val programError = json.optString("program_error", "")
      if (programError.isNotEmpty()) {
        onOutput(TerminalLine(programError, OutputType.STDERR))
      }

      // 4. Return exit status
      val status = json.optString("status", "0")
      if (status == "0") {
        onOutput(TerminalLine("\n----------------------------------------", OutputType.INFO))
        onOutput(TerminalLine("Process finished with exit code 0", OutputType.SUCCESS))
      } else {
        onOutput(TerminalLine("\n----------------------------------------", OutputType.INFO))
        onOutput(TerminalLine("Process finished with exit code $status", OutputType.ERROR))
      }

      true
    } catch (e: IOException) {
      onOutput(TerminalLine("Network unavailable (${e.message ?: "offline"}). Falling back to local runner...", OutputType.INFO))
      false
    } catch (e: Exception) {
      onOutput(TerminalLine("Cloud compiler notice: ${e.message}. Using local runner...", OutputType.INFO))
      false
    }
  }
}
