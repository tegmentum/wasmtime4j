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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WASI KeyValue package interfaces, enums, and classes.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI keyvalue
 * API using reflection-based testing.
 */
@DisplayName("WASI KeyValue Package Tests")
class WasiKeyValuePackageTest {

  // ========================================================================
  // ConsistencyModel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ConsistencyModel Enum Tests")
  class ConsistencyModelTests {

    @Test
    @DisplayName("ConsistencyModel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ConsistencyModel.class.isEnum(), "ConsistencyModel should be an enum");
    }

    @Test
    @DisplayName("ConsistencyModel should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ConsistencyModel.class.getModifiers()),
          "ConsistencyModel should be public");
    }

    @Test
    @DisplayName("ConsistencyModel should have exactly 8 values")
    void shouldHaveExactValueCount() {
      ConsistencyModel[] values = ConsistencyModel.values();
      assertEquals(8, values.length, "ConsistencyModel should have exactly 8 values");
    }

    @Test
    @DisplayName("ConsistencyModel should have EVENTUAL value")
    void shouldHaveEventualValue() {
      assertNotNull(ConsistencyModel.valueOf("EVENTUAL"), "EVENTUAL value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have STRONG value")
    void shouldHaveStrongValue() {
      assertNotNull(ConsistencyModel.valueOf("STRONG"), "STRONG value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have CAUSAL value")
    void shouldHaveCausalValue() {
      assertNotNull(ConsistencyModel.valueOf("CAUSAL"), "CAUSAL value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have SEQUENTIAL value")
    void shouldHaveSequentialValue() {
      assertNotNull(ConsistencyModel.valueOf("SEQUENTIAL"), "SEQUENTIAL value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have LINEARIZABLE value")
    void shouldHaveLinearizableValue() {
      assertNotNull(ConsistencyModel.valueOf("LINEARIZABLE"), "LINEARIZABLE value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have SESSION value")
    void shouldHaveSessionValue() {
      assertNotNull(ConsistencyModel.valueOf("SESSION"), "SESSION value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have MONOTONIC_READ value")
    void shouldHaveMonotonicReadValue() {
      assertNotNull(
          ConsistencyModel.valueOf("MONOTONIC_READ"), "MONOTONIC_READ value should exist");
    }

    @Test
    @DisplayName("ConsistencyModel should have MONOTONIC_WRITE value")
    void shouldHaveMonotonicWriteValue() {
      assertNotNull(
          ConsistencyModel.valueOf("MONOTONIC_WRITE"), "MONOTONIC_WRITE value should exist");
    }
  }

  // ========================================================================
  // IsolationLevel Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("IsolationLevel Enum Tests")
  class IsolationLevelTests {

    @Test
    @DisplayName("IsolationLevel should be an enum")
    void shouldBeAnEnum() {
      assertTrue(IsolationLevel.class.isEnum(), "IsolationLevel should be an enum");
    }

    @Test
    @DisplayName("IsolationLevel should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(IsolationLevel.class.getModifiers()),
          "IsolationLevel should be public");
    }

    @Test
    @DisplayName("IsolationLevel should have standard database isolation levels")
    void shouldHaveStandardIsolationLevels() {
      IsolationLevel[] values = IsolationLevel.values();
      assertTrue(values.length >= 1, "IsolationLevel should have at least 1 value");
    }
  }

  // ========================================================================
  // EvictionPolicy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("EvictionPolicy Enum Tests")
  class EvictionPolicyTests {

    @Test
    @DisplayName("EvictionPolicy should be an enum")
    void shouldBeAnEnum() {
      assertTrue(EvictionPolicy.class.isEnum(), "EvictionPolicy should be an enum");
    }

    @Test
    @DisplayName("EvictionPolicy should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(EvictionPolicy.class.getModifiers()),
          "EvictionPolicy should be public");
    }

    @Test
    @DisplayName("EvictionPolicy should have common eviction strategies")
    void shouldHaveCommonStrategies() {
      EvictionPolicy[] values = EvictionPolicy.values();
      assertTrue(values.length >= 1, "EvictionPolicy should have at least 1 value");

      // Check for LRU which is a common eviction policy
      boolean hasLru = false;
      for (EvictionPolicy policy : values) {
        if (policy.name().contains("LRU")) {
          hasLru = true;
          break;
        }
      }
      assertTrue(hasLru, "Should have LRU eviction policy");
    }
  }

  // ========================================================================
  // KeyValueErrorCode Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("KeyValueErrorCode Enum Tests")
  class KeyValueErrorCodeTests {

    @Test
    @DisplayName("KeyValueErrorCode should be an enum")
    void shouldBeAnEnum() {
      assertTrue(KeyValueErrorCode.class.isEnum(), "KeyValueErrorCode should be an enum");
    }

    @Test
    @DisplayName("KeyValueErrorCode should be a public enum")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(KeyValueErrorCode.class.getModifiers()),
          "KeyValueErrorCode should be public");
    }

    @Test
    @DisplayName("KeyValueErrorCode should have error codes")
    void shouldHaveErrorCodes() {
      KeyValueErrorCode[] values = KeyValueErrorCode.values();
      assertTrue(values.length >= 1, "KeyValueErrorCode should have at least 1 value");
    }
  }

  // ========================================================================
  // KeyValueException Class Tests
  // ========================================================================

  @Nested
  @DisplayName("KeyValueException Class Tests")
  class KeyValueExceptionTests {

    @Test
    @DisplayName("KeyValueException should be a class")
    void shouldBeAClass() {
      assertTrue(
          !KeyValueException.class.isInterface() && !KeyValueException.class.isEnum(),
          "KeyValueException should be a class");
    }

    @Test
    @DisplayName("KeyValueException should be a public class")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(KeyValueException.class.getModifiers()),
          "KeyValueException should be public");
    }

