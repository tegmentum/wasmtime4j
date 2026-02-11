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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitVariant} class.
 *
 * <p>WitVariant represents a WIT variant value (tagged union / sum type).
 */
@DisplayName("WitVariant Tests")
class WitVariantTest {

  private WitType createVariantType() {
    final Map<String, Optional<WitType>> cases = new LinkedHashMap<>();
    cases.put("success", Optional.of(WitType.createS32()));
    cases.put("error", Optional.of(WitType.createString()));
    cases.put("pending", Optional.empty());
    return WitType.variant("status", cases);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitVariant.class.getModifiers()), "WitVariant should be final");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitVariant.class), "WitVariant should extend WitValue");
    }

    @Test
    @DisplayName("should have getCaseName method")
    void shouldHaveGetCaseNameMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getCaseName");
      assertNotNull(method, "Should have getCaseName() method");
      assertEquals(String.class, method.getReturnType(), "getCaseName should return String");
    }

    @Test
    @DisplayName("should have getPayload method")
    void shouldHaveGetPayloadMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("getPayload");
      assertNotNull(method, "Should have getPayload() method");
      assertEquals(Optional.class, method.getReturnType(), "getPayload should return Optional");
    }

    @Test
    @DisplayName("should have hasPayload method")
    void shouldHaveHasPayloadMethod() throws NoSuchMethodException {
      final Method method = WitVariant.class.getMethod("hasPayload");
      assertNotNull(method, "Should have hasPayload() method");
      assertEquals(boolean.class, method.getReturnType(), "hasPayload should return boolean");
    }
  }

  @Nested
  @DisplayName("Creation Tests")
  class CreationTests {

    @Test
    @DisplayName("of with payload should create variant")
    void ofWithPayloadShouldCreateVariant() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "success", WitS32.of(42));
      assertNotNull(variant, "Variant with payload should be created");
      assertEquals("success", variant.getCaseName(), "Case name should be 'success'");
      assertTrue(variant.hasPayload(), "Variant should have payload");
    }

    @Test
    @DisplayName("of without payload should create variant")
    void ofWithoutPayloadShouldCreateVariant() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "pending");
      assertNotNull(variant, "Variant without payload should be created");
      assertEquals("pending", variant.getCaseName(), "Case name should be 'pending'");
      assertFalse(variant.hasPayload(), "Variant should not have payload");
    }

    @Test
    @DisplayName("of with null case name should throw")
    void ofWithNullCaseNameShouldThrow() {
      final WitType vt = createVariantType();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitVariant.of(vt, null),
          "Null case name should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("of with empty case name should throw")
    void ofWithEmptyCaseNameShouldThrow() {
      final WitType vt = createVariantType();
      assertThrows(
          IllegalArgumentException.class,
          () -> WitVariant.of(vt, ""),
          "Empty case name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Payload Access Tests")
  class PayloadAccessTests {

    @Test
    @DisplayName("getPayload should return value for case with payload")
    void getPayloadShouldReturnValueForCaseWithPayload() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "success", WitS32.of(42));
      assertTrue(variant.getPayload().isPresent(), "getPayload should return present Optional");
      assertEquals(WitS32.of(42), variant.getPayload().get(), "Payload should be WitS32(42)");
    }

    @Test
    @DisplayName("getPayload should return empty for case without payload")
    void getPayloadShouldReturnEmptyForCaseWithoutPayload() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "pending");
      assertFalse(
          variant.getPayload().isPresent(),
          "getPayload should return empty Optional for no-payload case");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same variants should be equal")
    void sameVariantsShouldBeEqual() {
      final WitType vt = createVariantType();
      final WitVariant v1 = WitVariant.of(vt, "pending");
      final WitVariant v2 = WitVariant.of(vt, "pending");
      assertEquals(v1, v2, "Same variants should be equal");
    }

    @Test
    @DisplayName("different case names should not be equal")
    void differentCaseNamesShouldNotBeEqual() {
      final WitType vt = createVariantType();
      final WitVariant v1 = WitVariant.of(vt, "pending");
      final WitVariant v2 = WitVariant.of(vt, "success", WitS32.of(1));
      assertNotEquals(v1, v2, "Different case names should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same variants should have same hash code")
    void sameVariantsShouldHaveSameHashCode() {
      final WitType vt = createVariantType();
      final WitVariant v1 = WitVariant.of(vt, "pending");
      final WitVariant v2 = WitVariant.of(vt, "pending");
      assertEquals(v1.hashCode(), v2.hashCode(), "Same variants should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WitVariant")
    void toStringShouldContainClassName() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "pending");
      final String str = variant.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("WitVariant"), "toString should contain 'WitVariant'");
    }

    @Test
    @DisplayName("toString should contain case name")
    void toStringShouldContainCaseName() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "pending");
      assertTrue(variant.toString().contains("pending"), "toString should contain the case name");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "pending");
      assertNotNull(variant.getType(), "Should have WitType");
    }
  }

  @Nested
  @DisplayName("Mutation Killing Tests")
  class MutationKillingTests {

    @Test
    @DisplayName("hasPayload must return true for payload and false for no payload")
    void hasPayloadMutationTest() {
      final WitType vt = createVariantType();

      // Variant with payload must return true for hasPayload()
      final WitVariant withPayload = WitVariant.of(vt, "success", WitS32.of(42));
      assertTrue(withPayload.hasPayload(), "hasPayload() with payload must return exactly true");
      assertFalse(!withPayload.hasPayload(), "hasPayload() result must be true, not false");

      // Variant without payload must return false for hasPayload()
      final WitVariant noPayload = WitVariant.of(vt, "pending");
      assertFalse(noPayload.hasPayload(), "hasPayload() without payload must return exactly false");
      assertTrue(!noPayload.hasPayload(), "hasPayload() result must be false, not true");
    }

    @Test
    @DisplayName("getPayload must return correct Optional state")
    void getPayloadMutationTest() {
      final WitType vt = createVariantType();

      // Variant with payload getPayload() must return present Optional
      final WitVariant withPayload = WitVariant.of(vt, "success", WitS32.of(42));
      assertTrue(withPayload.getPayload().isPresent(), "getPayload() with payload must be present");
      assertEquals(
          WitS32.of(42), withPayload.getPayload().get(), "getPayload() must contain correct value");

      // Variant without payload getPayload() must return empty Optional
      final WitVariant noPayload = WitVariant.of(vt, "pending");
      assertFalse(noPayload.getPayload().isPresent(), "getPayload() without payload must be empty");
      assertTrue(
          noPayload.getPayload().isEmpty(), "getPayload() must be empty for no-payload case");
    }

    @Test
    @DisplayName("getCaseName must return exact case name")
    void getCaseNameMutationTest() {
      final WitType vt = createVariantType();

      final WitVariant success = WitVariant.of(vt, "success", WitS32.of(1));
      assertEquals("success", success.getCaseName(), "getCaseName() must return 'success'");
      assertNotEquals("error", success.getCaseName(), "getCaseName() must not return other case");
      assertNotEquals("pending", success.getCaseName(), "getCaseName() must not return other case");

      final WitVariant pending = WitVariant.of(vt, "pending");
      assertEquals("pending", pending.getCaseName(), "getCaseName() must return 'pending'");
    }

    @Test
    @DisplayName("toJava must return map with correct structure")
    void toJavaMutationTest() {
      final WitType vt = createVariantType();

      // Variant with payload
      final WitVariant withPayload = WitVariant.of(vt, "success", WitS32.of(42));
      final Object javaValue = withPayload.toJava();
      assertTrue(javaValue instanceof Map, "toJava must return Map");
      @SuppressWarnings("unchecked")
      final Map<String, Object> mapWithPayload = (Map<String, Object>) javaValue;
      assertEquals("success", mapWithPayload.get("case"), "toJava case key must be 'success'");
      assertEquals(42, mapWithPayload.get("payload"), "toJava payload key must be 42");

      // Variant without payload
      final WitVariant noPayload = WitVariant.of(vt, "pending");
      @SuppressWarnings("unchecked")
      final Map<String, Object> mapNoPayload = (Map<String, Object>) noPayload.toJava();
      assertEquals("pending", mapNoPayload.get("case"), "toJava case key must be 'pending'");
      assertFalse(mapNoPayload.containsKey("payload"), "toJava must not have payload key");
    }

    @Test
    @DisplayName("validation must reject invalid case names")
    void validationInvalidCaseMutationTest() {
      final WitType vt = createVariantType();

      // Non-existent case must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitVariant.of(vt, "unknown"),
          "Unknown case name must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("validation must reject payload when case doesn't expect one")
    void validationUnexpectedPayloadMutationTest() {
      final WitType vt = createVariantType();

      // pending case with payload must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitVariant.of(vt, "pending", WitS32.of(1)),
          "Payload for no-payload case must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("validation must reject type mismatch in payload")
    void validationTypeMismatchMutationTest() {
      final WitType vt = createVariantType();

      // success expects S32, providing String must throw
      assertThrows(
          IllegalArgumentException.class,
          () -> WitVariant.of(vt, "success", WitString.of("wrong")),
          "Wrong payload type must throw IllegalArgumentException");
    }

    @Test
    @DisplayName("equals must handle edge cases correctly")
    void equalsMutationTest() {
      final WitType vt = createVariantType();
      final WitVariant variant = WitVariant.of(vt, "success", WitS32.of(42));

      // Reflexive - same object
      assertTrue(variant.equals(variant), "equals(self) must return true");

      // Null comparison
      assertFalse(variant.equals(null), "equals(null) must return false");

      // Different type
      assertFalse(variant.equals("variant"), "equals(String) must return false");
      assertFalse(variant.equals(42), "equals(Integer) must return false");

      // Different case
      final WitVariant pending = WitVariant.of(vt, "pending");
      assertFalse(variant.equals(pending), "success.equals(pending) must return false");
      assertFalse(pending.equals(variant), "pending.equals(success) must return false");

      // Same case, different payload
      final WitVariant different = WitVariant.of(vt, "success", WitS32.of(99));
      assertFalse(variant.equals(different), "Different payload must not be equal");
    }
  }
}
