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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitFlags} class.
 *
 * <p>WitFlags represents a WIT flags value (bitset type).
 */
@DisplayName("WitFlags Tests")
class WitFlagsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitFlags.class.getModifiers()), "WitFlags should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitFlags.class.getModifiers()), "WitFlags should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitFlags.class), "WitFlags should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method with varargs")
    void shouldHaveOfFactoryMethodWithVarargs() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("of", WitType.class, String[].class);
      assertNotNull(method, "of method with varargs should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitFlags.class, method.getReturnType(), "of should return WitFlags");
    }

    @Test
    @DisplayName("should have of factory method with Set")
    void shouldHaveOfFactoryMethodWithSet() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("of", WitType.class, Set.class);
      assertNotNull(method, "of method with Set should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitFlags.class, method.getReturnType(), "of should return WitFlags");
    }

    @Test
    @DisplayName("should have empty factory method")
    void shouldHaveEmptyFactoryMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("empty", WitType.class);
      assertNotNull(method, "empty method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "empty should be static");
      assertEquals(WitFlags.class, method.getReturnType(), "empty should return WitFlags");
    }

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("builder", WitType.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(WitFlags.Builder.class, method.getReturnType(), "builder should return Builder");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have isSet method")
    void shouldHaveIsSetMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("isSet", String.class);
      assertNotNull(method, "isSet method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isSet should return boolean");
    }

    @Test
    @DisplayName("should have getSetFlags method")
    void shouldHaveGetSetFlagsMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("getSetFlags");
      assertNotNull(method, "getSetFlags method should exist");
      assertEquals(Set.class, method.getReturnType(), "getSetFlags should return Set");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning Set")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Set.class, method.getReturnType(), "toJava should return Set");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be a nested class")
    void builderShouldBeNestedClass() {
      assertTrue(
          WitFlags.Builder.class.getDeclaringClass() == WitFlags.class,
          "Builder should be nested in WitFlags");
    }

    @Test
    @DisplayName("Builder should be public and final")
    void builderShouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(WitFlags.Builder.class.getModifiers()), "Builder should be public");
      assertTrue(
          Modifier.isFinal(WitFlags.Builder.class.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have set method")
    void builderShouldHaveSetMethod() throws NoSuchMethodException {
      final Method method = WitFlags.Builder.class.getMethod("set", String.class);
      assertNotNull(method, "set method should exist");
      assertEquals(WitFlags.Builder.class, method.getReturnType(), "set should return Builder");
    }

    @Test
    @DisplayName("Builder should have setAll method")
    void builderShouldHaveSetAllMethod() throws NoSuchMethodException {
      final Method method = WitFlags.Builder.class.getMethod("setAll", String[].class);
      assertNotNull(method, "setAll method should exist");
      assertEquals(WitFlags.Builder.class, method.getReturnType(), "setAll should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WitFlags.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WitFlags.class, method.getReturnType(), "build should return WitFlags");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitFlags.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
