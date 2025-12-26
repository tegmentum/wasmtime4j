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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitEnum} class.
 *
 * <p>WitEnum represents a WIT enum value (enumerated type).
 */
@DisplayName("WitEnum Tests")
class WitEnumTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitEnum.class.getModifiers()), "WitEnum should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitEnum.class.getModifiers()), "WitEnum should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(WitValue.class.isAssignableFrom(WitEnum.class), "WitEnum should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("of", WitType.class, String.class);
      assertNotNull(method, "of method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "of should be static");
      assertEquals(WitEnum.class, method.getReturnType(), "of should return WitEnum");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getDiscriminant method")
    void shouldHaveGetDiscriminantMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("getDiscriminant");
      assertNotNull(method, "getDiscriminant method should exist");
      assertEquals(String.class, method.getReturnType(), "getDiscriminant should return String");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method returning String")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(String.class, method.getReturnType(), "toJava should return String");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getDiscriminant should take no parameters")
    void getDiscriminantShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("getDiscriminant");
      assertEquals(0, method.getParameterCount(), "getDiscriminant should take no parameters");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitEnum.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
