package ai.tegmentum.wasmtime4j.performance;

import java.util.Objects;
import java.util.Set;

/**
 * Target platform information for WebAssembly compilation.
 *
 * <p>This class provides details about the target platform that the WebAssembly module
 * was compiled for, including architecture, operating system, and CPU features.
 *
 * @since 1.0.0
 */
public final class Target {
  private final String architecture;
  private final String operatingSystem;
  private final String abi;
  private final Set<String> cpuFeatures;
  private final boolean is64Bit;

  /**
   * Creates a target platform record.
   *
   * @param architecture the target architecture (e.g., "x86_64", "aarch64")
   * @param operatingSystem the target OS (e.g., "linux", "windows", "macos")
   * @param abi the application binary interface (e.g., "gnu", "msvc")
   * @param cpuFeatures set of enabled CPU features
   * @param is64Bit whether the target is 64-bit
   */
  public Target(
      final String architecture,
      final String operatingSystem,
      final String abi,
      final Set<String> cpuFeatures,
      final boolean is64Bit) {
    this.architecture = Objects.requireNonNull(architecture, "architecture cannot be null");
    this.operatingSystem = Objects.requireNonNull(operatingSystem, "operatingSystem cannot be null");
    this.abi = Objects.requireNonNull(abi, "abi cannot be null");
    this.cpuFeatures = Set.copyOf(Objects.requireNonNull(cpuFeatures, "cpuFeatures cannot be null"));
    this.is64Bit = is64Bit;
  }

  /**
   * Gets the target architecture.
   *
   * @return architecture name
   */
  public String getArchitecture() {
    return architecture;
  }

  /**
   * Gets the target operating system.
   *
   * @return operating system name
   */
  public String getOperatingSystem() {
    return operatingSystem;
  }

  /**
   * Gets the application binary interface.
   *
   * @return ABI name
   */
  public String getAbi() {
    return abi;
  }

  /**
   * Gets the enabled CPU features.
   *
   * @return set of CPU features
   */
  public Set<String> getCpuFeatures() {
    return cpuFeatures;
  }

  /**
   * Checks if the target is 64-bit.
   *
   * @return true if 64-bit target
   */
  public boolean is64Bit() {
    return is64Bit;
  }

  /**
   * Checks if a specific CPU feature is enabled.
   *
   * @param feature the CPU feature name
   * @return true if the feature is enabled
   */
  public boolean hasCpuFeature(final String feature) {
    return cpuFeatures.contains(feature);
  }

  /**
   * Gets the target triple string (architecture-os-abi).
   *
   * @return target triple
   */
  public String getTargetTriple() {
    return String.format("%s-%s-%s", architecture, operatingSystem, abi);
  }

  /**
   * Checks if this is an x86-based architecture.
   *
   * @return true if x86 or x86_64
   */
  public boolean isX86() {
    return "x86".equals(architecture) || "x86_64".equals(architecture);
  }

  /**
   * Checks if this is an ARM-based architecture.
   *
   * @return true if ARM or AArch64
   */
  public boolean isArm() {
    return "arm".equals(architecture) || "aarch64".equals(architecture);
  }

  /**
   * Checks if advanced vector instructions are available.
   *
   * @return true if SIMD instructions are available
   */
  public boolean hasSimdSupport() {
    return hasCpuFeature("sse2") || hasCpuFeature("avx") || hasCpuFeature("neon");
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final Target target = (Target) obj;
    return is64Bit == target.is64Bit &&
        Objects.equals(architecture, target.architecture) &&
        Objects.equals(operatingSystem, target.operatingSystem) &&
        Objects.equals(abi, target.abi) &&
        Objects.equals(cpuFeatures, target.cpuFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(architecture, operatingSystem, abi, cpuFeatures, is64Bit);
  }

  @Override
  public String toString() {
    return String.format(
        "Target{architecture='%s', operatingSystem='%s', abi='%s', " +
        "cpuFeatures=%s, is64Bit=%s}",
        architecture, operatingSystem, abi, cpuFeatures, is64Bit);
  }
}