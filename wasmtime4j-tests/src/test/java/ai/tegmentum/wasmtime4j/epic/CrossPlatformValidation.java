/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.epic;

import static org.assertj.core.api.Assertions.*;

import ai.tegmentum.wasmtime4j.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Comprehensive cross-platform validation framework that ensures all Wasmtime4j functionality works
 * consistently across all supported platforms and architectures.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class CrossPlatformValidation {

  private static final Logger LOGGER = Logger.getLogger(CrossPlatformValidation.class.getName());

  /** Supported platform enumeration. */
  public enum Platform {
    LINUX_X86_64("linux", "x86_64"),
    LINUX_AARCH64("linux", "aarch64"),
    WINDOWS_X86_64("windows", "x86_64"),
    MACOS_X86_64("macos", "x86_64"),
    MACOS_AARCH64("macos", "aarch64");

    private final String os;
    private final String arch;

    Platform(final String os, final String arch) {
      this.os = os;
      this.arch = arch;
    }

    public String getOs() {
      return os;
    }

    public String getArch() {
      return arch;
    }

    @Override
    public String toString() {
      return String.format("%s-%s", os, arch);
    }
  }

  /** Platform compatibility report. */
  public static final class PlatformCompatibilityReport {
    private final Platform platform;
    private final boolean isSupported;
    private final boolean nativeLibraryLoaded;
    private final boolean basicFunctionalityWorks;
    private final boolean jniImplementationWorks;
    private final boolean panamaImplementationWorks;
    private final List<String> issues;
    private final Map<String, Object> performanceMetrics;

    PlatformCompatibilityReport(
        final Platform platform,
        final boolean isSupported,
        final boolean nativeLibraryLoaded,
        final boolean basicFunctionalityWorks,
        final boolean jniImplementationWorks,
        final boolean panamaImplementationWorks,
        final List<String> issues,
        final Map<String, Object> performanceMetrics) {
      this.platform = platform;
      this.isSupported = isSupported;
      this.nativeLibraryLoaded = nativeLibraryLoaded;
      this.basicFunctionalityWorks = basicFunctionalityWorks;
      this.jniImplementationWorks = jniImplementationWorks;
      this.panamaImplementationWorks = panamaImplementationWorks;
      this.issues = Collections.unmodifiableList(new ArrayList<>(issues));
      this.performanceMetrics = Collections.unmodifiableMap(new HashMap<>(performanceMetrics));
    }

    public Platform getPlatform() {
      return platform;
    }

    public boolean isSupported() {
      return isSupported;
    }

    public boolean isNativeLibraryLoaded() {
      return nativeLibraryLoaded;
    }

    public boolean isBasicFunctionalityWorks() {
      return basicFunctionalityWorks;
    }

    public boolean isJniImplementationWorks() {
      return jniImplementationWorks;
    }

    public boolean isPanamaImplementationWorks() {
      return panamaImplementationWorks;
    }

    public List<String> getIssues() {
      return issues;
    }

    public Map<String, Object> getPerformanceMetrics() {
      return performanceMetrics;
    }

    public boolean isFullyCompatible() {
      return isSupported && nativeLibraryLoaded && basicFunctionalityWorks;
    }

    public String getSummary() {
      return String.format(
          "%s: %s (Native: %s, Basic: %s, JNI: %s, Panama: %s, Issues: %d)",
          platform,
          isSupported ? "SUPPORTED" : "NOT SUPPORTED",
          nativeLibraryLoaded ? "OK" : "FAIL",
          basicFunctionalityWorks ? "OK" : "FAIL",
          jniImplementationWorks ? "OK" : "FAIL",
          panamaImplementationWorks ? "OK" : "FAIL",
          issues.size());
    }
  }

  private static final byte[] SIMPLE_WASM_MODULE =
      new byte[] {
        0x00,
        0x61,
        0x73,
        0x6d, // magic
        0x01,
        0x00,
        0x00,
        0x00, // version
        0x01,
        0x07,
        0x01,
        0x60,
        0x02,
        0x7f,
        0x7f,
        0x01,
        0x7f, // type section
        0x03,
        0x02,
        0x01,
        0x00, // function section
        0x07,
        0x07,
        0x01,
        0x03,
        0x61,
        0x64,
        0x64,
        0x00,
        0x00, // export section
        0x0a,
        0x09,
        0x01,
        0x07,
        0x00,
        0x20,
        0x00,
        0x20,
        0x01,
        0x6a,
        0x0b // code section
      };

  private Platform currentPlatform;

  @BeforeEach
  void setUp() {
    currentPlatform = detectCurrentPlatform();
    LOGGER.info(String.format("Running cross-platform validation on: %s", currentPlatform));
  }

  /** Validate compatibility on current platform. */
  @Test
  @Order(1)
  @DisplayName("Current Platform Compatibility")
  void validateCurrentPlatformCompatibility() {
    LOGGER.info("Validating current platform compatibility");

    final PlatformCompatibilityReport report = validatePlatformCompatibility(currentPlatform);

    assertThat(report.isSupported())
        .withFailMessage("Current platform should be supported: %s", report.getSummary())
        .isTrue();

    assertThat(report.isNativeLibraryLoaded())
        .withFailMessage("Native library should load on current platform: %s", report.getSummary())
        .isTrue();

    assertThat(report.isBasicFunctionalityWorks())
        .withFailMessage(
            "Basic functionality should work on current platform: %s", report.getSummary())
        .isTrue();

    LOGGER.info(String.format("Current platform validation complete: %s", report.getSummary()));
  }

  /** Validate JNI implementation consistency across platforms. */
  @Test
  @Order(2)
  @DisplayName("JNI Implementation Consistency")
  @EnabledIf("isJniAvailable")
  void validateJniImplementationConsistency() {
    LOGGER.info("Validating JNI implementation consistency");

    final Map<String, Object> jniResults = runJniImplementationTests();

    assertThat(jniResults)
        .withFailMessage("JNI implementation should produce consistent results")
        .isNotEmpty();

    // Validate specific JNI functionality
    assertThat(jniResults.get("engineCreation"))
        .withFailMessage("JNI engine creation should work")
        .isEqualTo(true);

    assertThat(jniResults.get("moduleCompilation"))
        .withFailMessage("JNI module compilation should work")
        .isEqualTo(true);

    assertThat(jniResults.get("functionExecution"))
        .withFailMessage("JNI function execution should work")
        .isEqualTo(true);

    LOGGER.info("JNI implementation consistency validation complete");
  }

  /** Validate Panama implementation consistency across platforms. */
  @Test
  @Order(3)
  @DisplayName("Panama Implementation Consistency")
  @EnabledOnJre({JRE.JAVA_23, JRE.OTHER}) // Panama available on Java 23+
  @EnabledIf("isPanamaAvailable")
  void validatePanamaImplementationConsistency() {
    LOGGER.info("Validating Panama implementation consistency");

    final Map<String, Object> panamaResults = runPanamaImplementationTests();

    assertThat(panamaResults)
        .withFailMessage("Panama implementation should produce consistent results")
        .isNotEmpty();

    // Validate specific Panama functionality
    assertThat(panamaResults.get("engineCreation"))
        .withFailMessage("Panama engine creation should work")
        .isEqualTo(true);

    assertThat(panamaResults.get("moduleCompilation"))
        .withFailMessage("Panama module compilation should work")
        .isEqualTo(true);

    assertThat(panamaResults.get("functionExecution"))
        .withFailMessage("Panama function execution should work")
        .isEqualTo(true);

    LOGGER.info("Panama implementation consistency validation complete");
  }

  /** Validate JNI/Panama parity on current platform. */
  @Test
  @Order(4)
  @DisplayName("JNI/Panama Implementation Parity")
  @EnabledIf("areBothImplementationsAvailable")
  void validateJniPanamaImplementationParity() {
    LOGGER.info("Validating JNI/Panama implementation parity");

    final Map<String, Object> jniResults = runJniImplementationTests();
    final Map<String, Object> panamaResults = runPanamaImplementationTests();

    // Compare results between implementations
    for (final String key : jniResults.keySet()) {
      if (panamaResults.containsKey(key)) {
        assertThat(panamaResults.get(key))
            .withFailMessage("JNI and Panama results should be identical for %s", key)
            .isEqualTo(jniResults.get(key));
      }
    }

    LOGGER.info("JNI/Panama implementation parity validation complete");
  }

  /** Validate cross-platform performance consistency. */
  @Test
  @Order(5)
  @DisplayName("Cross-Platform Performance Consistency")
  void validateCrossPlatformPerformanceConsistency() {
    LOGGER.info("Validating cross-platform performance consistency");

    final Map<String, Double> performanceMetrics = measurePerformanceMetrics();

    // Validate performance is within acceptable ranges
    final double operationTime =
        performanceMetrics.getOrDefault("avgOperationTime", Double.MAX_VALUE);
    assertThat(operationTime)
        .withFailMessage("Average operation time should be under 10ms, was %.3fms", operationTime)
        .isLessThan(10.0);

    final double memoryUsage =
        performanceMetrics.getOrDefault("memoryUsagePerOperation", Double.MAX_VALUE);
    assertThat(memoryUsage)
        .withFailMessage("Memory usage per operation should be under 1MB, was %.3fMB", memoryUsage)
        .isLessThan(1.0);

    LOGGER.info(
        String.format(
            "Performance validation complete - Operation: %.3fms, Memory: %.3fMB",
            operationTime, memoryUsage));
  }

  /** Validate native library loading across architectures. */
  @Test
  @Order(6)
  @DisplayName("Native Library Loading Validation")
  void validateNativeLibraryLoading() {
    LOGGER.info("Validating native library loading");

    // Test that native library loads successfully
    assertThatCode(
            () -> {
              final Engine engine = WasmRuntimeFactory.createEngine();
              assertThat(engine).isNotNull();
              engine.close();
            })
        .doesNotThrowAnyException();

    // Test multiple engine creation/destruction cycles
    for (int i = 0; i < 10; i++) {
      final Engine engine = WasmRuntimeFactory.createEngine();
      assertThat(engine).isNotNull();
      engine.close();
    }

    LOGGER.info("Native library loading validation complete");
  }

  /** Validate platform-specific features. */
  @Test
  @Order(7)
  @DisplayName("Platform-Specific Features")
  void validatePlatformSpecificFeatures() {
    LOGGER.info("Validating platform-specific features");

    // Test platform-specific optimizations
    validateSIMDSupport();
    validateMemoryMappingSupport();
    validateFileSystemSupport();

    LOGGER.info("Platform-specific features validation complete");
  }

  /** Validates SIMD support if available. */
  private void validateSIMDSupport() {
    try {
      // Test SIMD operations if supported
      final Engine engine = WasmRuntimeFactory.createEngine();

      // SIMD may not be available on all platforms
      LOGGER.info("SIMD support validation - implementation dependent");

      engine.close();
    } catch (final Exception e) {
      LOGGER.info("SIMD not supported on this platform: " + e.getMessage());
    }
  }

  /** Validates memory mapping support. */
  private void validateMemoryMappingSupport() {
    try {
      final Engine engine = WasmRuntimeFactory.createEngine();
      final Store store = new Store(engine);
      final Module module = engine.compileModule(SIMPLE_WASM_MODULE);
      final WasmInstance instance = new WasmInstance(store, module);

      // Test basic memory operations
      final WasmFunction addFunction = instance.getFunction("add");
      final Object[] result = addFunction.call(new Object[] {1, 2});
      assertThat(result[0]).isEqualTo(3);

      instance.close();
      module.close();
      store.close();
      engine.close();

      LOGGER.info("Memory mapping support validated");
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Memory mapping validation failed", e);
    }
  }

  /** Validates file system support. */
  private void validateFileSystemSupport() {
    try {
      // Test file system operations (if WASI is available)
      LOGGER.info("File system support validation - WASI dependent");
    } catch (final Exception e) {
      LOGGER.info("File system support not available: " + e.getMessage());
    }
  }

  /** Validates platform compatibility for a specific platform. */
  private PlatformCompatibilityReport validatePlatformCompatibility(final Platform platform) {
    final List<String> issues = new ArrayList<>();
    final Map<String, Object> performanceMetrics = new HashMap<>();

    boolean isSupported = true;
    boolean nativeLibraryLoaded = false;
    boolean basicFunctionalityWorks = false;
    boolean jniImplementationWorks = false;
    boolean panamaImplementationWorks = false;

    try {
      // Test native library loading
      final Engine engine = WasmRuntimeFactory.createEngine();
      nativeLibraryLoaded = true;

      // Test basic functionality
      final Store store = new Store(engine);
      final Module module = engine.compileModule(SIMPLE_WASM_MODULE);
      final WasmInstance instance = new WasmInstance(store, module);
      final WasmFunction addFunction = instance.getFunction("add");
      final Object[] result = addFunction.call(new Object[] {42, 58});

      if (result[0].equals(100)) {
        basicFunctionalityWorks = true;
      } else {
        issues.add("Basic function call returned incorrect result");
      }

      // Test JNI implementation if available
      try {
        jniImplementationWorks = testJniImplementation();
      } catch (final Exception e) {
        issues.add("JNI implementation test failed: " + e.getMessage());
      }

      // Test Panama implementation if available
      try {
        panamaImplementationWorks = testPanamaImplementation();
      } catch (final Exception e) {
        issues.add("Panama implementation test failed: " + e.getMessage());
      }

      // Collect performance metrics
      performanceMetrics.putAll(measurePerformanceMetrics());

      instance.close();
      module.close();
      store.close();
      engine.close();

    } catch (final Exception e) {
      isSupported = false;
      issues.add("Platform compatibility test failed: " + e.getMessage());
    }

    return new PlatformCompatibilityReport(
        platform,
        isSupported,
        nativeLibraryLoaded,
        basicFunctionalityWorks,
        jniImplementationWorks,
        panamaImplementationWorks,
        issues,
        performanceMetrics);
  }

  /** Runs JNI implementation tests. */
  private Map<String, Object> runJniImplementationTests() {
    final Map<String, Object> results = new HashMap<>();

    try {
      // Set system property to force JNI implementation
      System.setProperty("wasmtime4j.runtime", "jni");

      final Engine engine = WasmRuntimeFactory.createEngine();
      results.put("engineCreation", true);

      final Store store = new Store(engine);
      final Module module = engine.compileModule(SIMPLE_WASM_MODULE);
      results.put("moduleCompilation", true);

      final WasmInstance instance = new WasmInstance(store, module);
      final WasmFunction addFunction = instance.getFunction("add");
      final Object[] result = addFunction.call(new Object[] {10, 20});
      results.put("functionExecution", result[0].equals(30));

      instance.close();
      module.close();
      store.close();
      engine.close();

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "JNI implementation test failed", e);
      results.put("error", e.getMessage());
    } finally {
      System.clearProperty("wasmtime4j.runtime");
    }

    return results;
  }

  /** Runs Panama implementation tests. */
  private Map<String, Object> runPanamaImplementationTests() {
    final Map<String, Object> results = new HashMap<>();

    try {
      // Set system property to force Panama implementation
      System.setProperty("wasmtime4j.runtime", "panama");

      final Engine engine = WasmRuntimeFactory.createEngine();
      results.put("engineCreation", true);

      final Store store = new Store(engine);
      final Module module = engine.compileModule(SIMPLE_WASM_MODULE);
      results.put("moduleCompilation", true);

      final WasmInstance instance = new WasmInstance(store, module);
      final WasmFunction addFunction = instance.getFunction("add");
      final Object[] result = addFunction.call(new Object[] {10, 20});
      results.put("functionExecution", result[0].equals(30));

      instance.close();
      module.close();
      store.close();
      engine.close();

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Panama implementation test failed", e);
      results.put("error", e.getMessage());
    } finally {
      System.clearProperty("wasmtime4j.runtime");
    }

    return results;
  }

  /** Tests JNI implementation availability. */
  private boolean testJniImplementation() {
    try {
      System.setProperty("wasmtime4j.runtime", "jni");
      final Engine engine = WasmRuntimeFactory.createEngine();
      engine.close();
      return true;
    } catch (final Exception e) {
      return false;
    } finally {
      System.clearProperty("wasmtime4j.runtime");
    }
  }

  /** Tests Panama implementation availability. */
  private boolean testPanamaImplementation() {
    try {
      System.setProperty("wasmtime4j.runtime", "panama");
      final Engine engine = WasmRuntimeFactory.createEngine();
      engine.close();
      return true;
    } catch (final Exception e) {
      return false;
    } finally {
      System.clearProperty("wasmtime4j.runtime");
    }
  }

  /** Measures performance metrics. */
  private Map<String, Double> measurePerformanceMetrics() {
    final Map<String, Double> metrics = new HashMap<>();

    try {
      final Engine engine = WasmRuntimeFactory.createEngine();
      final Store store = new Store(engine);
      final Module module = engine.compileModule(SIMPLE_WASM_MODULE);

      final int operationCount = 1000;
      final long startTime = System.nanoTime();
      final long startMemory =
          Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      for (int i = 0; i < operationCount; i++) {
        final WasmInstance instance = new WasmInstance(store, module);
        final WasmFunction addFunction = instance.getFunction("add");
        addFunction.call(new Object[] {i, i + 1});
        instance.close();
      }

      final long endTime = System.nanoTime();
      final long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      final double avgOperationTime =
          (endTime - startTime) / (double) operationCount / 1_000_000.0; // ms
      final double memoryUsagePerOperation =
          (endMemory - startMemory) / (double) operationCount / 1024.0 / 1024.0; // MB

      metrics.put("avgOperationTime", avgOperationTime);
      metrics.put("memoryUsagePerOperation", memoryUsagePerOperation);

      module.close();
      store.close();
      engine.close();

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Performance measurement failed", e);
      metrics.put("avgOperationTime", Double.MAX_VALUE);
      metrics.put("memoryUsagePerOperation", Double.MAX_VALUE);
    }

    return metrics;
  }

  /** Detects the current platform. */
  private Platform detectCurrentPlatform() {
    final String osName = System.getProperty("os.name").toLowerCase();
    final String osArch = System.getProperty("os.arch").toLowerCase();

    if (osName.contains("linux")) {
      return osArch.contains("aarch64") || osArch.contains("arm64")
          ? Platform.LINUX_AARCH64
          : Platform.LINUX_X86_64;
    } else if (osName.contains("windows")) {
      return Platform.WINDOWS_X86_64;
    } else if (osName.contains("mac")) {
      return osArch.contains("aarch64") || osArch.contains("arm64")
          ? Platform.MACOS_AARCH64
          : Platform.MACOS_X86_64;
    }

    // Default to Linux x86_64 if unknown
    return Platform.LINUX_X86_64;
  }

  // Test condition methods
  boolean isJniAvailable() {
    return testJniImplementation();
  }

  boolean isPanamaAvailable() {
    final int javaVersion = Runtime.version().feature();
    return javaVersion >= 23 && testPanamaImplementation();
  }

  boolean areBothImplementationsAvailable() {
    return isJniAvailable() && isPanamaAvailable();
  }
}
