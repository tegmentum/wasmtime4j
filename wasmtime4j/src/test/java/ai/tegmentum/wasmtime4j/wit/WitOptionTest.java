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
 * Tests for {@link WitOption} class.
 *
 * <p>WitOption represents a WIT option value (nullable value).
 */
@DisplayName("WitOption Tests")
class WitOptionTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitOption.class.getModifiers()), "WitOption should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitOption.class.getModifiers()), "WitOption should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitOption.class), "WitOption should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have some factory method")
    void shouldHaveSomeFactoryMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("some", WitType.class, WitValue.class);
      assertNotNull(method, "some method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "some should be static");
      assertEquals(WitOption.class, method.getReturnType(), "some should return WitOption");
    }

    @Test
    @DisplayName("should have none factory method")
    void shouldHaveNoneFactoryMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("none", WitType.class);
      assertNotNull(method, "none method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "none should be static");
      assertEquals(WitOption.class, method.getReturnType(), "none should return WitOption");
    }

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("of", WitType.class, Optional.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitOption.class, method.getReturnType(), "of should return WitOption");
    }
  }

  @Nested
  @DisplayName("State Check Method Tests")
  class StateCheckMethodTests {

    @Test
    @DisplayName("should have isSome method")
    void shouldHaveIsSomeMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("isSome");
      assertNotNull(method, "isSome method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isSome should return boolean");
    }

    @Test
    @DisplayName("should have isNone method")
    void shouldHaveIsNoneMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("isNone");
      assertNotNull(method, "isNone method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNone should return boolean");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("get");
      assertNotNull(method, "get method should exist");
      assertEquals(WitValue.class, method.getReturnType(), "get should return WitValue");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getValue should return Optional");
    }

    @Test
    @DisplayName("should have getInnerType method")
    void shouldHaveGetInnerTypeMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("getInnerType");
      assertNotNull(method, "getInnerType method should exist");
      assertEquals(WitType.class, method.getReturnType(), "getInnerType should return WitType");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning Optional")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Optional.class, method.getReturnType(), "toJava should return Optional");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitOption.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
