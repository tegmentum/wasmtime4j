package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.I31Instance;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for {@link JniGcRuntime} and its inner classes.
 *
 * <p>These tests use reflection to verify class structure, field types, and method signatures
 * without requiring native library initialization. This approach ensures tests remain fast and
 * don't rely on platform-specific native code.
 */
@DisplayName("JniGcRuntime Tests")
class JniGcRuntimeTest {

  @Nested
  @DisplayName("JniGcRuntime Class Structure Tests")
  class JniGcRuntimeClassStructureTests {

    @Test
    @DisplayName("JniGcRuntime should implement GcRuntime interface")
    void jniGcRuntimeShouldImplementGcRuntimeInterface() {
      assertThat(GcRuntime.class.isAssignableFrom(JniGcRuntime.class)).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should be final class")
    void jniGcRuntimeShouldBeFinalClass() {
      assertThat(Modifier.isFinal(JniGcRuntime.class.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have Logger field")
    void jniGcRuntimeShouldHaveLoggerField() throws NoSuchFieldException {
      final Field loggerField = JniGcRuntime.class.getDeclaredField("LOGGER");
      assertThat(Modifier.isStatic(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(loggerField.getModifiers())).isTrue();
      assertThat(loggerField.getType()).isEqualTo(Logger.class);
    }

    @Test
    @DisplayName("JniGcRuntime should have nativeHandle field")
    void jniGcRuntimeShouldHaveNativeHandleField() throws NoSuchFieldException {
      final Field nativeHandleField = JniGcRuntime.class.getDeclaredField("nativeHandle");
      assertThat(Modifier.isPrivate(nativeHandleField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(nativeHandleField.getModifiers())).isTrue();
      assertThat(nativeHandleField.getType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("JniGcRuntime should have lock field for thread safety")
    void jniGcRuntimeShouldHaveLockField() throws NoSuchFieldException {
      final Field lockField = JniGcRuntime.class.getDeclaredField("lock");
      assertThat(Modifier.isPrivate(lockField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(lockField.getModifiers())).isTrue();
      assertThat(lockField.getType()).isEqualTo(ReentrantReadWriteLock.class);
    }

    @Test
    @DisplayName("JniGcRuntime should have disposed field with volatile modifier")
    void jniGcRuntimeShouldHaveDisposedField() throws NoSuchFieldException {
      final Field disposedField = JniGcRuntime.class.getDeclaredField("disposed");
      assertThat(Modifier.isPrivate(disposedField.getModifiers())).isTrue();
      assertThat(Modifier.isVolatile(disposedField.getModifiers())).isTrue();
      assertThat(disposedField.getType()).isEqualTo(boolean.class);
    }
  }

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
  @DisplayName("JniGcRuntime Struct Operations Tests")
  class JniGcRuntimeStructOperationsTests {

    @Test
    @DisplayName("createStruct method with type and values should exist")
    void createStructWithTypeAndValuesShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("createStruct", StructType.class, List.class);
      assertThat(method.getReturnType()).isEqualTo(StructInstance.class);
    }

    @Test
    @DisplayName("createStruct method with type only should exist")
    void createStructWithTypeOnlyShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("createStruct", StructType.class);
      assertThat(method.getReturnType()).isEqualTo(StructInstance.class);
    }

    @Test
    @DisplayName("getStructField method should exist")
    void getStructFieldShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("getStructField", StructInstance.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(GcValue.class);
    }

    @Test
    @DisplayName("setStructField method should exist")
    void setStructFieldShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod(
              "setStructField", StructInstance.class, int.class, GcValue.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("registerStructType method should exist")
    void registerStructTypeShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("registerStructType", StructType.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("JniGcRuntime Array Operations Tests")
  class JniGcRuntimeArrayOperationsTests {

    @Test
    @DisplayName("createArray method with type and elements should exist")
    void createArrayWithTypeAndElementsShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("createArray", ArrayType.class, List.class);
      assertThat(method.getReturnType()).isEqualTo(ArrayInstance.class);
    }

    @Test
    @DisplayName("createArray method with type and length should exist")
    void createArrayWithTypeAndLengthShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("createArray", ArrayType.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(ArrayInstance.class);
    }

    @Test
    @DisplayName("getArrayElement method should exist")
    void getArrayElementShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("getArrayElement", ArrayInstance.class, int.class);
      assertThat(method.getReturnType()).isEqualTo(GcValue.class);
    }

    @Test
    @DisplayName("setArrayElement method should exist")
    void setArrayElementShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod(
              "setArrayElement", ArrayInstance.class, int.class, GcValue.class);
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("getArrayLength method should exist")
    void getArrayLengthShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("getArrayLength", ArrayInstance.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }

    @Test
    @DisplayName("registerArrayType method should exist")
    void registerArrayTypeShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("registerArrayType", ArrayType.class);
      assertThat(method.getReturnType()).isEqualTo(int.class);
    }
  }

  @Nested
  @DisplayName("JniGcRuntime I31 Operations Tests")
  class JniGcRuntimeI31OperationsTests {

    @Test
    @DisplayName("createI31 method should exist")
    void createI31ShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("createI31", int.class);
      assertThat(method.getReturnType()).isEqualTo(I31Instance.class);
    }
  }

  @Nested
  @DisplayName("JniGcRuntime Reference Operations Tests")
  class JniGcRuntimeReferenceOperationsTests {

    @Test
    @DisplayName("refCast method should exist")
    void refCastShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refCast", GcObject.class, GcReferenceType.class);
      assertThat(method.getReturnType()).isEqualTo(GcObject.class);
    }

    @Test
    @DisplayName("refTest method should exist")
    void refTestShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refTest", GcObject.class, GcReferenceType.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("refEquals method should exist")
    void refEqualsShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refEquals", GcObject.class, GcObject.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("isNull method should exist")
    void isNullShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("isNull", GcObject.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("refCastStruct method should exist")
    void refCastStructShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refCastStruct", GcObject.class, StructType.class);
      assertThat(method.getReturnType()).isEqualTo(StructInstance.class);
    }

    @Test
    @DisplayName("refCastArray method should exist")
    void refCastArrayShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refCastArray", GcObject.class, ArrayType.class);
      assertThat(method.getReturnType()).isEqualTo(ArrayInstance.class);
    }

    @Test
    @DisplayName("refTestStruct method should exist")
    void refTestStructShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refTestStruct", GcObject.class, StructType.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("refTestArray method should exist")
    void refTestArrayShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refTestArray", GcObject.class, ArrayType.class);
      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("getRuntimeType method should exist")
    void getRuntimeTypeShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("getRuntimeType", GcObject.class);
      assertThat(method.getReturnType()).isEqualTo(GcReferenceType.class);
    }

    @Test
    @DisplayName("refCastNullable method should exist")
    void refCastNullableShouldExist() throws NoSuchMethodException {
      final Method method =
          JniGcRuntime.class.getMethod("refCastNullable", GcObject.class, GcReferenceType.class);
      assertThat(method.getReturnType()).isEqualTo(Optional.class);
    }
  }

  @Nested
  @DisplayName("JniGcRuntime Garbage Collection Tests")
  class JniGcRuntimeGarbageCollectionTests {

    @Test
    @DisplayName("collectGarbage method should exist")
    void collectGarbageShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("collectGarbage");
      assertThat(method.getReturnType()).isEqualTo(GcStats.class);
    }

    @Test
    @DisplayName("getGcStats method should exist")
    void getGcStatsShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("getGcStats");
      assertThat(method.getReturnType()).isEqualTo(GcStats.class);
    }
  }

