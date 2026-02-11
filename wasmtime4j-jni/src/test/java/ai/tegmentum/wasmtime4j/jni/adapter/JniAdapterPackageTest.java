/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 */

package ai.tegmentum.wasmtime4j.jni.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.func.Function;
import ai.tegmentum.wasmtime4j.memory.Global;
import ai.tegmentum.wasmtime4j.memory.Memory;
import ai.tegmentum.wasmtime4j.memory.Table;
import ai.tegmentum.wasmtime4j.WasmMemory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JNI adapter package.
 *
 * <p>This test covers all adapter classes in the ai.tegmentum.wasmtime4j.jni.adapter package
 * including WasmMemoryToMemoryAdapter, WasmGlobalToGlobalAdapter, WasmFunctionToFunctionAdapter,
 * and WasmTableToTableAdapter.
 */
@DisplayName("JNI Adapter Package Tests")
class JniAdapterPackageTest {

  // ========================================================================
  // WasmMemoryToMemoryAdapter Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmMemoryToMemoryAdapter Tests")
  class WasmMemoryToMemoryAdapterTests {

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should be a final class")
    void wasmMemoryToMemoryAdapterShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmMemoryToMemoryAdapter.class.getModifiers()),
          "WasmMemoryToMemoryAdapter should be final");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should implement Memory interface")
    void wasmMemoryToMemoryAdapterShouldImplementMemoryInterface() {
      assertTrue(
          Memory.class.isAssignableFrom(WasmMemoryToMemoryAdapter.class),
          "WasmMemoryToMemoryAdapter should implement Memory");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have constructor with WasmMemory parameter")
    void wasmMemoryToMemoryAdapterShouldHaveConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = WasmMemoryToMemoryAdapter.class.getConstructor(WasmMemory.class);
      assertNotNull(constructor, "Constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter constructor should reject null")
    void wasmMemoryToMemoryAdapterConstructorShouldRejectNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasmMemoryToMemoryAdapter(null),
          "Constructor should throw IllegalArgumentException for null delegate");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have getDelegate method")
    void wasmMemoryToMemoryAdapterShouldHaveGetDelegateMethod() throws NoSuchMethodException {
      Method method = WasmMemoryToMemoryAdapter.class.getMethod("getDelegate");
      assertNotNull(method, "getDelegate method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have getSize method")
    void wasmMemoryToMemoryAdapterShouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = WasmMemoryToMemoryAdapter.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have getSizeInBytes method")
    void wasmMemoryToMemoryAdapterShouldHaveGetSizeInBytesMethod() throws NoSuchMethodException {
      Method method = WasmMemoryToMemoryAdapter.class.getMethod("getSizeInBytes");
      assertNotNull(method, "getSizeInBytes method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have grow method")
    void wasmMemoryToMemoryAdapterShouldHaveGrowMethod() throws NoSuchMethodException {
      Method method = WasmMemoryToMemoryAdapter.class.getMethod("grow", long.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should have isValid method")
    void wasmMemoryToMemoryAdapterShouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasmMemoryToMemoryAdapter.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("WasmMemoryToMemoryAdapter should be in correct package")
    void wasmMemoryToMemoryAdapterShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.adapter",
          WasmMemoryToMemoryAdapter.class.getPackage().getName(),
          "WasmMemoryToMemoryAdapter should be in ai.tegmentum.wasmtime4j.jni.adapter package");
    }
  }

  // ========================================================================
  // WasmGlobalToGlobalAdapter Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmGlobalToGlobalAdapter Tests")
  class WasmGlobalToGlobalAdapterTests {

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should be a final class")
    void wasmGlobalToGlobalAdapterShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmGlobalToGlobalAdapter.class.getModifiers()),
          "WasmGlobalToGlobalAdapter should be final");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should implement Global interface")
    void wasmGlobalToGlobalAdapterShouldImplementGlobalInterface() {
      assertTrue(
          Global.class.isAssignableFrom(WasmGlobalToGlobalAdapter.class),
          "WasmGlobalToGlobalAdapter should implement Global");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should have public constructor")
    void wasmGlobalToGlobalAdapterShouldHavePublicConstructor() {
      Constructor<?>[] constructors = WasmGlobalToGlobalAdapter.class.getConstructors();
      assertTrue(
          constructors.length > 0, "WasmGlobalToGlobalAdapter should have public constructor");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should have getValue method")
    void wasmGlobalToGlobalAdapterShouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = WasmGlobalToGlobalAdapter.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should have setValue method")
    void wasmGlobalToGlobalAdapterShouldHaveSetValueMethod() throws NoSuchMethodException {
      Method method = WasmGlobalToGlobalAdapter.class.getMethod("setValue", Object.class);
      assertNotNull(method, "setValue method should exist");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should have isMutable method")
    void wasmGlobalToGlobalAdapterShouldHaveIsMutableMethod() throws NoSuchMethodException {
      Method method = WasmGlobalToGlobalAdapter.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("WasmGlobalToGlobalAdapter should be in correct package")
    void wasmGlobalToGlobalAdapterShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.adapter",
          WasmGlobalToGlobalAdapter.class.getPackage().getName(),
          "WasmGlobalToGlobalAdapter should be in ai.tegmentum.wasmtime4j.jni.adapter package");
    }
  }

  // ========================================================================
  // WasmFunctionToFunctionAdapter Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmFunctionToFunctionAdapter Tests")
  class WasmFunctionToFunctionAdapterTests {

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should be a final class")
    void wasmFunctionToFunctionAdapterShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmFunctionToFunctionAdapter.class.getModifiers()),
          "WasmFunctionToFunctionAdapter should be final");
    }

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should implement Function interface")
    void wasmFunctionToFunctionAdapterShouldImplementFunctionInterface() {
      assertTrue(
          Function.class.isAssignableFrom(WasmFunctionToFunctionAdapter.class),
          "WasmFunctionToFunctionAdapter should implement Function");
    }

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should have public constructor")
    void wasmFunctionToFunctionAdapterShouldHavePublicConstructor() {
      Constructor<?>[] constructors = WasmFunctionToFunctionAdapter.class.getConstructors();
      assertTrue(
          constructors.length > 0, "WasmFunctionToFunctionAdapter should have public constructor");
    }

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should have call method")
    void wasmFunctionToFunctionAdapterShouldHaveCallMethod() throws NoSuchMethodException {
      Method method = WasmFunctionToFunctionAdapter.class.getMethod("call", Object[].class);
      assertNotNull(method, "call method should exist");
    }

    @Test
    @DisplayName("WasmFunctionToFunctionAdapter should be in correct package")
    void wasmFunctionToFunctionAdapterShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.adapter",
          WasmFunctionToFunctionAdapter.class.getPackage().getName(),
          "WasmFunctionToFunctionAdapter should be in ai.tegmentum.wasmtime4j.jni.adapter package");
    }
  }

  // ========================================================================
  // WasmTableToTableAdapter Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmTableToTableAdapter Tests")
  class WasmTableToTableAdapterTests {

    @Test
    @DisplayName("WasmTableToTableAdapter should be a final class")
    void wasmTableToTableAdapterShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasmTableToTableAdapter.class.getModifiers()),
          "WasmTableToTableAdapter should be final");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should implement Table interface")
    void wasmTableToTableAdapterShouldImplementTableInterface() {
      assertTrue(
          Table.class.isAssignableFrom(WasmTableToTableAdapter.class),
          "WasmTableToTableAdapter should implement Table");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should have public constructor")
    void wasmTableToTableAdapterShouldHavePublicConstructor() {
      Constructor<?>[] constructors = WasmTableToTableAdapter.class.getConstructors();
      assertTrue(constructors.length > 0, "WasmTableToTableAdapter should have public constructor");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should have getSize method")
    void wasmTableToTableAdapterShouldHaveGetSizeMethod() throws NoSuchMethodException {
      Method method = WasmTableToTableAdapter.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should have get method")
    void wasmTableToTableAdapterShouldHaveGetMethod() throws NoSuchMethodException {
      Method method = WasmTableToTableAdapter.class.getMethod("get", long.class);
      assertNotNull(method, "get method should exist");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should have set method")
    void wasmTableToTableAdapterShouldHaveSetMethod() throws NoSuchMethodException {
      Method method = WasmTableToTableAdapter.class.getMethod("set", long.class, Object.class);
      assertNotNull(method, "set method should exist");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should have grow method")
    void wasmTableToTableAdapterShouldHaveGrowMethod() throws NoSuchMethodException {
      Method method = WasmTableToTableAdapter.class.getMethod("grow", long.class, Object.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("WasmTableToTableAdapter should be in correct package")
    void wasmTableToTableAdapterShouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.adapter",
          WasmTableToTableAdapter.class.getPackage().getName(),
          "WasmTableToTableAdapter should be in ai.tegmentum.wasmtime4j.jni.adapter package");
    }
  }

  // ========================================================================
  // Package-Level Tests
  // ========================================================================

  @Nested
  @DisplayName("Package-Level Tests")
  class PackageLevelTests {

    @Test
    @DisplayName("All adapter classes should be in correct package")
    void allAdapterClassesShouldBeInCorrectPackage() {
      Class<?>[] adapterClasses = {
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class,
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class
      };

      String expectedPackage = "ai.tegmentum.wasmtime4j.jni.adapter";
      for (Class<?> clazz : adapterClasses) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("All adapter classes should be final")
    void allAdapterClassesShouldBeFinal() {
      Class<?>[] adapterClasses = {
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class,
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class
      };

      for (Class<?> clazz : adapterClasses) {
        assertTrue(
            Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " should be final");
      }
    }

    @Test
    @DisplayName("All adapter classes should not be interfaces")
    void allAdapterClassesShouldNotBeInterfaces() {
      Class<?>[] adapterClasses = {
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class,
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class
      };

      for (Class<?> clazz : adapterClasses) {
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }

    @Test
    @DisplayName("Adapter classes should have public constructors")
    void adapterClassesShouldHavePublicConstructors() {
      Class<?>[] adapterClasses = {
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class,
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class
      };

      for (Class<?> clazz : adapterClasses) {
        Constructor<?>[] constructors = clazz.getConstructors();
        assertTrue(
            constructors.length > 0, clazz.getSimpleName() + " should have public constructor");
      }
    }
  }
}
