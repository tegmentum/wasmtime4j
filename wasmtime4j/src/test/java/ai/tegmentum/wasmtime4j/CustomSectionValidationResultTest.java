/*
 * Copyright 2025 Tegmentum AI
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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CustomSectionValidationResult class.
 *
 * <p>CustomSectionValidationResult contains the results of validating WebAssembly custom sections,
 * including errors, warnings, and diagnostic information. This test verifies the class structure,
 * builder pattern, and nested types.
 */
@DisplayName("CustomSectionValidationResult Class Tests")
class CustomSectionValidationResultTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(CustomSectionValidationResult.class.getModifiers()),
          "CustomSectionValidationResult should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CustomSectionValidationResult.class.getModifiers()),
          "CustomSectionValidationResult should be public");
    }

    @Test
    @DisplayName("should not be abstract")
    void shouldNotBeAbstract() {
      assertTrue(
          !Modifier.isAbstract(CustomSectionValidationResult.class.getModifiers()),
          "CustomSectionValidationResult should not be abstract");
    }
  }

  // ========================================================================
  // Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Methods Tests")
  class GetterMethodsTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getErrors method")
    void shouldHaveGetErrorsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getErrors");
      assertNotNull(method, "getErrors method should exist");
      assertEquals(List.class, method.getReturnType(), "getErrors should return List");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "getWarnings should return List");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "getSummary should return String");
    }

    @Test
    @DisplayName("should have hasIssues method")
    void shouldHaveHasIssuesMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("hasIssues");
      assertNotNull(method, "hasIssues method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasIssues should return boolean");
    }

    @Test
    @DisplayName("should have getIssueCount method")
    void shouldHaveGetIssueCountMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getIssueCount");
      assertNotNull(method, "getIssueCount method should exist");
      assertEquals(int.class, method.getReturnType(), "getIssueCount should return int");
    }

    @Test
    @DisplayName("should have getAllIssues method")
    void shouldHaveGetAllIssuesMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getAllIssues");
      assertNotNull(method, "getAllIssues method should exist");
      assertEquals(List.class, method.getReturnType(), "getAllIssues should return List");
    }

    @Test
    @DisplayName("should have getIssuesForSection method")
    void shouldHaveGetIssuesForSectionMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionValidationResult.class.getMethod("getIssuesForSection", String.class);
      assertNotNull(method, "getIssuesForSection method should exist");
      assertEquals(List.class, method.getReturnType(), "getIssuesForSection should return List");
    }
  }

  // ========================================================================
  // Static Factory Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Methods Tests")
  class StaticFactoryMethodsTests {

    @Test
    @DisplayName("should have success factory method")
    void shouldHaveSuccessFactoryMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("success");
      assertNotNull(method, "success method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "success should be static");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "success should return CustomSectionValidationResult");
    }

    @Test
    @DisplayName("should have successWithWarnings factory method")
    void shouldHaveSuccessWithWarningsFactoryMethod() throws NoSuchMethodException {
      final Method method =
          CustomSectionValidationResult.class.getMethod("successWithWarnings", List.class);
      assertNotNull(method, "successWithWarnings method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "successWithWarnings should be static");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "successWithWarnings should return CustomSectionValidationResult");
    }

    @Test
    @DisplayName("should have failure factory method with errors only")
    void shouldHaveFailureFactoryMethodWithErrorsOnly() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("failure", List.class);
      assertNotNull(method, "failure method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "failure should be static");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "failure should return CustomSectionValidationResult");
    }

    @Test
    @DisplayName("should have failure factory method with errors and warnings")
    void shouldHaveFailureFactoryMethodWithErrorsAndWarnings() throws NoSuchMethodException {
      final Method method =
          CustomSectionValidationResult.class.getMethod("failure", List.class, List.class);
      assertNotNull(method, "failure method with warnings should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "failure should be static");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "failure should return CustomSectionValidationResult");
    }

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
    }
  }

  // ========================================================================
  // Builder Class Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Class Tests")
  class BuilderClassTests {

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      Class<?>[] nestedClasses = CustomSectionValidationResult.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "CustomSectionValidationResult should have Builder nested class");
    }

    @Test
    @DisplayName("Builder should be public")
    void builderShouldBePublic() throws ClassNotFoundException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
    }

    @Test
    @DisplayName("Builder should be static")
    void builderShouldBeStatic() throws ClassNotFoundException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      assertTrue(Modifier.isStatic(builderClass.getModifiers()), "Builder should be static");
    }

    @Test
    @DisplayName("Builder should be final")
    void builderShouldBeFinal() throws ClassNotFoundException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have setErrors method")
    void builderShouldHaveSetErrorsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("setErrors", List.class);
      assertNotNull(method, "setErrors method should exist");
      assertEquals(builderClass, method.getReturnType(), "setErrors should return Builder");
    }

    @Test
    @DisplayName("Builder should have setWarnings method")
    void builderShouldHaveSetWarningsMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("setWarnings", List.class);
      assertNotNull(method, "setWarnings method should exist");
      assertEquals(builderClass, method.getReturnType(), "setWarnings should return Builder");
    }

    @Test
    @DisplayName("Builder should have addError method with ValidationIssue")
    void builderShouldHaveAddErrorMethodWithValidationIssue()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      Class<?> validationIssueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = builderClass.getMethod("addError", validationIssueClass);
      assertNotNull(method, "addError method should exist");
      assertEquals(builderClass, method.getReturnType(), "addError should return Builder");
    }

    @Test
    @DisplayName("Builder should have addError method with strings")
    void builderShouldHaveAddErrorMethodWithStrings()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("addError", String.class, String.class);
      assertNotNull(method, "addError method should exist");
      assertEquals(builderClass, method.getReturnType(), "addError should return Builder");
    }

    @Test
    @DisplayName("Builder should have addWarning method with ValidationIssue")
    void builderShouldHaveAddWarningMethodWithValidationIssue()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      Class<?> validationIssueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = builderClass.getMethod("addWarning", validationIssueClass);
      assertNotNull(method, "addWarning method should exist");
      assertEquals(builderClass, method.getReturnType(), "addWarning should return Builder");
    }

    @Test
    @DisplayName("Builder should have addWarning method with strings")
    void builderShouldHaveAddWarningMethodWithStrings()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("addWarning", String.class, String.class);
      assertNotNull(method, "addWarning method should exist");
      assertEquals(builderClass, method.getReturnType(), "addWarning should return Builder");
    }

    @Test
    @DisplayName("Builder should have setSummary method")
    void builderShouldHaveSetSummaryMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("setSummary", String.class);
      assertNotNull(method, "setSummary method should exist");
      assertEquals(builderClass, method.getReturnType(), "setSummary should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws ClassNotFoundException, NoSuchMethodException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$Builder");
      final Method method = builderClass.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          CustomSectionValidationResult.class,
          method.getReturnType(),
          "build should return CustomSectionValidationResult");
    }
  }

  // ========================================================================
  // ValidationIssue Class Tests
  // ========================================================================

  @Nested
  @DisplayName("ValidationIssue Class Tests")
  class ValidationIssueClassTests {

    @Test
    @DisplayName("should have ValidationIssue nested class")
    void shouldHaveValidationIssueNestedClass() {
      Class<?>[] nestedClasses = CustomSectionValidationResult.class.getDeclaredClasses();
      boolean hasValidationIssue =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("ValidationIssue"));
      assertTrue(
          hasValidationIssue,
          "CustomSectionValidationResult should have ValidationIssue nested class");
    }

    @Test
    @DisplayName("ValidationIssue should be public")
    void validationIssueShouldBePublic() throws ClassNotFoundException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      assertTrue(Modifier.isPublic(issueClass.getModifiers()), "ValidationIssue should be public");
    }

    @Test
    @DisplayName("ValidationIssue should be static")
    void validationIssueShouldBeStatic() throws ClassNotFoundException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      assertTrue(Modifier.isStatic(issueClass.getModifiers()), "ValidationIssue should be static");
    }

    @Test
    @DisplayName("ValidationIssue should be final")
    void validationIssueShouldBeFinal() throws ClassNotFoundException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      assertTrue(Modifier.isFinal(issueClass.getModifiers()), "ValidationIssue should be final");
    }

    @Test
    @DisplayName("ValidationIssue should have error factory method")
    void validationIssueShouldHaveErrorFactoryMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("error", String.class, String.class);
      assertNotNull(method, "error method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "error should be static");
      assertEquals(issueClass, method.getReturnType(), "error should return ValidationIssue");
    }

    @Test
    @DisplayName("ValidationIssue should have warning factory method")
    void validationIssueShouldHaveWarningFactoryMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("warning", String.class, String.class);
      assertNotNull(method, "warning method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "warning should be static");
      assertEquals(issueClass, method.getReturnType(), "warning should return ValidationIssue");
    }

    @Test
    @DisplayName("ValidationIssue should have getType method")
    void validationIssueShouldHaveGetTypeMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("getType");
      assertNotNull(method, "getType method should exist");
    }

    @Test
    @DisplayName("ValidationIssue should have getSectionName method")
    void validationIssueShouldHaveGetSectionNameMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("getSectionName");
      assertNotNull(method, "getSectionName method should exist");
      assertEquals(String.class, method.getReturnType(), "getSectionName should return String");
    }

    @Test
    @DisplayName("ValidationIssue should have getMessage method")
    void validationIssueShouldHaveGetMessageMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("getMessage");
      assertNotNull(method, "getMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "getMessage should return String");
    }

    @Test
    @DisplayName("ValidationIssue should have getDetails method")
    void validationIssueShouldHaveGetDetailsMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("getDetails");
      assertNotNull(method, "getDetails method should exist");
      assertEquals(String.class, method.getReturnType(), "getDetails should return String");
    }

    @Test
    @DisplayName("ValidationIssue should have isError method")
    void validationIssueShouldHaveIsErrorMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("isError");
      assertNotNull(method, "isError method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isError should return boolean");
    }

    @Test
    @DisplayName("ValidationIssue should have isWarning method")
    void validationIssueShouldHaveIsWarningMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      Class<?> issueClass =
          Class.forName("ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssue");
      final Method method = issueClass.getMethod("isWarning");
      assertNotNull(method, "isWarning method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isWarning should return boolean");
    }
  }

  // ========================================================================
  // ValidationIssueType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ValidationIssueType Enum Tests")
  class ValidationIssueTypeEnumTests {

    @Test
    @DisplayName("should have ValidationIssueType nested enum")
    void shouldHaveValidationIssueTypeNestedEnum() {
      Class<?>[] nestedClasses = CustomSectionValidationResult.class.getDeclaredClasses();
      boolean hasEnum =
          Arrays.stream(nestedClasses)
              .anyMatch(c -> c.getSimpleName().equals("ValidationIssueType"));
      assertTrue(
          hasEnum, "CustomSectionValidationResult should have ValidationIssueType nested enum");
    }

    @Test
    @DisplayName("ValidationIssueType should be an enum")
    void validationIssueTypeShouldBeAnEnum() throws ClassNotFoundException {
      Class<?> enumClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssueType");
      assertTrue(enumClass.isEnum(), "ValidationIssueType should be an enum");
    }

    @Test
    @DisplayName("ValidationIssueType should have ERROR value")
    void validationIssueTypeShouldHaveErrorValue() throws ClassNotFoundException {
      Class<?> enumClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssueType");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasError = Arrays.stream(constants).anyMatch(c -> c.toString().equals("ERROR"));
      assertTrue(hasError, "ValidationIssueType should have ERROR value");
    }

    @Test
    @DisplayName("ValidationIssueType should have WARNING value")
    void validationIssueTypeShouldHaveWarningValue() throws ClassNotFoundException {
      Class<?> enumClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssueType");
      Object[] constants = enumClass.getEnumConstants();
      boolean hasWarning = Arrays.stream(constants).anyMatch(c -> c.toString().equals("WARNING"));
      assertTrue(hasWarning, "ValidationIssueType should have WARNING value");
    }

    @Test
    @DisplayName("ValidationIssueType should have exactly 2 values")
    void validationIssueTypeShouldHaveExactly2Values() throws ClassNotFoundException {
      Class<?> enumClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.CustomSectionValidationResult$ValidationIssueType");
      Object[] constants = enumClass.getEnumConstants();
      assertEquals(2, constants.length, "ValidationIssueType should have exactly 2 values");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public methods")
    void shouldHaveAllExpectedPublicMethods() {
      Set<String> expectedMethods =
          Set.of(
              "isValid",
              "getErrors",
              "getWarnings",
              "getSummary",
              "hasIssues",
              "getIssueCount",
              "getAllIssues",
              "getIssuesForSection",
              "success",
              "successWithWarnings",
              "failure",
              "builder",
              "toString");

      Set<String> actualMethods =
          Arrays.stream(CustomSectionValidationResult.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "CustomSectionValidationResult should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Nested Classes Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Count Tests")
  class NestedClassesCountTests {

    @Test
    @DisplayName("should have exactly 3 public nested types")
    void shouldHaveExactly3PublicNestedTypes() {
      Class<?>[] nestedClasses = CustomSectionValidationResult.class.getDeclaredClasses();
      long publicNestedCount =
          Arrays.stream(nestedClasses).filter(c -> Modifier.isPublic(c.getModifiers())).count();
      assertEquals(3, publicNestedCount, "Should have exactly 3 public nested types");
    }

    @Test
    @DisplayName("should have Builder, ValidationIssue, and ValidationIssueType nested types")
    void shouldHaveCorrectNestedTypes() {
      Class<?>[] nestedClasses = CustomSectionValidationResult.class.getDeclaredClasses();
      Set<String> nestedNames =
          Arrays.stream(nestedClasses)
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedNames.contains("Builder"), "Should have Builder nested class");
      assertTrue(
          nestedNames.contains("ValidationIssue"), "Should have ValidationIssue nested class");
      assertTrue(
          nestedNames.contains("ValidationIssueType"),
          "Should have ValidationIssueType nested enum");
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getErrors should return List<ValidationIssue>")
    void getErrorsShouldReturnListOfValidationIssue() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getErrors");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
    }

    @Test
    @DisplayName("getWarnings should return List<ValidationIssue>")
    void getWarningsShouldReturnListOfValidationIssue() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getWarnings");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
    }

    @Test
    @DisplayName("getAllIssues should return List<ValidationIssue>")
    void getAllIssuesShouldReturnListOfValidationIssue() throws NoSuchMethodException {
      final Method method = CustomSectionValidationResult.class.getMethod("getAllIssues");
      Type returnType = method.getGenericReturnType();
      assertTrue(returnType instanceof ParameterizedType, "Return type should be parameterized");
      ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
    }
  }
}
