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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for WASI data classes (POJOs).
 *
 * <p>Tests WasiFileStat, WasiFileMetadata, WasiDirectoryEntry, WasiSubscription, and WasiEvent.
 */
@DisplayName("WASI Data Classes Tests")
public class WasiDataClassesTest {

  private static final Logger LOGGER = Logger.getLogger(WasiDataClassesTest.class.getName());

  @Nested
  @DisplayName("WasiFileStat Tests")
  class WasiFileStatTests {

    @Test
    @DisplayName("Should create file stat with all fields")
    void shouldCreateFileStatWithAllFields() {
      LOGGER.info("Testing WasiFileStat constructor");

      final WasiFileStat stat =
          new WasiFileStat(
              1L, // device
              12345L, // inode
              4, // fileType (regular file)
              1L, // linkCount
              1024L, // size
              1000000000L, // accessTime
              2000000000L, // modificationTime
              3000000000L // changeTime
              );

      assertEquals(1L, stat.getDevice(), "Device should match");
      assertEquals(12345L, stat.getInode(), "Inode should match");
      assertEquals(4, stat.getFileType(), "FileType should match");
      assertEquals(1L, stat.getLinkCount(), "LinkCount should match");
      assertEquals(1024L, stat.getSize(), "Size should match");
      assertEquals(1000000000L, stat.getAccessTime(), "AccessTime should match");
      assertEquals(2000000000L, stat.getModificationTime(), "ModificationTime should match");
      assertEquals(3000000000L, stat.getChangeTime(), "ChangeTime should match");

      LOGGER.info("Created: " + stat);
    }

    @Test
    @DisplayName("Should handle zero values")
    void shouldHandleZeroValues() {
      LOGGER.info("Testing WasiFileStat with zero values");

      final WasiFileStat stat = new WasiFileStat(0, 0, 0, 0, 0, 0, 0, 0);

      assertEquals(0L, stat.getDevice());
      assertEquals(0L, stat.getInode());
      assertEquals(0, stat.getFileType());
      assertEquals(0L, stat.getLinkCount());
      assertEquals(0L, stat.getSize());
      assertEquals(0L, stat.getAccessTime());
      assertEquals(0L, stat.getModificationTime());
      assertEquals(0L, stat.getChangeTime());

      LOGGER.info("Zero values handled correctly");
    }

    @Test
    @DisplayName("Should handle max values")
    void shouldHandleMaxValues() {
      LOGGER.info("Testing WasiFileStat with max values");

      final WasiFileStat stat =
          new WasiFileStat(
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Integer.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, stat.getDevice());
      assertEquals(Long.MAX_VALUE, stat.getSize());

      LOGGER.info("Max values handled correctly");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing WasiFileStat toString");

      final WasiFileStat stat = new WasiFileStat(1, 2, 3, 4, 5, 6, 7, 8);
      final String str = stat.toString();

      assertNotNull(str);
      assertTrue(str.contains("WasiFileStat"));
      assertTrue(str.contains("device=1"));
      assertTrue(str.contains("inode=2"));
      assertTrue(str.contains("size=5"));

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasiFileMetadata Tests")
  class WasiFileMetadataTests {

    private final FileTime testTime = FileTime.from(Instant.ofEpochSecond(1609459200, 500000000));

    @Test
    @DisplayName("Should create metadata for regular file")
    void shouldCreateMetadataForRegularFile() {
      LOGGER.info("Testing WasiFileMetadata for regular file");

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1024L, testTime, testTime, testTime, true, false, false, true, true, false);

      assertEquals(1024L, metadata.getSize());
      assertEquals(testTime, metadata.getLastModifiedTime());
      assertEquals(testTime, metadata.getLastAccessTime());
      assertEquals(testTime, metadata.getCreationTime());
      assertTrue(metadata.isRegularFile());
      assertFalse(metadata.isDirectory());
      assertFalse(metadata.isSymbolicLink());
      assertTrue(metadata.isReadable());
      assertTrue(metadata.isWritable());
      assertFalse(metadata.isExecutable());

      LOGGER.info("Created regular file metadata: " + metadata);
    }

    @Test
    @DisplayName("Should create metadata for directory")
    void shouldCreateMetadataForDirectory() {
      LOGGER.info("Testing WasiFileMetadata for directory");

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L, testTime, testTime, testTime, false, true, false, true, false, true);

      assertFalse(metadata.isRegularFile());
      assertTrue(metadata.isDirectory());
      assertFalse(metadata.isSymbolicLink());
      assertTrue(metadata.isReadable());
      assertFalse(metadata.isWritable());
      assertTrue(metadata.isExecutable());

      LOGGER.info("Created directory metadata");
    }

