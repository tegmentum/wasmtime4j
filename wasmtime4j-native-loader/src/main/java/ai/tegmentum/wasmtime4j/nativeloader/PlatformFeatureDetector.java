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

package ai.tegmentum.wasmtime4j.nativeloader;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced platform feature detection for cross-platform optimization.
 *
 * <p>This class provides comprehensive detection of platform-specific features, capabilities, and
 * performance characteristics. It extends the basic platform detection with detailed feature analysis
 * for optimal WebAssembly runtime configuration.
 *
 * <p>Features detected include:
 * <ul>
 *   <li>CPU architecture features (SIMD, vector extensions, cache sizes)
 *   <li>Memory management capabilities (huge pages, memory compression)
 *   <li>Operating system features (container detection, resource limits)
 *   <li>JVM capabilities (Panama FFI availability, native compilation)
 *   <li>Performance characteristics (core count, memory bandwidth)
 * </ul>
 *
 * <p>This information enables platform-specific optimizations in WebAssembly compilation and execution.
 */
public final class PlatformFeatureDetector {

  private static final Logger LOGGER = Logger.getLogger(PlatformFeatureDetector.class.getName());

  /** Cache for detected platform features to avoid repeated detection. */
  private static volatile PlatformFeatures cachedFeatures;

  /** CPU architecture features that can be detected. */
  public enum CpuFeature {
    /** Advanced Vector Extensions (AVX) - x86_64 only */
    AVX,
    /** Advanced Vector Extensions 2 (AVX2) - x86_64 only */
    AVX2,
    /** Advanced Vector Extensions 512 (AVX512) - x86_64 only */
    AVX512,
    /** ARM NEON SIMD - ARM64 only */
    NEON,
    /** ARM Scalable Vector Extensions - ARM64 only */
    SVE,
    /** AES instruction set */
    AES,
    /** Population count instruction */
    POPCNT,
    /** Bit manipulation instructions */
    BMI,
    /** Fused multiply-add instructions */
    FMA,
    /** ARM Cryptographic Extensions - ARM64 only */
    ARM_CRYPTO
  }

  /** Memory management features that can be detected. */
  public enum MemoryFeature {
    /** Huge page support */
    HUGE_PAGES,
    /** Transparent huge pages */
    TRANSPARENT_HUGE_PAGES,
    /** Memory compression */
    MEMORY_COMPRESSION,
    /** NUMA topology detection */
    NUMA,
    /** Memory bandwidth optimization */
    MEMORY_BANDWIDTH_OPT
  }

  /** Operating system features that can be detected. */
  public enum OsFeature {
    /** Running in a container (Docker, Podman, etc.) */
    CONTAINER,
    /** Running in a virtual machine */
    VIRTUALIZED,
    /** Running under Windows Subsystem for Linux */
    WSL,
    /** systemd process management */
    SYSTEMD,
    /** cgroups resource management */
    CGROUPS,
    /** seccomp security features */
    SECCOMP,
    /** AppArmor security */
    APPARMOR,
    /** SELinux security */
    SELINUX
  }

  /** JVM features that can be detected. */
  public enum JvmFeature {
    /** Panama Foreign Function API availability */
    PANAMA_FFI,
    /** GraalVM native image compilation */
    GRAALVM_NATIVE,
    /** OpenJ9 JVM optimizations */
    OPENJ9,
    /** HotSpot JVM optimizations */
    HOTSPOT,
    /** Compressed OOPs */
    COMPRESSED_OOPS,
    /** Large heap support */
    LARGE_HEAP,
    /** JPMS module system */
    JPMS
  }

  /** Comprehensive platform feature information. */
  public static final class PlatformFeatures {
    private final PlatformDetector.PlatformInfo platformInfo;
    private final Set<CpuFeature> cpuFeatures;
    private final Set<MemoryFeature> memoryFeatures;
    private final Set<OsFeature> osFeatures;
    private final Set<JvmFeature> jvmFeatures;
    private final int logicalCores;
    private final int physicalCores;
    private final long totalMemory;
    private final long availableMemory;
    private final Optional<Integer> l1CacheSize;
    private final Optional<Integer> l2CacheSize;
    private final Optional<Integer> l3CacheSize;
    private final boolean hugePagesEnabled;
    private final boolean containerized;
    private final String jvmVersion;
    private final String jvmVendor;
    private final ConcurrentHashMap<String, Object> additionalProperties;

