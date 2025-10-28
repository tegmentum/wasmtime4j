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

package ai.tegmentum.wasmtime4j.comparison.framework;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.HostFunction;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Test runner for executing WAST-style tests.
 *
 * <p>This class provides utilities for running WebAssembly test assertions similar to those in WAST
 * (WebAssembly Script) files. It handles module compilation, instantiation, and assertion
 * verification.
 *
 * <p>Supports both simple modules and modules with host function imports via Linker.
 */
public final class WastTestRunner implements AutoCloseable {

  private final Engine engine;
  private final Store store;
  private final Linker linker;
  private final Map<String, Instance> namedInstances;
  private Instance currentInstance;
  private boolean hasHostFunctions;

  /**
   * Creates a new WAST test runner with a default engine and store.
   *
   * <p>Uses the currently configured runtime (JNI or Panama) based on system properties.
   */
  public WastTestRunner() throws Exception {
    this.engine = Engine.create();
    this.store = engine.createStore();
    this.linker = Linker.create(engine);
    this.namedInstances = new HashMap<>();
    this.currentInstance = null;
    this.hasHostFunctions = false;
  }

  /**
   * Creates a new WAST test runner with a specific runtime type.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the runner cannot be created
   */
  public WastTestRunner(final ai.tegmentum.wasmtime4j.RuntimeType runtime) throws Exception {
    // Set the runtime before creating the engine
    DualRuntimeTest.setRuntime(runtime);
    this.engine = Engine.create();
    this.store = engine.createStore();
    this.linker = Linker.create(engine);
    this.namedInstances = new HashMap<>();
    this.currentInstance = null;
    this.hasHostFunctions = false;
  }

  /**
   * Defines a host function that can be imported by WASM modules.
   *
   * @param moduleName the module name for the import (e.g., "env" or "")
   * @param functionName the function name for the import (e.g., "add" or "")
   * @param functionType the function type signature
   * @param hostFunction the host function implementation
   * @throws Exception if the host function cannot be defined
   */
  public void defineHostFunction(
      final String moduleName,
      final String functionName,
      final FunctionType functionType,
      final HostFunction hostFunction)
      throws Exception {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(functionName, "Function name cannot be null");
    Objects.requireNonNull(functionType, "Function type cannot be null");
    Objects.requireNonNull(hostFunction, "Host function cannot be null");

    linker.defineHostFunction(moduleName, functionName, functionType, hostFunction);
    hasHostFunctions = true;
  }

  /**
   * Defines a global that can be imported by WASM modules.
   *
   * @param moduleName the module name for the import (e.g., "spectest")
   * @param globalName the global name for the import (e.g., "global_i32")
   * @param global the global to define
   * @throws Exception if the global cannot be defined
   */
  public void defineGlobal(
      final String moduleName,
      final String globalName,
      final ai.tegmentum.wasmtime4j.WasmGlobal global)
      throws Exception {
    Objects.requireNonNull(moduleName, "Module name cannot be null");
    Objects.requireNonNull(globalName, "Global name cannot be null");
    Objects.requireNonNull(global, "Global cannot be null");

    linker.defineGlobal(store, moduleName, globalName, global);
    hasHostFunctions = true; // Mark as using linker
  }

  /**
   * Compiles and instantiates a WAT module.
   *
   * <p>Uses Linker if host functions have been defined, otherwise uses direct instantiation.
   *
   * @param wat the WebAssembly Text format module
   * @return the instantiated module instance
   * @throws Exception if compilation or instantiation fails
   */
  public Instance compileAndInstantiate(final String wat) throws Exception {
    Objects.requireNonNull(wat, "WAT cannot be null");

    final Module module = engine.compileWat(wat);
    final Instance instance;

    if (hasHostFunctions) {
      // Use linker when host functions are defined
      instance = linker.instantiate(store, module);
    } else {
      // Direct instantiation for simple modules
      instance = module.instantiate(store);
    }

    // Set as current instance
    this.currentInstance = instance;

    return instance;
  }

  /**
   * Compiles and instantiates a WAT module with a specific name.
   *
   * @param name the name to associate with this instance
   * @param wat the WebAssembly Text format module
   * @return the instantiated module instance
   * @throws Exception if compilation or instantiation fails
   */
  public Instance compileAndInstantiate(final String name, final String wat) throws Exception {
    Objects.requireNonNull(name, "Name cannot be null");
    Objects.requireNonNull(wat, "WAT cannot be null");

    final Instance instance = compileAndInstantiate(wat);
    namedInstances.put(name, instance);

    return instance;
  }

