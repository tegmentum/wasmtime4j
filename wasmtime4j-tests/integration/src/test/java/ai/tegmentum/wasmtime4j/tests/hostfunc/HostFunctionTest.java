package ai.tegmentum.wasmtime4j.tests.hostfunc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.func.HostFunction;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/** Comprehensive tests for host functions - Java functions callable from WebAssembly. */
@DisplayName("Host Function Tests")
@SuppressWarnings("deprecation")
public class HostFunctionTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(HostFunctionTest.class.getName());

  /** Clears the runtime selection after each test. */
  @AfterEach
  void cleanup() {
    clearRuntimeSelection();
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Simple host function returning i32")
  public void testSimpleHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing simple host function returning i32");

    // Create host function that adds two numbers
    final HostFunction addFunction =
        HostFunction.singleValue(
            params -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return WasmValue.i32(a + b);
            });

    // Define function type: (i32, i32) -> i32
    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    // WASM module that imports and calls the host function
    final String wat =
        """
        (module
          (import "env" "add" (func $host_add (param i32 i32) (result i32)))
          (func (export "test") (result i32)
            i32.const 10
            i32.const 32
            call $host_add
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "add", funcType, addFunction);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");

        assertEquals(1, results.length, "Should have exactly 1 result");
        assertEquals(42, results[0].asInt(), "10 + 32 should equal 42");
        LOGGER.info("[" + runtime + "] Simple host function returned: " + results[0].asInt());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function with no parameters")
  public void testNoParamHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function with no parameters");

    final HostFunction getConstant = HostFunction.singleValue(params -> WasmValue.i32(42));

    final FunctionType funcType =
        FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    final String wat =
        """
        (module
          (import "env" "get_const" (func $get_const (result i32)))
          (func (export "test") (result i32)
            call $get_const
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "get_const", funcType, getConstant);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");
        assertEquals(42, results[0].asInt(), "Constant should be 42");
        LOGGER.info("[" + runtime + "] No-param host function returned: " + results[0].asInt());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Void host function (no return value)")
  public void testVoidHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing void host function");

    final int[] counter = {0};

    final HostFunction increment =
        HostFunction.voidFunction(
            params -> {
              counter[0]++;
            });

    final FunctionType funcType = FunctionType.of(new WasmValueType[0], new WasmValueType[0]);

    final String wat =
        """
        (module
          (import "env" "increment" (func $increment))
          (func (export "test")
            call $increment
            call $increment
            call $increment
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "increment", funcType, increment);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        assertEquals(0, counter[0], "Counter should start at 0");
        instance.callFunction("test");
        assertEquals(3, counter[0], "Counter should be 3 after three calls");
        LOGGER.info("[" + runtime + "] Void host function called " + counter[0] + " times");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multi-value host function")
  public void testMultiValueHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multi-value host function");

    final HostFunction mathOps =
        HostFunction.multiValue(
            params -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              return new WasmValue[] {
                WasmValue.i32(a + b), // sum
                WasmValue.i32(a - b), // difference
                WasmValue.i32(a * b) // product
              };
            });

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32, WasmValueType.I32});

    final String wat =
        """
        (module
          (import "env" "math_ops" (func $math_ops (param i32 i32) (result i32 i32 i32)))
          (func (export "test") (result i32)
            i32.const 10
            i32.const 3
            call $math_ops
            ;; Stack now has: sum(13), diff(7), product(30)
            ;; Add them: 13 + 7 + 30 = 50
            i32.add
            i32.add
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "math_ops", funcType, mathOps);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");
        assertEquals(1, results.length, "Should have 1 result after addition");
        assertEquals(50, results[0].asInt(), "13 + 7 + 30 should equal 50");
        LOGGER.info("[" + runtime + "] Multi-value host function result: " + results[0].asInt());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function with different types")
  public void testMixedTypeHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function with mixed types");

    final HostFunction mixedTypes =
        HostFunction.singleValue(
            params -> {
              final int i = params[0].asInt();
              final long l = params[1].asLong();
              final float f = params[2].asFloat();
              final double d = params[3].asDouble();
              // Return sum as f64
              return WasmValue.f64(i + l + f + d);
            });

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {
              WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
            },
            new WasmValueType[] {WasmValueType.F64});

    final String wat =
        """
        (module
          (import "env" "mixed" (func $mixed (param i32 i64 f32 f64) (result f64)))
          (func (export "test") (result f64)
            i32.const 1
            i64.const 2
            f32.const 3.5
            f64.const 4.5
            call $mixed
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "mixed", funcType, mixedTypes);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");
        assertEquals(1, results.length, "Should have 1 result");
        assertEquals(11.0, results[0].asDouble(), 0.001, "1 + 2 + 3.5 + 4.5 should equal 11.0");
        LOGGER.info("[" + runtime + "] Mixed type host function result: " + results[0].asDouble());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function called multiple times")
  public void testMultipleHostFunctionCalls(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function called multiple times");

    final int[] callCount = {0};

    final HostFunction counter =
        HostFunction.singleValue(
            params -> {
              callCount[0]++;
              return WasmValue.i32(callCount[0]);
            });

    final FunctionType funcType =
        FunctionType.of(new WasmValueType[0], new WasmValueType[] {WasmValueType.I32});

    final String wat =
        """
        (module
          (import "env" "counter" (func $counter (result i32)))
          (func (export "test") (result i32)
            call $counter
            drop          ;; Discard result of first call
            call $counter
            drop          ;; Discard result of second call
            call $counter ;; Returns result of third call (3)
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "counter", funcType, counter);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        assertEquals(0, callCount[0], "Call count should start at 0");
        final WasmValue[] results = instance.callFunction("test");
        assertEquals(3, callCount[0], "Call count should be 3 after three calls");
        assertEquals(3, results[0].asInt(), "Third call should return 3");
        LOGGER.info("[" + runtime + "] Host function called " + callCount[0] + " times");
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Multiple host functions")
  public void testMultipleHostFunctions(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing multiple host functions");

    final HostFunction add =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() + params[1].asInt()));

    final HostFunction multiply =
        HostFunction.singleValue(params -> WasmValue.i32(params[0].asInt() * params[1].asInt()));

    final FunctionType binaryOp =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    final String wat =
        """
        (module
          (import "env" "add" (func $add (param i32 i32) (result i32)))
          (import "env" "mul" (func $mul (param i32 i32) (result i32)))
          (func (export "test") (result i32)
            ;; (10 + 5) * 3 = 45
            i32.const 10
            i32.const 5
            call $add
            i32.const 3
            call $mul
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "add", binaryOp, add);
      linker.defineHostFunction("env", "mul", binaryOp, multiply);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");
        assertEquals(45, results[0].asInt(), "(10 + 5) * 3 should equal 45");
        LOGGER.info("[" + runtime + "] Multiple host functions result: " + results[0].asInt());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function with state")
  public void testStatefulHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing stateful host function");

    final AtomicInteger state = new AtomicInteger(0);

    final HostFunction increment =
        HostFunction.singleValue(
            params -> {
              final int delta = params[0].asInt();
              return WasmValue.i32(state.addAndGet(delta));
            });

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    final String wat =
        """
        (module
          (import "env" "increment" (func $increment (param i32) (result i32)))
          (func (export "test") (result i32)
            i32.const 10
            call $increment
            drop
            i32.const 20
            call $increment
            drop
            i32.const 12
            call $increment
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "increment", funcType, increment);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        assertEquals(0, state.get(), "State should start at 0");
        final WasmValue[] results = instance.callFunction("test");
        assertEquals(42, state.get(), "State should be 42 after 10+20+12");
        assertEquals(42, results[0].asInt(), "Final result should be 42");
        LOGGER.info("[" + runtime + "] Stateful host function final state: " + state.get());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function with validation - happy path and error path")
  public void testHostFunctionWithValidation(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function with validation");

    final HostFunction safeDiv =
        HostFunction.withFullValidation(
            params -> {
              final int a = params[0].asInt();
              final int b = params[1].asInt();
              if (b == 0) {
                throw new IllegalArgumentException("Division by zero");
              }
              return new WasmValue[] {WasmValue.i32(a / b)};
            },
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            WasmValueType.I32);

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32, WasmValueType.I32},
            new WasmValueType[] {WasmValueType.I32});

    // WAT module for happy path: 42 / 2 = 21
    final String watHappyPath =
        """
        (module
          (import "env" "safe_div" (func $div (param i32 i32) (result i32)))
          (func (export "test") (result i32)
            i32.const 42
            i32.const 2
            call $div
          )
        )
        """;

    // WAT module for error path: 42 / 0 triggers validation exception
    final String watDivByZero =
        """
        (module
          (import "env" "safe_div" (func $div (param i32 i32) (result i32)))
          (func (export "test_div_zero") (result i32)
            i32.const 42
            i32.const 0
            call $div
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "safe_div", funcType, safeDiv);

      // Test happy path: 42 / 2 = 21
      try (Module module = engine.compileWat(watHappyPath);
          Instance instance = linker.instantiate(store, module)) {

        final WasmValue[] results = instance.callFunction("test");
        assertEquals(21, results[0].asInt(), "42 / 2 should equal 21");
        LOGGER.info("[" + runtime + "] Validated division result: " + results[0].asInt());
      }

      // Test error path: division by zero should throw
      try (Module module = engine.compileWat(watDivByZero);
          Instance instance = linker.instantiate(store, module)) {

        LOGGER.info("[" + runtime + "] Testing division by zero validation path");
        final Exception thrown =
            assertThrows(
                Exception.class,
                () -> instance.callFunction("test_div_zero"),
                "Division by zero should throw an exception");
        LOGGER.info(
            "["
                + runtime
                + "] Division by zero threw: "
                + thrown.getClass().getSimpleName()
                + " - "
                + thrown.getMessage());
      }
    }
  }

  @ParameterizedTest
  @ArgumentsSource(RuntimeProvider.class)
  @DisplayName("Host function reading string from WASM memory")
  public void testStringHostFunction(final RuntimeType runtime) throws Exception {
    setRuntime(runtime);
    LOGGER.info("[" + runtime + "] Testing host function with string operations via WASM memory");

    // Use AtomicReference to capture the instance memory for reading in the host function
    final AtomicReference<WasmMemory> memoryRef = new AtomicReference<>();

    // Host function that reads a null-terminated string from WASM memory at the given pointer
    // and returns its length
    final HostFunction strlen =
        HostFunction.singleValue(
            params -> {
              final int ptr = params[0].asInt();
              final WasmMemory memory = memoryRef.get();
              if (memory == null) {
                throw new IllegalStateException("Memory not set");
              }
              // Read bytes from memory until we hit a null terminator
              int length = 0;
              while (memory.readByte(ptr + length) != 0) {
                length++;
              }
              return WasmValue.i32(length);
            });

    final FunctionType funcType =
        FunctionType.of(
            new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I32});

    // WASM module with memory containing a string "hello" at offset 0 (with null terminator)
    // and a function that calls strlen with the pointer to the string
    final String wat =
        """
        (module
          (import "env" "strlen" (func $strlen (param i32) (result i32)))
          (memory (export "memory") 1)
          (data (i32.const 0) "hello\\00")
          (data (i32.const 16) "hello, world!\\00")
          (func (export "test_hello") (result i32)
            i32.const 0
            call $strlen
          )
          (func (export "test_hello_world") (result i32)
            i32.const 16
            call $strlen
          )
        )
        """;

    try (Engine engine = Engine.create();
        Store store = engine.createStore();
        Linker<Void> linker = Linker.create(engine)) {

      linker.defineHostFunction("env", "strlen", funcType, strlen);

      try (Module module = engine.compileWat(wat);
          Instance instance = linker.instantiate(store, module)) {

        // Capture the exported memory so the host function can read from it
        final WasmMemory memory =
            instance
                .getMemory("memory")
                .orElseThrow(() -> new AssertionError("Memory export 'memory' should be present"));
        memoryRef.set(memory);

        // Verify the string "hello" is actually in memory
        final byte[] helloBytes = new byte[5];
        memory.readBytes(0, helloBytes, 0, 5);
        final String helloStr = new String(helloBytes, StandardCharsets.UTF_8);
        assertEquals("hello", helloStr, "Memory should contain 'hello' at offset 0");
        LOGGER.info("[" + runtime + "] Read string from WASM memory: '" + helloStr + "'");

        // Test strlen("hello") = 5
        final WasmValue[] result1 = instance.callFunction("test_hello");
        assertEquals(5, result1[0].asInt(), "strlen('hello') should be 5");
        LOGGER.info("[" + runtime + "] strlen('hello') = " + result1[0].asInt());

        // Test strlen("hello, world!") = 13
        final WasmValue[] result2 = instance.callFunction("test_hello_world");
        assertEquals(13, result2[0].asInt(), "strlen('hello, world!') should be 13");
        LOGGER.info("[" + runtime + "] strlen('hello, world!') = " + result2[0].asInt());
      }
    }
  }
}
