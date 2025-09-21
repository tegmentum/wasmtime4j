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

package ai.tegmentum.wasmtime4j.platform;

import ai.tegmentum.wasmtime4j.nativeloader.NativeLibraryUtils;
import ai.tegmentum.wasmtime4j.nativeloader.NativeLoader;
import ai.tegmentum.wasmtime4j.nativeloader.PlatformDetector;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Advanced native library version management and fallback system.
 *
 * <p>This class provides sophisticated native library version detection, compatibility checking,
 * and automatic fallback strategies for cross-platform deployment. It handles:
 *
 * <ul>
 *   <li>Native library version detection and validation
 *   <li>Platform-specific compatibility checking
 *   <li>Automatic fallback to compatible versions
 *   <li>Runtime library verification and validation
 *   <li>Performance optimization based on library versions
 * </ul>
 *
 * <p>The version manager ensures robust native library loading across different platforms
 * and deployment scenarios, with graceful degradation when optimal versions are unavailable.
 */
public final class NativeLibraryVersionManager {

    private static final Logger LOGGER = Logger.getLogger(NativeLibraryVersionManager.class.getName());

    /** Pattern for semantic version parsing. */
    private static final Pattern SEMANTIC_VERSION_PATTERN =
        Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

    /** Minimum supported Wasmtime version. */
    private static final SemanticVersion MINIMUM_WASMTIME_VERSION = new SemanticVersion(36, 0, 0);

    /** Recommended Wasmtime version. */
    private static final SemanticVersion RECOMMENDED_WASMTIME_VERSION = new SemanticVersion(36, 0, 2);

    /** Cache for loaded library information. */
    private static final ConcurrentHashMap<String, LibraryVersionInfo> versionCache = new ConcurrentHashMap<>();

    /** Semantic version representation. */
    public static final class SemanticVersion implements Comparable<SemanticVersion> {
        private final int major;
        private final int minor;
        private final int patch;
        private final String preRelease;
        private final String buildMetadata;

        public SemanticVersion(final int major, final int minor, final int patch) {
            this(major, minor, patch, null, null);
        }

        public SemanticVersion(final int major, final int minor, final int patch,
                             final String preRelease, final String buildMetadata) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.preRelease = preRelease;
            this.buildMetadata = buildMetadata;
        }

        public int getMajor() { return major; }
        public int getMinor() { return minor; }
        public int getPatch() { return patch; }
        public Optional<String> getPreRelease() { return Optional.ofNullable(preRelease); }
        public Optional<String> getBuildMetadata() { return Optional.ofNullable(buildMetadata); }

        public boolean isStable() {
            return preRelease == null || preRelease.isEmpty();
        }

        public boolean isCompatibleWith(final SemanticVersion other) {
            // Major version compatibility
            if (this.major != other.major) {
                return false;
            }
            // Minor version backward compatibility
            return this.minor >= other.minor;
        }

