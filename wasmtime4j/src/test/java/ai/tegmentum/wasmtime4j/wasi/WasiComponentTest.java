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

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiComponent} interface.
 *
 * <p>WasiComponent represents a WebAssembly component loaded with WASI capabilities.
 */
@DisplayName("WasiComponent Tests")
class WasiComponentTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiComponent.class.getModifiers()), "WasiComponent should be public");
      assertTrue(WasiComponent.class.isInterface(), "WasiComponent should be an interface");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiComponent.class),
          "WasiComponent should extend Closeable");
    }
  }

  @Nested
  @DisplayName("Name Method Tests")
  class NameMethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Interface Exports/Imports Method Tests")
  class InterfaceExportsImportsMethodTests {

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getImports method")
    void shouldHaveGetImportsMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getImports");
      assertNotNull(method, "getImports method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getExportMetadata method")
    void shouldHaveGetExportMetadataMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getExportMetadata", String.class);
      assertNotNull(method, "getExportMetadata method should exist");
      assertEquals(
          WasiInterfaceMetadata.class,
          method.getReturnType(),
          "Should return WasiInterfaceMetadata");
    }

    @Test
    @DisplayName("should have getImportMetadata method")
    void shouldHaveGetImportMetadataMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getImportMetadata", String.class);
      assertNotNull(method, "getImportMetadata method should exist");
      assertEquals(
          WasiInterfaceMetadata.class,
          method.getReturnType(),
          "Should return WasiInterfaceMetadata");
    }
  }

  @Nested
  @DisplayName("Instantiation Method Tests")
  class InstantiationMethodTests {

    @Test
    @DisplayName("should have instantiate method without config")
    void shouldHaveInstantiateMethodWithoutConfig() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("instantiate");
      assertNotNull(method, "instantiate method should exist");
      assertEquals(WasiInstance.class, method.getReturnType(), "Should return WasiInstance");
    }

    @Test
    @DisplayName("should have instantiate method with config")
    void shouldHaveInstantiateMethodWithConfig() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("instantiate", WasiConfig.class);
      assertNotNull(method, "instantiate method with config should exist");
      assertEquals(WasiInstance.class, method.getReturnType(), "Should return WasiInstance");
    }
  }

  @Nested
  @DisplayName("Validation Method Tests")
  class ValidationMethodTests {

    @Test
    @DisplayName("should have validate method without config")
    void shouldHaveValidateMethodWithoutConfig() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validate method with config")
    void shouldHaveValidateMethodWithConfig() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("validate", WasiConfig.class);
      assertNotNull(method, "validate method with config should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Stats Method Tests")
  class StatsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiComponentStats.class, method.getReturnType(), "Should return WasiComponentStats");
    }
  }

  @Nested
  @DisplayName("Validity Method Tests")
  class ValidityMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasiComponent.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
