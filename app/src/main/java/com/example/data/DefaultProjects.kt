package com.example.data

import com.example.model.CppFile
import com.example.model.Snippet

object DefaultProjects {

  val defaultMainFile = CppFile(
    id = "main_cpp",
    name = "main.cpp",
    content = """// GM'S c++ IDE - Developed by Sir Ghulam Mustafa
// Standard: C++20 | Native Mobile Compiler & Runner
#include <iostream>
#include <vector>
#include <string>
#include <algorithm>

int main() {
    std::cout << "========================================" << std::endl;
    std::cout << " Welcome to GM'S c++ Professional IDE! " << std::endl;
    std::cout << " Developer: Sir Ghulam Mustafa         " << std::endl;
    std::cout << "========================================" << std::endl;

    // Fast STL Vector demonstration
    std::vector<int> numbers = {64, 34, 25, 12, 22, 11, 90};
    
    std::cout << "\n[1] Original List: ";
    for (int num : numbers) {
        std::cout << num << " ";
    }
    std::cout << std::endl;

    // Sorting algorithm
    std::sort(numbers.begin(), numbers.end());

    std::cout << "[2] Sorted (std::sort): ";
    for (int num : numbers) {
        std::cout << num << " ";
    }
    std::cout << std::endl;

    std::cout << "\nHappy Coding in C++ on Mobile!" << std::endl;
    return 0;
}
""".trimIndent(),
    isMain = true
  )

  val defaultHeaderFile = CppFile(
    id = "math_utils_h",
    name = "math_utils.h",
    content = """// math_utils.h - Custom Header File
#ifndef MATH_UTILS_H
#define MATH_UTILS_H

#include <iostream>

inline long long factorial(int n) {
    if (n <= 1) return 1;
    return n * factorial(n - 1);
}

inline bool isPrime(int n) {
    if (n <= 1) return false;
    for (int i = 2; i * i <= n; ++i) {
        if (n % i == 0) return false;
    }
    return true;
}

#endif // MATH_UTILS_H
""".trimIndent(),
    isMain = false
  )

