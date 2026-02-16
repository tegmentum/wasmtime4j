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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

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

/** Direct integration tests for Panama WASI HTTP implementation. */
@DisplayName("Panama WASI HTTP Direct Tests")
public class PanamaWasiHttpDirectTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiHttpDirectTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI HTTP tests");
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
  @DisplayName("PanamaWasiHttpConfig Tests")
  class HttpConfigTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiHttpConfig class structure");

      final Class<?> clazz = PanamaWasiHttpConfig.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have host filtering methods")
    void shouldHaveHostFilteringMethods() {
      LOGGER.info("Testing host filtering methods");

      final Class<?> clazz = PanamaWasiHttpConfig.class;
      final String[] expectedMethods = {"getAllowedHosts", "getBlockedHosts"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have timeout configuration methods")
    void shouldHaveTimeoutConfigurationMethods() {
      LOGGER.info("Testing timeout configuration methods");

      final Class<?> clazz = PanamaWasiHttpConfig.class;
      final String[] expectedMethods = {"getConnectTimeout", "getReadTimeout", "getWriteTimeout"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have connection configuration methods")
    void shouldHaveConnectionConfigurationMethods() {
      LOGGER.info("Testing connection configuration methods");

      final Class<?> clazz = PanamaWasiHttpConfig.class;
      final String[] expectedMethods = {
        "getMaxConnections",
        "getMaxConnectionsPerHost",
        "isConnectionPoolingEnabled",
        "isHttp2Enabled"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasiHttpConfigBuilder Tests")
  class HttpConfigBuilderTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiHttpConfigBuilder class structure");

      final Class<?> clazz = PanamaWasiHttpConfigBuilder.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have fluent builder methods")
    void shouldHaveFluentBuilderMethods() {
      LOGGER.info("Testing fluent builder methods");

      final Class<?> clazz = PanamaWasiHttpConfigBuilder.class;
      final String[] expectedMethods = {
        "allowHost",
        "allowHosts",
        "allowAllHosts",
        "blockHost",
        "blockHosts",
        "withConnectTimeout",
        "withReadTimeout",
        "withWriteTimeout",
        "withMaxConnections",
        "build"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found builder method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }
  }

  @Nested
  @DisplayName("PanamaWasiHttpContext Tests")
  class HttpContextTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiHttpContext class structure");

      final Class<?> clazz = PanamaWasiHttpContext.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have context management methods")
    void shouldHaveContextManagementMethods() {
      LOGGER.info("Testing context management methods");

      final Class<?> clazz = PanamaWasiHttpContext.class;
      final String[] expectedMethods = {"getConfig", "isValid", "isHostAllowed", "close"};

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
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
          AutoCloseable.class.isAssignableFrom(PanamaWasiHttpContext.class),
          "Should implement AutoCloseable");
    }
  }
}
