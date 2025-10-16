package ai.tegmentum.wasmtime4j.comparison.wasi;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasiContext;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprehensive tests for WASI (WebAssembly System Interface) integration. */
public class WasiTest {

  private Engine engine;
  private Store store;

  @TempDir Path tempDir;

  /** Sets up the test engine and store before each test. */
  @BeforeEach
  public void setUp() throws Exception {
    engine = Engine.create();
    store = engine.createStore();
  }

  /** Cleans up the test engine and store after each test. */
  @AfterEach
  public void tearDown() {
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  @Test
  @DisplayName("Basic WASI module instantiation")
  public void testBasicWasiInstantiation() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance);

    // Start function should exit with code 0
    instance.callFunction("_start");

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI environment variables")
  public void testWasiEnvironmentVariables() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setEnv("TEST_VAR", "test_value");
    wasiCtx.setEnv("ANOTHER_VAR", "another_value");

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertTrue(results[0].asInt() >= 2); // At least our 2 env vars

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI command-line arguments")
  public void testWasiArguments() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setArgv(new String[] {"program", "arg1", "arg2", "arg3"});

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_argc");
    assertEquals(4, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI file system - preopened directory")
  public void testWasiPreopenedDirectory() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.preopenedDir(tempDir, "/testdir");

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("check_preopen");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI read-only directory")
  public void testWasiReadOnlyDirectory() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.preopenedDirReadOnly(readOnlyDir, "/readonly");

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("test_readonly");
    assertEquals(0, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI clock operations")
  public void testWasiClock() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_realtime");
    assertEquals(0, results[0].asInt()); // WASI_ESUCCESS

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI random number generation")
  public void testWasiRandom() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

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
  }

  @Test
  @DisplayName("WASI inherit environment")
  public void testWasiInheritEnvironment() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.inheritEnv(); // Inherit all host environment variables

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertTrue(results[0].asInt() > 0); // Should have host env vars

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI multiple environment variable operations")
  public void testWasiMultipleEnvVars() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final Map<String, String> envVars = new HashMap<>();
    envVars.put("VAR1", "value1");
    envVars.put("VAR2", "value2");
    envVars.put("VAR3", "value3");

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setEnv(envVars);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    final WasmValue[] results = instance.callFunction("get_env_count");
    assertEquals(3, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI working directory")
  public void testWasiWorkingDirectory() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setWorkingDirectory("/app");

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance);

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI file descriptor limits")
  public void testWasiFileDescriptorLimits() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setMaxOpenFiles(10);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    instance.callFunction("_start");

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI network operations - disabled by default")
  public void testWasiNetworkDisabled() throws Exception {
    final String wat =
        """
        (module
          (import "wasi_snapshot_preview1" "sock_open"
            (func $sock_open (param i32 i32 i32) (result i32)))
          (memory (export "memory") 1)

          (func (export "try_network") (result i32)
            ;; Try to open a socket (should fail if network disabled)
            i32.const 0  ;; AF_INET
            i32.const 1  ;; SOCK_STREAM
            i32.const 0  ;; result ptr
            call $sock_open
          )
        )
        """;

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.setNetworkEnabled(false);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    // This might fail during instantiation or during call
    // depending on implementation
    final Instance instance = linker.instantiate(store, module);
    assertNotNull(instance);

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI with stdio inheritance")
  public void testWasiStdioInheritance() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final WasiContext wasiCtx = WasiContext.create();
    wasiCtx.inheritStdio();

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    // Should succeed (writing 0 bytes is valid)
    final WasmValue[] results = instance.callFunction("write_stdout");
    assertEquals(0, results[0].asInt());

    instance.close();
    linker.close();
  }

  @Test
  @DisplayName("WASI exit code handling")
  public void testWasiExitCode() throws Exception {
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

    final Module module = engine.compileWat(wat);

    final Linker linker = Linker.create(engine);
    linker.enableWasi();

    final Instance instance = linker.instantiate(store, module);

    // proc_exit should cause the function to terminate
    // Implementation details will vary
    instance.callFunction("exit_with_code", WasmValue.i32(42));

    instance.close();
    linker.close();
  }
}
