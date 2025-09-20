package ai.tegmentum.wasmtime4j.webassembly;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads and manages official WebAssembly specification test suite and Wasmtime test suite
 * integration. Handles automated downloading, extraction, and organization of test files.
 */
public final class WasmSpecTestDownloader {
  private static final Logger LOGGER = Logger.getLogger(WasmSpecTestDownloader.class.getName());

  // Official WebAssembly specification test suite
  private static final String WASM_SPEC_REPO_URL = "https://github.com/WebAssembly/spec";
  private static final String WASM_SPEC_ZIP_URL =
      WASM_SPEC_REPO_URL + "/archive/refs/heads/main.zip";
  private static final String WASM_SPEC_BRANCH = "main";

  // Wasmtime test suite
  private static final String WASMTIME_REPO_URL = "https://github.com/bytecodealliance/wasmtime";
  private static final String WASMTIME_ZIP_URL = WASMTIME_REPO_URL + "/archive/refs/heads/main.zip";

  // Timeout settings
  private static final int DOWNLOAD_TIMEOUT_SECONDS = 300; // 5 minutes
  private static final int BUFFER_SIZE = 8192;

  private WasmSpecTestDownloader() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if test suite download is enabled via system properties.
   *
   * @return true if test suite download is enabled
   */
  public static boolean isTestSuiteDownloadEnabled() {
    return Boolean.parseBoolean(System.getProperty("wasmtime4j.test.download-suites", "false"));
  }

  /**
   * Downloads test suites automatically if enabled via system properties.
   *
   * @param targetDirectory the directory to extract tests to
   * @throws IOException if download or extraction fails
   */
  public static void downloadTestSuitesIfEnabled(final Path targetDirectory) throws IOException {
    if (isTestSuiteDownloadEnabled()) {
      LOGGER.info("Automatic test suite download enabled");
      downloadAllTestSuites(targetDirectory);
    } else {
      LOGGER.info("Automatic test suite download disabled (use -Dwasmtime4j.test.download-suites=true to enable)");
    }
  }

  /**
   * Downloads and extracts the WebAssembly specification test suite.
   *
   * @param targetDirectory the directory to extract tests to
   * @throws IOException if download or extraction fails
   */
  public static void downloadWebAssemblySpecTests(final Path targetDirectory) throws IOException {
    LOGGER.info("Downloading WebAssembly specification test suite...");

    final Path specTestsDir = targetDirectory.resolve("webassembly-spec");

    // Skip if already exists and is not empty
    if (Files.exists(specTestsDir) && !isDirectoryEmpty(specTestsDir)) {
      LOGGER.info("WebAssembly spec tests already exist at: " + specTestsDir);
      return;
    }

    Files.createDirectories(specTestsDir);

    try {
      // Download the ZIP file
      final byte[] zipData = downloadFromUrl(WASM_SPEC_ZIP_URL);

      // Extract test files from the ZIP
      extractWebAssemblySpecTests(zipData, specTestsDir);

      LOGGER.info(
          "Successfully downloaded and extracted WebAssembly spec tests to: " + specTestsDir);

    } catch (final Exception e) {
      // Clean up on failure
      if (Files.exists(specTestsDir)) {
        deleteDirectory(specTestsDir);
      }
      throw new IOException("Failed to download WebAssembly spec tests", e);
    }
  }

  /**
   * Downloads and extracts the Wasmtime test suite.
   *
   * @param targetDirectory the directory to extract tests to
   * @throws IOException if download or extraction fails
   */
  public static void downloadWasmtimeTests(final Path targetDirectory) throws IOException {
    LOGGER.info("Downloading Wasmtime test suite...");

    final Path wasmtimeTestsDir = targetDirectory.resolve("wasmtime-tests");

    // Skip if already exists and is not empty
    if (Files.exists(wasmtimeTestsDir) && !isDirectoryEmpty(wasmtimeTestsDir)) {
      LOGGER.info("Wasmtime tests already exist at: " + wasmtimeTestsDir);
      return;
    }

    Files.createDirectories(wasmtimeTestsDir);

    try {
      // Download the ZIP file
      final byte[] zipData = downloadFromUrl(WASMTIME_ZIP_URL);

      // Extract test files from the ZIP
      extractWasmtimeTests(zipData, wasmtimeTestsDir);

      LOGGER.info("Successfully downloaded and extracted Wasmtime tests to: " + wasmtimeTestsDir);

    } catch (final Exception e) {
      // Clean up on failure
      if (Files.exists(wasmtimeTestsDir)) {
        deleteDirectory(wasmtimeTestsDir);
      }
      throw new IOException("Failed to download Wasmtime tests", e);
    }
  }

