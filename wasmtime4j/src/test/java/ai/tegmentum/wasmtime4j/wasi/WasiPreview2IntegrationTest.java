package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Test suite for WASI Preview 2 integration interfaces.
 *
 * <p>Validates the WASI Preview 2 context management, resource configuration, and security
 * policy interfaces that enable capability-based component execution.
 *
 * <p>These tests ensure that the WASI Preview 2 API provides comprehensive resource management
 * and security control capabilities for component model execution.
 */
@DisplayName("WASI Preview 2 Integration Tests")
class WasiPreview2IntegrationTest {

  @Test
  @DisplayName("WasiResourceType enum should have all required types")
  void testWasiResourceTypeCompleteness() {
    // Test core resource types
    assertNotNull(WasiResourceType.FILESYSTEM);
    assertNotNull(WasiResourceType.NETWORK);
    assertNotNull(WasiResourceType.PROCESS);
    assertNotNull(WasiResourceType.ENVIRONMENT);
    assertNotNull(WasiResourceType.TIME);
    assertNotNull(WasiResourceType.RANDOM);
    assertNotNull(WasiResourceType.STDIO);

    // Test advanced resource types
    assertNotNull(WasiResourceType.MEMORY);
    assertNotNull(WasiResourceType.THREAD);
    assertNotNull(WasiResourceType.CRYPTO);
    assertNotNull(WasiResourceType.HTTP);
    assertNotNull(WasiResourceType.DATABASE);
    assertNotNull(WasiResourceType.LOG);
    assertNotNull(WasiResourceType.CUSTOM);
  }

  @Test
  @DisplayName("WasiResourceType properties should be correct")
  void testWasiResourceTypeProperties() {
    // Test that resource types have expected names
    assertEquals("filesystem", WasiResourceType.FILESYSTEM.getName());
    assertEquals("network", WasiResourceType.NETWORK.getName());
    assertEquals("process", WasiResourceType.PROCESS.getName());
    assertEquals("custom", WasiResourceType.CUSTOM.getName());

    // Test that dangerous resource types require special permissions
    assertTrue(WasiResourceType.FILESYSTEM.requiresSpecialPermissions());
    assertTrue(WasiResourceType.NETWORK.requiresSpecialPermissions());
    assertTrue(WasiResourceType.PROCESS.requiresSpecialPermissions());
    assertTrue(WasiResourceType.CRYPTO.requiresSpecialPermissions());

    // Test that safe resource types don't require special permissions
    assertFalse(WasiResourceType.TIME.requiresSpecialPermissions());
    assertFalse(WasiResourceType.RANDOM.requiresSpecialPermissions());
    assertFalse(WasiResourceType.LOG.requiresSpecialPermissions());

    // Test shareable resource types
    assertTrue(WasiResourceType.TIME.isShareable());
    assertTrue(WasiResourceType.RANDOM.isShareable());
    assertTrue(WasiResourceType.LOG.isShareable());
    assertTrue(WasiResourceType.ENVIRONMENT.isShareable());

    // Test non-shareable resource types
    assertFalse(WasiResourceType.FILESYSTEM.isShareable());
    assertFalse(WasiResourceType.NETWORK.isShareable());
    assertFalse(WasiResourceType.PROCESS.isShareable());

    // Test resource types that require cleanup
    assertTrue(WasiResourceType.FILESYSTEM.requiresCleanup());
    assertTrue(WasiResourceType.NETWORK.requiresCleanup());
    assertTrue(WasiResourceType.PROCESS.requiresCleanup());
    assertTrue(WasiResourceType.MEMORY.requiresCleanup());
    assertTrue(WasiResourceType.THREAD.requiresCleanup());

    // Test resource types that don't require cleanup
    assertFalse(WasiResourceType.TIME.requiresCleanup());
    assertFalse(WasiResourceType.RANDOM.requiresCleanup());
    assertFalse(WasiResourceType.ENVIRONMENT.requiresCleanup());
  }

