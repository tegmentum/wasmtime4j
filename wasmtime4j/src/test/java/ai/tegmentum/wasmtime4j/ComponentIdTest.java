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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentId} class.
 *
 * <p>ComponentId provides unique identification for WebAssembly components.
 */
@DisplayName("ComponentId Tests")
class ComponentIdTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentId.class.getModifiers()), "ComponentId should be public");
      assertTrue(Modifier.isFinal(ComponentId.class.getModifiers()), "ComponentId should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create ComponentId with id and name")
    void shouldCreateComponentIdWithIdAndName() {
      final ComponentId componentId = new ComponentId("test-id-123", "test-component");

      assertEquals("test-id-123", componentId.getId(), "ID should match");
      assertEquals("test-component", componentId.getName(), "Name should match");
    }

    @Test
    @DisplayName("should create ComponentId with only name")
    void shouldCreateComponentIdWithOnlyName() {
      final ComponentId componentId = new ComponentId("test-component");

      assertNotNull(componentId.getId(), "ID should not be null");
      assertTrue(componentId.getId().length() > 0, "ID should not be empty");
      assertEquals("test-component", componentId.getName(), "Name should match");
    }

    @Test
    @DisplayName("should generate UUID for auto-generated id")
    void shouldGenerateUuidForAutoGeneratedId() {
      final ComponentId componentId = new ComponentId("test-component");

      // UUID format: 8-4-4-4-12 characters
      final String id = componentId.getId();
      assertNotNull(id, "ID should not be null");
      assertEquals(36, id.length(), "UUID should be 36 characters");
      assertTrue(id.contains("-"), "UUID should contain dashes");
    }

    @Test
    @DisplayName("should throw exception for null id")
    void shouldThrowExceptionForNullId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentId(null, "name"),
          "Should throw for null id");
    }

    @Test
    @DisplayName("should throw exception for empty id")
    void shouldThrowExceptionForEmptyId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new ComponentId("", "name"),
          "Should throw for empty id");
    }

    @Test
    @DisplayName("should allow null name")
    void shouldAllowNullName() {
      final ComponentId componentId = new ComponentId("test-id", null);

      assertEquals("test-id", componentId.getId(), "ID should match");
      assertEquals(null, componentId.getName(), "Name should be null");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getId should return the id")
    void getIdShouldReturnTheId() {
      final ComponentId componentId = new ComponentId("my-id", "my-name");

      assertEquals("my-id", componentId.getId(), "Should return correct ID");
    }

    @Test
    @DisplayName("getName should return the name")
    void getNameShouldReturnTheName() {
      final ComponentId componentId = new ComponentId("my-id", "my-name");

      assertEquals("my-name", componentId.getName(), "Should return correct name");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      assertEquals(componentId, componentId, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to ComponentId with same id")
    void shouldBeEqualToComponentIdWithSameId() {
      final ComponentId id1 = new ComponentId("same-id", "name1");
      final ComponentId id2 = new ComponentId("same-id", "name2");

      assertEquals(id1, id2, "Should be equal when IDs match");
    }

    @Test
    @DisplayName("should not be equal to ComponentId with different id")
    void shouldNotBeEqualToComponentIdWithDifferentId() {
      final ComponentId id1 = new ComponentId("id1", "name");
      final ComponentId id2 = new ComponentId("id2", "name");

      assertNotEquals(id1, id2, "Should not be equal when IDs differ");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      assertNotEquals(null, componentId, "Should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      assertNotEquals("test-id", componentId, "Should not be equal to String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("should have same hashCode for equal objects")
    void shouldHaveSameHashCodeForEqualObjects() {
      final ComponentId id1 = new ComponentId("same-id", "name1");
      final ComponentId id2 = new ComponentId("same-id", "name2");

      assertEquals(id1.hashCode(), id2.hashCode(), "Hash codes should match for equal objects");
    }

    @Test
    @DisplayName("should have consistent hashCode")
    void shouldHaveConsistentHashCode() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      final int hash1 = componentId.hashCode();
      final int hash2 = componentId.hashCode();

      assertEquals(hash1, hash2, "Hash code should be consistent");
    }

    @Test
    @DisplayName("hashCode should be based on id")
    void hashCodeShouldBeBasedOnId() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      assertEquals(
          "test-id".hashCode(), componentId.hashCode(), "Hash code should match id's hashCode");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final ComponentId componentId = new ComponentId("test-id", "test-name");

      final String str = componentId.toString();
      assertTrue(str.contains("test-id"), "Should contain id");
      assertTrue(str.contains("test-name"), "Should contain name");
      assertTrue(str.contains("ComponentId"), "Should contain class name");
    }

    @Test
    @DisplayName("should handle null name in toString")
    void shouldHandleNullNameInToString() {
      final ComponentId componentId = new ComponentId("test-id", null);

      final String str = componentId.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("test-id"), "Should contain id");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle whitespace-only id as valid")
    void shouldHandleWhitespaceOnlyIdAsValid() {
      // Whitespace is not empty, so it should be accepted
      final ComponentId componentId = new ComponentId("   ", "name");
      assertEquals("   ", componentId.getId(), "Should accept whitespace id");
    }

    @Test
    @DisplayName("should generate unique IDs for different instances")
    void shouldGenerateUniqueIdsForDifferentInstances() {
      final ComponentId id1 = new ComponentId("name1");
      final ComponentId id2 = new ComponentId("name2");

      assertNotEquals(id1.getId(), id2.getId(), "Auto-generated IDs should be unique");
    }

    @Test
    @DisplayName("should handle special characters in id")
    void shouldHandleSpecialCharactersInId() {
      final ComponentId componentId = new ComponentId("id-with-special!@#$%^&*()", "name");
      assertEquals(
          "id-with-special!@#$%^&*()", componentId.getId(), "Should handle special characters");
    }

    @Test
    @DisplayName("should handle unicode characters in name")
    void shouldHandleUnicodeCharactersInName() {
      final ComponentId componentId = new ComponentId("id", "名前-日本語");
      assertEquals("名前-日本語", componentId.getName(), "Should handle unicode characters");
    }

    @Test
    @DisplayName("should handle very long id")
    void shouldHandleVeryLongId() {
      final String longId = "a".repeat(10000);
      final ComponentId componentId = new ComponentId(longId, "name");
      assertEquals(longId, componentId.getId(), "Should handle very long id");
    }
  }
}
