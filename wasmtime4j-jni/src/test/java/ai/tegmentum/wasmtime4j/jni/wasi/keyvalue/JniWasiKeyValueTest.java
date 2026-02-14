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

package ai.tegmentum.wasmtime4j.jni.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiKeyValue} class structure and API contract.
 *
 * <p>Verifies the class implements the WasiKeyValue interface with correct method signatures. These
 * tests do not require native library loading.
 */
@DisplayName("JniWasiKeyValue Tests")
class JniWasiKeyValueTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiKeyValueTest.class.getName());

  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.keyvalue.JniWasiKeyValue",
        false,
        getClass().getClassLoader());
  }

  @Test
  @DisplayName("should be a final public class implementing WasiKeyValue")
  void shouldHaveCorrectClassStructure() throws ClassNotFoundException {
    LOGGER.info("Testing JniWasiKeyValue class structure");
    final Class<?> clazz = loadClassWithoutInit();
    assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniWasiKeyValue should be final");
    assertTrue(Modifier.isPublic(clazz.getModifiers()), "JniWasiKeyValue should be public");
    assertTrue(
        WasiKeyValue.class.isAssignableFrom(clazz),
        "JniWasiKeyValue should implement WasiKeyValue");
    LOGGER.info("Class structure verified: final public class implementing WasiKeyValue");
  }

  @Test
  @DisplayName("should have core CRUD methods matching WasiKeyValue interface")
  void shouldHaveCoreCrudMethods() throws ClassNotFoundException, NoSuchMethodException {
    LOGGER.info("Testing core CRUD method signatures");
    final Class<?> clazz = loadClassWithoutInit();

    Method get = clazz.getMethod("get", String.class);
    assertEquals(Optional.class, get.getReturnType(), "get should return Optional");

    Method set = clazz.getMethod("set", String.class, byte[].class);
    assertEquals(void.class, set.getReturnType(), "set should return void");

    Method delete = clazz.getMethod("delete", String.class);
    assertEquals(boolean.class, delete.getReturnType(), "delete should return boolean");

    Method exists = clazz.getMethod("exists", String.class);
    assertEquals(boolean.class, exists.getReturnType(), "exists should return boolean");

    Method keys = clazz.getMethod("keys");
    assertEquals(Set.class, keys.getReturnType(), "keys should return Set");

    Method getEntry = clazz.getMethod("getEntry", String.class);
    assertEquals(Optional.class, getEntry.getReturnType(), "getEntry should return Optional");

    Method close = clazz.getMethod("close");
    assertNotNull(close, "close method should exist");

    LOGGER.info("All core CRUD method signatures verified");
  }

  @Test
  @DisplayName("should have static isAvailable method")
  void shouldHaveIsAvailableMethod() throws ClassNotFoundException, NoSuchMethodException {
    LOGGER.info("Testing static isAvailable method");
    final Class<?> clazz = loadClassWithoutInit();
    final Method method = clazz.getMethod("isAvailable");
    assertTrue(Modifier.isStatic(method.getModifiers()), "isAvailable should be static");
    assertEquals(boolean.class, method.getReturnType(), "isAvailable should return boolean");
    LOGGER.info("Static isAvailable method verified");
  }

  @Test
  @DisplayName("should have all WasiKeyValue interface methods implemented")
  void shouldImplementAllInterfaceMethods() throws ClassNotFoundException {
    LOGGER.info("Testing WasiKeyValue interface method coverage");
    final Class<?> clazz = loadClassWithoutInit();
    for (final Method interfaceMethod : WasiKeyValue.class.getMethods()) {
      try {
        clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
      } catch (NoSuchMethodException e) {
        throw new AssertionError(
            "Missing WasiKeyValue interface method: " + interfaceMethod.getName(), e);
      }
    }
    LOGGER.info("All WasiKeyValue interface methods are implemented");
  }
}
