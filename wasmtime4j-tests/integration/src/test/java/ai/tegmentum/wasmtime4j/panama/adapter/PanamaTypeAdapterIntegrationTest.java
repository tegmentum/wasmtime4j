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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for Panama adapter classes.
 *
 * <p>These tests verify the class structure, method signatures, and type conversion capabilities of
 * the adapter classes used to bridge WasmFunction/Table/Memory/Global to their corresponding public
 * API interfaces.
 */
@DisplayName("Panama Type Adapter Integration Tests")
public class PanamaTypeAdapterIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaTypeAdapterIntegrationTest.class.getName());
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for adapter tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
    LOGGER.info("Native library loaded successfully");
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up " + resources.size() + " resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Error closing resource: " + e.getMessage());
      }
    }
    resources.clear();
  }

  /** Tests for WasmFunctionToFunctionAdapter. */
  @Nested
  @DisplayName("WasmFunctionToFunctionAdapter Tests")
  class FunctionAdapterTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing WasmFunctionToFunctionAdapter class structure");

      final Class<?> clazz = WasmFunctionToFunctionAdapter.class;

      // Verify class is public and final
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      // Check for generic type parameter
      assertTrue(clazz.getTypeParameters().length > 0, "Class should have type parameter");

      LOGGER.info("Class structure verified: type parameters=" + clazz.getTypeParameters().length);
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() {
      LOGGER.info("Testing WasmFunctionToFunctionAdapter methods");

      final Class<?> clazz = WasmFunctionToFunctionAdapter.class;
      final String[] expectedMethods = {
        "call",
        "callSingle",
        "getSignature",
        "getParameterTypes",
        "getReturnTypes",
        "getName",
        "isValid",
        "getParameterCount",
        "getReturnCount",
        "callAsync",
        "callSingleAsync",
        "getDelegate"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info(
                "Found method: " + methodName + " with " + method.getParameterCount() + " params");
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have conversion methods")
    void shouldHaveConversionMethods() {
      LOGGER.info("Testing WasmFunctionToFunctionAdapter conversion methods");

      final Class<?> clazz = WasmFunctionToFunctionAdapter.class;

      // Check for private/protected conversion methods
      boolean hasConvertToWasmValues = false;
      boolean hasConvertFromWasmValues = false;
      boolean hasConvertToWasmValue = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("convertToWasmValues")) {
          hasConvertToWasmValues = true;
        }
        if (method.getName().equals("convertFromWasmValues")) {
          hasConvertFromWasmValues = true;
        }
        if (method.getName().equals("convertToWasmValue")) {
          hasConvertToWasmValue = true;
        }
      }

      assertTrue(
          hasConvertToWasmValues || hasConvertFromWasmValues || hasConvertToWasmValue,
          "Should have at least one conversion method");

      LOGGER.info(
          "Conversion methods: toWasmValues="
              + hasConvertToWasmValues
              + ", fromWasmValues="
              + hasConvertFromWasmValues
              + ", toWasmValue="
              + hasConvertToWasmValue);
    }

    @Test
    @DisplayName("Should implement Function interface")
    void shouldImplementFunctionInterface() {
      LOGGER.info("Testing WasmFunctionToFunctionAdapter interface implementation");

      final Class<?> clazz = WasmFunctionToFunctionAdapter.class;
      final Class<?>[] interfaces = clazz.getInterfaces();

      boolean implementsFunction = false;
      for (final Class<?> iface : interfaces) {
        if (iface.getSimpleName().equals("Function")) {
          implementsFunction = true;
          LOGGER.info("Implements: " + iface.getName());
        }
      }

      assertTrue(implementsFunction, "Should implement Function interface");
    }
  }

  /** Tests for WasmTableToTableAdapter. */
  @Nested
  @DisplayName("WasmTableToTableAdapter Tests")
  class TableAdapterTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing WasmTableToTableAdapter class structure");

      final Class<?> clazz = WasmTableToTableAdapter.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() {
      LOGGER.info("Testing WasmTableToTableAdapter methods");

      final Class<?> clazz = WasmTableToTableAdapter.class;
      final String[] expectedMethods = {
        "getSize",
        "grow",
        "get",
        "set",
        "getElementType",
        "getMaxSize",
        "isValid",
        "fill",
        "copy",
        "getTableType",
        "growAsync",
        "getDelegate"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have correct method signatures for table operations")
    void shouldHaveCorrectTableMethodSignatures() {
      LOGGER.info("Testing table method signatures");

      final Class<?> clazz = WasmTableToTableAdapter.class;

      // Check get method
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("get") && method.getParameterCount() == 1) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "get should take long index");
        }
      }

      // Check set method
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("set") && method.getParameterCount() == 2) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "set first param should be long index");
        }
      }

      // Check fill method
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("fill") && method.getParameterCount() == 3) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "fill first param should be long dstIndex");
          assertEquals(long.class, params[2], "fill third param should be long length");
        }
      }

      LOGGER.info("Table method signatures verified");
    }

    @Test
    @DisplayName("Should implement Table interface")
    void shouldImplementTableInterface() {
      LOGGER.info("Testing WasmTableToTableAdapter interface implementation");

      final Class<?> clazz = WasmTableToTableAdapter.class;
      final Class<?>[] interfaces = clazz.getInterfaces();

      boolean implementsTable = false;
      for (final Class<?> iface : interfaces) {
        if (iface.getSimpleName().equals("Table")) {
          implementsTable = true;
          LOGGER.info("Implements: " + iface.getName());
        }
      }

      assertTrue(implementsTable, "Should implement Table interface");
    }
  }

  /** Tests for WasmMemoryToMemoryAdapter. */
  @Nested
  @DisplayName("WasmMemoryToMemoryAdapter Tests")
  class MemoryAdapterTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing WasmMemoryToMemoryAdapter class structure");

      final Class<?> clazz = WasmMemoryToMemoryAdapter.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() {
      LOGGER.info("Testing WasmMemoryToMemoryAdapter methods");

      final Class<?> clazz = WasmMemoryToMemoryAdapter.class;
      final String[] expectedMethods = {
        "getSize",
        "getSizeInBytes",
        "grow",
        "read",
        "write",
        "readByte",
        "writeByte",
        "readInt32",
        "writeInt32",
        "readInt64",
        "writeInt64",
        "getMaxSize",
        "isValid",
        "getDelegate"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have correct method signatures for memory operations")
    void shouldHaveCorrectMemoryMethodSignatures() {
      LOGGER.info("Testing memory method signatures");

      final Class<?> clazz = WasmMemoryToMemoryAdapter.class;

      // Check readByte
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("readByte") && method.getParameterCount() == 1) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "readByte should take long offset");
        }
      }

      // Check writeByte
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("writeByte") && method.getParameterCount() == 2) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "writeByte first param should be long offset");
          assertEquals(byte.class, params[1], "writeByte second param should be byte value");
        }
      }

      // Check readInt32
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("readInt32") && method.getParameterCount() == 1) {
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(long.class, params[0], "readInt32 should take long offset");
        }
      }

      LOGGER.info("Memory method signatures verified");
    }

    @Test
    @DisplayName("Should implement Memory interface")
    void shouldImplementMemoryInterface() {
      LOGGER.info("Testing WasmMemoryToMemoryAdapter interface implementation");

      final Class<?> clazz = WasmMemoryToMemoryAdapter.class;
      final Class<?>[] interfaces = clazz.getInterfaces();

      boolean implementsMemory = false;
      for (final Class<?> iface : interfaces) {
        if (iface.getSimpleName().equals("Memory")) {
          implementsMemory = true;
          LOGGER.info("Implements: " + iface.getName());
        }
      }

      assertTrue(implementsMemory, "Should implement Memory interface");
    }
  }

  /** Tests for WasmGlobalToGlobalAdapter. */
  @Nested
  @DisplayName("WasmGlobalToGlobalAdapter Tests")
  class GlobalAdapterTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing WasmGlobalToGlobalAdapter class structure");

      final Class<?> clazz = WasmGlobalToGlobalAdapter.class;

      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() {
      LOGGER.info("Testing WasmGlobalToGlobalAdapter methods");

      final Class<?> clazz = WasmGlobalToGlobalAdapter.class;
      final String[] expectedMethods = {
        "getValue",
        "setValue",
        "getValueType",
        "isMutable",
        "isValid",
        "getIntValue",
        "setIntValue",
        "getLongValue",
        "setLongValue",
        "getFloatValue",
        "setFloatValue",
        "getDoubleValue",
        "setDoubleValue",
        "getDelegate"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            LOGGER.info("Found method: " + methodName);
            break;
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }
    }

    @Test
    @DisplayName("Should have typed accessor methods")
    void shouldHaveTypedAccessorMethods() {
      LOGGER.info("Testing typed accessor methods");

      final Class<?> clazz = WasmGlobalToGlobalAdapter.class;

      // Check int accessors
      boolean hasGetInt = false;
      boolean hasSetInt = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getIntValue") && method.getParameterCount() == 0) {
          hasGetInt = true;
          assertEquals(int.class, method.getReturnType(), "getIntValue should return int");
        }
        if (method.getName().equals("setIntValue") && method.getParameterCount() == 1) {
          hasSetInt = true;
          assertEquals(int.class, method.getParameterTypes()[0], "setIntValue should take int");
        }
      }
      assertTrue(hasGetInt && hasSetInt, "Should have int accessors");

      // Check long accessors
      boolean hasGetLong = false;
      boolean hasSetLong = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getLongValue") && method.getParameterCount() == 0) {
          hasGetLong = true;
        }
        if (method.getName().equals("setLongValue") && method.getParameterCount() == 1) {
          hasSetLong = true;
        }
      }
      assertTrue(hasGetLong && hasSetLong, "Should have long accessors");

      LOGGER.info("Typed accessor methods verified");
    }

    @Test
    @DisplayName("Should have conversion method")
    void shouldHaveConversionMethod() {
      LOGGER.info("Testing conversion method");

      final Class<?> clazz = WasmGlobalToGlobalAdapter.class;

      boolean hasConvertToWasmValue = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("convertToWasmValue")) {
          hasConvertToWasmValue = true;
        }
      }

      assertTrue(hasConvertToWasmValue, "Should have convertToWasmValue method");
      LOGGER.info("Conversion method verified");
    }

    @Test
    @DisplayName("Should implement Global interface")
    void shouldImplementGlobalInterface() {
      LOGGER.info("Testing WasmGlobalToGlobalAdapter interface implementation");

      final Class<?> clazz = WasmGlobalToGlobalAdapter.class;
      final Class<?>[] interfaces = clazz.getInterfaces();

      boolean implementsGlobal = false;
      for (final Class<?> iface : interfaces) {
        if (iface.getSimpleName().equals("Global")) {
          implementsGlobal = true;
          LOGGER.info("Implements: " + iface.getName());
        }
      }

      assertTrue(implementsGlobal, "Should implement Global interface");
    }
  }

  /** Cross-adapter integration tests. */
  @Nested
  @DisplayName("Cross-Adapter Integration Tests")
  class CrossAdapterTests {

    @Test
    @DisplayName("Should all have getDelegate method")
    void shouldAllHaveGetDelegateMethod() {
      LOGGER.info("Testing getDelegate method across all adapters");

      final Class<?>[] adapterClasses = {
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class,
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class
      };

      for (final Class<?> clazz : adapterClasses) {
        boolean hasGetDelegate = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals("getDelegate")) {
            hasGetDelegate = true;
            break;
          }
        }
        assertTrue(hasGetDelegate, clazz.getSimpleName() + " should have getDelegate method");
      }

      LOGGER.info("All adapters have getDelegate method");
    }

    @Test
    @DisplayName("Should all have isValid method")
    void shouldAllHaveIsValidMethod() {
      LOGGER.info("Testing isValid method across all adapters");

      final Class<?>[] adapterClasses = {
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class,
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class
      };

      for (final Class<?> clazz : adapterClasses) {
        boolean hasIsValid = false;
        for (final Method method : clazz.getMethods()) {
          if (method.getName().equals("isValid") && method.getParameterCount() == 0) {
            hasIsValid = true;
            assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
            break;
          }
        }
        assertTrue(hasIsValid, clazz.getSimpleName() + " should have isValid method");
      }

      LOGGER.info("All adapters have isValid method");
    }

    @Test
    @DisplayName("Should all be final classes")
    void shouldAllBeFinalClasses() {
      LOGGER.info("Testing final modifier across all adapters");

      final Class<?>[] adapterClasses = {
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class,
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class
      };

      for (final Class<?> clazz : adapterClasses) {
        assertTrue(
            java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
            clazz.getSimpleName() + " should be final");
      }

      LOGGER.info("All adapters are final classes");
    }

    @Test
    @DisplayName("Should all have single constructor with delegate parameter")
    void shouldAllHaveSingleConstructorWithDelegateParameter() {
      LOGGER.info("Testing constructor patterns across all adapters");

      final Class<?>[] adapterClasses = {
        WasmFunctionToFunctionAdapter.class,
        WasmTableToTableAdapter.class,
        WasmMemoryToMemoryAdapter.class,
        WasmGlobalToGlobalAdapter.class
      };

      for (final Class<?> clazz : adapterClasses) {
        final Constructor<?>[] constructors = clazz.getConstructors();
        assertEquals(
            1, constructors.length, clazz.getSimpleName() + " should have exactly 1 constructor");
        assertTrue(
            constructors[0].getParameterCount() >= 1,
            clazz.getSimpleName() + " constructor should take at least 1 parameter");
        LOGGER.info(
            clazz.getSimpleName()
                + " constructor parameters: "
                + constructors[0].getParameterCount());
      }

      LOGGER.info("All adapters have correct constructor pattern");
    }
  }
}
