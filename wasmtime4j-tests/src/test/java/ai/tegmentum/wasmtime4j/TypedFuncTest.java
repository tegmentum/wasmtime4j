package ai.tegmentum.wasmtime4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive integration tests for TypedFunc zero-cost typed function calls.
 *
 * <p>This test suite validates the complete TypedFunc lifecycle including creation, invocation,
 * type safety, and performance characteristics across both JNI and Panama implementations.
 *
 * <p>Tests cover all supported function signatures:
 *
 * <ul>
 *   <li>() -> void
 *   <li>(i32) -> i32
 *   <li>(i32, i32) -> i32
 *   <li>(i64) -> i64
 *   <li>(i64, i64) -> i64
 *   <li>(f32) -> f32
 *   <li>(f64) -> f64
 * </ul>
 */
class TypedFuncTest {

  private Engine engine;
  private Store store;
  private Module module;
  private Instance instance;

  @BeforeEach
  void setUp() throws WasmException, IOException, URISyntaxException {
    engine = Engine.create();
    store = Store.create(engine);

    // Load the typed-funcs.wasm test module
    final Path wasmPath =
        Paths.get(getClass().getClassLoader().getResource("wasm/typed-funcs.wasm").toURI());
    final byte[] wasmBytes = Files.readAllBytes(wasmPath);
    module = Module.compile(engine, wasmBytes);
    instance = Instance.create(store, module);
  }

  @AfterEach
  void tearDown() {
    if (instance != null) {
      instance.close();
    }
    if (module != null) {
      module.close();
    }
    if (store != null) {
      store.close();
    }
    if (engine != null) {
      engine.close();
    }
  }

  // Test factory method and creation

