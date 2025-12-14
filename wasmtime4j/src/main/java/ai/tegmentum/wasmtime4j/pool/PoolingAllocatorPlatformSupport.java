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

package ai.tegmentum.wasmtime4j.pool;

import java.util.Locale;

/**
 * Platform support utilities for pooling allocator features.
 *
 * <p>This class provides methods to check for platform-specific features that may be
 * available for the pooling allocator, such as memory protection keys (MPK) and
 * PAGEMAP_SCAN support.
 *
 * <p>Example usage:
 * <pre>{@code
 * if (PoolingAllocatorPlatformSupport.areMemoryProtectionKeysAvailable()) {
 *     // Configure allocator with MPK support
 *     PoolingAllocatorConfig config = PoolingAllocatorConfig.builder()
 *         .memoryProtectionKeysEnabled(true)
 *         .maxMemoryProtectionKeys(15)
 *         .build();
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class PoolingAllocatorPlatformSupport {

  private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  private static final String OS_ARCH = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

  private PoolingAllocatorPlatformSupport() {
    // Utility class
  }

  /**
   * Checks if memory protection keys (MPK) are potentially available on this platform.
   *
   * <p>Memory protection keys are a hardware feature available on Intel x86-64 processors
   * (Skylake and later) running Linux. This method checks if the platform is compatible,
   * but does not guarantee that MPK is actually available or enabled on this specific system.
   *
   * <p>MPK provides hardware-assisted memory isolation between WebAssembly instances,
   * offering additional security and potential performance benefits for the pooling allocator.
   *
   * @return true if MPK may be available on this platform
   */
  public static boolean areMemoryProtectionKeysAvailable() {
    // MPK is only available on Linux x86-64
    return isLinux() && isX86_64();
  }

  /**
   * Checks if PAGEMAP_SCAN ioctl is potentially available on this platform.
   *
   * <p>PAGEMAP_SCAN is a Linux kernel feature introduced in Linux 6.7 that enables
   * more efficient memory tracking. This method checks if the platform is compatible,
   * but does not guarantee that the feature is available on this specific kernel version.
   *
   * <p>When available, PAGEMAP_SCAN can improve the performance of memory decommit
   * operations in the pooling allocator.
   *
   * @return true if PAGEMAP_SCAN may be available on this platform
   */
  public static boolean isPagemapScanAvailable() {
    // PAGEMAP_SCAN is only available on Linux
    // Note: This only checks platform compatibility, not actual kernel version
    return isLinux();
  }

  /**
   * Checks if the current operating system is Linux.
   *
   * @return true if running on Linux
   */
  public static boolean isLinux() {
    return OS_NAME.contains("linux");
  }

  /**
   * Checks if the current operating system is Windows.
   *
   * @return true if running on Windows
   */
  public static boolean isWindows() {
    return OS_NAME.contains("windows");
  }

  /**
   * Checks if the current operating system is macOS.
   *
   * @return true if running on macOS
   */
  public static boolean isMacOS() {
    return OS_NAME.contains("mac") || OS_NAME.contains("darwin");
  }

  /**
   * Checks if the current architecture is x86-64 (AMD64).
   *
   * @return true if running on x86-64
   */
  public static boolean isX86_64() {
    return OS_ARCH.equals("amd64") || OS_ARCH.equals("x86_64");
  }

  /**
   * Checks if the current architecture is ARM64 (AArch64).
   *
   * @return true if running on ARM64
   */
  public static boolean isArm64() {
    return OS_ARCH.equals("aarch64") || OS_ARCH.equals("arm64");
  }

  /**
   * Gets a description of the current platform.
   *
   * @return a string describing the current platform (OS and architecture)
   */
  public static String getPlatformDescription() {
    return OS_NAME + "/" + OS_ARCH;
  }

  /**
   * Gets the maximum number of memory protection keys typically available.
   *
   * <p>On x86-64 processors with MPK support, there are typically 16 protection keys
   * available (0-15), with key 0 reserved by the system.
   *
   * @return the maximum number of MPK keys, or 0 if MPK is not available
   */
  public static int getMaxMemoryProtectionKeysHint() {
    if (areMemoryProtectionKeysAvailable()) {
      // x86-64 typically supports 16 keys (0-15), with key 0 reserved
      return 15;
    }
    return 0;
  }
}
