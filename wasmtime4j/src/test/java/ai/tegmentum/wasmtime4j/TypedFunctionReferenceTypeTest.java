package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TypedFunctionReferenceType;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypedFunctionReferenceType}.
 *
 * <p>Validates construction, equals/hashCode contract, toString format, and null handling.
 */
@DisplayName("TypedFunctionReferenceType Tests")
class TypedFunctionReferenceTypeTest {

  private static final Logger LOGGER =
      Logger.getLogger(TypedFunctionReferenceTypeTest.class.getName());

  private FunctionType createFunctionType(
      final WasmValueType[] params, final WasmValueType[] returns) {
    return new FunctionType(params, returns);
  }

  @Nested
  @DisplayName("Equals Correctness Tests")
  class EqualsCorrectnessTests {

    @Test
    @DisplayName("same reference should be equal")
    void sameReferenceShouldBeEqual() {
      final FunctionType funcType =
          createFunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      assertTrue(ref.equals(ref), "Same reference should be equal to itself");
      LOGGER.info("Identity equals verified");
    }

    @Test
    @DisplayName("two instances with same FunctionType reference should be equal")
    void sameBackingReferenceShouldBeEqual() {
      final FunctionType funcType =
          createFunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});
      final TypedFunctionReferenceType ref1 = new TypedFunctionReferenceType(funcType);
      final TypedFunctionReferenceType ref2 = new TypedFunctionReferenceType(funcType);

      assertTrue(ref1.equals(ref2), "Two instances sharing same FunctionType ref should be equal");
      assertTrue(ref2.equals(ref1), "equals should be symmetric");
      LOGGER.info("Same-reference equals verified");
    }

    @Test
    @DisplayName("null should not be equal")
    void nullShouldNotBeEqual() {
      final FunctionType funcType =
          createFunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      assertFalse(ref.equals(null), "Should not be equal to null");
      LOGGER.info("Null equals check verified");
    }

    @Test
    @DisplayName("different type should not be equal")
    void differentTypeShouldNotBeEqual() {
      final FunctionType funcType =
          createFunctionType(new WasmValueType[] {}, new WasmValueType[] {WasmValueType.I32});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      assertFalse(
          ref.equals("not a TypedFunctionReferenceType"), "Different type should not equal");
      assertFalse(ref.equals(42), "Integer should not equal");
      LOGGER.info("Different type equals check verified");
    }
  }

  @Nested
  @DisplayName("HashCode Correctness Tests")
  class HashCodeCorrectnessTests {

    @Test
    @DisplayName("same reference instances should have same hashCode")
    void sameReferenceInstancesShouldHaveSameHashCode() {
      final FunctionType funcType =
          createFunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64},
              new WasmValueType[] {WasmValueType.F32});
      final TypedFunctionReferenceType ref1 = new TypedFunctionReferenceType(funcType);
      final TypedFunctionReferenceType ref2 = new TypedFunctionReferenceType(funcType);

      assertEquals(ref1.hashCode(), ref2.hashCode(), "Equal objects should have same hashCode");
      LOGGER.info(
          "HashCode consistency verified: ref1=" + ref1.hashCode() + ", ref2=" + ref2.hashCode());
    }

    @Test
    @DisplayName("hashCode should be consistent across invocations")
    void hashCodeShouldBeConsistent() {
      final FunctionType funcType =
          createFunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      final int hash1 = ref.hashCode();
      final int hash2 = ref.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent across invocations");
      LOGGER.info("HashCode consistency: " + hash1 + " == " + hash2);
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ref prefix")
    void toStringShouldContainRefPrefix() {
      final FunctionType funcType =
          createFunctionType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      final String str = ref.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.startsWith("(ref "), "toString should start with '(ref '");
      assertTrue(str.endsWith(")"), "toString should end with ')'");
      LOGGER.info("toString format: " + str);
    }
  }

  @Nested
  @DisplayName("getFunctionType Tests")
  class GetFunctionTypeTests {

    @Test
    @DisplayName("getFunctionType should return the function type passed to constructor")
    void getFunctionTypeShouldReturnConstructorArg() {
      final FunctionType funcType =
          createFunctionType(
              new WasmValueType[] {WasmValueType.F32, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.I32});
      final TypedFunctionReferenceType ref = new TypedFunctionReferenceType(funcType);

      assertEquals(
          funcType, ref.getFunctionType(), "getFunctionType should return the same object");
      LOGGER.info("getFunctionType identity verified");
    }
  }
}