  /**
   * Downloads both WebAssembly spec tests and Wasmtime tests.
   *
   * @param targetDirectory the directory to extract tests to
   * @throws IOException if download or extraction fails
   */
  public static void downloadAllTestSuites(final Path targetDirectory) throws IOException {
    LOGGER.info("Downloading all WebAssembly test suites...");

    Files.createDirectories(targetDirectory);

    // Check system properties for selective downloading
    final String suiteTypesProperty = System.getProperty("wasmtime4j.test.suite-types", "all");
    final boolean downloadAll = "all".equals(suiteTypesProperty);
    final boolean downloadWebAssemblySpec = downloadAll || suiteTypesProperty.contains("webassembly-spec");
    final boolean downloadWasmtimeTests = downloadAll || suiteTypesProperty.contains("wasmtime-tests");

    // Download WebAssembly spec tests
    if (downloadWebAssemblySpec) {
      downloadWebAssemblySpecTests(targetDirectory);
    } else {
      LOGGER.info("Skipping WebAssembly spec tests (not requested in wasmtime4j.test.suite-types)");
    }

    // Download Wasmtime tests
    if (downloadWasmtimeTests) {
      downloadWasmtimeTests(targetDirectory);
    } else {
      LOGGER.info("Skipping Wasmtime tests (not requested in wasmtime4j.test.suite-types)");
    }

    LOGGER.info("Successfully downloaded requested test suites to: " + targetDirectory);
  }