  @Test
  @DisplayName("WasiResourceType default permissions should be appropriate")
  void testWasiResourceTypeDefaultPermissions() {
    // Test read-only default permissions
    assertEquals(WasiResourcePermissions.READ_ONLY,
        WasiResourceType.FILESYSTEM.getDefaultPermissions());
    assertEquals(WasiResourcePermissions.READ_ONLY,
        WasiResourceType.ENVIRONMENT.getDefaultPermissions());

    // Test read-write default permissions
    assertEquals(WasiResourcePermissions.READ_WRITE,
        WasiResourceType.NETWORK.getDefaultPermissions());
    assertEquals(WasiResourcePermissions.READ_WRITE,
        WasiResourceType.TIME.getDefaultPermissions());
    assertEquals(WasiResourcePermissions.READ_WRITE,
        WasiResourceType.RANDOM.getDefaultPermissions());
    assertEquals(WasiResourcePermissions.READ_WRITE,
        WasiResourceType.STDIO.getDefaultPermissions());

    // Test execute permissions
    assertEquals(WasiResourcePermissions.EXECUTE,
        WasiResourceType.PROCESS.getDefaultPermissions());
    assertEquals(WasiResourcePermissions.EXECUTE,
        WasiResourceType.CRYPTO.getDefaultPermissions());

    // Test write-only permissions
    assertEquals(WasiResourcePermissions.WRITE_ONLY,
        WasiResourceType.LOG.getDefaultPermissions());

    // Test no permissions for custom types
    assertEquals(WasiResourcePermissions.NONE,
        WasiResourceType.CUSTOM.getDefaultPermissions());
  }

  @Test
  @DisplayName("WasiResourceType lookup methods should work correctly")
  void testWasiResourceTypeLookupMethods() {
    // Test successful lookups
    assertEquals(WasiResourceType.FILESYSTEM, WasiResourceType.fromName("filesystem"));
    assertEquals(WasiResourceType.NETWORK, WasiResourceType.fromName("network"));
    assertEquals(WasiResourceType.PROCESS, WasiResourceType.fromName("process"));

    // Test case insensitive lookup
    assertEquals(WasiResourceType.FILESYSTEM, WasiResourceType.fromName("FILESYSTEM"));
    assertEquals(WasiResourceType.NETWORK, WasiResourceType.fromName("Network"));

    // Test with whitespace
    assertEquals(WasiResourceType.FILESYSTEM, WasiResourceType.fromName(" filesystem "));

    // Test invalid names
    assertNull(WasiResourceType.fromName("nonexistent"));
    assertNull(WasiResourceType.fromName(""));

    // Test null handling
    assertThrows(IllegalArgumentException.class, () -> WasiResourceType.fromName(null));

    // Test validation methods
    assertTrue(WasiResourceType.isValidName("filesystem"));
    assertTrue(WasiResourceType.isValidName("network"));
    assertFalse(WasiResourceType.isValidName("nonexistent"));
    assertFalse(WasiResourceType.isValidName(""));
  }

  @Test
  @DisplayName("WasiResourcePermissions enum should have all required permissions")
  void testWasiResourcePermissionsCompleteness() {
    assertNotNull(WasiResourcePermissions.NONE);
    assertNotNull(WasiResourcePermissions.READ);
    assertNotNull(WasiResourcePermissions.WRITE);
    assertNotNull(WasiResourcePermissions.EXECUTE);
    assertNotNull(WasiResourcePermissions.CREATE);
    assertNotNull(WasiResourcePermissions.DELETE);
    assertNotNull(WasiResourcePermissions.ADMIN);
  }

  @Test
  @DisplayName("WasiResourcePermissions properties should be correct")
  void testWasiResourcePermissionsProperties() {
    // Test permission names
    assertEquals("none", WasiResourcePermissions.NONE.getName());
    assertEquals("read", WasiResourcePermissions.READ.getName());
    assertEquals("write", WasiResourcePermissions.WRITE.getName());
    assertEquals("execute", WasiResourcePermissions.EXECUTE.getName());
    assertEquals("create", WasiResourcePermissions.CREATE.getName());
    assertEquals("delete", WasiResourcePermissions.DELETE.getName());
    assertEquals("admin", WasiResourcePermissions.ADMIN.getName());

    // Test permission masks are unique powers of 2 (except NONE)
    assertEquals(0, WasiResourcePermissions.NONE.getMask());
    assertEquals(1, WasiResourcePermissions.READ.getMask());
    assertEquals(2, WasiResourcePermissions.WRITE.getMask());
    assertEquals(4, WasiResourcePermissions.EXECUTE.getMask());
    assertEquals(8, WasiResourcePermissions.CREATE.getMask());
    assertEquals(16, WasiResourcePermissions.DELETE.getMask());
    assertEquals(32, WasiResourcePermissions.ADMIN.getMask());
  }