  val snippets = listOf(
    Snippet(
      id = "snip_hello",
      title = "Hello World",
      category = "Basics",
      description = "Standard greeting output using iostream",
      fileName = "hello.cpp",
      code = """#include <iostream>

int main() {
    std::cout << "Hello, World from GM'S c++!" << std::endl;
    std::cout << "Engineered by Sir Ghulam Mustafa" << std::endl;
    return 0;
}
"""
    ),
    Snippet(
      id = "snip_interactive_cin",
      title = "Interactive Input (std::cin)",
      category = "I/O",
      description = "Real-time user input prompt with calculations",
      fileName = "input_demo.cpp",
      code = """#include <iostream>
#include <string>

int main() {
    std::string name;
    int age;

    std::cout << "Enter your name: ";
    std::cin >> name;

    std::cout << "Enter your age: ";
    std::cin >> age;

    std::cout << "\nGreetings, " << name << "!" << std::endl;
    std::cout << "In 5 years, you will be " << (age + 5) << " years old." << std::endl;
    return 0;
}
"""
    ),
    Snippet(
      id = "snip_stl_vector",
      title = "STL Vector & Algorithms",
      category = "Data Structures",
      description = "Dynamic array, sorting, and transformations",
      fileName = "stl_vector.cpp",
      code = """#include <iostream>
#include <vector>
#include <algorithm>
#include <numeric>

int main() {
    std::vector<int> scores = {95, 72, 88, 60, 100, 84};

    std::cout << "Scores count: " << scores.size() << std::endl;
    
    // Sort ascending
    std::sort(scores.begin(), scores.end());
    std::cout << "Min Score: " << scores.front() << std::endl;
    std::cout << "Max Score: " << scores.back() << std::endl;

    double sum = 0;
    for (int s : scores) sum += s;
    std::cout << "Average Score: " << (sum / scores.size()) << std::endl;

    return 0;
}
"""
    ),
    Snippet(
      id = "snip_threads",
      title = "Multithreading (std::thread)",
      category = "Concurrency",
      description = "Concurrent worker threads with synchronization",
      fileName = "threads_demo.cpp",
      code = """#include <iostream>
#include <thread>
#include <chrono>

void workerTask(int id) {
    std::cout << "[Worker " << id << "] Task started on thread ID: " << id << std::endl;
    std::cout << "[Worker " << id << "] Performing computational work..." << std::endl;
    std::cout << "[Worker " << id << "] Completed successfully!" << std::endl;
}

int main() {
    std::cout << "Main thread starting workers..." << std::endl;

    std::thread t1(workerTask, 1);
    std::thread t2(workerTask, 2);

    t1.join();
    t2.join();

    std::cout << "All concurrent threads joined back to main!" << std::endl;
    return 0;
}
"""
    ),
    Snippet(
      id = "snip_oop",
      title = "OOP: Classes & Methods",
      category = "Object Oriented",
      description = "Class definitions, encapsulation, and constructors",
      fileName = "oop_student.cpp",
      code = """#include <iostream>
#include <string>

class Student {
private:
    std::string name;
    int rollNumber;
    double gpa;

public:
    Student(std::string n, int r, double g) : name(n), rollNumber(r), gpa(g) {}

    void displayInfo() const {
        std::cout << "Student: " << name << " | Roll #: " << rollNumber << " | GPA: " << gpa << std::endl;
    }

    bool isHonors() const {
        return gpa >= 3.5;
    }
};

int main() {
    Student s1("Ali", 101, 3.8);
    Student s2("Zain", 102, 3.2);

    s1.displayInfo();
    std::cout << "Honors status: " << (s1.isHonors() ? "YES" : "NO") << std::endl;

    s2.displayInfo();
    std::cout << "Honors status: " << (s2.isHonors() ? "YES" : "NO") << std::endl;

    return 0;
}
"""
    ),
    Snippet(
      id = "snip_header_include",
      title = "Multi-file (#include \"math_utils.h\")",
      category = "Projects",
      description = "Calling functions from custom local header file",
      fileName = "main.cpp",
      code = """#include <iostream>
#include "math_utils.h"

int main() {
    std::cout << "=== Multi-file Project Header Call ===" << std::endl;
    
    int number = 7;
    std::cout << "Factorial of " << number << " = " << factorial(number) << std::endl;
    std::cout << "Is " << number << " prime? " << (isPrime(number) ? "YES" : "NO") << std::endl;

    return 0;
}
"""
    ),
    Snippet(
      id = "snip_recursion_fib",
      title = "Recursion & Dynamic Programming",
      category = "Algorithms",
      description = "Fibonacci calculation with memoization",
      fileName = "fibonacci.cpp",
      code = """#include <iostream>
#include <vector>

long long fibonacci(int n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;
    long long a = 0, b = 1;
    for (int i = 2; i <= n; ++i) {
        long long c = a + b;
        a = b;
        b = c;
    }
    return b;
}

int main() {
    std::cout << "First 12 Fibonacci Numbers:" << std::endl;
    for (int i = 0; i <= 12; ++i) {
        std::cout << "F(" << i << ") = " << fibonacci(i) << std::endl;
    }
    return 0;
}
"""
    ),
    Snippet(
      id = "snip_competitive_io",
      title = "Competitive Programming Fast I/O",
      category = "Competitive",
      description = "Template with fast cin/cout and vector manipulation",
      fileName = "fast_io.cpp",
      code = """#include <iostream>
#include <vector>
#include <cmath>

int main() {
    // Fast I/O
    std::ios_base::sync_with_stdio(false);
    std::cin.tie(NULL);

    std::cout << "Competitive Programming Fast Template" << std::endl;
    int testCases = 3;
    for (int t = 1; t <= testCases; ++t) {
        long long n = t * 1000;
        long long sum = (n * (n + 1)) / 2;
        std::cout << "Case #" << t << ": Sum(1.." << n << ") = " << sum << std::endl;
    }
    return 0;
}
"""
    )
  )
}
