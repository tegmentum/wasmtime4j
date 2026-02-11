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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentLifecycleState}.
 *
 * <p>Verifies enum structure, constants, valueOf/values, ordinals, and toString.
 */
@DisplayName("ComponentLifecycleState Tests")
class ComponentLifecycleStateTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(ComponentLifecycleState.class.isEnum(),
          "ComponentLifecycleState should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 9 values")
    void shouldHaveExactValueCount() {
      assertEquals(9, ComponentLifecycleState.values().length,
          "ComponentLifecycleState should have exactly 9 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain CREATING")
    void shouldContainCreating() {
      assertNotNull(ComponentLifecycleState.CREATING, "CREATING constant should exist");
    }

    @Test
    @DisplayName("should contain READY")
    void shouldContainReady() {
      assertNotNull(ComponentLifecycleState.READY, "READY constant should exist");
    }

    @Test
    @DisplayName("should contain ACTIVE")
    void shouldContainActive() {
      assertNotNull(ComponentLifecycleState.ACTIVE, "ACTIVE constant should exist");
    }

    @Test
    @DisplayName("should contain SUSPENDED")
    void shouldContainSuspended() {
      assertNotNull(ComponentLifecycleState.SUSPENDED,
          "SUSPENDED constant should exist");
    }

    @Test
    @DisplayName("should contain UPDATING")
    void shouldContainUpdating() {
      assertNotNull(ComponentLifecycleState.UPDATING, "UPDATING constant should exist");
    }

    @Test
    @DisplayName("should contain DEPRECATED")
    void shouldContainDeprecated() {
      assertNotNull(ComponentLifecycleState.DEPRECATED,
          "DEPRECATED constant should exist");
    }

    @Test
    @DisplayName("should contain ERROR")
    void shouldContainError() {
      assertNotNull(ComponentLifecycleState.ERROR, "ERROR constant should exist");
    }

    @Test
    @DisplayName("should contain DESTROYING")
    void shouldContainDestroying() {
      assertNotNull(ComponentLifecycleState.DESTROYING,
          "DESTROYING constant should exist");
    }

    @Test
    @DisplayName("should contain DESTROYED")
    void shouldContainDestroyed() {
      assertNotNull(ComponentLifecycleState.DESTROYED,
          "DESTROYED constant should exist");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals")
    void shouldHaveUniqueOrdinals() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final ComponentLifecycleState value : ComponentLifecycleState.values()) {
        ordinals.add(value.ordinal());
      }
      assertEquals(ComponentLifecycleState.values().length, ordinals.size(),
          "All ordinals should be unique");
    }

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final ComponentLifecycleState[] values = ComponentLifecycleState.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(),
            "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final ComponentLifecycleState value : ComponentLifecycleState.values()) {
        assertEquals(value, ComponentLifecycleState.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> ComponentLifecycleState.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call")
    void shouldReturnNewArrayOnEachCall() {
      final ComponentLifecycleState[] first = ComponentLifecycleState.values();
      final ComponentLifecycleState[] second = ComponentLifecycleState.values();
      assertTrue(first != second, "values() should return a new array on each call");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return enum name as toString")
    void shouldReturnEnumNameAsToString() {
      for (final ComponentLifecycleState value : ComponentLifecycleState.values()) {
        assertEquals(value.name(), value.toString(),
            "toString should return the enum name for " + value.name());
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support switch statement over all values")
    void shouldSupportSwitchStatement() {
      for (final ComponentLifecycleState state : ComponentLifecycleState.values()) {
        final String result;
        switch (state) {
          case CREATING:
          case READY:
          case ACTIVE:
          case SUSPENDED:
          case UPDATING:
          case DEPRECATED:
          case ERROR:
          case DESTROYING:
          case DESTROYED:
            result = state.name();
            break;
          default:
            result = "unknown";
            break;
        }
        assertEquals(state.name(), result, "Switch should handle " + state.name());
      }
    }
  }
}
