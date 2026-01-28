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

package ai.tegmentum.wasmtime4j.panama.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.keyvalue.WasiKeyValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiKeyValue} class.
 *
 * <p>PanamaWasiKeyValue provides Panama FFI-based access to WASI keyvalue storage operations. These
 * tests verify the class structure and API contract without requiring native library loading.
 */
@DisplayName("PanamaWasiKeyValue Tests")
class PanamaWasiKeyValueTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiKeyValueTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.panama.wasi.keyvalue.PanamaWasiKeyValue",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing PanamaWasiKeyValue class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiKeyValue should be final");
      LOGGER.info("PanamaWasiKeyValue is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing PanamaWasiKeyValue visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiKeyValue should be public");
      LOGGER.info("PanamaWasiKeyValue is correctly marked as public");
    }

    @Test
    @DisplayName("should implement WasiKeyValue interface")
    void shouldImplementWasiKeyValueInterface() throws ClassNotFoundException {
      LOGGER.info("Testing PanamaWasiKeyValue interface implementation");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          WasiKeyValue.class.isAssignableFrom(clazz),
          "PanamaWasiKeyValue should implement WasiKeyValue");
      LOGGER.info("PanamaWasiKeyValue correctly implements WasiKeyValue interface");
    }

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing PanamaWasiKeyValue constructor");
      final Class<?> clazz = loadClassWithoutInit();
      boolean hasNoArgConstructor = false;

      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (constructor.getParameterCount() == 0) {
          hasNoArgConstructor = true;
          break;
        }
      }

      assertTrue(hasNoArgConstructor, "Should have no-arg constructor for creating keyvalue store");
      LOGGER.info("PanamaWasiKeyValue has no-arg constructor");
    }
  }

  @Nested
  @DisplayName("Panama-Specific Field Tests")
  class PanamaSpecificFieldTests {

    @Test
    @DisplayName("should have contextHandle field")
    void shouldHaveContextHandleField() throws ClassNotFoundException {
      LOGGER.info("Testing contextHandle field");
      final Class<?> clazz = loadClassWithoutInit();

      boolean foundContextHandle = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextHandle")) {
          foundContextHandle = true;
          assertEquals(long.class, field.getType(), "contextHandle should be long type");
          assertTrue(Modifier.isVolatile(field.getModifiers()), "contextHandle should be volatile");
          break;
        }
      }

      assertTrue(foundContextHandle, "Should have contextHandle field for native handle");
      LOGGER.info("contextHandle field verified");
    }

    @Test
    @DisplayName("should have arena field")
    void shouldHaveArenaField() throws ClassNotFoundException {
      LOGGER.info("Testing arena field");
      final Class<?> clazz = loadClassWithoutInit();

      boolean foundArena = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("arena")) {
          foundArena = true;
          assertTrue(
              field.getType().getName().contains("Arena"), "arena field should be Arena type");
          break;
        }
      }

      assertTrue(foundArena, "Should have arena field for memory management");
      LOGGER.info("arena field verified");
    }

    @Test
    @DisplayName("should have closed field")
    void shouldHaveClosedField() throws ClassNotFoundException {
      LOGGER.info("Testing closed field");
      final Class<?> clazz = loadClassWithoutInit();

      boolean foundClosed = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("closed")) {
          foundClosed = true;
          assertEquals(boolean.class, field.getType(), "closed should be boolean type");
          assertTrue(Modifier.isVolatile(field.getModifiers()), "closed should be volatile");
          break;
        }
      }

      assertTrue(foundClosed, "Should have closed field for state tracking");
      LOGGER.info("closed field verified");
    }
  }

  @Nested
  @DisplayName("Basic CRUD Method Tests")
  class BasicCrudMethodTests {

    @Test
    @DisplayName("should have get method with String parameter returning Optional<byte[]>")
    void shouldHaveGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing get method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("get", String.class);

      assertNotNull(method, "get method should exist");
      assertEquals(Optional.class, method.getReturnType(), "get should return Optional");
      assertTrue(Modifier.isPublic(method.getModifiers()), "get should be public");
      LOGGER.info("get method signature verified: " + method);
    }

    @Test
    @DisplayName("should have set method with String and byte[] parameters")
    void shouldHaveSetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing set method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("set", String.class, byte[].class);

      assertNotNull(method, "set method should exist");
      assertEquals(void.class, method.getReturnType(), "set should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "set should be public");
      LOGGER.info("set method signature verified: " + method);
    }

    @Test
    @DisplayName("should have set method with TTL support")
    void shouldHaveSetWithTtlMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing set with TTL method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("set", String.class, byte[].class, Duration.class);

      assertNotNull(method, "set with TTL method should exist");
      assertEquals(void.class, method.getReturnType(), "set with TTL should return void");
      LOGGER.info("set with TTL method signature verified: " + method);
    }

    @Test
    @DisplayName("should have delete method returning boolean")
    void shouldHaveDeleteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing delete method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("delete", String.class);

      assertNotNull(method, "delete method should exist");
      assertEquals(boolean.class, method.getReturnType(), "delete should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "delete should be public");
      LOGGER.info("delete method signature verified: " + method);
    }

    @Test
    @DisplayName("should have exists method returning boolean")
    void shouldHaveExistsMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing exists method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("exists", String.class);

      assertNotNull(method, "exists method should exist");
      assertEquals(boolean.class, method.getReturnType(), "exists should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "exists should be public");
      LOGGER.info("exists method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getEntry method returning Optional<KeyValueEntry>")
    void shouldHaveGetEntryMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getEntry method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getEntry", String.class);

      assertNotNull(method, "getEntry method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getEntry should return Optional");
      LOGGER.info("getEntry method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Multi-Key Operation Method Tests")
  class MultiKeyOperationMethodTests {

    @Test
    @DisplayName("should have getMultiple method with Set parameter returning Map")
    void shouldHaveGetMultipleMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getMultiple method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getMultiple", Set.class);

      assertNotNull(method, "getMultiple method should exist");
      assertEquals(Map.class, method.getReturnType(), "getMultiple should return Map");
      LOGGER.info("getMultiple method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setMultiple method with Map parameter")
    void shouldHaveSetMultipleMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setMultiple method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setMultiple", Map.class);

      assertNotNull(method, "setMultiple method should exist");
      assertEquals(void.class, method.getReturnType(), "setMultiple should return void");
      LOGGER.info("setMultiple method signature verified: " + method);
    }

    @Test
    @DisplayName("should have deleteMultiple method with Set parameter returning Set")
    void shouldHaveDeleteMultipleMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing deleteMultiple method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("deleteMultiple", Set.class);

      assertNotNull(method, "deleteMultiple method should exist");
      assertEquals(Set.class, method.getReturnType(), "deleteMultiple should return Set");
      LOGGER.info("deleteMultiple method signature verified: " + method);
    }

    @Test
    @DisplayName("should have keys method returning Set")
    void shouldHaveKeysMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing keys method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("keys");

      assertNotNull(method, "keys method should exist");
      assertEquals(Set.class, method.getReturnType(), "keys should return Set");
      LOGGER.info("keys method signature verified: " + method);
    }

    @Test
    @DisplayName("should have keys method with pattern parameter")
    void shouldHaveKeysWithPatternMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing keys with pattern method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("keys", String.class);

      assertNotNull(method, "keys with pattern method should exist");
      assertEquals(Set.class, method.getReturnType(), "keys should return Set");
      LOGGER.info("keys with pattern method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Atomic Operation Method Tests")
  class AtomicOperationMethodTests {

    @Test
    @DisplayName("should have setIfAbsent method returning boolean")
    void shouldHaveSetIfAbsentMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setIfAbsent method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setIfAbsent", String.class, byte[].class);

      assertNotNull(method, "setIfAbsent method should exist");
      assertEquals(boolean.class, method.getReturnType(), "setIfAbsent should return boolean");
      LOGGER.info("setIfAbsent method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setIfPresent method returning boolean")
    void shouldHaveSetIfPresentMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setIfPresent method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setIfPresent", String.class, byte[].class);

      assertNotNull(method, "setIfPresent method should exist");
      assertEquals(boolean.class, method.getReturnType(), "setIfPresent should return boolean");
      LOGGER.info("setIfPresent method signature verified: " + method);
    }

    @Test
    @DisplayName("should have compareAndSwap method returning boolean")
    void shouldHaveCompareAndSwapMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing compareAndSwap method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("compareAndSwap", String.class, byte[].class, byte[].class);

      assertNotNull(method, "compareAndSwap method should exist");
      assertEquals(boolean.class, method.getReturnType(), "compareAndSwap should return boolean");
      LOGGER.info("compareAndSwap method signature verified: " + method);
    }

    @Test
    @DisplayName("should have compareVersionAndSwap method")
    void shouldHaveCompareVersionAndSwapMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing compareVersionAndSwap method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod("compareVersionAndSwap", String.class, long.class, byte[].class);

      assertNotNull(method, "compareVersionAndSwap method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "compareVersionAndSwap should return boolean");
      LOGGER.info("compareVersionAndSwap method signature verified: " + method);
    }

    @Test
    @DisplayName("should have increment method returning long")
    void shouldHaveIncrementMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing increment method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("increment", String.class, long.class);

      assertNotNull(method, "increment method should exist");
      assertEquals(long.class, method.getReturnType(), "increment should return long");
      LOGGER.info("increment method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getAndDelete method returning Optional")
    void shouldHaveGetAndDeleteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getAndDelete method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getAndDelete", String.class);

      assertNotNull(method, "getAndDelete method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getAndDelete should return Optional");
      LOGGER.info("getAndDelete method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getAndSet method returning Optional")
    void shouldHaveGetAndSetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getAndSet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getAndSet", String.class, byte[].class);

      assertNotNull(method, "getAndSet method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getAndSet should return Optional");
      LOGGER.info("getAndSet method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("List Operation Method Tests")
  class ListOperationMethodTests {

    @Test
    @DisplayName("should have listAppend method")
    void shouldHaveListAppendMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listAppend method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listAppend", String.class, List.class);

      assertNotNull(method, "listAppend method should exist");
      assertEquals(long.class, method.getReturnType(), "listAppend should return long");
      LOGGER.info("listAppend method signature verified: " + method);
    }

    @Test
    @DisplayName("should have listPrepend method")
    void shouldHaveListPrependMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listPrepend method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listPrepend", String.class, List.class);

      assertNotNull(method, "listPrepend method should exist");
      assertEquals(long.class, method.getReturnType(), "listPrepend should return long");
      LOGGER.info("listPrepend method signature verified: " + method);
    }

    @Test
    @DisplayName("should have listRange method")
    void shouldHaveListRangeMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listRange method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listRange", String.class, long.class, long.class);

      assertNotNull(method, "listRange method should exist");
      assertEquals(List.class, method.getReturnType(), "listRange should return List");
      LOGGER.info("listRange method signature verified: " + method);
    }

    @Test
    @DisplayName("should have listLength method")
    void shouldHaveListLengthMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listLength method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listLength", String.class);

      assertNotNull(method, "listLength method should exist");
      assertEquals(long.class, method.getReturnType(), "listLength should return long");
      LOGGER.info("listLength method signature verified: " + method);
    }

    @Test
    @DisplayName("should have listPop method")
    void shouldHaveListPopMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listPop method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listPop", String.class);

      assertNotNull(method, "listPop method should exist");
      assertEquals(Optional.class, method.getReturnType(), "listPop should return Optional");
      LOGGER.info("listPop method signature verified: " + method);
    }

    @Test
    @DisplayName("should have listShift method")
    void shouldHaveListShiftMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing listShift method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("listShift", String.class);

      assertNotNull(method, "listShift method should exist");
      assertEquals(Optional.class, method.getReturnType(), "listShift should return Optional");
      LOGGER.info("listShift method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Set Operation Method Tests")
  class SetOperationMethodTests {

    @Test
    @DisplayName("should have setAdd method")
    void shouldHaveSetAddMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setAdd method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setAdd", String.class, Set.class);

      assertNotNull(method, "setAdd method should exist");
      assertEquals(long.class, method.getReturnType(), "setAdd should return long");
      LOGGER.info("setAdd method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setRemove method")
    void shouldHaveSetRemoveMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setRemove method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setRemove", String.class, Set.class);

      assertNotNull(method, "setRemove method should exist");
      assertEquals(long.class, method.getReturnType(), "setRemove should return long");
      LOGGER.info("setRemove method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setMembers method")
    void shouldHaveSetMembersMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setMembers method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setMembers", String.class);

      assertNotNull(method, "setMembers method should exist");
      assertEquals(Set.class, method.getReturnType(), "setMembers should return Set");
      LOGGER.info("setMembers method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setIsMember method")
    void shouldHaveSetIsMemberMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setIsMember method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setIsMember", String.class, byte[].class);

      assertNotNull(method, "setIsMember method should exist");
      assertEquals(boolean.class, method.getReturnType(), "setIsMember should return boolean");
      LOGGER.info("setIsMember method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setSize method")
    void shouldHaveSetSizeMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setSize method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setSize", String.class);

      assertNotNull(method, "setSize method should exist");
      assertEquals(long.class, method.getReturnType(), "setSize should return long");
      LOGGER.info("setSize method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Hash Operation Method Tests")
  class HashOperationMethodTests {

    @Test
    @DisplayName("should have hashSet method")
    void shouldHaveHashSetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashSet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashSet", String.class, String.class, byte[].class);

      assertNotNull(method, "hashSet method should exist");
      assertEquals(void.class, method.getReturnType(), "hashSet should return void");
      LOGGER.info("hashSet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have hashGet method")
    void shouldHaveHashGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashGet", String.class, String.class);

      assertNotNull(method, "hashGet method should exist");
      assertEquals(Optional.class, method.getReturnType(), "hashGet should return Optional");
      LOGGER.info("hashGet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have hashDelete method")
    void shouldHaveHashDeleteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashDelete method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashDelete", String.class, String.class);

      assertNotNull(method, "hashDelete method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hashDelete should return boolean");
      LOGGER.info("hashDelete method signature verified: " + method);
    }

    @Test
    @DisplayName("should have hashGetAll method")
    void shouldHaveHashGetAllMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashGetAll method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashGetAll", String.class);

      assertNotNull(method, "hashGetAll method should exist");
      assertEquals(Map.class, method.getReturnType(), "hashGetAll should return Map");
      LOGGER.info("hashGetAll method signature verified: " + method);
    }

    @Test
    @DisplayName("should have hashKeys method")
    void shouldHaveHashKeysMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashKeys method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashKeys", String.class);

      assertNotNull(method, "hashKeys method should exist");
      assertEquals(Set.class, method.getReturnType(), "hashKeys should return Set");
      LOGGER.info("hashKeys method signature verified: " + method);
    }

    @Test
    @DisplayName("should have hashExists method")
    void shouldHaveHashExistsMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing hashExists method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("hashExists", String.class, String.class);

      assertNotNull(method, "hashExists method should exist");
      assertEquals(boolean.class, method.getReturnType(), "hashExists should return boolean");
      LOGGER.info("hashExists method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Transaction Method Tests")
  class TransactionMethodTests {

    @Test
    @DisplayName("should have beginTransaction method")
    void shouldHaveBeginTransactionMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing beginTransaction method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("beginTransaction");

      assertNotNull(method, "beginTransaction method should exist");
      LOGGER.info("beginTransaction method signature verified: " + method);
    }

    @Test
    @DisplayName("should have beginTransaction method with IsolationLevel parameter")
    void shouldHaveBeginTransactionWithIsolationLevelMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing beginTransaction with IsolationLevel method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Class<?> isolationLevelClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.wasi.keyvalue.IsolationLevel",
              false,
              getClass().getClassLoader());
      final Method method = clazz.getMethod("beginTransaction", isolationLevelClass);

      assertNotNull(method, "beginTransaction with IsolationLevel method should exist");
      LOGGER.info("beginTransaction with IsolationLevel method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Store Management Method Tests")
  class StoreManagementMethodTests {

    @Test
    @DisplayName("should have size method returning long")
    void shouldHaveSizeMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing size method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("size");

      assertNotNull(method, "size method should exist");
      assertEquals(long.class, method.getReturnType(), "size should return long");
      LOGGER.info("size method signature verified: " + method);
    }

    @Test
    @DisplayName("should have isEmpty method returning boolean")
    void shouldHaveIsEmptyMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing isEmpty method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("isEmpty");

      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isEmpty should return boolean");
      LOGGER.info("isEmpty method signature verified: " + method);
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing clear method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("clear");

      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "clear should return void");
      LOGGER.info("clear method signature verified: " + method);
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing close method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("close");

      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      LOGGER.info("close method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getConsistencyModel method")
    void shouldHaveGetConsistencyModelMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getConsistencyModel method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getConsistencyModel");

      assertNotNull(method, "getConsistencyModel method should exist");
      LOGGER.info("getConsistencyModel method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setConsistencyModel method")
    void shouldHaveSetConsistencyModelMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setConsistencyModel method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Class<?> consistencyModelClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.wasi.keyvalue.ConsistencyModel",
              false,
              getClass().getClassLoader());
      final Method method = clazz.getMethod("setConsistencyModel", consistencyModelClass);

      assertNotNull(method, "setConsistencyModel method should exist");
      assertEquals(void.class, method.getReturnType(), "setConsistencyModel should return void");
      LOGGER.info("setConsistencyModel method signature verified: " + method);
    }

    @Test
    @DisplayName("should have getEvictionPolicy method")
    void shouldHaveGetEvictionPolicyMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getEvictionPolicy method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getEvictionPolicy");

      assertNotNull(method, "getEvictionPolicy method should exist");
      LOGGER.info("getEvictionPolicy method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("TTL Operation Method Tests")
  class TtlOperationMethodTests {

    @Test
    @DisplayName("should have getTtl method returning Optional<Duration>")
    void shouldHaveGetTtlMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getTtl method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getTtl", String.class);

      assertNotNull(method, "getTtl method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getTtl should return Optional");
      LOGGER.info("getTtl method signature verified: " + method);
    }

    @Test
    @DisplayName("should have setTtl method returning boolean")
    void shouldHaveSetTtlMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing setTtl method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("setTtl", String.class, Duration.class);

      assertNotNull(method, "setTtl method should exist");
      assertEquals(boolean.class, method.getReturnType(), "setTtl should return boolean");
      LOGGER.info("setTtl method signature verified: " + method);
    }

    @Test
    @DisplayName("should have persist method returning boolean")
    void shouldHavePersistMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing persist method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("persist", String.class);

      assertNotNull(method, "persist method should exist");
      assertEquals(boolean.class, method.getReturnType(), "persist should return boolean");
      LOGGER.info("persist method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have static isAvailable method")
    void shouldHaveStaticIsAvailableMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing static isAvailable method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("isAvailable");

      assertNotNull(method, "isAvailable method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "isAvailable should return boolean");
      LOGGER.info("Static isAvailable method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Panama Method Handle Field Tests")
  class PanamaMethodHandleFieldTests {

    @Test
    @DisplayName("should have static final method handle fields")
    void shouldHaveStaticFinalMethodHandleFields() throws ClassNotFoundException {
      LOGGER.info("Testing Panama method handle fields");
      final Class<?> clazz = loadClassWithoutInit();

      final Set<String> expectedMethodHandles =
          new HashSet<>(
              Arrays.asList(
                  "CREATE_CONTEXT",
                  "DESTROY_CONTEXT",
                  "GET",
                  "SET",
                  "DELETE",
                  "EXISTS",
                  "INCREMENT",
                  "SIZE",
                  "CLEAR",
                  "KEYS",
                  "FREE_BYTES",
                  "FREE_STRING",
                  "IS_AVAILABLE"));

      int foundMethodHandles = 0;
      for (final Field field : clazz.getDeclaredFields()) {
        if (Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())
            && field.getType().getName().contains("MethodHandle")) {
          foundMethodHandles++;
          LOGGER.info("Found method handle field: " + field.getName());
          assertTrue(
              expectedMethodHandles.contains(field.getName()),
              "Method handle " + field.getName() + " should be in expected set");
        }
      }

      assertTrue(
          foundMethodHandles >= expectedMethodHandles.size(),
          "Should have at least "
              + expectedMethodHandles.size()
              + " method handle fields, found: "
              + foundMethodHandles);
      LOGGER.info("Method handle fields verified: " + foundMethodHandles + " fields");
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountVerificationTests {

    @Test
    @DisplayName("should have all WasiKeyValue interface methods implemented")
    void shouldHaveAllInterfaceMethodsImplemented() throws ClassNotFoundException {
      LOGGER.info("Testing WasiKeyValue interface method implementation");
      final Class<?> clazz = loadClassWithoutInit();

      final Method[] interfaceMethods = WasiKeyValue.class.getMethods();
      final Set<String> missingMethods = new HashSet<>();

      for (final Method interfaceMethod : interfaceMethods) {
        try {
          clazz.getMethod(interfaceMethod.getName(), interfaceMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
          missingMethods.add(interfaceMethod.getName());
        }
      }

      assertTrue(
          missingMethods.isEmpty(),
          "All WasiKeyValue interface methods should be implemented. Missing: " + missingMethods);
      LOGGER.info(
          "All " + interfaceMethods.length + " WasiKeyValue interface methods are implemented");
    }

    @Test
    @DisplayName("should have substantial number of public methods")
    void shouldHaveSubstantialNumberOfPublicMethods() throws ClassNotFoundException {
      LOGGER.info("Testing public method count");
      final Class<?> clazz = loadClassWithoutInit();

      int publicMethodCount = 0;
      for (final Method method : clazz.getMethods()) {
        if (method.getDeclaringClass() == clazz && Modifier.isPublic(method.getModifiers())) {
          publicMethodCount++;
        }
      }

      // PanamaWasiKeyValue has isAvailable as additional public method
      assertTrue(
          publicMethodCount >= 1,
          "Should have at least 1 class-specific public method, found: " + publicMethodCount);
      LOGGER.info("Found " + publicMethodCount + " class-specific public methods");
    }
  }
}
