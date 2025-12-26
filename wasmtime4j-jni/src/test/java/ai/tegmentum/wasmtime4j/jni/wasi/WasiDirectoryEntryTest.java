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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiDirectoryEntry} class.
 *
 * <p>WasiDirectoryEntry represents a single entry in a WASI directory listing.
 */
@DisplayName("WasiDirectoryEntry Class Tests")
class WasiDirectoryEntryTest {

  private static final FileTime NOW = FileTime.from(Instant.now());

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create directory entry for regular file")
    void shouldCreateDirectoryEntryForRegularFile() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, NOW);

      assertNotNull(entry, "Entry should be created");
      assertEquals("test.txt", entry.getName());
      assertTrue(entry.isRegularFile());
      assertFalse(entry.isDirectory());
      assertFalse(entry.isSymbolicLink());
      assertEquals(1024L, entry.getSize());
      assertEquals(NOW, entry.getLastModifiedTime());
    }

    @Test
    @DisplayName("should create directory entry for directory")
    void shouldCreateDirectoryEntryForDirectory() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("subdir", false, true, false, 0L, NOW);

      assertNotNull(entry, "Entry should be created");
      assertEquals("subdir", entry.getName());
      assertFalse(entry.isRegularFile());
      assertTrue(entry.isDirectory());
      assertFalse(entry.isSymbolicLink());
      assertEquals(0L, entry.getSize());
    }

    @Test
    @DisplayName("should create directory entry for symbolic link")
    void shouldCreateDirectoryEntryForSymbolicLink() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("link", false, false, true, 100L, NOW);

      assertNotNull(entry, "Entry should be created");
      assertEquals("link", entry.getName());
      assertFalse(entry.isRegularFile());
      assertFalse(entry.isDirectory());
      assertTrue(entry.isSymbolicLink());
    }

    @Test
    @DisplayName("should throw for null name")
    void shouldThrowForNullName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry(null, true, false, false, 0L, NOW),
          "Should throw for null name");
    }

    @Test
    @DisplayName("should throw for empty name")
    void shouldThrowForEmptyName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry("", true, false, false, 0L, NOW),
          "Should throw for empty name");
    }

    @Test
    @DisplayName("should throw for null lastModifiedTime")
    void shouldThrowForNullLastModifiedTime() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new WasiDirectoryEntry("test.txt", true, false, false, 0L, null),
          "Should throw for null lastModifiedTime");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    private final WasiDirectoryEntry entry =
        new WasiDirectoryEntry("example.txt", true, false, false, 2048L, NOW);

    @Test
    @DisplayName("getName should return name")
    void getNameShouldReturnName() {
      assertEquals("example.txt", entry.getName());
    }

    @Test
    @DisplayName("isRegularFile should return correct value")
    void isRegularFileShouldReturnCorrectValue() {
      assertTrue(entry.isRegularFile());
    }

    @Test
    @DisplayName("isDirectory should return correct value")
    void isDirectoryShouldReturnCorrectValue() {
      assertFalse(entry.isDirectory());
    }

    @Test
    @DisplayName("isSymbolicLink should return correct value")
    void isSymbolicLinkShouldReturnCorrectValue() {
      assertFalse(entry.isSymbolicLink());
    }

    @Test
    @DisplayName("getSize should return correct size")
    void getSizeShouldReturnCorrectSize() {
      assertEquals(2048L, entry.getSize());
    }

    @Test
    @DisplayName("getLastModifiedTime should return correct time")
    void getLastModifiedTimeShouldReturnCorrectTime() {
      assertEquals(NOW, entry.getLastModifiedTime());
    }
  }

  @Nested
  @DisplayName("WASI File Type Tests")
  class WasiFileTypeTests {

    @Test
    @DisplayName("getWasiFileType should return 4 for regular file")
    void getWasiFileTypeShouldReturnFourForRegularFile() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("file.txt", true, false, false, 0L, NOW);
      assertEquals(4, entry.getWasiFileType(), "Regular file type should be 4");
    }

    @Test
    @DisplayName("getWasiFileType should return 3 for directory")
    void getWasiFileTypeShouldReturnThreeForDirectory() {
      final WasiDirectoryEntry entry = new WasiDirectoryEntry("dir", false, true, false, 0L, NOW);
      assertEquals(3, entry.getWasiFileType(), "Directory type should be 3");
    }

    @Test
    @DisplayName("getWasiFileType should return 7 for symbolic link")
    void getWasiFileTypeShouldReturnSevenForSymbolicLink() {
      final WasiDirectoryEntry entry = new WasiDirectoryEntry("link", false, false, true, 0L, NOW);
      assertEquals(7, entry.getWasiFileType(), "Symbolic link type should be 7");
    }

    @Test
    @DisplayName("getWasiFileType should return 0 for unknown")
    void getWasiFileTypeShouldReturnZeroForUnknown() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("unknown", false, false, false, 0L, NOW);
      assertEquals(0, entry.getWasiFileType(), "Unknown type should be 0");
    }
  }

  @Nested
  @DisplayName("Equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      assertEquals(entry, entry);
    }

    @Test
    @DisplayName("equals should return true for equal entries")
    void equalsShouldReturnTrueForEqualEntries() {
      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      assertEquals(entry1, entry2);
    }

    @Test
    @DisplayName("equals should return false for different names")
    void equalsShouldReturnFalseForDifferentNames() {
      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("file1.txt", true, false, false, 100L, NOW);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("file2.txt", true, false, false, 100L, NOW);
      assertNotEquals(entry1, entry2);
    }

    @Test
    @DisplayName("equals should return false for different file types")
    void equalsShouldReturnFalseForDifferentFileTypes() {
      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test", true, false, false, 100L, NOW);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test", false, true, false, 100L, NOW);
      assertNotEquals(entry1, entry2);
    }

    @Test
    @DisplayName("equals should return false for different sizes")
    void equalsShouldReturnFalseForDifferentSizes() {
      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test.txt", true, false, false, 200L, NOW);
      assertNotEquals(entry1, entry2);
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      assertNotEquals(null, entry);
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      final int hash1 = entry.hashCode();
      final int hash2 = entry.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("hashCode should be equal for equal entries")
    void hashCodeShouldBeEqualForEqualEntries() {
      final WasiDirectoryEntry entry1 =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      final WasiDirectoryEntry entry2 =
          new WasiDirectoryEntry("test.txt", true, false, false, 100L, NOW);
      assertEquals(entry1.hashCode(), entry2.hashCode());
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include name and FILE type")
    void toStringShouldIncludeNameAndFileType() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("test.txt", true, false, false, 1024L, NOW);
      final String str = entry.toString();

      assertTrue(str.contains("WasiDirectoryEntry"), "Should contain class name");
      assertTrue(str.contains("test.txt"), "Should contain file name");
      assertTrue(str.contains("FILE"), "Should contain FILE type");
      assertTrue(str.contains("1024"), "Should contain size");
    }

    @Test
    @DisplayName("toString should show DIR type for directories")
    void toStringShouldShowDirTypeForDirectories() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("subdir", false, true, false, 0L, NOW);
      assertTrue(entry.toString().contains("DIR"));
    }

    @Test
    @DisplayName("toString should show LINK type for symbolic links")
    void toStringShouldShowLinkTypeForSymbolicLinks() {
      final WasiDirectoryEntry entry = new WasiDirectoryEntry("link", false, false, true, 0L, NOW);
      assertTrue(entry.toString().contains("LINK"));
    }

    @Test
    @DisplayName("toString should show UNKNOWN type for unknown entries")
    void toStringShouldShowUnknownTypeForUnknownEntries() {
      final WasiDirectoryEntry entry =
          new WasiDirectoryEntry("unknown", false, false, false, 0L, NOW);
      assertTrue(entry.toString().contains("UNKNOWN"));
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("WasiDirectoryEntry should be final class")
    void wasiDirectoryEntryShouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiDirectoryEntry.class.getModifiers()),
          "WasiDirectoryEntry should be final");
    }
  }
}
