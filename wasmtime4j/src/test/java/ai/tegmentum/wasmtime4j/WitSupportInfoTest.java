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

import ai.tegmentum.wasmtime4j.wit.WitSupportInfo;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitSupportInfo} class.
 *
 * <p>WitSupportInfo provides information about WIT capabilities in the runtime.
 */
@DisplayName("WitSupportInfo Tests")
class WitSupportInfoTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
      final WitSupportInfo info =
          new WitSupportInfo(
              true, "2.0", Set.of("interfaces", "types"), List.of("u32", "string"), 15);

      assertTrue(info.isSupported());
      assertEquals("2.0", info.getVersion());
      assertEquals(2, info.getSupportedFeatures().size());
      assertEquals(2, info.getSupportedTypes().size());
      assertEquals(15, info.getMaxInterfaceDepth());
    }

    @Test
    @DisplayName("should create defensive copies")
    void shouldCreateDefensiveCopies() {
      final Set<String> features = Set.of("feature1");
      final List<String> types = List.of("type1");
      final WitSupportInfo info = new WitSupportInfo(true, "1.0", features, types, 10);

      assertNotNull(info.getSupportedFeatures());
      assertNotNull(info.getSupportedTypes());
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("unsupported should create unsupported info")
    void unsupportedShouldCreateUnsupportedInfo() {
      final WitSupportInfo info = WitSupportInfo.unsupported();

      assertFalse(info.isSupported());
      assertEquals("none", info.getVersion());
      assertTrue(info.getSupportedFeatures().isEmpty());
      assertTrue(info.getSupportedTypes().isEmpty());
      assertEquals(0, info.getMaxInterfaceDepth());
    }

    @Test
    @DisplayName("basic should create basic support info")
    void basicShouldCreateBasicSupportInfo() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertTrue(info.isSupported());
      assertEquals("1.0", info.getVersion());
      assertFalse(info.getSupportedFeatures().isEmpty());
      assertFalse(info.getSupportedTypes().isEmpty());
      assertEquals(10, info.getMaxInterfaceDepth());
    }

    @Test
    @DisplayName("basic should include standard features")
    void basicShouldIncludeStandardFeatures() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertTrue(info.getSupportedFeatures().contains("interfaces"));
      assertTrue(info.getSupportedFeatures().contains("functions"));
      assertTrue(info.getSupportedFeatures().contains("types"));
    }

    @Test
    @DisplayName("basic should include primitive types")
    void basicShouldIncludePrimitiveTypes() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertTrue(info.getSupportedTypes().contains("u8"));
      assertTrue(info.getSupportedTypes().contains("u32"));
      assertTrue(info.getSupportedTypes().contains("string"));
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("isSupported should return correct value")
    void isSupportedShouldReturnCorrectValue() {
      assertTrue(WitSupportInfo.basic().isSupported());
      assertFalse(WitSupportInfo.unsupported().isSupported());
    }

    @Test
    @DisplayName("getVersion should return version string")
    void getVersionShouldReturnVersionString() {
      final WitSupportInfo info = new WitSupportInfo(true, "2.1", Set.of(), List.of(), 10);
      assertEquals("2.1", info.getVersion());
    }

    @Test
    @DisplayName("getSupportedFeatures should return immutable set")
    void getSupportedFeaturesShouldReturnImmutableSet() {
      final WitSupportInfo info = WitSupportInfo.basic();
      final Set<String> features = info.getSupportedFeatures();

      assertNotNull(features);
      assertEquals(3, features.size());
    }

    @Test
    @DisplayName("getSupportedTypes should return immutable list")
    void getSupportedTypesShouldReturnImmutableList() {
      final WitSupportInfo info = WitSupportInfo.basic();
      final List<String> types = info.getSupportedTypes();

      assertNotNull(types);
      assertTrue(types.size() > 0);
    }

    @Test
    @DisplayName("getMaxInterfaceDepth should return depth")
    void getMaxInterfaceDepthShouldReturnDepth() {
      final WitSupportInfo info = new WitSupportInfo(true, "1.0", Set.of(), List.of(), 20);
      assertEquals(20, info.getMaxInterfaceDepth());
    }
  }

  @Nested
  @DisplayName("Feature Support Tests")
  class FeatureSupportTests {

    @Test
    @DisplayName("isFeatureSupported should return true for supported feature")
    void isFeatureSupportedShouldReturnTrueForSupportedFeature() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertTrue(info.isFeatureSupported("interfaces"));
      assertTrue(info.isFeatureSupported("functions"));
    }

    @Test
    @DisplayName("isFeatureSupported should return false for unsupported feature")
    void isFeatureSupportedShouldReturnFalseForUnsupportedFeature() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertFalse(info.isFeatureSupported("nonexistent"));
    }

    @Test
    @DisplayName("isFeatureSupported should return false for empty info")
    void isFeatureSupportedShouldReturnFalseForEmptyInfo() {
      final WitSupportInfo info = WitSupportInfo.unsupported();

      assertFalse(info.isFeatureSupported("interfaces"));
    }
  }

  @Nested
  @DisplayName("Type Support Tests")
  class TypeSupportTests {

    @Test
    @DisplayName("isTypeSupported should return true for supported type")
    void isTypeSupportedShouldReturnTrueForSupportedType() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertTrue(info.isTypeSupported("u32"));
      assertTrue(info.isTypeSupported("string"));
    }

    @Test
    @DisplayName("isTypeSupported should return false for unsupported type")
    void isTypeSupportedShouldReturnFalseForUnsupportedType() {
      final WitSupportInfo info = WitSupportInfo.basic();

      assertFalse(info.isTypeSupported("nonexistent"));
    }

    @Test
    @DisplayName("isTypeSupported should return false for empty info")
    void isTypeSupportedShouldReturnFalseForEmptyInfo() {
      final WitSupportInfo info = WitSupportInfo.unsupported();

      assertFalse(info.isTypeSupported("u32"));
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key information")
    void toStringShouldContainKeyInformation() {
      final WitSupportInfo info = WitSupportInfo.basic();
      final String str = info.toString();

      assertTrue(str.contains("WitSupportInfo"));
      assertTrue(str.contains("supported=true"));
      assertTrue(str.contains("version='1.0'"));
      assertTrue(str.contains("features="));
      assertTrue(str.contains("types="));
      assertTrue(str.contains("maxDepth="));
    }

    @Test
    @DisplayName("toString for unsupported should show false")
    void toStringForUnsupportedShouldShowFalse() {
      final WitSupportInfo info = WitSupportInfo.unsupported();
      final String str = info.toString();

      assertTrue(str.contains("supported=false"));
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WitSupportInfo should be final")
    void witSupportInfoShouldBeFinal() {
      assertTrue(java.lang.reflect.Modifier.isFinal(WitSupportInfo.class.getModifiers()));
    }
  }
}