  /**
   * Registers the current module's exports under a specified name in the linker.
   *
   * <p>This allows subsequent modules to import items from this module by using the registered name
   * as the import module name. This is equivalent to the WAST (register "name") directive.
   *
   * <p>All exports from the current instance (tables, memories, globals, functions) will be made
   * available to future module instantiations under the specified module name.
   *
   * @param moduleName the name to register this module under (e.g., "test")
   * @throws Exception if registration fails
   * @throws IllegalStateException if no current instance is available
   */
  public void registerModule(final String moduleName) throws Exception {
    Objects.requireNonNull(moduleName, "Module name cannot be null");

    if (currentInstance == null) {
      throw new IllegalStateException("No current module instance to register");
    }

    // Use Linker.defineInstance() to register all exports at once
    linker.defineInstance(moduleName, currentInstance);
    hasHostFunctions = true; // Mark as using linker
  }

  /**
   * Invokes an exported function on the current instance.
   *
   * @param functionName the name of the exported function
   * @param args the arguments to pass to the function
   * @return the result values from the function call
   * @throws Exception if the function call fails
   */
  public WasmValue[] invoke(final String functionName, final WasmValue... args) throws Exception {
    Objects.requireNonNull(functionName, "Function name cannot be null");

    if (currentInstance == null) {
      throw new IllegalStateException("No module instance available");
    }

    return currentInstance.callFunction(functionName, args);
  }

  /**
   * Invokes an exported function on a named instance.
   *
   * @param instanceName the name of the instance
   * @param functionName the name of the exported function
   * @param args the arguments to pass to the function
   * @return the result values from the function call
   * @throws Exception if the function call fails
   */
  public WasmValue[] invoke(
      final String instanceName, final String functionName, final WasmValue... args)
      throws Exception {
    Objects.requireNonNull(instanceName, "Instance name cannot be null");
    Objects.requireNonNull(functionName, "Function name cannot be null");

    final Instance instance = namedInstances.get(instanceName);
    if (instance == null) {
      throw new IllegalArgumentException("No instance found with name: " + instanceName);
    }

    return instance.callFunction(functionName, args);
  }

  /**
   * Asserts that invoking a function returns the expected values.
   *
   * @param functionName the function to invoke
   * @param expectedResults the expected return values
   * @param args the arguments to pass to the function
   * @throws Exception if the assertion fails
   */
  public void assertReturn(
      final String functionName, final WasmValue[] expectedResults, final WasmValue... args)
      throws Exception {
    final WasmValue[] actualResults = invoke(functionName, args);

    if (expectedResults.length != actualResults.length) {
      throw new AssertionError(
          String.format(
              "Expected %d return values but got %d",
              expectedResults.length, actualResults.length));
    }

    for (int i = 0; i < expectedResults.length; i++) {
      final WasmValue expected = expectedResults[i];
      final WasmValue actual = actualResults[i];

      if (!valuesEqual(expected, actual)) {
        throw new AssertionError(
            String.format(
                "Return value mismatch at index %d: expected %s but got %s", i, expected, actual));
      }
    }
  }

  /**
   * Asserts that invoking a function throws a trap with the expected message.
   *
   * @param functionName the function to invoke
   * @param expectedTrapMessage the expected trap message (can be null for any trap)
   * @param args the arguments to pass to the function
   * @throws Exception if the assertion fails
   */
  public void assertTrap(
      final String functionName, final String expectedTrapMessage, final WasmValue... args)
      throws Exception {
    try {
      invoke(functionName, args);
      throw new AssertionError("Expected trap but function call succeeded");
    } catch (final Exception e) {
      // Expected trap occurred
      if (expectedTrapMessage != null) {
        final String normalizedExpected = normalizeTrapMessage(expectedTrapMessage);
        final String normalizedActual = normalizeTrapMessage(e.getMessage());

        if (!normalizedActual.contains(normalizedExpected)) {
          throw new AssertionError(
              String.format(
                  "Expected trap message containing '%s' but got: %s",
                  expectedTrapMessage, e.getMessage()));
        }
      }
    }
  }

