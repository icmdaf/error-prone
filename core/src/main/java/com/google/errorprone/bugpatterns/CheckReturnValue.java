/*
 * Copyright 2012 The Error Prone Authors.
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

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.util.ASTHelpers.hasDirectAnnotationWithSimpleName;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import java.util.stream.Stream;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;

/** @author eaftan@google.com (Eddie Aftandilian) */
@BugPattern(
    name = "CheckReturnValue",
    altNames = {"ResultOfMethodCallIgnored", "ReturnValueIgnored"},
    summary = "Ignored return value of method that is annotated with @CheckReturnValue",
    severity = ERROR)
public class CheckReturnValue extends AbstractReturnValueIgnored
    implements MethodTreeMatcher, ClassTreeMatcher {

  private static final String CHECK_RETURN_VALUE = "CheckReturnValue";
  private static final String CAN_IGNORE_RETURN_VALUE = "CanIgnoreReturnValue";

  private static Stream<Boolean> shouldCheckReturnValue(Symbol sym) {
    if (hasDirectAnnotationWithSimpleName(sym, CAN_IGNORE_RETURN_VALUE)) {
      return Stream.of(false);
    }
    if (hasDirectAnnotationWithSimpleName(sym, CHECK_RETURN_VALUE)) {
      return Stream.of(true);
    }
    return Stream.empty();
  }

  /**
   * Return a matcher for method invocations in which the method being called has the
   * {@code @CheckReturnValue} annotation.
   */
  @Override
  public Matcher<ExpressionTree> specializedMatcher() {
    return (tree, state) -> {
      Symbol sym = ASTHelpers.getSymbol(tree);
      if (!(sym instanceof MethodSymbol)) {
        return false;
      }
      return ASTHelpers.enclosingElements(sym)
          .flatMap(CheckReturnValue::shouldCheckReturnValue)
          .findFirst()
          .orElse(false);
    };
  }

  private static final String BOTH_ERROR =
      "@CheckReturnValue and @CanIgnoreReturnValue cannot both be applied to the same %s";

  /**
   * Validate {@code @CheckReturnValue} and {@link CanIgnoreReturnValue} usage on methods.
   *
   * <p>The annotations should not both be applied to the same method.
   *
   * <p>The annotations should not be applied to void-returning methods. Doing so makes no sense,
   * because there is no return value to check.
   */
  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol method = ASTHelpers.getSymbol(tree);

    boolean checkReturn = hasDirectAnnotationWithSimpleName(method, CHECK_RETURN_VALUE);
    boolean canIgnore = hasDirectAnnotationWithSimpleName(method, CAN_IGNORE_RETURN_VALUE);

    if (checkReturn && canIgnore) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "method")).build();
    }

    String annotationToValidate;
    if (checkReturn) {
      annotationToValidate = CHECK_RETURN_VALUE;
    } else if (canIgnore) {
      annotationToValidate = CAN_IGNORE_RETURN_VALUE;
    } else {
      return Description.NO_MATCH;
    }
    if (method.getKind() != ElementKind.METHOD) {
      // skip constructors (which javac thinks are void-returning)
      return Description.NO_MATCH;
    }
    if (!ASTHelpers.isVoidType(method.getReturnType(), state)) {
      return Description.NO_MATCH;
    }
    String message =
        String.format("@%s may not be applied to void-returning methods", annotationToValidate);
    return buildDescription(tree).setMessage(message).build();
  }

  /**
   * Validate that at most one of {@code CheckReturnValue} and {@code CanIgnoreReturnValue} are
   * applied to a class (or interface or enum).
   */
  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    if (hasDirectAnnotationWithSimpleName(ASTHelpers.getSymbol(tree), CHECK_RETURN_VALUE)
        && hasDirectAnnotationWithSimpleName(ASTHelpers.getSymbol(tree), CAN_IGNORE_RETURN_VALUE)) {
      return buildDescription(tree).setMessage(String.format(BOTH_ERROR, "class")).build();
    }
    return Description.NO_MATCH;
  }

  @Override
  protected String getMessage(Name name) {
    return String.format(
        "Ignored return value of '%s', which is annotated with @CheckReturnValue", name);
  }
}
