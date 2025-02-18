/*
 * Copyright 2019 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static java.util.Arrays.asList;

import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.BugCheckerRefactoringTestHelper.TestMode;
import com.google.errorprone.CompilationTestHelper;
import com.google.errorprone.util.RuntimeVersion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link UnusedNestedClass} bug pattern. */
@RunWith(JUnit4.class)
public class UnusedNestedClassTest {
  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(UnusedNestedClass.class, getClass());

  @Test
  public void positive() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class A {",
            "  // BUG: Diagnostic contains:",
            "  private class B {}",
            "}")
        .doTest();
  }

  @Test
  public void nonPrivateNestedClass_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java", //
            "class A {",
            "  class B {}",
            "}")
        .doTest();
  }

  @Test
  public void usedWithinSelf_warning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  // BUG: Diagnostic contains:",
            "  private static class B {",
            "    private static final B b = new B();",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void usedOutsideSelf_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  private static final B b = new B();",
            "  private static class B {}",
            "}")
        .doTest();
  }

  @Test
  public void usedOutsideSelf_oddQualification_noWarning() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  public static final Object b = new A.B();",
            "  private static class B {}",
            "}")
        .doTest();
  }

  @Test
  public void suppression() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "class A {",
            "  @SuppressWarnings(\"unused\")",
            "  private class B {}",
            "}")
        .doTest();
  }

  @Test
  public void usedReflectively_suppressed() {
    compilationHelper
        .addSourceLines(
            "Test.java",
            "import com.google.errorprone.annotations.Keep;",
            "class A {",
            "  @Keep",
            "  private class B {}",
            "}")
        .doTest();
  }

  @Test
  public void refactoring() {
    BugCheckerRefactoringTestHelper.newInstance(UnusedNestedClass.class, getClass())
        .addInputLines(
            "Test.java", //
            "class A {",
            "  /** Foo. */",
            "  private class B {}",
            "}")
        .addOutputLines(
            "Test.java", //
            "class A {",
            "}")
        .doTest(TestMode.TEXT_MATCH);
  }

  @Test
  public void moduleInfo() {
    if (!RuntimeVersion.isAtLeast9()) {
      return;
    }
    compilationHelper
        .setArgs(asList("-source", "9", "-target", "9"))
        .addSourceLines(
            "module-info.java", //
            "module foo {",
            "  requires java.base;",
            "}")
        .doTest();
  }
}
