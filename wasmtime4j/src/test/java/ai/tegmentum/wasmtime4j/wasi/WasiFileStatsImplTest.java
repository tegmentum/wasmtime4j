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

package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasiFileStatsImpl} package-private class.
 *
 * <p>Verifies builder construction, field accessors, validation, defaults, equals/hashCode, and
 * toString behavior. Tests access WasiFileStatsImpl through the WasiFileStats.builder() factory.
 */
@DisplayName("WasiFileStatsImpl Tests")
class WasiFileStatsImplTest {

  @Nested
  @DisplayName("Builder Construction Tests")
  class BuilderConstructionTests {

    @Test
    @DisplayName("should build with all fields set")
    void shouldBuildWithAllFieldsSet() {
      final Instant now = Instant.now();
      final WasiPermissions permissions = WasiPermissions.of(0644);
      final WasiFileStats stats =
          WasiFileStats.builder()
              .device(1L)
              .inode(2L)
              .fileType(WasiFileType.REGULAR_FILE)
              .linkCount(3L)
              .size(4096L)
              .accessTime(now)
              .modificationTime(now)
              .statusChangeTime(now)
              .creationTime(now)
              .permissions(permissions)
              .build();

      assertEquals(1L, stats.getDevice(), "Device should be 1");
      assertEquals(2L, stats.getInode(), "Inode should be 2");
      assertEquals(WasiFileType.REGULAR_FILE, stats.getFileType(), "FileType should be REGULAR_FILE");
      assertEquals(3L, stats.getLinkCount(), "LinkCount should be 3");
      assertEquals(4096L, stats.getSize(), "Size should be 4096");
      assertEquals(now, stats.getAccessTime(), "AccessTime should match");
      assertEquals(now, stats.getModificationTime(), "ModificationTime should match");
      assertEquals(now, stats.getStatusChangeTime(), "StatusChangeTime should match");
      assertEquals(now, stats.getCreationTime(), "CreationTime should match");
      assertEquals(permissions, stats.getPermissions(), "Permissions should match");
    }

    @Test
    @DisplayName("should build with minimal fields and default permissions for file")
    void shouldBuildWithMinimalFieldsForFile() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.REGULAR_FILE)
              .build();

