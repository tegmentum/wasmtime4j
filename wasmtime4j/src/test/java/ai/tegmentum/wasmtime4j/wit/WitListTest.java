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
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitList} class.
 *
 * <p>WitList represents a WIT list value (variable-length array).
 */
@DisplayName("WitList Tests")
class WitListTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitList.class.getModifiers()), "WitList should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitList.class.getModifiers()), "WitList should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(WitValue.class.isAssignableFrom(WitList.class), "WitList should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method with varargs")
    void shouldHaveOfFactoryMethodWithVarargs() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("of", WitValue[].class);
      assertNotNull(method, "of method with varargs should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitList.class, method.getReturnType(), "of should return WitList");
    }

    @Test
    @DisplayName("should have of factory method with List")
    void shouldHaveOfFactoryMethodWithList() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("of", List.class);
      assertNotNull(method, "of method with List should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitList.class, method.getReturnType(), "of should return WitList");
    }

    @Test
    @DisplayName("should have empty factory method")
    void shouldHaveEmptyFactoryMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("empty", WitType.class);
      assertNotNull(method, "empty method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "empty should be static");
      assertEquals(WitList.class, method.getReturnType(), "empty should return WitList");
    }

    @Test
    @DisplayName("should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("builder", WitType.class);
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(WitList.Builder.class, method.getReturnType(), "builder should return Builder");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(WitType.class, method.getReturnType(), "getElementType should return WitType");
    }

    @Test
    @DisplayName("should have getElements method")
    void shouldHaveGetElementsMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("getElements");
      assertNotNull(method, "getElements method should exist");
      assertEquals(List.class, method.getReturnType(), "getElements should return List");
    }

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("get", int.class);
      assertNotNull(method, "get method should exist");
      assertEquals(WitValue.class, method.getReturnType(), "get should return WitValue");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning List")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(List.class, method.getReturnType(), "toJava should return List");
    }
  }

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("Builder should be a nested class")
    void builderShouldBeNestedClass() {
      assertTrue(
          WitList.Builder.class.getDeclaringClass() == WitList.class,
          "Builder should be nested in WitList");
    }

    @Test
    @DisplayName("Builder should be public and final")
    void builderShouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(WitList.Builder.class.getModifiers()), "Builder should be public");
      assertTrue(Modifier.isFinal(WitList.Builder.class.getModifiers()), "Builder should be final");
    }

    @Test
    @DisplayName("Builder should have add method")
    void builderShouldHaveAddMethod() throws NoSuchMethodException {
      final Method method = WitList.Builder.class.getMethod("add", WitValue.class);
      assertNotNull(method, "add method should exist");
      assertEquals(WitList.Builder.class, method.getReturnType(), "add should return Builder");
    }

    @Test
    @DisplayName("Builder should have addAll method")
    void builderShouldHaveAddAllMethod() throws NoSuchMethodException {
      final Method method = WitList.Builder.class.getMethod("addAll", List.class);
      assertNotNull(method, "addAll method should exist");
      assertEquals(WitList.Builder.class, method.getReturnType(), "addAll should return Builder");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = WitList.Builder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(WitList.class, method.getReturnType(), "build should return WitList");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitList.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
