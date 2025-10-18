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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JniHostFunction defensive programming and validation logic.
 *
 * <p>Note: JniHostFunction constructors call native code immediately, so most tests require native
 * library loading and are in integration tests. These unit tests verify parameter validation and
 * static methods only.
 */
class JniHostFunctionTest {
  private static final long VALID_HANDLE = 1L;
  private JniEngine testEngine;
  private JniStore testStore;
  private FunctionType testFunctionType;
  private HostFunction testImplementation;

  @BeforeEach
  void setUp() {
    testEngine = new JniEngine(VALID_HANDLE);
    testStore = new JniStore(VALID_HANDLE, testEngine);
    testFunctionType = new FunctionType(new WasmValueType[0], new WasmValueType[0]);
    testImplementation = params -> null;
  }

  // Constructor validation tests - all will fail at native level since library not loaded

  @Test
  void testConstructorWithNullFunctionName() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> new JniHostFunction(null, testFunctionType, testImplementation, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native host function");
  }

  @Test
  void testConstructorWithNullFunctionType() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> new JniHostFunction("test", null, testImplementation, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native host function");
  }

  @Test
  void testConstructorWithNullImplementation() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> new JniHostFunction("test", testFunctionType, null, testStore));

    assertThat(exception.getMessage()).contains("Failed to create native host function");
  }

  @Test
  void testConstructorWithNullStore() {
    final WasmException exception =
        assertThrows(
            WasmException.class,
            () -> new JniHostFunction("test", testFunctionType, testImplementation, null));

    assertThat(exception.getMessage()).contains("Failed to create native host function");
  }

  // Registry tests - these are static and don't require instances

  @Test
  void testRegistryStatsNotNull() {
    final long[] stats = JniHostFunction.getRegistryStats();

    assertThat(stats).isNotNull();
    assertThat(stats).hasSize(2);
    assertThat(stats[0]).isGreaterThanOrEqualTo(0); // registry size
    assertThat(stats[1]).isGreaterThan(0); // next ID
  }

  @Test
  void testGetFromRegistryWithInvalidId() {
    final JniHostFunction result = JniHostFunction.getFromRegistry(-1L);

    assertThat(result).isNull();
  }

  @Test
  void testGetFromRegistryWithNonExistentId() {
    final JniHostFunction result = JniHostFunction.getFromRegistry(999999999L);

    assertThat(result).isNull();
  }
}
