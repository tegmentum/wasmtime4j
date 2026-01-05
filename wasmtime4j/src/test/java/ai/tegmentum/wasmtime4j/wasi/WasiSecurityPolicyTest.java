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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiSecurityPolicy} interface.
 *
 * <p>WasiSecurityPolicy defines access control rules and security constraints for WASI components.
 */
@DisplayName("WasiSecurityPolicy Tests")
class WasiSecurityPolicyTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicy.class.getModifiers()),
          "WasiSecurityPolicy should be public");
      assertTrue(
          WasiSecurityPolicy.class.isInterface(), "WasiSecurityPolicy should be an interface");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          WasiSecurityPolicyBuilder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have permissive static method")
    void shouldHavePermissiveStaticMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("permissive");
      assertNotNull(method, "permissive method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "permissive should be static");
      assertEquals(
          WasiSecurityPolicy.class, method.getReturnType(), "Should return WasiSecurityPolicy");
    }

    @Test
    @DisplayName("should have restrictive static method")
    void shouldHaveRestrictiveStaticMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("restrictive");
      assertNotNull(method, "restrictive method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "restrictive should be static");
      assertEquals(
          WasiSecurityPolicy.class, method.getReturnType(), "Should return WasiSecurityPolicy");
    }
  }

  @Nested
  @DisplayName("File System Access Method Tests")
  class FileSystemAccessMethodTests {

    @Test
    @DisplayName("should have isFileSystemAccessAllowed method")
    void shouldHaveIsFileSystemAccessAllowedMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityPolicy.class.getMethod("isFileSystemAccessAllowed", Path.class, String.class);
      assertNotNull(method, "isFileSystemAccessAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getAllowedFileSystemOperations method")
    void shouldHaveGetAllowedFileSystemOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("getAllowedFileSystemOperations");
      assertNotNull(method, "getAllowedFileSystemOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }

    @Test
    @DisplayName("should have getAllowedPaths method")
    void shouldHaveGetAllowedPathsMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("getAllowedPaths");
      assertNotNull(method, "getAllowedPaths method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getBlockedPaths method")
    void shouldHaveGetBlockedPathsMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("getBlockedPaths");
      assertNotNull(method, "getBlockedPaths method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Network Access Method Tests")
  class NetworkAccessMethodTests {

    @Test
    @DisplayName("should have isNetworkAccessAllowed method")
    void shouldHaveIsNetworkAccessAllowedMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityPolicy.class.getMethod(
              "isNetworkAccessAllowed", String.class, int.class, String.class);
      assertNotNull(method, "isNetworkAccessAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getAllowedNetworkOperations method")
    void shouldHaveGetAllowedNetworkOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("getAllowedNetworkOperations");
      assertNotNull(method, "getAllowedNetworkOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Should return Set");
    }
  }

  @Nested
  @DisplayName("Environment and Process Method Tests")
  class EnvironmentAndProcessMethodTests {

    @Test
    @DisplayName("should have isEnvironmentVariableAllowed method")
    void shouldHaveIsEnvironmentVariableAllowedMethod() throws NoSuchMethodException {
      final Method method =
          WasiSecurityPolicy.class.getMethod("isEnvironmentVariableAllowed", String.class);
      assertNotNull(method, "isEnvironmentVariableAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isProcessSpawningAllowed method")
    void shouldHaveIsProcessSpawningAllowedMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("isProcessSpawningAllowed");
      assertNotNull(method, "isProcessSpawningAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isThreadingAllowed method")
    void shouldHaveIsThreadingAllowedMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("isThreadingAllowed");
      assertNotNull(method, "isThreadingAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      final Method method = WasiSecurityPolicy.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
