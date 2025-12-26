/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive API tests for WASI-keyvalue package.
 *
 * <p>These tests verify the API contracts, class structures, and method signatures for the keyvalue
 * package without requiring native runtime initialization.
 *
 * @since 1.0.0
 */
@DisplayName("WASI KeyValue API Tests")
class WasiKeyValueApiTest {

  private static final Logger LOGGER = Logger.getLogger(WasiKeyValueApiTest.class.getName());

  // ==================== ConsistencyModel Enum Tests ====================

  @Nested
  @DisplayName("ConsistencyModel Enum Tests")
  class ConsistencyModelTests {

    @Test
    @DisplayName("ConsistencyModel should have all expected values")
    void shouldHaveAllExpectedValues() {
      LOGGER.info("Testing ConsistencyModel enum values");
      ConsistencyModel[] values = ConsistencyModel.values();
      assertEquals(8, values.length, "ConsistencyModel should have 8 values");

      Set<String> expectedValues =
          new HashSet<>(
              Arrays.asList(
                  "EVENTUAL",
                  "STRONG",
                  "CAUSAL",
                  "SEQUENTIAL",
                  "LINEARIZABLE",
                  "SESSION",
                  "MONOTONIC_READ",
                  "MONOTONIC_WRITE"));

      for (ConsistencyModel model : values) {
        LOGGER.info("  Found model: " + model.name());
        assertTrue(
            expectedValues.contains(model.name()), "Unexpected consistency model: " + model.name());
      }
    }

    @Test
    @DisplayName("ConsistencyModel valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(ConsistencyModel.EVENTUAL, ConsistencyModel.valueOf("EVENTUAL"));
      assertEquals(ConsistencyModel.STRONG, ConsistencyModel.valueOf("STRONG"));
      assertEquals(ConsistencyModel.CAUSAL, ConsistencyModel.valueOf("CAUSAL"));
      assertEquals(ConsistencyModel.LINEARIZABLE, ConsistencyModel.valueOf("LINEARIZABLE"));
    }

    @Test
    @DisplayName("ConsistencyModel ordinals should be sequential")
    void ordinalsShouldBeSequential() {
      ConsistencyModel[] values = ConsistencyModel.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal mismatch at index " + i);
      }
    }

