/*
 * Copyright 2024 Tegmentum AI
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

/**
 * Backward-compatible wrapper for platform detection utilities.
 *
 * <p>This class provides the same API as the original PlatformDetector but delegates to the
 * refactored implementation in the wasmtime4j-native-loader module. This maintains backward
 * compatibility for existing code while enabling the new configurable functionality.
 *
 * @deprecated This class is maintained for backward compatibility. New code should use {@link
 *     ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector} directly.
 */
@Deprecated
public final class PlatformDetector {

  /** Cached platform info instance. */
  private static volatile PlatformInfo cachedPlatformInfo;

  /** Private constructor to prevent instantiation of utility class. */
  private PlatformDetector() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /** Operating system enumeration. */
  public enum OperatingSystem {
    LINUX("linux", "lib", ".so"),
    WINDOWS("windows", "", ".dll"),
    MACOS("darwin", "lib", ".dylib");

    private final String name;
    private final String libraryPrefix;
    private final String libraryExtension;

    OperatingSystem(final String name, final String libraryPrefix, final String libraryExtension) {
      this.name = name;
      this.libraryPrefix = libraryPrefix;
      this.libraryExtension = libraryExtension;
    }

    public String getName() {
      return name;
    }

    public String getLibraryPrefix() {
      return libraryPrefix;
    }

    public String getLibraryExtension() {
      return libraryExtension;
    }
  }

  /** Architecture enumeration. */
  public enum Architecture {
    X86_64("x86_64"),
    AARCH64("aarch64");

    private final String name;

    Architecture(final String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  /** Platform information wrapper. */
  public static final class PlatformInfo {
    private final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.PlatformInfo delegate;

    PlatformInfo(
        final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.PlatformInfo delegate) {
      this.delegate = delegate;
    }

    /**
     * Gets the platform ID.
     *
     * @return the platform ID
     */
    public String getPlatformId() {
      return delegate.getPlatformId();
    }

    /**
     * Gets the operating system.
     *
     * @return the operating system
     */
    public OperatingSystem getOperatingSystem() {
      final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.OperatingSystem delegateOs =
          delegate.getOperatingSystem();
      switch (delegateOs) {
        case LINUX:
          return OperatingSystem.LINUX;
        case WINDOWS:
          return OperatingSystem.WINDOWS;
        case MACOS:
          return OperatingSystem.MACOS;
        default:
          throw new IllegalStateException("Unknown operating system: " + delegateOs);
      }
    }

    /**
     * Gets the architecture.
     *
     * @return the architecture
     */
    public Architecture getArchitecture() {
      final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.Architecture delegateArch =
          delegate.getArchitecture();
      switch (delegateArch) {
        case X86_64:
          return Architecture.X86_64;
        case AARCH64:
          return Architecture.AARCH64;
        default:
          throw new IllegalStateException("Unknown architecture: " + delegateArch);
      }
    }

    /**
     * Gets the library file name for the given library.
     *
     * @param libraryName the library name
     * @return the library file name
     */
    public String getLibraryFileName(final String libraryName) {
      return delegate.getLibraryFileName(libraryName);
    }

    /**
     * Gets the library resource path for the given library.
     *
     * @param libraryName the library name
     * @return the library resource path
     */
    public String getLibraryResourcePath(final String libraryName) {
      return delegate.getLibraryResourcePath(libraryName);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final PlatformInfo other = (PlatformInfo) obj;
      return delegate.equals(other.delegate);
    }

    @Override
    public int hashCode() {
      return delegate.hashCode();
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  /**
   * Detects the current platform.
   *
   * @return platform information
   */
  public static PlatformInfo detect() {
    PlatformInfo result = cachedPlatformInfo;
    if (result == null) {
      synchronized (PlatformDetector.class) {
        result = cachedPlatformInfo;
        if (result == null) {
          cachedPlatformInfo =
              result =
                  new PlatformInfo(ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.detect());
        }
      }
    }
    return result;
  }

  /**
   * Gets platform description.
   *
   * @return platform description
   */
  public static String getPlatformDescription() {
    return ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.getPlatformDescription();
  }

  /**
   * Checks if the platform is supported.
   *
   * @return true if platform is supported
   */
  public static boolean isPlatformSupported() {
    return ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.isPlatformSupported();
  }

  /**
   * Detects the operating system.
   *
   * @return the operating system
   */
  public static OperatingSystem detectOperatingSystem() {
    final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.OperatingSystem delegateOs =
        ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.detectOperatingSystem();
    switch (delegateOs) {
      case LINUX:
        return OperatingSystem.LINUX;
      case WINDOWS:
        return OperatingSystem.WINDOWS;
      case MACOS:
        return OperatingSystem.MACOS;
      default:
        throw new IllegalStateException("Unknown operating system: " + delegateOs);
    }
  }

  /**
   * Detects the architecture.
   *
   * @return the architecture
   */
  public static Architecture detectArchitecture() {
    final ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.Architecture delegateArch =
        ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector.detectArchitecture();
    switch (delegateArch) {
      case X86_64:
        return Architecture.X86_64;
      case AARCH64:
        return Architecture.AARCH64;
      default:
        throw new IllegalStateException("Unknown architecture: " + delegateArch);
    }
  }
}
