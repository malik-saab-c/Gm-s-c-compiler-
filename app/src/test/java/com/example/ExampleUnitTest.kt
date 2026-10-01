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
  fun testUserCustomProgramEven() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    val code = """
      #include <iostream>
      using namespace std;

      int main() {
          int num = 4;
          if (num % 2 == 0) {
              cout << "number is even!";
          } else {
              cout << "number is odd!";
          }
          return 0;
      }
    """.trimIndent()

    val file = CppFile("main.cpp", "main.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(file),
      config = CompilerConfig(useOnlineCompiler = false),
      inputProvider = { "4" },
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue("Should output 'number is even!'", allText.contains("number is even!"))
    assertTrue("Should complete with exit code 0", allText.contains("exit code 0"))
  }

  @Test
  fun testUserCustomProgramOdd() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    val code = """
      #include <iostream>
      using namespace std;

      int main() {
          int num;
          cout << "enter any number: ";
          cin >> num;

          if (num % 2 == 0) {
              cout << "number is even!";
          } else {
              cout << "number is odd!";
          }

          return 0;
      }
    """.trimIndent()

    val file = CppFile("main.cpp", "main.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(file),
      config = CompilerConfig(useOnlineCompiler = false),
      inputProvider = { "7" }, // Odd number
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue("Should print 'enter any number: '", allText.contains("enter any number:"))
    assertTrue("Should output 'number is odd!'", allText.contains("number is odd!"))
    assertTrue("Should complete with exit code 0", allText.contains("exit code 0"))
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

    val allOutput = output.joinToString("") { it.text }
    assertTrue(allOutput.contains("Target Toolchain"))
    assertTrue(allOutput.contains("1 2 5 8"))
    assertTrue(allOutput.contains("exit code 0"))
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
  fun testUserExactCodeSnippetNoSpacesCin() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    // The exact snippet provided by the user in their report
    val code = """
      #include <iostream>
      using namespace std;

      int main() {
          int a;
          cout << "H" << endl;
          cin>>a;
          return 0;
      }
    """.trimIndent()

    val file = CppFile("jfg.cpp", "jfg.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(file),
      config = CompilerConfig(useOnlineCompiler = false),
      inputProvider = { "99" },
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue("Should print 'H'", allText.contains("H"))
    assertTrue("Should echo stdin input '99'", allText.contains("99"))
    assertTrue("Should finish with exit code 0", allText.contains("exit code 0"))
  }

  @Test
  fun testUserPolymorphicClassExecution() = runBlocking {
    val engine = CppEngine()
    val output = mutableListOf<TerminalLine>()

    val code = """
      #include <iostream>
      #include <vector>
      #include <memory>
      #include <string>
      #include <algorithm>
      #include <numeric>

      class Device {
      protected:
          std::string name;
      public:
          Device(const std::string& n) : name(n) {}
          virtual ~Device() = default;
          virtual void processData() = 0;
      };

      class Sensor : public Device {
      private:
          std::vector<int> readings;
      public:
          Sensor(const std::string& n, const std::vector<int>& r) : Device(n), readings(r) {}
          void processData() override {
              std::cout << ">>> Processing Device: " << name << "\n";
              std::vector<int> evens;
              std::copy_if(readings.begin(), readings.end(), std::back_inserter(evens), [](int x) { return x % 2 == 0; });
              std::sort(evens.begin(), evens.end());
              int sum = std::accumulate(evens.begin(), evens.end(), 0);
              std::cout << "Filtered Even Values (Sorted): ";
              for (int v : evens) {
                  std::cout << v << " ";
              }
              std::cout << "\nTotal Sum of Evens: " << sum << "\n\n";
          }
      };

      class Processor : public Device {
      private:
          double factor;
      public:
          Processor(const std::string& n, double f) : Device(n), factor(f) {}
          void processData() override {
              std::cout << ">>> Processing Device: " << name << "\n";
              double result = 100.0 * factor;
              std::cout << "Calculated Output Factor: " << result << "\n\n";
          }
      };

      int main() {
          std::cout << "==========================================" << std::endl;
          std::cout << "       COMPILER INTEGRATION TEST          " << std::endl;
          std::cout << "==========================================\n" << std::endl;

          std::vector<std::unique_ptr<Device>> devices;
          devices.push_back(std::make_unique<Sensor>("Temperature Sensor A", std::vector<int>{45, 12, 88, 32, 9, 74, 21}));
          devices.push_back(std::make_unique<Processor>("Core Engine B", 1.85));
          devices.push_back(std::make_unique<Sensor>("Pressure Sensor C", std::vector<int>{100, 23, 64, 12, 5}));

          for (const auto& dev : devices) {
              dev->processData();
          }

          std::cout << "STATUS: C++20 Compiler & STL Execution Successful!" << std::endl;
          return 0;
      }
    """.trimIndent()

    val file = CppFile("mmm.cpp", "mmm.cpp", code, isMain = true)
    engine.execute(
      code = code,
      projectFiles = listOf(file),
      config = CompilerConfig(useOnlineCompiler = false),
      inputProvider = { "" },
      onOutput = { output.add(it) }
    )

    val allText = output.joinToString("\n") { it.text }
    assertTrue("Should contain Temperature Sensor A", allText.contains("Temperature Sensor A"))
    assertTrue("Should contain Core Engine B", allText.contains("Core Engine B"))
    assertTrue("Should contain Pressure Sensor C", allText.contains("Pressure Sensor C"))
    assertTrue("Should calculate sum 206", allText.contains("206"))
    assertTrue("Should calculate factor 185", allText.contains("185"))
  }

  @Test
  fun testSyntaxHighlighter() {
    val code = "#include <iostream>\nint main() { return 0; }"
    val highlighted = CppSyntaxHighlighter.highlight(code)
    assertTrue(highlighted.spanStyles.isNotEmpty())
  }
}
