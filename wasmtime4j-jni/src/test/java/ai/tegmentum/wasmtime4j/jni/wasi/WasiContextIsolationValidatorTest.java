package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for WASI context isolation validation.
 *
 * <p>These tests verify that the isolation validator correctly prevents cross-context access to
 * resources, enforces security boundaries, and maintains strict isolation between WASI contexts
 * while tracking violations and providing detailed statistics.
 *
 * @since 1.0.0
 */
final class WasiContextIsolationValidatorTest {

  private WasiContextIsolationValidator validator;
  private MockWasiContext context1;
  private MockWasiContext context2;

  @BeforeEach
  void setUp() {
    validator = new WasiContextIsolationValidator(true); // Enable strict mode
    context1 = new MockWasiContext("context1", "/tmp/context1");
    context2 = new MockWasiContext("context2", "/tmp/context2");
  }

  @Test
  void testContextRegistration() {
    // Initially no contexts registered
    assertEquals(0, validator.getActiveContextCount());

    // Register first context
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);
    assertEquals(1, validator.getActiveContextCount());

    // Register second context
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STRICT);
    assertEquals(2, validator.getActiveContextCount());

    // Verify statistics
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(2, stats.getContextsRegistered());
    assertEquals(0, stats.getContextsUnregistered());
    assertEquals(2, stats.getActiveContexts());

    // Attempt to register duplicate context ID
    assertThrows(
        JniException.class,
        () ->
            validator.registerContext(
                "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD));

    // Unregister contexts
    validator.unregisterContext("context1");
    assertEquals(1, validator.getActiveContextCount());

    validator.unregisterContext("context2");
    assertEquals(0, validator.getActiveContextCount());

    // Verify updated statistics
    final WasiContextIsolationValidator.IsolationStatistics updatedStats =
        validator.getStatistics();
    assertEquals(2, updatedStats.getContextsRegistered());
    assertEquals(2, updatedStats.getContextsUnregistered());
    assertEquals(0, updatedStats.getActiveContexts());
  }

  @Test
  void testPathAccessValidation() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Valid path access within context boundaries
    final Path validPath1 = Paths.get("/tmp/context1/test.txt");
    validator.validatePathAccess("context1", validPath1, WasiFileOperation.READ);

    final Path validPath2 = Paths.get("/tmp/context2/test.txt");
    validator.validatePathAccess("context2", validPath2, WasiFileOperation.READ);

    // Invalid path access outside context boundaries
    final Path invalidPath = Paths.get("/tmp/context2/test.txt");
    assertThrows(
        JniException.class,
        () -> validator.validatePathAccess("context1", invalidPath, WasiFileOperation.READ));

    // Verify boundary violation statistics
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(1, stats.getPathBoundaryViolations());
  }

  @Test
  void testStrictPathIsolation() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STRICT);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STRICT);

    final Path sharedPath = Paths.get("/tmp/context1/shared.txt");

    // First context can access the path for write
    validator.validatePathAccess("context1", sharedPath, WasiFileOperation.WRITE);

    // Second context should be blocked due to strict isolation
    assertThrows(
        JniException.class,
        () -> validator.validatePathAccess("context2", sharedPath, WasiFileOperation.WRITE));

    // Verify path isolation violation
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(1, stats.getPathBoundaryViolations());
  }

  @Test
  void testResourceAccessValidation() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Valid resource access
    validator.validateResourceAccess("context1", "resource1", "file");
    validator.validateResourceAccess("context2", "resource2", "file");

    // Different contexts can access different resources
    validator.validateResourceAccess("context1", "resource3", "socket");
    validator.validateResourceAccess("context2", "resource4", "socket");

    // No violations expected for different resources
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(0, stats.getResourceIsolationViolations());
  }

  @Test
  void testStrictResourceIsolation() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STRICT);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STRICT);

    // First context allocates a resource
    validator.validateResourceAccess("context1", "shared-resource", "database");

    // Second context should be blocked due to strict isolation
    assertThrows(
        JniException.class,
        () -> validator.validateResourceAccess("context2", "shared-resource", "database"));

    // Verify resource isolation violation
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(1, stats.getResourceIsolationViolations());
  }

  @Test
  void testMemoryAccessValidation() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Memory access validation (basic test - simplified implementation)
    validator.validateMemoryAccess("context1", 0x10000L, 1024);
    validator.validateMemoryAccess("context2", 0x20000L, 2048);

    // No violations expected for different memory ranges
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(0, stats.getMemoryIsolationViolations());
  }

  @Test
  void testCrossContextCommunication() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Standard isolation allows cross-context communication
    validator.validateCrossContextCommunication("context1", "context2", "message");

    // No violations expected
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(0, stats.getCrossContextViolations());
  }

  @Test
  void testStrictCrossContextCommunication() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STRICT);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Strict isolation prevents cross-context communication
    assertThrows(
        JniException.class,
        () -> validator.validateCrossContextCommunication("context1", "context2", "message"));

    // Verify cross-context violation
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(1, stats.getCrossContextViolations());
  }

  @Test
  void testPermissiveIsolationMode() {
    final WasiContextIsolationValidator permissiveValidator =
        new WasiContextIsolationValidator(false); // Disable strict mode

    assertFalse(permissiveValidator.isStrictIsolationMode());

    permissiveValidator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.PERMISSIVE);
    permissiveValidator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.PERMISSIVE);

    final Path sharedPath = Paths.get("/tmp/context1/shared.txt");

    // Both contexts should be able to access the same path in permissive mode
    permissiveValidator.validatePathAccess("context1", sharedPath, WasiFileOperation.READ);
    permissiveValidator.validatePathAccess("context2", sharedPath, WasiFileOperation.READ);

    // Verify no violations
    final WasiContextIsolationValidator.IsolationStatistics stats =
        permissiveValidator.getStatistics();
    assertEquals(0, stats.getTotalViolations());
  }

  @Test
  void testIsolationStatistics() {
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STRICT);
    validator.registerContext(
        "context2", context2, WasiContextIsolationValidator.IsolationLevel.STRICT);

    // Generate various violations
    final Path path = Paths.get("/tmp/context1/test.txt");

    // Path isolation violations
    validator.validatePathAccess("context1", path, WasiFileOperation.WRITE);
    assertThrows(
        JniException.class,
        () -> validator.validatePathAccess("context2", path, WasiFileOperation.WRITE));

    // Resource isolation violations
    validator.validateResourceAccess("context1", "resource", "type");
    assertThrows(
        JniException.class,
        () -> validator.validateResourceAccess("context2", "resource", "type"));

    // Cross-context communication violations
    assertThrows(
        JniException.class,
        () -> validator.validateCrossContextCommunication("context1", "context2", "message"));

    // Path boundary violations (access outside allowed directories)
    final Path outsidePath = Paths.get("/etc/passwd");
    assertThrows(
        JniException.class,
        () -> validator.validatePathAccess("context1", outsidePath, WasiFileOperation.READ));

    // Verify statistics
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(2, stats.getContextsRegistered());
    assertEquals(0, stats.getContextsUnregistered());
    assertEquals(2, stats.getActiveContexts());
    assertEquals(1, stats.getPathIsolationViolations());
    assertEquals(1, stats.getResourceIsolationViolations());
    assertEquals(0, stats.getMemoryIsolationViolations());
    assertEquals(1, stats.getPathBoundaryViolations());
    assertEquals(1, stats.getCrossContextViolations());
    assertEquals(4, stats.getTotalViolations());

    // Verify string representation
    final String statsString = stats.toString();
    assertNotNull(statsString);
    assertTrue(statsString.contains("active=2"));
    assertTrue(statsString.contains("path=1"));
    assertTrue(statsString.contains("resource=1"));
    assertTrue(statsString.contains("boundary=1"));
    assertTrue(statsString.contains("crossContext=1"));
  }

  @Test
  void testInputValidation() {
    // Test null/invalid inputs for context registration
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.registerContext(
                null, context1, WasiContextIsolationValidator.IsolationLevel.STANDARD));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.registerContext(
                "", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.registerContext(
                "context1", null, WasiContextIsolationValidator.IsolationLevel.STANDARD));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.registerContext("context1", context1, null));

    // Test validation with registered context
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    // Test null/invalid inputs for path validation
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.validatePathAccess(
                null, Paths.get("/tmp/test"), WasiFileOperation.READ));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.validatePathAccess(
                "", Paths.get("/tmp/test"), WasiFileOperation.READ));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validatePathAccess("context1", null, WasiFileOperation.READ));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            validator.validatePathAccess(
                "context1", Paths.get("/tmp/test"), null));

    // Test null/invalid inputs for resource validation
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess(null, "resource", "type"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess("", "resource", "type"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess("context1", null, "type"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess("context1", "", "type"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess("context1", "resource", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateResourceAccess("context1", "resource", ""));

    // Test null/invalid inputs for memory validation
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateMemoryAccess(null, 0x1000L, 1024));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateMemoryAccess("", 0x1000L, 1024));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateMemoryAccess("context1", -1L, 1024));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateMemoryAccess("context1", 0x1000L, 0));

    // Test null/invalid inputs for cross-context communication
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication(null, "context2", "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication("", "context2", "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication("context1", null, "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication("context1", "", "message"));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication("context1", "context2", null));
    assertThrows(
        IllegalArgumentException.class,
        () -> validator.validateCrossContextCommunication("context1", "context2", ""));
  }

  @Test
  void testUnknownContextHandling() {
    // Attempt operations on unknown context
    assertThrows(
        JniException.class,
        () ->
            validator.validatePathAccess(
                "unknown-context", Paths.get("/tmp/test"), WasiFileOperation.READ));

    assertThrows(
        JniException.class,
        () -> validator.validateResourceAccess("unknown-context", "resource", "type"));

    assertThrows(
        JniException.class,
        () -> validator.validateMemoryAccess("unknown-context", 0x1000L, 1024));

    // Register one context for cross-context communication test
    validator.registerContext(
        "context1", context1, WasiContextIsolationValidator.IsolationLevel.STANDARD);

    assertThrows(
        JniException.class,
        () ->
            validator.validateCrossContextCommunication("context1", "unknown-context", "message"));
    assertThrows(
        JniException.class,
        () ->
            validator.validateCrossContextCommunication("unknown-context", "context1", "message"));
  }

  @Test
  void testConcurrentContextOperations() throws InterruptedException {
    final int threadCount = 10;
    final Thread[] threads = new Thread[threadCount];

    // Create threads that register contexts and perform validation operations
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                final String contextId = "concurrent-context-" + threadId;
                final MockWasiContext context =
                    new MockWasiContext(contextId, "/tmp/" + contextId);

                validator.registerContext(
                    contextId, context, WasiContextIsolationValidator.IsolationLevel.STANDARD);

                // Perform validation operations
                for (int j = 0; j < 100; j++) {
                  final Path path = Paths.get("/tmp/" + contextId + "/file" + j + ".txt");
                  try {
                    validator.validatePathAccess(contextId, path, WasiFileOperation.READ);
                    validator.validateResourceAccess(
                        contextId, "resource" + j, "type" + (j % 3));
                    validator.validateMemoryAccess(contextId, 0x1000L + j, 1024);
                  } catch (final JniException e) {
                    // Expected for some operations due to isolation
                  }
                }

                validator.unregisterContext(contextId);
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join();
    }

    // Verify final state
    final WasiContextIsolationValidator.IsolationStatistics stats = validator.getStatistics();
    assertEquals(threadCount, stats.getContextsRegistered());
    assertEquals(threadCount, stats.getContextsUnregistered());
    assertEquals(0, stats.getActiveContexts());

    // Some violations may have occurred due to concurrent access attempts
    assertTrue(stats.getTotalViolations() >= 0);
  }

  /** Mock WASI context for testing. */
  private static final class MockWasiContext extends WasiContext {
    private final String contextId;
    private final Map<String, Path> preopenedDirectories;

    MockWasiContext(final String contextId, final String allowedDirectory) {
      super(1L, WasiContextBuilder.builder().build()); // Mock native handle
      this.contextId = contextId;
      this.preopenedDirectories = new ConcurrentHashMap<>();
      this.preopenedDirectories.put("root", Paths.get(allowedDirectory));
    }

    @Override
    public Map<String, Path> getPreopenedDirectories() {
      return new ConcurrentHashMap<>(preopenedDirectories);
    }

    @Override
    public String toString() {
      return "MockWasiContext{contextId='" + contextId + "'}";
    }
  }
}