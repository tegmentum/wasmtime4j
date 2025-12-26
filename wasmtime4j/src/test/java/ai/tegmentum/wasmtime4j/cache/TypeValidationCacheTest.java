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

package ai.tegmentum.wasmtime4j.cache;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeValidationCache} class.
 *
 * <p>TypeValidationCache provides high-performance caching for WebAssembly type validation results.
 */
@DisplayName("TypeValidationCache Tests")
class TypeValidationCacheTest {

  private TypeValidationCache cache;

  @BeforeEach
  void setUp() {
    cache = TypeValidationCache.getInstance();
    cache.clear();
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance")
    void shouldReturnSameInstance() {
      final TypeValidationCache instance1 = TypeValidationCache.getInstance();
      final TypeValidationCache instance2 = TypeValidationCache.getInstance();
      assertEquals(instance1, instance2, "Should return same instance");
    }

    @Test
    @DisplayName("getInstance should return non-null")
    void getInstanceShouldReturnNonNull() {
      assertNotNull(TypeValidationCache.getInstance(), "Instance should not be null");
    }
  }

  @Nested
  @DisplayName("Validation Result Cache Tests")
  class ValidationResultCacheTests {

    @Test
    @DisplayName("should put and get validation result")
    void shouldPutAndGetValidationResult() {
      final String[] params = {"i32", "i64"};
      final String[] returns = {"f64"};
      final TypeValidationCache.TypeInfo typeInfo =
          new TypeValidationCache.TypeInfo(params, returns, false, "test", null);
      final TypeValidationCache.ValidationResult result =
          new TypeValidationCache.ValidationResult(true, null, null, typeInfo, 10);

      cache.putValidationResult(params, returns, "test", result, 10);
      final TypeValidationCache.ValidationResult retrieved =
          cache.getValidationResult(params, returns, "test");

      assertNotNull(retrieved, "Should retrieve validation result");
      assertTrue(retrieved.isValid(), "Result should be valid");
    }

    @Test
    @DisplayName("should return null for non-existent validation result")
    void shouldReturnNullForNonExistentValidationResult() {
      final TypeValidationCache.ValidationResult result =
          cache.getValidationResult(new String[] {"unknown"}, new String[] {"type"}, "context");
      assertNull(result, "Should return null for non-existent result");
    }

    @Test
    @DisplayName("should compute validation if absent")
    void shouldComputeValidationIfAbsent() {
      final String[] params = {"i32"};
      final String[] returns = {"i32"};
      final TypeValidationCache.ValidationResult computed =
          cache.computeValidationIfAbsent(
              params,
              returns,
              "compute_test",
              () -> {
                final TypeValidationCache.TypeInfo info =
                    new TypeValidationCache.TypeInfo(params, returns, false, "compute_test", null);
                return new TypeValidationCache.ValidationResult(true, null, null, info, 5);
              });

      assertNotNull(computed, "Should compute and return result");
      assertTrue(computed.isValid(), "Computed result should be valid");

      // Verify it was cached
      final TypeValidationCache.ValidationResult cached =
          cache.getValidationResult(params, returns, "compute_test");
      assertNotNull(cached, "Result should be cached");
    }
  }

  @Nested
  @DisplayName("Compatibility Result Cache Tests")
  class CompatibilityResultCacheTests {

    @Test
    @DisplayName("should put and get compatibility result")
    void shouldPutAndGetCompatibilityResult() {
      final String[] source = {"i32", "i64"};
      final String[] target = {"i32", "i64"};
      final TypeValidationCache.CompatibilityResult result =
          new TypeValidationCache.CompatibilityResult(true, "Types match", null);

      cache.putCompatibilityResult(source, target, "compat_test", result);
      final TypeValidationCache.CompatibilityResult retrieved =
          cache.getCompatibilityResult(source, target, "compat_test");

      assertNotNull(retrieved, "Should retrieve compatibility result");
      assertTrue(retrieved.isCompatible(), "Result should be compatible");
    }