    @Test
    @DisplayName("Should create metadata for symbolic link")
    void shouldCreateMetadataForSymbolicLink() {
      LOGGER.info("Testing WasiFileMetadata for symbolic link");

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L, testTime, testTime, testTime, false, false, true, true, true, true);

      assertFalse(metadata.isRegularFile());
      assertFalse(metadata.isDirectory());
      assertTrue(metadata.isSymbolicLink());

      LOGGER.info("Created symbolic link metadata");
    }

    @Test
    @DisplayName("Should get time in seconds and nanos")
    void shouldGetTimeInSecondsAndNanos() {
      LOGGER.info("Testing time conversion methods");

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1024L, testTime, testTime, testTime, true, false, false, true, true, false);

      assertEquals(1609459200L, metadata.getLastModifiedTimeSeconds());
      assertEquals(500000000L, metadata.getLastModifiedTimeNanos());
      assertEquals(1609459200L, metadata.getLastAccessTimeSeconds());
      assertEquals(500000000L, metadata.getLastAccessTimeNanos());
      assertEquals(1609459200L, metadata.getCreationTimeSeconds());
      assertEquals(500000000L, metadata.getCreationTimeNanos());

      LOGGER.info("Time conversions verified");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing WasiFileMetadata toString");

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1024L, testTime, testTime, testTime, true, false, false, true, true, false);

      final String str = metadata.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiFileMetadata"));
      assertTrue(str.contains("size=1024"));
      assertTrue(str.contains("regularFile=true"));

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasiDirectoryEntry Tests")
  class WasiDirectoryEntryTests {

    private final FileTime testTime = FileTime.from(Instant.ofEpochSecond(1609459200));

    @Test
    @DisplayName("Should create entry for regular file")
    void shouldCreateEntryForRegularFile() {
      LOGGER.info("Testing WasiDirectoryEntry for regular file");

      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);

      assertEquals("test.txt", entry.getName());
      assertTrue(entry.isRegularFile());
      assertFalse(entry.isDirectory());
      assertFalse(entry.isSymbolicLink());
      assertEquals(1024L, entry.getSize());
      assertEquals(testTime, entry.getLastModifiedTime());
      assertEquals(4, entry.getWasiFileType()); // WASI_FILETYPE_REGULAR_FILE

      LOGGER.info("Created: " + entry);
    }

    @Test
    @DisplayName("Should create entry for directory")
    void shouldCreateEntryForDirectory() {
      LOGGER.info("Testing WasiDirectoryEntry for directory");

      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("subdir", false, true, false, 0L, testTime);

      assertEquals("subdir", entry.getName());
      assertFalse(entry.isRegularFile());
      assertTrue(entry.isDirectory());
      assertFalse(entry.isSymbolicLink());
      assertEquals(3, entry.getWasiFileType()); // WASI_FILETYPE_DIRECTORY

      LOGGER.info("Created directory entry");
    }

    @Test
    @DisplayName("Should create entry for symbolic link")
    void shouldCreateEntryForSymbolicLink() {
      LOGGER.info("Testing WasiDirectoryEntry for symbolic link");

      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("link", false, false, true, 0L, testTime);

      assertTrue(entry.isSymbolicLink());
      assertEquals(7, entry.getWasiFileType()); // WASI_FILETYPE_SYMBOLIC_LINK

      LOGGER.info("Created symbolic link entry");
    }

    @Test
    @DisplayName("Should return unknown file type for unclassified entry")
    void shouldReturnUnknownFileType() {
      LOGGER.info("Testing WasiDirectoryEntry for unknown type");

      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("unknown", false, false, false, 0L, testTime);

      assertEquals(0, entry.getWasiFileType()); // WASI_FILETYPE_UNKNOWN

      LOGGER.info("Unknown file type verified");
    }

    @Test
    @DisplayName("Should reject null name")
    void shouldRejectNullName() {
      LOGGER.info("Testing null name rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry(null, true, false, false, 0L, testTime));

      LOGGER.info("Null name rejected");
    }

    @Test
    @DisplayName("Should reject empty name")
    void shouldRejectEmptyName() {
      LOGGER.info("Testing empty name rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry("", true, false, false, 0L, testTime));

      LOGGER.info("Empty name rejected");
    }

    @Test
    @DisplayName("Should reject null lastModifiedTime")
    void shouldRejectNullLastModifiedTime() {
      LOGGER.info("Testing null lastModifiedTime rejection");

      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry("test.txt", true, false, false, 0L, null));

      LOGGER.info("Null lastModifiedTime rejected");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing equals implementation");

      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);
      final WasiDirectoryEntry entry3 =
          new WasiDirectoryEntry("other.txt", true, false, false, 1024L, testTime);

      assertEquals(entry1, entry1, "Same object should be equal");
      assertEquals(entry1, entry2, "Equal objects should be equal");
      assertNotEquals(entry1, entry3, "Different names should not be equal");
      assertNotEquals(entry1, null, "Should not equal null");
      assertNotEquals(entry1, "string", "Should not equal different type");

      LOGGER.info("Equals implementation verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing hashCode implementation");

      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);

      assertEquals(
          entry1.hashCode(), entry2.hashCode(), "Equal objects should have equal hash codes");

      LOGGER.info("HashCode implementation verified");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, testTime);

      final String str = entry.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiDirectoryEntry"));
      assertTrue(str.contains("test.txt"));
      assertTrue(str.contains("FILE"));
      assertTrue(str.contains("1024"));

      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should show DIR in toString for directory")
    void shouldShowDirInToString() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("subdir", false, true, false, 0L, testTime);

      assertTrue(entry.toString().contains("DIR"));
    }

    @Test
    @DisplayName("Should show LINK in toString for symbolic link")
    void shouldShowLinkInToString() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("link", false, false, true, 0L, testTime);

      assertTrue(entry.toString().contains("LINK"));
    }

    @Test
    @DisplayName("Should show UNKNOWN in toString for unclassified")
    void shouldShowUnknownInToString() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("unknown", false, false, false, 0L, testTime);

      assertTrue(entry.toString().contains("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("WasiSubscription Tests")
  class WasiSubscriptionTests {

    @Test
    @DisplayName("Should create empty subscription")
    void shouldCreateEmptySubscription() {
      LOGGER.info("Testing default constructor");

      final WasiSubscription sub = new WasiSubscription();

      assertEquals(0L, sub.getUserData());
      assertEquals(0, sub.getType());
      assertEquals(0, sub.getFd());
      assertEquals(0, sub.getFlags());

      LOGGER.info("Created empty subscription: " + sub);
    }

    @Test
    @DisplayName("Should set and get user data")
    void shouldSetAndGetUserData() {
      LOGGER.info("Testing userData getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setUserData(12345L);

      assertEquals(12345L, sub.getUserData());

      LOGGER.info("UserData set/get verified");
    }

    @Test
    @DisplayName("Should set and get type")
    void shouldSetAndGetType() {
      LOGGER.info("Testing type getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setType(1);

      assertEquals(1, sub.getType());

      LOGGER.info("Type set/get verified");
    }

    @Test
    @DisplayName("Should set and get fd")
    void shouldSetAndGetFd() {
      LOGGER.info("Testing fd getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFd(42);

      assertEquals(42, sub.getFd());

      LOGGER.info("Fd set/get verified");
    }

    @Test
    @DisplayName("Should set and get flags")
    void shouldSetAndGetFlags() {
      LOGGER.info("Testing flags getter/setter");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFlags(0xFF);

      assertEquals(0xFF, sub.getFlags());

      LOGGER.info("Flags set/get verified");
    }

    @Test
    @DisplayName("Should set fd readwrite")
    void shouldSetFdReadwrite() {
      LOGGER.info("Testing setFdReadwrite");

      final WasiSubscription sub = new WasiSubscription();
      sub.setFdReadwrite(10, 3);

      assertEquals(10, sub.getFd());
      assertEquals(3, sub.getFlags());

      LOGGER.info("FdReadwrite set verified");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiSubscription sub = new WasiSubscription();
      sub.setUserData(100L);
      sub.setType(1);
      sub.setFd(5);
      sub.setFlags(2);

      final String str = sub.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiSubscription"));
      assertTrue(str.contains("userData=100"));
      assertTrue(str.contains("type=1"));
      assertTrue(str.contains("fd=5"));
      assertTrue(str.contains("flags=2"));

      LOGGER.info("toString: " + str);
    }
  }

  @Nested
  @DisplayName("WasiEvent Tests")
  class WasiEventTests {

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
      LOGGER.info("Testing WasiEvent constructor");

      final WasiEvent event = new WasiEvent(12345L, 0, 1, 1024);

      assertEquals(12345L, event.getUserData());
      assertEquals(0, event.getError());
      assertEquals(1, event.getType());
      assertEquals(1024, event.getNbytes());
      assertFalse(event.hasError());

      LOGGER.info("Created: " + event);
    }

    @Test
    @DisplayName("Should detect error condition")
    void shouldDetectErrorCondition() {
      LOGGER.info("Testing hasError");

      final WasiEvent successEvent = new WasiEvent(1L, 0, 1, 0);
      final WasiEvent errorEvent = new WasiEvent(1L, 1, 1, 0);

      assertFalse(successEvent.hasError(), "Error=0 should not indicate error");
      assertTrue(errorEvent.hasError(), "Error>0 should indicate error");

      LOGGER.info("Error detection verified");
    }

    @Test
    @DisplayName("Should handle negative error codes")
    void shouldHandleNegativeErrorCodes() {
      LOGGER.info("Testing negative error codes");

      final WasiEvent event = new WasiEvent(1L, -1, 1, 0);

      assertTrue(event.hasError(), "Negative error code should indicate error");
      assertEquals(-1, event.getError());

      LOGGER.info("Negative error codes handled");
    }

    @Test
    @DisplayName("Should produce readable toString")
    void shouldProduceReadableToString() {
      LOGGER.info("Testing toString");

      final WasiEvent event = new WasiEvent(100L, 0, 2, 512);

      final String str = event.toString();
      assertNotNull(str);
      assertTrue(str.contains("WasiEvent"));
      assertTrue(str.contains("userData=100"));
      assertTrue(str.contains("error=0"));
      assertTrue(str.contains("type=2"));
      assertTrue(str.contains("nbytes=512"));

      LOGGER.info("toString: " + str);
    }

    @Test
    @DisplayName("Should handle max values")
    void shouldHandleMaxValues() {
      LOGGER.info("Testing max values");

      final WasiEvent event =
          new WasiEvent(Long.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, event.getUserData());
      assertEquals(Integer.MAX_VALUE, event.getError());
      assertEquals(Integer.MAX_VALUE, event.getType());
      assertEquals(Integer.MAX_VALUE, event.getNbytes());

      LOGGER.info("Max values handled");
    }
  }
}