      assertEquals(0L, stats.getDevice(), "Device should default to 0");
      assertEquals(0L, stats.getInode(), "Inode should default to 0");
      assertEquals(WasiFileType.REGULAR_FILE, stats.getFileType(), "FileType should be REGULAR_FILE");
      assertEquals(1L, stats.getLinkCount(), "LinkCount should default to 1");
      assertEquals(0L, stats.getSize(), "Size should default to 0");
      assertNull(stats.getAccessTime(), "AccessTime should be null by default");
      assertNull(stats.getModificationTime(), "ModificationTime should be null by default");
      assertNull(stats.getStatusChangeTime(), "StatusChangeTime should be null by default");
      assertNull(stats.getCreationTime(), "CreationTime should be null by default");
      assertNotNull(stats.getPermissions(), "Default permissions should be applied for file");
      assertEquals(
          0644, stats.getPermissions().getMode(),
          "Default file permissions should be 0644");
    }

    @Test
    @DisplayName("should apply default directory permissions when no permissions set")
    void shouldApplyDefaultDirectoryPermissions() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.DIRECTORY)
              .build();

      assertEquals(
          0755, stats.getPermissions().getMode(),
          "Default directory permissions should be 0755");
    }

    @Test
    @DisplayName("should throw when fileType is not set")
    void shouldThrowWhenFileTypeNotSet() {
      final WasiFileStats.Builder builder = WasiFileStats.builder();
      assertThrows(
          IllegalStateException.class,
          builder::build,
          "Should throw when fileType is not set");
    }

    @Test
    @DisplayName("should throw when fileType is null")
    void shouldThrowWhenFileTypeIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> WasiFileStats.builder().fileType(null),
          "Should throw when fileType is null");
    }

    @Test
    @DisplayName("should throw when permissions is null in builder")
    void shouldThrowWhenPermissionsIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> WasiFileStats.builder().permissions(null),
          "Should throw when permissions is null");
    }
  }

  @Nested
  @DisplayName("Builder Validation Tests")
  class BuilderValidationTests {

    @Test
    @DisplayName("should throw for negative link count")
    void shouldThrowForNegativeLinkCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileStats.builder().linkCount(-1),
          "Should throw for negative link count");
    }

    @Test
    @DisplayName("should throw for negative size")
    void shouldThrowForNegativeSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileStats.builder().size(-1),
          "Should throw for negative size");
    }

    @Test
    @DisplayName("should accept zero link count")
    void shouldAcceptZeroLinkCount() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.REGULAR_FILE)
              .linkCount(0)
              .build();
      assertEquals(0L, stats.getLinkCount(), "Link count should be 0");
    }

    @Test
    @DisplayName("should accept zero size")
    void shouldAcceptZeroSize() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.REGULAR_FILE)
              .size(0)
              .build();
      assertEquals(0L, stats.getSize(), "Size should be 0");
    }
  }

  @Nested
  @DisplayName("Default Interface Method Tests")
  class DefaultInterfaceMethodTests {

    @Test
    @DisplayName("isFile should return true for REGULAR_FILE")
    void isFileShouldReturnTrueForRegularFile() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.REGULAR_FILE)
              .build();
      assertTrue(stats.isFile(), "isFile should be true for REGULAR_FILE");
    }

    @Test
    @DisplayName("isDirectory should return true for DIRECTORY")
    void isDirectoryShouldReturnTrueForDirectory() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.DIRECTORY)
              .build();
      assertTrue(stats.isDirectory(), "isDirectory should be true for DIRECTORY");
    }

    @Test
    @DisplayName("isSymbolicLink should return true for SYMBOLIC_LINK")
    void isSymbolicLinkShouldReturnTrueForSymbolicLink() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.SYMBOLIC_LINK)
              .build();
      assertTrue(stats.isSymbolicLink(), "isSymbolicLink should be true for SYMBOLIC_LINK");
    }

    @Test
    @DisplayName("isSpecialFile should return true for BLOCK_DEVICE")
    void isSpecialFileShouldReturnTrueForBlockDevice() {
      final WasiFileStats stats =
          WasiFileStats.builder()
              .fileType(WasiFileType.BLOCK_DEVICE)
              .build();
      assertTrue(stats.isSpecialFile(), "isSpecialFile should be true for BLOCK_DEVICE");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal stats should be equal")
    void equalStatsShouldBeEqual() {
      final Instant now = Instant.now();
      final WasiPermissions permissions = WasiPermissions.of(0644);
      final WasiFileStats stats1 =
          WasiFileStats.builder()
              .device(1L).inode(2L).fileType(WasiFileType.REGULAR_FILE)
              .linkCount(1).size(100).accessTime(now).modificationTime(now)
              .statusChangeTime(now).creationTime(now).permissions(permissions)
              .build();
      final WasiFileStats stats2 =
          WasiFileStats.builder()
              .device(1L).inode(2L).fileType(WasiFileType.REGULAR_FILE)
              .linkCount(1).size(100).accessTime(now).modificationTime(now)
              .statusChangeTime(now).creationTime(now).permissions(permissions)
              .build();

      assertEquals(stats1, stats2, "Stats with same values should be equal");
      assertEquals(
          stats1.hashCode(), stats2.hashCode(),
          "Stats with same values should have same hashCode");
    }

    @Test
    @DisplayName("stats with different devices should not be equal")
    void statsWithDifferentDevicesShouldNotBeEqual() {
      final WasiFileStats stats1 =
          WasiFileStats.builder().device(1L).fileType(WasiFileType.REGULAR_FILE).build();
      final WasiFileStats stats2 =
          WasiFileStats.builder().device(2L).fileType(WasiFileType.REGULAR_FILE).build();
      assertNotEquals(stats1, stats2, "Stats with different devices should not be equal");
    }

    @Test
    @DisplayName("stats with different file types should not be equal")
    void statsWithDifferentFileTypesShouldNotBeEqual() {
      final WasiFileStats stats1 =
          WasiFileStats.builder().fileType(WasiFileType.REGULAR_FILE).build();
      final WasiFileStats stats2 =
          WasiFileStats.builder().fileType(WasiFileType.DIRECTORY).build();
      assertNotEquals(stats1, stats2, "Stats with different file types should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final WasiFileStats stats =
          WasiFileStats.builder().fileType(WasiFileType.REGULAR_FILE).build();
      assertNotEquals(null, stats, "Stats should not equal null");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain file type information")
    void toStringShouldContainFileType() {
      final WasiFileStats stats =
          WasiFileStats.builder().fileType(WasiFileType.REGULAR_FILE).size(1024).build();
      final String result = stats.toString();
      assertTrue(
          result.contains("REGULAR_FILE"),
          "toString should contain file type: " + result);
      assertTrue(
          result.contains("1024"),
          "toString should contain size: " + result);
    }
  }
}