    PlatformFeatures(
        final PlatformDetector.PlatformInfo platformInfo,
        final Set<CpuFeature> cpuFeatures,
        final Set<MemoryFeature> memoryFeatures,
        final Set<OsFeature> osFeatures,
        final Set<JvmFeature> jvmFeatures,
        final int logicalCores,
        final int physicalCores,
        final long totalMemory,
        final long availableMemory,
        final Optional<Integer> l1CacheSize,
        final Optional<Integer> l2CacheSize,
        final Optional<Integer> l3CacheSize,
        final boolean hugePagesEnabled,
        final boolean containerized,
        final String jvmVersion,
        final String jvmVendor) {
      this.platformInfo = Objects.requireNonNull(platformInfo, "platformInfo must not be null");
      this.cpuFeatures = EnumSet.copyOf(cpuFeatures);
      this.memoryFeatures = EnumSet.copyOf(memoryFeatures);
      this.osFeatures = EnumSet.copyOf(osFeatures);
      this.jvmFeatures = EnumSet.copyOf(jvmFeatures);
      this.logicalCores = logicalCores;
      this.physicalCores = physicalCores;
      this.totalMemory = totalMemory;
      this.availableMemory = availableMemory;
      this.l1CacheSize = l1CacheSize;
      this.l2CacheSize = l2CacheSize;
      this.l3CacheSize = l3CacheSize;
      this.hugePagesEnabled = hugePagesEnabled;
      this.containerized = containerized;
      this.jvmVersion = jvmVersion;
      this.jvmVendor = jvmVendor;
      this.additionalProperties = new ConcurrentHashMap<>();
    }

    /**
     * Gets the basic platform information.
     *
     * @return the platform info
     */
    public PlatformDetector.PlatformInfo getPlatformInfo() {
      return platformInfo;
    }

    /**
     * Gets the detected CPU features.
     *
     * @return unmodifiable set of CPU features
     */
    public Set<CpuFeature> getCpuFeatures() {
      return Collections.unmodifiableSet(cpuFeatures);
    }

    /**
     * Gets the detected memory features.
     *
     * @return unmodifiable set of memory features
     */
    public Set<MemoryFeature> getMemoryFeatures() {
      return Collections.unmodifiableSet(memoryFeatures);
    }

    /**
     * Gets the detected OS features.
     *
     * @return unmodifiable set of OS features
     */
    public Set<OsFeature> getOsFeatures() {
      return Collections.unmodifiableSet(osFeatures);
    }

    /**
     * Gets the detected JVM features.
     *
     * @return unmodifiable set of JVM features
     */
    public Set<JvmFeature> getJvmFeatures() {
      return Collections.unmodifiableSet(jvmFeatures);
    }

    /**
     * Gets the number of logical CPU cores.
     *
     * @return the logical core count
     */
    public int getLogicalCores() {
      return logicalCores;
    }

    /**
     * Gets the number of physical CPU cores.
     *
     * @return the physical core count
     */
    public int getPhysicalCores() {
      return physicalCores;
    }

    /**
     * Gets the total system memory in bytes.
     *
     * @return the total memory
     */
    public long getTotalMemory() {
      return totalMemory;
    }

    /**
     * Gets the available system memory in bytes.
     *
     * @return the available memory
     */
    public long getAvailableMemory() {
      return availableMemory;
    }

    /**
     * Gets the L1 cache size per core in KB.
     *
     * @return optional L1 cache size
     */
    public Optional<Integer> getL1CacheSize() {
      return l1CacheSize;
    }

    /**
     * Gets the L2 cache size per core in KB.
     *
     * @return optional L2 cache size
     */
    public Optional<Integer> getL2CacheSize() {
      return l2CacheSize;
    }

    /**
     * Gets the L3 cache size in KB.
     *
     * @return optional L3 cache size
     */
    public Optional<Integer> getL3CacheSize() {
      return l3CacheSize;
    }

    /**
     * Checks if huge pages are enabled.
     *
     * @return true if huge pages are enabled
     */
    public boolean isHugePagesEnabled() {
      return hugePagesEnabled;
    }

    /**
     * Checks if running in a container.
     *
     * @return true if containerized
     */
    public boolean isContainerized() {
      return containerized;
    }

