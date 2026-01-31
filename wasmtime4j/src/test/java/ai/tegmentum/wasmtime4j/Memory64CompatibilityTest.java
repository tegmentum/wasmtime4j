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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Memory64Compatibility} class.
 *
 * <p>Memory64Compatibility is a utility class with static methods for compatibility mode toggling,
 * migration warnings, memory wrapping, requirements analysis, migration planning, and validation.
 */
@DisplayName("Memory64Compatibility Tests")
class Memory64CompatibilityTest {

  /** Save original state to restore after tests. */
  private boolean originalCompatibilityMode;

  private boolean originalMigrationWarnings;

  @BeforeEach
  void saveOriginalState() {
    originalCompatibilityMode = Memory64Compatibility.isCompatibilityModeEnabled();
    originalMigrationWarnings = Memory64Compatibility.isMigrationWarningsEnabled();
  }

  @AfterEach
  void restoreOriginalState() {
    Memory64Compatibility.setCompatibilityModeEnabled(originalCompatibilityMode);
    Memory64Compatibility.setMigrationWarningsEnabled(originalMigrationWarnings);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final utility class")
    void shouldBePublicFinalUtilityClass() {
      assertTrue(
          Modifier.isPublic(Memory64Compatibility.class.getModifiers()),
          "Memory64Compatibility should be public");
      assertTrue(
          Modifier.isFinal(Memory64Compatibility.class.getModifiers()),
          "Memory64Compatibility should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          Memory64Compatibility.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private for utility class");
    }
  }

  @Nested
  @DisplayName("Compatibility Mode Tests")
  class CompatibilityModeTests {

    @Test
    @DisplayName("should enable compatibility mode")
    void shouldEnableCompatibilityMode() {
      Memory64Compatibility.setCompatibilityModeEnabled(true);

      assertTrue(
          Memory64Compatibility.isCompatibilityModeEnabled(),
          "Compatibility mode should be enabled");
    }

    @Test
    @DisplayName("should disable compatibility mode")
    void shouldDisableCompatibilityMode() {
      Memory64Compatibility.setCompatibilityModeEnabled(false);

      assertFalse(
          Memory64Compatibility.isCompatibilityModeEnabled(),
          "Compatibility mode should be disabled");
    }

    @Test
    @DisplayName("should toggle compatibility mode back and forth")
    void shouldToggleCompatibilityMode() {
      Memory64Compatibility.setCompatibilityModeEnabled(true);
      assertTrue(
          Memory64Compatibility.isCompatibilityModeEnabled(),
          "Should be enabled after setting true");

      Memory64Compatibility.setCompatibilityModeEnabled(false);
      assertFalse(
          Memory64Compatibility.isCompatibilityModeEnabled(),
          "Should be disabled after setting false");

      Memory64Compatibility.setCompatibilityModeEnabled(true);
      assertTrue(
          Memory64Compatibility.isCompatibilityModeEnabled(),
          "Should be enabled again after re-setting true");
    }
  }

  @Nested
  @DisplayName("Migration Warnings Tests")
  class MigrationWarningsTests {

    @Test
    @DisplayName("should enable migration warnings")
    void shouldEnableMigrationWarnings() {
      Memory64Compatibility.setMigrationWarningsEnabled(true);

      assertTrue(
          Memory64Compatibility.isMigrationWarningsEnabled(),
          "Migration warnings should be enabled");
    }

    @Test
    @DisplayName("should disable migration warnings")
    void shouldDisableMigrationWarnings() {
      Memory64Compatibility.setMigrationWarningsEnabled(false);

      assertFalse(
          Memory64Compatibility.isMigrationWarningsEnabled(),
          "Migration warnings should be disabled");
    }
  }

  @Nested
  @DisplayName("wrapForCompatibility Tests")
  class WrapForCompatibilityTests {

    @Test
    @DisplayName("should reject null memory")
    void shouldRejectNullMemory() {
      assertThrows(
          IllegalArgumentException.class,
          () -> Memory64Compatibility.wrapForCompatibility(null),
          "Null memory should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should wrap valid memory and return WasmMemory")
    void shouldWrapValidMemory() {
      final WasmMemory mockMemory = createMockWasmMemory();
      final WasmMemory wrapped = Memory64Compatibility.wrapForCompatibility(mockMemory);

      assertNotNull(wrapped, "Wrapped memory should not be null");
    }
  }

  @Nested
  @DisplayName("analyzeMemoryRequirements Tests")
  class AnalyzeMemoryRequirementsTests {

    @Test
    @DisplayName("should analyze small memory requirements as 32-bit compatible")
    void shouldAnalyzeSmallRequirementsAs32Bit() {
      final Memory64Compatibility.MemoryRecommendation result =
          Memory64Compatibility.analyzeMemoryRequirements(10L, 1000L, null);

      assertNotNull(result, "Recommendation should not be null");
    }

    @Test
    @DisplayName("should analyze with usage pattern")
    void shouldAnalyzeWithUsagePattern() {
      final Memory64Compatibility.MemoryUsagePattern pattern =
          new Memory64Compatibility.MemoryUsagePattern();
      final Memory64Compatibility.MemoryRecommendation result =
          Memory64Compatibility.analyzeMemoryRequirements(10L, 1000L, pattern);

      assertNotNull(result, "Recommendation with usage pattern should not be null");
    }

    @Test
    @DisplayName("should analyze requirements exceeding 32-bit limits")
    void shouldAnalyzeRequirementsExceeding32BitLimits() {
      final Memory64Compatibility.MemoryRecommendation result =
          Memory64Compatibility.analyzeMemoryRequirements(10L, 100000L, null);

      assertNotNull(result, "Recommendation for large memory should not be null");
    }
  }

