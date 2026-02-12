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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileStat} class.
 *
 * <p>WasiFileStat represents file metadata returned by WASI filestat operations.
 */
@DisplayName("WasiFileStat Class Tests")
class WasiFileStatTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create file stat with all fields")
    void shouldCreateFileStatWithAllFields() {
      final WasiFileStat stat =
          new WasiFileStat(
              1L, // device
              100L, // inode
              4, // fileType (REGULAR_FILE)
              2L, // linkCount
              1024L, // size
              1000000000L, // accessTime
              2000000000L, // modificationTime
              3000000000L // changeTime
              );

      assertNotNull(stat, "WasiFileStat should be created");
    }

    @Test
    @DisplayName("should create file stat with zero values")
    void shouldCreateFileStatWithZeroValues() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 0, 0L, 0L, 0L, 0L, 0L);

      assertNotNull(stat, "WasiFileStat should be created with zero values");
      assertEquals(0L, stat.getDevice());
      assertEquals(0L, stat.getInode());
      assertEquals(0, stat.getFileType());
      assertEquals(0L, stat.getLinkCount());
      assertEquals(0L, stat.getSize());
      assertEquals(0L, stat.getAccessTime());
      assertEquals(0L, stat.getModificationTime());
      assertEquals(0L, stat.getChangeTime());
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    private final WasiFileStat stat =
        new WasiFileStat(
            123L, // device
            456L, // inode
            4, // fileType
            3L, // linkCount
            2048L, // size
            1609459200000000000L, // accessTime (Jan 1, 2021 in nanoseconds)
            1612137600000000000L, // modificationTime (Feb 1, 2021 in nanoseconds)
            1614556800000000000L // changeTime (Mar 1, 2021 in nanoseconds)
            );

    @Test
    @DisplayName("getDevice should return device ID")
    void getDeviceShouldReturnDeviceId() {
      assertEquals(123L, stat.getDevice());
    }

    @Test
    @DisplayName("getInode should return inode number")
    void getInodeShouldReturnInodeNumber() {
      assertEquals(456L, stat.getInode());
    }

    @Test
    @DisplayName("getFileType should return file type")
    void getFileTypeShouldReturnFileType() {
      assertEquals(4, stat.getFileType());
    }

    @Test
    @DisplayName("getLinkCount should return link count")
    void getLinkCountShouldReturnLinkCount() {
      assertEquals(3L, stat.getLinkCount());
    }

    @Test
    @DisplayName("getSize should return file size")
    void getSizeShouldReturnFileSize() {
      assertEquals(2048L, stat.getSize());
    }

    @Test
    @DisplayName("getAccessTime should return access time")
    void getAccessTimeShouldReturnAccessTime() {
      assertEquals(1609459200000000000L, stat.getAccessTime());
    }

    @Test
    @DisplayName("getModificationTime should return modification time")
    void getModificationTimeShouldReturnModificationTime() {
      assertEquals(1612137600000000000L, stat.getModificationTime());
    }

    @Test
    @DisplayName("getChangeTime should return change time")
    void getChangeTimeShouldReturnChangeTime() {
      assertEquals(1614556800000000000L, stat.getChangeTime());
    }
  }

  @Nested
  @DisplayName("File Type Tests")
  class FileTypeTests {

    @Test
    @DisplayName("file type 0 should represent UNKNOWN")
    void fileTypeZeroShouldRepresentUnknown() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 0, 0L, 0L, 0L, 0L, 0L);
      assertEquals(0, stat.getFileType());
    }

    @Test
    @DisplayName("file type 3 should represent DIRECTORY")
    void fileTypeThreeShouldRepresentDirectory() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 3, 0L, 0L, 0L, 0L, 0L);
      assertEquals(3, stat.getFileType());
    }

    @Test
    @DisplayName("file type 4 should represent REGULAR_FILE")
    void fileTypeFourShouldRepresentRegularFile() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 4, 0L, 0L, 0L, 0L, 0L);
      assertEquals(4, stat.getFileType());
    }

    @Test
    @DisplayName("file type 7 should represent SYMBOLIC_LINK")
    void fileTypeSevenShouldRepresentSymbolicLink() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 7, 0L, 0L, 0L, 0L, 0L);
      assertEquals(7, stat.getFileType());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all fields")
    void toStringShouldIncludeAllFields() {
      final WasiFileStat stat = new WasiFileStat(1L, 2L, 4, 3L, 1024L, 100L, 200L, 300L);
      final String str = stat.toString();

      assertTrue(str.contains("WasiFileStat"), "Should contain class name");
      assertTrue(str.contains("device=1"), "Should contain device");
      assertTrue(str.contains("inode=2"), "Should contain inode");
      assertTrue(str.contains("fileType=4"), "Should contain fileType");
      assertTrue(str.contains("linkCount=3"), "Should contain linkCount");
      assertTrue(str.contains("size=1024"), "Should contain size");
      assertTrue(str.contains("accessTime=100"), "Should contain accessTime");
      assertTrue(str.contains("modificationTime=200"), "Should contain modificationTime");
      assertTrue(str.contains("changeTime=300"), "Should contain changeTime");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle Long.MAX_VALUE for size")
    void shouldHandleMaxValueForSize() {
      final WasiFileStat stat = new WasiFileStat(0L, 0L, 4, 0L, Long.MAX_VALUE, 0L, 0L, 0L);
      assertEquals(Long.MAX_VALUE, stat.getSize());
    }

    @Test
    @DisplayName("should handle large nanosecond timestamps")
    void shouldHandleLargeNanosecondTimestamps() {
      final long futureTime = 2524608000000000000L; // Year 2050 in nanoseconds
      final WasiFileStat stat =
          new WasiFileStat(0L, 0L, 4, 0L, 0L, futureTime, futureTime, futureTime);

      assertEquals(futureTime, stat.getAccessTime());
      assertEquals(futureTime, stat.getModificationTime());
      assertEquals(futureTime, stat.getChangeTime());
    }

    @Test
    @DisplayName("should handle negative values (wrap-around)")
    void shouldHandleNegativeValues() {
      final WasiFileStat stat = new WasiFileStat(-1L, -1L, -1, -1L, -1L, -1L, -1L, -1L);

      assertEquals(-1L, stat.getDevice());
      assertEquals(-1L, stat.getInode());
      assertEquals(-1, stat.getFileType());
      assertEquals(-1L, stat.getLinkCount());
      assertEquals(-1L, stat.getSize());
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("WasiFileStat should be final class")
    void wasiFileStatShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiFileStat.class.getModifiers()),
          "WasiFileStat should be final");
    }

    @Test
    @DisplayName("should have no setter methods")
    void shouldHaveNoSetterMethods() {
      final java.lang.reflect.Method[] methods = WasiFileStat.class.getDeclaredMethods();
      for (final java.lang.reflect.Method method : methods) {
        assertTrue(
            !method.getName().startsWith("set"),
            "Should not have setter method: " + method.getName());
      }
    }
  }
}
