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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiConfigBuilder interface.
 *
 * <p>WasiConfigBuilder provides a fluent API for configuring WASI component instantiation.
 */
@DisplayName("WasiConfigBuilder Interface Tests")
class WasiConfigBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiConfigBuilder.class.isInterface(), "WasiConfigBuilder should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiConfigBuilder.class.getModifiers()),
          "WasiConfigBuilder should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0, WasiConfigBuilder.class.getInterfaces().length, "Should not extend any interface");
    }
  }

  // ========================================================================
  // Environment Variable Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Environment Variable Builder Method Tests")
  class EnvironmentVariableBuilderMethodTests {

    @Test
    @DisplayName("should have withEnvironment(String, String) method")
    void shouldHaveWithEnvironmentStringStringMethod() throws NoSuchMethodException {
      Method method =
          WasiConfigBuilder.class.getMethod("withEnvironment", String.class, String.class);
      assertNotNull(method, "withEnvironment(String, String) method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withEnvironment(Map) method")
    void shouldHaveWithEnvironmentMapMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withEnvironment", Map.class);
      assertNotNull(method, "withEnvironment(Map) method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutEnvironment method")
    void shouldHaveWithoutEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutEnvironment", String.class);
      assertNotNull(method, "withoutEnvironment method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearEnvironment method")
    void shouldHaveClearEnvironmentMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("clearEnvironment");
      assertNotNull(method, "clearEnvironment method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Argument Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Argument Builder Method Tests")
  class ArgumentBuilderMethodTests {

    @Test
    @DisplayName("should have withArgument method")
    void shouldHaveWithArgumentMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withArgument", String.class);
      assertNotNull(method, "withArgument method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withArguments method")
    void shouldHaveWithArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withArguments", List.class);
      assertNotNull(method, "withArguments method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearArguments method")
    void shouldHaveClearArgumentsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("clearArguments");
      assertNotNull(method, "clearArguments method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Directory Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Directory Builder Method Tests")
  class DirectoryBuilderMethodTests {

    @Test
    @DisplayName("should have withPreopenDirectory method")
    void shouldHaveWithPreopenDirectoryMethod() throws NoSuchMethodException {
      Method method =
          WasiConfigBuilder.class.getMethod("withPreopenDirectory", String.class, Path.class);
      assertNotNull(method, "withPreopenDirectory method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withPreopenDirectories method")
    void shouldHaveWithPreopenDirectoriesMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withPreopenDirectories", Map.class);
      assertNotNull(method, "withPreopenDirectories method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutPreopenDirectory method")
    void shouldHaveWithoutPreopenDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutPreopenDirectory", String.class);
      assertNotNull(method, "withoutPreopenDirectory method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearPreopenDirectories method")
    void shouldHaveClearPreopenDirectoriesMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("clearPreopenDirectories");
      assertNotNull(method, "clearPreopenDirectories method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withWorkingDirectory method")
    void shouldHaveWithWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withWorkingDirectory", String.class);
      assertNotNull(method, "withWorkingDirectory method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutWorkingDirectory method")
    void shouldHaveWithoutWorkingDirectoryMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutWorkingDirectory");
      assertNotNull(method, "withoutWorkingDirectory method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Resource Limit Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Limit Builder Method Tests")
  class ResourceLimitBuilderMethodTests {

    @Test
    @DisplayName("should have withMemoryLimit method")
    void shouldHaveWithMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withMemoryLimit", long.class);
      assertNotNull(method, "withMemoryLimit method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutMemoryLimit method")
    void shouldHaveWithoutMemoryLimitMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutMemoryLimit");
      assertNotNull(method, "withoutMemoryLimit method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withExecutionTimeout method")
    void shouldHaveWithExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withExecutionTimeout", Duration.class);
      assertNotNull(method, "withExecutionTimeout method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutExecutionTimeout method")
    void shouldHaveWithoutExecutionTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutExecutionTimeout");
      assertNotNull(method, "withoutExecutionTimeout method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withResourceLimits method")
    void shouldHaveWithResourceLimitsMethod() throws NoSuchMethodException {
      Method method =
          WasiConfigBuilder.class.getMethod("withResourceLimits", WasiResourceLimits.class);
      assertNotNull(method, "withResourceLimits method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutResourceLimits method")
    void shouldHaveWithoutResourceLimitsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutResourceLimits");
      assertNotNull(method, "withoutResourceLimits method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }


  // ========================================================================
  // Import Resolver Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Import Resolver Builder Method Tests")
  class ImportResolverBuilderMethodTests {

    @Test
    @DisplayName("should have withImportResolver method")
    void shouldHaveWithImportResolverMethod() throws NoSuchMethodException {
      Method method =
          WasiConfigBuilder.class.getMethod(
              "withImportResolver", String.class, WasiImportResolver.class);
      assertNotNull(method, "withImportResolver method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withImportResolvers method")
    void shouldHaveWithImportResolversMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withImportResolvers", Map.class);
      assertNotNull(method, "withImportResolvers method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutImportResolver method")
    void shouldHaveWithoutImportResolverMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutImportResolver", String.class);
      assertNotNull(method, "withoutImportResolver method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearImportResolvers method")
    void shouldHaveClearImportResolversMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("clearImportResolvers");
      assertNotNull(method, "clearImportResolvers method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Validation and Mode Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Validation and Mode Builder Method Tests")
  class ValidationAndModeBuilderMethodTests {

    @Test
    @DisplayName("should have withValidation method")
    void shouldHaveWithValidationMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withValidation", boolean.class);
      assertNotNull(method, "withValidation method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withStrictMode method")
    void shouldHaveWithStrictModeMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withStrictMode", boolean.class);
      assertNotNull(method, "withStrictMode method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withWasiVersion method")
    void shouldHaveWithWasiVersionMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withWasiVersion", WasiVersion.class);
      assertNotNull(method, "withWasiVersion method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Async Operation Builder Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Async Operation Builder Method Tests")
  class AsyncOperationBuilderMethodTests {

    @Test
    @DisplayName("should have withAsyncOperations method")
    void shouldHaveWithAsyncOperationsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withAsyncOperations", boolean.class);
      assertNotNull(method, "withAsyncOperations method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withMaxAsyncOperations method")
    void shouldHaveWithMaxAsyncOperationsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withMaxAsyncOperations", int.class);
      assertNotNull(method, "withMaxAsyncOperations method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutMaxAsyncOperations method")
    void shouldHaveWithoutMaxAsyncOperationsMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutMaxAsyncOperations");
      assertNotNull(method, "withoutMaxAsyncOperations method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withAsyncOperationTimeout method")
    void shouldHaveWithAsyncOperationTimeoutMethod() throws NoSuchMethodException {
      Method method =
          WasiConfigBuilder.class.getMethod("withAsyncOperationTimeout", Duration.class);
      assertNotNull(method, "withAsyncOperationTimeout method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutAsyncOperationTimeout method")
    void shouldHaveWithoutAsyncOperationTimeoutMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("withoutAsyncOperationTimeout");
      assertNotNull(method, "withoutAsyncOperationTimeout method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  // ========================================================================
  // Build and Validate Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build and Validate Method Tests")
  class BuildAndValidateMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(WasiConfig.class, method.getReturnType(), "Should return WasiConfig");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      Method method = WasiConfigBuilder.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected builder methods")
    void shouldHaveAllExpectedBuilderMethods() {
      Set<String> expectedMethods =
          Set.of(
              "withEnvironment",
              "withoutEnvironment",
              "clearEnvironment",
              "withArgument",
              "withArguments",
              "clearArguments",
              "withPreopenDirectory",
              "withPreopenDirectories",
              "withoutPreopenDirectory",
              "clearPreopenDirectories",
              "withWorkingDirectory",
              "withoutWorkingDirectory",
              "withMemoryLimit",
              "withoutMemoryLimit",
              "withExecutionTimeout",
              "withoutExecutionTimeout",
              "withResourceLimits",
              "withoutResourceLimits",

              "withImportResolver",
              "withImportResolvers",
              "withoutImportResolver",
              "clearImportResolvers",
              "withValidation",
              "withStrictMode",
              "withWasiVersion",
              "withAsyncOperations",
              "withMaxAsyncOperations",
              "withoutMaxAsyncOperations",
              "withAsyncOperationTimeout",
              "withoutAsyncOperationTimeout",
              "build",
              "validate");

      Set<String> actualMethods =
          Arrays.stream(WasiConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 32 abstract methods")
    void shouldHaveAtLeast32AbstractMethods() {
      long abstractCount =
          Arrays.stream(WasiConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractCount >= 32, "Should have at least 32 abstract methods");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(WasiConfigBuilder.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          WasiConfigBuilder.class.getDeclaredClasses().length,
          "WasiConfigBuilder should have no nested classes");
    }
  }
}
