package ai.tegmentum.wasmtime4j.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

/** Utility class providing common functionality for Wasmtime4j tests. */
public final class TestUtils {
  private static final Logger LOGGER = Logger.getLogger(TestUtils.class.getName());

  private static final String RESOURCES_PATH = "wasmtime4j.test.resources";
  private static final String DEFAULT_TIMEOUT = "wasmtime4j.test.timeout";
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private TestUtils() {
    // Utility class - prevent instantiation
  }

  /**
   * Gets the test resources directory path.
   *
   * @return the test resources directory path
   */
  public static Path getTestResourcesPath() {
    final String resourcesPath = System.getProperty(RESOURCES_PATH);
    if (resourcesPath != null) {
      return Paths.get(resourcesPath);
    }
    return Paths.get("src", "test", "resources");
  }

  /**
   * Gets the WebAssembly test files directory path.
   *
   * @return the WebAssembly test files directory path
   */
  public static Path getWasmTestFilesPath() {
    return getTestResourcesPath().resolve("wasm");
  }

  /**
   * Loads a WebAssembly module from the test resources.
   *
   * @param fileName the WebAssembly file name
   * @return the WebAssembly module bytes
   * @throws IOException if the file cannot be read
   */
  public static byte[] loadWasmModule(final String fileName) throws IOException {
    final Path wasmFile = getWasmTestFilesPath().resolve(fileName);
    if (!Files.exists(wasmFile)) {
      throw new IOException("WebAssembly test file not found: " + wasmFile);
    }

    LOGGER.info("Loading WebAssembly module: " + wasmFile);
    return Files.readAllBytes(wasmFile);
  }

  /**
   * Gets the test timeout in seconds.
   *
   * @return the test timeout in seconds
   */
  public static int getTestTimeoutSeconds() {
    final String timeoutStr = System.getProperty(DEFAULT_TIMEOUT);
    if (timeoutStr != null) {
      try {
        return Integer.parseInt(timeoutStr);
      } catch (final NumberFormatException e) {
        LOGGER.warning(
            "Invalid timeout value: " + timeoutStr + ", using default: " + DEFAULT_TIMEOUT_SECONDS);
      }
    }
    return DEFAULT_TIMEOUT_SECONDS;
  }

  /**
   * Checks if a specific test category is enabled.
   *
   * @param category the test category (e.g., "wasm.suite", "native", "platform")
   * @return true if the test category is enabled
   */
  public static boolean isTestCategoryEnabled(final String category) {
    final String property = "wasmtime4j.test." + category + ".enabled";
    return Boolean.parseBoolean(System.getProperty(property, "false"));
  }

  /**
   * Gets the current operating system name.
   *
   * @return the operating system name
   */
  public static String getOperatingSystem() {
    return System.getProperty("os.name").toLowerCase();
  }

  /**
   * Gets the current system architecture.
   *
   * @return the system architecture
   */
  public static String getSystemArchitecture() {
    return System.getProperty("os.arch").toLowerCase();
  }

  /**
   * Checks if the current platform is Linux.
   *
   * @return true if running on Linux
   */
  public static boolean isLinux() {
    return getOperatingSystem().contains("linux");
  }

  /**
   * Checks if the current platform is Windows.
   *
   * @return true if running on Windows
   */
  public static boolean isWindows() {
    return getOperatingSystem().contains("windows");
  }

  /**
   * Checks if the current platform is macOS.
   *
   * @return true if running on macOS
   */
  public static boolean isMacOS() {
    return getOperatingSystem().contains("mac") || getOperatingSystem().contains("darwin");
  }

  /**
   * Checks if the current architecture is x86_64.
   *
   * @return true if running on x86_64
   */
  public static boolean isX86_64() {
    final String arch = getSystemArchitecture();
    return arch.contains("amd64") || arch.contains("x86_64");
  }

  /**
   * Checks if the current architecture is ARM64.
   *
   * @return true if running on ARM64
   */
  public static boolean isARM64() {
    final String arch = getSystemArchitecture();
    return arch.contains("aarch64") || arch.contains("arm64");
  }

  /**
   * Gets the Java version.
   *
   * @return the Java version
   */
  public static int getJavaVersion() {
    final String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      return Integer.parseInt(version.substring(2, 3));
    } else {
      final int dot = version.indexOf(".");
      if (dot != -1) {
        return Integer.parseInt(version.substring(0, dot));
      } else {
        return Integer.parseInt(version);
      }
    }
  }

  /**
   * Checks if Panama FFI is available on the current Java version.
   *
   * @return true if Panama FFI is available
   */
  public static boolean isPanamaAvailable() {
    return getJavaVersion() >= 23;
  }

  /**
   * Creates a simple WebAssembly module for testing. This creates a minimal WAT (WebAssembly Text
   * format) module.
   *
   * @return WebAssembly module bytes for a simple add function
   */
  public static byte[] createSimpleWasmModule() {
    // This is a simple WebAssembly module in binary format that exports an "add" function
    // (module
    //   (func $add (param $lhs i32) (param $rhs i32) (result i32)
    //     local.get $lhs
    //     local.get $rhs
    //     i32.add)
    //   (export "add" (func $add))
    // )
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic number
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
  }

  /**
   * Creates a WebAssembly module that imports memory.
   *
   * @return WebAssembly module bytes for a module that imports memory
   */
  public static byte[] createMemoryImportWasmModule() {
    // This is a WebAssembly module that imports memory
    // (module
    //   (import "env" "memory" (memory 1))
    //   (func (export "load") (param i32) (result i32)
    //     local.get 0
    //     i32.load)
    // )
    return new byte[] {
      0x00,
      0x61,
      0x73,
      0x6d, // magic number
      0x01,
      0x00,
      0x00,
      0x00, // version
      0x01,
      0x06,
      0x01,
      0x60,
      0x01,
      0x7f,
      0x01,
      0x7f, // type section
      0x02,
      0x0c,
      0x01,
      0x03,
      0x65,
      0x6e,
      0x76,
      0x06,
      0x6d,
      0x65,
      0x6d,
      0x6f,
      0x72,
      0x79,
      0x02,
      0x00,
      0x01, // import section
      0x03,
      0x02,
      0x01,
      0x00, // function section
      0x07,
      0x08,
      0x01,
      0x04,
      0x6c,
      0x6f,
      0x61,
      0x64,
      0x00,
      0x00, // export section
      0x0a,
      0x07,
      0x01,
      0x05,
      0x00,
      0x20,
      0x00,
      0x28,
      0x02,
      0x00,
      0x0b // code section
    };
  }

  /**
   * Loads system properties from a properties file.
   *
   * @param propertiesFile the properties file path
   * @return the loaded properties
   * @throws IOException if the properties file cannot be read
   */
  public static Properties loadProperties(final String propertiesFile) throws IOException {
    final Properties properties = new Properties();
    try (final InputStream input =
        TestUtils.class.getClassLoader().getResourceAsStream(propertiesFile)) {
      if (input == null) {
        throw new IOException("Properties file not found: " + propertiesFile);
      }
      properties.load(input);
    }
    return properties;
  }
}
