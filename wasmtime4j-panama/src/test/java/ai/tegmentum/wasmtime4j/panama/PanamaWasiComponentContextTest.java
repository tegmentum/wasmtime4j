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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.WasiComponent;
import ai.tegmentum.wasmtime4j.wasi.WasiComponentContext;
import ai.tegmentum.wasmtime4j.wasi.WasiRuntimeInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiComponentContext} class.
 *
 * <p>PanamaWasiComponentContext provides WASI context functionality for components.
 */
@DisplayName("PanamaWasiComponentContext Tests")
class PanamaWasiComponentContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiComponentContext.class.getModifiers()),
          "PanamaWasiComponentContext should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiComponentContext.class.getModifiers()),
          "PanamaWasiComponentContext should be final");
    }

    @Test
    @DisplayName("should implement WasiComponentContext interface")
    void shouldImplementWasiComponentContextInterface() {
      assertTrue(
          WasiComponentContext.class.isAssignableFrom(PanamaWasiComponentContext.class),
          "PanamaWasiComponentContext should implement WasiComponentContext");
    }
  }

  @Nested
  @DisplayName("WasiContext Method Tests")
  class WasiContextMethodTests {

    @Test
    @DisplayName("should have createComponent method with byte array")
    void shouldHaveCreateComponentMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiComponentContext.class.getMethod("createComponent", byte[].class);
      assertNotNull(method, "createComponent method should exist");
      assertEquals(WasiComponent.class, method.getReturnType(), "Should return WasiComponent");
    }

    @Test
    @DisplayName("should have getRuntimeInfo method")
    void shouldHaveGetRuntimeInfoMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("getRuntimeInfo");
      assertNotNull(method, "getRuntimeInfo method should exist");
      assertEquals(WasiRuntimeInfo.class, method.getReturnType(), "Should return WasiRuntimeInfo");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Extended Method Tests")
  class ExtendedMethodTests {

    @Test
    @DisplayName("should have createComponentFromFile method")
    void shouldHaveCreateComponentFromFileMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiComponentContext.class.getMethod("createComponentFromFile", Path.class);
      assertNotNull(method, "createComponentFromFile method should exist");
      assertEquals(WasiComponent.class, method.getReturnType(), "Should return WasiComponent");
    }

    @Test
    @DisplayName("should have createComponentFromFile method with name")
    void shouldHaveCreateComponentFromFileMethodWithName() throws NoSuchMethodException {
      final Method method =
          PanamaWasiComponentContext.class.getMethod(
              "createComponentFromFile", Path.class, String.class);
      assertNotNull(method, "createComponentFromFile method should exist");
      assertEquals(WasiComponent.class, method.getReturnType(), "Should return WasiComponent");
    }

    @Test
    @DisplayName("should have getActiveInstancesCount method")
    void shouldHaveGetActiveInstancesCountMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("getActiveInstancesCount");
      assertNotNull(method, "getActiveInstancesCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have cleanupInstances method")
    void shouldHaveCleanupInstancesMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("cleanupInstances");
      assertNotNull(method, "cleanupInstances method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMemoryStatistics method")
    void shouldHaveGetMemoryStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiComponentContext.class.getMethod("getMemoryStatistics");
      assertNotNull(method, "getMemoryStatistics method should exist");
      assertEquals(
          ArenaResourceManager.Statistics.class,
          method.getReturnType(),
          "Should return Statistics");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getComponentEngine method")
    void shouldHaveGetComponentEngineMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiComponentContext.class.getDeclaredMethod("getComponentEngine");
      assertNotNull(method, "getComponentEngine method should exist");
    }

    @Test
    @DisplayName("should have getResourceManager method")
    void shouldHaveGetResourceManagerMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiComponentContext.class.getDeclaredMethod("getResourceManager");
      assertNotNull(method, "getResourceManager method should exist");
      assertEquals(
          ArenaResourceManager.class, method.getReturnType(), "Should return ArenaResourceManager");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public default constructor")
    void shouldHavePublicDefaultConstructor() throws NoSuchMethodException {
      var constructor = PanamaWasiComponentContext.class.getConstructor();
      assertNotNull(constructor, "Default constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }
}