    @Test
    @DisplayName("ConsistencyModel should be an enum")
    void shouldBeEnum() {
      assertTrue(ConsistencyModel.class.isEnum(), "ConsistencyModel should be an enum");
    }
  }

  // ==================== IsolationLevel Enum Tests ====================

  @Nested
  @DisplayName("IsolationLevel Enum Tests")
  class IsolationLevelTests {

    @Test
    @DisplayName("IsolationLevel should have all expected values")
    void shouldHaveAllExpectedValues() {
      LOGGER.info("Testing IsolationLevel enum values");
      IsolationLevel[] values = IsolationLevel.values();
      assertEquals(5, values.length, "IsolationLevel should have 5 values");

      Set<String> expectedValues =
          new HashSet<>(
              Arrays.asList(
                  "READ_UNCOMMITTED",
                  "READ_COMMITTED",
                  "REPEATABLE_READ",
                  "SERIALIZABLE",
                  "SNAPSHOT"));

      for (IsolationLevel level : values) {
        LOGGER.info("  Found isolation level: " + level.name());
        assertTrue(
            expectedValues.contains(level.name()), "Unexpected isolation level: " + level.name());
      }
    }

    @Test
    @DisplayName("IsolationLevel valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(IsolationLevel.READ_UNCOMMITTED, IsolationLevel.valueOf("READ_UNCOMMITTED"));
      assertEquals(IsolationLevel.READ_COMMITTED, IsolationLevel.valueOf("READ_COMMITTED"));
      assertEquals(IsolationLevel.SERIALIZABLE, IsolationLevel.valueOf("SERIALIZABLE"));
      assertEquals(IsolationLevel.SNAPSHOT, IsolationLevel.valueOf("SNAPSHOT"));
    }

    @Test
    @DisplayName("IsolationLevel should be an enum")
    void shouldBeEnum() {
      assertTrue(IsolationLevel.class.isEnum(), "IsolationLevel should be an enum");
    }
  }

  // ==================== EvictionPolicy Enum Tests ====================

  @Nested
  @DisplayName("EvictionPolicy Enum Tests")
  class EvictionPolicyTests {

    @Test
    @DisplayName("EvictionPolicy should have all expected values")
    void shouldHaveAllExpectedValues() {
      LOGGER.info("Testing EvictionPolicy enum values");
      EvictionPolicy[] values = EvictionPolicy.values();
      assertEquals(7, values.length, "EvictionPolicy should have 7 values");

      Set<String> expectedValues =
          new HashSet<>(Arrays.asList("LRU", "LFU", "FIFO", "TTL", "SIZE_BASED", "RANDOM", "NONE"));

      for (EvictionPolicy policy : values) {
        LOGGER.info("  Found eviction policy: " + policy.name());
        assertTrue(
            expectedValues.contains(policy.name()), "Unexpected eviction policy: " + policy.name());
      }
    }

    @Test
    @DisplayName("EvictionPolicy valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(EvictionPolicy.LRU, EvictionPolicy.valueOf("LRU"));
      assertEquals(EvictionPolicy.LFU, EvictionPolicy.valueOf("LFU"));
      assertEquals(EvictionPolicy.FIFO, EvictionPolicy.valueOf("FIFO"));
      assertEquals(EvictionPolicy.NONE, EvictionPolicy.valueOf("NONE"));
    }

    @Test
    @DisplayName("EvictionPolicy should be an enum")
    void shouldBeEnum() {
      assertTrue(EvictionPolicy.class.isEnum(), "EvictionPolicy should be an enum");
    }
  }

  // ==================== KeyValueErrorCode Enum Tests ====================

  @Nested
  @DisplayName("KeyValueErrorCode Enum Tests")
  class KeyValueErrorCodeTests {

    @Test
    @DisplayName("KeyValueErrorCode should have all expected values")
    void shouldHaveAllExpectedValues() {
      LOGGER.info("Testing KeyValueErrorCode enum values");
      KeyValueErrorCode[] values = KeyValueErrorCode.values();
      assertEquals(16, values.length, "KeyValueErrorCode should have 16 values");

      Set<String> expectedValues =
          new HashSet<>(
              Arrays.asList(
                  "UNKNOWN",
                  "KEY_NOT_FOUND",
                  "KEY_EXISTS",
                  "INVALID_KEY",
                  "INVALID_VALUE",
                  "CAPACITY_EXCEEDED",
                  "TRANSACTION_CONFLICT",
                  "TRANSACTION_TIMEOUT",
                  "CAS_FAILED",
                  "NOT_PERMITTED",
                  "CONNECTION_FAILED",
                  "READ_ONLY",
                  "TIMEOUT",
                  "CONSISTENCY_VIOLATION",
                  "REPLICATION_FAILED",
                  "INTERNAL_ERROR"));

      for (KeyValueErrorCode code : values) {
        LOGGER.info("  Found error code: " + code.name());
        assertTrue(expectedValues.contains(code.name()), "Unexpected error code: " + code.name());
      }
    }

    @Test
    @DisplayName("KeyValueErrorCode valueOf should work correctly")
    void valueOfShouldWork() {
      assertEquals(KeyValueErrorCode.UNKNOWN, KeyValueErrorCode.valueOf("UNKNOWN"));
      assertEquals(KeyValueErrorCode.KEY_NOT_FOUND, KeyValueErrorCode.valueOf("KEY_NOT_FOUND"));
      assertEquals(KeyValueErrorCode.CAS_FAILED, KeyValueErrorCode.valueOf("CAS_FAILED"));
      assertEquals(KeyValueErrorCode.TIMEOUT, KeyValueErrorCode.valueOf("TIMEOUT"));
    }

    @Test
    @DisplayName("KeyValueErrorCode should be an enum")
    void shouldBeEnum() {
      assertTrue(KeyValueErrorCode.class.isEnum(), "KeyValueErrorCode should be an enum");
    }
  }

  // ==================== KeyValueException Tests ====================

  @Nested
  @DisplayName("KeyValueException Tests")
  class KeyValueExceptionTests {

    @Test
    @DisplayName("KeyValueException should extend WasmException")
    void shouldExtendWasmException() {
      assertTrue(
          WasmException.class.isAssignableFrom(KeyValueException.class),
          "KeyValueException should extend WasmException");
    }

    @Test
    @DisplayName("KeyValueException constructor with message should work")
    void constructorWithMessageShouldWork() {
      String message = "Test error message";
      KeyValueException exception = new KeyValueException(message);
      assertEquals(message, exception.getMessage());
      assertEquals(
          KeyValueErrorCode.UNKNOWN,
          exception.getErrorCode(),
          "Default error code should be UNKNOWN");
    }

    @Test
    @DisplayName("KeyValueException constructor with message and error code should work")
    void constructorWithMessageAndCodeShouldWork() {
      String message = "Key not found error";
      KeyValueException exception = new KeyValueException(message, KeyValueErrorCode.KEY_NOT_FOUND);
      assertEquals(message, exception.getMessage());
      assertEquals(KeyValueErrorCode.KEY_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("KeyValueException constructor with message and cause should work")
    void constructorWithMessageAndCauseShouldWork() {
      String message = "Test error with cause";
      Throwable cause = new RuntimeException("Underlying cause");
      KeyValueException exception = new KeyValueException(message, cause);
      assertEquals(message, exception.getMessage());
      assertEquals(cause, exception.getCause());
      assertEquals(KeyValueErrorCode.UNKNOWN, exception.getErrorCode());
    }

    @Test
    @DisplayName("KeyValueException constructor with all parameters should work")
    void constructorWithAllParametersShouldWork() {
      String message = "Complete error";
      KeyValueErrorCode code = KeyValueErrorCode.TRANSACTION_CONFLICT;
      Throwable cause = new RuntimeException("Conflict cause");
      KeyValueException exception = new KeyValueException(message, code, cause);
      assertEquals(message, exception.getMessage());
      assertEquals(code, exception.getErrorCode());
      assertEquals(cause, exception.getCause());
    }

    @Test
    @DisplayName("KeyValueException should have getErrorCode method")
    void shouldHaveGetErrorCodeMethod() throws NoSuchMethodException {
      Method method = KeyValueException.class.getMethod("getErrorCode");
      assertNotNull(method, "getErrorCode method should exist");
      assertEquals(KeyValueErrorCode.class, method.getReturnType());
    }

    @Test
    @DisplayName("KeyValueException should have serialVersionUID")
    void shouldHaveSerialVersionUID() throws NoSuchFieldException {
      java.lang.reflect.Field field = KeyValueException.class.getDeclaredField("serialVersionUID");
      assertTrue(Modifier.isStatic(field.getModifiers()), "serialVersionUID should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "serialVersionUID should be final");
    }
  }

  // ==================== KeyValueEntry Tests ====================

  @Nested
  @DisplayName("KeyValueEntry Tests")
  class KeyValueEntryTests {

    @Test
    @DisplayName("KeyValueEntry should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(KeyValueEntry.class.getModifiers()),
          "KeyValueEntry should be a final class");
    }

    @Test
    @DisplayName("KeyValueEntry builder should create entry correctly")
    void builderShouldCreateEntryCorrectly() {
      String key = "testKey";
      byte[] value = "testValue".getBytes();
      Instant now = Instant.now();

      KeyValueEntry entry =
          KeyValueEntry.builder(key, value)
              .version(5)
              .createdAt(now)
              .modifiedAt(now)
              .expiresAt(now.plusSeconds(3600))
              .build();

      assertEquals(key, entry.getKey());
      assertArrayEquals(value, entry.getValue());
      assertEquals(5, entry.getVersion());
      assertTrue(entry.getCreatedAt().isPresent());
      assertEquals(now, entry.getCreatedAt().get());
      assertTrue(entry.getExpiresAt().isPresent());
    }

    @Test
    @DisplayName("KeyValueEntry builder with minimal parameters should work")
    void builderWithMinimalParametersShouldWork() {
      String key = "simpleKey";
      byte[] value = "simpleValue".getBytes();

      KeyValueEntry entry = KeyValueEntry.builder(key, value).build();

      assertEquals(key, entry.getKey());
      assertArrayEquals(value, entry.getValue());
      assertEquals(1, entry.getVersion(), "Default version should be 1");
      assertFalse(entry.getCreatedAt().isPresent());
      assertFalse(entry.getModifiedAt().isPresent());
      assertFalse(entry.getExpiresAt().isPresent());
    }

    @Test
    @DisplayName("KeyValueEntry should handle null value correctly")
    void shouldHandleNullValueCorrectly() {
      KeyValueEntry entry = KeyValueEntry.builder("key", null).build();
      assertNull(entry.getValue(), "Null value should be preserved");
    }

    @Test
    @DisplayName("KeyValueEntry value should be defensive copied")
    void valueShouldBeDefensiveCopied() {
      byte[] originalValue = "original".getBytes();
      KeyValueEntry entry = KeyValueEntry.builder("key", originalValue).build();

      // Modify original
      originalValue[0] = 'X';

      byte[] retrievedValue = entry.getValue();
      assertNotEquals(
          originalValue[0], retrievedValue[0], "Entry should have defensive copy of value");
    }

    @Test
    @DisplayName("KeyValueEntry isExpired should work correctly")
    void isExpiredShouldWorkCorrectly() {
      KeyValueEntry notExpired =
          KeyValueEntry.builder("key", "value".getBytes())
              .expiresAt(Instant.now().plusSeconds(3600))
              .build();
      assertFalse(notExpired.isExpired(), "Entry should not be expired");

      KeyValueEntry expired =
          KeyValueEntry.builder("key", "value".getBytes())
              .expiresAt(Instant.now().minusSeconds(3600))
              .build();
      assertTrue(expired.isExpired(), "Entry should be expired");
    }

    @Test
    @DisplayName("KeyValueEntry equals should compare key and version")
    void equalsShouldCompareKeyAndVersion() {
      KeyValueEntry entry1 = KeyValueEntry.builder("key1", "value1".getBytes()).version(1).build();
      KeyValueEntry entry2 = KeyValueEntry.builder("key1", "value2".getBytes()).version(1).build();
      KeyValueEntry entry3 = KeyValueEntry.builder("key1", "value1".getBytes()).version(2).build();
      KeyValueEntry entry4 = KeyValueEntry.builder("key2", "value1".getBytes()).version(1).build();

      assertEquals(entry1, entry2, "Same key and version should be equal");
      assertNotEquals(entry1, entry3, "Different versions should not be equal");
      assertNotEquals(entry1, entry4, "Different keys should not be equal");
    }

    @Test
    @DisplayName("KeyValueEntry hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      KeyValueEntry entry1 = KeyValueEntry.builder("key1", "value1".getBytes()).version(1).build();
      KeyValueEntry entry2 = KeyValueEntry.builder("key1", "value2".getBytes()).version(1).build();

      assertEquals(entry1.hashCode(), entry2.hashCode(), "Equal entries should have same hashCode");
    }

    @Test
    @DisplayName("KeyValueEntry toString should contain key and version")
    void toStringShouldContainKeyAndVersion() {
      KeyValueEntry entry =
          KeyValueEntry.builder("testKey", "testValue".getBytes()).version(42).build();
      String str = entry.toString();
      assertTrue(str.contains("testKey"), "toString should contain key");
      assertTrue(str.contains("42"), "toString should contain version");
    }
  }

  // ==================== KeyValueEntry.Builder Tests ====================

  @Nested
  @DisplayName("KeyValueEntry.Builder Tests")
  class KeyValueEntryBuilderTests {

    @Test
    @DisplayName("Builder should be a static inner class")
    void shouldBeStaticInnerClass() throws ClassNotFoundException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry$Builder");
      assertTrue(
          Modifier.isStatic(builderClass.getModifiers()), "Builder should be a static class");
      assertTrue(Modifier.isFinal(builderClass.getModifiers()), "Builder should be final");
      assertTrue(Modifier.isPublic(builderClass.getModifiers()), "Builder should be public");
    }

    @Test
    @DisplayName("Builder methods should return this for chaining")
    void builderMethodsShouldReturnThis() {
      KeyValueEntry.Builder builder = KeyValueEntry.builder("key", "value".getBytes());
      assertNotNull(builder.version(1), "version() should return builder");
      assertNotNull(builder.createdAt(Instant.now()), "createdAt() should return builder");
      assertNotNull(builder.modifiedAt(Instant.now()), "modifiedAt() should return builder");
      assertNotNull(builder.expiresAt(Instant.now()), "expiresAt() should return builder");
    }

    @Test
    @DisplayName("Builder should have all expected methods")
    void shouldHaveAllExpectedMethods() throws ClassNotFoundException {
      Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.wasi.keyvalue.KeyValueEntry$Builder");

      Set<String> expectedMethods =
          new HashSet<>(Arrays.asList("version", "createdAt", "modifiedAt", "expiresAt", "build"));

      for (Method method : builderClass.getDeclaredMethods()) {
        if (expectedMethods.contains(method.getName())) {
          LOGGER.info("  Found builder method: " + method.getName());
          expectedMethods.remove(method.getName());
        }
      }

      assertTrue(expectedMethods.isEmpty(), "Missing builder methods: " + expectedMethods);
    }
  }

  // ==================== WasiKeyValue Interface Tests ====================

  @Nested
  @DisplayName("WasiKeyValue Interface Tests")
  class WasiKeyValueInterfaceTests {

    @Test
    @DisplayName("WasiKeyValue should be an interface")
    void shouldBeInterface() {
      assertTrue(WasiKeyValue.class.isInterface(), "WasiKeyValue should be an interface");
    }

    @Test
    @DisplayName("WasiKeyValue should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasiKeyValue.class),
          "WasiKeyValue should extend AutoCloseable");
    }

    @Test
    @DisplayName("WasiKeyValue should have basic CRUD methods")
    void shouldHaveBasicCrudMethods() throws NoSuchMethodException {
      // get
      Method get = WasiKeyValue.class.getMethod("get", String.class);
      assertEquals(Optional.class, get.getReturnType());

      // getEntry
      Method getEntry = WasiKeyValue.class.getMethod("getEntry", String.class);
      assertEquals(Optional.class, getEntry.getReturnType());

      // set
      Method set = WasiKeyValue.class.getMethod("set", String.class, byte[].class);
      assertEquals(void.class, set.getReturnType());

      // set with TTL
      Method setWithTtl =
          WasiKeyValue.class.getMethod("set", String.class, byte[].class, Duration.class);
      assertEquals(void.class, setWithTtl.getReturnType());

      // delete
      Method delete = WasiKeyValue.class.getMethod("delete", String.class);
      assertEquals(boolean.class, delete.getReturnType());

      // exists
      Method exists = WasiKeyValue.class.getMethod("exists", String.class);
      assertEquals(boolean.class, exists.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have keys methods")
    void shouldHaveKeysMethods() throws NoSuchMethodException {
      Method keysWithPattern = WasiKeyValue.class.getMethod("keys", String.class);
      assertEquals(Set.class, keysWithPattern.getReturnType());

      Method keys = WasiKeyValue.class.getMethod("keys");
      assertEquals(Set.class, keys.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have atomic operation methods")
    void shouldHaveAtomicOperationMethods() throws NoSuchMethodException {
      // setIfAbsent
      Method setIfAbsent = WasiKeyValue.class.getMethod("setIfAbsent", String.class, byte[].class);
      assertEquals(boolean.class, setIfAbsent.getReturnType());

      // setIfPresent
      Method setIfPresent =
          WasiKeyValue.class.getMethod("setIfPresent", String.class, byte[].class);
      assertEquals(boolean.class, setIfPresent.getReturnType());

      // compareAndSwap
      Method cas =
          WasiKeyValue.class.getMethod("compareAndSwap", String.class, byte[].class, byte[].class);
      assertEquals(boolean.class, cas.getReturnType());

      // compareVersionAndSwap
      Method cvs =
          WasiKeyValue.class.getMethod(
              "compareVersionAndSwap", String.class, long.class, byte[].class);
      assertEquals(boolean.class, cvs.getReturnType());

      // increment
      Method increment = WasiKeyValue.class.getMethod("increment", String.class, long.class);
      assertEquals(long.class, increment.getReturnType());

      // getAndDelete
      Method getAndDelete = WasiKeyValue.class.getMethod("getAndDelete", String.class);
      assertEquals(Optional.class, getAndDelete.getReturnType());

      // getAndSet
      Method getAndSet = WasiKeyValue.class.getMethod("getAndSet", String.class, byte[].class);
      assertEquals(Optional.class, getAndSet.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have batch operation methods")
    void shouldHaveBatchOperationMethods() throws NoSuchMethodException {
      // getMultiple
      Method getMultiple = WasiKeyValue.class.getMethod("getMultiple", Set.class);
      assertEquals(Map.class, getMultiple.getReturnType());

      // setMultiple
      Method setMultiple = WasiKeyValue.class.getMethod("setMultiple", Map.class);
      assertEquals(void.class, setMultiple.getReturnType());

      // deleteMultiple
      Method deleteMultiple = WasiKeyValue.class.getMethod("deleteMultiple", Set.class);
      assertEquals(Set.class, deleteMultiple.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have list operation methods")
    void shouldHaveListOperationMethods() throws NoSuchMethodException {
      // listAppend
      Method listAppend = WasiKeyValue.class.getMethod("listAppend", String.class, List.class);
      assertEquals(long.class, listAppend.getReturnType());

      // listPrepend
      Method listPrepend = WasiKeyValue.class.getMethod("listPrepend", String.class, List.class);
      assertEquals(long.class, listPrepend.getReturnType());

      // listRange
      Method listRange =
          WasiKeyValue.class.getMethod("listRange", String.class, long.class, long.class);
      assertEquals(List.class, listRange.getReturnType());

      // listLength
      Method listLength = WasiKeyValue.class.getMethod("listLength", String.class);
      assertEquals(long.class, listLength.getReturnType());

      // listPop
      Method listPop = WasiKeyValue.class.getMethod("listPop", String.class);
      assertEquals(Optional.class, listPop.getReturnType());

      // listShift
      Method listShift = WasiKeyValue.class.getMethod("listShift", String.class);
      assertEquals(Optional.class, listShift.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have set operation methods")
    void shouldHaveSetOperationMethods() throws NoSuchMethodException {
      // setAdd
      Method setAdd = WasiKeyValue.class.getMethod("setAdd", String.class, Set.class);
      assertEquals(long.class, setAdd.getReturnType());

      // setRemove
      Method setRemove = WasiKeyValue.class.getMethod("setRemove", String.class, Set.class);
      assertEquals(long.class, setRemove.getReturnType());

      // setMembers
      Method setMembers = WasiKeyValue.class.getMethod("setMembers", String.class);
      assertEquals(Set.class, setMembers.getReturnType());

      // setIsMember
      Method setIsMember = WasiKeyValue.class.getMethod("setIsMember", String.class, byte[].class);
      assertEquals(boolean.class, setIsMember.getReturnType());

      // setSize
      Method setSize = WasiKeyValue.class.getMethod("setSize", String.class);
      assertEquals(long.class, setSize.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have hash operation methods")
    void shouldHaveHashOperationMethods() throws NoSuchMethodException {
      // hashSet
      Method hashSet =
          WasiKeyValue.class.getMethod("hashSet", String.class, String.class, byte[].class);
      assertEquals(void.class, hashSet.getReturnType());

      // hashGet
      Method hashGet = WasiKeyValue.class.getMethod("hashGet", String.class, String.class);
      assertEquals(Optional.class, hashGet.getReturnType());

      // hashDelete
      Method hashDelete = WasiKeyValue.class.getMethod("hashDelete", String.class, String.class);
      assertEquals(boolean.class, hashDelete.getReturnType());

      // hashGetAll
      Method hashGetAll = WasiKeyValue.class.getMethod("hashGetAll", String.class);
      assertEquals(Map.class, hashGetAll.getReturnType());

      // hashKeys
      Method hashKeys = WasiKeyValue.class.getMethod("hashKeys", String.class);
      assertEquals(Set.class, hashKeys.getReturnType());

      // hashExists
      Method hashExists = WasiKeyValue.class.getMethod("hashExists", String.class, String.class);
      assertEquals(boolean.class, hashExists.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have transaction methods")
    void shouldHaveTransactionMethods() throws NoSuchMethodException {
      // beginTransaction
      Method beginTransaction = WasiKeyValue.class.getMethod("beginTransaction");
      assertEquals(KeyValueTransaction.class, beginTransaction.getReturnType());

      // beginTransaction with isolation level
      Method beginTransactionWithLevel =
          WasiKeyValue.class.getMethod("beginTransaction", IsolationLevel.class);
      assertEquals(KeyValueTransaction.class, beginTransactionWithLevel.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have configuration methods")
    void shouldHaveConfigurationMethods() throws NoSuchMethodException {
      // getConsistencyModel
      Method getConsistencyModel = WasiKeyValue.class.getMethod("getConsistencyModel");
      assertEquals(ConsistencyModel.class, getConsistencyModel.getReturnType());

      // setConsistencyModel
      Method setConsistencyModel =
          WasiKeyValue.class.getMethod("setConsistencyModel", ConsistencyModel.class);
      assertEquals(void.class, setConsistencyModel.getReturnType());

      // getEvictionPolicy
      Method getEvictionPolicy = WasiKeyValue.class.getMethod("getEvictionPolicy");
      assertEquals(EvictionPolicy.class, getEvictionPolicy.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have TTL management methods")
    void shouldHaveTtlManagementMethods() throws NoSuchMethodException {
      // getTtl
      Method getTtl = WasiKeyValue.class.getMethod("getTtl", String.class);
      assertEquals(Optional.class, getTtl.getReturnType());

      // setTtl
      Method setTtl = WasiKeyValue.class.getMethod("setTtl", String.class, Duration.class);
      assertEquals(boolean.class, setTtl.getReturnType());

      // persist
      Method persist = WasiKeyValue.class.getMethod("persist", String.class);
      assertEquals(boolean.class, persist.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have store information methods")
    void shouldHaveStoreInformationMethods() throws NoSuchMethodException {
      // size
      Method size = WasiKeyValue.class.getMethod("size");
      assertEquals(long.class, size.getReturnType());

      // isEmpty
      Method isEmpty = WasiKeyValue.class.getMethod("isEmpty");
      assertEquals(boolean.class, isEmpty.getReturnType());

      // clear
      Method clear = WasiKeyValue.class.getMethod("clear");
      assertEquals(void.class, clear.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method close = WasiKeyValue.class.getMethod("close");
      assertEquals(void.class, close.getReturnType());
    }

    @Test
    @DisplayName("WasiKeyValue methods should declare KeyValueException")
    void methodsShouldDeclareKeyValueException() throws NoSuchMethodException {
      Method[] methods = {
        WasiKeyValue.class.getMethod("get", String.class),
        WasiKeyValue.class.getMethod("set", String.class, byte[].class),
        WasiKeyValue.class.getMethod("delete", String.class),
        WasiKeyValue.class.getMethod("exists", String.class)
      };

      for (Method method : methods) {
        Class<?>[] exceptionTypes = method.getExceptionTypes();
        boolean declaresKeyValueException = false;
        for (Class<?> exType : exceptionTypes) {
          if (KeyValueException.class.isAssignableFrom(exType)) {
            declaresKeyValueException = true;
            break;
          }
        }
        assertTrue(
            declaresKeyValueException, method.getName() + " should declare KeyValueException");
      }
    }
  }

  // ==================== KeyValueTransaction Interface Tests ====================

  @Nested
  @DisplayName("KeyValueTransaction Interface Tests")
  class KeyValueTransactionInterfaceTests {

    @Test
    @DisplayName("KeyValueTransaction should be an interface")
    void shouldBeInterface() {
      assertTrue(
          KeyValueTransaction.class.isInterface(), "KeyValueTransaction should be an interface");
    }

    @Test
    @DisplayName("KeyValueTransaction should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(KeyValueTransaction.class),
          "KeyValueTransaction should extend AutoCloseable");
    }

    @Test
    @DisplayName("KeyValueTransaction should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method getId = KeyValueTransaction.class.getMethod("getId");
      assertEquals(UUID.class, getId.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have getIsolationLevel method")
    void shouldHaveGetIsolationLevelMethod() throws NoSuchMethodException {
      Method getIsolationLevel = KeyValueTransaction.class.getMethod("getIsolationLevel");
      assertEquals(IsolationLevel.class, getIsolationLevel.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have CRUD methods")
    void shouldHaveCrudMethods() throws NoSuchMethodException {
      Method get = KeyValueTransaction.class.getMethod("get", String.class);
      assertEquals(Optional.class, get.getReturnType());

      Method set = KeyValueTransaction.class.getMethod("set", String.class, byte[].class);
      assertEquals(void.class, set.getReturnType());

      Method delete = KeyValueTransaction.class.getMethod("delete", String.class);
      assertEquals(boolean.class, delete.getReturnType());

      Method exists = KeyValueTransaction.class.getMethod("exists", String.class);
      assertEquals(boolean.class, exists.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have commit and abort methods")
    void shouldHaveCommitAndAbortMethods() throws NoSuchMethodException {
      Method commit = KeyValueTransaction.class.getMethod("commit");
      assertEquals(void.class, commit.getReturnType());

      Method abort = KeyValueTransaction.class.getMethod("abort");
      assertEquals(void.class, abort.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have isActive method")
    void shouldHaveIsActiveMethod() throws NoSuchMethodException {
      Method isActive = KeyValueTransaction.class.getMethod("isActive");
      assertEquals(boolean.class, isActive.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have getElapsedTime method")
    void shouldHaveGetElapsedTimeMethod() throws NoSuchMethodException {
      Method getElapsedTime = KeyValueTransaction.class.getMethod("getElapsedTime");
      assertEquals(Duration.class, getElapsedTime.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have savepoint methods")
    void shouldHaveSavepointMethods() throws NoSuchMethodException {
      Method savepoint = KeyValueTransaction.class.getMethod("savepoint", String.class);
      assertEquals(void.class, savepoint.getReturnType());

      Method rollbackToSavepoint =
          KeyValueTransaction.class.getMethod("rollbackToSavepoint", String.class);
      assertEquals(void.class, rollbackToSavepoint.getReturnType());

      Method releaseSavepoint =
          KeyValueTransaction.class.getMethod("releaseSavepoint", String.class);
      assertEquals(void.class, releaseSavepoint.getReturnType());
    }

    @Test
    @DisplayName("KeyValueTransaction should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method close = KeyValueTransaction.class.getMethod("close");
      assertEquals(void.class, close.getReturnType());
    }
  }

  // ==================== Package Consistency Tests ====================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("All keyvalue classes should be in correct package")
    void allClassesShouldBeInCorrectPackage() {
      String expectedPackage = "ai.tegmentum.wasmtime4j.wasi.keyvalue";
      Class<?>[] classes = {
        WasiKeyValue.class,
        KeyValueTransaction.class,
        KeyValueEntry.class,
        KeyValueException.class,
        KeyValueErrorCode.class,
        ConsistencyModel.class,
        IsolationLevel.class,
        EvictionPolicy.class
      };

      for (Class<?> clazz : classes) {
        assertEquals(
            expectedPackage,
            clazz.getPackage().getName(),
            clazz.getSimpleName() + " should be in " + expectedPackage);
      }
    }

    @Test
    @DisplayName("Package should have expected number of public classes")
    void shouldHaveExpectedNumberOfPublicClasses() {
      // 3 enums (ConsistencyModel, IsolationLevel, EvictionPolicy, KeyValueErrorCode = 4)
      // 2 interfaces (WasiKeyValue, KeyValueTransaction)
      // 2 classes (KeyValueEntry, KeyValueException)
      // Total: 8 main classes
      int expectedClasses = 8;
      Class<?>[] knownClasses = {
        WasiKeyValue.class,
        KeyValueTransaction.class,
        KeyValueEntry.class,
        KeyValueException.class,
        KeyValueErrorCode.class,
        ConsistencyModel.class,
        IsolationLevel.class,
        EvictionPolicy.class
      };
      assertEquals(
          expectedClasses,
          knownClasses.length,
          "Package should have expected number of main classes");
    }

    @Test
    @DisplayName("Exception class should have proper inheritance")
    void exceptionShouldHaveProperInheritance() {
      assertTrue(
          WasmException.class.isAssignableFrom(KeyValueException.class),
          "KeyValueException should extend WasmException");
      assertTrue(
          Exception.class.isAssignableFrom(KeyValueException.class),
          "KeyValueException should be an Exception");
    }
  }

  // ==================== Exception Throwing Tests ====================

  @Nested
  @DisplayName("Exception Throwing Tests")
  class ExceptionThrowingTests {

    @Test
    @DisplayName("KeyValueException can be thrown and caught")
    void exceptionCanBeThrownAndCaught() {
      KeyValueException exception =
          new KeyValueException("Test exception", KeyValueErrorCode.KEY_NOT_FOUND);

      assertThrows(
          KeyValueException.class,
          () -> {
            throw exception;
          });
    }

    @Test
    @DisplayName("KeyValueException can be caught as WasmException")
    void exceptionCanBeCaughtAsWasmException() {
      KeyValueException exception =
          new KeyValueException("Test exception", KeyValueErrorCode.INTERNAL_ERROR);

      assertThrows(
          WasmException.class,
          () -> {
            throw exception;
          });
    }

    @Test
    @DisplayName("KeyValueException error codes should be distinguishable")
    void errorCodesShouldBeDistinguishable() {
      KeyValueException notFound =
          new KeyValueException("Not found", KeyValueErrorCode.KEY_NOT_FOUND);
      KeyValueException exists = new KeyValueException("Exists", KeyValueErrorCode.KEY_EXISTS);
      KeyValueException conflict =
          new KeyValueException("Conflict", KeyValueErrorCode.TRANSACTION_CONFLICT);

      assertNotEquals(notFound.getErrorCode(), exists.getErrorCode());
      assertNotEquals(exists.getErrorCode(), conflict.getErrorCode());
      assertNotEquals(notFound.getErrorCode(), conflict.getErrorCode());
    }
  }
}
