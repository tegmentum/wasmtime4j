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
package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Integration tests for WasiFileOperation enum.
 *
 * <p>These tests exercise actual code execution to improve JaCoCo coverage.
 */
@DisplayName("WASI File Operation Integration Tests")
public class WasiFileOperationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiFileOperationTest.class.getName());

  @Nested
  @DisplayName("Basic Properties Tests")
  class BasicPropertiesTests {

    @ParameterizedTest
    @EnumSource(WasiFileOperation.class)
    @DisplayName("Should have non-null operation ID for all values")
    void shouldHaveNonNullOperationId(final WasiFileOperation operation) {
      LOGGER.info("Testing operation ID for: " + operation.name());

      final String operationId = operation.getOperationId();
      assertNotNull(operationId, "Operation ID should not be null for " + operation);
      assertFalse(operationId.isEmpty(), "Operation ID should not be empty for " + operation);

      LOGGER.info(operation.name() + " -> " + operationId);
    }

    @ParameterizedTest
    @EnumSource(WasiFileOperation.class)
    @DisplayName("Should have non-null description for all values")
    void shouldHaveNonNullDescription(final WasiFileOperation operation) {
      LOGGER.info("Testing description for: " + operation.name());

      final String description = operation.getDescription();
      assertNotNull(description, "Description should not be null for " + operation);
      assertFalse(description.isEmpty(), "Description should not be empty for " + operation);

      LOGGER.info(operation.name() + " -> " + description);
    }

    @Test
    @DisplayName("Should have correct operation ID mappings")
    void shouldHaveCorrectOperationIdMappings() {
      LOGGER.info("Testing operation ID mappings");

      assertEquals("read", WasiFileOperation.READ.getOperationId());
      assertEquals("write", WasiFileOperation.WRITE.getOperationId());
      assertEquals("execute", WasiFileOperation.EXECUTE.getOperationId());
      assertEquals("create_directory", WasiFileOperation.CREATE_DIRECTORY.getOperationId());
      assertEquals("delete", WasiFileOperation.DELETE.getOperationId());
      assertEquals("rename", WasiFileOperation.RENAME.getOperationId());
      assertEquals("metadata", WasiFileOperation.METADATA.getOperationId());
      assertEquals("change_permissions", WasiFileOperation.CHANGE_PERMISSIONS.getOperationId());
      assertEquals("create_link", WasiFileOperation.CREATE_LINK.getOperationId());
      assertEquals("follow_symlinks", WasiFileOperation.FOLLOW_SYMLINKS.getOperationId());
      assertEquals("list_directory", WasiFileOperation.LIST_DIRECTORY.getOperationId());
      assertEquals("set_times", WasiFileOperation.SET_TIMES.getOperationId());
      assertEquals("truncate", WasiFileOperation.TRUNCATE.getOperationId());
      assertEquals("sync", WasiFileOperation.SYNC.getOperationId());
      assertEquals("seek", WasiFileOperation.SEEK.getOperationId());
      assertEquals("poll", WasiFileOperation.POLL.getOperationId());
      assertEquals("open", WasiFileOperation.OPEN.getOperationId());
      assertEquals("close", WasiFileOperation.CLOSE.getOperationId());

      LOGGER.info("All operation ID mappings verified");
    }
  }

  @Nested
  @DisplayName("Read Access Tests")
  class ReadAccessTests {

    @Test
    @DisplayName("Should identify operations requiring read access")
    void shouldIdentifyOperationsRequiringReadAccess() {
      LOGGER.info("Testing read access requirements");

      // Operations that require read access
      assertTrue(WasiFileOperation.READ.requiresReadAccess(), "READ should require read access");
      assertTrue(
          WasiFileOperation.METADATA.requiresReadAccess(), "METADATA should require read access");
      assertTrue(
          WasiFileOperation.LIST_DIRECTORY.requiresReadAccess(),
          "LIST_DIRECTORY should require read access");
      assertTrue(
          WasiFileOperation.FOLLOW_SYMLINKS.requiresReadAccess(),
          "FOLLOW_SYMLINKS should require read access");
      assertTrue(WasiFileOperation.POLL.requiresReadAccess(), "POLL should require read access");

      // Operations that don't require read access
      assertFalse(
          WasiFileOperation.WRITE.requiresReadAccess(), "WRITE should not require read access");
      assertFalse(
          WasiFileOperation.EXECUTE.requiresReadAccess(), "EXECUTE should not require read access");
      assertFalse(
          WasiFileOperation.DELETE.requiresReadAccess(), "DELETE should not require read access");

      LOGGER.info("Read access requirements verified");
    }
  }

  @Nested
  @DisplayName("Write Access Tests")
  class WriteAccessTests {

    @Test
    @DisplayName("Should identify operations requiring write access")
    void shouldIdentifyOperationsRequiringWriteAccess() {
      LOGGER.info("Testing write access requirements");

      // Operations that require write access
      assertTrue(
          WasiFileOperation.WRITE.requiresWriteAccess(), "WRITE should require write access");
      assertTrue(
          WasiFileOperation.CREATE_DIRECTORY.requiresWriteAccess(),
          "CREATE_DIRECTORY should require write access");
      assertTrue(
          WasiFileOperation.DELETE.requiresWriteAccess(), "DELETE should require write access");
      assertTrue(
          WasiFileOperation.RENAME.requiresWriteAccess(), "RENAME should require write access");
      assertTrue(
          WasiFileOperation.CHANGE_PERMISSIONS.requiresWriteAccess(),
          "CHANGE_PERMISSIONS should require write access");
      assertTrue(
          WasiFileOperation.CREATE_LINK.requiresWriteAccess(),
          "CREATE_LINK should require write access");
      assertTrue(
          WasiFileOperation.SET_TIMES.requiresWriteAccess(),
          "SET_TIMES should require write access");
      assertTrue(
          WasiFileOperation.TRUNCATE.requiresWriteAccess(), "TRUNCATE should require write access");
      assertTrue(WasiFileOperation.SYNC.requiresWriteAccess(), "SYNC should require write access");

      // Operations that don't require write access
      assertFalse(
          WasiFileOperation.READ.requiresWriteAccess(), "READ should not require write access");
      assertFalse(
          WasiFileOperation.EXECUTE.requiresWriteAccess(),
          "EXECUTE should not require write access");
      assertFalse(
          WasiFileOperation.METADATA.requiresWriteAccess(),
          "METADATA should not require write access");

      LOGGER.info("Write access requirements verified");
    }
  }

  @Nested
  @DisplayName("Execute Access Tests")
  class ExecuteAccessTests {

    @Test
    @DisplayName("Should identify operations requiring execute access")
    void shouldIdentifyOperationsRequiringExecuteAccess() {
      LOGGER.info("Testing execute access requirements");

      // Only EXECUTE should require execute access
      assertTrue(
          WasiFileOperation.EXECUTE.requiresExecuteAccess(),
          "EXECUTE should require execute access");

      // All others should not require execute access
      assertFalse(
          WasiFileOperation.READ.requiresExecuteAccess(), "READ should not require execute access");
      assertFalse(
          WasiFileOperation.WRITE.requiresExecuteAccess(),
          "WRITE should not require execute access");
      assertFalse(
          WasiFileOperation.DELETE.requiresExecuteAccess(),
          "DELETE should not require execute access");
      assertFalse(
          WasiFileOperation.CREATE_DIRECTORY.requiresExecuteAccess(),
          "CREATE_DIRECTORY should not require execute access");

      LOGGER.info("Execute access requirements verified");
    }

    @ParameterizedTest
    @EnumSource(value = WasiFileOperation.class, names = "EXECUTE", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("All operations except EXECUTE should not require execute access")
    void allExceptExecuteShouldNotRequireExecuteAccess(final WasiFileOperation operation) {
      assertFalse(
          operation.requiresExecuteAccess(), operation + " should not require execute access");
    }
  }

  @Nested
  @DisplayName("Modifying Operation Tests")
  class ModifyingOperationTests {

    @Test
    @DisplayName("Should identify modifying operations")
    void shouldIdentifyModifyingOperations() {
      LOGGER.info("Testing modifying operations");

      // Operations that modify the file system
      assertTrue(WasiFileOperation.WRITE.isModifyingOperation(), "WRITE should be modifying");
      assertTrue(
          WasiFileOperation.CREATE_DIRECTORY.isModifyingOperation(),
          "CREATE_DIRECTORY should be modifying");
      assertTrue(WasiFileOperation.DELETE.isModifyingOperation(), "DELETE should be modifying");
      assertTrue(WasiFileOperation.RENAME.isModifyingOperation(), "RENAME should be modifying");
      assertTrue(
          WasiFileOperation.CHANGE_PERMISSIONS.isModifyingOperation(),
          "CHANGE_PERMISSIONS should be modifying");
      assertTrue(
          WasiFileOperation.CREATE_LINK.isModifyingOperation(), "CREATE_LINK should be modifying");
      assertTrue(
          WasiFileOperation.SET_TIMES.isModifyingOperation(), "SET_TIMES should be modifying");
      assertTrue(WasiFileOperation.TRUNCATE.isModifyingOperation(), "TRUNCATE should be modifying");

      // Non-modifying operations
      assertFalse(WasiFileOperation.READ.isModifyingOperation(), "READ should not be modifying");
      assertFalse(
          WasiFileOperation.METADATA.isModifyingOperation(), "METADATA should not be modifying");
      assertFalse(
          WasiFileOperation.EXECUTE.isModifyingOperation(), "EXECUTE should not be modifying");
      assertFalse(
          WasiFileOperation.SYNC.isModifyingOperation(),
          "SYNC should not be modifying (writes to disk, doesn't modify content)");

      LOGGER.info("Modifying operations verified");
    }
  }

  @Nested
  @DisplayName("Dangerous Operation Tests")
  class DangerousOperationTests {

    @Test
    @DisplayName("Should identify dangerous operations")
    void shouldIdentifyDangerousOperations() {
      LOGGER.info("Testing dangerous operations");

      // Dangerous operations
      assertTrue(WasiFileOperation.EXECUTE.isDangerous(), "EXECUTE should be dangerous");
      assertTrue(WasiFileOperation.DELETE.isDangerous(), "DELETE should be dangerous");
      assertTrue(WasiFileOperation.RENAME.isDangerous(), "RENAME should be dangerous");
      assertTrue(
          WasiFileOperation.CHANGE_PERMISSIONS.isDangerous(),
          "CHANGE_PERMISSIONS should be dangerous");
      assertTrue(WasiFileOperation.CREATE_LINK.isDangerous(), "CREATE_LINK should be dangerous");

      // Non-dangerous operations
      assertFalse(WasiFileOperation.READ.isDangerous(), "READ should not be dangerous");
      assertFalse(WasiFileOperation.WRITE.isDangerous(), "WRITE should not be dangerous");
      assertFalse(WasiFileOperation.METADATA.isDangerous(), "METADATA should not be dangerous");
      assertFalse(
          WasiFileOperation.CREATE_DIRECTORY.isDangerous(),
          "CREATE_DIRECTORY should not be dangerous");

      LOGGER.info("Dangerous operations verified");
    }
  }

  @Nested
  @DisplayName("Write Operation Alias Tests")
  class WriteOperationAliasTests {

    @Test
    @DisplayName("isWriteOperation should be alias for requiresWriteAccess")
    void isWriteOperationShouldBeAliasForRequiresWriteAccess() {
      LOGGER.info("Testing isWriteOperation alias");

      for (final WasiFileOperation operation : WasiFileOperation.values()) {
        assertEquals(
            operation.requiresWriteAccess(),
            operation.isWriteOperation(),
            "isWriteOperation should match requiresWriteAccess for " + operation);
      }

      LOGGER.info("isWriteOperation alias verified for all operations");
    }
  }

  @Nested
  @DisplayName("FromOperationId Tests")
  class FromOperationIdTests {

    @Test
    @DisplayName("Should convert valid operation IDs")
    void shouldConvertValidOperationIds() {
      LOGGER.info("Testing fromOperationId for valid IDs");

      assertEquals(WasiFileOperation.READ, WasiFileOperation.fromOperationId("read"));
      assertEquals(WasiFileOperation.WRITE, WasiFileOperation.fromOperationId("write"));
      assertEquals(WasiFileOperation.EXECUTE, WasiFileOperation.fromOperationId("execute"));
      assertEquals(
          WasiFileOperation.CREATE_DIRECTORY,
          WasiFileOperation.fromOperationId("create_directory"));
      assertEquals(WasiFileOperation.DELETE, WasiFileOperation.fromOperationId("delete"));
      assertEquals(WasiFileOperation.RENAME, WasiFileOperation.fromOperationId("rename"));
      assertEquals(WasiFileOperation.METADATA, WasiFileOperation.fromOperationId("metadata"));
      assertEquals(
          WasiFileOperation.CHANGE_PERMISSIONS,
          WasiFileOperation.fromOperationId("change_permissions"));
      assertEquals(WasiFileOperation.CREATE_LINK, WasiFileOperation.fromOperationId("create_link"));
      assertEquals(
          WasiFileOperation.FOLLOW_SYMLINKS, WasiFileOperation.fromOperationId("follow_symlinks"));
      assertEquals(
          WasiFileOperation.LIST_DIRECTORY, WasiFileOperation.fromOperationId("list_directory"));
      assertEquals(WasiFileOperation.SET_TIMES, WasiFileOperation.fromOperationId("set_times"));
      assertEquals(WasiFileOperation.TRUNCATE, WasiFileOperation.fromOperationId("truncate"));
      assertEquals(WasiFileOperation.SYNC, WasiFileOperation.fromOperationId("sync"));
      assertEquals(WasiFileOperation.SEEK, WasiFileOperation.fromOperationId("seek"));
      assertEquals(WasiFileOperation.POLL, WasiFileOperation.fromOperationId("poll"));
      assertEquals(WasiFileOperation.OPEN, WasiFileOperation.fromOperationId("open"));
      assertEquals(WasiFileOperation.CLOSE, WasiFileOperation.fromOperationId("close"));

      LOGGER.info("All valid operation IDs converted successfully");
    }

    @ParameterizedTest
    @EnumSource(WasiFileOperation.class)
    @DisplayName("Should roundtrip all operations through getOperationId and fromOperationId")
    void shouldRoundtripAllOperations(final WasiFileOperation original) {
      final String operationId = original.getOperationId();
      final WasiFileOperation result = WasiFileOperation.fromOperationId(operationId);
      assertEquals(original, result, "Roundtrip should return same operation for " + original);
    }

    @Test
    @DisplayName("Should reject null operation ID")
    void shouldRejectNullOperationId() {
      LOGGER.info("Testing null operation ID rejection");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> WasiFileOperation.fromOperationId(null));

      assertTrue(
          ex.getMessage().contains("null") || ex.getMessage().contains("empty"),
          "Error should mention null or empty: " + ex.getMessage());

      LOGGER.info("Correctly rejected null: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject empty operation ID")
    void shouldRejectEmptyOperationId() {
      LOGGER.info("Testing empty operation ID rejection");

      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> WasiFileOperation.fromOperationId(""));

      assertTrue(
          ex.getMessage().contains("null") || ex.getMessage().contains("empty"),
          "Error should mention null or empty: " + ex.getMessage());

      LOGGER.info("Correctly rejected empty: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should reject unknown operation ID")
    void shouldRejectUnknownOperationId() {
      LOGGER.info("Testing unknown operation ID rejection");

      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class,
              () -> WasiFileOperation.fromOperationId("unknown_operation"));

      assertTrue(
          ex.getMessage().contains("Unknown") || ex.getMessage().contains("unknown"),
          "Error should mention unknown: " + ex.getMessage());

      LOGGER.info("Correctly rejected unknown: " + ex.getMessage());
    }

    @Test
    @DisplayName("Should be case sensitive")
    void shouldBeCaseSensitive() {
      LOGGER.info("Testing case sensitivity");

      // Uppercase should fail
      assertThrows(IllegalArgumentException.class, () -> WasiFileOperation.fromOperationId("READ"));

      // Mixed case should fail
      assertThrows(IllegalArgumentException.class, () -> WasiFileOperation.fromOperationId("Read"));

      LOGGER.info("Case sensitivity verified");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @ParameterizedTest
    @EnumSource(WasiFileOperation.class)
    @DisplayName("Should produce formatted toString for all operations")
    void shouldProduceFormattedToString(final WasiFileOperation operation) {
      final String str = operation.toString();

      assertNotNull(str, "toString should not be null for " + operation);
      assertTrue(str.contains(operation.name()), "toString should contain operation name: " + str);
      assertTrue(
          str.contains(operation.getDescription()), "toString should contain description: " + str);

      LOGGER.info(operation.name() + ".toString() = " + str);
    }

    @Test
    @DisplayName("Should format toString correctly")
    void shouldFormatToStringCorrectly() {
      LOGGER.info("Testing toString format");

      // toString format is: NAME (description)
      final String str = WasiFileOperation.READ.toString();
      assertTrue(str.startsWith("READ"), "Should start with name: " + str);
      assertTrue(str.contains("("), "Should contain opening paren: " + str);
      assertTrue(str.contains(")"), "Should contain closing paren: " + str);

      LOGGER.info("toString format verified: " + str);
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("Should have expected number of operations")
    void shouldHaveExpectedNumberOfOperations() {
      LOGGER.info("Testing operation count");

      final WasiFileOperation[] values = WasiFileOperation.values();
      assertEquals(18, values.length, "Should have 18 file operations");

      LOGGER.info("Operation count: " + values.length);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing ordinal values");

      assertEquals(0, WasiFileOperation.READ.ordinal());
      assertEquals(1, WasiFileOperation.WRITE.ordinal());
      assertEquals(2, WasiFileOperation.EXECUTE.ordinal());
      // First few ordinals should be in declaration order

      LOGGER.info("Ordinal values verified");
    }

    @Test
    @DisplayName("Should convert from name using valueOf")
    void shouldConvertFromNameUsingValueOf() {
      LOGGER.info("Testing valueOf");

      assertEquals(WasiFileOperation.READ, WasiFileOperation.valueOf("READ"));
      assertEquals(WasiFileOperation.WRITE, WasiFileOperation.valueOf("WRITE"));
      assertEquals(WasiFileOperation.DELETE, WasiFileOperation.valueOf("DELETE"));

      LOGGER.info("valueOf working correctly");
    }
  }

  @Nested
  @DisplayName("Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("All modifying operations should require write access")
    void allModifyingOperationsShouldRequireWriteAccess() {
      LOGGER.info("Testing modifying->write consistency");

      int count = 0;
      for (final WasiFileOperation op : WasiFileOperation.values()) {
        if (op.isModifyingOperation()) {
          assertTrue(
              op.requiresWriteAccess(), op + " is modifying but doesn't require write access");
          count++;
        }
      }

      LOGGER.info("Verified " + count + " modifying operations require write access");
    }

    @Test
    @DisplayName("All dangerous operations should be specific types")
    void allDangerousOperationsShouldBeSpecificTypes() {
      LOGGER.info("Testing dangerous operation categories");

      // Dangerous operations are: EXECUTE, DELETE, RENAME, CHANGE_PERMISSIONS, CREATE_LINK
      int dangerousCount = 0;
      for (final WasiFileOperation op : WasiFileOperation.values()) {
        if (op.isDangerous()) {
          dangerousCount++;
          assertTrue(
              op == WasiFileOperation.EXECUTE
                  || op == WasiFileOperation.DELETE
                  || op == WasiFileOperation.RENAME
                  || op == WasiFileOperation.CHANGE_PERMISSIONS
                  || op == WasiFileOperation.CREATE_LINK,
              "Unexpected dangerous operation: " + op);
        }
      }

      assertEquals(5, dangerousCount, "Should have exactly 5 dangerous operations");

      LOGGER.info("Verified " + dangerousCount + " dangerous operations");
    }
  }
}
