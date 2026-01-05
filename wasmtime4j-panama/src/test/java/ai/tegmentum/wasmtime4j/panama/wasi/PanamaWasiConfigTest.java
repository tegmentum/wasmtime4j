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
import ai.tegmentum.wasmtime4j.wasi.WasiVersion;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiConfig} class.
 *
 * <p>PanamaWasiConfig is the Panama implementation of WASI configuration.
 */
@DisplayName("PanamaWasiConfig Tests")
class PanamaWasiConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiConfig.class.getModifiers()),
          "PanamaWasiConfig should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiConfig.class.getModifiers()),
          "PanamaWasiConfig should be final");
    }

    @Test
    @DisplayName("should implement WasiConfig interface")
    void shouldImplementWasiConfigInterface() {
      assertTrue(
          WasiConfig.class.isAssignableFrom(PanamaWasiConfig.class),
          "PanamaWasiConfig should implement WasiConfig");
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getEnvironment");
      assertNotNull(method, "getEnvironment method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have isInheritEnvironment method")
    void shouldHaveIsInheritEnvironmentMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("isInheritEnvironment");
      assertNotNull(method, "isInheritEnvironment method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Arguments Method Tests")
  class ArgumentsMethodTests {

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getArguments");
      assertNotNull(method, "getArguments method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Directory Method Tests")
  class DirectoryMethodTests {

    @Test
    @DisplayName("should have getPreopenDirectories method")
    void shouldHaveGetPreopenDirectoriesMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getPreopenDirectories");
      assertNotNull(method, "getPreopenDirectories method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getWorkingDirectory");
      assertNotNull(method, "getWorkingDirectory method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Limits Method Tests")
  class LimitsMethodTests {

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getMemoryLimit");
      assertNotNull(method, "getMemoryLimit method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getExecutionTimeout method")
    void shouldHaveGetExecutionTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getExecutionTimeout");
      assertNotNull(method, "getExecutionTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getResourceLimits");
      assertNotNull(method, "getResourceLimits method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Security Method Tests")
  class SecurityMethodTests {

    @Test
    @DisplayName("should have getSecurityPolicy method")
    void shouldHaveGetSecurityPolicyMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getSecurityPolicy");
      assertNotNull(method, "getSecurityPolicy method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Import Resolver Method Tests")
  class ImportResolverMethodTests {

    @Test
    @DisplayName("should have getImportResolvers method")
    void shouldHaveGetImportResolversMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getImportResolvers");
      assertNotNull(method, "getImportResolvers method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have isValidationEnabled method")
    void shouldHaveIsValidationEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("isValidationEnabled");
      assertNotNull(method, "isValidationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isStrictModeEnabled method")
    void shouldHaveIsStrictModeEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("isStrictModeEnabled");
      assertNotNull(method, "isStrictModeEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Version Method Tests")
  class VersionMethodTests {

    @Test
    @DisplayName("should have getWasiVersion method")
    void shouldHaveGetWasiVersionMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getWasiVersion");
      assertNotNull(method, "getWasiVersion method should exist");
      assertEquals(WasiVersion.class, method.getReturnType(), "Should return WasiVersion");
    }
  }

  @Nested
  @DisplayName("Async Operations Method Tests")
  class AsyncOperationsMethodTests {

    @Test
    @DisplayName("should have isAsyncOperationsEnabled method")
    void shouldHaveIsAsyncOperationsEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("isAsyncOperationsEnabled");
      assertNotNull(method, "isAsyncOperationsEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxAsyncOperations method")
    void shouldHaveGetMaxAsyncOperationsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getMaxAsyncOperations");
      assertNotNull(method, "getMaxAsyncOperations method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getAsyncOperationTimeout method")
    void shouldHaveGetAsyncOperationTimeoutMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("getAsyncOperationTimeout");
      assertNotNull(method, "getAsyncOperationTimeout method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Builder Method Tests")
  class BuilderMethodTests {

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiConfig.class.getMethod("toBuilder");
      assertNotNull(method, "toBuilder method should exist");
      assertEquals(
          WasiConfigBuilder.class, method.getReturnType(), "Should return WasiConfigBuilder");
    }
  }
}