    @Test
    @DisplayName("should return null for non-existent compatibility result")
    void shouldReturnNullForNonExistentCompatibilityResult() {
      final TypeValidationCache.CompatibilityResult result =
          cache.getCompatibilityResult(new String[] {"a"}, new String[] {"b"}, "context");
      assertNull(result, "Should return null for non-existent result");
    }

    @Test
    @DisplayName("should compute compatibility if absent")
    void shouldComputeCompatibilityIfAbsent() {
      final String[] source = {"f32"};
      final String[] target = {"f32"};
      final TypeValidationCache.CompatibilityResult computed =
          cache.computeCompatibilityIfAbsent(
              source,
              target,
              "compute_compat",
              () -> new TypeValidationCache.CompatibilityResult(true, "Exact match", null));

      assertNotNull(computed, "Should compute and return result");
      assertTrue(computed.isCompatible(), "Computed result should be compatible");
    }
  }

  @Nested
  @DisplayName("Clear Tests")
  class ClearTests {

    @Test
    @DisplayName("should clear all cached entries")
    void shouldClearAllCachedEntries() {
      // Add validation result
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32"}, new String[] {"i32"}, false, "test", null);
      cache.putValidationResult(
          new String[] {"i32"},
          new String[] {"i32"},
          "test",
          new TypeValidationCache.ValidationResult(true, null, null, info, 5),
          5);

      // Add compatibility result
      cache.putCompatibilityResult(
          new String[] {"i32"},
          new String[] {"i32"},
          "compat",
          new TypeValidationCache.CompatibilityResult(true, "match", null));

      // Clear
      cache.clear();

      // Verify cleared
      assertNull(
          cache.getValidationResult(new String[] {"i32"}, new String[] {"i32"}, "test"),
          "Validation result should be cleared");
      assertNull(
          cache.getCompatibilityResult(new String[] {"i32"}, new String[] {"i32"}, "compat"),
          "Compatibility result should be cleared");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return non-null statistics")
    void shouldReturnNonNullStatistics() {
      assertNotNull(cache.getStatistics(), "Statistics should not be null");
    }

    @Test
    @DisplayName("should return hit rate")
    void shouldReturnHitRate() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32"}, new String[] {"i32"}, false, "test", null);
      cache.putValidationResult(
          new String[] {"i32"},
          new String[] {"i32"},
          "test",
          new TypeValidationCache.ValidationResult(true, null, null, info, 5),
          5);

      cache.getValidationResult(new String[] {"i32"}, new String[] {"i32"}, "test"); // Hit
      cache.getValidationResult(new String[] {"missing"}, new String[] {"type"}, "ctx"); // Miss

      final double hitRate = cache.getHitRate();
      assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");
    }

    @Test
    @DisplayName("should track validation time saved")
    void shouldTrackValidationTimeSaved() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32"}, new String[] {"i32"}, false, "test", null);
      cache.putValidationResult(
          new String[] {"i32"},
          new String[] {"i32"},
          "test",
          new TypeValidationCache.ValidationResult(true, null, null, info, 100),
          100);

      cache.getValidationResult(new String[] {"i32"}, new String[] {"i32"}, "test"); // Hit

      final long timeSaved = cache.getTotalValidationTimeSaved();
      assertTrue(timeSaved >= 0, "Time saved should be non-negative");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should be enabled by default")
    void shouldBeEnabledByDefault() {
      assertTrue(TypeValidationCache.isEnabled(), "Cache should be enabled by default");
    }

    @Test
    @DisplayName("should allow enabling and disabling")
    void shouldAllowEnablingAndDisabling() {
      final boolean originalState = TypeValidationCache.isEnabled();
      try {
        TypeValidationCache.setEnabled(false);
        assertFalse(TypeValidationCache.isEnabled(), "Cache should be disabled");
        TypeValidationCache.setEnabled(true);
        assertTrue(TypeValidationCache.isEnabled(), "Cache should be enabled");
      } finally {
        TypeValidationCache.setEnabled(originalState);
      }
    }
  }

  @Nested
  @DisplayName("Maintenance Tests")
  class MaintenanceTests {

