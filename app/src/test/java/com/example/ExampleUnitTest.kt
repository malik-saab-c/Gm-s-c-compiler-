package com.example

import com.example.data.DefaultProjects
import com.example.engine.CppDebuggerEngine
import com.example.engine.CppEngine
import com.example.model.CompilerConfig
import com.example.model.CppFile
import com.example.model.OutputType
import com.example.model.TerminalLine
import com.example.ui.editor.CppSyntaxHighlighter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testCppEngineExecution() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    val code = """
      #include <iostream>
      #include <vector>
      #include <algorithm>

      int main() {
          std::vector<int> nums = {5, 2, 8, 1};
          std::sort(nums.begin(), nums.end());
          std::cout << "Sorted: ";
          for (int x : nums) {
              std::cout << x << " ";
          }
          std::cout << std::endl;
          return 0;
      }
    """.trimIndent()

    val file = CppFile("test_main", "main.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(file),
      config = CompilerConfig(),
      inputProvider = { "42" },
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue(allText.contains("Build succeeded"))
    assertTrue(allText.contains("1 2 5 8"))
    assertTrue(allText.contains("exit code 0"))
  }

  @Test
  fun testCppHeaderResolution() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    val header = DefaultProjects.defaultHeaderFile
    val code = """
      #include <iostream>
      #include "math_utils.h"

      int main() {
          int n = 5;
          std::cout << "Fact: " << factorial(n) << std::endl;
          return 0;
      }
    """.trimIndent()

    val mainFile = CppFile("main", "main.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(mainFile, header),
      config = CompilerConfig(),
      inputProvider = { "" },
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue(allText.contains("Fact: 120"))
  }

  @Test
  fun testDebuggerSession() {
    val debugger = CppDebuggerEngine()
    val code = """
      #include <iostream>
      int main() {
          int a = 10;
          int b = 20;
          int c = a + b;
          return 0;
      }
    """.trimIndent()

    val file = CppFile("main", "main.cpp", code, breakpoints = setOf(4))
    val session = debugger.startSession(file)
    assertTrue(session.isActive)

    val stepped = debugger.stepOver(file)
    assertNotNull(stepped)
  }

  @Test
  fun testSyntaxHighlighter() {
    val code = "#include <iostream>\nint main() { return 0; }"
    val highlighted = CppSyntaxHighlighter.highlight(code)
    assertTrue(highlighted.spanStyles.isNotEmpty())
  }
}