        @Override
        public int compareTo(final SemanticVersion other) {
            int result = Integer.compare(this.major, other.major);
            if (result != 0) return result;

            result = Integer.compare(this.minor, other.minor);
            if (result != 0) return result;

            result = Integer.compare(this.patch, other.patch);
            if (result != 0) return result;

            // Handle pre-release versions (stable versions are greater than pre-release)
            if (this.preRelease == null && other.preRelease == null) return 0;
            if (this.preRelease == null) return 1;
            if (other.preRelease == null) return -1;

            return this.preRelease.compareTo(other.preRelease);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            final SemanticVersion that = (SemanticVersion) obj;
            return major == that.major && minor == that.minor && patch == that.patch
                && Objects.equals(preRelease, that.preRelease);
        }

        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, preRelease);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(major).append('.').append(minor).append('.').append(patch);
            if (preRelease != null && !preRelease.isEmpty()) {
                sb.append('-').append(preRelease);
            }
            if (buildMetadata != null && !buildMetadata.isEmpty()) {
                sb.append('+').append(buildMetadata);
            }
            return sb.toString();
        }

        /**
         * Parses a semantic version string.
         *
         * @param version the version string
         * @return the semantic version
         * @throws IllegalArgumentException if the version string is invalid
         */
        public static SemanticVersion parse(final String version) {
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("Version string cannot be null or empty");
            }

            final java.util.regex.Matcher matcher = SEMANTIC_VERSION_PATTERN.matcher(version.trim());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid semantic version format: " + version);
            }

            final int major = Integer.parseInt(matcher.group(1));
            final int minor = Integer.parseInt(matcher.group(2));
            final int patch = Integer.parseInt(matcher.group(3));
            final String preRelease = matcher.group(4);
            final String buildMetadata = matcher.group(5);

            return new SemanticVersion(major, minor, patch, preRelease, buildMetadata);
        }
    }

    /** Comprehensive library version information. */
    public static final class LibraryVersionInfo {
        private final String libraryName;
        private final SemanticVersion version;
        private final SemanticVersion wasmtimeVersion;
        private final PlatformDetector.PlatformInfo platformInfo;
        private final Path libraryPath;
        private final boolean isOptimal;
        private final boolean isCompatible;
        private final List<String> features;
        private final List<String> limitations;
        private final Properties buildInfo;
        private final long buildTimestamp;
        private final String commitHash;

        LibraryVersionInfo(
                final String libraryName,
                final SemanticVersion version,
                final SemanticVersion wasmtimeVersion,
                final PlatformDetector.PlatformInfo platformInfo,
                final Path libraryPath,
                final boolean isOptimal,
                final boolean isCompatible,
                final List<String> features,
                final List<String> limitations,
                final Properties buildInfo,
                final long buildTimestamp,
                final String commitHash) {
            this.libraryName = libraryName;
            this.version = version;
            this.wasmtimeVersion = wasmtimeVersion;
            this.platformInfo = platformInfo;
            this.libraryPath = libraryPath;
            this.isOptimal = isOptimal;
            this.isCompatible = isCompatible;
            this.features = List.copyOf(features);
            this.limitations = List.copyOf(limitations);
            this.buildInfo = new Properties();
            if (buildInfo != null) {
                this.buildInfo.putAll(buildInfo);
            }
            this.buildTimestamp = buildTimestamp;
            this.commitHash = commitHash;
        }

        public String getLibraryName() { return libraryName; }
        public SemanticVersion getVersion() { return version; }
        public SemanticVersion getWasmtimeVersion() { return wasmtimeVersion; }
        public PlatformDetector.PlatformInfo getPlatformInfo() { return platformInfo; }
        public Optional<Path> getLibraryPath() { return Optional.ofNullable(libraryPath); }
        public boolean isOptimal() { return isOptimal; }
        public boolean isCompatible() { return isCompatible; }
        public List<String> getFeatures() { return features; }
        public List<String> getLimitations() { return limitations; }
        public Properties getBuildInfo() { return new Properties(buildInfo); }
        public long getBuildTimestamp() { return buildTimestamp; }
        public Optional<String> getCommitHash() { return Optional.ofNullable(commitHash); }

        public boolean hasFeature(final String feature) {
            return features.contains(feature);
        }

        public boolean hasLimitation(final String limitation) {
            return limitations.contains(limitation);
        }

        @Override
        public String toString() {
            return String.format(
                "LibraryVersionInfo{name=%s, version=%s, wasmtime=%s, platform=%s, optimal=%s, compatible=%s}",
                libraryName, version, wasmtimeVersion, platformInfo.getPlatformId(), isOptimal, isCompatible);
        }
    }

    /** Library loading strategy. */
    public enum LoadingStrategy {
        /** Prefer the newest compatible version */
        PREFER_NEWEST,
        /** Prefer the most stable version (no pre-release) */
        PREFER_STABLE,
        /** Prefer the optimal version for the platform */
        PREFER_OPTIMAL,
        /** Use strict version matching */
        STRICT_VERSION,
        /** Use any compatible version as fallback */
        FALLBACK_COMPATIBLE
    }

    /** Version compatibility result. */
    public static final class CompatibilityResult {
        private final boolean isCompatible;
        private final List<String> issues;
        private final List<String> recommendations;
        private final SemanticVersion minimumVersion;
        private final SemanticVersion recommendedVersion;

        CompatibilityResult(
                final boolean isCompatible,
                final List<String> issues,
                final List<String> recommendations,
                final SemanticVersion minimumVersion,
                final SemanticVersion recommendedVersion) {
            this.isCompatible = isCompatible;
            this.issues = List.copyOf(issues);
            this.recommendations = List.copyOf(recommendations);
            this.minimumVersion = minimumVersion;
            this.recommendedVersion = recommendedVersion;
        }

        public boolean isCompatible() { return isCompatible; }
        public List<String> getIssues() { return issues; }
        public List<String> getRecommendations() { return recommendations; }
        public SemanticVersion getMinimumVersion() { return minimumVersion; }
        public SemanticVersion getRecommendedVersion() { return recommendedVersion; }
    }

    /** Private constructor to prevent instantiation of utility class. */
    private NativeLibraryVersionManager() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Loads a native library with advanced version management and fallback.
     *
     * @param libraryName the library name
     * @param strategy the loading strategy
     * @return the library version info
     * @throws IllegalStateException if no compatible library can be loaded
     */
    public static LibraryVersionInfo loadLibraryWithVersionManagement(
            final String libraryName, final LoadingStrategy strategy) {

        // Check cache first
        final String cacheKey = libraryName + "_" + strategy.name();
        LibraryVersionInfo cached = versionCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        LOGGER.info(String.format("Loading library %s with strategy %s", libraryName, strategy));

        // Discover available versions
        final List<LibraryVersionInfo> availableVersions = discoverAvailableVersions(libraryName);
        if (availableVersions.isEmpty()) {
            throw new IllegalStateException("No versions of library " + libraryName + " found");
        }

        // Select best version based on strategy
        final LibraryVersionInfo selectedVersion = selectBestVersion(availableVersions, strategy);

        // Validate compatibility
        final CompatibilityResult compatibility = validateCompatibility(selectedVersion);
        if (!compatibility.isCompatible()) {
            LOGGER.warning(String.format("Selected library version has compatibility issues: %s",
                compatibility.getIssues()));
        }

        // Attempt to load the library
        try {
            final NativeLibraryUtils.LibraryLoadInfo loadInfo = NativeLoader.loadLibrary(libraryName);
            if (!loadInfo.isSuccessful()) {
                throw new RuntimeException("Failed to load library: " + loadInfo.getErrorMessage());
            }

            LOGGER.info(String.format("Successfully loaded library: %s", selectedVersion));

            // Cache the result
            versionCache.put(cacheKey, selectedVersion);

            return selectedVersion;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load selected library version: " + selectedVersion, e);
            throw new IllegalStateException("Failed to load library " + libraryName, e);
        }
    }

    /**
     * Loads a library using the default strategy (prefer optimal).
     *
     * @param libraryName the library name
     * @return the library version info
     */
    public static LibraryVersionInfo loadLibraryWithVersionManagement(final String libraryName) {
        return loadLibraryWithVersionManagement(libraryName, LoadingStrategy.PREFER_OPTIMAL);
    }

    /**
     * Validates compatibility of a library version.
     *
     * @param versionInfo the version info to validate
     * @return the compatibility result
     */
    public static CompatibilityResult validateCompatibility(final LibraryVersionInfo versionInfo) {
        final List<String> issues = new ArrayList<>();
        final List<String> recommendations = new ArrayList<>();

        // Check minimum version requirement
        if (versionInfo.getWasmtimeVersion().compareTo(MINIMUM_WASMTIME_VERSION) < 0) {
            issues.add(String.format("Wasmtime version %s is below minimum required %s",
                versionInfo.getWasmtimeVersion(), MINIMUM_WASMTIME_VERSION));
        }

        // Check if version is optimal
        if (!versionInfo.isOptimal()) {
            recommendations.add("Consider upgrading to an optimal version for better performance");
        }

        // Check for limitations
        if (!versionInfo.getLimitations().isEmpty()) {
            for (final String limitation : versionInfo.getLimitations()) {
                issues.add("Limitation: " + limitation);
            }
        }

        // Check platform compatibility
        final PlatformDetector.PlatformInfo currentPlatform = PlatformDetector.detect();
        if (!versionInfo.getPlatformInfo().equals(currentPlatform)) {
            issues.add(String.format("Platform mismatch: library built for %s, running on %s",
                versionInfo.getPlatformInfo().getPlatformId(), currentPlatform.getPlatformId()));
        }

        // Recommend upgrade if not using latest recommended
        if (versionInfo.getWasmtimeVersion().compareTo(RECOMMENDED_WASMTIME_VERSION) < 0) {
            recommendations.add(String.format("Consider upgrading to Wasmtime %s for latest features",
                RECOMMENDED_WASMTIME_VERSION));
        }

        final boolean isCompatible = issues.stream().noneMatch(issue ->
            issue.contains("below minimum") || issue.contains("Platform mismatch"));

        return new CompatibilityResult(isCompatible, issues, recommendations,
            MINIMUM_WASMTIME_VERSION, RECOMMENDED_WASMTIME_VERSION);
    }

    /**
     * Discovers all available versions of a library.
     *
     * @param libraryName the library name
     * @return list of available versions
     */
    public static List<LibraryVersionInfo> discoverAvailableVersions(final String libraryName) {
        final List<LibraryVersionInfo> versions = new ArrayList<>();
        final PlatformDetector.PlatformInfo currentPlatform = PlatformDetector.detect();

        // Check the current library (if loaded)
        try {
            final LibraryVersionInfo currentVersion = getCurrentLibraryVersion(libraryName, currentPlatform);
            if (currentVersion != null) {
                versions.add(currentVersion);
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not get current library version", e);
        }

        // Check for bundled versions in resources
        versions.addAll(discoverBundledVersions(libraryName, currentPlatform));

        // Check system library paths
        versions.addAll(discoverSystemVersions(libraryName, currentPlatform));

        // Sort by version (newest first)
        versions.sort(Comparator.comparing(LibraryVersionInfo::getVersion).reversed());

        LOGGER.fine(String.format("Discovered %d versions of library %s", versions.size(), libraryName));
        return versions;
    }

    /**
     * Gets the currently loaded library version.
     *
     * @param libraryName the library name
     * @param platformInfo the platform info
     * @return the current version info, or null if not loaded
     */
    private static LibraryVersionInfo getCurrentLibraryVersion(
            final String libraryName, final PlatformDetector.PlatformInfo platformInfo) {

        // Try to load version information from the library itself
        final Properties versionProps = loadVersionProperties(libraryName);
        if (versionProps.isEmpty()) {
            return null;
        }

        try {
            final SemanticVersion version = SemanticVersion.parse(
                versionProps.getProperty("wasmtime4j.version", "0.0.0"));
            final SemanticVersion wasmtimeVersion = SemanticVersion.parse(
                versionProps.getProperty("wasmtime.version", "0.0.0"));

            final List<String> features = parseFeatureList(versionProps.getProperty("features", ""));
            final List<String> limitations = parseFeatureList(versionProps.getProperty("limitations", ""));

            final long buildTimestamp = Long.parseLong(versionProps.getProperty("build.timestamp", "0"));
            final String commitHash = versionProps.getProperty("git.commit.hash");

            final boolean isOptimal = isOptimalVersion(version, wasmtimeVersion, platformInfo);
            final boolean isCompatible = wasmtimeVersion.compareTo(MINIMUM_WASMTIME_VERSION) >= 0;

            return new LibraryVersionInfo(
                libraryName, version, wasmtimeVersion, platformInfo, null,
                isOptimal, isCompatible, features, limitations, versionProps, buildTimestamp, commitHash);

        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not parse version information", e);
            return null;
        }
    }

    /**
     * Discovers bundled versions in JAR resources.
     *
     * @param libraryName the library name
     * @param platformInfo the platform info
     * @return list of bundled versions
     */
    private static List<LibraryVersionInfo> discoverBundledVersions(
            final String libraryName, final PlatformDetector.PlatformInfo platformInfo) {

        final List<LibraryVersionInfo> versions = new ArrayList<>();

        // Check for the standard bundled version
        final String resourcePath = platformInfo.getLibraryResourcePath(libraryName);
        try (InputStream resourceStream = NativeLibraryVersionManager.class.getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                // This is the current bundled version
                final LibraryVersionInfo bundledVersion = getCurrentLibraryVersion(libraryName, platformInfo);
                if (bundledVersion != null) {
                    versions.add(bundledVersion);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Could not check bundled version", e);
        }

        return versions;
    }

    /**
     * Discovers versions in system library paths.
     *
     * @param libraryName the library name
     * @param platformInfo the platform info
     * @return list of system versions
     */
    private static List<LibraryVersionInfo> discoverSystemVersions(
            final String libraryName, final PlatformDetector.PlatformInfo platformInfo) {

        final List<LibraryVersionInfo> versions = new ArrayList<>();

        // Check common system library paths
        final List<String> systemPaths = getSystemLibraryPaths(platformInfo);
        final String libraryFileName = platformInfo.getLibraryFileName(libraryName);

        for (final String systemPath : systemPaths) {
            try {
                final Path libraryPath = Paths.get(systemPath, libraryFileName);
                if (Files.exists(libraryPath)) {
                    // Try to determine version from the file
                    final LibraryVersionInfo systemVersion = analyzeSystemLibrary(
                        libraryName, libraryPath, platformInfo);
                    if (systemVersion != null) {
                        versions.add(systemVersion);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Could not analyze system library at " + systemPath, e);
            }
        }

        return versions;
    }

    /**
     * Gets system library search paths for the platform.
     *
     * @param platformInfo the platform info
     * @return list of system paths
     */
    private static List<String> getSystemLibraryPaths(final PlatformDetector.PlatformInfo platformInfo) {
        final List<String> paths = new ArrayList<>();

        switch (platformInfo.getOperatingSystem()) {
            case LINUX:
                paths.add("/usr/lib");
                paths.add("/usr/local/lib");
                paths.add("/lib");
                if (platformInfo.getArchitecture() == PlatformDetector.Architecture.X86_64) {
                    paths.add("/usr/lib/x86_64-linux-gnu");
                    paths.add("/usr/lib64");
                } else if (platformInfo.getArchitecture() == PlatformDetector.Architecture.AARCH64) {
                    paths.add("/usr/lib/aarch64-linux-gnu");
                }
                break;

            case MACOS:
                paths.add("/usr/lib");
                paths.add("/usr/local/lib");
                paths.add("/opt/homebrew/lib");
                paths.add("/opt/local/lib");
                break;

            case WINDOWS:
                paths.add("C:\\Windows\\System32");
                paths.add("C:\\Windows\\SysWOW64");
                final String programFiles = System.getenv("ProgramFiles");
                if (programFiles != null) {
                    paths.add(programFiles);
                }
                break;
        }

        return paths;
    }

    /**
     * Analyzes a system library file.
     *
     * @param libraryName the library name
     * @param libraryPath the library path
     * @param platformInfo the platform info
     * @return the version info, or null if analysis fails
     */
    private static LibraryVersionInfo analyzeSystemLibrary(
            final String libraryName, final Path libraryPath, final PlatformDetector.PlatformInfo platformInfo) {

        // For now, create a basic version info for system libraries
        // In a real implementation, this would parse the library binary for version information

        final SemanticVersion defaultVersion = new SemanticVersion(1, 0, 0);
        final SemanticVersion defaultWasmtimeVersion = MINIMUM_WASMTIME_VERSION;

        return new LibraryVersionInfo(
            libraryName, defaultVersion, defaultWasmtimeVersion, platformInfo, libraryPath,
            false, // System libraries are not considered optimal
            true,  // Assume compatible
            Collections.emptyList(), // No features detected
            List.of("System library - version detection limited"), // Limitation
            new Properties(), // No build info
            0, // No build timestamp
            null); // No commit hash
    }

    /**
     * Selects the best version based on the loading strategy.
     *
     * @param availableVersions the available versions
     * @param strategy the loading strategy
     * @return the selected version
     */
    private static LibraryVersionInfo selectBestVersion(
            final List<LibraryVersionInfo> availableVersions, final LoadingStrategy strategy) {

        if (availableVersions.isEmpty()) {
            throw new IllegalArgumentException("No versions available for selection");
        }

        switch (strategy) {
            case PREFER_NEWEST:
                return availableVersions.stream()
                    .filter(LibraryVersionInfo::isCompatible)
                    .max(Comparator.comparing(LibraryVersionInfo::getVersion))
                    .orElse(availableVersions.get(0));

            case PREFER_STABLE:
                return availableVersions.stream()
                    .filter(LibraryVersionInfo::isCompatible)
                    .filter(v -> v.getVersion().isStable())
                    .max(Comparator.comparing(LibraryVersionInfo::getVersion))
                    .orElse(availableVersions.stream()
                        .filter(LibraryVersionInfo::isCompatible)
                        .max(Comparator.comparing(LibraryVersionInfo::getVersion))
                        .orElse(availableVersions.get(0)));

            case PREFER_OPTIMAL:
                return availableVersions.stream()
                    .filter(LibraryVersionInfo::isCompatible)
                    .filter(LibraryVersionInfo::isOptimal)
                    .max(Comparator.comparing(LibraryVersionInfo::getVersion))
                    .orElse(availableVersions.stream()
                        .filter(LibraryVersionInfo::isCompatible)
                        .max(Comparator.comparing(LibraryVersionInfo::getVersion))
                        .orElse(availableVersions.get(0)));

            case STRICT_VERSION:
                // For strict version, prefer exact matches with recommended version
                return availableVersions.stream()
                    .filter(v -> v.getWasmtimeVersion().equals(RECOMMENDED_WASMTIME_VERSION))
                    .findFirst()
                    .orElse(availableVersions.stream()
                        .filter(LibraryVersionInfo::isCompatible)
                        .findFirst()
                        .orElse(availableVersions.get(0)));

            case FALLBACK_COMPATIBLE:
                return availableVersions.stream()
                    .filter(LibraryVersionInfo::isCompatible)
                    .findFirst()
                    .orElse(availableVersions.get(0));

            default:
                return availableVersions.get(0);
        }
    }

    /**
     * Loads version properties from resources.
     *
     * @param libraryName the library name
     * @return the version properties
     */
    private static Properties loadVersionProperties(final String libraryName) {
        final Properties props = new Properties();

        // Try to load from standard location
        final String resourcePath = "/META-INF/" + libraryName + "-version.properties";
        try (InputStream stream = NativeLibraryVersionManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                props.load(stream);
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "Could not load version properties from " + resourcePath, e);
        }

        return props;
    }

    /**
     * Parses a comma-separated feature list.
     *
     * @param featureString the feature string
     * @return list of features
     */
    private static List<String> parseFeatureList(final String featureString) {
        if (featureString == null || featureString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> features = new ArrayList<>();
        for (final String feature : featureString.split(",")) {
            final String trimmed = feature.trim();
            if (!trimmed.isEmpty()) {
                features.add(trimmed);
            }
        }
        return features;
    }

    /**
     * Determines if a version is optimal for the platform.
     *
     * @param version the library version
     * @param wasmtimeVersion the Wasmtime version
     * @param platformInfo the platform info
     * @return true if optimal
     */
    private static boolean isOptimalVersion(
            final SemanticVersion version, final SemanticVersion wasmtimeVersion,
            final PlatformDetector.PlatformInfo platformInfo) {

        // Consider a version optimal if:
        // 1. It uses the recommended Wasmtime version
        // 2. It's a stable release
        // 3. It matches the current platform exactly

        return wasmtimeVersion.equals(RECOMMENDED_WASMTIME_VERSION)
            && version.isStable()
            && platformInfo.equals(PlatformDetector.detect());
    }

    /**
     * Clears the version cache.
     */
    public static void clearVersionCache() {
        versionCache.clear();
        LOGGER.fine("Cleared version cache");
    }

    /**
     * Gets diagnostic information about loaded libraries.
     *
     * @return diagnostic information
     */
    public static String getDiagnosticInfo() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Native Library Version Manager:\n");
        sb.append("  Minimum Wasmtime version: ").append(MINIMUM_WASMTIME_VERSION).append("\n");
        sb.append("  Recommended Wasmtime version: ").append(RECOMMENDED_WASMTIME_VERSION).append("\n");
        sb.append("  Cached library versions: ").append(versionCache.size()).append("\n");

        if (!versionCache.isEmpty()) {
            sb.append("  Cached versions:\n");
            versionCache.forEach((key, version) ->
                sb.append("    ").append(key).append(": ").append(version).append("\n"));
        }

        return sb.toString();
    }
}