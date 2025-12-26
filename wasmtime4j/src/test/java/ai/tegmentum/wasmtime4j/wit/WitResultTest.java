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
 * Tests for {@link WitResult} class.
 *
 * <p>WitResult represents a WIT result value (success or error).
 */
@DisplayName("WitResult Tests")
class WitResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitResult.class.getModifiers()), "WitResult should be final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WitResult.class.getModifiers()), "WitResult should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitResult.class), "WitResult should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have ok factory method with value")
    void shouldHaveOkFactoryMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class, WitValue.class);
      assertNotNull(method, "ok method with value should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(WitResult.class, method.getReturnType(), "ok should return WitResult");
    }

    @Test
    @DisplayName("should have ok factory method without value")
    void shouldHaveOkFactoryMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class);
      assertNotNull(method, "ok method without value should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(WitResult.class, method.getReturnType(), "ok should return WitResult");
    }

    @Test
    @DisplayName("should have err factory method with value")
    void shouldHaveErrFactoryMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class, WitValue.class);
      assertNotNull(method, "err method with value should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(WitResult.class, method.getReturnType(), "err should return WitResult");
    }

    @Test
    @DisplayName("should have err factory method without value")
    void shouldHaveErrFactoryMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class);
      assertNotNull(method, "err method without value should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(WitResult.class, method.getReturnType(), "err should return WitResult");
    }
  }

  @Nested
  @DisplayName("State Check Method Tests")
  class StateCheckMethodTests {

    @Test
    @DisplayName("should have isOk method")
    void shouldHaveIsOkMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("isOk");
      assertNotNull(method, "isOk method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isOk should return boolean");
    }

    @Test
    @DisplayName("should have isErr method")
    void shouldHaveIsErrMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("isErr");
      assertNotNull(method, "isErr method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isErr should return boolean");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getOk method")
    void shouldHaveGetOkMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getOk");
      assertNotNull(method, "getOk method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getOk should return Optional");
    }

    @Test
    @DisplayName("should have getErr method")
    void shouldHaveGetErrMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getErr");
      assertNotNull(method, "getErr method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getErr should return Optional");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getValue should return Optional");
    }
  }

  @Nested
  @DisplayName("ToJava Method Tests")
  class ToJavaMethodTests {

    @Test
    @DisplayName("should have toJava method")
    void shouldHaveToJavaMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("toJava");
      assertNotNull(method, "toJava method should exist");
      assertEquals(Object.class, method.getReturnType(), "toJava should return Object");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("should have equals method")
    void shouldHaveEqualsMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
    }

    @Test
    @DisplayName("should have hashCode method")
    void shouldHaveHashCodeMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
    }
  }
}
