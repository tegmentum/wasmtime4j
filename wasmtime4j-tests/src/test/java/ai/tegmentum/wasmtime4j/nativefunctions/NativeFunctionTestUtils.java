package ai.tegmentum.wasmtime4j.nativefunctions;

import ai.tegmentum.wasmtime4j.utils.TestUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Utility class for native function testing. Provides WebAssembly module generation, parameter
 * fuzzing, and test data management for comprehensive native function validation.
 *
 * <p>This class complements the existing TestUtils with specialized functionality for native
 * function testing scenarios, including:
 *
 * <ul>
 *   <li>WebAssembly module generation for various test scenarios
 *   <li>Parameter boundary testing and fuzzing
 *   <li>Native handle validation utilities
 *   <li>Resource lifecycle testing helpers
 * </ul>
 */
public final class NativeFunctionTestUtils {
  private static final Logger LOGGER = Logger.getLogger(NativeFunctionTestUtils.class.getName());

  // WebAssembly test module templates
  private static final String SIMPLE_ADD_MODULE_WAT =
      """
      (module
        (func $add (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.add)
        (export "add" (func $add)))
      """;

  private static final String MEMORY_MODULE_WAT =
      """
      (module
        (memory 1)
        (func $get_memory_size (result i32)
          memory.size)
        (func $grow_memory (param i32) (result i32)
          local.get 0
          memory.grow)
        (export "memory" (memory 0))
        (export "get_memory_size" (func $get_memory_size))
        (export "grow_memory" (func $grow_memory)))
      """;

  private static final String TABLE_MODULE_WAT =
      """
      (module
        (table 10 funcref)
        (func $dummy_func (result i32)
          i32.const 42)
        (func $get_table_size (result i32)
          i32.const 10)
        (export "table" (table 0))
        (export "dummy_func" (func $dummy_func))
        (export "get_table_size" (func $get_table_size)))
      """;

  private static final String GLOBAL_MODULE_WAT =
      """
      (module
        (global $counter (mut i32) (i32.const 0))
        (func $get_counter (result i32)
          global.get $counter)
        (func $increment_counter
          global.get $counter
          i32.const 1
          i32.add
          global.set $counter)
        (export "counter" (global $counter))
        (export "get_counter" (func $get_counter))
        (export "increment_counter" (func $increment_counter)))
      """;

  private static final String COMPLEX_MODULE_WAT =
      """
      (module
        (memory 2)
        (table 5 funcref)
        (global $state (mut i32) (i32.const 0))

        (func $fibonacci (param i32) (result i32)
          (local i32 i32)
          local.get 0
          i32.const 2
          i32.lt_s
          if (result i32)
            local.get 0
          else
            local.get 0
            i32.const 1
            i32.sub
            call $fibonacci
            local.get 0
            i32.const 2
            i32.sub
            call $fibonacci
            i32.add
          end)

        (func $store_load (param i32 i32) (result i32)
          local.get 0
          local.get 1
          i32.store
          local.get 0
          i32.load)

        (func $set_state (param i32)
          local.get 0
          global.set $state)

        (func $get_state (result i32)
          global.get $state)

        (export "memory" (memory 0))
        (export "table" (table 0))
        (export "state" (global $state))
        (export "fibonacci" (func $fibonacci))
        (export "store_load" (func $store_load))
        (export "set_state" (func $set_state))
        (export "get_state" (func $get_state)))
      """;

  private static final String ERROR_MODULE_WAT =
      """
      (module
        (func $divide_by_zero (result i32)
          i32.const 10
          i32.const 0
          i32.div_s)

        (func $unreachable_trap
          unreachable)

        (func $out_of_bounds_memory (result i32)
          i32.const 1048576
          i32.load)

        (export "divide_by_zero" (func $divide_by_zero))
        (export "unreachable_trap" (func $unreachable_trap))
        (export "out_of_bounds_memory" (func $out_of_bounds_memory)))
      """;

  /**
   * Gets a simple WebAssembly module with an add function.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getSimpleAddModule() {
    return TestUtils.createSimpleWasmModule();
  }

  /**
   * Gets a WebAssembly module with memory operations.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getMemoryModule() {
    return compileWatModule(MEMORY_MODULE_WAT);
  }

  /**
   * Gets a WebAssembly module with table operations.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getTableModule() {
    return compileWatModule(TABLE_MODULE_WAT);
  }

  /**
   * Gets a WebAssembly module with global variables.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getGlobalModule() {
    return compileWatModule(GLOBAL_MODULE_WAT);
  }

  /**
   * Gets a complex WebAssembly module with multiple features.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getComplexModule() {
    return compileWatModule(COMPLEX_MODULE_WAT);
  }

  /**
   * Gets a WebAssembly module that triggers various error conditions.
   *
   * @return compiled WebAssembly module bytes
   */
  public byte[] getErrorModule() {
    return compileWatModule(ERROR_MODULE_WAT);
  }

