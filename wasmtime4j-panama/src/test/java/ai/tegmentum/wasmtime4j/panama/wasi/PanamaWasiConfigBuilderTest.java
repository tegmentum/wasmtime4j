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

package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.WasiImportResolver;
import ai.tegmentum.wasmtime4j.wasi.WasiSecurityPolicy;
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiConfigBuilder} class.
 *
 * <p>PanamaWasiConfigBuilder provides builder for WASI configuration.
 */
@DisplayName("PanamaWasiConfigBuilder Tests")
class PanamaWasiConfigBuilderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiConfigBuilder.class.getModifiers()),
          "PanamaWasiConfigBuilder should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiConfigBuilder.class.getModifiers()),
          "PanamaWasiConfigBuilder should be final");
    }

    @Test
    @DisplayName("should implement WasiConfigBuilder interface")
    void shouldImplementWasiConfigBuilderInterface() {
      assertTrue(
          WasiConfigBuilder.class.isAssignableFrom(PanamaWasiConfigBuilder.class),
          "PanamaWasiConfigBuilder should implement WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have default constructor")
    void shouldHaveDefaultConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = PanamaWasiConfigBuilder.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have withEnvironment method with name and value")
    void shouldHaveWithEnvironmentMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withEnvironment", String.class, String.class);
      assertNotNull(method, "withEnvironment(name, value) method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withEnvironment method with map")
    void shouldHaveWithEnvironmentMapMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withEnvironment", Map.class);
      assertNotNull(method, "withEnvironment(map) method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutEnvironment method")
    void shouldHaveWithoutEnvironmentMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withoutEnvironment", String.class);
      assertNotNull(method, "withoutEnvironment method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearEnvironment method")
    void shouldHaveClearEnvironmentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("clearEnvironment");
      assertNotNull(method, "clearEnvironment method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have withArgument method")
    void shouldHaveWithArgumentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withArgument", String.class);
      assertNotNull(method, "withArgument method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withArguments method")
    void shouldHaveWithArgumentsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withArguments", List.class);
      assertNotNull(method, "withArguments method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearArguments method")
    void shouldHaveClearArgumentsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("clearArguments");
      assertNotNull(method, "clearArguments method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have withPreopenDirectory method")
    void shouldHaveWithPreopenDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withPreopenDirectory", String.class, Path.class);
      assertNotNull(method, "withPreopenDirectory method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withPreopenDirectories method")
    void shouldHaveWithPreopenDirectoriesMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withPreopenDirectories", Map.class);
      assertNotNull(method, "withPreopenDirectories method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutPreopenDirectory method")
    void shouldHaveWithoutPreopenDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withoutPreopenDirectory", String.class);
      assertNotNull(method, "withoutPreopenDirectory method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearPreopenDirectories method")
    void shouldHaveClearPreopenDirectoriesMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("clearPreopenDirectories");
      assertNotNull(method, "clearPreopenDirectories method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withWorkingDirectory method")
    void shouldHaveWithWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withWorkingDirectory", String.class);
      assertNotNull(method, "withWorkingDirectory method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutWorkingDirectory method")
    void shouldHaveWithoutWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withoutWorkingDirectory");
      assertNotNull(method, "withoutWorkingDirectory method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Timeout Method Tests")
  class TimeoutMethodTests {

    @Test
    @DisplayName("should have withExecutionTimeout method")
    void shouldHaveWithExecutionTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withExecutionTimeout", Duration.class);
      assertNotNull(method, "withExecutionTimeout method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutExecutionTimeout method")
    void shouldHaveWithoutExecutionTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withoutExecutionTimeout");
      assertNotNull(method, "withoutExecutionTimeout method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Security Method Tests")
  class SecurityMethodTests {

    @Test
    @DisplayName("should have withSecurityPolicy method")
    void shouldHaveWithSecurityPolicyMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withSecurityPolicy", WasiSecurityPolicy.class);
      assertNotNull(method, "withSecurityPolicy method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutSecurityPolicy method")
    void shouldHaveWithoutSecurityPolicyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withoutSecurityPolicy");
      assertNotNull(method, "withoutSecurityPolicy method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Import Resolver Method Tests")
  class ImportResolverMethodTests {

    @Test
    @DisplayName("should have withImportResolver method")
    void shouldHaveWithImportResolverMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod(
              "withImportResolver", String.class, WasiImportResolver.class);
      assertNotNull(method, "withImportResolver method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withImportResolvers method")
    void shouldHaveWithImportResolversMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withImportResolvers", Map.class);
      assertNotNull(method, "withImportResolvers method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutImportResolver method")
    void shouldHaveWithoutImportResolverMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withoutImportResolver", String.class);
      assertNotNull(method, "withoutImportResolver method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have clearImportResolvers method")
    void shouldHaveClearImportResolversMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("clearImportResolvers");
      assertNotNull(method, "clearImportResolvers method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Mode Method Tests")
  class ModeMethodTests {

    @Test
    @DisplayName("should have withValidation method")
    void shouldHaveWithValidationMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withValidation", boolean.class);
      assertNotNull(method, "withValidation method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withStrictMode method")
    void shouldHaveWithStrictModeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withStrictMode", boolean.class);
      assertNotNull(method, "withStrictMode method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Version Method Tests")
  class VersionMethodTests {

    @Test
    @DisplayName("should have withWasiVersion method")
    void shouldHaveWithWasiVersionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withWasiVersion", WasiVersion.class);
      assertNotNull(method, "withWasiVersion method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Async Method Tests")
  class AsyncMethodTests {

    @Test
    @DisplayName("should have withAsyncOperations method")
    void shouldHaveWithAsyncOperationsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withAsyncOperations", boolean.class);
      assertNotNull(method, "withAsyncOperations method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withMaxAsyncOperations method")
    void shouldHaveWithMaxAsyncOperationsMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withMaxAsyncOperations", int.class);
      assertNotNull(method, "withMaxAsyncOperations method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withoutMaxAsyncOperations method")
    void shouldHaveWithoutMaxAsyncOperationsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("withoutMaxAsyncOperations");
      assertNotNull(method, "withoutMaxAsyncOperations method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have withAsyncOperationTimeout method")
    void shouldHaveWithAsyncOperationTimeoutMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiConfigBuilder.class.getMethod("withAsyncOperationTimeout", Duration.class);
      assertNotNull(method, "withAsyncOperationTimeout method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfigBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WasiConfig.class, method.getReturnType(), "Should return WasiConfig");
    }
  }
}
