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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have static ok method with value")
    void shouldHaveOkMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class, WitValue.class);
      assertNotNull(method, "Should have ok(WitType, WitValue) method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static ok method without value")
    void shouldHaveOkMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("ok", WitType.class);
      assertNotNull(method, "Should have ok(WitType) method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "ok method should be static");
    }

    @Test
    @DisplayName("should have static err method with value")
    void shouldHaveErrMethodWithValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class, WitValue.class);
      assertNotNull(method, "Should have err(WitType, WitValue) method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err method should be static");
    }

    @Test
    @DisplayName("should have static err method without value")
    void shouldHaveErrMethodWithoutValue() throws NoSuchMethodException {
      final Method method = WitResult.class.getMethod("err", WitType.class);
      assertNotNull(method, "Should have err(WitType) method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "err method should be static");
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
          Optional.empty(), result.getOk(), "ok result without payload getOk should be empty");
    }

    @Test
    @DisplayName("err result getOk should return empty")
    void errResultGetOkShouldReturnEmpty() {
      final WitType resultType = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(resultType);
      assertEquals(Optional.empty(), result.getOk(), "err result getOk should return empty");
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
      assertEquals(r1.hashCode(), r2.hashCode(), "Same results should have same hash code");
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

  @Nested
  @DisplayName("isOk/isErr Mutation Tests")
  class IsOkIsErrMutationTests {

    @Test
    @DisplayName("ok result isOk must return true not false")
    void okResultIsOkMustReturnTrue() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);
      // This kills mutation: isOk() returning !isOk or false
      assertTrue(result.isOk(), "ok result isOk() must return exactly true");
      assertFalse(result.isErr(), "ok result isErr() must return exactly false");
    }

    @Test
    @DisplayName("err result isOk must return false not true")
    void errResultIsOkMustReturnFalse() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);
      // This kills mutation: isOk() returning isOk or true
      assertFalse(result.isOk(), "err result isOk() must return exactly false");
      assertTrue(result.isErr(), "err result isErr() must return exactly true");
    }

    @Test
    @DisplayName("isErr must be inverse of isOk for ok result")
    void isErrMustBeInverseOfIsOkForOkResult() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);
      // This kills mutation: isErr() returning isOk instead of !isOk
      assertEquals(!result.isOk(), result.isErr(), "isErr() must be exactly the inverse of isOk()");
    }

    @Test
    @DisplayName("isErr must be inverse of isOk for err result")
    void isErrMustBeInverseOfIsOkForErrResult() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);
      // This kills mutation: isErr() returning isOk instead of !isOk
      assertEquals(!result.isOk(), result.isErr(), "isErr() must be exactly the inverse of isOk()");
    }
  }

  @Nested
  @DisplayName("getOk/getErr Return Value Mutation Tests")
  class GetOkGetErrMutationTests {

    @Test
    @DisplayName("ok result without payload getOk returns empty")
    void okResultWithoutPayloadGetOkReturnsEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);

      // For no-payload ok, getOk returns the empty value (which is empty)
      assertFalse(
          result.getOk().isPresent(), "ok result without payload getOk() returns empty optional");
    }

    @Test
    @DisplayName("ok result getErr must return empty")
    void okResultGetErrMustReturnEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);

      // This kills mutation: getErr returning value when isOk is true
      assertFalse(result.getErr().isPresent(), "ok result getErr() must return empty Optional");
    }

    @Test
    @DisplayName("err result without payload getErr returns empty")
    void errResultWithoutPayloadGetErrReturnsEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);

      // For no-payload err, getErr returns the empty value (which is empty)
      assertFalse(
          result.getErr().isPresent(),
          "err result without payload getErr() returns empty optional");
    }

    @Test
    @DisplayName("err result getOk must return empty")
    void errResultGetOkMustReturnEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);

      // This kills mutation: getOk returning value when isOk is false
      assertFalse(result.getOk().isPresent(), "err result getOk() must return empty Optional");
    }

    @Test
    @DisplayName("ok and err results have different getOk behavior")
    void okAndErrResultsHaveDifferentGetOkBehavior() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());

      final WitResult okResult = WitResult.ok(rt);
      final WitResult errResult = WitResult.err(rt);

      // The isOk flag determines which accessor returns the value
      // For ok: getOk() should return the value, getErr() should be empty
      // For err: getErr() should return the value, getOk() should be empty
      // Since both have no payload, both return empty - but isOk flag differs
      assertTrue(okResult.isOk(), "ok result must have isOk=true");
      assertFalse(errResult.isOk(), "err result must have isOk=false");
    }
  }

  @Nested
  @DisplayName("getValue Mutation Tests")
  class GetValueMutationTests {

    @Test
    @DisplayName("ok result without payload getValue must return empty")
    void okResultWithoutPayloadGetValueMustReturnEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt);

      assertFalse(
          result.getValue().isPresent(), "ok result without payload getValue() should be empty");
    }

    @Test
    @DisplayName("err result without payload getValue must return empty")
    void errResultWithoutPayloadGetValueMustReturnEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.err(rt);

      assertFalse(
          result.getValue().isPresent(), "err result without payload getValue() should be empty");
    }

    @Test
    @DisplayName("getValue returns same result for both ok and err without payload")
    void getValueReturnsSameResultForBothWithoutPayload() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult okResult = WitResult.ok(rt);
      final WitResult errResult = WitResult.err(rt);

      // Both should return empty, but the isOk flag must differ
      assertEquals(
          okResult.getValue(),
          errResult.getValue(),
          "Both should have empty getValue when no payload");
      assertNotEquals(okResult.isOk(), errResult.isOk(), "But isOk flag must be different");
    }
  }

  @Nested
  @DisplayName("Equality Mutation Tests")
  class EqualityMutationTests {

    @Test
    @DisplayName("ok and err with same empty payload must not be equal")
    void okAndErrWithSameEmptyPayloadMustNotBeEqual() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult okResult = WitResult.ok(rt);
      final WitResult errResult = WitResult.err(rt);

      // This kills mutation: equals ignoring isOk field
      assertNotEquals(
          okResult, errResult, "ok and err results must not be equal even with same empty payload");
    }

    @Test
    @DisplayName("two ok results must be equal")
    void twoOkResultsMustBeEqual() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok1 = WitResult.ok(rt);
      final WitResult ok2 = WitResult.ok(rt);

      assertEquals(ok1, ok2, "Two ok results with same type must be equal");
    }

    @Test
    @DisplayName("two err results must be equal")
    void twoErrResultsMustBeEqual() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult err1 = WitResult.err(rt);
      final WitResult err2 = WitResult.err(rt);

      assertEquals(err1, err2, "Two err results with same type must be equal");
    }

    @Test
    @DisplayName("equals must use isOk in comparison")
    void equalsMustUseIsOkInComparison() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok = WitResult.ok(rt);
      final WitResult err = WitResult.err(rt);

      // Both have empty value, so only isOk differentiates them
      assertNotEquals(ok, err, "isOk field must be part of equals comparison");
      assertNotEquals(err, ok, "isOk field must be part of equals comparison (symmetric)");
    }
  }
}
