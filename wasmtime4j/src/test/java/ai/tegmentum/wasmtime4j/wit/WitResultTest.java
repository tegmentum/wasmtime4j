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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
 * <p>WitResult represents a WIT result value (success or error), similar to Rust's Result type.
 */
@DisplayName("WitResult Tests")
class WitResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WitResult.class.getModifiers()), "WitResult should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitResult.class),
          "WitResult should extend WitValue");
    }

    @Test
    @DisplayName("should have isOk method")
    void shouldHaveIsOkMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("isOk");
      assertNotNull(method, "Should have isOk() method");
      assertEquals(
          boolean.class, method.getReturnType(), "isOk should return boolean");
    }

    @Test
    @DisplayName("should have isErr method")
    void shouldHaveIsErrMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("isErr");
      assertNotNull(method, "Should have isErr() method");
      assertEquals(
          boolean.class, method.getReturnType(), "isErr should return boolean");
    }

    @Test
    @DisplayName("should have getOk method")
    void shouldHaveGetOkMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getOk");
      assertNotNull(method, "Should have getOk() method");
      assertEquals(
          Optional.class, method.getReturnType(), "getOk should return Optional");
    }

    @Test
    @DisplayName("should have getErr method")
    void shouldHaveGetErrMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getErr");
      assertNotNull(method, "Should have getErr() method");
      assertEquals(
          Optional.class, method.getReturnType(), "getErr should return Optional");
    }

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("getValue");
      assertNotNull(method, "Should have getValue() method");
      assertEquals(
          Optional.class, method.getReturnType(), "getValue should return Optional");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have static ok method with value")
    void shouldHaveOkMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class, WitValue.class);
      assertNotNull(method, "Should have ok(WitType, WitValue) method");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static ok method without value")
    void shouldHaveOkMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class);
      assertNotNull(method, "Should have ok(WitType) method");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static err method with value")
    void shouldHaveErrMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class, WitValue.class);
      assertNotNull(method, "Should have err(WitType, WitValue) method");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "err method should be static");
    }

    @Test
    @DisplayName("should have static err method without value")
    void shouldHaveErrMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class);
      assertNotNull(method, "Should have err(WitType) method");
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "err method should be static");
    }

    @Test
    @DisplayName("ok with non-result type should throw")
    void okWithNonResultTypeShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(WitType.createS32()),
          "ok with non-result type should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("err with non-result type should throw")
    void errWithNonResultTypeShouldThrow() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(WitType.createS32()),
          "err with non-result type should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Ok/Err Result Tests")
  class OkErrResultTests {

    @Test
    @DisplayName("ok result without payload should be ok")
    void okResultWithoutPayloadShouldBeOk() {
      final WitType resultType = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(resultType);
      assertTrue(result.isOk(), "ok result should report isOk as true");
    }

    @Test
    @DisplayName("err result without payload should be err")
    void errResultWithoutPayloadShouldBeErr() {
      final WitType resultType = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(resultType);
      assertTrue(result.isErr(), "err result should report isErr as true");
    }

    @Test
    @DisplayName("ok result getOk should return empty for no-payload")
    void okResultGetOkShouldReturnEmptyForNoPayload() {
      final WitType resultType = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(resultType);
      assertEquals(
          Optional.empty(), result.getOk(),
          "ok result without payload getOk should be empty");
    }

    @Test
    @DisplayName("err result getOk should return empty")
    void errResultGetOkShouldReturnEmpty() {
      final WitType resultType = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(resultType);
      assertEquals(
          Optional.empty(), result.getOk(),
          "err result getOk should return empty");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same ok results should be equal")
    void sameOkResultsShouldBeEqual() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult r1 = WitResult.ok(rt);
      final WitResult r2 = WitResult.ok(rt);
      assertEquals(r1, r2, "Same ok results should be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same results should have same hash code")
    void sameResultsShouldHaveSameHashCode() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult r1 = WitResult.ok(rt);
      final WitResult r2 = WitResult.ok(rt);
      assertEquals(
          r1.hashCode(), r2.hashCode(),
          "Same results should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("ok toString should contain ok")
    void okToStringShouldContainOk() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);
      final String str = result.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("ok"), "ok result toString should contain 'ok'");
    }

    @Test
    @DisplayName("err toString should contain err")
    void errToStringShouldContainErr() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);
      final String str = result.toString();
      assertTrue(str.contains("err"), "err result toString should contain 'err'");
    }
  }
}
