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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitTuple} class.
 *
 * <p>WitTuple represents a WIT tuple value (fixed-size ordered collection).
 */
@DisplayName("WitTuple Tests")
class WitTupleTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitTuple.class.getModifiers()), "WitTuple should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitTuple.class.getModifiers()), "WitTuple should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitTuple.class), "WitTuple should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method with varargs")
    void shouldHaveOfFactoryMethodWithVarargs() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("of", WitValue[].class);
      assertNotNull(method, "of method with varargs should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitTuple.class, method.getReturnType(), "of should return WitTuple");
    }

    @Test
    @DisplayName("should have of factory method with List")
    void shouldHaveOfFactoryMethodWithList() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("of", List.class);
      assertNotNull(method, "of method with List should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitTuple.class, method.getReturnType(), "of should return WitTuple");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("get", int.class);
      assertNotNull(method, "get method should exist");
      assertEquals(WitValue.class, method.getReturnType(), "get should return WitValue");
    }

    @Test
    @DisplayName("should have getElements method")
    void shouldHaveGetElementsMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("getElements");
      assertNotNull(method, "getElements method should exist");
      assertEquals(List.class, method.getReturnType(), "getElements should return List");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning List")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(List.class, method.getReturnType(), "toJava should return List");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitTuple.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