    @Test
    @DisplayName("should perform maintenance without error")
    void shouldPerformMaintenanceWithoutError() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32"}, new String[] {"i32"}, false, "test", null);
      cache.putValidationResult(
          new String[] {"i32"},
          new String[] {"i32"},
          "test",
          new TypeValidationCache.ValidationResult(true, null, null, info, 5),
          5);

      // Should not throw
      cache.performMaintenance();
    }
  }

  @Nested
  @DisplayName("ValidationResult Tests")
  class ValidationResultTests {

    @Test
    @DisplayName("should create valid ValidationResult")
    void shouldCreateValidValidationResult() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32"}, new String[] {"f64"}, false, "module", null);
      final TypeValidationCache.ValidationResult result =
          new TypeValidationCache.ValidationResult(true, null, new String[] {"warning"}, info, 50);

      assertTrue(result.isValid(), "Should be valid");
      assertEquals(0, result.getErrors().length, "Should have no errors");
      assertEquals(1, result.getWarnings().length, "Should have 1 warning");
      assertNotNull(result.getTypeInfo(), "Type info should not be null");
      assertEquals(50, result.getValidationTimeMs(), "Validation time should match");
    }

    @Test
    @DisplayName("should create invalid ValidationResult")
    void shouldCreateInvalidValidationResult() {
      final TypeValidationCache.ValidationResult result =
          new TypeValidationCache.ValidationResult(
              false, new String[] {"type mismatch"}, null, null, 100);

      assertFalse(result.isValid(), "Should be invalid");
      assertEquals(1, result.getErrors().length, "Should have 1 error");
      assertEquals(0, result.getWarnings().length, "Should have no warnings");
    }
  }

  @Nested
  @DisplayName("TypeInfo Tests")
  class TypeInfoTests {

    @Test
    @DisplayName("should create TypeInfo with all fields")
    void shouldCreateTypeInfoWithAllFields() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(
              new String[] {"i32", "i64"},
              new String[] {"f32"},
              true,
              "host_module",
              new String[] {"non_null"});

      assertArrayEquals(
          new String[] {"i32", "i64"}, info.getParameterTypes(), "Params should match");
      assertArrayEquals(new String[] {"f32"}, info.getReturnTypes(), "Returns should match");
      assertTrue(info.isHostFunction(), "Should be host function");
      assertEquals("host_module", info.getModuleContext(), "Module context should match");
      assertEquals(1, info.getConstraints().length, "Should have 1 constraint");
    }

    @Test
    @DisplayName("should handle null arrays in TypeInfo")
    void shouldHandleNullArraysInTypeInfo() {
      final TypeValidationCache.TypeInfo info =
          new TypeValidationCache.TypeInfo(null, null, false, "ctx", null);

      assertArrayEquals(new String[0], info.getParameterTypes(), "Params should be empty array");
      assertArrayEquals(new String[0], info.getReturnTypes(), "Returns should be empty array");
      assertArrayEquals(new String[0], info.getConstraints(), "Constraints should be empty array");
    }
  }

  @Nested
  @DisplayName("CompatibilityResult Tests")
  class CompatibilityResultTests {

    @Test
    @DisplayName("should create compatible result")
    void shouldCreateCompatibleResult() {
      final TypeValidationCache.CompatibilityResult result =
          new TypeValidationCache.CompatibilityResult(
              true, "Types are compatible", new String[] {"Consider using stricter types"});

      assertTrue(result.isCompatible(), "Should be compatible");
      assertEquals("Types are compatible", result.getReason(), "Reason should match");
      assertEquals(1, result.getSuggestions().length, "Should have 1 suggestion");
    }

    @Test
    @DisplayName("should create incompatible result")
    void shouldCreateIncompatibleResult() {
      final TypeValidationCache.CompatibilityResult result =
          new TypeValidationCache.CompatibilityResult(
              false, "Type mismatch: expected i32, got f64", null);

      assertFalse(result.isCompatible(), "Should not be compatible");
      assertTrue(result.getReason().contains("mismatch"), "Reason should mention mismatch");
      assertEquals(0, result.getSuggestions().length, "Should have no suggestions");
    }
  }
}