  /**
   * Normalizes trap messages to handle differences between JNI and Panama implementations.
   *
   * <p>Different runtimes may produce slightly different error messages for the same trap
   * condition. This method normalizes common variations to canonical forms.
   *
   * @param message the trap message to normalize
   * @return normalized message in lowercase with standardized wording
   */
  private static String normalizeTrapMessage(final String message) {
    if (message == null) {
      return "";
    }

    String normalized = message.toLowerCase();

    // Normalize common trap message variations
    normalized = normalized.replace("out of bounds memory access", "memory out of bounds");
    normalized = normalized.replace("memory access out of bounds", "memory out of bounds");
    normalized = normalized.replace("integer divide by zero", "divide by zero");
    normalized = normalized.replace("division by zero", "divide by zero");
    normalized = normalized.replace("integer overflow", "overflow");
    normalized = normalized.replace("call stack exhausted", "stack overflow");
    normalized = normalized.replace("stack overflow", "stack overflow");

    // Wasmtime doesn't include "unreachable" in the error message for unreachable instructions
    // It just says "error while executing" or similar generic runtime error
    if (normalized.contains("error while executing") || normalized.contains("wasm backtrace")) {
      normalized = normalized + " unreachable";
    }

    return normalized;
  }

  /**
   * Asserts that a module fails to link.
   *
   * @param wat the module that should fail to link
   * @param expectedErrorMessage the expected error message (can be null for any error)
   * @throws Exception if the assertion fails
   */
  public void assertUnlinkable(final String wat, final String expectedErrorMessage)
      throws Exception {
    try {
      compileAndInstantiate(wat);
      throw new AssertionError("Expected module to fail linking but it succeeded");
    } catch (final Exception e) {
      // Expected error occurred
      if (expectedErrorMessage != null && !e.getMessage().contains(expectedErrorMessage)) {
        throw new AssertionError(
            String.format(
                "Expected error message containing '%s' but got: %s",
                expectedErrorMessage, e.getMessage()));
      }
    }
  }

  /**
   * Asserts that a module is invalid (fails to compile/validate).
   *
   * @param wat the module that should be invalid
   * @param expectedErrorMessage the expected error message (can be null for any error)
   * @throws Exception if the assertion fails
   */
  public void assertInvalid(final String wat, final String expectedErrorMessage) throws Exception {
    try {
      engine.compileWat(wat);
      throw new AssertionError("Expected module to be invalid but compilation succeeded");
    } catch (final Exception e) {
      // Expected error occurred
      if (expectedErrorMessage != null && !e.getMessage().contains(expectedErrorMessage)) {
        throw new AssertionError(
            String.format(
                "Expected error message containing '%s' but got: %s",
                expectedErrorMessage, e.getMessage()));
      }
    }
  }

  /**
   * Asserts that a module fails to compile (malformed).
   *
   * @param wat the module that should fail to compile
   * @param expectedErrorMessage the expected error message (can be null for any error)
   * @throws Exception if the assertion fails
   */
  public void assertMalformed(final String wat, final String expectedErrorMessage)
      throws Exception {
    // For WAT, malformed and invalid are typically the same
    assertInvalid(wat, expectedErrorMessage);
  }

  /**
   * Compares two WasmValue instances for equality.
   *
   * @param expected the expected value
   * @param actual the actual value
   * @return true if the values are equal
   */
  private boolean valuesEqual(final WasmValue expected, final WasmValue actual) {
    if (expected.getType() != actual.getType()) {
      return false;
    }

    switch (expected.getType()) {
      case I32:
        return expected.asInt() == actual.asInt();
      case I64:
        return expected.asLong() == actual.asLong();
      case F32:
        return Float.compare(expected.asFloat(), actual.asFloat()) == 0;
      case F64:
        return Double.compare(expected.asDouble(), actual.asDouble()) == 0;
      case EXTERNREF:
      case FUNCREF:
      case V128:
        // Use WasmValue.equals() for reference types and vectors
        return expected.equals(actual);
      default:
        return false;
    }
  }

  /**
   * Gets the current instance.
   *
   * @return the current instance, or null if none
   */
  public Instance getCurrentInstance() {
    return currentInstance;
  }

  /**
   * Gets a named instance.
   *
   * @param name the instance name
   * @return the instance, or null if not found
   */
  public Instance getInstance(final String name) {
    return namedInstances.get(name);
  }

  /**
   * Gets the store used by this test runner.
   *
   * @return the store instance
   */
  public Store getStore() {
    return store;
  }

  /**
   * Gets the engine used by this test runner.
   *
   * @return the engine instance
   */
  public Engine getEngine() {
    return engine;
  }

  @Override
  public void close() {
    // Close all named instances
    for (final Instance instance : namedInstances.values()) {
      if (instance != null) {
        instance.close();
      }
    }

    // Close current instance if it's not in the named instances
    if (currentInstance != null && !namedInstances.containsValue(currentInstance)) {
      currentInstance.close();
    }

    // Close linker, store, and engine
    if (linker != null) {
      linker.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }
}
