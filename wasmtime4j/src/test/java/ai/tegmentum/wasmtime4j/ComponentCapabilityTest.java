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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentCapability.CapabilityLevel;
import ai.tegmentum.wasmtime4j.ComponentCapability.CapabilityType;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentCapability} class.
 *
 * <p>ComponentCapability represents capabilities that components can provide or require.
 */
@DisplayName("ComponentCapability Tests")
class ComponentCapabilityTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentCapability.class.getModifiers()),
          "ComponentCapability should be public");
      assertTrue(
          Modifier.isFinal(ComponentCapability.class.getModifiers()),
          "ComponentCapability should be final");
    }

    @Test
    @DisplayName("should have CapabilityType nested enum")
    void shouldHaveCapabilityTypeNestedEnum() {
      final var nestedClasses = ComponentCapability.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CapabilityType")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "CapabilityType should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have CapabilityType nested enum");
    }

    @Test
    @DisplayName("should have CapabilityLevel nested enum")
    void shouldHaveCapabilityLevelNestedEnum() {
      final var nestedClasses = ComponentCapability.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("CapabilityLevel")) {
          found = true;
          assertTrue(nestedClass.isEnum(), "CapabilityLevel should be an enum");
          break;
        }
      }
      assertTrue(found, "Should have CapabilityLevel nested enum");
    }

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      final var nestedClasses = ComponentCapability.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertTrue(
              Modifier.isFinal(nestedClass.getModifiers()), "Builder should be a final class");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested class");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create capability via builder")
    void shouldCreateCapabilityViaBuilder() {
      final ComponentCapability capability =
          ComponentCapability.builder("test-capability", CapabilityType.INTERFACE).build();

      assertNotNull(capability, "Capability should not be null");
      assertEquals("test-capability", capability.getName(), "Name should match");
      assertEquals(CapabilityType.INTERFACE, capability.getType(), "Type should match");
    }

    @Test
    @DisplayName("should set version via builder")
    void shouldSetVersionViaBuilder() {
      final ComponentVersion version = new ComponentVersion(1, 2, 3);
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.FEATURE).version(version).build();

      assertTrue(capability.getVersion().isPresent(), "Version should be present");
      assertEquals(version, capability.getVersion().get(), "Version should match");
    }

    @Test
    @DisplayName("should set attributes via builder")
    void shouldSetAttributesViaBuilder() {
      final Set<String> attributes = Set.of("attr1", "attr2");
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.RESOURCE)
              .attributes(attributes)
              .build();

      assertEquals(2, capability.getAttributes().size(), "Should have 2 attributes");
      assertTrue(capability.getAttributes().contains("attr1"), "Should contain attr1");
      assertTrue(capability.getAttributes().contains("attr2"), "Should contain attr2");
    }

    @Test
    @DisplayName("should set level via builder")
    void shouldSetLevelViaBuilder() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.SERVICE)
              .level(CapabilityLevel.OPTIONAL)
              .build();

      assertEquals(CapabilityLevel.OPTIONAL, capability.getLevel(), "Level should match");
    }

    @Test
    @DisplayName("should have default level of REQUIRED")
    void shouldHaveDefaultLevelOfRequired() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();

      assertEquals(
          CapabilityLevel.REQUIRED, capability.getLevel(), "Default level should be REQUIRED");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create interface capability")
    void shouldCreateInterfaceCapability() {
      final ComponentCapability capability =
          ComponentCapability.interfaceCapability("my-interface");

      assertEquals("my-interface", capability.getName(), "Name should match");
      assertEquals(CapabilityType.INTERFACE, capability.getType(), "Type should be INTERFACE");
      assertEquals(CapabilityLevel.REQUIRED, capability.getLevel(), "Level should be REQUIRED");
    }

    @Test
    @DisplayName("should create versioned interface capability")
    void shouldCreateVersionedInterfaceCapability() {
      final ComponentVersion version = new ComponentVersion(2, 0, 0);
      final ComponentCapability capability =
          ComponentCapability.interfaceCapability("my-interface", version);

      assertEquals("my-interface", capability.getName(), "Name should match");
      assertEquals(CapabilityType.INTERFACE, capability.getType(), "Type should be INTERFACE");
      assertTrue(capability.getVersion().isPresent(), "Version should be present");
      assertEquals(version, capability.getVersion().get(), "Version should match");
    }

    @Test
    @DisplayName("should create feature capability")
    void shouldCreateFeatureCapability() {
      final ComponentCapability capability = ComponentCapability.featureCapability("my-feature");

      assertEquals("my-feature", capability.getName(), "Name should match");
      assertEquals(CapabilityType.FEATURE, capability.getType(), "Type should be FEATURE");
      assertEquals(CapabilityLevel.OPTIONAL, capability.getLevel(), "Level should be OPTIONAL");
    }

    @Test
    @DisplayName("should create resource capability")
    void shouldCreateResourceCapability() {
      final ComponentCapability capability = ComponentCapability.resourceCapability("my-resource");

      assertEquals("my-resource", capability.getName(), "Name should match");
      assertEquals(CapabilityType.RESOURCE, capability.getType(), "Type should be RESOURCE");
      assertEquals(CapabilityLevel.REQUIRED, capability.getLevel(), "Level should be REQUIRED");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return capability name")
    void getNameShouldReturnCapabilityName() {
      final ComponentCapability capability =
          ComponentCapability.builder("test-name", CapabilityType.INTERFACE).build();

      assertEquals("test-name", capability.getName(), "Name should match");
    }

    @Test
    @DisplayName("getType should return capability type")
    void getTypeShouldReturnCapabilityType() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.SECURITY).build();

      assertEquals(CapabilityType.SECURITY, capability.getType(), "Type should match");
    }

    @Test
    @DisplayName("getVersion should return empty optional when not set")
    void getVersionShouldReturnEmptyOptionalWhenNotSet() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();

      assertEquals(Optional.empty(), capability.getVersion(), "Version should be empty");
    }

    @Test
    @DisplayName("getAttributes should return immutable set")
    void getAttributesShouldReturnImmutableSet() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .attributes(Set.of("attr1"))
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> capability.getAttributes().add("new-attr"),
          "Attributes should be immutable");
    }
  }

  @Nested
  @DisplayName("Compatibility Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("should be compatible with same capability")
    void shouldBeCompatibleWithSameCapability() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();

      assertTrue(cap1.isCompatibleWith(cap2), "Same capabilities should be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different name")
    void shouldNotBeCompatibleWithDifferentName() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test1", CapabilityType.INTERFACE).build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test2", CapabilityType.INTERFACE).build();

      assertFalse(cap1.isCompatibleWith(cap2), "Different names should not be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different type")
    void shouldNotBeCompatibleWithDifferentType() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test", CapabilityType.FEATURE).build();

      assertFalse(cap1.isCompatibleWith(cap2), "Different types should not be compatible");
    }

    @Test
    @DisplayName("should be compatible with same major version")
    void shouldBeCompatibleWithSameMajorVersion() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .version(new ComponentVersion(1, 0, 0))
              .build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .version(new ComponentVersion(1, 2, 0))
              .build();

      assertTrue(cap1.isCompatibleWith(cap2), "Same major version should be compatible");
    }

    @Test
    @DisplayName("should not be compatible with different major version")
    void shouldNotBeCompatibleWithDifferentMajorVersion() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .version(new ComponentVersion(1, 0, 0))
              .build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .version(new ComponentVersion(2, 0, 0))
              .build();

      assertFalse(cap1.isCompatibleWith(cap2), "Different major versions should not be compatible");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsHashCodeTests {

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();

      assertEquals(capability, capability, "Should be equal to itself");
    }

    @Test
    @DisplayName("should be equal to capability with same properties")
    void shouldBeEqualToCapabilityWithSameProperties() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .level(CapabilityLevel.REQUIRED)
              .build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE)
              .level(CapabilityLevel.REQUIRED)
              .build();

      assertEquals(cap1, cap2, "Should be equal");
      assertEquals(cap1.hashCode(), cap2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("should not be equal to capability with different properties")
    void shouldNotBeEqualToCapabilityWithDifferentProperties() {
      final ComponentCapability cap1 =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();
      final ComponentCapability cap2 =
          ComponentCapability.builder("different", CapabilityType.INTERFACE).build();

      assertNotEquals(cap1, cap2, "Should not be equal");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final ComponentCapability capability =
          ComponentCapability.builder("test", CapabilityType.INTERFACE).build();

      assertNotEquals(null, capability, "Should not be equal to null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final ComponentCapability capability =
          ComponentCapability.builder("test-cap", CapabilityType.INTERFACE)
              .level(CapabilityLevel.REQUIRED)
              .build();

      final String str = capability.toString();
      assertTrue(str.contains("test-cap"), "Should contain name");
      assertTrue(str.contains("INTERFACE"), "Should contain type");
      assertTrue(str.contains("REQUIRED"), "Should contain level");
    }
  }

  @Nested
  @DisplayName("CapabilityType Enum Tests")
  class CapabilityTypeEnumTests {

    @Test
    @DisplayName("should have all capability types")
    void shouldHaveAllCapabilityTypes() {
      final var types = CapabilityType.values();
      assertEquals(7, types.length, "Should have 7 capability types");
    }

    @Test
    @DisplayName("should have INTERFACE type")
    void shouldHaveInterfaceType() {
      assertEquals(CapabilityType.INTERFACE, CapabilityType.valueOf("INTERFACE"));
    }

    @Test
    @DisplayName("should have FEATURE type")
    void shouldHaveFeatureType() {
      assertEquals(CapabilityType.FEATURE, CapabilityType.valueOf("FEATURE"));
    }

    @Test
    @DisplayName("should have RESOURCE type")
    void shouldHaveResourceType() {
      assertEquals(CapabilityType.RESOURCE, CapabilityType.valueOf("RESOURCE"));
    }

    @Test
    @DisplayName("should have SERVICE type")
    void shouldHaveServiceType() {
      assertEquals(CapabilityType.SERVICE, CapabilityType.valueOf("SERVICE"));
    }

    @Test
    @DisplayName("should have SECURITY type")
    void shouldHaveSecurityType() {
      assertEquals(CapabilityType.SECURITY, CapabilityType.valueOf("SECURITY"));
    }

    @Test
    @DisplayName("should have PERFORMANCE type")
    void shouldHavePerformanceType() {
      assertEquals(CapabilityType.PERFORMANCE, CapabilityType.valueOf("PERFORMANCE"));
    }

    @Test
    @DisplayName("should have CUSTOM type")
    void shouldHaveCustomType() {
      assertEquals(CapabilityType.CUSTOM, CapabilityType.valueOf("CUSTOM"));
    }
  }

  @Nested
  @DisplayName("CapabilityLevel Enum Tests")
  class CapabilityLevelEnumTests {

    @Test
    @DisplayName("should have all capability levels")
    void shouldHaveAllCapabilityLevels() {
      final var levels = CapabilityLevel.values();
      assertEquals(4, levels.length, "Should have 4 capability levels");
    }

    @Test
    @DisplayName("should have REQUIRED level")
    void shouldHaveRequiredLevel() {
      assertEquals(CapabilityLevel.REQUIRED, CapabilityLevel.valueOf("REQUIRED"));
    }

    @Test
    @DisplayName("should have PREFERRED level")
    void shouldHavePreferredLevel() {
      assertEquals(CapabilityLevel.PREFERRED, CapabilityLevel.valueOf("PREFERRED"));
    }

    @Test
    @DisplayName("should have OPTIONAL level")
    void shouldHaveOptionalLevel() {
      assertEquals(CapabilityLevel.OPTIONAL, CapabilityLevel.valueOf("OPTIONAL"));
    }

    @Test
    @DisplayName("should have PROHIBITED level")
    void shouldHaveProhibitedLevel() {
      assertEquals(CapabilityLevel.PROHIBITED, CapabilityLevel.valueOf("PROHIBITED"));
    }

    @Test
    @DisplayName("REQUIRED should not be compatible with PROHIBITED")
    void requiredShouldNotBeCompatibleWithProhibited() {
      assertFalse(
          CapabilityLevel.REQUIRED.isCompatibleWith(CapabilityLevel.PROHIBITED),
          "REQUIRED should not be compatible with PROHIBITED");
    }

    @Test
    @DisplayName("PROHIBITED should not be compatible with REQUIRED")
    void prohibitedShouldNotBeCompatibleWithRequired() {
      assertFalse(
          CapabilityLevel.PROHIBITED.isCompatibleWith(CapabilityLevel.REQUIRED),
          "PROHIBITED should not be compatible with REQUIRED");
    }

    @Test
    @DisplayName("OPTIONAL should be compatible with all levels")
    void optionalShouldBeCompatibleWithAllLevels() {
      assertTrue(
          CapabilityLevel.OPTIONAL.isCompatibleWith(CapabilityLevel.REQUIRED),
          "OPTIONAL should be compatible with REQUIRED");
      assertTrue(
          CapabilityLevel.OPTIONAL.isCompatibleWith(CapabilityLevel.PREFERRED),
          "OPTIONAL should be compatible with PREFERRED");
      assertTrue(
          CapabilityLevel.OPTIONAL.isCompatibleWith(CapabilityLevel.OPTIONAL),
          "OPTIONAL should be compatible with OPTIONAL");
      assertTrue(
          CapabilityLevel.OPTIONAL.isCompatibleWith(CapabilityLevel.PROHIBITED),
          "OPTIONAL should be compatible with PROHIBITED");
    }
  }
}
