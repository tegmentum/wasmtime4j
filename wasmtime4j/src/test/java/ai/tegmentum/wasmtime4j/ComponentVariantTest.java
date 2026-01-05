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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentVariant} interface.
 *
 * <p>ComponentVariant represents a Component Model variant value (tagged union).
 */
@DisplayName("ComponentVariant Tests")
class ComponentVariantTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentVariant.class.getModifiers()),
          "ComponentVariant should be public");
      assertTrue(ComponentVariant.class.isInterface(), "ComponentVariant should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getCaseName method")
    void shouldHaveGetCaseNameMethod() throws NoSuchMethodException {
      final Method method = ComponentVariant.class.getMethod("getCaseName");
      assertNotNull(method, "getCaseName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getPayload method")
    void shouldHaveGetPayloadMethod() throws NoSuchMethodException {
      final Method method = ComponentVariant.class.getMethod("getPayload");
      assertNotNull(method, "getPayload method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have hasPayload default method")
    void shouldHaveHasPayloadDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentVariant.class.getMethod("hasPayload");
      assertNotNull(method, "hasPayload method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertTrue(method.isDefault(), "hasPayload should be a default method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have of static method without payload")
    void shouldHaveOfStaticMethodWithoutPayload() throws NoSuchMethodException {
      final Method method = ComponentVariant.class.getMethod("of", String.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          ComponentVariant.class, method.getReturnType(), "Should return ComponentVariant");
    }

    @Test
    @DisplayName("should have of static method with payload")
    void shouldHaveOfStaticMethodWithPayload() throws NoSuchMethodException {
      final Method method =
          ComponentVariant.class.getMethod("of", String.class, ComponentVal.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(
          ComponentVariant.class, method.getReturnType(), "Should return ComponentVariant");
    }
  }

  @Nested
  @DisplayName("Impl Nested Class Tests")
  class ImplNestedClassTests {

    @Test
    @DisplayName("should have Impl nested class")
    void shouldHaveImplNestedClass() {
      final var nestedClasses = ComponentVariant.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Impl")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Impl should be a class");
          assertTrue(
              ComponentVariant.class.isAssignableFrom(nestedClass),
              "Impl should implement ComponentVariant");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Impl should be final");
          break;
        }
      }
      assertTrue(found, "Should have Impl nested class");
    }
  }

  @Nested
  @DisplayName("Factory Method Behavior Tests")
  class FactoryMethodBehaviorTests {

    @Test
    @DisplayName("of without payload should create variant")
    void ofWithoutPayloadShouldCreateVariant() {
      final ComponentVariant variant = ComponentVariant.of("none");

      assertNotNull(variant, "Should create variant");
      assertEquals("none", variant.getCaseName(), "Case name should match");
      assertFalse(variant.hasPayload(), "Should not have payload");
      assertTrue(variant.getPayload().isEmpty(), "Payload should be empty");
    }

    @Test
    @DisplayName("of with payload should create variant with payload")
    void ofWithPayloadShouldCreateVariantWithPayload() {
      final ComponentVal payload = ComponentVal.s32(42);
      final ComponentVariant variant = ComponentVariant.of("some", payload);

      assertNotNull(variant, "Should create variant");
      assertEquals("some", variant.getCaseName(), "Case name should match");
      assertTrue(variant.hasPayload(), "Should have payload");
      assertTrue(variant.getPayload().isPresent(), "Payload should be present");
      assertEquals(payload, variant.getPayload().get(), "Payload should match");
    }

    @Test
    @DisplayName("of with null payload should create variant without payload")
    void ofWithNullPayloadShouldCreateVariantWithoutPayload() {
      final ComponentVariant variant = ComponentVariant.of("empty", null);

      assertNotNull(variant, "Should create variant");
      assertEquals("empty", variant.getCaseName(), "Case name should match");
      assertFalse(variant.hasPayload(), "Should not have payload");
    }

    @Test
    @DisplayName("of should reject null case name")
    void ofShouldRejectNullCaseName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVariant.of(null),
          "Should throw for null case name");

      assertThrows(
          IllegalArgumentException.class,
          () -> ComponentVariant.of(null, ComponentVal.s32(1)),
          "Should throw for null case name with payload");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("two variants with same case and no payload should be equal")
    void twoVariantsWithSameCaseAndNoPayloadShouldBeEqual() {
      final ComponentVariant v1 = ComponentVariant.of("case1");
      final ComponentVariant v2 = ComponentVariant.of("case1");

      assertEquals(v1, v2, "Variants should be equal");
      assertEquals(v1.hashCode(), v2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("two variants with same case and same payload should be equal")
    void twoVariantsWithSameCaseAndSamePayloadShouldBeEqual() {
      final ComponentVal payload1 = ComponentVal.s32(42);
      final ComponentVal payload2 = ComponentVal.s32(42);
      final ComponentVariant v1 = ComponentVariant.of("case1", payload1);
      final ComponentVariant v2 = ComponentVariant.of("case1", payload2);

      assertEquals(v1, v2, "Variants should be equal");
      assertEquals(v1.hashCode(), v2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("two variants with different cases should not be equal")
    void twoVariantsWithDifferentCasesShouldNotBeEqual() {
      final ComponentVariant v1 = ComponentVariant.of("case1");
      final ComponentVariant v2 = ComponentVariant.of("case2");

      assertNotEquals(v1, v2, "Variants should not be equal");
    }

    @Test
    @DisplayName("two variants with same case but different payloads should not be equal")
    void twoVariantsWithSameCaseButDifferentPayloadsShouldNotBeEqual() {
      final ComponentVariant v1 = ComponentVariant.of("case1", ComponentVal.s32(1));
      final ComponentVariant v2 = ComponentVariant.of("case1", ComponentVal.s32(2));

      assertNotEquals(v1, v2, "Variants should not be equal");
    }

    @Test
    @DisplayName("variant with payload and variant without payload should not be equal")
    void variantWithPayloadAndWithoutPayloadShouldNotBeEqual() {
      final ComponentVariant v1 = ComponentVariant.of("case1");
      final ComponentVariant v2 = ComponentVariant.of("case1", ComponentVal.s32(1));

      assertNotEquals(v1, v2, "Variants should not be equal");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("variant without payload should return case name")
    void variantWithoutPayloadShouldReturnCaseName() {
      final ComponentVariant variant = ComponentVariant.of("none");
      assertEquals("none", variant.toString(), "toString should be case name");
    }

    @Test
    @DisplayName("variant with payload should include payload in toString")
    void variantWithPayloadShouldIncludePayloadInToString() {
      final ComponentVariant variant = ComponentVariant.of("some", ComponentVal.s32(42));
      final String str = variant.toString();

      assertTrue(str.startsWith("some("), "Should start with case name and paren");
      assertTrue(str.endsWith(")"), "Should end with paren");
    }
  }
}