  @Nested
  @DisplayName("JniGcRuntime Lifecycle Tests")
  class JniGcRuntimeLifecycleTests {

    @Test
    @DisplayName("dispose method should exist")
    void disposeShouldExist() throws NoSuchMethodException {
      final Method method = JniGcRuntime.class.getMethod("dispose");
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("JniStructInstance Inner Class Tests")
  class JniStructInstanceTests {

    @Test
    @DisplayName("JniStructInstance should be public static inner class")
    void jniStructInstanceShouldBePublicStaticInnerClass() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniStructInstance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(Modifier.isPublic(innerClass.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(innerClass.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("JniStructInstance should implement StructInstance")
    void jniStructInstanceShouldImplementStructInstance() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniStructInstance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(StructInstance.class.isAssignableFrom(innerClass)).isTrue();
    }
  }

  @Nested
  @DisplayName("JniArrayInstance Inner Class Tests")
  class JniArrayInstanceTests {

    @Test
    @DisplayName("JniArrayInstance should be public static inner class")
    void jniArrayInstanceShouldBePublicStaticInnerClass() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniArrayInstance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(Modifier.isPublic(innerClass.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(innerClass.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("JniArrayInstance should implement ArrayInstance")
    void jniArrayInstanceShouldImplementArrayInstance() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniArrayInstance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(ArrayInstance.class.isAssignableFrom(innerClass)).isTrue();
    }
  }

  @Nested
  @DisplayName("JniI31Instance Inner Class Tests")
  class JniI31InstanceTests {

    @Test
    @DisplayName("JniI31Instance should be public static inner class")
    void jniI31InstanceShouldBePublicStaticInnerClass() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniI31Instance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(Modifier.isPublic(innerClass.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(innerClass.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("JniI31Instance should implement I31Instance")
    void jniI31InstanceShouldImplementI31Instance() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("JniI31Instance")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      assertThat(I31Instance.class.isAssignableFrom(innerClass)).isTrue();
    }
  }

  @Nested
  @DisplayName("GcReferenceMarker Inner Class Tests")
  class GcReferenceMarkerTests {

    @Test
    @DisplayName("GcReferenceMarker should be package-private static inner class")
    void gcReferenceMarkerShouldBePackagePrivateStaticInnerClass() {
      Class<?> innerClass = null;
      for (Class<?> c : JniGcRuntime.class.getDeclaredClasses()) {
        if (c.getSimpleName().equals("GcReferenceMarker")) {
          innerClass = c;
          break;
        }
      }
      assertThat(innerClass).isNotNull();
      // Should NOT be public
      assertThat(Modifier.isPublic(innerClass.getModifiers())).isFalse();
      assertThat(Modifier.isStatic(innerClass.getModifiers())).isTrue();
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

  @Nested
  @DisplayName("Native Method Declarations Tests")
  class NativeMethodDeclarationsTests {

    @Test
    @DisplayName("JniGcRuntime should have createRuntimeNative method")
    void jniGcRuntimeShouldHaveCreateRuntimeNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("createRuntimeNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have destroyRuntimeNative method")
    void jniGcRuntimeShouldHaveDestroyRuntimeNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("destroyRuntimeNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have structNewNative method")
    void jniGcRuntimeShouldHaveStructNewNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("structNewNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have arrayNewNative method")
    void jniGcRuntimeShouldHaveArrayNewNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("arrayNewNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have i31NewNative method")
    void jniGcRuntimeShouldHaveI31NewNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("i31NewNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have refCastNative method")
    void jniGcRuntimeShouldHaveRefCastNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("refCastNative"));
      assertThat(methodExists).isTrue();
    }

    @Test
    @DisplayName("JniGcRuntime should have collectGarbageNative method")
    void jniGcRuntimeShouldHaveCollectGarbageNativeMethod() {
      final boolean methodExists =
          Arrays.stream(JniGcRuntime.class.getDeclaredMethods())
              .anyMatch(m -> m.getName().equals("collectGarbageNative"));
      assertThat(methodExists).isTrue();
    }
  }
}
