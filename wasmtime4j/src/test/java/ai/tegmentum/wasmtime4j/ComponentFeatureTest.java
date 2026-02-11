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

import ai.tegmentum.wasmtime4j.component.Component;
import ai.tegmentum.wasmtime4j.component.ComponentCapability;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibility;
import ai.tegmentum.wasmtime4j.component.ComponentCompatibilityResult;
import ai.tegmentum.wasmtime4j.component.ComponentDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentDependencyGraph;
import ai.tegmentum.wasmtime4j.component.ComponentEngine;
import ai.tegmentum.wasmtime4j.component.ComponentEngineConfig;
import ai.tegmentum.wasmtime4j.component.ComponentEngineDebugInfo;
import ai.tegmentum.wasmtime4j.component.ComponentFeature;
import ai.tegmentum.wasmtime4j.component.ComponentFunc;
import ai.tegmentum.wasmtime4j.component.ComponentFunction;
import ai.tegmentum.wasmtime4j.component.ComponentHostFunction;
import ai.tegmentum.wasmtime4j.component.ComponentId;
import ai.tegmentum.wasmtime4j.component.ComponentImportValidation;
import ai.tegmentum.wasmtime4j.component.ComponentInstance;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceConfig;
import ai.tegmentum.wasmtime4j.component.ComponentInstanceState;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleManager;
import ai.tegmentum.wasmtime4j.component.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.component.ComponentLinker;
import ai.tegmentum.wasmtime4j.component.ComponentLinkInfo;
import ai.tegmentum.wasmtime4j.component.ComponentLoadConfig;
import ai.tegmentum.wasmtime4j.component.ComponentMetadata;
import ai.tegmentum.wasmtime4j.component.ComponentRegistry;
import ai.tegmentum.wasmtime4j.component.ComponentRegistryStatistics;
import ai.tegmentum.wasmtime4j.component.ComponentResourceDefinition;
import ai.tegmentum.wasmtime4j.component.ComponentResourceHandle;
import ai.tegmentum.wasmtime4j.component.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.component.ComponentResult;
import ai.tegmentum.wasmtime4j.component.ComponentSearchCriteria;
import ai.tegmentum.wasmtime4j.component.ComponentSpecification;
import ai.tegmentum.wasmtime4j.component.ComponentStateTransitionConfig;
import ai.tegmentum.wasmtime4j.component.ComponentType;
import ai.tegmentum.wasmtime4j.component.ComponentTypeDescriptor;
import ai.tegmentum.wasmtime4j.component.ComponentTypedFunc;
import ai.tegmentum.wasmtime4j.component.ComponentVal;
import ai.tegmentum.wasmtime4j.component.ComponentValFactory;
import ai.tegmentum.wasmtime4j.component.ComponentValidationConfig;
import ai.tegmentum.wasmtime4j.component.ComponentValidationResult;
import ai.tegmentum.wasmtime4j.component.ComponentVariant;
import ai.tegmentum.wasmtime4j.component.ComponentVersion;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ComponentFeature enum.
 *
 * <p>This test class verifies the enum structure, values, and functionality for ComponentFeature
 * using reflection-based testing.
 */
@DisplayName("ComponentFeature Tests")
class ComponentFeatureTest {

