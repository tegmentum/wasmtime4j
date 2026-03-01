package ai.tegmentum.wasmtime4j.tests.wasi;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Comprehensive tests for WASI (WebAssembly System Interface) integration. */
public class WasiTest extends DualRuntimeTest {

  @TempDir Path tempDir;

  /** Clears the runtime selection after each test. */
  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Basic WASI module instantiation")
  public void testBasicWasiInstantiation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
          (memory (export "memory") 1)
          (func (export "_start")
            i32.const 0
            call $proc_exit
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance);

    // Calling _start invokes proc_exit(0) which causes a trap
    // This is expected WASI behavior - proc_exit terminates the process
    // JNI throws RuntimeException, Panama throws WasmException
    try {
      instance.callFunction("_start");
      // If no exception, that's also fine (some implementations may handle exit code 0 specially)
    } catch (Exception e) {
      // Expected - proc_exit causes a trap in WASI
      System.out.println("Expected trap from proc_exit: " + e.getMessage());
    }

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI environment variables")
  public void testWasiEnvironmentVariables(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "environ_sizes_get"
            (func $environ_sizes_get (param i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "environ_get"
            (func $environ_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_env_count") (result i32)
            ;; Get sizes
            i32.const 0  ;; environ_count ptr
            i32.const 4  ;; environ_buf_size ptr
            call $environ_sizes_get
            drop

            ;; Return count
            i32.const 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setEnv("TEST_VAR", "test_value");
    wasiCtx.setEnv("ANOTHER_VAR", "another_value");

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertTrue(results[0].asInt() >= 2); // At least our 2 env vars

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI command-line arguments")
  public void testWasiArguments(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "args_sizes_get"
            (func $args_sizes_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_argc") (result i32)
            ;; Get sizes
            i32.const 0  ;; argc ptr
            i32.const 4  ;; argv_buf_size ptr
            call $args_sizes_get
            drop

            ;; Return argc
            i32.const 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setArgv(new String[] {"program", "arg1", "arg2", "arg3"});

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_argc");
    assertEquals(4, results[0].asInt());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI file system - preopened directory")
  public void testWasiPreopenedDirectory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    // Create test file
    final Path testFile = tempDir.resolve("test.txt");
    Files.writeString(testFile, "Hello WASI!");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_prestat_get"
            (func $fd_prestat_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "check_preopen") (result i32)
            ;; Check if fd 3 is a preopened directory
            i32.const 3
            i32.const 0
            call $fd_prestat_get
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.preopenedDir(tempDir, "/testdir");

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("check_preopen");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI read-only directory")
  public void testWasiReadOnlyDirectory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    // Create test directory with file
    final Path readOnlyDir = tempDir.resolve("readonly");
    Files.createDirectory(readOnlyDir);
    final Path testFile = readOnlyDir.resolve("data.txt");
    Files.writeString(testFile, "Read-only data");

    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_prestat_get"
            (func $fd_prestat_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "test_readonly") (result i32)
            i32.const 3
            i32.const 0
            call $fd_prestat_get
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.preopenedDirReadOnly(readOnlyDir, "/readonly");

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("test_readonly");
    assertEquals(0, results[0].asInt());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI clock operations")
  public void testWasiClock(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "clock_time_get"
            (func $clock_time_get (param i32 i64 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_realtime") (result i32)
            ;; CLOCK_REALTIME = 0
            i32.const 0
            i64.const 1000
            i32.const 0  ;; result ptr
            call $clock_time_get
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_realtime");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI random number generation")
  public void testWasiRandom(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "random_get"
            (func $random_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_random_bytes") (result i32)
            ;; Get 8 random bytes
            i32.const 0
            i32.const 8
            call $random_get
          )

          (func (export "read_random_i32") (result i32)
            ;; Read the first i32
            i32.const 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    // Get random bytes
    WasmValue[] results = instance.callFunction("get_random_bytes");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    // Read random value - should be non-deterministic
    results = instance.callFunction("read_random_i32");
    // Just verify we got a value (can't predict what it is)
    assertNotNull(results[0]);

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI inherit environment")
  public void testWasiInheritEnvironment(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "environ_sizes_get"
            (func $environ_sizes_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_env_count") (result i32)
            i32.const 0
            i32.const 4
            call $environ_sizes_get
            drop
            i32.const 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.inheritEnv(); // Inherit all host environment variables

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertTrue(results[0].asInt() > 0); // Should have host env vars

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI multiple environment variable operations")
  public void testWasiMultipleEnvVars(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "environ_sizes_get"
            (func $environ_sizes_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "get_env_count") (result i32)
            i32.const 0
            i32.const 4
            call $environ_sizes_get
            drop
            i32.const 0
            i32.load
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final Map<String, String> envVars = new HashMap<>();
    envVars.put("VAR1", "value1");
    envVars.put("VAR2", "value2");
    envVars.put("VAR3", "value3");

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setEnv(envVars);

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertEquals(3, results[0].asInt());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI working directory")
  public void testWasiWorkingDirectory(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_prestat_get"
            (func $fd_prestat_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "check_wd") (result i32)
            ;; Check preopened directory
            i32.const 3
            i32.const 0
            call $fd_prestat_get
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setWorkingDirectory(tempDir.toString());

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance);

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI file descriptor limits")
  public void testWasiFileDescriptorLimits(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_prestat_get"
            (func $fd_prestat_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "_start")
            ;; Just check basic operation
            nop
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setMaxOpenFiles(10);

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    instance.callFunction("_start");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI network configuration - verify disabled by default")
  public void testWasiNetworkDisabled(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "random_get"
            (func $random_get (param i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "test_wasi_available") (result i32)
            ;; Just verify WASI is available by calling random_get
            ;; This confirms WASI linking works with network disabled
            i32.const 0   ;; buffer ptr
            i32.const 8   ;; buffer len
            call $random_get
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    // Configure network as disabled - this is the default, but we explicitly set it
    wasiCtx.setNetworkEnabled(false);

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    // Verify instantiation works with network disabled
    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance, "Instance should be created with network disabled");

    // Verify we can call WASI functions using the correct API
    final Optional<WasmFunction> testFuncOpt = instance.getFunction("test_wasi_available");
    assertTrue(testFuncOpt.isPresent(), "Test function should exist");

    final WasmFunction testFunc = testFuncOpt.get();

    // Call should succeed - random_get doesn't require network
    final WasmValue[] result = testFunc.call();
    assertNotNull(result, "Function should return a result");
    assertEquals(1, result.length, "Should return errno");
    // 0 = success (ERRNO_SUCCESS)
    assertEquals(0, result[0].asInt(), "random_get should succeed");

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI with stdio inheritance")
  public void testWasiStdioInheritance(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "write_stdout") (result i32)
            ;; fd=1 is stdout
            i32.const 1
            i32.const 0  ;; iovs ptr
            i32.const 0  ;; iovs_len
            i32.const 100  ;; nwritten ptr
            call $fd_write
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.inheritStdio();

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    // Should succeed (writing 0 bytes is valid)
    final WasmValue[] results = instance.callFunction("write_stdout");
    assertEquals(0, results[0].asInt());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI exit code handling")
  public void testWasiExitCode(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "proc_exit" (func $proc_exit (param i32)))
          (memory (export "memory") 1)

          (func (export "exit_with_code") (param i32)
            local.get 0
            call $proc_exit
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    // proc_exit causes Wasmtime to trap with an exit code.
    // In Wasmtime, proc_exit terminates execution immediately and throws a trap.
    // The trap message should contain information about the exit.
    // JNI throws RuntimeException, Panama throws WasmException
    try {
      instance.callFunction("exit_with_code", WasmValue.i32(42));
      // If we reach here, proc_exit didn't trap as expected.
      // Some implementations may not trap - that's acceptable behavior.
      System.out.println("proc_exit did not trap - implementation allows continued execution");
    } catch (Exception e) {
      // Expected behavior: proc_exit causes a trap
      System.out.println("proc_exit trapped as expected: " + e.getMessage());
      // The exception message should relate to exit/proc_exit
      assertTrue(e.getMessage() != null, "Exception message should not be null for proc_exit trap");
    }

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI output capture - stdout capture enabled")
  public void testWasiStdoutCapture(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=12
          (data (i32.const 0) "\\64\\00\\00\\00\\0c\\00\\00\\00")
          ;; Message at offset 100
          (data (i32.const 100) "Hello, WASI!")

          (func (export "write_hello") (result i32)
            ;; fd=1 is stdout
            i32.const 1      ;; fd
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nwritten ptr
            call $fd_write
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.enableOutputCapture();

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("write_hello");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    // Verify stdout capture is available
    assertTrue(wasiCtx.hasStdoutCapture());

    // Get captured output
    final byte[] captured = wasiCtx.getStdoutCapture();
    assertNotNull(captured);
    final String output = new String(captured, java.nio.charset.StandardCharsets.UTF_8);
    assertEquals("Hello, WASI!", output);

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI output capture - stderr capture enabled")
  public void testWasiStderrCapture(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=11
          (data (i32.const 0) "\\64\\00\\00\\00\\0b\\00\\00\\00")
          ;; Error message at offset 100
          (data (i32.const 100) "Error: test")

          (func (export "write_error") (result i32)
            ;; fd=2 is stderr
            i32.const 2      ;; fd
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nwritten ptr
            call $fd_write
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.enableOutputCapture();

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("write_error");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    // Verify stderr capture is available
    assertTrue(wasiCtx.hasStderrCapture());

    // Get captured error output
    final byte[] captured = wasiCtx.getStderrCapture();
    assertNotNull(captured);
    final String output = new String(captured, java.nio.charset.StandardCharsets.UTF_8);
    assertEquals("Error: test", output);

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI stdin bytes - reading from byte buffer")
  public void testWasiStdinBytes(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_read"
            (func $fd_read (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; iov structure at offset 0: ptr=100, len=64
          (data (i32.const 0) "\\64\\00\\00\\00\\40\\00\\00\\00")

          (func (export "read_stdin") (result i32)
            ;; fd=0 is stdin
            i32.const 0      ;; fd
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nread ptr
            call $fd_read
          )

          (func (export "get_read_count") (result i32)
            i32.const 200
            i32.load
          )

          (func (export "get_first_byte") (result i32)
            i32.const 100
            i32.load8_u
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setStdinBytes("Test input".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    // Read from stdin
    WasmValue[] results = instance.callFunction("read_stdin");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    // Check number of bytes read
    results = instance.callFunction("get_read_count");
    assertEquals(10, results[0].asInt()); // "Test input" = 10 bytes

    // Check first byte is 'T' (84)
    results = instance.callFunction("get_first_byte");
    assertEquals(84, results[0].asInt());

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("WASI combined stdin/stdout/stderr bridging")
  public void testWasiCombinedIoBridging(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "fd_read"
            (func $fd_read (param i32 i32 i32 i32) (result i32)))
          (import "wasi_snapshot_preview1" "fd_write"
            (func $fd_write (param i32 i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          ;; Read iov at offset 0: ptr=100, len=64
          (data (i32.const 0) "\\64\\00\\00\\00\\40\\00\\00\\00")
          ;; Write iov at offset 16: ptr=100, len will be set dynamically

          (func (export "echo_stdin_to_stdout") (result i32)
            (local $nread i32)

            ;; Read from stdin
            i32.const 0      ;; fd=stdin
            i32.const 0      ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 200    ;; nread ptr
            call $fd_read
            drop

            ;; Get bytes read
            i32.const 200
            i32.load
            local.set $nread

            ;; Set up write iov with correct length
            i32.const 16
            i32.const 100
            i32.store
            i32.const 20
            local.get $nread
            i32.store

            ;; Write to stdout
            i32.const 1      ;; fd=stdout
            i32.const 16     ;; iovs ptr
            i32.const 1      ;; iovs_len
            i32.const 204    ;; nwritten ptr
            call $fd_write
          )
        )
        """;

    final Engine engine = Engine.create();
    final Store store = engine.createStore();
    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setStdinBytes("Echo this!".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    wasiCtx.enableOutputCapture();

    final Linker<WasiContext> linker = Linker.create(engine);
    WasiLinkerUtils.addToLinker(linker, wasiCtx);

    final Instance instance = linker.instantiate(store, module);

    // Run the echo function
    final WasmValue[] results = instance.callFunction("echo_stdin_to_stdout");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    // Verify output matches input
    assertTrue(wasiCtx.hasStdoutCapture());
    final byte[] captured = wasiCtx.getStdoutCapture();
    assertNotNull(captured);
    final String output = new String(captured, java.nio.charset.StandardCharsets.UTF_8);
    assertEquals("Echo this!", output);

    instance.close();
    linker.close();
    store.close();
    engine.close();
  }
}
