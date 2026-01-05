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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResult} interface.
 *
 * <p>ComponentResult represents a Component Model result value (similar to Rust's Result type).
 */
@DisplayName("ComponentResult Tests")
class ComponentResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentResult.class.getModifiers()),
          "ComponentResult should be public");
      assertTrue(ComponentResult.class.isInterface(), "ComponentResult should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have isOk method")
    void shouldHaveIsOkMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("isOk");
      assertNotNull(method, "isOk method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isErr method")
    void shouldHaveIsErrMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("isErr");
      assertNotNull(method, "isErr method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getOk method")
    void shouldHaveGetOkMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("getOk");
      assertNotNull(method, "getOk method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getErr method")
    void shouldHaveGetErrMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("getErr");
      assertNotNull(method, "getErr method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have unwrap default method")
    void shouldHaveUnwrapDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("unwrap");
      assertNotNull(method, "unwrap method should exist");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
      assertTrue(method.isDefault(), "unwrap should be a default method");
    }

    @Test
    @DisplayName("should have unwrapErr default method")
    void shouldHaveUnwrapErrDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("unwrapErr");
      assertNotNull(method, "unwrapErr method should exist");
      assertEquals(ComponentVal.class, method.getReturnType(), "Should return ComponentVal");
      assertTrue(method.isDefault(), "unwrapErr should be a default method");
    }

    @Test
    @DisplayName("should have map default method")
    void shouldHaveMapDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("map", Function.class);
      assertNotNull(method, "map method should exist");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
      assertTrue(method.isDefault(), "map should be a default method");
    }

    @Test
    @DisplayName("should have mapErr default method")
    void shouldHaveMapErrDefaultMethod() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("mapErr", Function.class);
      assertNotNull(method, "mapErr method should exist");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
      assertTrue(method.isDefault(), "mapErr should be a default method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have ok static method with value")
    void shouldHaveOkStaticMethodWithValue() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("ok", ComponentVal.class);
      assertNotNull(method, "ok method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
    }

    @Test
    @DisplayName("should have ok static method without value")
    void shouldHaveOkStaticMethodWithoutValue() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("ok");
      assertNotNull(method, "ok method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok should be static");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
    }

    @Test
    @DisplayName("should have err static method with value")
    void shouldHaveErrStaticMethodWithValue() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("err", ComponentVal.class);
      assertNotNull(method, "err method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
    }

    @Test
    @DisplayName("should have err static method without value")
    void shouldHaveErrStaticMethodWithoutValue() throws NoSuchMethodException {
      final Method method = ComponentResult.class.getMethod("err");
      assertNotNull(method, "err method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err should be static");
      assertEquals(ComponentResult.class, method.getReturnType(), "Should return ComponentResult");
    }
  }

  @Nested
  @DisplayName("Impl Nested Class Tests")
  class ImplNestedClassTests {

    @Test
    @DisplayName("should have Impl nested class")
    void shouldHaveImplNestedClass() {
      final var nestedClasses = ComponentResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Impl")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Impl should be a class");
          assertTrue(
              ComponentResult.class.isAssignableFrom(nestedClass),
              "Impl should implement ComponentResult");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Impl should be final");
          break;
        }
      }
      assertTrue(found, "Should have Impl nested class");
    }
  }

  @Nested
  @DisplayName("Ok Result Behavior Tests")
  class OkResultBehaviorTests {

    @Test
    @DisplayName("ok result without value should be ok")
    void okResultWithoutValueShouldBeOk() {
      final ComponentResult result = ComponentResult.ok();

      assertTrue(result.isOk(), "Should be ok");
      assertFalse(result.isErr(), "Should not be err");
    }

    @Test
    @DisplayName("ok result without value should have empty optional")
    void okResultWithoutValueShouldHaveEmptyOptional() {
      final ComponentResult result = ComponentResult.ok();

      assertTrue(result.getOk().isEmpty(), "getOk should be empty");
      assertTrue(result.getErr().isEmpty(), "getErr should be empty for ok result");
    }

    @Test
    @DisplayName("ok result with null value should have empty optional")
    void okResultWithNullValueShouldHaveEmptyOptional() {
      final ComponentResult result = ComponentResult.ok(null);

      assertTrue(result.isOk(), "Should be ok");
      assertTrue(result.getOk().isEmpty(), "getOk should be empty for null value");
    }

    @Test
    @DisplayName("unwrap on ok result should return null for empty result")
    void unwrapOnOkResultShouldReturnNullForEmptyResult() {
      final ComponentResult result = ComponentResult.ok();

      assertNull(result.unwrap(), "unwrap should return null for empty ok result");
    }

    @Test
    @DisplayName("unwrapErr on ok result should throw exception")
    void unwrapErrOnOkResultShouldThrowException() {
      final ComponentResult result = ComponentResult.ok();

      assertThrows(
          IllegalStateException.class,
          result::unwrapErr,
          "unwrapErr on ok result should throw IllegalStateException");
    }
  }

  @Nested
  @DisplayName("Err Result Behavior Tests")
  class ErrResultBehaviorTests {

    @Test
    @DisplayName("err result without value should be err")
    void errResultWithoutValueShouldBeErr() {
      final ComponentResult result = ComponentResult.err();

      assertFalse(result.isOk(), "Should not be ok");
      assertTrue(result.isErr(), "Should be err");
    }

    @Test
    @DisplayName("err result without value should have empty optional")
    void errResultWithoutValueShouldHaveEmptyOptional() {
      final ComponentResult result = ComponentResult.err();

      assertTrue(result.getOk().isEmpty(), "getOk should be empty for err result");
      assertTrue(result.getErr().isEmpty(), "getErr should be empty for null error");
    }

    @Test
    @DisplayName("err result with null value should have empty optional")
    void errResultWithNullValueShouldHaveEmptyOptional() {
      final ComponentResult result = ComponentResult.err(null);

      assertTrue(result.isErr(), "Should be err");
      assertTrue(result.getErr().isEmpty(), "getErr should be empty for null error value");
    }

    @Test
    @DisplayName("unwrap on err result should throw exception")
    void unwrapOnErrResultShouldThrowException() {
      final ComponentResult result = ComponentResult.err();

      assertThrows(
          IllegalStateException.class,
          result::unwrap,
          "unwrap on err result should throw IllegalStateException");
    }

    @Test
    @DisplayName("unwrapErr on err result should return null for empty result")
    void unwrapErrOnErrResultShouldReturnNullForEmptyResult() {
      final ComponentResult result = ComponentResult.err();

      assertNull(result.unwrapErr(), "unwrapErr should return null for empty err result");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("two ok results without values should be equal")
    void twoOkResultsWithoutValuesShouldBeEqual() {
      final ComponentResult result1 = ComponentResult.ok();
      final ComponentResult result2 = ComponentResult.ok();

      assertEquals(result1, result2, "Two ok results without values should be equal");
      assertEquals(result1.hashCode(), result2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("two err results without values should be equal")
    void twoErrResultsWithoutValuesShouldBeEqual() {
      final ComponentResult result1 = ComponentResult.err();
      final ComponentResult result2 = ComponentResult.err();

      assertEquals(result1, result2, "Two err results without values should be equal");
      assertEquals(result1.hashCode(), result2.hashCode(), "Hash codes should be equal");
    }

    @Test
    @DisplayName("ok and err results should not be equal")
    void okAndErrResultsShouldNotBeEqual() {
      final ComponentResult okResult = ComponentResult.ok();
      final ComponentResult errResult = ComponentResult.err();

      assertNotEquals(okResult, errResult, "Ok and err results should not be equal");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("ok result toString should contain ok")
    void okResultToStringShouldContainOk() {
      final ComponentResult result = ComponentResult.ok();

      assertTrue(result.toString().contains("ok"), "toString should contain 'ok'");
    }

    @Test
    @DisplayName("err result toString should contain err")
    void errResultToStringShouldContainErr() {
      final ComponentResult result = ComponentResult.err();

      assertTrue(result.toString().contains("err"), "toString should contain 'err'");
    }
  }
}