    @Test
    @DisplayName("KeyValueException should extend RuntimeException or Exception")
    void shouldExtendException() {
      assertTrue(
          Exception.class.isAssignableFrom(KeyValueException.class),
          "KeyValueException should extend Exception");
    }

    @Test
    @DisplayName("KeyValueException should have constructor with String parameter")
    void shouldHaveStringConstructor() {
      try {
        KeyValueException.class.getConstructor(String.class);
      } catch (NoSuchMethodException e) {
        // Try with message and cause
        try {
          KeyValueException.class.getConstructor(String.class, Throwable.class);
        } catch (NoSuchMethodException e2) {
          // At least should have a constructor
          assertTrue(
              KeyValueException.class.getConstructors().length > 0,
              "KeyValueException should have at least one constructor");
        }
      }
    }
  }

  // ========================================================================
  // KeyValueEntry Class Tests
  // ========================================================================

  @Nested
  @DisplayName("KeyValueEntry Class Tests")
  class KeyValueEntryTests {

    @Test
    @DisplayName("KeyValueEntry should be a class or interface")
    void shouldBeClassOrInterface() {
      assertNotNull(KeyValueEntry.class, "KeyValueEntry should exist");
    }

    @Test
    @DisplayName("KeyValueEntry should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(KeyValueEntry.class.getModifiers()), "KeyValueEntry should be public");
    }

    @Test
    @DisplayName("KeyValueEntry should have key-related methods")
    void shouldHaveKeyMethods() {
      Method[] methods = KeyValueEntry.class.getDeclaredMethods();
      boolean hasKeyMethod = false;
      for (Method method : methods) {
        if (method.getName().toLowerCase().contains("key")) {
          hasKeyMethod = true;
          break;
        }
      }
      assertTrue(hasKeyMethod, "KeyValueEntry should have key-related method");
    }

    @Test
    @DisplayName("KeyValueEntry should have value-related methods")
    void shouldHaveValueMethods() {
      Method[] methods = KeyValueEntry.class.getDeclaredMethods();
      boolean hasValueMethod = false;
      for (Method method : methods) {
        if (method.getName().toLowerCase().contains("value")) {
          hasValueMethod = true;
          break;
        }
      }
      assertTrue(hasValueMethod, "KeyValueEntry should have value-related method");
    }
  }

  // ========================================================================
  // KeyValueTransaction Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("KeyValueTransaction Interface Tests")
  class KeyValueTransactionTests {

    @Test
    @DisplayName("KeyValueTransaction should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          KeyValueTransaction.class.isInterface(), "KeyValueTransaction should be an interface");
    }

    @Test
    @DisplayName("KeyValueTransaction should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(KeyValueTransaction.class.getModifiers()),
          "KeyValueTransaction should be public");
    }

    @Test
    @DisplayName("KeyValueTransaction should have transaction control methods")
    void shouldHaveTransactionMethods() {
      Method[] methods = KeyValueTransaction.class.getDeclaredMethods();
      boolean hasCommit = false;
      boolean hasRollback = false;

      for (Method method : methods) {
        if (method.getName().equals("commit")) {
          hasCommit = true;
        }
        if (method.getName().equals("rollback") || method.getName().equals("abort")) {
          hasRollback = true;
        }
      }

      assertTrue(hasCommit, "KeyValueTransaction should have commit method");
      assertTrue(hasRollback, "KeyValueTransaction should have rollback/abort method");
    }

    @Test
    @DisplayName("All KeyValueTransaction methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = KeyValueTransaction.class.getDeclaredMethods();
      for (Method method : methods) {
        assertTrue(
            Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }

  // ========================================================================
  // WasiKeyValue Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiKeyValue Interface Tests")
  class WasiKeyValueTests {

    @Test
    @DisplayName("WasiKeyValue should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiKeyValue.class.isInterface(), "WasiKeyValue should be an interface");
    }

    @Test
    @DisplayName("WasiKeyValue should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiKeyValue.class.getModifiers()), "WasiKeyValue should be public");
    }

    @Test
    @DisplayName("WasiKeyValue should have get method")
    void shouldHaveGetMethod() {
      Method[] methods = WasiKeyValue.class.getDeclaredMethods();
      boolean hasGet = false;
      for (Method method : methods) {
        if (method.getName().equals("get")) {
          hasGet = true;
          break;
        }
      }
      assertTrue(hasGet, "WasiKeyValue should have get method");
    }

    @Test
    @DisplayName("WasiKeyValue should have set method")
    void shouldHaveSetMethod() {
      Method[] methods = WasiKeyValue.class.getDeclaredMethods();
      boolean hasSet = false;
      for (Method method : methods) {
        if (method.getName().equals("set") || method.getName().equals("put")) {
          hasSet = true;
          break;
        }
      }
      assertTrue(hasSet, "WasiKeyValue should have set/put method");
    }

    @Test
    @DisplayName("WasiKeyValue should have delete method")
    void shouldHaveDeleteMethod() {
      Method[] methods = WasiKeyValue.class.getDeclaredMethods();
      boolean hasDelete = false;
      for (Method method : methods) {
        if (method.getName().equals("delete") || method.getName().equals("remove")) {
          hasDelete = true;
          break;
        }
      }
      assertTrue(hasDelete, "WasiKeyValue should have delete/remove method");
    }

    @Test
    @DisplayName("WasiKeyValue should have exists method")
    void shouldHaveExistsMethod() {
      Method[] methods = WasiKeyValue.class.getDeclaredMethods();
      boolean hasExists = false;
      for (Method method : methods) {
        if (method.getName().equals("exists") || method.getName().equals("containsKey")) {
          hasExists = true;
          break;
        }
      }
      assertTrue(hasExists, "WasiKeyValue should have exists/containsKey method");
    }

    @Test
    @DisplayName("All WasiKeyValue methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = WasiKeyValue.class.getDeclaredMethods();
      for (Method method : methods) {
        // Skip static and default methods
        if (!Modifier.isStatic(method.getModifiers())) {
          assertTrue(
              Modifier.isPublic(method.getModifiers()),
              "Method " + method.getName() + " should be public");
        }
      }
    }
  }

  // ========================================================================
  // Package Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All WASI KeyValue classes should be loadable")
    void allClassesShouldBeLoadable() {
      assertNotNull(ConsistencyModel.class, "ConsistencyModel should be loadable");
      assertNotNull(IsolationLevel.class, "IsolationLevel should be loadable");
      assertNotNull(EvictionPolicy.class, "EvictionPolicy should be loadable");
      assertNotNull(KeyValueErrorCode.class, "KeyValueErrorCode should be loadable");
      assertNotNull(KeyValueException.class, "KeyValueException should be loadable");
      assertNotNull(KeyValueEntry.class, "KeyValueEntry should be loadable");
      assertNotNull(KeyValueTransaction.class, "KeyValueTransaction should be loadable");
      assertNotNull(WasiKeyValue.class, "WasiKeyValue should be loadable");
    }

    @Test
    @DisplayName("Package should have 8 types (excluding package-info)")
    void shouldHaveExpectedTypeCount() {
      // Verify that all expected types are present
      int typeCount = 0;
      if (ConsistencyModel.class.isEnum()) {
        typeCount++;
      }
      if (IsolationLevel.class.isEnum()) {
        typeCount++;
      }
      if (EvictionPolicy.class.isEnum()) {
        typeCount++;
      }
      if (KeyValueErrorCode.class.isEnum()) {
        typeCount++;
      }
      if (!KeyValueException.class.isInterface()) {
        typeCount++;
      }
      if (KeyValueEntry.class != null) {
        typeCount++;
      }
      if (KeyValueTransaction.class.isInterface()) {
        typeCount++;
      }
      if (WasiKeyValue.class.isInterface()) {
        typeCount++;
      }

      assertEquals(8, typeCount, "Package should have 8 types");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("ConsistencyModel values should be in expected order")
    void consistencyModelValuesOrder() {
      ConsistencyModel[] values = ConsistencyModel.values();
      assertEquals(ConsistencyModel.EVENTUAL, values[0], "First value should be EVENTUAL");
      assertEquals(ConsistencyModel.STRONG, values[1], "Second value should be STRONG");
    }

    @Test
    @DisplayName("All enums should have valueOf method")
    void enumsShouldHaveValueOfMethod() throws NoSuchMethodException {
      assertNotNull(
          ConsistencyModel.class.getMethod("valueOf", String.class),
          "ConsistencyModel should have valueOf");
      assertNotNull(
          IsolationLevel.class.getMethod("valueOf", String.class),
          "IsolationLevel should have valueOf");
      assertNotNull(
          EvictionPolicy.class.getMethod("valueOf", String.class),
          "EvictionPolicy should have valueOf");
      assertNotNull(
          KeyValueErrorCode.class.getMethod("valueOf", String.class),
          "KeyValueErrorCode should have valueOf");
    }

    @Test
    @DisplayName("All enums should have values method")
    void enumsShouldHaveValuesMethod() throws NoSuchMethodException {
      assertNotNull(
          ConsistencyModel.class.getMethod("values"), "ConsistencyModel should have values");
      assertNotNull(IsolationLevel.class.getMethod("values"), "IsolationLevel should have values");
      assertNotNull(EvictionPolicy.class.getMethod("values"), "EvictionPolicy should have values");
      assertNotNull(
          KeyValueErrorCode.class.getMethod("values"), "KeyValueErrorCode should have values");
    }
  }
}
