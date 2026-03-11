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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentVariant}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentVariant")
class ComponentVariantTest {

  @Nested
  @DisplayName("factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("creates variant without payload")
    void createsWithoutPayload() {
      final ComponentVariant variant = ComponentVariant.of("none");
      assertEquals("none", variant.getCaseName());
      assertFalse(variant.hasPayload());
      assertFalse(variant.getPayload().isPresent());
    }

    @Test
    @DisplayName("creates variant with payload")
    void createsWithPayload() {
      final ComponentVal payload = ComponentVal.s32(42);
      final ComponentVariant variant = ComponentVariant.of("some", payload);
      assertEquals("some", variant.getCaseName());
      assertTrue(variant.hasPayload());
      assertTrue(variant.getPayload().isPresent());
      assertEquals(42, variant.getPayload().get().asS32());
    }

    @Test
    @DisplayName("rejects null case name")
    void rejectsNullCaseName() {
      assertThrows(IllegalArgumentException.class, () -> ComponentVariant.of(null));
    }

    @Test
    @DisplayName("rejects null case name with payload")
    void rejectsNullCaseNameWithPayload() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentVariant.of(null, ComponentVal.s32(1)));
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("equal variants without payload")
    void equalWithoutPayload() {
      final ComponentVariant v1 = ComponentVariant.of("none");
      final ComponentVariant v2 = ComponentVariant.of("none");
      assertEquals(v1, v2);
      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("different case names not equal")
    void differentCaseNames() {
      final ComponentVariant v1 = ComponentVariant.of("a");
      final ComponentVariant v2 = ComponentVariant.of("b");
      assertNotEquals(v1, v2);
    }

    @Test
    @DisplayName("equal to self")
    void equalToSelf() {
      final ComponentVariant v1 = ComponentVariant.of("test");
      assertEquals(v1, v1);
    }

    @Test
    @DisplayName("not equal to null")
    void notEqualToNull() {
      final ComponentVariant v1 = ComponentVariant.of("test");
      assertNotEquals(null, v1);
    }
  }

  @Nested
  @DisplayName("toString")
  class ToStringTests {

    @Test
    @DisplayName("without payload shows case name only")
    void withoutPayload() {
      assertEquals("none", ComponentVariant.of("none").toString());
    }

    @Test
    @DisplayName("with payload shows case name and payload")
    void withPayload() {
      final ComponentVariant variant = ComponentVariant.of("some", ComponentVal.s32(42));
      final String str = variant.toString();
      assertTrue(str.startsWith("some("));
      assertTrue(str.endsWith(")"));
    }
  }
}
