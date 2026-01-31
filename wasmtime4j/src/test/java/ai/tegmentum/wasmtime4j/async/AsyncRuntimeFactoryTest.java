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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AsyncRuntimeFactory} - factory for creating AsyncRuntime instances.
 *
 * <p>Validates utility class structure and ServiceLoader-based factory behavior. No native runtime
 * providers are available in the test environment, so create() should throw WasmException.
 */
@DisplayName("AsyncRuntimeFactory Tests")
class AsyncRuntimeFactoryTest {

  @Nested
  @DisplayName("Utility Class Structure Tests")
  class UtilityClassStructureTests {

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(AsyncRuntimeFactory.class.getModifiers()),
          "AsyncRuntimeFactory should be a final class");
    }

    @Test
    @DisplayName("constructor should be private")
    void constructorShouldBePrivate() throws NoSuchMethodException {
      final java.lang.reflect.Constructor<AsyncRuntimeFactory> constructor =
          AsyncRuntimeFactory.class.getDeclaredConstructor();
      assertTrue(
          java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
          "AsyncRuntimeFactory constructor should be private");
    }
  }

  @Nested
  @DisplayName("Create Method Tests")
  class CreateMethodTests {

    @Test
    @DisplayName("create should throw WasmException when no provider available")
    void createShouldThrowWhenNoProviderAvailable() {
      final WasmException ex =
          assertThrows(
              WasmException.class,
              AsyncRuntimeFactory::create,
              "create() should throw WasmException when no AsyncRuntimeProvider is on classpath");
      assertNotNull(ex.getMessage(), "Exception should have a descriptive message");
      assertTrue(
          ex.getMessage().contains("No AsyncRuntime implementation found"),
          "Exception message should indicate missing implementation, got: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("GetSharedInstance Method Tests")
  class GetSharedInstanceMethodTests {

    @Test
    @DisplayName("getSharedInstance should fail when no provider available")
    void getSharedInstanceShouldFailWhenNoProviderAvailable() {
      assertThrows(
          ExceptionInInitializerError.class,
          AsyncRuntimeFactory::getSharedInstance,
          "getSharedInstance() should throw when no provider is available");
    }
  }

  @Nested
  @DisplayName("AsyncRuntimeProvider Interface Tests")
  class AsyncRuntimeProviderInterfaceTests {

    @Test
    @DisplayName("AsyncRuntimeProvider should be a public interface")
    void asyncRuntimeProviderShouldBePublicInterface() {
      assertTrue(
          AsyncRuntimeFactory.AsyncRuntimeProvider.class.isInterface(),
          "AsyncRuntimeProvider should be an interface");
    }

    @Test
    @DisplayName("AsyncRuntimeProvider should have create method")
    void asyncRuntimeProviderShouldHaveCreateMethod() throws NoSuchMethodException {
      final java.lang.reflect.Method createMethod =
          AsyncRuntimeFactory.AsyncRuntimeProvider.class.getDeclaredMethod("create");
      assertNotNull(createMethod, "AsyncRuntimeProvider should have a create() method");
      assertTrue(
          AsyncRuntime.class.isAssignableFrom(createMethod.getReturnType()),
          "create() should return AsyncRuntime");
    }
  }
}
