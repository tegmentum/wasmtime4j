package ai.tegmentum.wasmtime4j.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link StorageType} class.
 *
 * <p>StorageType represents packed integer field storage (I8, I16) and ValType-based storage in GC
 * struct/array fields.
 */
@DisplayName("StorageType Tests")
class StorageTypeTest {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("i8() should create I8 storage type")
    void i8ShouldCreateI8StorageType() {
      final StorageType type = StorageType.i8();
      assertNotNull(type);
      assertTrue(type.isI8(), "Should be I8");
      assertFalse(type.isI16(), "Should not be I16");
      assertFalse(type.isValType(), "Should not be VAL");
    }

    @Test
    @DisplayName("i16() should create I16 storage type")
    void i16ShouldCreateI16StorageType() {
      final StorageType type = StorageType.i16();
      assertNotNull(type);
      assertFalse(type.isI8(), "Should not be I8");
      assertTrue(type.isI16(), "Should be I16");
      assertFalse(type.isValType(), "Should not be VAL");
    }

    @Test
    @DisplayName("val(ValType) should create VAL storage type")
    void valShouldCreateValStorageType() {
      final StorageType type = StorageType.val(ValType.i32());
      assertNotNull(type);
      assertFalse(type.isI8(), "Should not be I8");
      assertFalse(type.isI16(), "Should not be I16");
      assertTrue(type.isValType(), "Should be VAL");
    }

    @Test
    @DisplayName("val(null) should throw IllegalArgumentException")
    void valWithNullShouldThrow() {
      assertThrows(IllegalArgumentException.class, () -> StorageType.val(null));
    }
  }

  @Nested
  @DisplayName("AsValType Tests")
  class AsValTypeTests {

    @Test
    @DisplayName("VAL type should return its ValType")
    void valTypeShouldReturnValType() {
      final ValType f64 = ValType.f64();
      final StorageType type = StorageType.val(f64);
      assertEquals(f64, type.asValType());
    }

    @Test
    @DisplayName("I8 type should throw on asValType()")
    void i8ShouldThrowOnAsValType() {
      assertThrows(IllegalStateException.class, () -> StorageType.i8().asValType());
    }

    @Test
    @DisplayName("I16 type should throw on asValType()")
    void i16ShouldThrowOnAsValType() {
      assertThrows(IllegalStateException.class, () -> StorageType.i16().asValType());
    }
  }

  @Nested
  @DisplayName("Unpack Tests")
  class UnpackTests {

    @Test
    @DisplayName("I8 should unpack to I32")
    void i8ShouldUnpackToI32() {
      assertEquals(ValType.i32(), StorageType.i8().unpack());
    }

    @Test
    @DisplayName("I16 should unpack to I32")
    void i16ShouldUnpackToI32() {
      assertEquals(ValType.i32(), StorageType.i16().unpack());
    }

    @Test
    @DisplayName("VAL(I64) should unpack to I64")
    void valI64ShouldUnpackToI64() {
      final ValType i64 = ValType.i64();
      assertEquals(i64, StorageType.val(i64).unpack());
    }

    @Test
    @DisplayName("VAL(F32) should unpack to F32")
    void valF32ShouldUnpackToF32() {
      final ValType f32 = ValType.f32();
      assertEquals(f32, StorageType.val(f32).unpack());
    }
  }

  @Nested
  @DisplayName("Kind Tests")
  class KindTests {

    @Test
    @DisplayName("should have I8 kind")
    void shouldHaveI8Kind() {
      assertEquals(StorageType.Kind.I8, StorageType.i8().getKind());
    }

    @Test
    @DisplayName("should have I16 kind")
    void shouldHaveI16Kind() {
      assertEquals(StorageType.Kind.I16, StorageType.i16().getKind());
    }

    @Test
    @DisplayName("should have VAL kind")
    void shouldHaveValKind() {
      assertEquals(StorageType.Kind.VAL, StorageType.val(ValType.i32()).getKind());
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same I8 instances should be equal")
    void sameI8ShouldBeEqual() {
      assertEquals(StorageType.i8(), StorageType.i8());
    }

    @Test
    @DisplayName("same I16 instances should be equal")
    void sameI16ShouldBeEqual() {
      assertEquals(StorageType.i16(), StorageType.i16());
    }

    @Test
    @DisplayName("same VAL instances should be equal")
    void sameValShouldBeEqual() {
      assertEquals(StorageType.val(ValType.i32()), StorageType.val(ValType.i32()));
    }

    @Test
    @DisplayName("I8 and I16 should not be equal")
    void i8AndI16ShouldNotBeEqual() {
      assertNotEquals(StorageType.i8(), StorageType.i16());
    }

    @Test
    @DisplayName("different VAL types should not be equal")
    void differentValTypesShouldNotBeEqual() {
      assertNotEquals(StorageType.val(ValType.i32()), StorageType.val(ValType.i64()));
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("I8 should have meaningful toString")
    void i8ShouldHaveMeaningfulToString() {
      assertEquals("i8", StorageType.i8().toString());
    }

    @Test
    @DisplayName("I16 should have meaningful toString")
    void i16ShouldHaveMeaningfulToString() {
      assertEquals("i16", StorageType.i16().toString());
    }

    @Test
    @DisplayName("VAL should include val type in toString")
    void valShouldIncludeValTypeInToString() {
      final String str = StorageType.val(ValType.f64()).toString();
      assertNotNull(str);
      assertFalse(str.isEmpty(), "toString should not be empty");
    }
  }
}
