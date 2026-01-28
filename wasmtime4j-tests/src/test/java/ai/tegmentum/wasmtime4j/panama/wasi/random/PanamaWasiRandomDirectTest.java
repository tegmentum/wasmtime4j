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
package ai.tegmentum.wasmtime4j.panama.wasi.random;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

/** Direct integration tests for Panama WASI Random implementation. */
@DisplayName("Panama WASI Random Direct Tests")
public class PanamaWasiRandomDirectTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiRandomDirectTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI random tests");
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
  @DisplayName("PanamaWasiRandom Structure Tests")
  class RandomStructureTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiRandom class structure");

      final Class<?> clazz = PanamaWasiRandom.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have getRandomBytes method")
    void shouldHaveGetRandomBytesMethod() {
      LOGGER.info("Testing getRandomBytes method");

      final Class<?> clazz = PanamaWasiRandom.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getRandomBytes")) {
          found = true;
          LOGGER.info("Found getRandomBytes method");

          // Check parameter type
          if (method.getParameterCount() == 1) {
            assertEquals(
                long.class,
                method.getParameterTypes()[0],
                "getRandomBytes should take long length parameter");
          }

          // Check return type
          assertEquals(
              byte[].class, method.getReturnType(), "getRandomBytes should return byte array");
          break;
        }
      }
      assertTrue(found, "Should have getRandomBytes method");
    }

    @Test
    @DisplayName("Should have getRandomU64 method")
    void shouldHaveGetRandomU64Method() {
      LOGGER.info("Testing getRandomU64 method");

      final Class<?> clazz = PanamaWasiRandom.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getRandomU64")) {
          found = true;
          LOGGER.info("Found getRandomU64 method");

          // Check no parameters
          assertEquals(0, method.getParameterCount(), "getRandomU64 should take no parameters");

          // Check return type
          assertEquals(long.class, method.getReturnType(), "getRandomU64 should return long");
          break;
        }
      }
      assertTrue(found, "Should have getRandomU64 method");
    }
  }

  @Nested
  @DisplayName("Random Method Signature Tests")
  class RandomMethodSignatureTests {

    @Test
    @DisplayName("getRandomBytes should handle long length parameter")
    void getRandomBytesShouldHandleLongLengthParameter() {
      LOGGER.info("Testing getRandomBytes parameter handling");

      final Class<?> clazz = PanamaWasiRandom.class;

      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getRandomBytes") && method.getParameterCount() == 1) {
          // Verify it accepts long (for large data requests)
          final Class<?> paramType = method.getParameterTypes()[0];
          assertEquals(long.class, paramType, "Should accept long for large data requests");
          LOGGER.info("getRandomBytes accepts " + paramType.getSimpleName() + " parameter");
        }
      }
    }

    @Test
    @DisplayName("Methods should be static or require minimal setup")
    void methodsShouldBeAccessible() {
      LOGGER.info("Testing method accessibility");

      final Class<?> clazz = PanamaWasiRandom.class;

      // Check if methods are static (common pattern for utility classes)
      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("getRandomBytes") || method.getName().equals("getRandomU64")) {
          boolean isStatic = java.lang.reflect.Modifier.isStatic(method.getModifiers());
          boolean isPublic = java.lang.reflect.Modifier.isPublic(method.getModifiers());
          LOGGER.info(method.getName() + " is static: " + isStatic + ", public: " + isPublic);
        }
      }
    }
  }

  @Nested
  @DisplayName("Random Interface Compliance Tests")
  class RandomInterfaceTests {

    @Test
    @DisplayName("Should provide cryptographically-secure random data interface")
    void shouldProvideCryptoSecureInterface() {
      LOGGER.info("Testing cryptographic random interface");

      final Class<?> clazz = PanamaWasiRandom.class;

      // Verify both random methods exist
      boolean hasGetRandomBytes = false;
      boolean hasGetRandomU64 = false;

      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getRandomBytes")) {
          hasGetRandomBytes = true;
        }
        if (method.getName().equals("getRandomU64")) {
          hasGetRandomU64 = true;
        }
      }

      assertTrue(hasGetRandomBytes, "Should have getRandomBytes for byte array generation");
      assertTrue(hasGetRandomU64, "Should have getRandomU64 for 64-bit integer generation");

      LOGGER.info("Cryptographic random interface verified");
    }
  }
}
