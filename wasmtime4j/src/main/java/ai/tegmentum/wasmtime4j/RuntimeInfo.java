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
package ai.tegmentum.wasmtime4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Information about the WebAssembly runtime implementation.
 *
 * <p>This class provides metadata about the runtime, including version information, implementation
 * type, and supported features.
 *
 * @since 1.0.0
 */
public final class RuntimeInfo {

  private static final Logger LOGGER = Logger.getLogger(RuntimeInfo.class.getName());
  private static final String VERSION_RESOURCE = "wasmtime4j-version.properties";
  private static final String WASMTIME_VERSION;
  private static final String WASMTIME4J_VERSION;

  static {
    String wasmtimeVer = "unknown";
    String wasmtime4jVer = "unknown";
    try (InputStream is =
        RuntimeInfo.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE)) {
      if (is != null) {
        final Properties props = new Properties();
        props.load(is);
        wasmtimeVer = props.getProperty("wasmtime.version", "unknown");
        wasmtime4jVer = props.getProperty("wasmtime4j.version", "unknown");
      } else {
        LOGGER.warning("Could not find " + VERSION_RESOURCE + " on classpath");
      }
    } catch (IOException e) {
      LOGGER.log(Level.WARNING, "Failed to load " + VERSION_RESOURCE, e);
    }
    WASMTIME_VERSION = wasmtimeVer;
    WASMTIME4J_VERSION = wasmtime4jVer;
  }

  /**
   * Gets the Wasmtime library version from build-time properties.
   *
   * @return the Wasmtime version string (e.g. "41.0.3"), or "unknown" if unavailable
   */
  public static String getWasmtimeLibraryVersion() {
    return WASMTIME_VERSION;
  }

  /**
   * Gets the wasmtime4j bindings version from build-time properties.
   *
   * @return the wasmtime4j version string (e.g. "1.0.0"), or "unknown" if unavailable
   */
  public static String getBindingsVersion() {
    return WASMTIME4J_VERSION;
  }

  private final String runtimeName;
  private final String runtimeVersion;
  private final String wasmtimeVersion;
  private final RuntimeType runtimeType;
  private final String javaVersion;
  private final String platformInfo;

  /**
   * Creates a new runtime information instance.
   *
   * @param runtimeName the name of the runtime implementation
   * @param runtimeVersion the version of the runtime implementation
   * @param wasmtimeVersion the version of the underlying Wasmtime library
   * @param runtimeType the type of runtime (JNI or Panama)
   * @param javaVersion the Java version being used
   * @param platformInfo information about the platform
   */
  public RuntimeInfo(
      final String runtimeName,
      final String runtimeVersion,
      final String wasmtimeVersion,
      final RuntimeType runtimeType,
      final String javaVersion,
      final String platformInfo) {
    this.runtimeName = runtimeName;
    this.runtimeVersion = runtimeVersion;
    this.wasmtimeVersion = wasmtimeVersion;
    this.runtimeType = runtimeType;
    this.javaVersion = javaVersion;
    this.platformInfo = platformInfo;
  }

  /**
   * Gets the name of the runtime implementation.
   *
   * @return the runtime name
   */
  public String getRuntimeName() {
    return runtimeName;
  }

  /**
   * Gets the version of the runtime implementation.
   *
   * @return the runtime version
   */
  public String getRuntimeVersion() {
    return runtimeVersion;
  }

  /**
   * Gets the version of the underlying Wasmtime library.
   *
   * @return the Wasmtime version
   */
  public String getWasmtimeVersion() {
    return wasmtimeVersion;
  }

  /**
   * Gets the type of runtime implementation.
   *
   * @return the runtime type
   */
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Gets the Java version being used.
   *
   * @return the Java version
   */
  public String getJavaVersion() {
    return javaVersion;
  }

  /**
   * Gets information about the platform.
   *
   * @return the platform information
   */
  public String getPlatformInfo() {
    return platformInfo;
  }

  @Override
  public String toString() {
    return String.format(
        "RuntimeInfo{name='%s', version='%s', wasmtime='%s', type=%s, java='%s', platform='%s'}",
        runtimeName, runtimeVersion, wasmtimeVersion, runtimeType, javaVersion, platformInfo);
  }
}
