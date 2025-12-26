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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentGarbageCollectionConfig} interface.
 *
 * <p>ComponentGarbageCollectionConfig provides configuration for component garbage collection.
 */
@DisplayName("ComponentGarbageCollectionConfig Tests")
class ComponentGarbageCollectionConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentGarbageCollectionConfig.class.getModifiers()),
          "ComponentGarbageCollectionConfig should be public");
      assertTrue(
          ComponentGarbageCollectionConfig.class.isInterface(),
          "ComponentGarbageCollectionConfig should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getGcThreshold method")
    void shouldHaveGetGcThresholdMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionConfig.class.getMethod("getGcThreshold");
      assertNotNull(method, "getGcThreshold method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getGcInterval method")
    void shouldHaveGetGcIntervalMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionConfig.class.getMethod("getGcInterval");
      assertNotNull(method, "getGcInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have isGcEnabled method")
    void shouldHaveIsGcEnabledMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionConfig.class.getMethod("isGcEnabled");
      assertNotNull(method, "isGcEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getGcStrategy method")
    void shouldHaveGetGcStrategyMethod() throws NoSuchMethodException {
      final Method method = ComponentGarbageCollectionConfig.class.getMethod("getGcStrategy");
      assertNotNull(method, "getGcStrategy method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Stub Implementation Tests")
  class StubImplementationTests {

    /** Stub implementation for testing interface contract. */
    private static class StubComponentGarbageCollectionConfig
        implements ComponentGarbageCollectionConfig {
      private final long gcThreshold;
      private final long gcInterval;
      private final boolean gcEnabled;
      private final String gcStrategy;

      StubComponentGarbageCollectionConfig(
          final long gcThreshold,
          final long gcInterval,
          final boolean gcEnabled,
          final String gcStrategy) {
        this.gcThreshold = gcThreshold;
        this.gcInterval = gcInterval;
        this.gcEnabled = gcEnabled;
        this.gcStrategy = gcStrategy;
      }

      @Override
      public long getGcThreshold() {
        return gcThreshold;
      }

      @Override
      public long getGcInterval() {
        return gcInterval;
      }

      @Override
      public boolean isGcEnabled() {
        return gcEnabled;
      }

      @Override
      public String getGcStrategy() {
        return gcStrategy;
      }
    }

    @Test
    @DisplayName("stub should return correct GC threshold")
    void stubShouldReturnCorrectGcThreshold() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(1024 * 1024L, 60000L, true, "incremental");

      assertEquals(1024 * 1024L, config.getGcThreshold(), "GC threshold should be 1MB");
    }

    @Test
    @DisplayName("stub should return correct GC interval")
    void stubShouldReturnCorrectGcInterval() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "mark-sweep");

      assertEquals(30000L, config.getGcInterval(), "GC interval should be 30 seconds");
    }

    @Test
    @DisplayName("stub should return correct GC enabled state")
    void stubShouldReturnCorrectGcEnabledState() {
      final ComponentGarbageCollectionConfig configEnabled =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "incremental");
      final ComponentGarbageCollectionConfig configDisabled =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, false, "incremental");

      assertTrue(configEnabled.isGcEnabled(), "GC should be enabled");
      assertFalse(configDisabled.isGcEnabled(), "GC should be disabled");
    }

    @Test
    @DisplayName("stub should return correct GC strategy")
    void stubShouldReturnCorrectGcStrategy() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "generational");

      assertEquals("generational", config.getGcStrategy(), "GC strategy should be generational");
    }

    @Test
    @DisplayName("stub should handle zero threshold")
    void stubShouldHandleZeroThreshold() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(0L, 30000L, true, "incremental");

      assertEquals(0L, config.getGcThreshold(), "GC threshold can be 0");
    }

    @Test
    @DisplayName("stub should handle zero interval")
    void stubShouldHandleZeroInterval() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(1024L, 0L, true, "incremental");

      assertEquals(0L, config.getGcInterval(), "GC interval can be 0");
    }

    @Test
    @DisplayName("stub should handle max long values")
    void stubShouldHandleMaxLongValues() {
      final ComponentGarbageCollectionConfig config =
          new StubComponentGarbageCollectionConfig(
              Long.MAX_VALUE, Long.MAX_VALUE, true, "incremental");

      assertEquals(Long.MAX_VALUE, config.getGcThreshold(), "Should handle max threshold");
      assertEquals(Long.MAX_VALUE, config.getGcInterval(), "Should handle max interval");
    }

    @Test
    @DisplayName("stub should handle different GC strategies")
    void stubShouldHandleDifferentGcStrategies() {
      final ComponentGarbageCollectionConfig incremental =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "incremental");
      final ComponentGarbageCollectionConfig markSweep =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "mark-sweep");
      final ComponentGarbageCollectionConfig generational =
          new StubComponentGarbageCollectionConfig(1024L, 30000L, true, "generational");

      assertEquals("incremental", incremental.getGcStrategy(), "Should be incremental");
      assertEquals("mark-sweep", markSweep.getGcStrategy(), "Should be mark-sweep");
      assertEquals("generational", generational.getGcStrategy(), "Should be generational");
    }
  }

  @Nested
  @DisplayName("Interface Completeness Tests")
  class InterfaceCompletenessTests {

    @Test
    @DisplayName("should have exactly 4 declared methods")
    void shouldHaveExactlyFourDeclaredMethods() {
      final var methods = ComponentGarbageCollectionConfig.class.getDeclaredMethods();
      assertEquals(4, methods.length, "Should have exactly 4 declared methods");
    }

    @Test
    @DisplayName("should have all required methods")
    void shouldHaveAllRequiredMethods() {
      final var methods = ComponentGarbageCollectionConfig.class.getMethods();
      final var methodNames =
          java.util.Arrays.stream(methods)
              .map(Method::getName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(methodNames.contains("getGcThreshold"), "Should have getGcThreshold");
      assertTrue(methodNames.contains("getGcInterval"), "Should have getGcInterval");
      assertTrue(methodNames.contains("isGcEnabled"), "Should have isGcEnabled");
      assertTrue(methodNames.contains("getGcStrategy"), "Should have getGcStrategy");
    }
  }
}
