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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitVariant} class.
 *
 * <p>WitVariant represents a WIT variant value (sum type / tagged union).
 */
@DisplayName("WitVariant Tests")
class WitVariantTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitVariant.class.getModifiers()), "WitVariant should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitVariant.class.getModifiers()), "WitVariant should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitVariant.class), "WitVariant should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method with payload")
    void shouldHaveOfFactoryMethodWithPayload() throws NoSuchMethodException {
      final Method method =
          WitVariant.class.getMethod("of", WitType.class, String.class, WitValue.class);
      assertNotNull(method, "of method with payload should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitVariant.class, method.getReturnType(), "of should return WitVariant");
    }

    @Test
    @DisplayName("should have of factory method without payload")
    void shouldHaveOfFactoryMethodWithoutPayload() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("of", WitType.class, String.class);
      assertNotNull(method, "of method without payload should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitVariant.class, method.getReturnType(), "of should return WitVariant");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getCaseName method")
    void shouldHaveGetCaseNameMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getCaseName");
      assertNotNull(method, "getCaseName method should exist");
      assertEquals(String.class, method.getReturnType(), "getCaseName should return String");
    }

    @Test
    @DisplayName("should have getPayload method")
    void shouldHaveGetPayloadMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getPayload");
      assertNotNull(method, "getPayload method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getPayload should return Optional");
    }

    @Test
    @DisplayName("should have hasPayload method")
    void shouldHaveHasPayloadMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("hasPayload");
      assertNotNull(method, "hasPayload method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hasPayload should return boolean");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Object.class, method.getReturnType(), "toJava should return Object");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getCaseName should take no parameters")
    void getCaseNameShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getCaseName");
      assertEquals(0, method.getParameterCount(), "getCaseName should take no parameters");
    }

    @Test
    @DisplayName("getPayload should take no parameters")
    void getPayloadShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getPayload");
      assertEquals(0, method.getParameterCount(), "getPayload should take no parameters");
    }

    @Test
    @DisplayName("hasPayload should take no parameters")
    void hasPayloadShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("hasPayload");
      assertEquals(0, method.getParameterCount(), "hasPayload should take no parameters");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
