package ai.tegmentum.wasmtime4j.benchmarks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ai.tegmentum.wasmtime4j.RuntimeType;
import org.junit.jupiter.api.Test;

/** Unit tests for BenchmarkBase utility methods and constants. */
final class BenchmarkBaseTest {

  @Test
  void testSimpleWatModuleIsDefined() {
    // Test that the simple WAT module string is defined and non-empty
    assertThat(BenchmarkBase.SIMPLE_WAT_MODULE).isNotNull();
    assertThat(BenchmarkBase.SIMPLE_WAT_MODULE).isNotEmpty();
    assertThat(BenchmarkBase.SIMPLE_WAT_MODULE).contains("(module");
    assertThat(BenchmarkBase.SIMPLE_WAT_MODULE).contains("add");
  }

  @Test
  void testComplexWatModuleIsDefined() {
    // Test that the complex WAT module string is defined and non-empty
    assertThat(BenchmarkBase.COMPLEX_WAT_MODULE).isNotNull();
    assertThat(BenchmarkBase.COMPLEX_WAT_MODULE).isNotEmpty();
    assertThat(BenchmarkBase.COMPLEX_WAT_MODULE).contains("(module");
    assertThat(BenchmarkBase.COMPLEX_WAT_MODULE).contains("fibonacci");
    assertThat(BenchmarkBase.COMPLEX_WAT_MODULE).contains("memory");
  }

  @Test
  void testGetJavaVersion() {
    final int version = BenchmarkBase.getJavaVersion();
    assertThat(version).isGreaterThanOrEqualTo(8);
    assertThat(version).isLessThan(50); // Sanity check for future versions
  }

  @Test
  void testGetRecommendedRuntime() {
    final RuntimeType runtime = BenchmarkBase.getRecommendedRuntime();
    assertThat(runtime).isIn(RuntimeType.JNI, RuntimeType.PANAMA);
  }

  @Test
  void testValidateWasmModuleWithValidModule() {
    // Minimal valid WASM module: magic number + version + empty sections
    final byte[] validWasmModule = {
      0x00, 0x61, 0x73, 0x6d, // WASM magic number
      0x01, 0x00, 0x00, 0x00 // WASM version 1
    };
    // Should not throw exception
    BenchmarkBase.validateWasmModule(validWasmModule);
  }

  @Test
  void testValidateWasmModuleWithNullModule() {
    assertThatThrownBy(() -> BenchmarkBase.validateWasmModule(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");
  }

  @Test
  void testValidateWasmModuleWithTooSmallModule() {
    final byte[] tooSmall = new byte[4];
    assertThatThrownBy(() -> BenchmarkBase.validateWasmModule(tooSmall))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("too small");
  }

  @Test
  void testValidateWasmModuleWithInvalidMagicNumber() {
    final byte[] invalidMagic = {0x01, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};
    assertThatThrownBy(() -> BenchmarkBase.validateWasmModule(invalidMagic))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid WASM magic number");
  }

  @Test
  void testFormatBenchmarkId() {
    final String id = BenchmarkBase.formatBenchmarkId("test_operation", RuntimeType.JNI);
    assertThat(id).startsWith("test_operation_jni_");
    assertThat(id)
        .hasSizeGreaterThanOrEqualTo("test_operation_jni_".length() + 1); // Variable suffix
    assertThat(id)
        .hasSizeLessThanOrEqualTo("test_operation_jni_".length() + 4); // Max 4-digit suffix
  }

  @Test
  void testPreventOptimizationWithInt() {
    final int value = 42;
    final int result = BenchmarkBase.preventOptimization(value);
    assertThat(result).isEqualTo(value);
  }

  @Test
  void testPreventOptimizationWithByteArray() {
    final byte[] value = new byte[10];
    final int result = BenchmarkBase.preventOptimization(value);
    assertThat(result).isEqualTo(10);
  }

  @Test
  void testPreventOptimizationWithNullByteArray() {
    final int result = BenchmarkBase.preventOptimization((byte[]) null);
    assertThat(result).isEqualTo(0);
  }

  @Test
  void testRuntimeTypeValues() {
    // Test that all expected runtime types exist
    assertThat(RuntimeType.values()).containsExactly(RuntimeType.JNI, RuntimeType.PANAMA);
  }
}