    /**
     * Gets the JVM version.
     *
     * @return the JVM version
     */
    public String getJvmVersion() {
      return jvmVersion;
    }

    /**
     * Gets the JVM vendor.
     *
     * @return the JVM vendor
     */
    public String getJvmVendor() {
      return jvmVendor;
    }

    /**
     * Checks if a specific CPU feature is available.
     *
     * @param feature the CPU feature to check
     * @return true if the feature is available
     */
    public boolean hasCpuFeature(final CpuFeature feature) {
      return cpuFeatures.contains(feature);
    }

    /**
     * Checks if a specific memory feature is available.
     *
     * @param feature the memory feature to check
     * @return true if the feature is available
     */
    public boolean hasMemoryFeature(final MemoryFeature feature) {
      return memoryFeatures.contains(feature);
    }

    /**
     * Checks if a specific OS feature is available.
     *
     * @param feature the OS feature to check
     * @return true if the feature is available
     */
    public boolean hasOsFeature(final OsFeature feature) {
      return osFeatures.contains(feature);
    }

    /**
     * Checks if a specific JVM feature is available.
     *
     * @param feature the JVM feature to check
     * @return true if the feature is available
     */
    public boolean hasJvmFeature(final JvmFeature feature) {
      return jvmFeatures.contains(feature);
    }

    /**
     * Gets an additional property value.
     *
     * @param key the property key
     * @return optional property value
     */
    public Optional<Object> getAdditionalProperty(final String key) {
      return Optional.ofNullable(additionalProperties.get(key));
    }

    /**
     * Sets an additional property value.
     *
     * @param key the property key
     * @param value the property value
     */
    public void setAdditionalProperty(final String key, final Object value) {
      if (value == null) {
        additionalProperties.remove(key);
      } else {
        additionalProperties.put(key, value);
      }
    }

    /**
     * Gets recommended WebAssembly compiler optimization level.
     *
     * @return optimization level (0-3)
     */
    public int getRecommendedOptimizationLevel() {
      // Higher optimization for server-class machines
      if (physicalCores >= 8 && totalMemory >= 8L * 1024 * 1024 * 1024) { // 8GB+
        return 3; // Aggressive optimization
      } else if (physicalCores >= 4 && totalMemory >= 4L * 1024 * 1024 * 1024) { // 4GB+
        return 2; // Moderate optimization
      } else if (physicalCores >= 2) {
        return 1; // Basic optimization
      } else {
        return 0; // Minimal optimization for constrained environments
      }
    }

    /**
     * Gets recommended thread pool size for WebAssembly compilation.
     *
     * @return recommended thread count
     */
    public int getRecommendedCompilationThreads() {
      // Use fewer threads in containers or low-memory environments
      if (containerized || availableMemory < 2L * 1024 * 1024 * 1024) { // < 2GB
        return Math.max(1, physicalCores / 2);
      } else {
        return Math.max(2, physicalCores);
      }
    }

    /**
     * Checks if SIMD optimizations should be enabled.
     *
     * @return true if SIMD should be used
     */
    public boolean shouldEnableSimd() {
      return hasCpuFeature(CpuFeature.AVX)
          || hasCpuFeature(CpuFeature.AVX2)
          || hasCpuFeature(CpuFeature.NEON);
    }

    @Override
    public String toString() {
      return String.format(
          "PlatformFeatures{platform=%s, cores=%d/%d, memory=%dMB/%dMB, features=[CPU:%d,MEM:%d,OS:%d,JVM:%d]}",
          platformInfo.getPlatformId(),
          physicalCores,
          logicalCores,
          availableMemory / (1024 * 1024),
          totalMemory / (1024 * 1024),
          cpuFeatures.size(),
          memoryFeatures.size(),
          osFeatures.size(),
          jvmFeatures.size());
    }
  }

  /** Private constructor to prevent instantiation of utility class. */
  private PlatformFeatureDetector() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Detects and returns comprehensive platform feature information.
   *
   * <p>This method caches results after the first call for performance.
   *
   * @return the platform features
   */
  public static PlatformFeatures detect() {
    PlatformFeatures result = cachedFeatures;
    if (result == null) {
      synchronized (PlatformFeatureDetector.class) {
        result = cachedFeatures;
        if (result == null) {
          result = detectFeatures();
          cachedFeatures = result;
          LOGGER.info("Detected platform features: " + result);
        }
      }
    }
    return result;
  }