  @Test
  @DisplayName("WasiResourcePermissions predefined sets should be correct")
  void testWasiResourcePermissionsPredefinedSets() {
    // Test READ_ONLY set
    assertEquals(1, WasiResourcePermissions.READ_ONLY.size());
    assertTrue(WasiResourcePermissions.READ_ONLY.contains(WasiResourcePermissions.READ));

    // Test WRITE_ONLY set
    assertEquals(1, WasiResourcePermissions.WRITE_ONLY.size());
    assertTrue(WasiResourcePermissions.WRITE_ONLY.contains(WasiResourcePermissions.WRITE));

    // Test READ_WRITE set
    assertEquals(2, WasiResourcePermissions.READ_WRITE.size());
    assertTrue(WasiResourcePermissions.READ_WRITE.contains(WasiResourcePermissions.READ));
    assertTrue(WasiResourcePermissions.READ_WRITE.contains(WasiResourcePermissions.WRITE));

    // Test FULL set (all except ADMIN and NONE)
    assertEquals(5, WasiResourcePermissions.FULL.size());
    assertTrue(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.READ));
    assertTrue(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.WRITE));
    assertTrue(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.EXECUTE));
    assertTrue(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.CREATE));
    assertTrue(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.DELETE));
    assertFalse(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.ADMIN));
    assertFalse(WasiResourcePermissions.FULL.contains(WasiResourcePermissions.NONE));

    // Test ALL set
    assertEquals(7, WasiResourcePermissions.ALL.size());
    assertTrue(WasiResourcePermissions.ALL.contains(WasiResourcePermissions.ADMIN));
    assertTrue(WasiResourcePermissions.ALL.contains(WasiResourcePermissions.NONE));
  }

  @Test
  @DisplayName("WasiResourcePermissions utility methods should work correctly")
  void testWasiResourcePermissionsUtilityMethods() {
    // Test includes method
    assertTrue(WasiResourcePermissions.ADMIN.includes(WasiResourcePermissions.READ));
    assertFalse(WasiResourcePermissions.READ.includes(WasiResourcePermissions.ADMIN));

    // Test with method
    java.util.Set<WasiResourcePermissions> combined = WasiResourcePermissions.READ.with(WasiResourcePermissions.WRITE);
    assertEquals(2, combined.size());
    assertTrue(combined.contains(WasiResourcePermissions.READ));
    assertTrue(combined.contains(WasiResourcePermissions.WRITE));

    // Test hasAll method
    assertTrue(WasiResourcePermissions.hasAll(WasiResourcePermissions.READ_WRITE,
        WasiResourcePermissions.READ, WasiResourcePermissions.WRITE));
    assertFalse(WasiResourcePermissions.hasAll(WasiResourcePermissions.READ_ONLY,
        WasiResourcePermissions.READ, WasiResourcePermissions.WRITE));

    // Test hasAny method
    assertTrue(WasiResourcePermissions.hasAny(WasiResourcePermissions.READ_WRITE,
        WasiResourcePermissions.READ, WasiResourcePermissions.EXECUTE));
    assertFalse(WasiResourcePermissions.hasAny(WasiResourcePermissions.READ_ONLY,
        WasiResourcePermissions.WRITE, WasiResourcePermissions.EXECUTE));
  }

  @Test
  @DisplayName("WasiResourcePermissions mask conversion should work correctly")
  void testWasiResourcePermissionsMaskConversion() {
    // Test toMask method
    int readWriteMask = WasiResourcePermissions.toMask(WasiResourcePermissions.READ_WRITE);
    assertEquals(3, readWriteMask); // 1 (READ) | 2 (WRITE) = 3

    // Test fromMask method
    java.util.Set<WasiResourcePermissions> permissions = WasiResourcePermissions.fromMask(3);
    assertEquals(2, permissions.size());
    assertTrue(permissions.contains(WasiResourcePermissions.READ));
    assertTrue(permissions.contains(WasiResourcePermissions.WRITE));

    // Test round-trip conversion
    java.util.Set<WasiResourcePermissions> original = WasiResourcePermissions.FULL;
    int mask = WasiResourcePermissions.toMask(original);
    java.util.Set<WasiResourcePermissions> restored = WasiResourcePermissions.fromMask(mask);
    assertEquals(original, restored);
  }

  @Test
  @DisplayName("WasiResourcePermissions lookup methods should work correctly")
  void testWasiResourcePermissionsLookupMethods() {
    // Test successful lookups
    assertEquals(WasiResourcePermissions.READ, WasiResourcePermissions.fromName("read"));
    assertEquals(WasiResourcePermissions.WRITE, WasiResourcePermissions.fromName("write"));
    assertEquals(WasiResourcePermissions.EXECUTE, WasiResourcePermissions.fromName("execute"));

    // Test case insensitive lookup
    assertEquals(WasiResourcePermissions.READ, WasiResourcePermissions.fromName("READ"));
    assertEquals(WasiResourcePermissions.WRITE, WasiResourcePermissions.fromName("Write"));

    // Test with whitespace
    assertEquals(WasiResourcePermissions.READ, WasiResourcePermissions.fromName(" read "));

    // Test invalid names
    assertNull(WasiResourcePermissions.fromName("nonexistent"));
    assertNull(WasiResourcePermissions.fromName(""));

    // Test null handling
    assertThrows(IllegalArgumentException.class, () -> WasiResourcePermissions.fromName(null));

    // Test validation methods
    assertTrue(WasiResourcePermissions.isValidName("read"));
    assertTrue(WasiResourcePermissions.isValidName("admin"));
    assertFalse(WasiResourcePermissions.isValidName("nonexistent"));
    assertFalse(WasiResourcePermissions.isValidName(""));
  }

  @Test
  @DisplayName("WASI P2 Context factory method should exist")
  void testWasiP2ContextFactoryMethod() {
    // The builder method should exist and throw UnsupportedOperationException until implemented
    assertThrows(UnsupportedOperationException.class, () -> WasiP2Context.builder());
  }

  @Test
  @DisplayName("WasiResourceManager factory method should exist")
  void testWasiResourceManagerFactoryMethod() {
    // The create method should exist and throw UnsupportedOperationException until implemented
    assertThrows(UnsupportedOperationException.class, () -> WasiResourceManager.create());
  }

  @Test
  @DisplayName("WASI P2 interfaces should have proper method signatures")
  void testWasiP2InterfaceMethodSignatures() {
    // Test WasiP2Context interface methods
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method setResourceLimits = WasiP2Context.class.getMethod(
          "setResourceLimits", WasiResourceLimits.class);
      assertNotNull(setResourceLimits);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method setSecurityPolicy = WasiP2Context.class.getMethod(
          "setSecurityPolicy", WasiSecurityPolicy.class);
      assertNotNull(setSecurityPolicy);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getResources = WasiP2Context.class.getMethod("getResources");
      assertNotNull(getResources);
    });

    // Test WasiResourceManager interface methods
    assertDoesNotThrow(() -> {
      java.lang.reflect.Method createResource = WasiResourceManager.class.getMethod(
          "createResource", Class.class, WasiResourceConfig.class);
      assertNotNull(createResource);
    });

    assertDoesNotThrow(() -> {
      java.lang.reflect.Method getResource = WasiResourceManager.class.getMethod(
          "getResource", String.class);
      assertNotNull(getResource);
    });
  }

  @Test
  @DisplayName("WASI P2 interfaces should follow expected hierarchy")
  void testWasiP2InterfaceHierarchy() {
    // Test that context interfaces extend Closeable where appropriate
    assertTrue(java.io.Closeable.class.isAssignableFrom(WasiP2Context.class));
    assertTrue(java.io.Closeable.class.isAssignableFrom(WasiResourceManager.class));
  }

  @Test
  @DisplayName("WASI P2 configuration classes should exist")
  void testWasiP2ConfigurationClasses() {
    // Verify that all necessary configuration classes are defined
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiP2Context"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiP2ContextBuilder"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiResourceManager"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiResourceType"));
    assertDoesNotThrow(() -> Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions"));
  }
}