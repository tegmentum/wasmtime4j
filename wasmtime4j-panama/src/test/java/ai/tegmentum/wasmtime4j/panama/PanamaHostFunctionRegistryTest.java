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

package ai.tegmentum.wasmtime4j.panama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PanamaHostFunctionRegistry}.
 *
 * <p>These tests verify the registry's ability to safely store and retrieve host function
 * implementations in a thread-safe manner, manage unique IDs, and handle cleanup operations.
 */
class PanamaHostFunctionRegistryTest {

  private HostFunction testFunction1;
  private HostFunction testFunction2;
  private FunctionType functionType;

  @BeforeEach
  void setUp() {
    // Clear registry before each test
    PanamaHostFunctionRegistry.clear();

    testFunction1 = (params) -> new Object[]{42};
    testFunction2 = (params) -> new Object[]{"hello"};

    functionType = mock(FunctionType.class);
  }

  @Test
  void testRegisterNullFunction() {
    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        PanamaHostFunctionRegistry.register(null, functionType));
  }

  @Test
  void testRegisterFunctionWithType() {
    // When
    final long id = PanamaHostFunctionRegistry.register(testFunction1, functionType);

    // Then
    assertThat(id).isGreaterThan(0);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(1);

    final PanamaHostFunctionRegistry.HostFunctionEntry entry =
        PanamaHostFunctionRegistry.get(id);
    assertThat(entry).isNotNull();
    assertThat(entry.getImplementation()).isEqualTo(testFunction1);
    assertThat(entry.getFunctionType()).isEqualTo(functionType);
    assertThat(entry.hasFunctionType()).isTrue();
    assertThat(entry.getRegistrationTime()).isGreaterThan(0);
  }

  @Test
  void testRegisterFunctionWithoutType() {
    // When
    final long id = PanamaHostFunctionRegistry.register(testFunction1, null);

    // Then
    assertThat(id).isGreaterThan(0);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(1);

    final PanamaHostFunctionRegistry.HostFunctionEntry entry =
        PanamaHostFunctionRegistry.get(id);
    assertThat(entry).isNotNull();
    assertThat(entry.getImplementation()).isEqualTo(testFunction1);
    assertThat(entry.getFunctionType()).isNull();
    assertThat(entry.hasFunctionType()).isFalse();
  }

  @Test
  void testRegisterMultipleFunctions() {
    // When
    final long id1 = PanamaHostFunctionRegistry.register(testFunction1, functionType);
    final long id2 = PanamaHostFunctionRegistry.register(testFunction2, null);

    // Then
    assertThat(id1).isNotEqualTo(id2);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(2);

    final PanamaHostFunctionRegistry.HostFunctionEntry entry1 =
        PanamaHostFunctionRegistry.get(id1);
    final PanamaHostFunctionRegistry.HostFunctionEntry entry2 =
        PanamaHostFunctionRegistry.get(id2);

    assertThat(entry1.getImplementation()).isEqualTo(testFunction1);
    assertThat(entry2.getImplementation()).isEqualTo(testFunction2);
  }

  @Test
  void testGetNonExistentFunction() {
    // When
    final PanamaHostFunctionRegistry.HostFunctionEntry entry =
        PanamaHostFunctionRegistry.get(999999L);

    // Then
    assertThat(entry).isNull();
  }

  @Test
  void testUnregisterFunction() {
    // Given
    final long id = PanamaHostFunctionRegistry.register(testFunction1, functionType);

    // When
    final boolean result = PanamaHostFunctionRegistry.unregister(id);

    // Then
    assertTrue(result);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(0);
    assertThat(PanamaHostFunctionRegistry.get(id)).isNull();
  }

  @Test
  void testUnregisterNonExistentFunction() {
    // When
    final boolean result = PanamaHostFunctionRegistry.unregister(999999L);

    // Then
    assertFalse(result);
  }

  @Test
  void testClearRegistry() {
    // Given
    PanamaHostFunctionRegistry.register(testFunction1, functionType);
    PanamaHostFunctionRegistry.register(testFunction2, null);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(2);

    // When
    PanamaHostFunctionRegistry.clear();

    // Then
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(0);
  }

  @Test
  void testUniqueIds() {
    // When
    final long id1 = PanamaHostFunctionRegistry.register(testFunction1, null);
    final long id2 = PanamaHostFunctionRegistry.register(testFunction1, null); // Same function
    final long id3 = PanamaHostFunctionRegistry.register(testFunction2, null);

    // Then
    assertThat(id1).isNotEqualTo(id2);
    assertThat(id2).isNotEqualTo(id3);
    assertThat(id1).isNotEqualTo(id3);

    // All should be sequential (ID counter increments)
    assertThat(id2).isGreaterThan(id1);
    assertThat(id3).isGreaterThan(id2);
  }

  @Test
  void testCleanupOperation() {
    // Given
    PanamaHostFunctionRegistry.register(testFunction1, functionType);

    // When
    PanamaHostFunctionRegistry.cleanup();

    // Then - Currently cleanup is a no-op, but it should not crash
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(1);
  }

  @Test
  void testHostFunctionEntryToString() {
    // Given
    final long id = PanamaHostFunctionRegistry.register(testFunction1, functionType);
    final PanamaHostFunctionRegistry.HostFunctionEntry entry =
        PanamaHostFunctionRegistry.get(id);

    // When
    final String toString = entry.toString();

    // Then
    assertThat(toString).contains("HostFunctionEntry");
    assertThat(toString).contains("implementation=");
    assertThat(toString).contains("functionType=");
    assertThat(toString).contains("registrationTime=");
  }

  @Test
  void testRegistrationTimeProgression() throws InterruptedException {
    // Given
    final long id1 = PanamaHostFunctionRegistry.register(testFunction1, null);

    Thread.sleep(10); // Small delay to ensure different timestamps

    final long id2 = PanamaHostFunctionRegistry.register(testFunction2, null);

    // When
    final PanamaHostFunctionRegistry.HostFunctionEntry entry1 =
        PanamaHostFunctionRegistry.get(id1);
    final PanamaHostFunctionRegistry.HostFunctionEntry entry2 =
        PanamaHostFunctionRegistry.get(id2);

    // Then
    assertThat(entry2.getRegistrationTime()).isGreaterThanOrEqualTo(entry1.getRegistrationTime());
  }

  @Test
  void testConcurrentAccess() throws InterruptedException {
    // Given
    final int numThreads = 10;
    final int registrationsPerThread = 10;
    final Thread[] threads = new Thread[numThreads];

    // When - Register functions from multiple threads
    for (int i = 0; i < numThreads; i++) {
      final int threadIndex = i;
      threads[i] = new Thread(() -> {
        for (int j = 0; j < registrationsPerThread; j++) {
          final HostFunction func = (params) -> new Object[]{threadIndex * 100 + j};
          PanamaHostFunctionRegistry.register(func, null);
        }
      });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      thread.join();
    }

    // Then
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount())
        .isEqualTo(numThreads * registrationsPerThread);
  }

  @Test
  void testRegistryState() {
    // Initially empty
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(0);

    // Add some functions
    final long id1 = PanamaHostFunctionRegistry.register(testFunction1, null);
    final long id2 = PanamaHostFunctionRegistry.register(testFunction2, functionType);

    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(2);

    // Remove one
    PanamaHostFunctionRegistry.unregister(id1);
    assertThat(PanamaHostFunctionRegistry.getRegisteredCount()).isEqualTo(1);

    // The remaining one should still be accessible
    final PanamaHostFunctionRegistry.HostFunctionEntry remaining =
        PanamaHostFunctionRegistry.get(id2);
    assertThat(remaining).isNotNull();
    assertThat(remaining.getImplementation()).isEqualTo(testFunction2);
    assertThat(remaining.getFunctionType()).isEqualTo(functionType);
  }
}