  @Test
  void testCreateTypedFuncFromWasmFunction() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    assertThat(typedAdd).isNotNull();
    assertThat(typedAdd.getSignature()).isEqualTo("ii->i");
    assertThat(typedAdd.getFunction()).isSameAs(addFunc);
  }

  @Test
  void testCreateTypedFuncWithNullFunction() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TypedFunc.create(null, "ii->i");
        });
  }

  @Test
  void testCreateTypedFuncWithNullSignature() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TypedFunc.create(addFunc, null);
        });
  }

  @Test
  void testCreateTypedFuncWithEmptySignature() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TypedFunc.create(addFunc, "");
        });
  }

  // Test void -> void signature

  @Test
  void testCallVoidToVoid() throws WasmException {
    final WasmFunction noopFunc = instance.getFunction("noop").orElseThrow();
    final TypedFunc typedNoop = TypedFunc.create(noopFunc, "v->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedNoop.callVoidToVoid();
        });
  }

  // Test (i32) -> void signature

  @Test
  void testCallI32ToVoid() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i32").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "i->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI32ToVoid(42);
        });
  }

  @Test
  void testCallI32ToVoidWithZero() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i32").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "i->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI32ToVoid(0);
        });
  }

  @Test
  void testCallI32ToVoidWithNegative() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i32").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "i->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI32ToVoid(-100);
        });
  }

  // Test (i32, i32) -> void signature

  @Test
  void testCallI32I32ToVoid() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i32i32").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "ii->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI32I32ToVoid(10, 20);
        });
  }

  @Test
  void testCallI32I32ToVoidWithZeros() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i32i32").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "ii->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI32I32ToVoid(0, 0);
        });
  }

  // Test (i64) -> void signature

  @Test
  void testCallI64ToVoid() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i64").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "I->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI64ToVoid(9999999999L);
        });
  }

  @Test
  void testCallI64ToVoidWithZero() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i64").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "I->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI64ToVoid(0L);
        });
  }

  // Test (i64, i64) -> void signature

  @Test
  void testCallI64I64ToVoid() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i64i64").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "II->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI64I64ToVoid(100000000000L, 200000000000L);
        });
  }

  @Test
  void testCallI64I64ToVoidWithZeros() throws WasmException {
    final WasmFunction consumeFunc = instance.getFunction("consume_i64i64").orElseThrow();
    final TypedFunc typedConsume = TypedFunc.create(consumeFunc, "II->v");

    // Should execute without throwing
    assertDoesNotThrow(
        () -> {
          typedConsume.callI64I64ToVoid(0L, 0L);
        });
  }

  // Test i32 -> i32 signature

  @Test
  void testCallI32ToI32() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i32").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "i->i");

    final int result = typedIncrement.callI32ToI32(41);
    assertThat(result).isEqualTo(42);
  }

  @Test
  void testCallI32ToI32WithZero() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i32").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "i->i");

    final int result = typedIncrement.callI32ToI32(0);
    assertThat(result).isEqualTo(1);
  }

  @Test
  void testCallI32ToI32WithNegative() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i32").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "i->i");

    final int result = typedIncrement.callI32ToI32(-100);
    assertThat(result).isEqualTo(-99);
  }

  @Test
  void testCallI32ToI32WithMaxValue() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i32").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "i->i");

    // Integer overflow wraps around
    final int result = typedIncrement.callI32ToI32(Integer.MAX_VALUE);
    assertThat(result).isEqualTo(Integer.MIN_VALUE);
  }

  // Test (i32, i32) -> i32 signature

  @Test
  void testCallI32I32ToI32() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int result = typedAdd.callI32I32ToI32(10, 20);
    assertThat(result).isEqualTo(30);
  }

  @Test
  void testCallI32I32ToI32WithZeros() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int result = typedAdd.callI32I32ToI32(0, 0);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void testCallI32I32ToI32WithNegatives() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int result = typedAdd.callI32I32ToI32(-50, 30);
    assertThat(result).isEqualTo(-20);
  }

  @Test
  void testCallI32I32ToI32WithLargeValues() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int result = typedAdd.callI32I32ToI32(1_000_000, 2_000_000);
    assertThat(result).isEqualTo(3_000_000);
  }

  // Test i64 -> i64 signature

  @Test
  void testCallI64ToI64() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i64").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "I->I");

    final long result = typedIncrement.callI64ToI64(9999999999L);
    assertThat(result).isEqualTo(10000000000L);
  }

  @Test
  void testCallI64ToI64WithZero() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i64").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "I->I");

    final long result = typedIncrement.callI64ToI64(0L);
    assertThat(result).isEqualTo(1L);
  }

  @Test
  void testCallI64ToI64WithNegative() throws WasmException {
    final WasmFunction incrementFunc = instance.getFunction("increment_i64").orElseThrow();
    final TypedFunc typedIncrement = TypedFunc.create(incrementFunc, "I->I");

    final long result = typedIncrement.callI64ToI64(-1000000000000L);
    assertThat(result).isEqualTo(-999999999999L);
  }

  // Test (i64, i64) -> i64 signature

  @Test
  void testCallI64I64ToI64() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "II->I");

    final long result = typedAdd.callI64I64ToI64(100000000000L, 200000000000L);
    assertThat(result).isEqualTo(300000000000L);
  }

  @Test
  void testCallI64I64ToI64WithZeros() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "II->I");

    final long result = typedAdd.callI64I64ToI64(0L, 0L);
    assertThat(result).isEqualTo(0L);
  }

  @Test
  void testCallI64I64ToI64WithNegatives() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "II->I");

    final long result = typedAdd.callI64I64ToI64(-5000000000L, 3000000000L);
    assertThat(result).isEqualTo(-2000000000L);
  }

  // Test f32 -> f32 signature

  @Test
  void testCallF32ToF32() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f32").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "f->f");

    final float result = typedNegate.callF32ToF32(3.14159f);
    assertThat(result).isEqualTo(-3.14159f, offset(0.00001f));
  }

  @Test
  void testCallF32ToF32WithZero() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f32").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "f->f");

    final float result = typedNegate.callF32ToF32(0.0f);
    assertThat(result).isEqualTo(-0.0f);
  }

  @Test
  void testCallF32ToF32WithNegative() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f32").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "f->f");

    final float result = typedNegate.callF32ToF32(-2.718f);
    assertThat(result).isEqualTo(2.718f, offset(0.001f));
  }

  @Test
  void testCallF32ToF32WithLargeValue() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f32").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "f->f");

    final float result = typedNegate.callF32ToF32(123456.789f);
    assertThat(result).isEqualTo(-123456.789f, offset(0.01f));
  }

  // Test f64 -> f64 signature

  @Test
  void testCallF64ToF64() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f64").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "F->F");

    final double result = typedNegate.callF64ToF64(2.71828182845904523536);
    assertThat(result).isEqualTo(-2.71828182845904523536, offset(0.000000000000001));
  }

  @Test
  void testCallF64ToF64WithZero() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f64").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "F->F");

    final double result = typedNegate.callF64ToF64(0.0);
    assertThat(result).isEqualTo(-0.0);
  }

  @Test
  void testCallF64ToF64WithNegative() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f64").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "F->F");

    final double result = typedNegate.callF64ToF64(-123.456789);
    assertThat(result).isEqualTo(123.456789, offset(0.000001));
  }

  @Test
  void testCallF64ToF64WithLargeValue() throws WasmException {
    final WasmFunction negateFunc = instance.getFunction("negate_f64").orElseThrow();
    final TypedFunc typedNegate = TypedFunc.create(negateFunc, "F->F");

    final double result = typedNegate.callF64ToF64(9876543210.123456789);
    assertThat(result).isEqualTo(-9876543210.123456789, offset(0.000000001));
  }

  // Test (f32, f32) -> f32 signature

  @Test
  void testCallF32F32ToF32() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ff->f");

    final float result = typedAdd.callF32F32ToF32(1.5f, 2.5f);
    assertThat(result).isEqualTo(4.0f, offset(0.0001f));
  }

  @Test
  void testCallF32F32ToF32WithZeros() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ff->f");

    final float result = typedAdd.callF32F32ToF32(0.0f, 0.0f);
    assertThat(result).isEqualTo(0.0f, offset(0.0001f));
  }

  @Test
  void testCallF32F32ToF32WithNegatives() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ff->f");

    final float result = typedAdd.callF32F32ToF32(-3.5f, 1.5f);
    assertThat(result).isEqualTo(-2.0f, offset(0.0001f));
  }

  // Test (f64, f64) -> f64 signature

  @Test
  void testCallF64F64ToF64() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "FF->F");

    final double result = typedAdd.callF64F64ToF64(123.456, 789.012);
    assertThat(result).isEqualTo(912.468, offset(0.000001));
  }

  @Test
  void testCallF64F64ToF64WithZeros() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "FF->F");

    final double result = typedAdd.callF64F64ToF64(0.0, 0.0);
    assertThat(result).isEqualTo(0.0, offset(0.000001));
  }

  @Test
  void testCallF64F64ToF64WithNegatives() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_f64").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "FF->F");

    final double result = typedAdd.callF64F64ToF64(-999.999, 111.111);
    assertThat(result).isEqualTo(-888.888, offset(0.000001));
  }

  // Test (i32, i32, i32) -> i32 signature

  @Test
  void testCallI32I32I32ToI32() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i32").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "iii->i");

    final int result = typedAddThree.callI32I32I32ToI32(10, 20, 30);
    assertThat(result).isEqualTo(60);
  }

  @Test
  void testCallI32I32I32ToI32WithZeros() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i32").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "iii->i");

    final int result = typedAddThree.callI32I32I32ToI32(0, 0, 0);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void testCallI32I32I32ToI32WithNegatives() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i32").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "iii->i");

    final int result = typedAddThree.callI32I32I32ToI32(-100, 50, 25);
    assertThat(result).isEqualTo(-25);
  }

  @Test
  void testCallI32I32I32ToI32WithLargeValues() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i32").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "iii->i");

    final int result = typedAddThree.callI32I32I32ToI32(1_000_000, 2_000_000, 3_000_000);
    assertThat(result).isEqualTo(6_000_000);
  }

  // Test (i64, i64, i64) -> i64 signature

  @Test
  void testCallI64I64I64ToI64() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i64").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "III->I");

    final long result = typedAddThree.callI64I64I64ToI64(100L, 200L, 300L);
    assertThat(result).isEqualTo(600L);
  }

  @Test
  void testCallI64I64I64ToI64WithZeros() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i64").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "III->I");

    final long result = typedAddThree.callI64I64I64ToI64(0L, 0L, 0L);
    assertThat(result).isEqualTo(0L);
  }

  @Test
  void testCallI64I64I64ToI64WithNegatives() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i64").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "III->I");

    final long result = typedAddThree.callI64I64I64ToI64(-1000000000000L, 500000000000L, 250000000000L);
    assertThat(result).isEqualTo(-250000000000L);
  }

  @Test
  void testCallI64I64I64ToI64WithLargeValues() throws WasmException {
    final WasmFunction addThreeFunc = instance.getFunction("add_three_i64").orElseThrow();
    final TypedFunc typedAddThree = TypedFunc.create(addThreeFunc, "III->I");

    final long result =
        typedAddThree.callI64I64I64ToI64(100000000000L, 200000000000L, 300000000000L);
    assertThat(result).isEqualTo(600000000000L);
  }

  // Test resource management

  @Test
  void testTypedFuncClose() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    // Should work before close
    final int result1 = typedAdd.callI32I32ToI32(10, 20);
    assertThat(result1).isEqualTo(30);

    // Close the typed function
    typedAdd.close();

    // Should throw after close
    assertThrows(
        IllegalStateException.class,
        () -> {
          typedAdd.callI32I32ToI32(10, 20);
        });
  }

  @Test
  void testTypedFuncDoubleClose() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    // First close should succeed
    typedAdd.close();

    // Second close should be idempotent (not throw)
    assertDoesNotThrow(
        () -> {
          typedAdd.close();
        });
  }

  @Test
  void testTypedFuncAutoClose() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();

    // Use try-with-resources
    try (final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i")) {
      final int result = typedAdd.callI32I32ToI32(100, 200);
      assertThat(result).isEqualTo(300);
    } // Should auto-close here

    // No assertion needed - just verifying no exceptions during auto-close
  }

  // Test performance characteristics

  @Test
  void testTypedFuncPerformance() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int iterations = 100_000;
    final long startTime = System.nanoTime();

    int sum = 0;
    for (int i = 0; i < iterations; i++) {
      sum += typedAdd.callI32I32ToI32(i, i + 1);
    }

    final long endTime = System.nanoTime();
    final long durationMs = (endTime - startTime) / 1_000_000;

    // Performance assertion - should complete in reasonable time
    assertThat(durationMs).isLessThan(5000); // 5 seconds max for 100k operations

    System.out.printf(
        "TypedFunc performance: %d calls in %d ms (%.2f ops/ms)%n",
        iterations, durationMs, (double) iterations / durationMs);

    // Verify correctness
    assertThat(sum).isGreaterThan(0);
  }

  @Test
  void testTypedFuncVsRegularFuncPerformanceComparison() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();
    final TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");

    final int warmupIterations = 1000;
    final int iterations = 50_000;

    // Warmup both approaches
    for (int i = 0; i < warmupIterations; i++) {
      addFunc.call(WasmValue.i32(i), WasmValue.i32(i + 1));
      typedAdd.callI32I32ToI32(i, i + 1);
    }

    // Test regular WasmFunction
    long regularStartTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      addFunc.call(WasmValue.i32(i), WasmValue.i32(i + 1));
    }
    long regularEndTime = System.nanoTime();
    long regularDuration = (regularEndTime - regularStartTime) / 1_000_000;

    // Test TypedFunc
    long typedStartTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      typedAdd.callI32I32ToI32(i, i + 1);
    }
    long typedEndTime = System.nanoTime();
    long typedDuration = (typedEndTime - typedStartTime) / 1_000_000;

    System.out.printf(
        "Performance comparison (%d iterations):%n"
            + "  Regular WasmFunction: %d ms (%.2f ops/ms)%n"
            + "  TypedFunc: %d ms (%.2f ops/ms)%n"
            + "  Speedup: %.2fx%n",
        iterations,
        regularDuration,
        (double) iterations / regularDuration,
        typedDuration,
        (double) iterations / typedDuration,
        (double) regularDuration / typedDuration);

    // TypedFunc should be at least as fast as regular function calls
    // (may be faster due to zero-cost abstraction, but at minimum shouldn't be slower)
    assertThat(typedDuration).isLessThanOrEqualTo(regularDuration * 2); // Allow 2x tolerance
  }

  // Test multiple typed functions from same WasmFunction

  @Test
  void testMultipleTypedFuncsFromSameFunction() throws WasmException {
    final WasmFunction addFunc = instance.getFunction("add_i32").orElseThrow();

    final TypedFunc typedAdd1 = TypedFunc.create(addFunc, "ii->i");
    final TypedFunc typedAdd2 = TypedFunc.create(addFunc, "ii->i");

    // Both should work independently
    final int result1 = typedAdd1.callI32I32ToI32(10, 20);
    final int result2 = typedAdd2.callI32I32ToI32(30, 40);

    assertThat(result1).isEqualTo(30);
    assertThat(result2).isEqualTo(70);

    // They should reference the same underlying function
    assertThat(typedAdd1.getFunction()).isSameAs(typedAdd2.getFunction());
    assertThat(typedAdd1.getFunction()).isSameAs(addFunc);
  }

  // Helper method for AssertJ offset

  private static org.assertj.core.data.Offset<Float> offset(final float value) {
    return org.assertj.core.data.Offset.offset(value);
  }

  private static org.assertj.core.data.Offset<Double> offset(final double value) {
    return org.assertj.core.data.Offset.offset(value);
  }
}