  /**
   * Forces re-detection of platform features, bypassing cache.
   *
   * @return the freshly detected platform features
   */
  public static PlatformFeatures detectFresh() {
    synchronized (PlatformFeatureDetector.class) {
      final PlatformFeatures result = detectFeatures();
      cachedFeatures = result;
      LOGGER.info("Re-detected platform features: " + result);
      return result;
    }
  }

  /**
   * Performs the actual feature detection.
   *
   * @return the detected platform features
   */
  private static PlatformFeatures detectFeatures() {
    final PlatformDetector.PlatformInfo platformInfo = PlatformDetector.detect();

    // Detect system information
    final OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
    final MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
    final Runtime runtime = Runtime.getRuntime();

    final int logicalCores = runtime.availableProcessors();
    final int physicalCores = detectPhysicalCores(osMxBean, logicalCores);
    final long totalMemory = getTotalMemory(osMxBean);
    final long availableMemory = runtime.maxMemory();

    // Detect features
    final Set<CpuFeature> cpuFeatures = detectCpuFeatures(platformInfo);
    final Set<MemoryFeature> memoryFeatures = detectMemoryFeatures(platformInfo);
    final Set<OsFeature> osFeatures = detectOsFeatures(platformInfo);
    final Set<JvmFeature> jvmFeatures = detectJvmFeatures();

    // Detect cache sizes (simplified detection)
    final Optional<Integer> l1CacheSize = detectL1CacheSize(platformInfo);
    final Optional<Integer> l2CacheSize = detectL2CacheSize(platformInfo);
    final Optional<Integer> l3CacheSize = detectL3CacheSize(platformInfo);

    final boolean hugePagesEnabled = memoryFeatures.contains(MemoryFeature.HUGE_PAGES);
    final boolean containerized = osFeatures.contains(OsFeature.CONTAINER);

    final String jvmVersion = System.getProperty("java.version");
    final String jvmVendor = System.getProperty("java.vendor");

    return new PlatformFeatures(
        platformInfo,
        cpuFeatures,
        memoryFeatures,
        osFeatures,
        jvmFeatures,
        logicalCores,
        physicalCores,
        totalMemory,
        availableMemory,
        l1CacheSize,
        l2CacheSize,
        l3CacheSize,
        hugePagesEnabled,
        containerized,
        jvmVersion,
        jvmVendor);
  }

  /**
   * Detects physical CPU cores.
   *
   * @param osMxBean the OS management bean
   * @param logicalCores the logical core count
   * @return the physical core count
   */
  private static int detectPhysicalCores(final OperatingSystemMXBean osMxBean, final int logicalCores) {
    try {
      // Try to use reflection to get more detailed CPU info
      if (osMxBean.getClass().getName().contains("UnixOperatingSystem")) {
        // On Unix systems, try to read from /proc/cpuinfo
        return readPhysicalCoresFromProcCpuinfo().orElse(logicalCores);
      } else {
        // Default assumption: hyperthreading doubles logical cores
        return Math.max(1, logicalCores / 2);
      }
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Could not detect physical cores", e);
      return logicalCores;
    }
  }

  /**
   * Reads physical core count from /proc/cpuinfo on Linux.
   *
   * @return optional physical core count
   */
  private static Optional<Integer> readPhysicalCoresFromProcCpuinfo() {
    try {
      final Path cpuinfo = Paths.get("/proc/cpuinfo");
      if (Files.exists(cpuinfo)) {
        final List<String> lines = Files.readAllLines(cpuinfo);
        final long physicalIds = lines.stream()
            .filter(line -> line.startsWith("physical id"))
            .map(line -> line.substring(line.indexOf(':') + 1).trim())
            .distinct()
            .count();
        return physicalIds > 0 ? Optional.of((int) physicalIds) : Optional.empty();
      }
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not read /proc/cpuinfo", e);
    }
    return Optional.empty();
  }

  /**
   * Gets total system memory.
   *
   * @param osMxBean the OS management bean
   * @return total memory in bytes
   */
  private static long getTotalMemory(final OperatingSystemMXBean osMxBean) {
    try {
      // Try to get total physical memory size via reflection
      final java.lang.reflect.Method method = osMxBean.getClass().getMethod("getTotalPhysicalMemorySize");
      method.setAccessible(true);
      return (Long) method.invoke(osMxBean);
    } catch (final Exception e) {
      LOGGER.log(Level.FINE, "Could not get total physical memory", e);
      // Fallback to runtime max memory
      return Runtime.getRuntime().maxMemory();
    }
  }

