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

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("ok with wrong-typed payload should throw")
    void okWithWrongTypedPayloadShouldThrow() {
      final WitType rt = WitType.result(Optional.of(WitType.createString()), Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt, WitS32.of(42)),
          "ok with s32 value on result<string, _> should throw");
    }

    @Test
    @DisplayName("err with wrong-typed payload should throw")
    void errWithWrongTypedPayloadShouldThrow() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(rt, WitString.of("wrong")),
          "err with string value on result<_, s32> should throw");
    }

    @Test
    @DisplayName("ok without value when ok type requires payload should throw")
    void okWithoutValueWhenOkTypeRequiresPayloadShouldThrow() {
      final WitType rt = WitType.result(Optional.of(WitType.createString()), Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt),
          "ok() without value on result<string, _> should throw");
    }

    @Test
    @DisplayName("err without value when error type requires payload should throw")
    void errWithoutValueWhenErrorTypeRequiresPayloadShouldThrow() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(rt),
          "err() without value on result<_, s32> should throw");
    }

    @Test
    @DisplayName("ok with value when ok type is absent should throw")
    void okWithValueWhenOkTypeAbsentShouldThrow() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt, WitS32.of(1)),
          "ok with value on result<_, _> should throw");
    }
  }

  @Nested
  @DisplayName("GetOk and GetErr with Payload Tests")
  class GetOkGetErrWithPayloadTests {

    @Test
    @DisplayName("getOk on ok result with payload should return the value")
    void getOkOnOkWithPayloadShouldReturnValue() {
      final WitType rt = WitType.result(Optional.of(WitType.createS32()), Optional.empty());
      final WitResult ok = WitResult.ok(rt, WitS32.of(42));

      assertTrue(ok.getOk().isPresent(), "getOk should be present");
      assertEquals(42, ((WitS32) ok.getOk().get()).getValue(), "getOk value should be 42");
    }

    @Test
    @DisplayName("getOk on err result should return empty")
    void getOkOnErrShouldReturnEmpty() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      final WitResult err = WitResult.err(rt, WitS32.of(99));

      assertFalse(err.getOk().isPresent(), "getOk on err result should be empty");
    }

    @Test
    @DisplayName("getErr on err result with payload should return the value")
    void getErrOnErrWithPayloadShouldReturnValue() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      final WitResult err = WitResult.err(rt, WitS32.of(99));

      assertTrue(err.getErr().isPresent(), "getErr should be present");
      assertEquals(99, ((WitS32) err.getErr().get()).getValue(), "getErr value should be 99");
    }

    @Test
    @DisplayName("getErr on ok result should return empty")
    void getErrOnOkShouldReturnEmpty() {
      final WitType rt = WitType.result(Optional.of(WitType.createS32()), Optional.empty());
      final WitResult ok = WitResult.ok(rt, WitS32.of(42));

      assertFalse(ok.getErr().isPresent(), "getErr on ok result should be empty");
    }
  }

  @Nested
  @DisplayName("ToJava with Payload Tests")
  class ToJavaWithPayloadTests {

    @Test
    @DisplayName("toJava for ok with payload should include ok key")
    void toJavaOkWithPayloadShouldIncludeOkKey() {
      final WitType rt = WitType.result(Optional.of(WitType.createS32()), Optional.empty());
      final WitResult ok = WitResult.ok(rt, WitS32.of(42));

      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) ok.toJava();
      assertEquals(true, map.get("isOk"), "Should be ok");
      assertEquals(42, map.get("ok"), "Should contain ok value");
    }

    @Test
    @DisplayName("toJava for err with payload should include err key")
    void toJavaErrWithPayloadShouldIncludeErrKey() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      final WitResult err = WitResult.err(rt, WitS32.of(99));

      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) err.toJava();
      assertEquals(false, map.get("isOk"), "Should be err");
      assertEquals(99, map.get("err"), "Should contain err value");
    }
  }

  @Nested
  @DisplayName("Constructor Null Handling Tests")
  class ConstructorNullHandlingTests {

    @Test
    @DisplayName("ok factory with null value treats null as empty via Optional.ofNullable")
    void okFactoryNullValueProducesEmptyValue() {
      // This targets the constructor null check at line 67: value == null ? Optional.empty() :
      // value
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult result = WitResult.ok(rt, null);
      assertTrue(result.isOk(), "Should be ok");
      assertFalse(result.getValue().isPresent(), "Null value should become Optional.empty()");
      assertEquals(Optional.empty(), result.getValue(), "Value must be Optional.empty not null");
    }
  }

  @Nested
  @DisplayName("ExtractTypes Validation Tests")
  class ExtractTypesValidationTests {

    @Test
    @DisplayName("extractTypes must verify kind is not null")
    void extractTypesMustVerifyKindNotNull() {
      // This targets line 210: resultType.getKind() == null check
      // A non-result type (like s32) will have a kind that is not RESULT category
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(WitType.createS32(), WitS32.of(1)),
          "Should reject non-result type");
    }

    @Test
    @DisplayName("extractTypes must verify category is RESULT")
    void extractTypesMustVerifyCategoryIsResult() {
      // This targets line 211: getCategory() != WitTypeCategory.RESULT
      final WitType listType = WitType.list(WitType.createS32());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(listType),
          "Should reject list type as result type");
    }

    @Test
    @DisplayName("extractTypes returns correct ok and error types")
    void extractTypesReturnsCorrectOkAndErrorTypes() throws Exception {
      // This targets line 215: the array indexing [0] and [1]
      final WitType rt =
          WitType.result(Optional.of(WitType.createS32()), Optional.of(WitType.createString()));

      // Verify ok type is S32 by passing correct type
      final WitResult ok = WitResult.ok(rt, WitS32.of(42));
      assertTrue(ok.isOk(), "Should be ok");
      assertEquals(42, ((WitS32) ok.getOk().get()).getValue(), "Ok value should be 42");

      // Verify error type is String by passing correct type
      final WitResult err = WitResult.err(rt, WitString.of("fail"));
      assertTrue(err.isErr(), "Should be err");
      assertEquals("fail", ((WitString) err.getErr().get()).getValue(), "Err value should be fail");

      // Verify wrong ok type is rejected (proves ok type is extracted correctly at index 0)
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt, WitString.of("wrong")),
          "Should reject string as ok value when ok type is s32");

      // Verify wrong error type is rejected (proves error type is extracted correctly at index 1)
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(rt, WitS32.of(1)),
          "Should reject s32 as error value when error type is string");
    }
  }

  @Nested
  @DisplayName("ToJava Map Size Tests")
  class ToJavaMapSizeTests {

    @Test
    @DisplayName("toJava for ok without payload should have exactly 1 entry")
    void toJavaOkWithoutPayloadShouldHaveOneEntry() {
      // This targets line 165: HashMap(2) constant mutation
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok = WitResult.ok(rt);

      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) ok.toJava();
      assertEquals(1, map.size(), "Map should have exactly 1 entry (isOk only)");
      assertTrue(map.containsKey("isOk"), "Map must contain isOk key");
      assertFalse(map.containsKey("ok"), "Map must not contain ok key when no payload");
      assertFalse(map.containsKey("err"), "Map must not contain err key");
    }

    @Test
    @DisplayName("toJava for ok with payload should have exactly 2 entries")
    void toJavaOkWithPayloadShouldHaveTwoEntries() {
      final WitType rt = WitType.result(Optional.of(WitType.createS32()), Optional.empty());
      final WitResult ok = WitResult.ok(rt, WitS32.of(42));

      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) ok.toJava();
      assertEquals(2, map.size(), "Map should have exactly 2 entries (isOk + ok)");
      assertTrue(map.containsKey("isOk"), "Map must contain isOk key");
      assertTrue(map.containsKey("ok"), "Map must contain ok key for ok payload");
    }

    @Test
    @DisplayName("toJava for err with payload should have exactly 2 entries with err key")
    void toJavaErrWithPayloadShouldHaveTwoEntriesWithErrKey() {
      final WitType rt = WitType.result(Optional.empty(), Optional.of(WitType.createS32()));
      final WitResult err = WitResult.err(rt, WitS32.of(99));

      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) err.toJava();
      assertEquals(2, map.size(), "Map should have exactly 2 entries (isOk + err)");
      assertTrue(map.containsKey("err"), "Map must contain err key not ok key");
      assertFalse(map.containsKey("ok"), "Map must not contain ok key for err result");
    }
  }

  @Nested
  @DisplayName("Validate Index Math Tests")
  class ValidateIndexMathTests {

    @Test
    @DisplayName("validate checks exact type match for ok payload")
    void validateChecksExactTypeMatchForOkPayload() throws Exception {
      // Targets lines 185, 188 - type comparison and error message formatting
      final WitType rt =
          WitType.result(Optional.of(WitType.createString()), Optional.of(WitType.createS32()));

      // Correct ok type should work
      final WitResult ok = WitResult.ok(rt, WitString.of("hello"));
      assertEquals("hello", ((WitString) ok.getOk().get()).getValue());

      // Wrong ok type should fail
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitResult.ok(rt, WitS32.of(1)),
              "Should reject wrong type");
      assertTrue(ex.getMessage().contains("ok"), "Error message should mention ok variant");
    }

    @Test
    @DisplayName("validate checks exact type match for err payload")
    void validateChecksExactTypeMatchForErrPayload() {
      // Targets lines 185, 188 - type comparison and error message for err side
      final WitType rt =
          WitType.result(Optional.of(WitType.createString()), Optional.of(WitType.createS32()));

      // Correct error type should work
      final WitResult err = WitResult.err(rt, WitS32.of(404));
      assertEquals(404, ((WitS32) err.getErr().get()).getValue());

      // Wrong error type should fail
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitResult.err(rt, WitString.of("wrong")),
              "Should reject wrong type");
      assertTrue(ex.getMessage().contains("err"), "Error message should mention err variant");
    }

    @Test
    @DisplayName("validate rejects payload when type expects none")
    void validateRejectsPayloadWhenTypeExpectsNone() {
      // Targets line 194 - expectedType != null check in else branch
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt, WitS32.of(1)),
          "Should reject payload when ok type is absent");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(rt, WitS32.of(1)),
          "Should reject payload when err type is absent");
    }

    @Test
    @DisplayName("validate rejects missing payload when type requires one")
    void validateRejectsMissingPayloadWhenRequired() {
      // Targets line 194 - expectedType != null check
      final WitType rt =
          WitType.result(Optional.of(WitType.createS32()), Optional.of(WitType.createString()));
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.ok(rt),
          "Should require ok payload when ok type is present");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitResult.err(rt),
          "Should require err payload when error type is present");
    }
  }

  @Nested
  @DisplayName("ToJava and getValue Tests")
  class ToJavaAndGetValueTests {

    @Test
    @DisplayName("toJava for ok without payload should return Map with isOk=true")
    void toJavaOkWithoutPayloadShouldReturnMapWithIsOk() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok = WitResult.ok(rt);

      final Object java = ok.toJava();
      assertNotNull(java, "toJava should not return null");
      assertTrue(java instanceof java.util.Map, "toJava should return a Map");
      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) java;
      assertEquals(true, map.get("isOk"), "Map should have isOk=true");
    }

    @Test
    @DisplayName("toJava for err without payload should return Map with isOk=false")
    void toJavaErrWithoutPayloadShouldReturnMapWithIsOkFalse() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult err = WitResult.err(rt);

      final Object java = err.toJava();
      assertNotNull(java, "toJava should not return null");
      assertTrue(java instanceof java.util.Map, "toJava should return a Map");
      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> map = (java.util.Map<String, Object>) java;
      assertEquals(false, map.get("isOk"), "Map should have isOk=false");
    }

    @Test
    @DisplayName("getValue for ok should be same as getOk")
    void getValueForOkShouldBeSameAsGetOk() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok = WitResult.ok(rt);

      assertEquals(ok.getOk(), ok.getValue(), "getValue should return same as getOk for ok result");
    }

    @Test
    @DisplayName("getValue for err should be same as getErr")
    void getValueForErrShouldBeSameAsGetErr() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult err = WitResult.err(rt);

      assertEquals(
          err.getErr(), err.getValue(), "getValue should return same as getErr for err result");
    }

    @Test
    @DisplayName("ok factory with null value should create ok with empty value")
    void okFactoryWithNullValueShouldCreateEmptyOk() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult ok = WitResult.ok(rt, null);

      assertTrue(ok.isOk(), "Should be ok");
      assertFalse(ok.getValue().isPresent(), "Value should be empty for null");
    }

    @Test
    @DisplayName("err factory with null value should create err with empty value")
    void errFactoryWithNullValueShouldCreateEmptyErr() {
      final WitType rt = WitType.result(Optional.empty(), Optional.empty());
      final WitResult err = WitResult.err(rt, null);

      assertTrue(err.isErr(), "Should be err");
      assertFalse(err.getValue().isPresent(), "Value should be empty for null");
    }
  }
}
