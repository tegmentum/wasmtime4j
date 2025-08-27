package ai.tegmentum.wasmtime4j.jni.native;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Utility class for loading native libraries required for JNI operations.
 *
 * <p>This class handles the extraction and loading of platform-specific native libraries from
 * JAR resources. It provides defensive programming to prevent multiple loading attempts and
 * ensures proper resource cleanup.
 *
 * <p>The loader supports automatic platform detection and loads the appropriate native library
 * for the current operating system and architecture.
 */
public final class NativeLibraryLoader {

    private static final Logger LOGGER = Logger.getLogger(NativeLibraryLoader.class.getName());

    /** Flag to track if the native library has been loaded. */
    private static final AtomicBoolean LIBRARY_LOADED = new AtomicBoolean(false);

    /** The name of the native library without platform-specific suffixes. */
    private static final String LIBRARY_NAME = "wasmtime4j";

    /** Prefix for temporary files. */
    private static final String TEMP_FILE_PREFIX = "wasmtime4j-native-";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private NativeLibraryLoader() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Loads the native library required for JNI operations.
     *
     * <p>This method is thread-safe and will only load the library once, even if called
     * multiple times. It automatically detects the current platform and loads the appropriate
     * native library.
     *
     * @throws UnsatisfiedLinkError if the native library cannot be loaded
     * @throws RuntimeException if there's an error during library extraction or loading
     */
    public static void loadLibrary() {
        if (LIBRARY_LOADED.get()) {
            return; // Already loaded
        }

        synchronized (NativeLibraryLoader.class) {
            if (LIBRARY_LOADED.get()) {
                return; // Double-check after acquiring lock
            }

            try {
                // First try to load from system library path
                System.loadLibrary(LIBRARY_NAME);
                LOGGER.info("Loaded native library from system library path: " + LIBRARY_NAME);
                LIBRARY_LOADED.set(true);
                return;
            } catch (final UnsatisfiedLinkError e) {
                LOGGER.fine("Failed to load from system path, trying embedded library: " + e.getMessage());
            }

            // If system loading fails, extract and load from JAR
            try {
                loadFromJar();
                LIBRARY_LOADED.set(true);
            } catch (final Exception e) {
                throw new RuntimeException("Failed to load native library", e);
            }
        }
    }

    /**
     * Checks if the native library has been loaded.
     *
     * @return true if the library is loaded, false otherwise
     */
    public static boolean isLibraryLoaded() {
        return LIBRARY_LOADED.get();
    }

    /**
     * Gets the expected native library resource path for the current platform.
     *
     * @return the resource path to the native library
     * @throws RuntimeException if the current platform is not supported
     */
    public static String getLibraryResourcePath() {
        final String os = detectOperatingSystem();
        final String arch = detectArchitecture();
        final String extension = getLibraryExtension(os);

        return "/natives/" + os + "/" + arch + "/" + LIBRARY_NAME + extension;
    }

    /**
     * Loads the native library by extracting it from the JAR.
     *
     * @throws IOException if there's an error extracting or loading the library
     * @throws UnsatisfiedLinkError if the extracted library cannot be loaded
     */
    private static void loadFromJar() throws IOException {
        final String resourcePath = getLibraryResourcePath();

        try (final InputStream inputStream = NativeLibraryLoader.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Native library not found in JAR: " + resourcePath);
            }

            // Create temporary file for the native library
            final String extension = getLibraryExtension(detectOperatingSystem());
            final Path tempFile = Files.createTempFile(TEMP_FILE_PREFIX, extension);

            // Ensure the temporary file is deleted when the JVM exits
            tempFile.toFile().deleteOnExit();

            // Copy the library from JAR to temporary file
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

            // Set executable permissions on Unix-like systems
            if (!isWindows()) {
                tempFile.toFile().setExecutable(true);
            }

            // Load the library from the temporary file
            System.load(tempFile.toAbsolutePath().toString());
            LOGGER.info("Loaded native library from JAR: " + resourcePath + " -> " + tempFile);
        }
    }

    /**
     * Detects the current operating system.
     *
     * @return the operating system name ("linux", "windows", "macos")
     * @throws RuntimeException if the operating system is not supported
     */
    private static String detectOperatingSystem() {
        final String osName = System.getProperty("os.name").toLowerCase();

        if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("windows")) {
            return "windows";
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return "macos";
        } else {
            throw new RuntimeException("Unsupported operating system: " + osName);
        }
    }

    /**
     * Detects the current CPU architecture.
     *
     * @return the architecture name ("x86_64", "aarch64")
     * @throws RuntimeException if the architecture is not supported
     */
    private static String detectArchitecture() {
        final String archName = System.getProperty("os.arch").toLowerCase();

        if (archName.equals("amd64") || archName.equals("x86_64")) {
            return "x86_64";
        } else if (archName.equals("aarch64") || archName.equals("arm64")) {
            return "aarch64";
        } else {
            throw new RuntimeException("Unsupported architecture: " + archName);
        }
    }

    /**
     * Gets the file extension for native libraries on the given operating system.
     *
     * @param os the operating system name
     * @return the library file extension
     */
    private static String getLibraryExtension(final String os) {
        switch (os) {
            case "linux":
                return ".so";
            case "windows":
                return ".dll";
            case "macos":
                return ".dylib";
            default:
                throw new RuntimeException("Unknown operating system: " + os);
        }
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return true if running on Windows, false otherwise
     */
    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    /**
     * Gets information about the current platform and library loading status.
     *
     * @return a string describing the platform and library status
     */
    public static String getPlatformInfo() {
        final String os = detectOperatingSystem();
        final String arch = detectArchitecture();
        final String resourcePath = getLibraryResourcePath();
        final boolean loaded = isLibraryLoaded();

        return String.format("Platform: %s-%s, Library: %s, Loaded: %s", 
                os, arch, resourcePath, loaded);
    }
}