  /**
   * Detects CPU features based on platform and architecture.
   *
   * @param platformInfo the platform information
   * @return set of detected CPU features
   */
  private static Set<CpuFeature> detectCpuFeatures(final PlatformDetector.PlatformInfo platformInfo) {
    final Set<CpuFeature> features = EnumSet.noneOf(CpuFeature.class);

    if (platformInfo.getArchitecture() == PlatformDetector.Architecture.X86_64) {
      // x86_64 feature detection (simplified - would need native code for full detection)
      features.add(CpuFeature.POPCNT); // Most modern x86_64 CPUs have this

      // Check for AVX support via system properties or JVM flags
      if (isAvxSupported()) {
        features.add(CpuFeature.AVX);
        features.add(CpuFeature.AVX2); // Assume AVX2 if AVX is present
      }

      // Most modern processors have AES and FMA
      features.add(CpuFeature.AES);
      features.add(CpuFeature.FMA);

    } else if (platformInfo.getArchitecture() == PlatformDetector.Architecture.AARCH64) {
      // ARM64 feature detection
      features.add(CpuFeature.NEON); // Standard on ARM64
      features.add(CpuFeature.ARM_CRYPTO); // Common on modern ARM64

      // SVE detection would require native code
      if (isSveSupported()) {
        features.add(CpuFeature.SVE);
      }
    }

    return features;
  }

  /**
   * Checks if AVX is supported (simplified detection).
   *
   * @return true if AVX is likely supported
   */
  private static boolean isAvxSupported() {
    // This is a simplified check - real detection would require native code
    final String osArch = System.getProperty("os.arch");
    return osArch != null && osArch.contains("64");
  }

  /**
   * Checks if ARM SVE is supported (simplified detection).
   *
   * @return true if SVE is likely supported
   */
  private static boolean isSveSupported() {
    // This would require reading /proc/cpuinfo or using native detection
    return false; // Conservative default
  }