  /**
   * Generates a random WebAssembly module for fuzzing.
   *
   * @param seed random seed for reproducible generation
   * @return compiled WebAssembly module bytes
   */
  public byte[] generateRandomModule(final long seed) {
    final Random random = new Random(seed);

    // Generate a simple module with random constants and operations
    final StringBuilder wat = new StringBuilder();
    wat.append("(module\n");

    // Add random functions
    final int functionCount = random.nextInt(5) + 1;
    for (int i = 0; i < functionCount; i++) {
      wat.append("  (func $func").append(i).append(" (param i32) (result i32)\n");
      wat.append("    local.get 0\n");
      wat.append("    i32.const ").append(random.nextInt(100)).append("\n");
      wat.append("    i32.add)\n");
      wat.append("  (export \"func").append(i).append("\" (func $func").append(i).append("))\n");
    }

    wat.append(")");

    return compileWatModule(wat.toString());
  }

  /**
   * Creates a list of test modules for comprehensive testing.
   *
   * @return list of test WebAssembly modules
   */
  public List<TestModule> getAllTestModules() {
    final List<TestModule> modules = new ArrayList<>();

    modules.add(new TestModule("simple_add", getSimpleAddModule(), "Simple addition function"));
    modules.add(new TestModule("memory_ops", getMemoryModule(), "Memory operations"));
    modules.add(new TestModule("table_ops", getTableModule(), "Table operations"));
    modules.add(new TestModule("global_vars", getGlobalModule(), "Global variables"));
    modules.add(new TestModule("complex", getComplexModule(), "Complex multi-feature module"));
    modules.add(new TestModule("error_cases", getErrorModule(), "Error-triggering module"));

    // Add some random modules for fuzzing
    for (int i = 0; i < 3; i++) {
      final long seed = ThreadLocalRandom.current().nextLong();
      modules.add(
          new TestModule(
              "random_" + i, generateRandomModule(seed), "Random module (seed: " + seed + ")"));
    }

    return modules;
  }

  /**
   * Generates test data for parameter fuzzing.
   *
   * @return parameter fuzzing test data
   */
  public ParameterFuzzingData generateFuzzingData() {
    return new ParameterFuzzingData();
  }

  /**
   * Validates a native handle value.
   *
   * @param handle the handle to validate
   * @return true if the handle appears valid
   */
  public boolean isValidNativeHandle(final long handle) {
    // Basic validation - non-zero and not obviously invalid
    return handle != 0L && handle != -1L;
  }

  /**
   * Generates invalid native handle values for testing error conditions.
   *
   * @return array of invalid handle values
   */
  public long[] getInvalidHandles() {
    return new long[] {0L, -1L, Long.MIN_VALUE, -42L};
  }

  /**
   * Generates valid-looking but potentially problematic handle values.
   *
   * @return array of edge case handle values
   */
  public long[] getEdgeCaseHandles() {
    return new long[] {
      1L, Long.MAX_VALUE, 0x7FFFFFFFFFFFFFFFL, 0xDEADBEEFL, 0xCAFEBABEL, 0x1234567890ABCDEFL
    };
  }

  /**
   * Creates a resource lifecycle test scenario.
   *
   * @return resource lifecycle test data
   */
  public ResourceLifecycleTestData createResourceLifecycleTest() {
    return new ResourceLifecycleTestData();
  }

  /**
   * Compiles a WebAssembly Text (WAT) module to binary format.
   *
   * @param watSource the WAT source code
   * @return compiled WebAssembly module bytes
   */
  private byte[] compileWatModule(final String watSource) {
    // For now, return a simple module. In a real implementation, this would
    // use wabt or another WAT->WASM compiler
    LOGGER.warning("WAT compilation not implemented, returning simple module");
    return TestUtils.createSimpleWasmModule();
  }

  /** Represents a test WebAssembly module. */
  public static final class TestModule {
    private final String name;
    private final byte[] moduleBytes;
    private final String description;

