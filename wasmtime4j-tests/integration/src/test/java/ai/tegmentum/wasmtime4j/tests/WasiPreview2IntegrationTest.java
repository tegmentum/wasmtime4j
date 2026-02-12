package ai.tegmentum.wasmtime4j.tests;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiLinkerUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for WASI Preview 2 functionality.
 *
 * <p>This test suite validates the complete WASI Preview 2 implementation including:
 *
 * <ul>
 *   <li>Component model integration
 *   <li>Async I/O operations
 *   <li>Enhanced filesystem permissions
 *   <li>Network operations (where supported)
 *   <li>Process management
 *   <li>Resource management and cleanup
 * </ul>
 */
public class WasiPreview2IntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiPreview2IntegrationTest.class.getName());

  private WasmRuntime runtime;
  private Engine engine;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    LOGGER.info("Setting up test: " + testInfo.getDisplayName());
    try {
      runtime = WasmRuntimeFactory.create();
      engine = runtime.createEngine();
    } catch (WasmException e) {
      fail("Failed to set up runtime: " + e.getMessage(), e);
    }
  }

  @AfterEach
  void tearDown(TestInfo testInfo) {
    LOGGER.info("Tearing down test: " + testInfo.getDisplayName());
    if (runtime != null) {
      try {
        runtime.close();
      } catch (Exception e) {
        LOGGER.warning("Failed to close runtime: " + e.getMessage());
      }
    }
  }

  @Test
  void testWasiPreview2ContextCreation() throws WasmException {
    // Create WASI context with Preview 2 capabilities
    WasiContext context = runtime.createWasiContext();
    assertNotNull(context, "WASI context should be created");

    // Configure Preview 2 specific features
    context
        .setAsyncIoEnabled(true)
        .setMaxAsyncOperations(10)
        .setAsyncTimeout(5000)
        .setComponentModelEnabled(true)
        .setProcessEnabled(true)
        .setNetworkEnabled(true);

    LOGGER.info("Successfully created and configured WASI Preview 2 context");
  }

  @Test
  void testWasiPreview2LinkerCreation() throws WasmException {
    WasiContext context =
        runtime.createWasiContext().setAsyncIoEnabled(true).setComponentModelEnabled(true);

    // Create linker with WASI Preview 2 support
    Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
    assertNotNull(linker, "Preview 2 linker should be created");

    // Verify WASI Preview 2 imports are present
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Linker should have WASI Preview 2 imports");

    LOGGER.info("Successfully created WASI Preview 2 linker");
  }

  @Test
  void testComponentModelSupport() throws WasmException {
    // Check if runtime supports component model
    boolean supportsComponents = runtime.supportsComponentModel();
    LOGGER.info("Runtime component model support: " + supportsComponents);

    WasiContext context = runtime.createWasiContext().setComponentModelEnabled(true);

    Linker<WasiContext> linker = runtime.createLinker(engine);

    // Add component model support to linker
    assertDoesNotThrow(
        () -> {
          runtime.addComponentModelToLinker(linker);
        },
        "Adding component model to linker should not throw");

    // Verify component model imports
    assertTrue(
        WasiLinkerUtils.hasComponentModelImports(linker),
        "Linker should have component model imports");

    LOGGER.info("Successfully validated component model support");
  }

  @Test
  void testEnhancedFilesystemPermissions() throws WasmException, IOException {
    // Create test directories
    Path readOnlyDir = tempDir.resolve("readonly");
    Path readWriteDir = tempDir.resolve("readwrite");
    Files.createDirectories(readOnlyDir);
    Files.createDirectories(readWriteDir);

    // Create test files
    Path readOnlyFile = readOnlyDir.resolve("readonly.txt");
    Path readWriteFile = readWriteDir.resolve("readwrite.txt");
    Files.writeString(readOnlyFile, "Read-only content");
    Files.writeString(readWriteFile, "Read-write content");

    WasiContext context = runtime.createWasiContext().setAsyncIoEnabled(true);

    // Add directories with specific permissions
    WasiDirectoryPermissions readOnlyPerms = WasiDirectoryPermissions.readOnly();
    WasiDirectoryPermissions readWritePerms = WasiDirectoryPermissions.readWrite();

    assertDoesNotThrow(
        () -> {
          context.preopenedDirWithPermissions(readOnlyDir, "/readonly", readOnlyPerms);
          context.preopenedDirWithPermissions(readWriteDir, "/readwrite", readWritePerms);
        },
        "Setting directory permissions should not throw");

    // Validate permission configurations
    assertTrue(readOnlyPerms.canRead(), "Read-only permissions should allow reading");
    assertFalse(readOnlyPerms.canWrite(), "Read-only permissions should not allow writing");
    assertTrue(readWritePerms.canRead(), "Read-write permissions should allow reading");
    assertTrue(readWritePerms.canWrite(), "Read-write permissions should allow writing");

    LOGGER.info("Successfully configured enhanced filesystem permissions");
  }

  @Test
  void testAsyncIoConfiguration() throws WasmException {
    WasiContext context = runtime.createWasiContext();

    // Configure async I/O settings
    assertDoesNotThrow(
        () -> {
          context.setAsyncIoEnabled(true).setMaxAsyncOperations(20).setAsyncTimeout(10000);
        },
        "Configuring async I/O should not throw");

    // Test with different timeout values
    assertDoesNotThrow(
        () -> {
          context.setAsyncTimeout(-1); // No timeout
        },
        "Setting no timeout should not throw");

    assertDoesNotThrow(
        () -> {
          context.setAsyncTimeout(0); // Immediate timeout
        },
        "Setting immediate timeout should not throw");

    // Test max operations limits
    assertDoesNotThrow(
        () -> {
          context.setMaxAsyncOperations(-1); // Unlimited
        },
        "Setting unlimited async operations should not throw");

    LOGGER.info("Successfully configured async I/O settings");
  }

  @Test
  void testProcessAndNetworkConfiguration() throws WasmException {
    WasiContext context = runtime.createWasiContext();

    // Configure process and network capabilities
    assertDoesNotThrow(
        () -> {
          context.setProcessEnabled(true).setNetworkEnabled(true);
        },
        "Enabling process and network should not throw");

    // Test disabling capabilities
    assertDoesNotThrow(
        () -> {
          context.setProcessEnabled(false).setNetworkEnabled(false);
        },
        "Disabling process and network should not throw");

    LOGGER.info("Successfully configured process and network settings");
  }

  @Test
  void testFullWasiPreview2Linker() throws WasmException {
    WasiContext context =
        runtime
            .createWasiContext()
            .setAsyncIoEnabled(true)
            .setComponentModelEnabled(true)
            .setProcessEnabled(true)
            .setNetworkEnabled(true)
            .setMaxAsyncOperations(50)
            .setAsyncTimeout(15000);

    // Create full linker with both Preview 2 and Component Model
    Linker<WasiContext> linker = WasiLinkerUtils.createFullLinker(engine, context);
    assertNotNull(linker, "Full linker should be created");

    // Verify all imports are present
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Full linker should have WASI Preview 2 imports");
    assertTrue(
        WasiLinkerUtils.hasComponentModelImports(linker),
        "Full linker should have Component Model imports");

    LOGGER.info("Successfully created full WASI Preview 2 + Component Model linker");
  }

  @Test
  void testWasiPreview2AddToLinkerDirectly() throws WasmException {
    WasiContext context =
        runtime.createWasiContext().setAsyncIoEnabled(true).setComponentModelEnabled(true);

    Linker<WasiContext> linker = runtime.createLinker(engine);

    // Add WASI Preview 2 directly to linker
    assertDoesNotThrow(
        () -> {
          runtime.addWasiPreview2ToLinker(linker, context);
        },
        "Adding WASI Preview 2 to linker should not throw");

    // Verify imports were added
    assertTrue(
        WasiLinkerUtils.hasWasiPreview2Imports(linker),
        "Linker should have WASI Preview 2 imports after adding");

    LOGGER.info("Successfully added WASI Preview 2 imports to linker");
  }

  @Test
  void testDirectoryPermissionsBuilder() {
    // Test permission builder patterns
    WasiDirectoryPermissions customPerms =
        WasiDirectoryPermissions.builder()
            .allowRead()
            .allowList()
            .allowTraverse()
            .allowMetadata()
            .build();

    assertTrue(customPerms.canRead(), "Custom permissions should allow reading");
    assertTrue(customPerms.canList(), "Custom permissions should allow listing");
    assertTrue(customPerms.canTraverse(), "Custom permissions should allow traversal");
    assertTrue(customPerms.canAccessMetadata(), "Custom permissions should allow metadata access");
    assertFalse(customPerms.canWrite(), "Custom permissions should not allow writing");
    assertFalse(customPerms.canCreate(), "Custom permissions should not allow creation");
    assertFalse(customPerms.canDelete(), "Custom permissions should not allow deletion");

    // Test pre-built permission sets
    WasiDirectoryPermissions readOnly = WasiDirectoryPermissions.readOnly();
    WasiDirectoryPermissions readWrite = WasiDirectoryPermissions.readWrite();
    WasiDirectoryPermissions none = WasiDirectoryPermissions.none();
    WasiDirectoryPermissions full = WasiDirectoryPermissions.full();

    assertNotEquals(readOnly, readWrite, "Read-only and read-write permissions should differ");
    assertNotEquals(none, full, "No permissions and full permissions should differ");

    LOGGER.info("Successfully validated directory permissions builder");
  }

  @Test
  void testErrorHandlingInPreview2() throws WasmException {
    WasiContext context = runtime.createWasiContext();

    // Test invalid async operation limits
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setMaxAsyncOperations(-2);
        },
        "Invalid max async operations should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.setAsyncTimeout(-2);
        },
        "Invalid async timeout should throw");

    // Test null parameters
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.preopenedDirWithPermissions(null, "/test", WasiDirectoryPermissions.readOnly());
        },
        "Null host path should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.preopenedDirWithPermissions(tempDir, null, WasiDirectoryPermissions.readOnly());
        },
        "Null guest path should throw");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          context.preopenedDirWithPermissions(tempDir, "/test", null);
        },
        "Null permissions should throw");

    LOGGER.info("Successfully validated error handling in WASI Preview 2");
  }

  @Test
  void testContextLifecycleManagement() throws WasmException {
    // Test multiple context creation and cleanup
    for (int i = 0; i < 5; i++) {
      WasiContext context =
          runtime
              .createWasiContext()
              .setAsyncIoEnabled(true)
              .setComponentModelEnabled(true)
              .setMaxAsyncOperations(10);

      Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
      assertNotNull(linker, "Linker " + i + " should be created");

      // Contexts and linkers should be properly managed by the runtime
      LOGGER.fine("Created context and linker iteration: " + i);
    }

    LOGGER.info("Successfully validated context lifecycle management");
  }

  /** Test is enabled only if async I/O is actually supported by the runtime. */
  @EnabledIf("supportsAsyncIo")
  @Test
  void testAsyncIoIntegration() throws WasmException {
    // This test would require actual WASM components that use async I/O
    // For now, we test the configuration and setup
    WasiContext context =
        runtime
            .createWasiContext()
            .setAsyncIoEnabled(true)
            .setMaxAsyncOperations(5)
            .setAsyncTimeout(1000);

    Linker<WasiContext> linker = WasiLinkerUtils.createPreview2Linker(engine, context);
    assertNotNull(linker, "Async I/O linker should be created");

    // In a real test, we would load a WASM component that performs async I/O
    // and verify that operations complete correctly with timeouts and cancellation

    LOGGER.info("Successfully validated async I/O integration setup");
  }

  /** Checks if the runtime supports async I/O operations. */
  static boolean supportsAsyncIo() {
    try {
      WasmRuntime testRuntime = WasmRuntimeFactory.create();
      WasiContext testContext = testRuntime.createWasiContext();
      testContext.setAsyncIoEnabled(true);
      testRuntime.close();
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
