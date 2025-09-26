package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive tests for WASI filesystem snapshot operations using Panama FFI implementation.
 *
 * <p>This test suite mirrors the JNI tests but exercises the Panama Foreign Function Interface
 * implementation, ensuring feature parity and correctness across both binding approaches.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WasiFilesystemSnapshotTest {

  private static ExecutorService executorService;
  private WasiContext mockWasiContext;
  private WasiFilesystemSnapshot snapshotHandler;

  @TempDir
  Path tempDirectory;

  @BeforeAll
  static void setUpClass() {
    executorService = Executors.newFixedThreadPool(4);
  }

  @AfterAll
  static void tearDownClass() {
    if (executorService != null) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  @BeforeEach
  void setUp() {
    mockWasiContext = mock(WasiContext.class);
    when(mockWasiContext.getNativeHandle()).thenReturn(54321L);

    snapshotHandler = new WasiFilesystemSnapshot(mockWasiContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (snapshotHandler != null) {
      snapshotHandler.close();
    }
  }

  @Test
  @Order(1)
  @DisplayName("Test Panama snapshot handler initialization")
  void testPanamaSnapshotHandlerInitialization() {
    assertNotNull(snapshotHandler, "Panama snapshot handler should be initialized");

    // Verify initial state
    List<WasiFilesystemSnapshot.SnapshotInfo> snapshots = snapshotHandler.listSnapshots();
    assertTrue(snapshots.isEmpty(), "Initial snapshot list should be empty");
  }

  @Test
  @Order(2)
  @DisplayName("Test Panama full snapshot creation with default options")
  void testPanamaFullSnapshotCreationDefault() throws Exception {
    // Create test directory structure
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    CompletableFuture<Long> future = snapshotHandler.createFullSnapshotAsync(
        testRoot.toString(), options);

    // Wait for completion with timeout
    Long snapshotHandle = future.get(10, TimeUnit.SECONDS);

    assertNotNull(snapshotHandle, "Panama snapshot handle should not be null");
    assertTrue(snapshotHandle > 0, "Panama snapshot handle should be positive");

    // Verify snapshot is tracked
    List<WasiFilesystemSnapshot.SnapshotInfo> snapshots = snapshotHandler.listSnapshots();
    assertEquals(1, snapshots.size(), "Should have one snapshot");

    WasiFilesystemSnapshot.SnapshotInfo info = snapshots.get(0);
    assertEquals(snapshotHandle, info.handle, "Snapshot handle should match");
    assertEquals(testRoot.toString(), info.rootPath, "Root path should match");
    assertEquals(WasiFilesystemSnapshot.SnapshotType.FULL, info.type, "Should be full snapshot");
  }

  @Test
  @Order(3)
  @DisplayName("Test Panama differential snapshot creation")
  void testPanamaDifferentialSnapshotCreation() throws Exception {
    // First create a full snapshot
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    Long fullSnapshotHandle = snapshotHandler.createFullSnapshotAsync(
        testRoot.toString(), options).get(10, TimeUnit.SECONDS);

    // Modify files
    Files.writeString(testRoot.resolve("differential.txt"), "Differential change");

    // Create differential snapshot
    Long differentialSnapshotHandle = snapshotHandler.createDifferentialSnapshotAsync(
        testRoot.toString(), options).get(10, TimeUnit.SECONDS);

    assertNotNull(differentialSnapshotHandle, "Panama differential snapshot should be created");

    List<WasiFilesystemSnapshot.SnapshotInfo> snapshots = snapshotHandler.listSnapshots();
    assertEquals(2, snapshots.size(), "Should have two snapshots");

    // Verify differential snapshot
    WasiFilesystemSnapshot.SnapshotInfo differentialSnapshot = snapshots.stream()
        .filter(s -> s.type == WasiFilesystemSnapshot.SnapshotType.DIFFERENTIAL)
        .findFirst()
        .orElse(null);

    assertNotNull(differentialSnapshot, "Should find Panama differential snapshot");
  }

  @Test
  @Order(4)
  @DisplayName("Test Panama snapshot restoration with various options")
  void testPanamaSnapshotRestoration() throws Exception {
    // Create test structure and snapshot
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    Long snapshotHandle = snapshotHandler.createFullSnapshotAsync(
        testRoot.toString(), options).get(10, TimeUnit.SECONDS);

    // Create restore target
    Path restoreTarget = tempDirectory.resolve("panama_restore_target");
    Files.createDirectories(restoreTarget);

    // Test with various restore options
    WasiFilesystemSnapshot.RestoreOptions restoreOptions =
        new WasiFilesystemSnapshot.RestoreOptions(
            true,  // overwriteExisting
            true,  // preservePermissions
            true,  // preserveTimestamps
            true   // verifyIntegrity
        );

    CompletableFuture<Void> restoreFuture = snapshotHandler.restoreFromSnapshotAsync(
        snapshotHandle, restoreTarget.toString(), restoreOptions);

    // Wait for restoration to complete
    assertDoesNotThrow(() -> restoreFuture.get(15, TimeUnit.SECONDS),
        "Panama snapshot restoration should complete without errors");
  }

  @Test
  @Order(5)
  @DisplayName("Test Panama snapshot metrics collection")
  void testPanamaSnapshotMetrics() throws Exception {
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Create multiple snapshots to generate metrics
    snapshotHandler.createFullSnapshotAsync(testRoot.toString(), options)
        .get(10, TimeUnit.SECONDS);

    Files.writeString(testRoot.resolve("modified.txt"), "Modified for incremental");
    Long incrementalHandle = snapshotHandler.createIncrementalSnapshotAsync(
        testRoot.toString(), 1L, options).get(10, TimeUnit.SECONDS);

    WasiFilesystemSnapshot.SnapshotMetrics metrics = snapshotHandler.getSnapshotMetrics();

    assertNotNull(metrics, "Panama metrics should not be null");
    assertTrue(metrics.totalSnapshotsCreated >= 2, "Should have at least two snapshots created");
    assertTrue(metrics.activeSnapshots >= 2, "Should have at least two active snapshots");
    assertNotNull(metrics.performanceMetrics, "Performance metrics should not be null");
    assertNotNull(metrics.dedupStats, "Deduplication stats should not be null");
    assertNotNull(metrics.compressionStats, "Compression stats should not be null");

    // Test specific performance metrics
    assertTrue(metrics.performanceMetrics.avgSnapshotCreationTimeMs >= 0,
        "Average creation time should be non-negative");
    assertTrue(metrics.performanceMetrics.throughputBytesPerSec >= 0,
        "Throughput should be non-negative");
  }

  @Test
  @Order(6)
  @DisplayName("Test Panama storage optimization")
  void testPanamaStorageOptimization() throws Exception {
    // Create some snapshots first to have data to optimize
    Path testRoot = createTestDirectoryStructure();
    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    snapshotHandler.createFullSnapshotAsync(testRoot.toString(), options)
        .get(10, TimeUnit.SECONDS);

    CompletableFuture<WasiFilesystemSnapshot.OptimizationResult> optimizationFuture =
        snapshotHandler.optimizeStorageAsync();

    WasiFilesystemSnapshot.OptimizationResult result =
        optimizationFuture.get(15, TimeUnit.SECONDS);

    assertNotNull(result, "Panama optimization result should not be null");
    assertTrue(result.optimizationTimeMs >= 0, "Optimization time should be non-negative");
    assertTrue(result.blocksRemoved >= 0, "Blocks removed should be non-negative");
    assertTrue(result.spaceReclaimed >= 0, "Space reclaimed should be non-negative");
  }

  @Test
  @Order(7)
  @DisplayName("Test Panama advanced snapshot filtering")
  void testPanamaAdvancedSnapshotFiltering() throws Exception {
    Path testRoot1 = createTestDirectoryStructure("panama_filter_test1");
    Path testRoot2 = createTestDirectoryStructure("panama_filter_test2");

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Create snapshots for different paths
    Long snapshot1 = snapshotHandler.createFullSnapshotAsync(
        testRoot1.toString(), options).get(10, TimeUnit.SECONDS);

    Long snapshot2 = snapshotHandler.createFullSnapshotAsync(
        testRoot2.toString(), options).get(10, TimeUnit.SECONDS);

    Files.writeString(testRoot1.resolve("modified.txt"), "Modified content");
    Long incremental1 = snapshotHandler.createIncrementalSnapshotAsync(
        testRoot1.toString(), snapshot1, options).get(10, TimeUnit.SECONDS);

    // Test path-based filtering
    List<WasiFilesystemSnapshot.SnapshotInfo> path1Snapshots =
        snapshotHandler.listSnapshotsFiltered(
            WasiFilesystemSnapshot.SnapshotFilter.byPath(testRoot1.toString()));

    assertEquals(2, path1Snapshots.size(), "Should have two snapshots for path1");

    List<WasiFilesystemSnapshot.SnapshotInfo> path2Snapshots =
        snapshotHandler.listSnapshotsFiltered(
            WasiFilesystemSnapshot.SnapshotFilter.byPath(testRoot2.toString()));

    assertEquals(1, path2Snapshots.size(), "Should have one snapshot for path2");

    // Test type-based filtering
    List<WasiFilesystemSnapshot.SnapshotInfo> fullSnapshots =
        snapshotHandler.listSnapshotsFiltered(
            WasiFilesystemSnapshot.SnapshotFilter.byType(
                WasiFilesystemSnapshot.SnapshotType.FULL));

    assertEquals(2, fullSnapshots.size(), "Should have two full snapshots");

    List<WasiFilesystemSnapshot.SnapshotInfo> incrementalSnapshots =
        snapshotHandler.listSnapshotsFiltered(
            WasiFilesystemSnapshot.SnapshotFilter.byType(
                WasiFilesystemSnapshot.SnapshotType.INCREMENTAL));

    assertEquals(1, incrementalSnapshots.size(), "Should have one incremental snapshot");

    // Test age-based filtering
    List<WasiFilesystemSnapshot.SnapshotInfo> recentSnapshots =
        snapshotHandler.listSnapshotsFiltered(
            WasiFilesystemSnapshot.SnapshotFilter.byAgeNewer(60000)); // 1 minute

    assertEquals(3, recentSnapshots.size(), "All snapshots should be recent");
  }

  @Test
  @Order(8)
  @DisplayName("Test Panama snapshot validation with detailed reports")
  void testPanamaSnapshotValidationDetailed() throws Exception {
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        new WasiFilesystemSnapshot.SnapshotOptions(
            true,  // includeHiddenFiles
            6,     // compressionLevel
            false, // encryptionEnabled
            null   // encryptionKey
        );

    Long snapshotHandle = snapshotHandler.createFullSnapshotAsync(
        testRoot.toString(), options).get(10, TimeUnit.SECONDS);

    CompletableFuture<WasiFilesystemSnapshot.SnapshotVerificationResult> validationFuture =
        snapshotHandler.verifySnapshotAsync(snapshotHandle);

    WasiFilesystemSnapshot.SnapshotVerificationResult result =
        validationFuture.get(15, TimeUnit.SECONDS);

    assertNotNull(result, "Panama validation result should not be null");
    assertTrue(result.checkedFiles >= 0, "Checked files should be non-negative");
    assertTrue(result.corruptedFiles >= 0, "Corrupted files should be non-negative");
    assertTrue(result.missingFiles >= 0, "Missing files should be non-negative");
    assertTrue(result.checksumMismatch >= 0, "Checksum mismatches should be non-negative");

    // In a valid snapshot, corrupted/missing files should typically be 0
    assertEquals(0, result.corruptedFiles, "Should have no corrupted files in valid snapshot");
    assertEquals(0, result.missingFiles, "Should have no missing files in valid snapshot");
  }

  @Test
  @Order(9)
  @DisplayName("Test Panama error handling and edge cases")
  void testPanamaErrorHandling() {
    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Test null parameters
    assertThrows(IllegalArgumentException.class,
        () -> snapshotHandler.createFullSnapshotAsync(null, options),
        "Should throw for null root path");

    assertThrows(IllegalArgumentException.class,
        () -> snapshotHandler.createFullSnapshotAsync("/tmp", null),
        "Should throw for null options");

    // Test invalid snapshot operations
    assertThrows(WasiException.class,
        () -> snapshotHandler.deleteSnapshot(999999L),
        "Should throw for non-existent snapshot deletion");

    assertThrows(WasiException.class,
        () -> snapshotHandler.getSnapshotMetadata(999999L),
        "Should throw for non-existent snapshot metadata");

    // Test invalid restore parameters
    WasiFilesystemSnapshot.RestoreOptions restoreOptions =
        WasiFilesystemSnapshot.RestoreOptions.defaultOptions();

    assertThrows(WasiException.class,
        () -> snapshotHandler.restoreFromSnapshotAsync(
            999999L, "/tmp/restore", restoreOptions),
        "Should throw for non-existent snapshot restoration");
  }

  @Test
  @Order(10)
  @DisplayName("Test Panama concurrent operations and thread safety")
  void testPanamaConcurrentOperations() throws Exception {
    Path testRoot1 = createTestDirectoryStructure("panama_concurrent1");
    Path testRoot2 = createTestDirectoryStructure("panama_concurrent2");
    Path testRoot3 = createTestDirectoryStructure("panama_concurrent3");

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Launch multiple concurrent snapshot operations
    CompletableFuture<Long> snapshot1 = snapshotHandler.createFullSnapshotAsync(
        testRoot1.toString(), options);

    CompletableFuture<Long> snapshot2 = snapshotHandler.createFullSnapshotAsync(
        testRoot2.toString(), options);

    CompletableFuture<Long> snapshot3 = snapshotHandler.createFullSnapshotAsync(
        testRoot3.toString(), options);

    // Wait for all to complete
    CompletableFuture<Void> allSnapshots = CompletableFuture.allOf(
        snapshot1, snapshot2, snapshot3);

    assertDoesNotThrow(() -> allSnapshots.get(20, TimeUnit.SECONDS),
        "All concurrent Panama snapshots should complete successfully");

    // Verify all snapshots were created with unique handles
    Long handle1 = snapshot1.get();
    Long handle2 = snapshot2.get();
    Long handle3 = snapshot3.get();

    assertNotEquals(handle1, handle2, "Concurrent snapshots should have unique handles");
    assertNotEquals(handle2, handle3, "Concurrent snapshots should have unique handles");
    assertNotEquals(handle1, handle3, "Concurrent snapshots should have unique handles");

    // Verify total count
    List<WasiFilesystemSnapshot.SnapshotInfo> snapshots = snapshotHandler.listSnapshots();
    assertEquals(3, snapshots.size(), "Should have three concurrent snapshots");

    // Test concurrent operations on same snapshots
    CompletableFuture<WasiFilesystemSnapshot.SnapshotVerificationResult> validation1 =
        snapshotHandler.verifySnapshotAsync(handle1);

    CompletableFuture<WasiFilesystemSnapshot.SnapshotVerificationResult> validation2 =
        snapshotHandler.verifySnapshotAsync(handle2);

    // Both validations should complete successfully
    assertNotNull(validation1.get(10, TimeUnit.SECONDS),
        "Concurrent validation 1 should succeed");
    assertNotNull(validation2.get(10, TimeUnit.SECONDS),
        "Concurrent validation 2 should succeed");
  }

  @Test
  @Order(11)
  @DisplayName("Test Panama resource cleanup and memory management")
  void testPanamaResourceCleanup() throws Exception {
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Create and delete several snapshots to test cleanup
    for (int i = 0; i < 5; i++) {
      Long snapshotHandle = snapshotHandler.createFullSnapshotAsync(
          testRoot.toString(), options).get(10, TimeUnit.SECONDS);

      // Verify creation
      assertNotNull(snapshotHandle, "Snapshot " + i + " should be created");
      assertEquals(1, snapshotHandler.listSnapshots().size(),
          "Should have one snapshot at a time");

      // Delete immediately
      snapshotHandler.deleteSnapshot(snapshotHandle);
      assertEquals(0, snapshotHandler.listSnapshots().size(),
          "Should have no snapshots after deletion");
    }

    // Verify no resource leaks by checking metrics
    WasiFilesystemSnapshot.SnapshotMetrics metrics = snapshotHandler.getSnapshotMetrics();
    assertEquals(0, metrics.activeSnapshots, "Should have no active snapshots after cleanup");
  }

  @Test
  @Order(12)
  @DisplayName("Test Panama close and cleanup operations")
  void testPanamaCloseAndCleanup() throws Exception {
    Path testRoot = createTestDirectoryStructure();

    WasiFilesystemSnapshot.SnapshotOptions options =
        WasiFilesystemSnapshot.SnapshotOptions.defaultOptions();

    // Create some snapshots
    snapshotHandler.createFullSnapshotAsync(testRoot.toString(), options)
        .get(10, TimeUnit.SECONDS);

    Files.writeString(testRoot.resolve("incremental.txt"), "Incremental content");
    snapshotHandler.createIncrementalSnapshotAsync(testRoot.toString(), 1L, options)
        .get(10, TimeUnit.SECONDS);

    // Verify snapshots exist
    assertEquals(2, snapshotHandler.listSnapshots().size(), "Should have two snapshots");

    // Close handler - this should clean up all resources
    assertDoesNotThrow(() -> snapshotHandler.close(),
        "Panama handler close should complete successfully");

    // Verify cleanup occurred (snapshots should be deleted)
    assertEquals(0, snapshotHandler.listSnapshots().size(),
        "Should have no snapshots after close");
  }

  // Utility methods

  private Path createTestDirectoryStructure() throws IOException {
    return createTestDirectoryStructure("panama_test_structure");
  }

  private Path createTestDirectoryStructure(String name) throws IOException {
    Path testRoot = tempDirectory.resolve(name);
    Files.createDirectories(testRoot);

    // Create various file types for comprehensive testing
    Files.writeString(testRoot.resolve("panama_file1.txt"), "Panama test content 1");
    Files.writeString(testRoot.resolve("panama_file2.txt"), "Panama test content 2");

    // Create subdirectory with files
    Path subDir = testRoot.resolve("panama_subdir");
    Files.createDirectories(subDir);
    Files.writeString(subDir.resolve("panama_nested.txt"), "Panama nested file content");

    // Create empty directory
    Files.createDirectories(testRoot.resolve("panama_empty_dir"));

    // Create binary file with more complex pattern
    byte[] binaryData = new byte[256];
    for (int i = 0; i < 256; i++) {
      binaryData[i] = (byte) i;
    }
    Files.write(testRoot.resolve("panama_binary.dat"), binaryData);

    // Create hidden file (Unix-style)
    Files.writeString(testRoot.resolve(".panama_hidden"), "Hidden file content");

    // Create larger text file
    StringBuilder largeContent = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      largeContent.append("Panama line ").append(i).append("\n");
    }
    Files.writeString(testRoot.resolve("panama_large.txt"), largeContent.toString());

    return testRoot;
  }
}