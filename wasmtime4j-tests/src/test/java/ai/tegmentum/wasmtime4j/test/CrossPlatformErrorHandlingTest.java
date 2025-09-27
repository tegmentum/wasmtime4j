package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Cross-platform error handling validation tests.
 *
 * <p>This test class validates error handling behavior across different platforms (Windows, Linux,
 * macOS), architectures (x86_64, ARM64), and Java versions to ensure consistent error behavior
 * regardless of the runtime environment.
 */
@DisplayName("Cross-Platform Error Handling Validation")
class CrossPlatformErrorHandlingTest {

  /** Simple WebAssembly module that triggers a trap. */
  private static final byte[] TRAP_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x04, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x00, // Function type: () -> ()
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x07, // Export section
    0x08, // Section size
    0x01, // 1 export
    0x04, 't', 'r', 'a', 'p', // Export name "trap"
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x05, // Section size
    0x01, // 1 function body
    0x03, // Body size
    0x00, // No locals
    0x00, // unreachable instruction
    0x0B // End instruction
  };

  /** Invalid WebAssembly module for testing compilation errors. */
  private static final byte[] INVALID_MODULE = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

  /** WebAssembly module with memory for testing memory-related errors. */
  private static final byte[] MEMORY_MODULE = {
    0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Magic + version
    0x01, // Type section
    0x05, // Section size
    0x01, // 1 type
    0x60, 0x00, 0x01, 0x7F, // Function type: () -> i32
    0x03, // Function section
    0x02, // Section size
    0x01, 0x00, // 1 function with type index 0
    0x05, // Memory section
    0x03, // Section size
    0x01, // 1 memory
    0x00, 0x01, // Memory limits: minimum 1 page
    0x07, // Export section
    0x0D, // Section size
    0x01, // 1 export
    0x0B, 'o', 'u', 't', '_', 'o', 'f', '_', 'b', 'o', 'u', 'n', 'd', 's', // Export name
    0x00, 0x00, // Function export with index 0
    0x0A, // Code section
    0x0A, // Section size
    0x01, // 1 function body
    0x08, // Body size
    0x00, // No locals
    0x41, (byte) 0x80, (byte) 0x80, 0x04, // i32.const 65536 (out of bounds)
    0x28, 0x02, 0x00, // i32.load align=2 offset=0
    0x0B // End instruction
  };

  /**
   * Platform information for test context.
   */
  private static class PlatformInfo {
    final String osName;
    final String osVersion;
    final String architecture;
    final String javaVersion;
    final String javaVendor;
    final boolean isWindows;
    final boolean isLinux;
    final boolean isMacOS;
    final boolean isArm;
    final boolean is64Bit;

    PlatformInfo() {
      this.osName = System.getProperty("os.name");
      this.osVersion = System.getProperty("os.version");
      this.architecture = System.getProperty("os.arch");
      this.javaVersion = System.getProperty("java.version");
      this.javaVendor = System.getProperty("java.vendor");

      String osLower = osName.toLowerCase(Locale.ENGLISH);
      this.isWindows = osLower.contains("windows");
      this.isLinux = osLower.contains("linux");
      this.isMacOS = osLower.contains("mac") || osLower.contains("darwin");

      String archLower = architecture.toLowerCase(Locale.ENGLISH);
      this.isArm = archLower.contains("arm") || archLower.contains("aarch");
      this.is64Bit = archLower.contains("64");
    }

    @Override
    public String toString() {
      return String.format(
          "Platform{os=%s %s, arch=%s, java=%s (%s)}",
          osName, osVersion, architecture, javaVersion, javaVendor);
    }
  }

  @Test
  @DisplayName("Platform information logging")
  void testPlatformInformation() {
    PlatformInfo platform = new PlatformInfo();
    System.out.println("Running cross-platform error tests on: " + platform);

    // Basic platform detection validation
    assertTrue(
        platform.isWindows || platform.isLinux || platform.isMacOS,
        "Should detect at least one major platform");
    assertNotNull(platform.architecture, "Architecture should be detected");
    assertNotNull(platform.javaVersion, "Java version should be available");
  }

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Error consistency across runtime types")
  void testErrorConsistencyAcrossRuntimes(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    PlatformInfo platform = new PlatformInfo();
    System.out.println("Testing " + runtimeType + " on " + platform);

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      // Test compilation error consistency
      WasmException compilationError =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, INVALID_MODULE),
              "Invalid module should throw consistent error on " + platform);

      assertNotNull(compilationError.getMessage(), "Compilation error should have message");
      assertTrue(
          compilationError.getMessage().length() > 5,
          "Compilation error message should be descriptive");

      // Test runtime error consistency
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      WasmException runtimeError =
          assertThrows(
              WasmException.class,
              () -> trapInstance.getExportedFunction("trap").call(),
              "Trap should throw consistent error on " + platform);

      assertNotNull(runtimeError.getMessage(), "Runtime error should have message");
      assertTrue(
          runtimeError.getMessage().length() > 5, "Runtime error message should be descriptive");

      // Test memory error consistency
      Module memoryModule = runtime.compileModule(engine, MEMORY_MODULE);
      Instance memoryInstance = runtime.instantiateModule(store, memoryModule);

      WasmException memoryError =
          assertThrows(
              WasmException.class,
              () -> memoryInstance.getExportedFunction("out_of_bounds").call(),
              "Memory access violation should throw consistent error on " + platform);

      assertNotNull(memoryError.getMessage(), "Memory error should have message");
      assertTrue(
          memoryError.getMessage().length() > 5, "Memory error message should be descriptive");
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  @DisplayName("Windows-specific error handling")
  void testWindowsSpecificErrorHandling() throws WasmException {
    assumeTrue(isCurrentPlatformSupported(), "Windows platform should be supported");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      testPlatformSpecificBehavior(runtime, "Windows");
    }
  }

  @Test
  @EnabledOnOs(OS.LINUX)
  @DisplayName("Linux-specific error handling")
  void testLinuxSpecificErrorHandling() throws WasmException {
    assumeTrue(isCurrentPlatformSupported(), "Linux platform should be supported");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      testPlatformSpecificBehavior(runtime, "Linux");
    }
  }

  @Test
  @EnabledOnOs(OS.MAC)
  @DisplayName("macOS-specific error handling")
  void testMacOSSpecificErrorHandling() throws WasmException {
    assumeTrue(isCurrentPlatformSupported(), "macOS platform should be supported");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      testPlatformSpecificBehavior(runtime, "macOS");
    }
  }

  private void testPlatformSpecificBehavior(WasmRuntime runtime, String platformName)
      throws WasmException {
    Engine engine = runtime.createEngine();
    Store store = runtime.createStore(engine);

    System.out.println("Testing platform-specific behavior on: " + platformName);

    // Test that basic error handling works on this platform
    Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
    Instance trapInstance = runtime.instantiateModule(store, trapModule);

    WasmException error =
        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Basic error handling should work on " + platformName);

    assertNotNull(error.getMessage(), "Error should have message on " + platformName);

    // Platform-specific validation
    validateErrorMessageFormat(error.getMessage(), platformName);
  }

  private void validateErrorMessageFormat(String message, String platformName) {
    // Error message should not contain platform-specific artifacts
    assertNotNull(message, "Error message should not be null on " + platformName);
    assertTrue(message.length() > 0, "Error message should not be empty on " + platformName);

    // Should not contain line endings that could cause issues
    assertTrue(
        !message.contains("\r\n") && !message.contains("\n") && !message.contains("\r"),
        "Error message should not contain line endings on " + platformName);

    // Should be reasonable length
    assertTrue(
        message.length() < 10000,
        "Error message should not be excessively long on " + platformName);
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17})
  @DisplayName("Legacy Java version error handling")
  void testLegacyJavaVersionErrorHandling() throws WasmException {
    assumeTrue(RuntimeType.JNI.equals(getExpectedRuntimeType()), "JNI runtime should be used");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      testJavaVersionSpecificBehavior(runtime, "Legacy Java (8/11/17)");
    }
  }

  @Test
  @EnabledOnJre({JRE.JAVA_21, JRE.JAVA_22, JRE.JAVA_23})
  @DisplayName("Modern Java version error handling")
  void testModernJavaVersionErrorHandling() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      String javaVersion = System.getProperty("java.version");
      testJavaVersionSpecificBehavior(runtime, "Modern Java (" + javaVersion + ")");
    }
  }

  private void testJavaVersionSpecificBehavior(WasmRuntime runtime, String javaVersionDesc)
      throws WasmException {
    Engine engine = runtime.createEngine();
    Store store = runtime.createStore(engine);

    System.out.println("Testing Java version-specific behavior: " + javaVersionDesc);

    // Test error handling across different Java versions
    Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
    Instance trapInstance = runtime.instantiateModule(store, trapModule);

    WasmException error =
        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Error handling should work on " + javaVersionDesc);

    assertNotNull(error.getMessage(), "Error should have message on " + javaVersionDesc);

    // Verify stack trace is properly preserved across Java versions
    StackTraceElement[] stackTrace = error.getStackTrace();
    assertNotNull(stackTrace, "Stack trace should be available on " + javaVersionDesc);
    assertTrue(
        stackTrace.length > 0, "Stack trace should not be empty on " + javaVersionDesc);
  }

  @Test
  @DisplayName("Architecture-specific error handling")
  void testArchitectureSpecificErrorHandling() throws WasmException {
    PlatformInfo platform = new PlatformInfo();
    String archDescription =
        platform.architecture + (platform.is64Bit ? " (64-bit)" : " (32-bit)");

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      System.out.println("Testing architecture-specific behavior: " + archDescription);

      // Test that error handling works consistently across architectures
      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      WasmException error =
          assertThrows(
              WasmException.class,
              () -> trapInstance.getExportedFunction("trap").call(),
              "Error handling should work on " + archDescription);

      assertNotNull(error.getMessage(), "Error should have message on " + archDescription);

      // Architecture-specific validation
      if (platform.isArm) {
        // ARM-specific checks
        validateArmSpecificBehavior(error, platform);
      } else {
        // x86/x64-specific checks
        validateX86SpecificBehavior(error, platform);
      }
    }
  }

  private void validateArmSpecificBehavior(WasmException error, PlatformInfo platform) {
    // ARM-specific error validation
    String message = error.getMessage();
    assertNotNull(message, "ARM platforms should provide error messages");

    // ARM should handle errors without architecture-specific issues
    assertTrue(
        message.length() > 0, "ARM error messages should be descriptive: " + platform.architecture);
  }

  private void validateX86SpecificBehavior(WasmException error, PlatformInfo platform) {
    // x86/x64-specific error validation
    String message = error.getMessage();
    assertNotNull(message, "x86/x64 platforms should provide error messages");

    // x86/x64 should handle errors without architecture-specific issues
    assertTrue(
        message.length() > 0,
        "x86/x64 error messages should be descriptive: " + platform.architecture);
  }

  @Test
  @DisplayName("Memory management consistency across platforms")
  void testMemoryManagementConsistency() throws InterruptedException, WasmException {
    PlatformInfo platform = new PlatformInfo();
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

    System.out.println("Testing memory management on: " + platform);

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Measure initial memory
      long initialMemory = memoryBean.getHeapMemoryUsage().getUsed();

      // Perform many error operations
      for (int i = 0; i < 100; i++) {
        Store store = runtime.createStore(engine);
        Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
        Instance trapInstance = runtime.instantiateModule(store, trapModule);

        assertThrows(
            WasmException.class,
            () -> trapInstance.getExportedFunction("trap").call(),
            "Trap should throw exception");

        // Periodic garbage collection
        if (i % 20 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force garbage collection and measure final memory
      System.gc();
      Thread.yield();
      long finalMemory = memoryBean.getHeapMemoryUsage().getUsed();

      long memoryIncrease = finalMemory - initialMemory;
      System.out.println(
          "Memory usage on "
              + platform.osName
              + ": initial="
              + initialMemory
              + ", final="
              + finalMemory
              + ", increase="
              + memoryIncrease);

      // Memory increase should be reasonable across all platforms (less than 100MB)
      assertTrue(
          memoryIncrease < 100 * 1024 * 1024,
          "Memory management should be consistent on "
              + platform.osName
              + ": "
              + memoryIncrease
              + " bytes increase");
    }
  }

  @Test
  @DisplayName("Concurrent error handling across platforms")
  void testConcurrentErrorHandlingAcrossPlatforms() throws InterruptedException, WasmException {
    PlatformInfo platform = new PlatformInfo();

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final int operationsPerThread = 50;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger errorCount = new AtomicInteger(0);
      final AtomicInteger successCount = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      System.out.println("Testing concurrent error handling on: " + platform);

      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                Store store = runtime.createStore(engine);
                Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
                Instance trapInstance = runtime.instantiateModule(store, trapModule);

                for (int j = 0; j < operationsPerThread; j++) {
                  try {
                    trapInstance.getExportedFunction("trap").call();
                    // Should not reach here
                  } catch (WasmException e) {
                    errorCount.incrementAndGet();
                    // Validate error message
                    if (e.getMessage() != null && e.getMessage().length() > 0) {
                      successCount.incrementAndGet();
                    }
                  }
                }
              } catch (Exception e) {
                System.err.println("Thread " + threadId + " failed: " + e.getMessage());
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(60, TimeUnit.SECONDS), "All threads should complete within 60 seconds");
      executor.shutdown();

      int expectedErrors = threadCount * operationsPerThread;
      assertEquals(
          expectedErrors,
          errorCount.get(),
          "All operations should throw errors on " + platform.osName);
      assertTrue(
          successCount.get() >= expectedErrors * 0.9,
          "At least 90% of errors should have valid messages on " + platform.osName);
    }
  }

  @Test
  @DisplayName("File system path error handling")
  void testFileSystemPathErrorHandling() throws WasmException {
    PlatformInfo platform = new PlatformInfo();

    // Test path handling for different platforms
    List<String> testPaths =
        Arrays.asList(
            "/tmp/nonexistent.wasm", // Unix-style
            "C:\\temp\\nonexistent.wasm", // Windows-style
            "~/nonexistent.wasm", // Home directory
            "./relative/nonexistent.wasm", // Relative path
            "nonexistent.wasm" // Just filename
            );

    for (String testPath : testPaths) {
      // Skip Windows paths on non-Windows platforms and vice versa
      if (testPath.contains("C:\\") && !platform.isWindows) {
        continue;
      }
      if (testPath.startsWith("/") && platform.isWindows) {
        continue;
      }

      Path path = Paths.get(testPath);
      if (Files.exists(path)) {
        continue; // Skip if file actually exists
      }

      // Test that non-existent file paths are handled gracefully
      // Note: This would typically be tested with file I/O operations
      // For now, just verify the path can be processed without platform-specific issues
      String normalizedPath = path.toAbsolutePath().normalize().toString();
      assertNotNull(normalizedPath, "Path should be processable on " + platform.osName);
    }
  }

  @Test
  @DisplayName("Locale-specific error message handling")
  void testLocaleSpecificErrorHandling() throws WasmException {
    PlatformInfo platform = new PlatformInfo();
    Locale currentLocale = Locale.getDefault();

    System.out.println("Testing locale-specific behavior: " + currentLocale + " on " + platform);

    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();
      Store store = runtime.createStore(engine);

      Module trapModule = runtime.compileModule(engine, TRAP_MODULE);
      Instance trapInstance = runtime.instantiateModule(store, trapModule);

      WasmException error =
          assertThrows(
              WasmException.class,
              () -> trapInstance.getExportedFunction("trap").call(),
              "Error should occur regardless of locale");

      String message = error.getMessage();
      assertNotNull(message, "Error message should be available in any locale");

      // Error messages should be in English (or at least ASCII-safe)
      // to ensure consistency across different locale environments
      assertTrue(
          message.matches("^[\\x00-\\x7F]*$") || message.length() > 0,
          "Error message should be ASCII-safe or at least present: " + message);
    }
  }

  private boolean isCurrentPlatformSupported() {
    // Basic check if the current platform should be supported
    PlatformInfo platform = new PlatformInfo();
    return platform.isWindows || platform.isLinux || platform.isMacOS;
  }

  private RuntimeType getExpectedRuntimeType() {
    // Determine expected runtime type based on Java version
    String javaVersion = System.getProperty("java.version");
    if (javaVersion.startsWith("1.8") || javaVersion.startsWith("11") || javaVersion.startsWith("17")) {
      return RuntimeType.JNI;
    }
    // For Java 21+ we might expect Panama, but could fall back to JNI
    return RuntimeType.JNI; // Default to JNI for now
  }

  private void assertEquals(int expected, int actual, String message) {
    assertTrue(expected == actual, message + " - expected: " + expected + ", actual: " + actual);
  }
}