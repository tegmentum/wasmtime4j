package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive test suite for WASI Preview 1 operations.
 *
 * <p>This test validates complete WASI Preview 1 functionality including:
 *
 * <ul>
 *   <li>File descriptor operations (fd_read, fd_write, fd_close)
 *   <li>Path operations (path_open, path_create_directory, path_unlink)
 *   <li>Environment variable access (environ_get, environ_sizes_get)
 *   <li>Process operations (proc_exit, proc_raise)
 *   <li>Clock operations (clock_time_get, clock_res_get)
 *   <li>Random number generation (random_get)
 *   <li>Vectored I/O operations
 *   <li>Error handling and edge cases
 * </ul>
 */
class WasiPreview1OperationsTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiPreview1Operations wasiOps;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    System.out.println("Setting up WASI Preview 1 test: " + testInfo.getDisplayName());

    // Create comprehensive WASI context
    wasiContext =
        WasiContext.builder()
            .withEnvironment("WASI_TEST_ENV", "preview1_test")
            .withEnvironment("TEST_VAR", "test_value")
            .withEnvironment("NUMERIC_VAR", "12345")
            .withEnvironment("UNICODE_VAR", "测试值")
            .withArgument("wasi_test")
            .withArgument("--preview1")
            .withArgument("--verbose")
            .withPreopenDirectory("/tmp", tempDirectory.toString())
            .withWorkingDirectory(tempDirectory.toString())
            .build();

    assertNotNull(wasiContext, "WASI context must be created successfully");

    wasiOps = new WasiPreview1Operations(wasiContext);
    assertNotNull(wasiOps, "WASI Preview 1 operations must be initialized");

    System.out.println("WASI Preview 1 test setup completed");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    System.out.println("Tearing down WASI Preview 1 test: " + testInfo.getDisplayName());

    try {
      if (wasiOps != null) {
        wasiOps.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing WASI operations: " + e.getMessage());
    }

    try {
      if (wasiContext != null) {
        wasiContext.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing WASI context: " + e.getMessage());
    }

    System.out.println("WASI Preview 1 test teardown completed");
  }

  @Test
  void testFileDescriptorReadWriteOperations() throws IOException {
    // Create test file with known content
    final Path testFile = tempDirectory.resolve("fd_test.txt");
    final String originalContent = "Hello, WASI Preview 1 file operations!";
    Files.write(testFile, originalContent.getBytes(), StandardOpenOption.CREATE);

    // Test fd_open (path_open)
    final int fd =
        wasiOps.pathOpen(
            3, // preopen fd for /tmp
            0, // flags
            "/tmp/fd_test.txt",
            0, // oflags
            WasiRights.FD_READ.getValue() | WasiRights.FD_WRITE.getValue(),
            0L, // rights_inheriting
            0 // fdflags
            );
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test fd_read
    final List<ByteBuffer> readIovs = new ArrayList<>();
    readIovs.add(ByteBuffer.allocate(originalContent.length()));
    final int bytesRead = wasiOps.fdRead(fd, readIovs);
    assertEquals(originalContent.length(), bytesRead, "Must read all bytes");

    final String readContent = new String(readIovs.get(0).array()).trim();
    assertEquals(originalContent, readContent, "Read content must match original");

    // Test fd_write
    final String writeContent = "\nAppended content via WASI fd_write";
    final List<ByteBuffer> writeIovs = new ArrayList<>();
    writeIovs.add(ByteBuffer.wrap(writeContent.getBytes()));
    final int bytesWritten = wasiOps.fdWrite(fd, writeIovs);
    assertEquals(writeContent.length(), bytesWritten, "Must write all bytes");

    // Test fd_sync
    assertDoesNotThrow(() -> wasiOps.fdSync(fd), "fd_sync must not throw");

    // Test fd_close
    assertDoesNotThrow(() -> wasiOps.fdClose(fd), "fd_close must not throw");

    // Verify file content after operations
    final String finalContent = Files.readString(testFile);
    assertTrue(finalContent.contains(originalContent), "File must contain original content");
    assertTrue(finalContent.contains(writeContent.trim()), "File must contain written content");
  }

  @Test
  void testVectoredInputOutputOperations() throws IOException {
    final Path testFile = tempDirectory.resolve("vectored_io_test.txt");

    // Create file for vectored I/O testing
    final int fd =
        wasiOps.pathOpen(
            3, // preopen fd for /tmp
            0, // flags
            "/tmp/vectored_io_test.txt",
            WasiOpenFlags.CREAT.getValue() | WasiOpenFlags.TRUNC.getValue(),
            WasiRights.FD_READ.getValue() | WasiRights.FD_WRITE.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test vectored write (multiple buffers)
    final List<ByteBuffer> writeIovs = new ArrayList<>();
    writeIovs.add(ByteBuffer.wrap("First chunk ".getBytes()));
    writeIovs.add(ByteBuffer.wrap("Second chunk ".getBytes()));
    writeIovs.add(ByteBuffer.wrap("Third chunk\n".getBytes()));

    final int totalWriteSize = writeIovs.stream().mapToInt(ByteBuffer::remaining).sum();
    final int bytesWritten = wasiOps.fdWrite(fd, writeIovs);
    assertEquals(totalWriteSize, bytesWritten, "Must write all vectored data");

    // Reset file position for reading
    wasiOps.fdSeek(fd, 0, WasiWhence.SET.getValue());

    // Test vectored read (multiple buffers)
    final List<ByteBuffer> readIovs = new ArrayList<>();
    readIovs.add(ByteBuffer.allocate(12)); // "First chunk "
    readIovs.add(ByteBuffer.allocate(13)); // "Second chunk "
    readIovs.add(ByteBuffer.allocate(12)); // "Third chunk\n"

    final int bytesRead = wasiOps.fdRead(fd, readIovs);
    assertEquals(totalWriteSize, bytesRead, "Must read all vectored data");

    // Verify vectored read results
    assertEquals("First chunk ", new String(readIovs.get(0).array()));
    assertEquals("Second chunk ", new String(readIovs.get(1).array()));
    assertEquals("Third chunk\n", new String(readIovs.get(2).array()));

    wasiOps.fdClose(fd);
  }

  @Test
  void testEnvironmentVariableOperations() {
    // Test environ_sizes_get
    final int[] sizes = wasiOps.environSizesGet();
    assertNotNull(sizes, "Environment sizes must not be null");
    assertEquals(2, sizes.length, "Must return count and buffer size");
    assertTrue(sizes[0] > 0, "Environment variable count must be positive");
    assertTrue(sizes[1] > 0, "Environment buffer size must be positive");

    // Test environ_get
    final String[] environment = wasiOps.environGet();
    assertNotNull(environment, "Environment variables must not be null");
    assertTrue(environment.length >= 4, "Must have at least 4 test environment variables");

    // Verify specific environment variables
    final Map<String, String> envMap = new HashMap<>();
    for (final String env : environment) {
      final String[] parts = env.split("=", 2);
      if (parts.length == 2) {
        envMap.put(parts[0], parts[1]);
      }
    }

    assertEquals("preview1_test", envMap.get("WASI_TEST_ENV"), "WASI_TEST_ENV must be correct");
    assertEquals("test_value", envMap.get("TEST_VAR"), "TEST_VAR must be correct");
    assertEquals("12345", envMap.get("NUMERIC_VAR"), "NUMERIC_VAR must be correct");
    assertEquals("测试值", envMap.get("UNICODE_VAR"), "UNICODE_VAR must support Unicode");

    System.out.println("Environment variables validated: " + envMap.size());
  }

  @Test
  void testArgumentsAccess() {
    // Test args_sizes_get
    final int[] sizes = wasiOps.argsSizesGet();
    assertNotNull(sizes, "Arguments sizes must not be null");
    assertEquals(2, sizes.length, "Must return count and buffer size");
    assertEquals(3, sizes[0], "Must have 3 arguments");
    assertTrue(sizes[1] > 0, "Arguments buffer size must be positive");

    // Test args_get
    final String[] arguments = wasiOps.argsGet();
    assertNotNull(arguments, "Arguments must not be null");
    assertEquals(3, arguments.length, "Must have exactly 3 arguments");
    assertEquals("wasi_test", arguments[0], "First argument must be program name");
    assertEquals("--preview1", arguments[1], "Second argument must be --preview1");
    assertEquals("--verbose", arguments[2], "Third argument must be --verbose");

    System.out.println("Arguments validated: " + String.join(", ", arguments));
  }

  @Test
  void testClockOperations() {
    // Test clock_time_get for realtime clock
    final long realtimeNanos = wasiOps.clockTimeGet(WasiClockId.REALTIME.getValue(), 1000);
    assertTrue(realtimeNanos > 0, "Realtime clock must return positive nanoseconds");

    final long currentTimeMillis = System.currentTimeMillis();
    final long wasiTimeMillis = realtimeNanos / 1_000_000;
    final long timeDifference = Math.abs(currentTimeMillis - wasiTimeMillis);
    assertTrue(
        timeDifference < 10000, // 10 seconds tolerance
        "WASI time must be close to system time");

    // Test clock_time_get for monotonic clock
    final long monotonic1 = wasiOps.clockTimeGet(WasiClockId.MONOTONIC.getValue(), 1000);
    assertTrue(monotonic1 > 0, "Monotonic clock must return positive value");

    // Sleep briefly and check monotonic clock again
    try {
      Thread.sleep(10);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    final long monotonic2 = wasiOps.clockTimeGet(WasiClockId.MONOTONIC.getValue(), 1000);
    assertTrue(monotonic2 >= monotonic1, "Monotonic clock must be non-decreasing");

    // Test clock_res_get
    final long realtimeResolution = wasiOps.clockResGet(WasiClockId.REALTIME.getValue());
    assertTrue(realtimeResolution > 0, "Realtime clock resolution must be positive");

    final long monotonicResolution = wasiOps.clockResGet(WasiClockId.MONOTONIC.getValue());
    assertTrue(monotonicResolution > 0, "Monotonic clock resolution must be positive");

    System.out.printf(
        "Clock validation: realtime=%dns, monotonic=%dns, res_realtime=%dns, res_monotonic=%dns%n",
        realtimeNanos, monotonic2, realtimeResolution, monotonicResolution);
  }

  @Test
  void testRandomNumberGeneration() {
    // Test random_get with different buffer sizes
    final byte[] small = wasiOps.randomGet(8);
    assertNotNull(small, "Small random buffer must not be null");
    assertEquals(8, small.length, "Small random buffer must have correct size");

    final byte[] medium = wasiOps.randomGet(256);
    assertNotNull(medium, "Medium random buffer must not be null");
    assertEquals(256, medium.length, "Medium random buffer must have correct size");

    final byte[] large = wasiOps.randomGet(4096);
    assertNotNull(large, "Large random buffer must not be null");
    assertEquals(4096, large.length, "Large random buffer must have correct size");

    // Test randomness quality (basic statistical test)
    final byte[] randomData = wasiOps.randomGet(1000);
    int zeros = 0;
    int ones = 0;
    for (final byte b : randomData) {
      for (int bit = 0; bit < 8; bit++) {
        if ((b & (1 << bit)) != 0) {
          ones++;
        } else {
          zeros++;
        }
      }
    }

    final double ratio = (double) ones / (zeros + ones);
    assertTrue(ratio > 0.4 && ratio < 0.6, "Random data must have reasonable bit distribution");

    // Verify different calls produce different data
    final byte[] random1 = wasiOps.randomGet(32);
    final byte[] random2 = wasiOps.randomGet(32);
    assertFalse(
        java.util.Arrays.equals(random1, random2), "Random calls must produce different data");

    System.out.printf(
        "Random generation validated: %d zeros, %d ones, ratio=%.3f%n", zeros, ones, ratio);
  }

  @Test
  void testPathOperations() throws IOException {
    // Test path_create_directory
    assertDoesNotThrow(
        () -> wasiOps.pathCreateDirectory(3, "/tmp/wasi_test_dir"),
        "Directory creation must not throw");

    final Path createdDir = tempDirectory.resolve("wasi_test_dir");
    assertTrue(Files.exists(createdDir), "Directory must be created");
    assertTrue(Files.isDirectory(createdDir), "Created path must be a directory");

    // Test path_open for directory
    final int dirFd =
        wasiOps.pathOpen(
            3, // preopen fd
            0, // flags
            "/tmp/wasi_test_dir",
            0, // oflags
            WasiRights.PATH_CREATE_FILE.getValue() | WasiRights.PATH_OPEN.getValue(),
            0L,
            0);
    assertTrue(dirFd >= 0, "Directory file descriptor must be valid");

    // Create file in directory
    final int fileFd =
        wasiOps.pathOpen(
            dirFd,
            0,
            "test_file.txt",
            WasiOpenFlags.CREAT.getValue(),
            WasiRights.FD_WRITE.getValue() | WasiRights.FD_READ.getValue(),
            0L,
            0);
    assertTrue(fileFd >= 0, "File descriptor for new file must be valid");

    // Write to file
    final String fileContent = "Content in subdirectory file";
    final List<ByteBuffer> writeIovs = new ArrayList<>();
    writeIovs.add(ByteBuffer.wrap(fileContent.getBytes()));
    final int bytesWritten = wasiOps.fdWrite(fileFd, writeIovs);
    assertEquals(fileContent.length(), bytesWritten, "Must write all file content");

    wasiOps.fdClose(fileFd);

    // Test path_unlink_file
    assertDoesNotThrow(
        () -> wasiOps.pathUnlinkFile(dirFd, "test_file.txt"), "File unlink must not throw");

    final Path unlinkedFile = createdDir.resolve("test_file.txt");
    assertFalse(Files.exists(unlinkedFile), "File must be unlinked");

    wasiOps.fdClose(dirFd);

    // Test path_remove_directory
    assertDoesNotThrow(
        () -> wasiOps.pathRemoveDirectory(3, "/tmp/wasi_test_dir"),
        "Directory removal must not throw");

    assertFalse(Files.exists(createdDir), "Directory must be removed");
  }

  @Test
  void testFileSeekAndTell() throws IOException {
    final Path testFile = tempDirectory.resolve("seek_test.txt");
    final String content = "0123456789ABCDEFGHIJ";
    Files.write(testFile, content.getBytes(), StandardOpenOption.CREATE);

    final int fd =
        wasiOps.pathOpen(
            3,
            0,
            "/tmp/seek_test.txt",
            0,
            WasiRights.FD_READ.getValue()
                | WasiRights.FD_SEEK.getValue()
                | WasiRights.FD_TELL.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test initial position (should be 0)
    long position = wasiOps.fdTell(fd);
    assertEquals(0, position, "Initial position must be 0");

    // Test seeking to middle
    position = wasiOps.fdSeek(fd, 10, WasiWhence.SET.getValue());
    assertEquals(10, position, "Seek SET to 10 must return position 10");

    // Read from position 10
    final List<ByteBuffer> readIovs = new ArrayList<>();
    readIovs.add(ByteBuffer.allocate(5));
    final int bytesRead = wasiOps.fdRead(fd, readIovs);
    assertEquals(5, bytesRead, "Must read 5 bytes from position 10");
    assertEquals("ABCDE", new String(readIovs.get(0).array()), "Must read correct content");

    // Test current position after read
    position = wasiOps.fdTell(fd);
    assertEquals(15, position, "Position after read must be 15");

    // Test relative seek
    position = wasiOps.fdSeek(fd, -5, WasiWhence.CUR.getValue());
    assertEquals(10, position, "Relative seek -5 from 15 must be 10");

    // Test seek from end
    position = wasiOps.fdSeek(fd, -5, WasiWhence.END.getValue());
    assertEquals(content.length() - 5, position, "Seek from end must work correctly");

    wasiOps.fdClose(fd);
  }

  @Test
  void testFileStatusAndMetadata() throws IOException {
    final Path testFile = tempDirectory.resolve("stat_test.txt");
    final String content = "File status and metadata test content";
    Files.write(testFile, content.getBytes(), StandardOpenOption.CREATE);

    final int fd =
        wasiOps.pathOpen(
            3, 0, "/tmp/stat_test.txt", 0, WasiRights.FD_FILESTAT_GET.getValue(), 0L, 0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test fd_filestat_get
    final WasiFileStat fileStat = wasiOps.fdFilestatGet(fd);
    assertNotNull(fileStat, "File stat must not be null");
    assertEquals(content.length(), fileStat.getSize(), "File size must match content length");
    assertEquals(
        WasiFileType.REGULAR_FILE.getValue(), fileStat.getFileType(), "Must be regular file");
    assertTrue(fileStat.getModificationTime() > 0, "Modification time must be positive");
    assertTrue(fileStat.getAccessTime() > 0, "Access time must be positive");

    // Test path_filestat_get
    final WasiFileStat pathStat = wasiOps.pathFilestatGet(3, 0, "/tmp/stat_test.txt");
    assertNotNull(pathStat, "Path stat must not be null");
    assertEquals(fileStat.getSize(), pathStat.getSize(), "Path and fd stats must match");
    assertEquals(fileStat.getFileType(), pathStat.getFileType(), "File types must match");

    wasiOps.fdClose(fd);
  }

  @Test
  void testErrorHandling() {
    // Test invalid file descriptor operations
    assertThrows(
        WasiException.class,
        () -> {
          final List<ByteBuffer> iovs = new ArrayList<>();
          iovs.add(ByteBuffer.allocate(10));
          wasiOps.fdRead(999, iovs); // Invalid fd
        },
        "Invalid fd must throw WasiException");

    // Test path operations with invalid paths
    assertThrows(
        WasiException.class,
        () ->
            wasiOps.pathOpen(
                3, 0, "/tmp/nonexistent_file.txt", 0, WasiRights.FD_READ.getValue(), 0L, 0),
        "Opening nonexistent file must throw WasiException");

    // Test path traversal protection
    assertThrows(
        Exception.class,
        () ->
            wasiOps.pathOpen(3, 0, "../../../etc/passwd", 0, WasiRights.FD_READ.getValue(), 0L, 0),
        "Path traversal must be prevented");

    // Test write to read-only file descriptor
    assertThrows(
        WasiException.class,
        () -> {
          final Path testFile = tempDirectory.resolve("readonly_test.txt");
          Files.write(testFile, "test".getBytes(), StandardOpenOption.CREATE);

          final int fd =
              wasiOps.pathOpen(
                  3, 0, "/tmp/readonly_test.txt", 0, WasiRights.FD_READ.getValue(), 0L, 0);

          final List<ByteBuffer> writeIovs = new ArrayList<>();
          writeIovs.add(ByteBuffer.wrap("write attempt".getBytes()));
          wasiOps.fdWrite(fd, writeIovs); // Should fail
        },
        "Write to read-only fd must throw WasiException");
  }

  @Test
  void testResourceCleanupAndLimits() {
    // Test file descriptor limits by opening many files
    final List<Integer> openFds = new ArrayList<>();

    try {
      // Create multiple test files
      for (int i = 0; i < 10; i++) {
        final Path testFile = tempDirectory.resolve("limit_test_" + i + ".txt");
        Files.write(testFile, ("Content " + i).getBytes(), StandardOpenOption.CREATE);

        final int fd =
            wasiOps.pathOpen(
                3, 0, "/tmp/limit_test_" + i + ".txt", 0, WasiRights.FD_READ.getValue(), 0L, 0);
        assertTrue(fd >= 0, "File descriptor " + i + " must be valid");
        openFds.add(fd);
      }

      // Verify all file descriptors are valid
      assertEquals(10, openFds.size(), "Must have opened 10 files");

    } catch (final IOException e) {
      throw new RuntimeException("Failed to create test files", e);
    } finally {
      // Clean up all file descriptors
      for (final Integer fd : openFds) {
        try {
          wasiOps.fdClose(fd);
        } catch (final Exception e) {
          System.err.println("Error closing fd " + fd + ": " + e.getMessage());
        }
      }
    }
  }

  @Test
  void testPollingOperations() throws IOException {
    final Path testFile = tempDirectory.resolve("poll_test.txt");
    Files.write(testFile, "Polling test content".getBytes(), StandardOpenOption.CREATE);

    final int fd =
        wasiOps.pathOpen(
            3,
            0,
            "/tmp/poll_test.txt",
            0,
            WasiRights.FD_READ.getValue() | WasiRights.POLL_FD_READWRITE.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Create poll subscription for file descriptor
    final WasiSubscription subscription = new WasiSubscription();
    subscription.setUserData(42L);
    subscription.setType(WasiEventType.FD_READ.getValue());
    subscription.setFdReadwrite(fd, WasiEventRwFlags.FD_READWRITE_HANGUP.getValue());

    final WasiSubscription[] subscriptions = {subscription};
    final WasiEvent[] events = wasiOps.pollOneoff(subscriptions);

    assertNotNull(events, "Poll events must not be null");
    assertTrue(events.length >= 0, "Must return valid events array");

    // For file descriptors that are ready to read, we should get an event
    if (events.length > 0) {
      assertEquals(42L, events[0].getUserData(), "Event user data must match subscription");
      assertEquals(WasiEventType.FD_READ.getValue(), events[0].getType(), "Event type must match");
    }

    wasiOps.fdClose(fd);
  }

  @Test
  void testProcessOperations() {
    // Test proc_raise with different signals (but don't actually exit)
    // This is a challenging test since proc_raise might terminate the process
    // We'll test the method exists and handles invalid signals

    assertThrows(
        WasiException.class,
        () -> wasiOps.procRaise(-1), // Invalid signal
        "Invalid signal must throw WasiException");

    // Test that we can call proc_raise with valid signals that don't terminate
    // SIGCHLD (17) is typically safe to test
    assertDoesNotThrow(() -> wasiOps.procRaise(17), "SIGCHLD signal must not throw");
  }

  @Test
  void testAdvancedFileOperations() throws IOException {
    final Path testFile = tempDirectory.resolve("advanced_test.txt");
    final String originalContent = "Advanced file operations test content";
    Files.write(testFile, originalContent.getBytes(), StandardOpenOption.CREATE);

    final int fd =
        wasiOps.pathOpen(
            3,
            0,
            "/tmp/advanced_test.txt",
            0,
            WasiRights.FD_READ.getValue()
                | WasiRights.FD_WRITE.getValue()
                | WasiRights.FD_ALLOCATE.getValue()
                | WasiRights.FD_FILESTAT_SET_SIZE.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test fd_allocate (preallocate file space)
    assertDoesNotThrow(() -> wasiOps.fdAllocate(fd, 0, 1024), "File allocation must not throw");

    // Test fd_filestat_set_size (truncate/extend file)
    assertDoesNotThrow(
        () -> wasiOps.fdFilestatSetSize(fd, 100), "File size setting must not throw");

    // Verify file size was changed
    final WasiFileStat stat = wasiOps.fdFilestatGet(fd);
    assertEquals(100, stat.getSize(), "File size must be set to 100");

    // Test fd_datasync
    assertDoesNotThrow(() -> wasiOps.fdDatasync(fd), "Data sync must not throw");

    wasiOps.fdClose(fd);
  }
}
