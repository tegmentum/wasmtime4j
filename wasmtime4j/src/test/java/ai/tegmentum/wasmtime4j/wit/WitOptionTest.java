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

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitOption} class.
 *
 * <p>WitOption represents a WIT option value (nullable value), similar to Java Optional.
 */
@DisplayName("WitOption Tests")
class WitOptionTest {

  private WitType createOptionS32Type() {
    return WitType.option(WitType.createS32());
  }

  @Nested
  @DisplayName("Some Tests")
  class SomeTests {

    @Test
    @DisplayName("some should create option with value")
    void someShouldCreateOptionWithValue() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.some(ot, WitS32.of(42));
      assertNotNull(opt, "WitOption.some should create non-null option");
      assertTrue(opt.isSome(), "some option should report isSome as true");
      assertFalse(opt.isNone(), "some option should report isNone as false");
    }

    @Test
    @DisplayName("some get should return inner value")
    void someGetShouldReturnInnerValue() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.some(ot, WitS32.of(42));
      assertEquals(WitS32.of(42), opt.get(), "get should return the inner value");
    }

    @Test
    @DisplayName("some getValue should return present Optional")
    void someGetValueShouldReturnPresentOptional() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.some(ot, WitS32.of(42));
      assertTrue(opt.getValue().isPresent(), "getValue should return present Optional for some");
    }

    @Test
    @DisplayName("some with null value should throw")
    void someWithNullValueShouldThrow() {
      final WitType ot = createOptionS32Type();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOption.some(ot, null),
          "some with null value should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("None Tests")
  class NoneTests {

    @Test
    @DisplayName("none should create empty option")
    void noneShouldCreateEmptyOption() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      assertNotNull(opt, "WitOption.none should create non-null option");
      assertTrue(opt.isNone(), "none option should report isNone as true");
      assertFalse(opt.isSome(), "none option should report isSome as false");
    }

    @Test
    @DisplayName("none get should throw NoSuchElementException")
    void noneGetShouldThrow() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      assertThrows(
          NoSuchElementException.class,
          opt::get,
          "get on none should throw NoSuchElementException");
    }

    @Test
    @DisplayName("none getValue should return empty Optional")
    void noneGetValueShouldReturnEmpty() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      assertFalse(opt.getValue().isPresent(), "getValue should return empty Optional for none");
    }
  }

  @Nested
  @DisplayName("Of Tests")
  class OfTests {

    @Test
    @DisplayName("of with present Optional should create some")
    void ofWithPresentShouldCreateSome() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.of(ot, Optional.of(WitS32.of(10)));
      assertTrue(opt.isSome(), "of with present Optional should create some");
    }

    @Test
    @DisplayName("of with empty Optional should create none")
    void ofWithEmptyShouldCreateNone() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.of(ot, Optional.empty());
      assertTrue(opt.isNone(), "of with empty Optional should create none");
    }
  }

  @Nested
  @DisplayName("InnerType Tests")
  class InnerTypeTests {

    @Test
    @DisplayName("getInnerType should return non-null")
    void getInnerTypeShouldReturnNonNull() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      assertNotNull(opt.getInnerType(), "getInnerType should return non-null type");
    }
  }

  @Nested
  @DisplayName("ToJava Tests")
  class ToJavaTests {

    @Test
    @DisplayName("toJava of some should return present Optional")
    void toJavaOfSomeShouldReturnPresent() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.some(ot, WitS32.of(42));
      final Object javaValue = opt.toJava();
      assertTrue(javaValue instanceof Optional, "toJava should return Optional");
    }

    @Test
    @DisplayName("toJava of none should return empty Optional")
    void toJavaOfNoneShouldReturnEmpty() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      final Object javaValue = opt.toJava();
      assertTrue(javaValue instanceof Optional, "toJava should return Optional");
      assertFalse(((Optional<?>) javaValue).isPresent(), "toJava of none should be empty Optional");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same some values should be equal")
    void sameSomeValuesShouldBeEqual() {
      final WitType ot = createOptionS32Type();
      final WitOption o1 = WitOption.some(ot, WitS32.of(42));
      final WitOption o2 = WitOption.some(ot, WitS32.of(42));
      assertEquals(o1, o2, "Same some values should be equal");
    }

    @Test
    @DisplayName("none options should be equal")
    void noneOptionsShouldBeEqual() {
      final WitType ot = createOptionS32Type();
      final WitOption o1 = WitOption.none(ot);
      final WitOption o2 = WitOption.none(ot);
      assertEquals(o1, o2, "None options should be equal");
    }

    @Test
    @DisplayName("some and none should not be equal")
    void someAndNoneShouldNotBeEqual() {
      final WitType ot = createOptionS32Type();
      final WitOption some = WitOption.some(ot, WitS32.of(42));
      final WitOption none = WitOption.none(ot);
      assertNotEquals(some, none, "Some and none should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same options should have same hash code")
    void sameOptionsShouldHaveSameHashCode() {
      final WitType ot = createOptionS32Type();
      final WitOption o1 = WitOption.some(ot, WitS32.of(42));
      final WitOption o2 = WitOption.some(ot, WitS32.of(42));
      assertEquals(o1.hashCode(), o2.hashCode(), "Same options should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("some toString should contain some")
    void someToStringShouldContainSome() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.some(ot, WitS32.of(42));
      final String str = opt.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("some"), "some toString should contain 'some'");
    }

    @Test
    @DisplayName("none toString should contain none")
    void noneToStringShouldContainNone() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      final String str = opt.toString();
      assertTrue(str.contains("none"), "none toString should contain 'none'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitType ot = createOptionS32Type();
      final WitOption opt = WitOption.none(ot);
      assertNotNull(opt.getType(), "Should have WitType");
    }
  }

  @Nested
  @DisplayName("Mutation Killing Tests")
  class MutationKillingTests {

    @Test
    @DisplayName("isSome must return true for some and false for none - exact boolean values")
    void isSomeMutationTest() {
      final WitType ot = createOptionS32Type();

      // Some option isSome() must return exactly true
      final WitOption some = WitOption.some(ot, WitS32.of(42));
      assertTrue(some.isSome(), "isSome() on some option must return exactly true");
      assertFalse(!some.isSome(), "isSome() result must be true, not false");

      // None option isSome() must return exactly false
      final WitOption none = WitOption.none(ot);
      assertFalse(none.isSome(), "isSome() on none option must return exactly false");
      assertTrue(!none.isSome(), "isSome() result must be false, not true");
    }

    @Test
    @DisplayName("isNone must return true for none and false for some - exact boolean values")
    void isNoneMutationTest() {
      final WitType ot = createOptionS32Type();

      // None option isNone() must return exactly true
      final WitOption none = WitOption.none(ot);
      assertTrue(none.isNone(), "isNone() on none option must return exactly true");
      assertFalse(!none.isNone(), "isNone() result must be true, not false");

      // Some option isNone() must return exactly false
      final WitOption some = WitOption.some(ot, WitS32.of(42));
      assertFalse(some.isNone(), "isNone() on some option must return exactly false");
      assertTrue(!some.isNone(), "isNone() result must be false, not true");
    }

    @Test
    @DisplayName("isSome and isNone must be logical inverses")
    void isSomeIsNoneInverseMutationTest() {
      final WitType ot = createOptionS32Type();

      final WitOption some = WitOption.some(ot, WitS32.of(42));
      assertTrue(some.isSome() && !some.isNone(), "some: isSome=true AND isNone=false");
      assertFalse(some.isSome() == some.isNone(), "isSome and isNone must be inverses for some");

      final WitOption none = WitOption.none(ot);
      assertTrue(!none.isSome() && none.isNone(), "none: isSome=false AND isNone=true");
      assertFalse(none.isSome() == none.isNone(), "isSome and isNone must be inverses for none");
    }

    @Test
    @DisplayName("getValue must return Optional with correct presence")
    void getValueMutationTest() {
      final WitType ot = createOptionS32Type();

      // Some option getValue() must return present Optional
      final WitOption some = WitOption.some(ot, WitS32.of(42));
      assertTrue(some.getValue().isPresent(), "getValue() on some must be present");
      assertEquals(WitS32.of(42), some.getValue().get(), "getValue() must contain correct value");

      // None option getValue() must return empty Optional
      final WitOption none = WitOption.none(ot);
      assertFalse(none.getValue().isPresent(), "getValue() on none must not be present");
      assertTrue(none.getValue().isEmpty(), "getValue() on none must be empty");
    }

    @Test
    @DisplayName("toJava must return Optional with correct structure")
    void toJavaMutationTest() {
      final WitType ot = createOptionS32Type();

      // Some option toJava() must return present Optional with value
      final WitOption some = WitOption.some(ot, WitS32.of(42));
      final Optional<Object> someJava = some.toJava();
      assertTrue(someJava.isPresent(), "toJava() on some must return present Optional");
      assertEquals(42, someJava.get(), "toJava() value must be unwrapped to Java type");

      // None option toJava() must return empty Optional
      final WitOption none = WitOption.none(ot);
      final Optional<Object> noneJava = none.toJava();
      assertFalse(noneJava.isPresent(), "toJava() on none must return empty Optional");
      assertTrue(noneJava.isEmpty(), "toJava() on none must be empty");
    }

    @Test
    @DisplayName("type mismatch in some must be rejected")
    void typeMismatchMutationTest() {
      // Option expects S32 but we provide U64
      final WitType optS32Type = WitType.option(WitType.createS32());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOption.some(optS32Type, WitU64.of(100L)),
          "Type mismatch in some must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("equals must handle edge cases correctly")
    void equalsMutationTest() {
      final WitType ot = createOptionS32Type();
      final WitOption some = WitOption.some(ot, WitS32.of(42));

      // Reflexive - same object
      assertTrue(some.equals(some), "equals(self) must return true");

      // Null comparison
      assertFalse(some.equals(null), "equals(null) must return false");

      // Different type
      assertFalse(some.equals("option"), "equals(String) must return false");
      assertFalse(some.equals(42), "equals(Integer) must return false");

      // Some vs none
      final WitOption none = WitOption.none(ot);
      assertFalse(some.equals(none), "some.equals(none) must return false");
      assertFalse(none.equals(some), "none.equals(some) must return false");

      // Different some values
      final WitOption some2 = WitOption.some(ot, WitS32.of(99));
      assertFalse(some.equals(some2), "some(42).equals(some(99)) must return false");
    }

    @Test
    @DisplayName("of with null Optional should create none")
    void ofWithNullOptionalMutationTest() {
      final WitType ot = createOptionS32Type();

      // of() with null Optional should create none (null handled as empty)
      final WitOption opt = WitOption.of(ot, null);
      assertTrue(opt.isNone(), "of(null) should create none option");
      assertFalse(opt.isSome(), "of(null) should not create some option");
    }
  }

  @Nested
  @DisplayName("Surviving Mutant Killer Tests")
  class SurvivingMutantKillerTests {

    @Test
    @DisplayName("extractInnerType must verify kind is not null - line 174")
    void extractInnerTypeMustVerifyKindNotNull() {
      // Targets line 174: optionType.getKind() == null
      final WitType primitiveType = WitType.createS32();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOption.none(primitiveType),
          "Should reject non-option type");
    }

    @Test
    @DisplayName("extractInnerType must verify category is OPTION - line 175")
    void extractInnerTypeMustVerifyCategoryIsOption() {
      // Targets line 175: getCategory() != WitTypeCategory.OPTION
      final WitType listType = WitType.list(WitType.createS32());
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOption.none(listType),
          "Should reject list type as option type");
    }

    @Test
    @DisplayName("validate must check type equality - lines 156, 159")
    void validateMustCheckTypeEquality() {
      // Targets lines 156, 159: !v.getType().equals(innerType)
      final WitType optS32Type = WitType.option(WitType.createS32());

      // Correct type must work
      final WitOption valid = WitOption.some(optS32Type, WitS32.of(42));
      assertEquals(WitS32.of(42), valid.get(), "Correct type should be accepted");

      // Wrong type must be rejected
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WitOption.some(optS32Type, WitString.of("wrong")),
              "Wrong type must be rejected");
      assertTrue(
          ex.getMessage().contains("type") || ex.getMessage().contains("expected"),
          "Error should mention type: " + ex.getMessage());
    }

    @Test
    @DisplayName("getInnerType must return the extracted inner type")
    void getInnerTypeMustReturnExtractedType() {
      // Validates that extractInnerType correctly extracts the inner type
      final WitType optS32Type = WitType.option(WitType.createS32());
      final WitOption opt = WitOption.none(optS32Type);
      assertEquals(
          WitType.createS32(),
          opt.getInnerType(),
          "getInnerType must return the inner type from the option type");
    }

    @Test
    @DisplayName("validate correctly uses innerType not some other type")
    void validateCorrectlyUsesInnerType() throws Exception {
      // Tests that validate uses the correct innerType (not a different index or wrong type)
      final WitType optStringType = WitType.option(WitType.createString());

      // String value should work with option<string>
      final WitOption valid = WitOption.some(optStringType, WitString.of("hello"));
      assertEquals("hello", ((WitString) valid.get()).getValue());

      // S32 value should NOT work with option<string>
      assertThrows(
          IllegalArgumentException.class,
          () -> WitOption.some(optStringType, WitS32.of(42)),
          "S32 should not be accepted for option<string>");
    }
  }
}