  @Nested
  @DisplayName("createMigrationPlan Tests")
  class CreateMigrationPlanTests {

    @Test
    @DisplayName("should create plan for 32-bit to 64-bit migration")
    void shouldCreatePlanFor32To64Migration() {
      final Memory64Config currentConfig = Memory64Config.builder(10)
          .addressing32Bit()
          .maximumPages(1000L)
          .build();
      final Memory64Compatibility.MemoryRequirements requirements =
          new Memory64Compatibility.MemoryRequirements() {
            @Override
            public boolean requires64BitAddressing() {
              return true;
            }
          };

      final Memory64Compatibility.MigrationPlan plan =
          Memory64Compatibility.createMigrationPlan(currentConfig, requirements);

      assertNotNull(plan, "Migration plan should not be null");
    }

    @Test
    @DisplayName("should create plan when already compatible")
    void shouldCreatePlanWhenAlreadyCompatible() {
      final Memory64Config currentConfig = Memory64Config.builder(10)
          .addressing32Bit()
          .maximumPages(1000L)
          .build();
      final Memory64Compatibility.MemoryRequirements requirements =
          new Memory64Compatibility.MemoryRequirements();

      final Memory64Compatibility.MigrationPlan plan =
          Memory64Compatibility.createMigrationPlan(currentConfig, requirements);

      assertNotNull(plan, "Plan should not be null when already compatible");
    }
  }

  @Nested
  @DisplayName("validateCompatibility Tests")
  class ValidateCompatibilityTests {

    @Test
    @DisplayName("should validate compatible config with default RuntimeInfo")
    void shouldValidateCompatibleConfig() {
      final Memory64Config config = Memory64Config.builder(10)
          .addressing32Bit()
          .maximumPages(1000L)
          .build();
      final Memory64Compatibility.RuntimeInfo runtimeInfo =
          new Memory64Compatibility.RuntimeInfo();

      final Memory64Compatibility.CompatibilityValidationResult result =
          Memory64Compatibility.validateCompatibility(config, runtimeInfo);

      assertNotNull(result, "Validation result should not be null");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("MemoryUsagePattern should have default values")
    void memoryUsagePatternShouldHaveDefaults() {
      final Memory64Compatibility.MemoryUsagePattern pattern =
          new Memory64Compatibility.MemoryUsagePattern();

      assertEquals(1024.0, pattern.getAverageOperationSize(),
          "Default averageOperationSize should be 1024.0");
      assertEquals(0.01, pattern.getGrowthFrequency(),
          "Default growthFrequency should be 0.01");
      assertFalse(pattern.hasLargeOffsetAccess(), "Default hasLargeOffsetAccess should be false");
      assertFalse(pattern.hasConcurrentAccess(), "Default hasConcurrentAccess should be false");
      assertFalse(
          pattern.needsLargeAddressSpace(),
          "Default needsLargeAddressSpace should be false");
    }

    @Test
    @DisplayName("MigrationEffort should have all expected values")
    void migrationEffortShouldHaveExpectedValues() {
      final Memory64Compatibility.MigrationEffort[] values =
          Memory64Compatibility.MigrationEffort.values();

      assertEquals(4, values.length, "MigrationEffort should have 4 values");
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("NONE"),
          "NONE should exist");
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("LOW"),
          "LOW should exist");
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("MODERATE"),
          "MODERATE should exist");
      assertNotNull(
          Memory64Compatibility.MigrationEffort.valueOf("HIGH"),
          "HIGH should exist");
    }

    @Test
    @DisplayName("OptimizationImpact should have all expected values")
    void optimizationImpactShouldHaveExpectedValues() {
      final Memory64Compatibility.OptimizationImpact[] values =
          Memory64Compatibility.OptimizationImpact.values();

      assertEquals(3, values.length, "OptimizationImpact should have 3 values");
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("LOW"),
          "LOW should exist");
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("MEDIUM"),
          "MEDIUM should exist");
      assertNotNull(
          Memory64Compatibility.OptimizationImpact.valueOf("HIGH"),
          "HIGH should exist");
    }
  }

  private void assertEquals(
      final double expected, final double actual, final String message) {
    org.junit.jupiter.api.Assertions.assertEquals(expected, actual, 0.001, message);
  }

  /** Creates a minimal WasmMemory proxy for testing wrapping. */
  private WasmMemory createMockWasmMemory() {
    return (WasmMemory) java.lang.reflect.Proxy.newProxyInstance(
        WasmMemory.class.getClassLoader(),
        new Class<?>[] {WasmMemory.class},
        (proxy, method, args) -> {
          if ("supports64BitAddressing".equals(method.getName())) {
            return false;
          }
          if ("getSize".equals(method.getName())) {
            return 0;
          }
          if ("getSize64".equals(method.getName())) {
            return 0L;
          }
          if ("getMaxSize".equals(method.getName())) {
            return 0;
          }
          if ("getMaxSize64".equals(method.getName())) {
            return 0L;
          }
          if ("getSizeInBytes64".equals(method.getName())) {
            return 0L;
          }
          if ("getMaxSizeInBytes64".equals(method.getName())) {
            return 0L;
          }
          if ("isShared".equals(method.getName())) {
            return false;
          }
          if ("grow".equals(method.getName())) {
            return 0;
          }
          if ("grow64".equals(method.getName())) {
            return 0L;
          }
          if ("readByte".equals(method.getName())) {
            return (byte) 0;
          }
          if ("readByte64".equals(method.getName())) {
            return (byte) 0;
          }
          if ("getMemoryType".equals(method.getName())) {
            return null;
          }
          if ("getBuffer".equals(method.getName())) {
            return null;
          }
          return null;
        });
  }
}
