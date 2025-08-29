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

package ai.tegmentum.wasmtime4j.panama.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Advanced capability detector for Panama FFI availability and feature support.
 *
 * <p>This utility provides comprehensive detection of Panama Foreign Function API capabilities,
 * including Java version validation, native access permissions, preview feature detection, and
 * graceful fallback recommendations. It serves as the foundation for runtime selection logic and
 * provides detailed diagnostics for troubleshooting Panama availability issues.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Comprehensive Panama FFI capability detection
 *   <li>Detailed diagnostic information for troubleshooting
 *   <li>Performance-optimized detection with caching
 *   <li>Graceful fallback detection and recommendations
 *   <li>Native access permission validation
 *   <li>Preview feature status detection
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaCapabilityDetector {
  private static final Logger logger = Logger.getLogger(PanamaCapabilityDetector.class.getName());

  // Cached detection results for performance
  private static final AtomicReference<DetectionResult> cachedResult = new AtomicReference<>();

  // Required Panama classes for detection
  private static final String[] REQUIRED_PANAMA_CLASSES = {
    "java.lang.foreign.MemorySegment",
    "java.lang.foreign.Arena",
    "java.lang.foreign.SymbolLookup",
    "java.lang.foreign.FunctionDescriptor",
    "java.lang.foreign.ValueLayout",
    "java.lang.foreign.Linker"
  };

  // Preview feature system properties to check
  private static final String[] PREVIEW_PROPERTIES = {"jdk.preview.enable", "jdk.foreign.enable"};

  // Native access related properties
  private static final String[] NATIVE_ACCESS_PROPERTIES = {"native.encoding", "java.library.path"};

  // Prevent instantiation
  private PanamaCapabilityDetector() {
    throw new UnsupportedOperationException("Utility class");
  }

  /**
   * Performs comprehensive Panama FFI capability detection.
   *
   * <p>This method performs a thorough analysis of the current Java environment to determine Panama
   * FFI availability and provides detailed diagnostic information. Results are cached for
   * performance on subsequent calls.
   *
   * @return detailed detection results including availability status and diagnostics
   */
  public static DetectionResult detectCapabilities() {
    DetectionResult cached = cachedResult.get();
    if (cached != null) {
      return cached;
    }

    synchronized (PanamaCapabilityDetector.class) {
      cached = cachedResult.get();
      if (cached != null) {
        return cached;
      }

      final DetectionResult result = performDetection();
      cachedResult.set(result);
      return result;
    }
  }

  /**
   * Checks if Panama FFI is available in the current environment.
   *
   * <p>This is a convenience method that returns only the availability status. For detailed
   * diagnostic information, use {@link #detectCapabilities()}.
   *
   * @return true if Panama FFI is available and functional
   */
  public static boolean isPanamaAvailable() {
    return detectCapabilities().isAvailable();
  }

  /**
   * Gets a human-readable status description of Panama FFI availability.
   *
   * @return a status description suitable for logging or user display
   */
  public static String getStatusDescription() {
    return detectCapabilities().getStatusDescription();
  }

  /**
   * Gets detailed diagnostic information about Panama FFI availability.
   *
   * @return diagnostic information for troubleshooting
   */
  public static String getDiagnosticInfo() {
    return detectCapabilities().getDiagnosticInfo();
  }

  /**
   * Gets fallback recommendations if Panama FFI is not available.
   *
   * @return recommended fallback strategies
   */
  public static FallbackRecommendation getFallbackRecommendation() {
    return detectCapabilities().getFallbackRecommendation();
  }

  /**
   * Forces re-detection of capabilities, clearing cached results.
   *
   * <p>This method is useful for testing scenarios or when the environment might have changed
   * during runtime (e.g., through dynamic configuration).
   */
  public static void clearCache() {
    cachedResult.set(null);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Cleared Panama capability detection cache");
    }
  }

  /**
   * Performs the actual capability detection logic.
   *
   * @return comprehensive detection results
   */
  private static DetectionResult performDetection() {
    final long startTime = System.nanoTime();

    try {
      final DetectionResult.Builder builder = new DetectionResult.Builder();

      // Step 1: Check Java version
      final JavaVersionInfo versionInfo = analyzeJavaVersion();
      builder.setJavaVersionInfo(versionInfo);

      if (!versionInfo.isJava23OrHigher()) {
        return builder
            .setAvailable(false)
            .setFailureReason("Java 23+ required for Panama FFI")
            .setFallbackRecommendation(FallbackRecommendation.USE_JNI)
            .build();
      }

      // Step 2: Check Panama class availability
      final ClassAvailabilityInfo classInfo = analyzePanamaClasses();
      builder.setClassAvailabilityInfo(classInfo);

      if (!classInfo.areAllClassesAvailable()) {
        return builder
            .setAvailable(false)
            .setFailureReason("Panama FFI classes not available: " + classInfo.getMissingClasses())
            .setFallbackRecommendation(FallbackRecommendation.ENABLE_PREVIEW)
            .build();
      }

      // Step 3: Check native access permissions
      final NativeAccessInfo nativeAccessInfo = analyzeNativeAccess();
      builder.setNativeAccessInfo(nativeAccessInfo);

      // Step 4: Check preview features (if applicable)
      final PreviewFeatureInfo previewInfo = analyzePreviewFeatures();
      builder.setPreviewFeatureInfo(previewInfo);

      // Step 5: Perform functional tests
      final FunctionalTestInfo functionalInfo = performFunctionalTests();
      builder.setFunctionalTestInfo(functionalInfo);

      // Determine overall availability
      boolean available =
          versionInfo.isJava23OrHigher()
              && classInfo.areAllClassesAvailable()
              && functionalInfo.areBasicTestsPassing();

      if (!available && functionalInfo.hasNativeAccessIssues()) {
        return builder
            .setAvailable(false)
            .setFailureReason("Native access not enabled or restricted")
            .setFallbackRecommendation(FallbackRecommendation.ENABLE_NATIVE_ACCESS)
            .build();
      }

      return builder
          .setAvailable(available)
          .setFallbackRecommendation(
              available ? FallbackRecommendation.NONE : FallbackRecommendation.USE_JNI)
          .build();

    } catch (Exception e) {
      logger.log(Level.WARNING, "Error during Panama capability detection", e);

      return new DetectionResult.Builder()
          .setAvailable(false)
          .setFailureReason("Detection error: " + e.getMessage())
          .setFallbackRecommendation(FallbackRecommendation.USE_JNI)
          .build();
    } finally {
      final long duration = System.nanoTime() - startTime;
      if (logger.isLoggable(Level.FINE)) {
        logger.fine(
            String.format(
                "Panama capability detection completed in %.2f ms", duration / 1_000_000.0));
      }
    }
  }

  /** Analyzes Java version information. */
  private static JavaVersionInfo analyzeJavaVersion() {
    final String javaVersion = System.getProperty("java.version");
    final String javaVendor = System.getProperty("java.vendor");
    final String runtimeName = System.getProperty("java.runtime.name");

    final int majorVersion = parseMajorVersion(javaVersion);
    final boolean isJava23OrHigher = majorVersion >= 23;

    return new JavaVersionInfo(
        javaVersion, javaVendor, runtimeName, majorVersion, isJava23OrHigher);
  }

  /** Analyzes Panama class availability. */
  private static ClassAvailabilityInfo analyzePanamaClasses() {
    final ClassAvailabilityInfo.Builder builder = new ClassAvailabilityInfo.Builder();

    for (String className : REQUIRED_PANAMA_CLASSES) {
      try {
        Class.forName(className);
        builder.addAvailableClass(className);
      } catch (ClassNotFoundException e) {
        builder.addMissingClass(className);
      }
    }

    return builder.build();
  }

  /** Analyzes native access configuration. */
  private static NativeAccessInfo analyzeNativeAccess() {
    final NativeAccessInfo.Builder builder = new NativeAccessInfo.Builder();

    // Check relevant system properties
    for (String property : NATIVE_ACCESS_PROPERTIES) {
      final String value = System.getProperty(property);
      if (value != null) {
        builder.addProperty(property, value);
      }
    }

    // Try to detect native access restrictions
    boolean nativeAccessEnabled = true;
    String restrictionReason = null;

    try {
      // Attempt to access a native method that requires permissions
      final Class<?> runtimeClass = Class.forName("java.lang.Runtime");
      final Method loadLibraryMethod =
          runtimeClass.getDeclaredMethod("loadLibrary0", Class.class, String.class);

      // If we can access this method, native access is likely enabled
      if (loadLibraryMethod != null) {
        builder.setNativeAccessEnabled(true);
      }
    } catch (Exception e) {
      nativeAccessEnabled = false;
      restrictionReason = e.getMessage();
    }

    return builder
        .setNativeAccessEnabled(nativeAccessEnabled)
        .setRestrictionReason(restrictionReason)
        .build();
  }

  /** Analyzes preview feature configuration. */
  private static PreviewFeatureInfo analyzePreviewFeatures() {
    final PreviewFeatureInfo.Builder builder = new PreviewFeatureInfo.Builder();

    for (String property : PREVIEW_PROPERTIES) {
      final String value = System.getProperty(property);
      if (value != null) {
        builder.addPreviewProperty(property, value);
      }
    }

    return builder.build();
  }

  /** Performs functional tests of Panama FFI capabilities. */
  private static FunctionalTestInfo performFunctionalTests() {
    final FunctionalTestInfo.Builder builder = new FunctionalTestInfo.Builder();

    // Test 1: Basic class loading
    try {
      Class.forName("java.lang.foreign.MemorySegment");
      builder.addPassingTest("Basic MemorySegment class loading");
    } catch (Exception e) {
      builder.addFailingTest("Basic MemorySegment class loading", e.getMessage());
    }

    // Test 2: Arena creation
    try {
      final Class<?> arenaClass = Class.forName("java.lang.foreign.Arena");
      final Method ofConfinedMethod = arenaClass.getMethod("ofConfined");
      final Object arena = ofConfinedMethod.invoke(null);
      if (arena != null) {
        builder.addPassingTest("Arena creation");

        // Clean up
        final Method closeMethod = arena.getClass().getMethod("close");
        closeMethod.invoke(arena);
      }
    } catch (Exception e) {
      builder.addFailingTest("Arena creation", e.getMessage());
      if (e.getMessage().contains("access") || e.getMessage().contains("permission")) {
        builder.setHasNativeAccessIssues(true);
      }
    }

    // Test 3: Native linker access
    try {
      final Class<?> linkerClass = Class.forName("java.lang.foreign.Linker");
      final Method nativeLinkerMethod = linkerClass.getMethod("nativeLinker");
      final Object linker = nativeLinkerMethod.invoke(null);
      if (linker != null) {
        builder.addPassingTest("Native linker access");
      }
    } catch (Exception e) {
      builder.addFailingTest("Native linker access", e.getMessage());
      if (e.getMessage().contains("access") || e.getMessage().contains("permission")) {
        builder.setHasNativeAccessIssues(true);
      }
    }

    return builder.build();
  }

  /** Parses the major version number from a Java version string. */
  private static int parseMajorVersion(final String version) {
    try {
      // Handle old format like "1.8.0_261"
      if (version.startsWith("1.")) {
        return Integer.parseInt(version.substring(2, version.indexOf('.', 2)));
      }

      // Handle new format like "17.0.1", "23-ea", etc.
      final int dotIndex = version.indexOf('.');
      final int dashIndex = version.indexOf('-');

      int endIndex = version.length();
      if (dotIndex > 0) {
        endIndex = Math.min(endIndex, dotIndex);
      }
      if (dashIndex > 0) {
        endIndex = Math.min(endIndex, dashIndex);
      }

      return Integer.parseInt(version.substring(0, endIndex));
    } catch (Exception e) {
      logger.warning("Failed to parse Java version: " + version);
      return 0;
    }
  }

  /** Represents the result of Panama capability detection. */
  public static final class DetectionResult {
    private final boolean available;
    private final String failureReason;
    private final FallbackRecommendation fallbackRecommendation;
    private final JavaVersionInfo javaVersionInfo;
    private final ClassAvailabilityInfo classAvailabilityInfo;
    private final NativeAccessInfo nativeAccessInfo;
    private final PreviewFeatureInfo previewFeatureInfo;
    private final FunctionalTestInfo functionalTestInfo;

    private DetectionResult(final Builder builder) {
      this.available = builder.available;
      this.failureReason = builder.failureReason;
      this.fallbackRecommendation = builder.fallbackRecommendation;
      this.javaVersionInfo = builder.javaVersionInfo;
      this.classAvailabilityInfo = builder.classAvailabilityInfo;
      this.nativeAccessInfo = builder.nativeAccessInfo;
      this.previewFeatureInfo = builder.previewFeatureInfo;
      this.functionalTestInfo = builder.functionalTestInfo;
    }

    public boolean isAvailable() {
      return available;
    }

    public String getFailureReason() {
      return failureReason;
    }

    /** Gets the fallback recommendation for this platform.
     *
     * @return the fallback recommendation
     */
    public FallbackRecommendation getFallbackRecommendation() {
      return fallbackRecommendation != null
          ? fallbackRecommendation
          : FallbackRecommendation.USE_JNI;
    }

    /** Gets a human-readable description of the Panama status.
     *
     * @return status description
     */
    public String getStatusDescription() {
      if (available) {
        return "Panama FFI is available and functional";
      }

      if (failureReason != null) {
        return "Panama FFI not available: " + failureReason;
      }

      return "Panama FFI availability unknown";
    }

    /** Gets detailed diagnostic information about Panama capabilities.
     *
     * @return diagnostic information string
     */
    public String getDiagnosticInfo() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Panama FFI Diagnostic Information:\n");
      sb.append("Available: ").append(available).append("\n");

      if (failureReason != null) {
        sb.append("Failure Reason: ").append(failureReason).append("\n");
      }

      if (javaVersionInfo != null) {
        sb.append("Java Version: ").append(javaVersionInfo.getFullVersion()).append("\n");
        sb.append("Java Vendor: ").append(javaVersionInfo.getVendor()).append("\n");
        sb.append("Runtime: ").append(javaVersionInfo.getRuntimeName()).append("\n");
        sb.append("Major Version: ").append(javaVersionInfo.getMajorVersion()).append("\n");
      }

      if (classAvailabilityInfo != null) {
        sb.append("Available Classes: ")
            .append(classAvailabilityInfo.getAvailableClasses().size())
            .append("\n");
        sb.append("Missing Classes: ")
            .append(classAvailabilityInfo.getMissingClasses().size())
            .append("\n");
      }

      if (functionalTestInfo != null) {
        sb.append("Passing Tests: ")
            .append(functionalTestInfo.getPassingTests().size())
            .append("\n");
        sb.append("Failing Tests: ")
            .append(functionalTestInfo.getFailingTests().size())
            .append("\n");
      }

      sb.append("Fallback Recommendation: ").append(getFallbackRecommendation()).append("\n");

      return sb.toString();
    }

    public JavaVersionInfo getJavaVersionInfo() {
      return javaVersionInfo;
    }

    public ClassAvailabilityInfo getClassAvailabilityInfo() {
      return classAvailabilityInfo;
    }

    public NativeAccessInfo getNativeAccessInfo() {
      return nativeAccessInfo;
    }

    public PreviewFeatureInfo getPreviewFeatureInfo() {
      return previewFeatureInfo;
    }

    public FunctionalTestInfo getFunctionalTestInfo() {
      return functionalTestInfo;
    }

    static final class Builder {
      private boolean available = false;
      private String failureReason;
      private FallbackRecommendation fallbackRecommendation;
      private JavaVersionInfo javaVersionInfo;
      private ClassAvailabilityInfo classAvailabilityInfo;
      private NativeAccessInfo nativeAccessInfo;
      private PreviewFeatureInfo previewFeatureInfo;
      private FunctionalTestInfo functionalTestInfo;

      public Builder setAvailable(final boolean available) {
        this.available = available;
        return this;
      }

      public Builder setFailureReason(final String failureReason) {
        this.failureReason = failureReason;
        return this;
      }

      public Builder setFallbackRecommendation(final FallbackRecommendation recommendation) {
        this.fallbackRecommendation = recommendation;
        return this;
      }

      public Builder setJavaVersionInfo(final JavaVersionInfo info) {
        this.javaVersionInfo = info;
        return this;
      }

      public Builder setClassAvailabilityInfo(final ClassAvailabilityInfo info) {
        this.classAvailabilityInfo = info;
        return this;
      }

      public Builder setNativeAccessInfo(final NativeAccessInfo info) {
        this.nativeAccessInfo = info;
        return this;
      }

      public Builder setPreviewFeatureInfo(final PreviewFeatureInfo info) {
        this.previewFeatureInfo = info;
        return this;
      }

      public Builder setFunctionalTestInfo(final FunctionalTestInfo info) {
        this.functionalTestInfo = info;
        return this;
      }

      public DetectionResult build() {
        return new DetectionResult(this);
      }
    }
  }

  /** Recommendations for fallback strategies when Panama is not available. */
  public enum FallbackRecommendation {
    /** No fallback needed - Panama is available. */
    NONE("Panama FFI is available"),

    /** Use JNI implementation instead. */
    USE_JNI("Use JNI implementation for Java < 23 or when Panama is unavailable"),

    /** Enable preview features to access Panama. */
    ENABLE_PREVIEW("Add --enable-preview to JVM arguments"),

    /** Enable native access for Panama operations. */
    ENABLE_NATIVE_ACCESS("Add --enable-native-access=ALL-UNNAMED to JVM arguments"),

    /** Upgrade to Java 23 or higher. */
    UPGRADE_JAVA("Upgrade to Java 23 or higher for Panama FFI support"),

    /** Manual configuration required. */
    MANUAL_CONFIGURATION("Manual JVM configuration required - see documentation");

    private final String description;

    FallbackRecommendation(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return description;
    }
  }

  // Additional info classes would be implemented here (JavaVersionInfo, ClassAvailabilityInfo,
  // etc.)
  // For brevity, showing just the structure

  /** Information about the Java version and vendor. */
  public static final class JavaVersionInfo {
    private final String fullVersion;
    private final String vendor;
    private final String runtimeName;
    private final int majorVersion;
    private final boolean java23OrHigher;

    /** Creates a new JavaVersionInfo instance.
     *
     * @param fullVersion the full version string
     * @param vendor the vendor name
     * @param runtimeName the runtime name
     * @param majorVersion the major version number
     * @param java23OrHigher whether this is Java 23 or higher
     */
    public JavaVersionInfo(
        final String fullVersion,
        final String vendor,
        final String runtimeName,
        final int majorVersion,
        final boolean java23OrHigher) {
      this.fullVersion = fullVersion;
      this.vendor = vendor;
      this.runtimeName = runtimeName;
      this.majorVersion = majorVersion;
      this.java23OrHigher = java23OrHigher;
    }

    public String getFullVersion() {
      return fullVersion;
    }

    public String getVendor() {
      return vendor;
    }

    public String getRuntimeName() {
      return runtimeName;
    }

    public int getMajorVersion() {
      return majorVersion;
    }

    public boolean isJava23OrHigher() {
      return java23OrHigher;
    }
  }

  // Additional info classes (ClassAvailabilityInfo, NativeAccessInfo, etc.) would be implemented
  // similarly
  // For brevity, showing minimal structure

  /** Information about class availability for Panama features. */
  public static final class ClassAvailabilityInfo {
    private final java.util.List<String> availableClasses;
    private final java.util.List<String> missingClasses;

    private ClassAvailabilityInfo(final Builder builder) {
      this.availableClasses = java.util.List.copyOf(builder.availableClasses);
      this.missingClasses = java.util.List.copyOf(builder.missingClasses);
    }

    public java.util.List<String> getAvailableClasses() {
      return availableClasses;
    }

    public java.util.List<String> getMissingClasses() {
      return missingClasses;
    }

    public boolean areAllClassesAvailable() {
      return missingClasses.isEmpty();
    }

    static final class Builder {
      private final java.util.List<String> availableClasses = new java.util.ArrayList<>();
      private final java.util.List<String> missingClasses = new java.util.ArrayList<>();

      public Builder addAvailableClass(final String className) {
        availableClasses.add(className);
        return this;
      }

      public Builder addMissingClass(final String className) {
        missingClasses.add(className);
        return this;
      }

      public ClassAvailabilityInfo build() {
        return new ClassAvailabilityInfo(this);
      }
    }
  }

  // Stub classes for other info types
  /** Information about native access capabilities. */
  public static final class NativeAccessInfo {
    private final boolean nativeAccessEnabled;
    private final String restrictionReason;
    private final java.util.Map<String, String> properties;

    private NativeAccessInfo(final Builder builder) {
      this.nativeAccessEnabled = builder.nativeAccessEnabled;
      this.restrictionReason = builder.restrictionReason;
      this.properties = java.util.Map.copyOf(builder.properties);
    }

    public boolean isNativeAccessEnabled() {
      return nativeAccessEnabled;
    }

    public String getRestrictionReason() {
      return restrictionReason;
    }

    public java.util.Map<String, String> getProperties() {
      return properties;
    }

    static final class Builder {
      private boolean nativeAccessEnabled = true;
      private String restrictionReason;
      private final java.util.Map<String, String> properties = new java.util.HashMap<>();

      public Builder setNativeAccessEnabled(final boolean enabled) {
        this.nativeAccessEnabled = enabled;
        return this;
      }

      public Builder setRestrictionReason(final String reason) {
        this.restrictionReason = reason;
        return this;
      }

      public Builder addProperty(final String key, final String value) {
        this.properties.put(key, value);
        return this;
      }

      public NativeAccessInfo build() {
        return new NativeAccessInfo(this);
      }
    }
  }

  /** Information about preview features. */
  public static final class PreviewFeatureInfo {
    private final java.util.Map<String, String> previewProperties;

    private PreviewFeatureInfo(final Builder builder) {
      this.previewProperties = java.util.Map.copyOf(builder.previewProperties);
    }

    public java.util.Map<String, String> getPreviewProperties() {
      return previewProperties;
    }

    static final class Builder {
      private final java.util.Map<String, String> previewProperties = new java.util.HashMap<>();

      public Builder addPreviewProperty(final String key, final String value) {
        this.previewProperties.put(key, value);
        return this;
      }

      public PreviewFeatureInfo build() {
        return new PreviewFeatureInfo(this);
      }
    }
  }

  /** Information about functional test results. */
  public static final class FunctionalTestInfo {
    private final java.util.List<String> passingTests;
    private final java.util.Map<String, String> failingTests;
    private final boolean hasNativeAccessIssues;

    private FunctionalTestInfo(final Builder builder) {
      this.passingTests = java.util.List.copyOf(builder.passingTests);
      this.failingTests = java.util.Map.copyOf(builder.failingTests);
      this.hasNativeAccessIssues = builder.hasNativeAccessIssues;
    }

    public java.util.List<String> getPassingTests() {
      return passingTests;
    }

    public java.util.Map<String, String> getFailingTests() {
      return failingTests;
    }

    public boolean hasNativeAccessIssues() {
      return hasNativeAccessIssues;
    }

    public boolean areBasicTestsPassing() {
      return !passingTests.isEmpty() && passingTests.size() > failingTests.size();
    }

    static final class Builder {
      private final java.util.List<String> passingTests = new java.util.ArrayList<>();
      private final java.util.Map<String, String> failingTests = new java.util.HashMap<>();
      private boolean hasNativeAccessIssues = false;

      public Builder addPassingTest(final String testName) {
        this.passingTests.add(testName);
        return this;
      }

      public Builder addFailingTest(final String testName, final String reason) {
        this.failingTests.put(testName, reason);
        return this;
      }

      public Builder setHasNativeAccessIssues(final boolean hasIssues) {
        this.hasNativeAccessIssues = hasIssues;
        return this;
      }

      public FunctionalTestInfo build() {
        return new FunctionalTestInfo(this);
      }
    }
  }
}
