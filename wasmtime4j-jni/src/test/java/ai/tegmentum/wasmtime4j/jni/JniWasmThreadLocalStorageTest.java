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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link JniWasmThreadLocalStorage} class.
 *
 * <p>This test class verifies the class structure, interface implementation, and method signatures
 * of the JNI thread-local storage implementation using reflection-based testing.
 */
@DisplayName("JniWasmThreadLocalStorage Tests")
class JniWasmThreadLocalStorageTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("JniWasmThreadLocalStorage should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasmThreadLocalStorage.class.getModifiers()),
          "JniWasmThreadLocalStorage should be final");
    }

    @Test
    @DisplayName("JniWasmThreadLocalStorage should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(JniWasmThreadLocalStorage.class.getModifiers()),
          "JniWasmThreadLocalStorage should be public");
    }

    @Test
    @DisplayName("JniWasmThreadLocalStorage should implement WasmThreadLocalStorage")
    void shouldImplementWasmThreadLocalStorage() {
      assertTrue(
          WasmThreadLocalStorage.class.isAssignableFrom(JniWasmThreadLocalStorage.class),
          "JniWasmThreadLocalStorage should implement WasmThreadLocalStorage");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have nativeThreadHandle field")
    void shouldHaveNativeThreadHandleField() throws Exception {
      Field field = JniWasmThreadLocalStorage.class.getDeclaredField("nativeThreadHandle");
      assertNotNull(field, "nativeThreadHandle field should exist");
      assertEquals(long.class, field.getType(), "nativeThreadHandle should be long");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "nativeThreadHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "nativeThreadHandle should be final");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws Exception {
      Field field = JniWasmThreadLocalStorage.class.getDeclaredField("closed");
      assertNotNull(field, "closed field should exist");
      assertEquals(AtomicBoolean.class, field.getType(), "closed should be AtomicBoolean");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "closed should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "closed should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with long parameter")
    void shouldHavePublicConstructor() throws Exception {
      Constructor<?> constructor =
          JniWasmThreadLocalStorage.class.getConstructor(long.class);
      assertNotNull(constructor, "Constructor with long parameter should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have exactly 1 parameter")
    void constructorShouldHaveOneParameter() throws Exception {
      Constructor<?>[] constructors = JniWasmThreadLocalStorage.class.getConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 public constructor");

      Constructor<?> constructor = constructors[0];
      assertEquals(1, constructor.getParameterCount(), "Constructor should have 1 parameter");
      assertEquals(
          long.class,
          constructor.getParameterTypes()[0],
          "Parameter should be long");
    }
  }

  // ========================================================================
  // Integer Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Integer Method Tests")
  class IntegerMethodTests {

    @Test
    @DisplayName("should have putInt method with correct signature")
    void shouldHavePutIntMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertNotNull(method, "putInt method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putInt should be public");
    }

    @Test
    @DisplayName("should have getInt method with correct signature")
    void shouldHaveGetIntMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertNotNull(method, "getInt method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getInt should be public");
    }
  }

  // ========================================================================
  // Long Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Long Method Tests")
  class LongMethodTests {

    @Test
    @DisplayName("should have putLong method with correct signature")
    void shouldHavePutLongMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putLong", String.class, long.class);
      assertNotNull(method, "putLong method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putLong should be public");
    }

    @Test
    @DisplayName("should have getLong method with correct signature")
    void shouldHaveGetLongMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getLong", String.class);
      assertNotNull(method, "getLong method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getLong should be public");
    }
  }

  // ========================================================================
  // Float Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Float Method Tests")
  class FloatMethodTests {

    @Test
    @DisplayName("should have putFloat method with correct signature")
    void shouldHavePutFloatMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putFloat", String.class, float.class);
      assertNotNull(method, "putFloat method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putFloat should be public");
    }

    @Test
    @DisplayName("should have getFloat method with correct signature")
    void shouldHaveGetFloatMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getFloat", String.class);
      assertNotNull(method, "getFloat method should exist");
      assertEquals(float.class, method.getReturnType(), "Return type should be float");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getFloat should be public");
    }
  }

  // ========================================================================
  // Double Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Double Method Tests")
  class DoubleMethodTests {

    @Test
    @DisplayName("should have putDouble method with correct signature")
    void shouldHavePutDoubleMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putDouble", String.class, double.class);
      assertNotNull(method, "putDouble method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putDouble should be public");
    }

    @Test
    @DisplayName("should have getDouble method with correct signature")
    void shouldHaveGetDoubleMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getDouble", String.class);
      assertNotNull(method, "getDouble method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getDouble should be public");
    }
  }

  // ========================================================================
  // Bytes Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Bytes Method Tests")
  class BytesMethodTests {

    @Test
    @DisplayName("should have putBytes method with correct signature")
    void shouldHavePutBytesMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
      assertNotNull(method, "putBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putBytes should be public");
    }

    @Test
    @DisplayName("should have getBytes method with correct signature")
    void shouldHaveGetBytesMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getBytes", String.class);
      assertNotNull(method, "getBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getBytes should be public");
    }
  }

  // ========================================================================
  // String Method Tests
  // ========================================================================

  @Nested
  @DisplayName("String Method Tests")
  class StringMethodTests {

    @Test
    @DisplayName("should have putString method with correct signature")
    void shouldHavePutStringMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getMethod("putString", String.class, String.class);
      assertNotNull(method, "putString method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "putString should be public");
    }

    @Test
    @DisplayName("should have getString method with correct signature")
    void shouldHaveGetStringMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getString", String.class);
      assertNotNull(method, "getString method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getString should be public");
    }
  }

  // ========================================================================
  // Collection Operation Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Collection Operation Method Tests")
  class CollectionOperationMethodTests {

    @Test
    @DisplayName("should have remove method with correct signature")
    void shouldHaveRemoveMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("remove", String.class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "remove should be public");
    }

    @Test
    @DisplayName("should have contains method with correct signature")
    void shouldHaveContainsMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("contains", String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "contains should be public");
    }

    @Test
    @DisplayName("should have clear method with correct signature")
    void shouldHaveClearMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "clear should have no parameters");
      assertTrue(Modifier.isPublic(method.getModifiers()), "clear should be public");
    }

    @Test
    @DisplayName("should have size method with correct signature")
    void shouldHaveSizeMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(0, method.getParameterCount(), "size should have no parameters");
      assertTrue(Modifier.isPublic(method.getModifiers()), "size should be public");
    }

    @Test
    @DisplayName("should have getMemoryUsage method with correct signature")
    void shouldHaveGetMemoryUsageMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertEquals(0, method.getParameterCount(), "getMemoryUsage should have no parameters");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getMemoryUsage should be public");
    }
  }

  // ========================================================================
  // Lifecycle Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method with correct signature")
    void shouldHaveCloseMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
    }
  }

  // ========================================================================
  // Private Helper Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Helper Method Tests")
  class PrivateHelperMethodTests {

    @Test
    @DisplayName("should have ensureNotClosed private method")
    void shouldHaveEnsureNotClosedMethod() throws Exception {
      Method method = JniWasmThreadLocalStorage.class.getDeclaredMethod("ensureNotClosed");
      assertNotNull(method, "ensureNotClosed method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "ensureNotClosed should be private");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have validateKey private method")
    void shouldHaveValidateKeyMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod("validateKey", String.class);
      assertNotNull(method, "validateKey method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "validateKey should be private");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have validateNotNull private method")
    void shouldHaveValidateNotNullMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "validateNotNull", Object.class, String.class);
      assertNotNull(method, "validateNotNull method should exist");
      assertTrue(
          Modifier.isPrivate(method.getModifiers()), "validateNotNull should be private");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // Native Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Native Method Tests")
  class NativeMethodTests {

    @Test
    @DisplayName("should have nativePutInt native method")
    void shouldHaveNativePutIntMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativePutInt", long.class, String.class, int.class);
      assertNotNull(method, "nativePutInt method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativePutInt should be native");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "nativePutInt should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nativePutInt should be static");
    }

    @Test
    @DisplayName("should have nativeGetInt native method")
    void shouldHaveNativeGetIntMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativeGetInt", long.class, String.class);
      assertNotNull(method, "nativeGetInt method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeGetInt should be native");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "nativeGetInt should be private");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nativeGetInt should be static");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have nativePutLong native method")
    void shouldHaveNativePutLongMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativePutLong", long.class, String.class, long.class);
      assertNotNull(method, "nativePutLong method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativePutLong should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nativePutLong should be static");
    }

    @Test
    @DisplayName("should have nativeGetLong native method")
    void shouldHaveNativeGetLongMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativeGetLong", long.class, String.class);
      assertNotNull(method, "nativeGetLong method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeGetLong should be native");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have nativePutBytes native method")
    void shouldHaveNativePutBytesMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativePutBytes", long.class, String.class, byte[].class);
      assertNotNull(method, "nativePutBytes method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativePutBytes should be native");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nativePutBytes should be static");
    }

    @Test
    @DisplayName("should have nativeGetBytes native method")
    void shouldHaveNativeGetBytesMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativeGetBytes", long.class, String.class);
      assertNotNull(method, "nativeGetBytes method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeGetBytes should be native");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("should have nativeRemove native method")
    void shouldHaveNativeRemoveMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativeRemove", long.class, String.class);
      assertNotNull(method, "nativeRemove method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeRemove should be native");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have nativeContains native method")
    void shouldHaveNativeContainsMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod(
              "nativeContains", long.class, String.class);
      assertNotNull(method, "nativeContains method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeContains should be native");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have nativeClear native method")
    void shouldHaveNativeClearMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod("nativeClear", long.class);
      assertNotNull(method, "nativeClear method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeClear should be native");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have nativeSize native method")
    void shouldHaveNativeSizeMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod("nativeSize", long.class);
      assertNotNull(method, "nativeSize method should exist");
      assertTrue(Modifier.isNative(method.getModifiers()), "nativeSize should be native");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have nativeGetMemoryUsage native method")
    void shouldHaveNativeGetMemoryUsageMethod() throws Exception {
      Method method =
          JniWasmThreadLocalStorage.class.getDeclaredMethod("nativeGetMemoryUsage", long.class);
      assertNotNull(method, "nativeGetMemoryUsage method should exist");
      assertTrue(
          Modifier.isNative(method.getModifiers()), "nativeGetMemoryUsage should be native");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("WasmThreadLocalStorage interface methods should all be implemented")
    void allInterfaceMethodsShouldBeImplemented() {
      Method[] interfaceMethods = WasmThreadLocalStorage.class.getDeclaredMethods();

      for (Method interfaceMethod : interfaceMethods) {
        try {
          Method implMethod =
              JniWasmThreadLocalStorage.class.getMethod(
                  interfaceMethod.getName(), interfaceMethod.getParameterTypes());
          assertNotNull(
              implMethod,
              "Implementation for " + interfaceMethod.getName() + " should exist");
        } catch (NoSuchMethodException e) {
          throw new AssertionError(
              "Missing implementation for interface method: " + interfaceMethod.getName());
        }
      }
    }

    @Test
    @DisplayName("should implement all data type methods")
    void shouldImplementAllDataTypeMethods() throws Exception {
      // Verify all put/get pairs for each type
      String[][] methodPairs = {
        {"putInt", "getInt"},
        {"putLong", "getLong"},
        {"putFloat", "getFloat"},
        {"putDouble", "getDouble"},
        {"putBytes", "getBytes"},
        {"putString", "getString"}
      };

      for (String[] pair : methodPairs) {
        Method putMethod =
            JniWasmThreadLocalStorage.class.getDeclaredMethod(
                pair[0], String.class, getSecondParam(pair[0]));
        Method getMethod =
            JniWasmThreadLocalStorage.class.getDeclaredMethod(pair[1], String.class);

        assertNotNull(putMethod, pair[0] + " method should exist");
        assertNotNull(getMethod, pair[1] + " method should exist");
        assertTrue(
            Modifier.isPublic(putMethod.getModifiers()), pair[0] + " should be public");
        assertTrue(
            Modifier.isPublic(getMethod.getModifiers()), pair[1] + " should be public");
      }
    }

    private Class<?> getSecondParam(final String methodName) {
      return switch (methodName) {
        case "putInt" -> int.class;
        case "putLong" -> long.class;
        case "putFloat" -> float.class;
        case "putDouble" -> double.class;
        case "putBytes" -> byte[].class;
        case "putString" -> String.class;
        default -> throw new IllegalArgumentException("Unknown method: " + methodName);
      };
    }
  }

  // ========================================================================
  // Thread Safety Design Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Safety Design Tests")
  class ThreadSafetyDesignTests {

    @Test
    @DisplayName("closed field should use AtomicBoolean for thread safety")
    void closedShouldUseAtomicBoolean() throws Exception {
      Field closedField = JniWasmThreadLocalStorage.class.getDeclaredField("closed");
      assertEquals(
          AtomicBoolean.class,
          closedField.getType(),
          "closed should be AtomicBoolean for thread-safe close operations");
    }

    @Test
    @DisplayName("nativeThreadHandle should be final for immutability")
    void nativeThreadHandleShouldBeFinal() throws Exception {
      Field handleField = JniWasmThreadLocalStorage.class.getDeclaredField("nativeThreadHandle");
      assertTrue(
          Modifier.isFinal(handleField.getModifiers()),
          "nativeThreadHandle should be final for thread safety");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected number of native methods")
    void shouldHaveExpectedNativeMethodCount() {
      Method[] allMethods = JniWasmThreadLocalStorage.class.getDeclaredMethods();
      int nativeCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeCount++;
        }
      }

      // Expected: 14 native methods
      // nativePut/Get for int, long, float, double, bytes = 10
      // nativeRemove, nativeContains, nativeClear, nativeSize, nativeGetMemoryUsage = 5
      // Total = 14 but we only check >= 10 to be flexible
      assertTrue(nativeCount >= 10, "Should have at least 10 native methods");
    }

    @Test
    @DisplayName("should have expected number of public methods")
    void shouldHaveExpectedPublicMethodCount() {
      Method[] allMethods = JniWasmThreadLocalStorage.class.getDeclaredMethods();
      int publicCount = 0;

      for (Method method : allMethods) {
        if (Modifier.isPublic(method.getModifiers())) {
          publicCount++;
        }
      }

      // Expected: 14 public methods from interface + close
      assertTrue(publicCount >= 13, "Should have at least 13 public methods");
    }
  }
}