  /**
   * Downloads data from the specified URL.
   *
   * @param url the URL to download from
   * @return the downloaded data as byte array
   * @throws IOException if download fails
   */
  private static byte[] downloadFromUrl(final String url) throws IOException {
    LOGGER.info("Downloading from: " + url);

    final URI uri;
    try {
      uri = new URI(url);
    } catch (final URISyntaxException e) {
      throw new IOException("Invalid URL: " + url, e);
    }

    final HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
    connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(DOWNLOAD_TIMEOUT_SECONDS));
    connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(DOWNLOAD_TIMEOUT_SECONDS));
    connection.setRequestMethod("GET");
    connection.setRequestProperty("User-Agent", "wasmtime4j-test-downloader/1.0");

    final int responseCode = connection.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new IOException("HTTP " + responseCode + " when downloading from: " + url);
    }

    final long contentLength = connection.getContentLengthLong();
    LOGGER.info("Downloading " + (contentLength > 0 ? contentLength + " bytes" : "unknown size"));

    try (final InputStream input = new BufferedInputStream(connection.getInputStream());
        final ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      final byte[] buffer = new byte[BUFFER_SIZE];
      int bytesRead;
      long totalRead = 0;

      while ((bytesRead = input.read(buffer)) != -1) {
        output.write(buffer, 0, bytesRead);
        totalRead += bytesRead;

        if (contentLength > 0 && totalRead % (1024 * 1024) == 0) {
          final long percentComplete = (totalRead * 100) / contentLength;
          LOGGER.info("Downloaded " + percentComplete + "% (" + totalRead + " bytes)");
        }
      }

      LOGGER.info("Download completed: " + totalRead + " bytes");
      return output.toByteArray();

    } finally {
      connection.disconnect();
    }
  }

  /**
   * Extracts WebAssembly specification tests from the downloaded ZIP data.
   *
   * @param zipData the ZIP file data
   * @param targetDirectory the directory to extract to
   * @throws IOException if extraction fails
   */
  private static void extractWebAssemblySpecTests(final byte[] zipData, final Path targetDirectory)
      throws IOException {
    LOGGER.info("Extracting WebAssembly spec tests...");

    int extractedWastFiles = 0;
    int extractedWasmFiles = 0;
    int convertedWatFiles = 0;

    try (final ZipInputStream zipStream =
        new ZipInputStream(new BufferedInputStream(new java.io.ByteArrayInputStream(zipData)))) {

      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        final String entryName = entry.getName();

        // Extract test files from test/core directory (main spec tests)
        if (entryName.contains("/test/core/")
            && (entryName.endsWith(".wast") || entryName.endsWith(".wasm") || entryName.endsWith(".wat"))) {
          final String fileName = getFileName(entryName);
          final Path outputFile = targetDirectory.resolve(fileName);
          Files.createDirectories(outputFile.getParent());

          try (final BufferedOutputStream output =
              new BufferedOutputStream(new FileOutputStream(outputFile.toFile()))) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = zipStream.read(buffer)) != -1) {
              output.write(buffer, 0, bytesRead);
            }
          }

          if (entryName.endsWith(".wast")) {
            extractedWastFiles++;
            // Convert WAST files to WASM if wabt is available
            convertWastFileToWasm(outputFile);
          } else if (entryName.endsWith(".wasm")) {
            extractedWasmFiles++;
          } else if (entryName.endsWith(".wat")) {
            convertedWatFiles++;
            // Convert WAT files to WASM if wabt is available
            convertWatFileToWasm(outputFile);
          }

          if ((extractedWastFiles + extractedWasmFiles + convertedWatFiles) % 50 == 0) {
            LOGGER.info("Extracted "
                + (extractedWastFiles + extractedWasmFiles + convertedWatFiles)
                + " test files...");
          }
        }

        zipStream.closeEntry();
      }
    }

    LOGGER.info("Extracted " + extractedWastFiles + " WAST files, " + extractedWasmFiles + " WASM files, "
        + convertedWatFiles + " WAT files from WebAssembly spec tests");

    // Convert any extracted WAT/WAST files to WASM format
    convertExtractedFilesToWasm(targetDirectory);
  }

  /**
   * Extracts Wasmtime tests from the downloaded ZIP data.
   *
   * @param zipData the ZIP file data
   * @param targetDirectory the directory to extract to
   * @throws IOException if extraction fails
   */
  private static void extractWasmtimeTests(final byte[] zipData, final Path targetDirectory)
      throws IOException {
    LOGGER.info("Extracting Wasmtime tests...");

    int extractedFiles = 0;
    try (final ZipInputStream zipStream =
        new ZipInputStream(new BufferedInputStream(new java.io.ByteArrayInputStream(zipData)))) {

      ZipEntry entry;
      while ((entry = zipStream.getNextEntry()) != null) {
        final String entryName = entry.getName();

        // Extract relevant test files from tests directory
        if (entryName.contains("/tests/")
            && (entryName.endsWith(".wat")
                || entryName.endsWith(".wasm")
                || entryName.endsWith(".wast"))) {
          final Path outputFile = targetDirectory.resolve(getRelativeTestPath(entryName));
          Files.createDirectories(outputFile.getParent());

          try (final BufferedOutputStream output =
              new BufferedOutputStream(new FileOutputStream(outputFile.toFile()))) {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = zipStream.read(buffer)) != -1) {
              output.write(buffer, 0, bytesRead);
            }
          }

          extractedFiles++;
          if (extractedFiles % 50 == 0) {
            LOGGER.info("Extracted " + extractedFiles + " test files...");
          }
        }

        zipStream.closeEntry();
      }
    }

    LOGGER.info("Extracted " + extractedFiles + " Wasmtime test files");
  }

  /**
   * Gets the relative test path from a full ZIP entry path.
   *
   * @param fullPath the full ZIP entry path
   * @return the relative test path
   */
  private static String getRelativeTestPath(final String fullPath) {
    // Find the test directory part and get everything after it
    final int testIndex = fullPath.lastIndexOf("/test/");
    if (testIndex >= 0) {
      return fullPath.substring(testIndex + 6); // +6 to skip "/test/"
    }

    final int testsIndex = fullPath.lastIndexOf("/tests/");
    if (testsIndex >= 0) {
      return fullPath.substring(testsIndex + 7); // +7 to skip "/tests/"
    }

    // Fallback: use just the filename
    final int lastSlash = fullPath.lastIndexOf('/');
    return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
  }

  /**
   * Gets just the filename from a full path.
   *
   * @param fullPath the full path
   * @return the filename
   */
  private static String getFileName(final String fullPath) {
    final int lastSlash = fullPath.lastIndexOf('/');
    return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
  }

  /**
   * Converts a WAT file to WASM format.
   *
   * @param watFile the WAT file to convert
   */
  private static void convertWatFileToWasm(final Path watFile) {
    try {
      final String baseName = watFile.getFileName().toString();
      final String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
      final Path wasmFile = watFile.getParent().resolve(nameWithoutExt + ".wasm");

      if (WatToWasmConverter.convertWatToWasm(watFile, wasmFile)) {
        LOGGER.fine("Converted " + watFile + " to " + wasmFile);
      } else {
        LOGGER.warning("Failed to convert " + watFile + " (wabt not available)");
      }
    } catch (final IOException e) {
      LOGGER.warning("Failed to convert " + watFile + ": " + e.getMessage());
    }
  }

  /**
   * Converts a WAST file to WASM format (treats it as WAT for now).
   *
   * @param wastFile the WAST file to convert
   */
  private static void convertWastFileToWasm(final Path wastFile) {
    // For now, treat WAST files similar to WAT files
    // In a full implementation, this would parse WAST assertions and extract individual modules
    try {
      final String baseName = wastFile.getFileName().toString();
      final String nameWithoutExt = baseName.substring(0, baseName.lastIndexOf('.'));
      final Path wasmFile = wastFile.getParent().resolve(nameWithoutExt + ".wasm");

      // For WAST files, we'll need more sophisticated processing
      // For now, just log that we encountered one
      LOGGER.fine("Found WAST file: " + wastFile + " (complex conversion not yet implemented)");
    } catch (final Exception e) {
      LOGGER.warning("Failed to process WAST file " + wastFile + ": " + e.getMessage());
    }
  }

  /**
   * Converts any extracted WAT/WAST files to WASM format.
   *
   * @param directory the directory containing extracted files
   */
  private static void convertExtractedFilesToWasm(final Path directory) {
    if (!WatToWasmConverter.isWabtAvailable()) {
      LOGGER.warning("wabt not available - WAT/WAST files will not be converted to WASM");
      return;
    }

    try {
      Files.walk(directory)
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".wat"))
          .forEach(WasmSpecTestDownloader::convertWatFileToWasm);
    } catch (final IOException e) {
      LOGGER.warning("Failed to convert WAT files: " + e.getMessage());
    }
  }

  /**
   * Checks if a directory is empty.
   *
   * @param directory the directory to check
   * @return true if the directory is empty or doesn't exist
   * @throws IOException if directory cannot be read
   */
  private static boolean isDirectoryEmpty(final Path directory) throws IOException {
    if (!Files.exists(directory)) {
      return true;
    }

    try (final var files = Files.newDirectoryStream(directory)) {
      return !files.iterator().hasNext();
    }
  }

  /**
   * Recursively deletes a directory and its contents.
   *
   * @param directory the directory to delete
   * @throws IOException if deletion fails
   */
  private static void deleteDirectory(final Path directory) throws IOException {
    if (Files.exists(directory)) {
      Files.walk(directory)
          .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
          .forEach(
              path -> {
                try {
                  Files.delete(path);
                } catch (final IOException e) {
                  LOGGER.warning("Failed to delete: " + path + " - " + e.getMessage());
                }
              });
    }
  }

  /**
   * Validates that test suites are properly downloaded and available.
   *
   * @param testSuiteDirectory the test suite directory
   * @return true if test suites are available
   */
  public static boolean validateTestSuites(final Path testSuiteDirectory) {
    final Path specTestsDir = testSuiteDirectory.resolve("webassembly-spec");
    final Path wasmtimeTestsDir = testSuiteDirectory.resolve("wasmtime-tests");

    try {
      final boolean specExists = Files.exists(specTestsDir) && !isDirectoryEmpty(specTestsDir);
      final boolean wasmtimeExists =
          Files.exists(wasmtimeTestsDir) && !isDirectoryEmpty(wasmtimeTestsDir);

      if (specExists) {
        LOGGER.info("WebAssembly spec tests available at: " + specTestsDir);
      } else {
        LOGGER.warning("WebAssembly spec tests not available at: " + specTestsDir);
      }

      if (wasmtimeExists) {
        LOGGER.info("Wasmtime tests available at: " + wasmtimeTestsDir);
      } else {
        LOGGER.warning("Wasmtime tests not available at: " + wasmtimeTestsDir);
      }

      return specExists || wasmtimeExists;

    } catch (final IOException e) {
      LOGGER.warning("Error validating test suites: " + e.getMessage());
      return false;
    }
  }
}