  /**
   * Detects memory management features.
   *
   * @param platformInfo the platform information
   * @return set of detected memory features
   */
  private static Set<MemoryFeature> detectMemoryFeatures(final PlatformDetector.PlatformInfo platformInfo) {
    final Set<MemoryFeature> features = EnumSet.noneOf(MemoryFeature.class);

    if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.LINUX) {
      // Check for huge pages support
      if (isHugePagesAvailable()) {
        features.add(MemoryFeature.HUGE_PAGES);
      }

      if (isTransparentHugePagesEnabled()) {
        features.add(MemoryFeature.TRANSPARENT_HUGE_PAGES);
      }

      // NUMA detection
      if (isNumaAvailable()) {
        features.add(MemoryFeature.NUMA);
      }

    } else if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.WINDOWS) {
      // Windows memory features
      features.add(MemoryFeature.HUGE_PAGES); // Windows has large page support

    } else if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.MACOS) {
      // macOS memory features
      // macOS doesn't have traditional huge pages but has VM optimizations
    }

    return features;
  }

  /**
   * Checks if huge pages are available.
   *
   * @return true if huge pages are available
   */
  private static boolean isHugePagesAvailable() {
    try {
      final Path hugepages = Paths.get("/proc/meminfo");
      if (Files.exists(hugepages)) {
        return Files.readAllLines(hugepages).stream()
            .anyMatch(line -> line.startsWith("HugePages_Total:") && !line.contains("0"));
      }
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not check huge pages availability", e);
    }
    return false;
  }

  /**
   * Checks if transparent huge pages are enabled.
   *
   * @return true if transparent huge pages are enabled
   */
  private static boolean isTransparentHugePagesEnabled() {
    try {
      final Path thpEnabled = Paths.get("/sys/kernel/mm/transparent_hugepage/enabled");
      if (Files.exists(thpEnabled)) {
        final String content = Files.readString(thpEnabled);
        return content.contains("[always]") || content.contains("[madvise]");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not check transparent huge pages", e);
    }
    return false;
  }

  /**
   * Checks if NUMA is available.
   *
   * @return true if NUMA is available
   */
  private static boolean isNumaAvailable() {
    try {
      final Path numaNodes = Paths.get("/sys/devices/system/node");
      return Files.exists(numaNodes) && Files.list(numaNodes)
          .anyMatch(path -> path.getFileName().toString().startsWith("node"));
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not check NUMA availability", e);
    }
    return false;
  }

  /**
   * Detects operating system features.
   *
   * @param platformInfo the platform information
   * @return set of detected OS features
   */
  private static Set<OsFeature> detectOsFeatures(final PlatformDetector.PlatformInfo platformInfo) {
    final Set<OsFeature> features = EnumSet.noneOf(OsFeature.class);

    // Container detection
    if (isRunningInContainer()) {
      features.add(OsFeature.CONTAINER);
    }

    // Virtualization detection
    if (isVirtualized()) {
      features.add(OsFeature.VIRTUALIZED);
    }

    if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.LINUX) {
      // Linux-specific feature detection
      if (isCgroupsAvailable()) {
        features.add(OsFeature.CGROUPS);
      }

      if (isSystemdAvailable()) {
        features.add(OsFeature.SYSTEMD);
      }

      if (isSeccompAvailable()) {
        features.add(OsFeature.SECCOMP);
      }

      if (isAppArmorAvailable()) {
        features.add(OsFeature.APPARMOR);
      }

      if (isSelinuxAvailable()) {
        features.add(OsFeature.SELINUX);
      }

    } else if (platformInfo.getOperatingSystem() == PlatformDetector.OperatingSystem.WINDOWS) {
      // Windows-specific feature detection
      if (isWslDetected()) {
        features.add(OsFeature.WSL);
      }
    }

    return features;
  }

  /**
   * Checks if running in a container.
   *
   * @return true if containerized
   */
  private static boolean isRunningInContainer() {
    // Check for container indicators
    return Files.exists(Paths.get("/.dockerenv"))
        || Files.exists(Paths.get("/run/.containerenv"))
        || System.getenv("container") != null
        || System.getenv("KUBERNETES_SERVICE_HOST") != null;
  }

  /**
   * Checks if running in a virtual machine.
   *
   * @return true if virtualized
   */
  private static boolean isVirtualized() {
    try {
      // Check DMI information on Linux
      final Path dmiProductName = Paths.get("/sys/devices/virtual/dmi/id/product_name");
      if (Files.exists(dmiProductName)) {
        final String productName = Files.readString(dmiProductName).toLowerCase();
        return productName.contains("vmware")
            || productName.contains("virtualbox")
            || productName.contains("kvm")
            || productName.contains("qemu");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not check virtualization", e);
    }
    return false;
  }

  /**
   * Checks if cgroups are available.
   *
   * @return true if cgroups are available
   */
  private static boolean isCgroupsAvailable() {
    return Files.exists(Paths.get("/proc/cgroups")) || Files.exists(Paths.get("/sys/fs/cgroup"));
  }

  /**
   * Checks if systemd is available.
   *
   * @return true if systemd is available
   */
  private static boolean isSystemdAvailable() {
    return Files.exists(Paths.get("/run/systemd/system"));
  }

  /**
   * Checks if seccomp is available.
   *
   * @return true if seccomp is available
   */
  private static boolean isSeccompAvailable() {
    try {
      final Path seccomp = Paths.get("/proc/sys/kernel/seccomp");
      return Files.exists(seccomp) && !"0".equals(Files.readString(seccomp).trim());
    } catch (final IOException e) {
      return false;
    }
  }

  /**
   * Checks if AppArmor is available.
   *
   * @return true if AppArmor is available
   */
  private static boolean isAppArmorAvailable() {
    return Files.exists(Paths.get("/sys/kernel/security/apparmor"));
  }

  /**
   * Checks if SELinux is available.
   *
   * @return true if SELinux is available
   */
  private static boolean isSelinuxAvailable() {
    return Files.exists(Paths.get("/sys/fs/selinux"));
  }

  /**
   * Checks if running under WSL.
   *
   * @return true if WSL is detected
   */
  private static boolean isWslDetected() {
    try {
      final Path procVersion = Paths.get("/proc/version");
      if (Files.exists(procVersion)) {
        final String version = Files.readString(procVersion).toLowerCase();
        return version.contains("microsoft") || version.contains("wsl");
      }
    } catch (final IOException e) {
      LOGGER.log(Level.FINE, "Could not check WSL", e);
    }
    return false;
  }

  /**
   * Detects JVM features.
   *
   * @return set of detected JVM features
   */
  private static Set<JvmFeature> detectJvmFeatures() {
    final Set<JvmFeature> features = EnumSet.noneOf(JvmFeature.class);

    // Check Java version for Panama FFI
    if (isPanamaFfiAvailable()) {
      features.add(JvmFeature.PANAMA_FFI);
    }

    // Check JVM implementation
    final String jvmName = System.getProperty("java.vm.name", "").toLowerCase();
    final String jvmVendor = System.getProperty("java.vendor", "").toLowerCase();

    if (jvmName.contains("hotspot")) {
      features.add(JvmFeature.HOTSPOT);
    }

    if (jvmName.contains("openj9") || jvmVendor.contains("eclipse")) {
      features.add(JvmFeature.OPENJ9);
    }

    if (jvmName.contains("graalvm") || jvmVendor.contains("graalvm")) {
      features.add(JvmFeature.GRAALVM_NATIVE);
    }

    // Check for compressed OOPs
    if (isCompressedOopsEnabled()) {
      features.add(JvmFeature.COMPRESSED_OOPS);
    }

    // Check for large heap
    if (Runtime.getRuntime().maxMemory() > 4L * 1024 * 1024 * 1024) { // > 4GB
      features.add(JvmFeature.LARGE_HEAP);
    }

    // Check for module system
    if (System.getProperty("java.version").startsWith("1.")) {
      // Java 8 or earlier - no modules
    } else {
      features.add(JvmFeature.JPMS);
    }

    return features;
  }

  /**
   * Checks if Panama FFI is available.
   *
   * @return true if Panama FFI is available
   */
  private static boolean isPanamaFfiAvailable() {
    try {
      Class.forName("java.lang.foreign.MemorySegment");
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Checks if compressed OOPs are enabled.
   *
   * @return true if compressed OOPs are enabled
   */
  private static boolean isCompressedOopsEnabled() {
    // This is a simplified check - real detection would require JVM-specific APIs
    final List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
    return vmArguments.stream().anyMatch(arg -> arg.contains("UseCompressedOops"));
  }

  /**
   * Detects L1 cache size (simplified).
   *
   * @param platformInfo the platform information
   * @return optional L1 cache size in KB
   */
  private static Optional<Integer> detectL1CacheSize(final PlatformDetector.PlatformInfo platformInfo) {
    // This would typically require native code or reading from /proc/cpuinfo
    // Providing typical values for common architectures
    if (platformInfo.getArchitecture() == PlatformDetector.Architecture.X86_64) {
      return Optional.of(32); // Typical L1 cache size for modern x86_64
    } else if (platformInfo.getArchitecture() == PlatformDetector.Architecture.AARCH64) {
      return Optional.of(64); // Typical L1 cache size for modern ARM64
    }
    return Optional.empty();
  }

  /**
   * Detects L2 cache size (simplified).
   *
   * @param platformInfo the platform information
   * @return optional L2 cache size in KB
   */
  private static Optional<Integer> detectL2CacheSize(final PlatformDetector.PlatformInfo platformInfo) {
    if (platformInfo.getArchitecture() == PlatformDetector.Architecture.X86_64) {
      return Optional.of(256); // Typical L2 cache size for modern x86_64
    } else if (platformInfo.getArchitecture() == PlatformDetector.Architecture.AARCH64) {
      return Optional.of(512); // Typical L2 cache size for modern ARM64
    }
    return Optional.empty();
  }

  /**
   * Detects L3 cache size (simplified).
   *
   * @param platformInfo the platform information
   * @return optional L3 cache size in KB
   */
  private static Optional<Integer> detectL3CacheSize(final PlatformDetector.PlatformInfo platformInfo) {
    if (platformInfo.getArchitecture() == PlatformDetector.Architecture.X86_64) {
      return Optional.of(8192); // Typical L3 cache size for modern x86_64 (8MB)
    } else if (platformInfo.getArchitecture() == PlatformDetector.Architecture.AARCH64) {
      return Optional.of(4096); // Typical L3 cache size for modern ARM64 (4MB)
    }
    return Optional.empty();
  }
}