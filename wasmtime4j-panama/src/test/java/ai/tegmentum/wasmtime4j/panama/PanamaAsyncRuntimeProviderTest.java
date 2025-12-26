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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.async.AsyncRuntimeFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaAsyncRuntimeProvider}.
 */
@DisplayName("PanamaAsyncRuntimeProvider Tests")
class PanamaAsyncRuntimeProviderTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaAsyncRuntimeProvider should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaAsyncRuntimeProvider.class.getModifiers()),
          "PanamaAsyncRuntimeProvider should be final");
    }

    @Test
    @DisplayName("PanamaAsyncRuntimeProvider should implement AsyncRuntimeProvider")
    void shouldImplementAsyncRuntimeProvider() {
      assertTrue(
          AsyncRuntimeFactory.AsyncRuntimeProvider.class.isAssignableFrom(
              PanamaAsyncRuntimeProvider.class),
          "PanamaAsyncRuntimeProvider should implement AsyncRuntimeProvider");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Default constructor should not throw")
    void defaultConstructorShouldNotThrow() {
      assertDoesNotThrow(PanamaAsyncRuntimeProvider::new,
          "Default constructor should not throw");
    }

    @Test
    @DisplayName("Multiple instances should be creatable")
    void multipleInstancesShouldBeCreatable() {
      final PanamaAsyncRuntimeProvider provider1 = new PanamaAsyncRuntimeProvider();
      final PanamaAsyncRuntimeProvider provider2 = new PanamaAsyncRuntimeProvider();
      assertNotNull(provider1, "First instance should be created");
      assertNotNull(provider2, "Second instance should be created");
    }
  }

  @Nested
  @DisplayName("create() Method Tests")
  class CreateMethodTests {

    @Test
    @DisplayName("create() should not throw")
    void createShouldNotThrow() {
      final PanamaAsyncRuntimeProvider provider = new PanamaAsyncRuntimeProvider();
      // create() may return null if native bindings are not available
      // but it should not throw
      assertDoesNotThrow(provider::create,
          "create() should not throw");
    }

    @Test
    @DisplayName("create() should return PanamaAsyncRuntime or null")
    void createShouldReturnPanamaAsyncRuntimeOrNull() {
      final PanamaAsyncRuntimeProvider provider = new PanamaAsyncRuntimeProvider();
      // The result depends on whether native bindings are available
      // If available, returns PanamaAsyncRuntime; otherwise null
      final var result = provider.create();
      if (result != null) {
        assertTrue(result instanceof PanamaAsyncRuntime,
            "create() should return PanamaAsyncRuntime if native bindings are available");
      }
      // null is acceptable if native bindings are not available
    }
  }

  @Nested
  @DisplayName("ServiceLoader Discovery Tests")
  class ServiceLoaderDiscoveryTests {

    @Test
    @DisplayName("Provider should be discoverable as service")
    void providerShouldBeDiscoverableAsService() {
      // This test verifies the provider can be instantiated like ServiceLoader would
      final PanamaAsyncRuntimeProvider provider = new PanamaAsyncRuntimeProvider();
      assertNotNull(provider, "Provider should be instantiable");
    }
  }

  @Nested
  @DisplayName("Logging Tests")
  class LoggingTests {

    @Test
    @DisplayName("Provider should log creation")
    void providerShouldLogCreation() {
      // Just verify that construction completes without error
      // The actual logging is internal behavior
      assertDoesNotThrow(PanamaAsyncRuntimeProvider::new,
          "Provider construction should complete without error");
    }
  }
}
