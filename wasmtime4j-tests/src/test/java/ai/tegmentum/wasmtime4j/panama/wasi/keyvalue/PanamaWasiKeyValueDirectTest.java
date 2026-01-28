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
package ai.tegmentum.wasmtime4j.panama.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Direct integration tests for Panama WASI KeyValue implementation. */
@DisplayName("Panama WASI KeyValue Direct Tests")
public class PanamaWasiKeyValueDirectTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiKeyValueDirectTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI keyvalue tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
  }

  @AfterEach
  void tearDown() {
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  @Nested
  @DisplayName("PanamaWasiKeyValue Structure Tests")
  class KeyValueStructureTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiKeyValue class structure");

      final Class<?> clazz = PanamaWasiKeyValue.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have CRUD methods")
    void shouldHaveCrudMethods() {
      LOGGER.info("Testing CRUD methods");

      final Class<?> clazz = PanamaWasiKeyValue.class;
      final String[] expectedMethods = {"get", "set", "delete", "exists"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found CRUD method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have atomic operation methods")
    void shouldHaveAtomicOperationMethods() {
      LOGGER.info("Testing atomic operation methods");

      final Class<?> clazz = PanamaWasiKeyValue.class;
      final String[] expectedMethods = {
        "increment", "setIfAbsent", "setIfPresent", "compareAndSwap", "getAndDelete", "getAndSet"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found atomic method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have batch operation methods")
    void shouldHaveBatchOperationMethods() {
      LOGGER.info("Testing batch operation methods");

      final Class<?> clazz = PanamaWasiKeyValue.class;
      final String[] expectedMethods = {"getMultiple", "setMultiple", "deleteMultiple"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found batch method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have store management methods")
    void shouldHaveStoreManagementMethods() {
      LOGGER.info("Testing store management methods");

      final Class<?> clazz = PanamaWasiKeyValue.class;
      final String[] expectedMethods = {"size", "clear", "keys", "close"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found store management method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      LOGGER.info("Testing AutoCloseable implementation");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PanamaWasiKeyValue.class),
          "Should implement AutoCloseable");
    }
  }
}