  // ========================================================================
  // Enum Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("ComponentFeature should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ComponentFeature.class.isEnum(), "ComponentFeature should be an enum");
    }

    @Test
    @DisplayName("ComponentFeature should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ComponentFeature.class.getModifiers()),
          "ComponentFeature should be public");
    }

    @Test
    @DisplayName("ComponentFeature should be a final enum (implicitly)")
    void shouldBeFinal() {
      // Enums are implicitly final
      assertTrue(
          Modifier.isFinal(ComponentFeature.class.getModifiers()),
          "ComponentFeature should be final");
    }

    @Test
    @DisplayName("ComponentFeature should have exactly 15 values")
    void shouldHaveExactlyFifteenValues() {
      ComponentFeature[] values = ComponentFeature.values();
      assertEquals(15, values.length, "ComponentFeature should have exactly 15 values");
    }
  }

  // ========================================================================
  // Enum Values Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("COMPONENT_MODEL should exist")
    void shouldHaveComponentModel() {
      assertNotNull(ComponentFeature.valueOf("COMPONENT_MODEL"), "COMPONENT_MODEL should exist");
    }

    @Test
    @DisplayName("ORCHESTRATION should exist")
    void shouldHaveOrchestration() {
      assertNotNull(ComponentFeature.valueOf("ORCHESTRATION"), "ORCHESTRATION should exist");
    }

    @Test
    @DisplayName("HOT_SWAPPING should exist")
    void shouldHaveHotSwapping() {
      assertNotNull(ComponentFeature.valueOf("HOT_SWAPPING"), "HOT_SWAPPING should exist");
    }

    @Test
    @DisplayName("DISTRIBUTED_COMPONENTS should exist")
    void shouldHaveDistributedComponents() {
      assertNotNull(
          ComponentFeature.valueOf("DISTRIBUTED_COMPONENTS"),
          "DISTRIBUTED_COMPONENTS should exist");
    }

    @Test
    @DisplayName("CLUSTERING should exist")
    void shouldHaveClustering() {
      assertNotNull(ComponentFeature.valueOf("CLUSTERING"), "CLUSTERING should exist");
    }

    @Test
    @DisplayName("AUDIT_LOGGING should exist")
    void shouldHaveAuditLogging() {
      assertNotNull(ComponentFeature.valueOf("AUDIT_LOGGING"), "AUDIT_LOGGING should exist");
    }

    @Test
    @DisplayName("SECURITY_POLICIES should exist")
    void shouldHaveSecurityPolicies() {
      assertNotNull(
          ComponentFeature.valueOf("SECURITY_POLICIES"), "SECURITY_POLICIES should exist");
    }

    @Test
    @DisplayName("MONITORING should exist")
    void shouldHaveMonitoring() {
      assertNotNull(ComponentFeature.valueOf("MONITORING"), "MONITORING should exist");
    }

    @Test
    @DisplayName("DIAGNOSTICS should exist")
    void shouldHaveDiagnostics() {
      assertNotNull(ComponentFeature.valueOf("DIAGNOSTICS"), "DIAGNOSTICS should exist");
    }

    @Test
    @DisplayName("WIT_COMPATIBILITY should exist")
    void shouldHaveWitCompatibility() {
      assertNotNull(
          ComponentFeature.valueOf("WIT_COMPATIBILITY"), "WIT_COMPATIBILITY should exist");
    }

    @Test
    @DisplayName("RESOURCE_OPTIMIZATION should exist")
    void shouldHaveResourceOptimization() {
      assertNotNull(
          ComponentFeature.valueOf("RESOURCE_OPTIMIZATION"), "RESOURCE_OPTIMIZATION should exist");
    }

    @Test
    @DisplayName("AUTO_SCALING should exist")
    void shouldHaveAutoScaling() {
      assertNotNull(ComponentFeature.valueOf("AUTO_SCALING"), "AUTO_SCALING should exist");
    }

    @Test
    @DisplayName("BACKUP_RESTORE should exist")
    void shouldHaveBackupRestore() {
      assertNotNull(ComponentFeature.valueOf("BACKUP_RESTORE"), "BACKUP_RESTORE should exist");
    }

    @Test
    @DisplayName("LOAD_BALANCING should exist")
    void shouldHaveLoadBalancing() {
      assertNotNull(ComponentFeature.valueOf("LOAD_BALANCING"), "LOAD_BALANCING should exist");
    }

    @Test
    @DisplayName("CONFIG_MANAGEMENT should exist")
    void shouldHaveConfigManagement() {
      assertNotNull(
          ComponentFeature.valueOf("CONFIG_MANAGEMENT"), "CONFIG_MANAGEMENT should exist");
    }

    @Test
    @DisplayName("All enum values should have unique names")
    void allValuesShouldHaveUniqueNames() {
      ComponentFeature[] values = ComponentFeature.values();
      Set<String> names = new HashSet<>();
      for (ComponentFeature feature : values) {
        names.add(feature.name());
      }
      assertEquals(values.length, names.size(), "All enum values should have unique names");
    }

    @Test
    @DisplayName("All enum values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      ComponentFeature[] values = ComponentFeature.values();
      Set<Integer> ordinals = new HashSet<>();
      for (ComponentFeature feature : values) {
        ordinals.add(feature.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All enum values should have unique ordinals");
    }
  }

  // ========================================================================
  // Enum Functionality Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Functionality Tests")
  class EnumFunctionalityTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllConstants() {
      ComponentFeature[] values = ComponentFeature.values();
      assertNotNull(values, "values() should not return null");
      assertEquals(15, values.length, "values() should return 15 constants");
    }

    @Test
    @DisplayName("valueOf() should return correct enum for valid name")
    void valueOfShouldReturnCorrectEnum() {
      assertEquals(
          ComponentFeature.COMPONENT_MODEL,
          ComponentFeature.valueOf("COMPONENT_MODEL"),
          "valueOf should return correct enum");
      assertEquals(
          ComponentFeature.MONITORING,
          ComponentFeature.valueOf("MONITORING"),
          "valueOf should return correct enum");
    }

    @Test
    @DisplayName("valueOf() should throw IllegalArgumentException for invalid name")
    void valueOfShouldThrowForInvalidName() {
      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalArgumentException.class,
          () -> ComponentFeature.valueOf("INVALID_FEATURE"),
          "valueOf should throw for invalid name");
    }

    @Test
    @DisplayName("name() should return the enum constant name")
    void nameShouldReturnConstantName() {
      assertEquals(
          "COMPONENT_MODEL",
          ComponentFeature.COMPONENT_MODEL.name(),
          "name() should return COMPONENT_MODEL");
      assertEquals(
          "MONITORING", ComponentFeature.MONITORING.name(), "name() should return MONITORING");
    }

    @Test
    @DisplayName("ordinal() should return sequential values starting from 0")
    void ordinalShouldReturnSequentialValues() {
      ComponentFeature[] values = ComponentFeature.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should match index");
      }
    }

    @Test
    @DisplayName("toString() should return the enum name")
    void toStringShouldReturnEnumName() {
      assertEquals(
          "COMPONENT_MODEL",
          ComponentFeature.COMPONENT_MODEL.toString(),
          "toString should return enum name");
    }

    @Test
    @DisplayName("Enum values should be comparable")
    void enumValuesShouldBeComparable() {
      ComponentFeature first = ComponentFeature.values()[0];
      ComponentFeature last = ComponentFeature.values()[ComponentFeature.values().length - 1];

      assertTrue(first.compareTo(last) < 0, "First should come before last");
      assertTrue(last.compareTo(first) > 0, "Last should come after first");
      assertEquals(0, first.compareTo(first), "Same should be equal");
    }
  }

  // ========================================================================
  // Category Tests
  // ========================================================================

  @Nested
  @DisplayName("Category Tests")
  class CategoryTests {

    @Test
    @DisplayName("Core features should be present")
    void coreFeaturesShouldBePresent() {
      Set<String> coreFeatures =
          Set.of("COMPONENT_MODEL", "ORCHESTRATION", "HOT_SWAPPING", "WIT_COMPATIBILITY");
      Set<String> actualFeatures = new HashSet<>();
      for (ComponentFeature feature : ComponentFeature.values()) {
        actualFeatures.add(feature.name());
      }

      assertTrue(actualFeatures.containsAll(coreFeatures), "All core features should be present");
    }

    @Test
    @DisplayName("Distributed features should be present")
    void distributedFeaturesShouldBePresent() {
      Set<String> distributedFeatures =
          Set.of("DISTRIBUTED_COMPONENTS", "CLUSTERING", "LOAD_BALANCING");
      Set<String> actualFeatures = new HashSet<>();
      for (ComponentFeature feature : ComponentFeature.values()) {
        actualFeatures.add(feature.name());
      }

      assertTrue(
          actualFeatures.containsAll(distributedFeatures),
          "All distributed features should be present");
    }

    @Test
    @DisplayName("Management features should be present")
    void managementFeaturesShouldBePresent() {
      Set<String> managementFeatures =
          Set.of(
              "AUDIT_LOGGING",
              "SECURITY_POLICIES",
              "MONITORING",
              "DIAGNOSTICS",
              "CONFIG_MANAGEMENT");
      Set<String> actualFeatures = new HashSet<>();
      for (ComponentFeature feature : ComponentFeature.values()) {
        actualFeatures.add(feature.name());
      }

      assertTrue(
          actualFeatures.containsAll(managementFeatures),
          "All management features should be present");
    }

    @Test
    @DisplayName("Optimization features should be present")
    void optimizationFeaturesShouldBePresent() {
      Set<String> optimizationFeatures =
          Set.of("RESOURCE_OPTIMIZATION", "AUTO_SCALING", "BACKUP_RESTORE");
      Set<String> actualFeatures = new HashSet<>();
      for (ComponentFeature feature : ComponentFeature.values()) {
        actualFeatures.add(feature.name());
      }

      assertTrue(
          actualFeatures.containsAll(optimizationFeatures),
          "All optimization features should be present");
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("COMPONENT_MODEL should be first (ordinal 0)")
    void componentModelShouldBeFirst() {
      assertEquals(
          0, ComponentFeature.COMPONENT_MODEL.ordinal(), "COMPONENT_MODEL should have ordinal 0");
    }

    @Test
    @DisplayName("CONFIG_MANAGEMENT should be last (ordinal 14)")
    void configManagementShouldBeLast() {
      assertEquals(
          14,
          ComponentFeature.CONFIG_MANAGEMENT.ordinal(),
          "CONFIG_MANAGEMENT should have ordinal 14");
    }

    @Test
    @DisplayName("Ordinal values should be continuous from 0 to 14")
    void ordinalsShouldBeContinuous() {
      ComponentFeature[] values = ComponentFeature.values();
      assertEquals(0, values[0].ordinal(), "First ordinal should be 0");
      assertEquals(14, values[values.length - 1].ordinal(), "Last ordinal should be 14");

      // Check all ordinals are continuous
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal at index " + i + " should be " + i);
      }
    }
  }

  // ========================================================================
  // Enum Instance Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Instance Tests")
  class EnumInstanceTests {

    @Test
    @DisplayName("Enum constants should be singleton")
    void enumConstantsShouldBeSingleton() {
      ComponentFeature first = ComponentFeature.valueOf("MONITORING");
      ComponentFeature second = ComponentFeature.valueOf("MONITORING");

      assertTrue(first == second, "Enum constants should be the same instance");
    }

    @Test
    @DisplayName("Enum should not allow null values")
    void enumShouldNotAllowNullValues() {
      ComponentFeature[] values = ComponentFeature.values();
      for (ComponentFeature feature : values) {
        assertNotNull(feature, "Enum value should not be null");
      }
    }

    @Test
    @DisplayName("Each enum should have correct declaring class")
    void eachEnumShouldHaveCorrectDeclaringClass() {
      for (ComponentFeature feature : ComponentFeature.values()) {
        assertEquals(
            ComponentFeature.class,
            feature.getDeclaringClass(),
            feature.name() + " should have ComponentFeature as declaring class");
      }
    }
  }

  // ========================================================================
  // EnumSet and EnumMap Compatibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Collection Compatibility Tests")
  class CollectionCompatibilityTests {

    @Test
    @DisplayName("Enum values should work with Arrays.asList")
    void enumValuesShouldWorkWithArraysList() {
      var list = Arrays.asList(ComponentFeature.values());
      assertEquals(15, list.size(), "List should contain 15 elements");
      assertTrue(list.contains(ComponentFeature.MONITORING), "List should contain MONITORING");
    }

    @Test
    @DisplayName("Enum values should work with HashSet")
    void enumValuesShouldWorkWithHashSet() {
      Set<ComponentFeature> set = new HashSet<>(Arrays.asList(ComponentFeature.values()));
      assertEquals(15, set.size(), "Set should contain 15 elements");
      assertTrue(set.contains(ComponentFeature.CLUSTERING), "Set should contain CLUSTERING");
    }
  }
}