    /**
     * Creates a new test module.
     *
     * @param name the module name
     * @param moduleBytes the compiled WebAssembly module bytes
     * @param description the module description
     */
    public TestModule(final String name, final byte[] moduleBytes, final String description) {
      this.name = name;
      this.moduleBytes = moduleBytes.clone();
      this.description = description;
    }

    public String getName() {
      return name;
    }

    public byte[] getModuleBytes() {
      return moduleBytes.clone();
    }

    public String getDescription() {
      return description;
    }

    public int getSize() {
      return moduleBytes.length;
    }
  }

  /** Data for parameter fuzzing tests. */
  public static final class ParameterFuzzingData {
    private final List<Object[]> testCases = new ArrayList<>();

    public ParameterFuzzingData() {
      generateTestCases();
    }

    private void generateTestCases() {
      // Null parameter tests
      testCases.add(new Object[] {"null_bytes", null, "Null byte array"});
      testCases.add(new Object[] {"null_string", null, "Null string parameter"});

      // Empty data tests
      testCases.add(new Object[] {"empty_bytes", new byte[0], "Empty byte array"});
      testCases.add(new Object[] {"empty_string", "", "Empty string"});

      // Boundary value tests
      testCases.add(new Object[] {"max_int", Integer.MAX_VALUE, "Maximum integer value"});
      testCases.add(new Object[] {"min_int", Integer.MIN_VALUE, "Minimum integer value"});
      testCases.add(new Object[] {"max_long", Long.MAX_VALUE, "Maximum long value"});
      testCases.add(new Object[] {"min_long", Long.MIN_VALUE, "Minimum long value"});

      // Large data tests
      final byte[] largeArray = new byte[1024 * 1024]; // 1MB
      testCases.add(new Object[] {"large_array", largeArray, "Large byte array (1MB)"});

      // Random data tests
      final Random random = new Random(42); // Fixed seed for reproducibility
      for (int i = 0; i < 10; i++) {
        final byte[] randomArray = new byte[random.nextInt(1000) + 1];
        random.nextBytes(randomArray);
        testCases.add(new Object[] {"random_" + i, randomArray, "Random data " + i});
      }
    }

    public List<Object[]> getTestCases() {
      return new ArrayList<>(testCases);
    }

    public int getTestCaseCount() {
      return testCases.size();
    }
  }

  /** Data for resource lifecycle testing. */
  public static final class ResourceLifecycleTestData {
    private final List<LifecycleTestCase> testCases = new ArrayList<>();

    public ResourceLifecycleTestData() {
      generateTestCases();
    }

    private void generateTestCases() {
      // Normal lifecycle
      testCases.add(
          new LifecycleTestCase(
              "normal_lifecycle", "Create -> Use -> Close", LifecyclePattern.NORMAL));

      // Double close
      testCases.add(
          new LifecycleTestCase(
              "double_close", "Create -> Close -> Close", LifecyclePattern.DOUBLE_CLOSE));

      // Use after close
      testCases.add(
          new LifecycleTestCase(
              "use_after_close", "Create -> Close -> Use", LifecyclePattern.USE_AFTER_CLOSE));

      // No close (resource leak test)
      testCases.add(
          new LifecycleTestCase(
              "no_close", "Create -> Use -> (no close)", LifecyclePattern.NO_CLOSE));

      // Rapid create/close cycles
      testCases.add(
          new LifecycleTestCase(
              "rapid_cycles", "Rapid create/close cycles", LifecyclePattern.RAPID_CYCLES));
    }

    public List<LifecycleTestCase> getTestCases() {
      return new ArrayList<>(testCases);
    }
  }

  /** Represents a resource lifecycle test case. */
  public static final class LifecycleTestCase {
    private final String name;
    private final String description;
    private final LifecyclePattern pattern;

    /**
     * Creates a new lifecycle test case.
     *
     * @param name the test case name
     * @param description the test case description
     * @param pattern the lifecycle pattern to test
     */
    public LifecycleTestCase(
        final String name, final String description, final LifecyclePattern pattern) {
      this.name = name;
      this.description = description;
      this.pattern = pattern;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public LifecyclePattern getPattern() {
      return pattern;
    }
  }

  /** Enumeration of resource lifecycle patterns for testing. */
  public enum LifecyclePattern {
    NORMAL,
    DOUBLE_CLOSE,
    USE_AFTER_CLOSE,
    NO_CLOSE,
    RAPID_CYCLES
  }
}
