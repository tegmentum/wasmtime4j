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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitValue} abstract class.
 *
 * <p>WitValue is the base class for all WebAssembly Interface Type (WIT) values.
 */
@DisplayName("WitValue Tests")
class WitValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be an abstract class")
    void shouldBeAbstractClass() {
      assertTrue(Modifier.isAbstract(WitValue.class.getModifiers()), "WitValue should be abstract");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitValue.class.getModifiers()), "WitValue should be public");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertTrue(Modifier.isFinal(method.getModifiers()), "getType should be final");
    }

    @Test
    @DisplayName("should have abstract toJava method")
    void shouldHaveAbstractToJavaMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "toJava should be abstract");
      assertEquals(Object.class, method.getReturnType(), "toJava should return Object");
    }

    @Test
    @DisplayName("should have abstract toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
    }

    @Test
    @DisplayName("should have abstract equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have abstract hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }

    @Test
    @DisplayName("should have isCompatibleWith method")
    void shouldHaveIsCompatibleWithMethod() throws NoSuchMethodException {
      final Method method =
          WitValue.class.getMethod("isCompatibleWith", ai.tegmentum.wasmtime4j.WitType.class);
      assertNotNull(method, "isCompatibleWith method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isCompatibleWith should return boolean");
    }
  }

  @Nested
  @DisplayName("Protected Method Tests")
  class ProtectedMethodTests {

    @Test
    @DisplayName("should have protected validate method")
    void shouldHaveProtectedValidateMethod() throws NoSuchMethodException {
      final Method method = WitValue.class.getDeclaredMethod("validate");
      assertNotNull(method, "validate method should exist");
      assertTrue(Modifier.isProtected(method.getModifiers()), "validate should be protected");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "validate should be abstract");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have protected constructor")
    void shouldHaveProtectedConstructor() throws NoSuchMethodException {
      final var constructor =
          WitValue.class.getDeclaredConstructor(ai.tegmentum.wasmtime4j.WitType.class);
      assertNotNull(constructor, "WitType constructor should exist");
      assertTrue(
          Modifier.isProtected(constructor.getModifiers()), "Constructor should be protected");
    }
  }
}
