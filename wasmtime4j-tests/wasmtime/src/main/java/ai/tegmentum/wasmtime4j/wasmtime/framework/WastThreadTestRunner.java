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
package ai.tegmentum.wasmtime4j.wasmtime.framework;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFeature;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Test runner for executing multi-threaded WAST-style tests.
 *
 * <p>This runner supports the WAST thread directive syntax used in Wasmtime's threading tests. It
 * allows multiple threads to share memory and execute assertions concurrently.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * try (WastThreadTestRunner runner = new WastThreadTestRunner()) {
 *   // Define shared module with shared memory
 *   runner.defineSharedModule("Mem", "(module (memory (export \"shared\") 1 1 shared))");
 *
 *   // Define thread 1
 *   runner.defineThread("T1", thread -> {
 *     thread.importMemory("mem", "shared", "Mem", "shared");
 *     thread.compileAndInstantiate("(module ...)");
 *     thread.assertReturn("run", new WasmValue[]{WasmValue.i32(0)});
 *   });
 *
 *   // Define thread 2
 *   runner.defineThread("T2", thread -> {
 *     thread.importMemory("mem", "shared", "Mem", "shared");
 *     thread.compileAndInstantiate("(module ...)");
 *     thread.assertReturn("notify", new WasmValue[]{WasmValue.i32(1)});
 *   });
 *
 *   // Run all threads and wait for completion
 *   runner.runThreads();
 * }
 * }</pre>
 */
