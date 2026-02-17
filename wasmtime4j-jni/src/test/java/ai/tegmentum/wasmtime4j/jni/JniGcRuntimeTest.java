package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link JniGcRuntime} and its inner classes.
 *
 * <p>These tests verify behavioral aspects of GC runtime types including constructor validation,
 * enum values, builder patterns, and value type operations.
 */
@DisplayName("JniGcRuntime Tests")
class JniGcRuntimeTest {

  @Nested
  @DisplayName("JniGcRuntime Constructor Tests")
  class JniGcRuntimeConstructorTests {

    @Test
    @DisplayName("Constructor should reject zero engine handle")
    void constructorShouldRejectZeroEngineHandle() {
      assertThatThrownBy(() -> new JniGcRuntime(0))
          .isInstanceOf(JniException.class)
          .hasMessageContaining("Invalid engine handle");
    }
  }

  @Nested
  @DisplayName("GcReferenceType Enum Tests")
  class GcReferenceTypeEnumTests {

    @Test
    @DisplayName("GcReferenceType should have ANY_REF value")
    void gcReferenceTypeShouldHaveAnyRefValue() {
      assertThat(GcReferenceType.ANY_REF).isNotNull();
    }

    @Test
    @DisplayName("GcReferenceType should have EQ_REF value")
    void gcReferenceTypeShouldHaveEqRefValue() {
      assertThat(GcReferenceType.EQ_REF).isNotNull();
    }

    @Test
    @DisplayName("GcReferenceType should have I31_REF value")
    void gcReferenceTypeShouldHaveI31RefValue() {
      assertThat(GcReferenceType.I31_REF).isNotNull();
    }

    @Test
    @DisplayName("GcReferenceType should have STRUCT_REF value")
    void gcReferenceTypeShouldHaveStructRefValue() {
      assertThat(GcReferenceType.STRUCT_REF).isNotNull();
    }

    @Test
    @DisplayName("GcReferenceType should have ARRAY_REF value")
    void gcReferenceTypeShouldHaveArrayRefValue() {
      assertThat(GcReferenceType.ARRAY_REF).isNotNull();
    }
  }

  @Nested
  @DisplayName("GcStats Builder Tests")
  class GcStatsBuilderTests {

    @Test
    @DisplayName("GcStats should be buildable with builder pattern")
    void gcStatsShouldBeBuildableWithBuilderPattern() {
      final GcStats stats =
          GcStats.builder()
              .totalAllocated(1000)
              .totalCollected(500)
              .bytesAllocated(50000)
              .bytesCollected(25000)
              .minorCollections(10)
              .majorCollections(2)
              .currentHeapSize(30000)
              .peakHeapSize(60000)
              .maxHeapSize(100000)
              .build();

      assertThat(stats).isNotNull();
    }
  }

  @Nested
  @DisplayName("GcValue Type Tests")
  class GcValueTypeTests {

    @Test
    @DisplayName("GcValue should support i32 type")
    void gcValueShouldSupportI32Type() {
      final GcValue value = GcValue.i32(42);
      assertThat(value).isNotNull();
      assertThat(value.asI32()).isEqualTo(42);
    }

    @Test
    @DisplayName("GcValue should support i64 type")
    void gcValueShouldSupportI64Type() {
      final GcValue value = GcValue.i64(9999999999L);
      assertThat(value).isNotNull();
      assertThat(value.asI64()).isEqualTo(9999999999L);
    }

    @Test
    @DisplayName("GcValue should support f32 type")
    void gcValueShouldSupportF32Type() {
      final GcValue value = GcValue.f32(3.14f);
      assertThat(value).isNotNull();
      assertThat(value.asF32()).isEqualTo(3.14f);
    }

    @Test
    @DisplayName("GcValue should support f64 type")
    void gcValueShouldSupportF64Type() {
      final GcValue value = GcValue.f64(2.718281828);
      assertThat(value).isNotNull();
      assertThat(value.asF64()).isEqualTo(2.718281828);
    }

    @Test
    @DisplayName("GcValue should support null value")
    void gcValueShouldSupportNullValue() {
      final GcValue value = GcValue.nullValue();
      assertThat(value).isNotNull();
    }
  }

  @Nested
  @DisplayName("StructType Builder Tests")
  class StructTypeBuilderTests {

    @Test
    @DisplayName("StructType should be creatable using builder")
    void structTypeShouldBeCreatableUsingBuilder() {
      final StructType structType =
          StructType.builder("TestStruct")
              .addField("field1", FieldType.i32(), true)
              .addField("field2", FieldType.i64())
              .build();
      assertThat(structType).isNotNull();
      assertThat(structType.getName()).isEqualTo("TestStruct");
      assertThat(structType.getFieldCount()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("ArrayType Builder Tests")
  class ArrayTypeBuilderTests {

    @Test
    @DisplayName("ArrayType should be creatable using builder")
    void arrayTypeShouldBeCreatableUsingBuilder() {
      final ArrayType arrayType =
          ArrayType.builder("TestArray").elementType(FieldType.i32()).mutable(true).build();
      assertThat(arrayType).isNotNull();
      assertThat(arrayType.getName()).isEqualTo("TestArray");
      assertThat(arrayType.isMutable()).isTrue();
    }
  }
}