public final class WastThreadTestRunner implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WastThreadTestRunner.class.getName());
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private final Engine engine;
  private final Store mainStore;
  private final Map<String, SharedModuleInfo> sharedModules;
  private final Map<String, ThreadDefinition> threadDefinitions;
  private final List<String> executionOrder;
  private final ConcurrentHashMap<String, ThreadResult> threadResults;
  private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

  /**
   * Creates a new multi-threaded WAST test runner with a default engine.
   *
   * @throws Exception if the runner cannot be created
   */
  public WastThreadTestRunner() throws Exception {
    this.engine = Engine.create(createDefaultConfig());
    this.mainStore = engine.createStore();
    this.sharedModules = new HashMap<>();
    this.threadDefinitions = new HashMap<>();
    this.executionOrder = new ArrayList<>();
    this.threadResults = new ConcurrentHashMap<>();
  }

  /**
   * Creates a new multi-threaded WAST test runner with a specific runtime type.
   *
   * @param runtime the runtime type to use (JNI or Panama)
   * @throws Exception if the runner cannot be created
   */
  public WastThreadTestRunner(final RuntimeType runtime) throws Exception {
    DualRuntimeTest.setRuntime(runtime);
    this.engine = Engine.create(createDefaultConfig());
    this.mainStore = engine.createStore();
    this.sharedModules = new HashMap<>();
    this.threadDefinitions = new HashMap<>();
    this.executionOrder = new ArrayList<>();
    this.threadResults = new ConcurrentHashMap<>();
  }

  /**
   * Creates an EngineConfig with threading support enabled.
   *
   * @return configured EngineConfig
   */
  private static EngineConfig createDefaultConfig() {
    return new EngineConfig()
        .sharedMemory(true)
        .addWasmFeature(WasmFeature.THREADS)
        .addWasmFeature(WasmFeature.BULK_MEMORY)
        .addWasmFeature(WasmFeature.MULTI_VALUE)
        .addWasmFeature(WasmFeature.REFERENCE_TYPES);
  }

  /**
   * Sets the timeout for thread execution in seconds.
   *
   * @param seconds the timeout in seconds
   * @return this runner for method chaining
   */
  public WastThreadTestRunner withTimeout(final int seconds) {
    this.timeoutSeconds = seconds;
    return this;
  }

  /**
   * Defines a shared module that can be accessed by multiple threads.
   *
   * <p>The module is instantiated in the main store and its exports (especially shared memory) can
   * be imported by thread-local instances.
   *
   * @param name the name to identify this shared module
   * @param wat the WebAssembly Text format module
   * @throws Exception if compilation or instantiation fails
   */
  public void defineSharedModule(final String name, final String wat) throws Exception {
    Objects.requireNonNull(name, "Module name cannot be null");
    Objects.requireNonNull(wat, "WAT cannot be null");

    final Module module = engine.compileWat(wat);
    final Instance instance = module.instantiate(mainStore);
    final SharedModuleInfo info = new SharedModuleInfo(module, instance);

    // Pre-extract memory exports while still in the main thread
    // This avoids Panama thread confinement issues when accessing from worker threads
    final String[] exportNames = instance.getExportNames();
    LOGGER.fine(() -> "defineSharedModule: name=" + name);
    LOGGER.fine(
        () -> "Export names: " + (exportNames != null ? Arrays.toString(exportNames) : "null"));
    LOGGER.fine(() -> "Export count: " + (exportNames != null ? exportNames.length : 0));

    if (exportNames != null) {
      for (final String exportName : exportNames) {
        LOGGER.fine(() -> "Checking export: '" + exportName + "'");
        try {
          final java.util.Optional<WasmMemory> memOpt = instance.getMemory(exportName);
          LOGGER.fine(() -> "getMemory('" + exportName + "') present: " + memOpt.isPresent());
          memOpt.ifPresent(
              memory -> {
                info.memories.put(exportName, memory);
                LOGGER.fine(() -> "Stored memory: " + exportName + ", shared=" + memory.isShared());
              });
        } catch (final Exception e) {
          // Not a memory export, skip
          LOGGER.fine(() -> "Export '" + exportName + "' error: " + e.getMessage());
        }
      }
    }
    LOGGER.fine(() -> "Module " + name + " memories: " + info.memories.keySet());

    sharedModules.put(name, info);
    LOGGER.fine(() -> "Defined shared module: " + name);
  }

  /**
   * Gets shared memory from a shared module.
   *
   * <p>This method returns the pre-extracted memory reference to avoid Panama thread confinement
   * issues when called from worker threads.
   *
   * @param moduleName the name of the shared module
   * @param exportName the name of the memory export
   * @return the shared memory
   * @throws IllegalArgumentException if the module or memory is not found
   */
  public WasmMemory getSharedMemory(final String moduleName, final String exportName) {
    final SharedModuleInfo info = sharedModules.get(moduleName);
    if (info == null) {
      throw new IllegalArgumentException("Shared module not found: " + moduleName);
    }
    final WasmMemory memory = info.memories.get(exportName);
    if (memory == null) {
      throw new IllegalArgumentException(
          "Memory export '" + exportName + "' not found in module: " + moduleName);
    }
    return memory;
  }

  /**
   * Defines a thread that will execute as part of this test.
   *
   * @param name the name to identify this thread
   * @param setup the setup function that configures the thread context
   */
  public void defineThread(final String name, final Consumer<ThreadContext> setup) {
    Objects.requireNonNull(name, "Thread name cannot be null");
    Objects.requireNonNull(setup, "Setup function cannot be null");

    threadDefinitions.put(name, new ThreadDefinition(name, setup));
    executionOrder.add(name);
    LOGGER.fine(() -> "Defined thread: " + name);
  }

  /**
   * Compiles and instantiates a module in the main store.
   *
   * <p>This is typically used for check modules that verify results after threads complete.
   *
   * @param name the name to identify this instance
   * @param wat the WebAssembly Text format module
   * @return the instantiated instance
   * @throws Exception if compilation or instantiation fails
   */
  public Instance compileAndInstantiate(final String name, final String wat) throws Exception {
    Objects.requireNonNull(name, "Name cannot be null");
    Objects.requireNonNull(wat, "WAT cannot be null");

    final Module module = engine.compileWat(wat);
    final Linker<?> linker = Linker.create(engine);

    // Register shared modules in the linker for imports
    for (final Map.Entry<String, SharedModuleInfo> entry : sharedModules.entrySet()) {
      final String moduleName = entry.getKey();
      final SharedModuleInfo info = entry.getValue();
      for (final Map.Entry<String, WasmMemory> memEntry : info.memories.entrySet()) {
        linker.defineMemory(mainStore, moduleName, memEntry.getKey(), memEntry.getValue());
      }
    }

    final Instance instance = linker.instantiate(mainStore, module);
    LOGGER.fine(() -> "Compiled and instantiated check module: " + name);
    return instance;
  }

  /**
   * Asserts that a function call on a given instance returns the expected values.
   *
   * @param instance the instance containing the function
   * @param functionName the name of the function to call
   * @param expected the expected return values
   * @param args the function arguments (if any)
   * @throws AssertionError if the assertion fails
   */
  public void assertReturn(
      final Instance instance,
      final String functionName,
      final WasmValue[] expected,
      final WasmValue... args) {
    final WasmFunction function =
        instance
            .getFunction(functionName)
            .orElseThrow(() -> new AssertionError("Function not found: " + functionName));
    final WasmValue[] results;
    try {
      results = function.call(args);
    } catch (final WasmException e) {
      throw new AssertionError("Function call failed: " + functionName, e);
    }
    if (!WasmValueComparator.arraysEqual(expected, results)) {
      throw new AssertionError(
          String.format(
              "assertReturn failed for '%s': expected %s but got %s",
              functionName, Arrays.toString(expected), Arrays.toString(results)));
    }
    LOGGER.fine(() -> "Main thread: assertReturn passed for " + functionName);
  }

  /**
   * Runs all defined threads concurrently and waits for their completion.
   *
   * @throws Exception if any thread fails or times out
   */
  public void runThreads() throws Exception {
    if (threadDefinitions.isEmpty()) {
      return;
    }

    final int threadCount = threadDefinitions.size();
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch doneLatch = new CountDownLatch(threadCount);
    final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    final List<Future<?>> futures = new ArrayList<>();

    try {
      // Submit all threads
      for (final String threadName : executionOrder) {
        final ThreadDefinition def = threadDefinitions.get(threadName);
        futures.add(executor.submit(() -> executeThread(def, startLatch, doneLatch)));
      }

      // Start all threads simultaneously
      startLatch.countDown();

      // Wait for completion with timeout
      if (!doneLatch.await(timeoutSeconds, TimeUnit.SECONDS)) {
        throw new AssertionError("Thread execution timed out after " + timeoutSeconds + " seconds");
      }

      // Check for errors
      final List<String> errors = new ArrayList<>();
      Exception firstCause = null;
      for (final String threadName : executionOrder) {
        final ThreadResult result = threadResults.get(threadName);
        if (result != null && result.error != null) {
          errors.add(threadName + ": " + result.error.getMessage());
          if (firstCause == null) {
            firstCause = result.error;
          }
          // Log full stack trace for debugging
          LOGGER.log(Level.WARNING, "Thread " + threadName + " error details:", result.error);
        }
      }

      if (!errors.isEmpty()) {
        final AssertionError error =
            new AssertionError("Thread errors:\n" + String.join("\n", errors));
        if (firstCause != null) {
          error.initCause(firstCause);
        }
        throw error;
      }

    } finally {
      executor.shutdownNow();
    }
  }

  /** Executes a single thread definition. */
  private void executeThread(
      final ThreadDefinition def, final CountDownLatch startLatch, final CountDownLatch doneLatch) {

    ThreadContext context = null;
    try {
      // Wait for start signal
      startLatch.await();

      // Create thread-local context
      context = new ThreadContext(def.name, engine, this);

      // Run the setup/execution function
      def.setup.accept(context);

      // Record success
      threadResults.put(def.name, new ThreadResult(null));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Thread " + def.name + " failed", e);
      threadResults.put(def.name, new ThreadResult(e));
    } catch (final AssertionError e) {
      LOGGER.log(Level.WARNING, "Thread " + def.name + " assertion failed", e);
      threadResults.put(def.name, new ThreadResult(new Exception(e.getMessage(), e)));
    } finally {
      // Clean up thread-local resources
      if (context != null) {
        context.close();
      }
      doneLatch.countDown();
    }
  }

  /**
   * Waits for a specific thread to complete. This is used to implement the WAST (wait $T)
   * directive.
   *
   * @param threadName the name of the thread to wait for
   * @throws Exception if the thread has not been executed or failed
   */
  public void waitForThread(final String threadName) throws Exception {
    final ThreadResult result = threadResults.get(threadName);
    if (result == null) {
      throw new IllegalStateException("Thread not executed: " + threadName);
    }
    if (result.error != null) {
      throw result.error;
    }
  }

  @Override
  public void close() {
    // Close shared module instances
    for (final SharedModuleInfo info : sharedModules.values()) {
      try {
        info.instance.close();
      } catch (final Exception e) {
        LOGGER.log(Level.FINE, "Error closing shared instance", e);
      }
      try {
        info.module.close();
      } catch (final Exception e) {
        LOGGER.log(Level.FINE, "Error closing shared module", e);
      }
    }
    sharedModules.clear();

    // Close main store and engine
    try {
      mainStore.close();
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Error closing main store", e);
    }
    try {
      engine.close();
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Error closing engine", e);
    }
  }

  /** Information about a shared module. */
  private static final class SharedModuleInfo {
    final Module module;
    final Instance instance;
    final Map<String, WasmMemory> memories;

    SharedModuleInfo(final Module module, final Instance instance) {
      this.module = module;
      this.instance = instance;
      this.memories = new HashMap<>();
    }
  }

  /** Definition of a thread to execute. */
  private static final class ThreadDefinition {
    final String name;
    final Consumer<ThreadContext> setup;

    ThreadDefinition(final String name, final Consumer<ThreadContext> setup) {
      this.name = name;
      this.setup = setup;
    }
  }

  /** Result of thread execution. */
  private static final class ThreadResult {
    final Exception error;

    ThreadResult(final Exception error) {
      this.error = error;
    }
  }

  /**
   * Context for executing operations within a thread.
   *
   * <p>Each thread gets its own Store and can create instances that import shared memory from the
   * main test runner.
   */
  public static final class ThreadContext implements AutoCloseable {

    private final String threadName;
    private final Engine engine;
    private final WastThreadTestRunner runner;
    private final Store store;
    private final Linker<?> linker;
    private final Map<String, Instance> instances;
    private Instance currentInstance;

    ThreadContext(final String threadName, final Engine engine, final WastThreadTestRunner runner)
        throws Exception {
      this.threadName = threadName;
      this.engine = engine;
      this.runner = runner;
      this.store = engine.createStore();
      this.linker = Linker.create(engine);
      this.instances = new HashMap<>();
    }

    /**
     * Imports shared memory from a shared module into this thread's linker.
     *
     * @param importModule the import module name (e.g., "mem")
     * @param importName the import name (e.g., "shared")
     * @param sourceModule the name of the shared module
     * @param exportName the name of the memory export in the shared module
     * @throws Exception if the import fails
     */
    public void importMemory(
        final String importModule,
        final String importName,
        final String sourceModule,
        final String exportName)
        throws Exception {
      final WasmMemory sharedMemory = runner.getSharedMemory(sourceModule, exportName);
      linker.defineMemory(store, importModule, importName, sharedMemory);
      LOGGER.fine(
          () -> "Thread " + threadName + ": imported memory " + importModule + "::" + importName);
    }

    /**
     * Registers a module's exports under a given name in this thread's linker.
     *
     * @param registerName the name to register under
     * @param instanceName the name of the instance to register (or null for current)
     * @throws Exception if registration fails
     */
    public void register(final String registerName, final String instanceName) throws Exception {
      final Instance instance =
          instanceName != null ? instances.get(instanceName) : currentInstance;
      if (instance == null) {
        throw new IllegalStateException("No instance to register");
      }
      linker.defineInstance(store, registerName, instance);
      LOGGER.fine(() -> "Thread " + threadName + ": registered as " + registerName);
    }

    /**
     * Compiles and instantiates a WAT module in this thread's context.
     *
     * @param wat the WebAssembly Text format module
     * @return the instantiated module instance
     * @throws Exception if compilation or instantiation fails
     */
    public Instance compileAndInstantiate(final String wat) throws Exception {
      final Module module = engine.compileWat(wat);
      final Instance instance = linker.instantiate(store, module);
      this.currentInstance = instance;
      LOGGER.fine(() -> "Thread " + threadName + ": instantiated module");
      return instance;
    }

    /**
     * Compiles and instantiates a WAT module with a name.
     *
     * @param name the name for this instance
     * @param wat the WebAssembly Text format module
     * @return the instantiated module instance
     * @throws Exception if compilation or instantiation fails
     */
    public Instance compileAndInstantiate(final String name, final String wat) throws Exception {
      final Instance instance = compileAndInstantiate(wat);
      instances.put(name, instance);
      return instance;
    }

    /**
     * Invokes a function on the current instance.
     *
     * @param functionName the name of the function to invoke
     * @param args the function arguments
     * @return the function results
     * @throws Exception if invocation fails
     */
    public WasmValue[] invoke(final String functionName, final WasmValue... args) throws Exception {
      if (currentInstance == null) {
        throw new IllegalStateException("No current instance");
      }
      final WasmFunction func =
          currentInstance
              .getFunction(functionName)
              .orElseThrow(
                  () -> new IllegalArgumentException("Function not found: " + functionName));
      return func.call(args);
    }

    /**
     * Asserts that a function returns the expected values.
     *
     * @param functionName the name of the function to invoke
     * @param expected the expected return values
     * @param args the function arguments
     * @throws AssertionError if the assertion fails
     */
    public void assertReturn(
        final String functionName, final WasmValue[] expected, final WasmValue... args) {
      try {
        final WasmValue[] actual = invoke(functionName, args);
        if (!WasmValueComparator.arraysEqual(expected, actual)) {
          throw new AssertionError(
              String.format(
                  "Thread %s: assertReturn failed for '%s': expected %s but got %s",
                  threadName, functionName, Arrays.toString(expected), Arrays.toString(actual)));
        }
        LOGGER.fine(() -> "Thread " + threadName + ": assertReturn passed for " + functionName);
      } catch (final Exception e) {
        throw new AssertionError(
            "Thread " + threadName + ": assertReturn failed for " + functionName + ": " + e, e);
      }
    }

    /**
     * Asserts that a function returns void (no return values).
     *
     * @param functionName the name of the function to invoke
     * @param args the function arguments
     * @throws AssertionError if the assertion fails
     */
    public void assertReturnVoid(final String functionName, final WasmValue... args) {
      assertReturn(functionName, new WasmValue[0], args);
    }

    /**
     * Asserts that a function traps with a message containing the expected text.
     *
     * @param functionName the name of the function to invoke
     * @param expectedMessage the expected message substring (or null for any trap)
     * @param args the function arguments
     * @throws AssertionError if the function doesn't trap or traps with wrong message
     */
    public void assertTrap(
        final String functionName, final String expectedMessage, final WasmValue... args) {
      try {
        invoke(functionName, args);
        throw new AssertionError(
            "Thread " + threadName + ": Expected trap for " + functionName + " but succeeded");
      } catch (final Exception e) {
        if (expectedMessage != null && !e.getMessage().contains(expectedMessage)) {
          throw new AssertionError(
              String.format(
                  "Thread %s: Expected trap message containing '%s' but got: %s",
                  threadName, expectedMessage, e.getMessage()),
              e);
        }
        LOGGER.fine(() -> "Thread " + threadName + ": assertTrap passed for " + functionName);
      }
    }

    @Override
    public void close() {
      for (final Instance instance : instances.values()) {
        try {
          instance.close();
        } catch (final Exception e) {
          LOGGER.log(Level.FINE, "Error closing instance", e);
        }
      }
      instances.clear();
      try {
        linker.close();
      } catch (final Exception e) {
        LOGGER.log(Level.FINE, "Error closing linker", e);
      }
      try {
        store.close();
      } catch (final Exception e) {
        LOGGER.log(Level.FINE, "Error closing store", e);
      }
    }
  }
}
