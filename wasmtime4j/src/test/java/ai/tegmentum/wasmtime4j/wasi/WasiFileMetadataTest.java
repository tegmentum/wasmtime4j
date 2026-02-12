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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiFileMetadata}. */
@DisplayName("WasiFileMetadata Tests")
class WasiFileMetadataTest {

  private static final long TEST_SIZE = 1024L;
  private static final FileTime TEST_MODIFIED_TIME = FileTime.from(Instant.now());
  private static final FileTime TEST_ACCESS_TIME = FileTime.from(Instant.now().minusSeconds(60));
  private static final FileTime TEST_CREATION_TIME =
      FileTime.from(Instant.now().minusSeconds(3600));

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiFileMetadata should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiFileMetadata.class.getModifiers()),
          "WasiFileMetadata should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should create metadata with all fields")
    void constructorShouldCreateMetadataWithAllFields() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              TEST_SIZE,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              true,
              true,
              false);

      assertNotNull(metadata, "Metadata should not be null");
      assertEquals(TEST_SIZE, metadata.getSize(), "Size should match");
      assertEquals(
          TEST_MODIFIED_TIME, metadata.getLastModifiedTime(), "Modified time should match");
      assertEquals(TEST_ACCESS_TIME, metadata.getLastAccessTime(), "Access time should match");
      assertEquals(TEST_CREATION_TIME, metadata.getCreationTime(), "Creation time should match");
      assertTrue(metadata.isRegularFile(), "Should be regular file");
      assertFalse(metadata.isDirectory(), "Should not be directory");
      assertFalse(metadata.isSymbolicLink(), "Should not be symbolic link");
      assertTrue(metadata.isReadable(), "Should be readable");
      assertTrue(metadata.isWritable(), "Should be writable");
      assertFalse(metadata.isExecutable(), "Should not be executable");
    }

    @Test
    @DisplayName("Constructor should create directory metadata")
    void constructorShouldCreateDirectoryMetadata() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              false,
              true,
              false,
              true,
              true,
              true);

      assertFalse(metadata.isRegularFile(), "Should not be regular file");
      assertTrue(metadata.isDirectory(), "Should be directory");
      assertFalse(metadata.isSymbolicLink(), "Should not be symbolic link");
    }

    @Test
    @DisplayName("Constructor should create symbolic link metadata")
    void constructorShouldCreateSymbolicLinkMetadata() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              false,
              false,
              true,
              true,
              false,
              false);

      assertFalse(metadata.isRegularFile(), "Should not be regular file");
      assertFalse(metadata.isDirectory(), "Should not be directory");
      assertTrue(metadata.isSymbolicLink(), "Should be symbolic link");
    }
  }

  @Nested
  @DisplayName("getSize Tests")
  class GetSizeTests {

    @Test
    @DisplayName("Should return correct size")
    void shouldReturnCorrectSize() {
      final WasiFileMetadata metadata = createTestMetadata(5000L);

      assertEquals(5000L, metadata.getSize(), "Size should be 5000");
    }

    @Test
    @DisplayName("Should handle zero size")
    void shouldHandleZeroSize() {
      final WasiFileMetadata metadata = createTestMetadata(0L);

      assertEquals(0L, metadata.getSize(), "Size should be 0");
    }

    @Test
    @DisplayName("Should handle large size")
    void shouldHandleLargeSize() {
      final WasiFileMetadata metadata = createTestMetadata(Long.MAX_VALUE);

      assertEquals(Long.MAX_VALUE, metadata.getSize(), "Size should be Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Time Tests")
  class TimeTests {

    @Test
    @DisplayName("Should return last modified time")
    void shouldReturnLastModifiedTime() {
      final WasiFileMetadata metadata = createTestMetadata(0L);

      assertEquals(
          TEST_MODIFIED_TIME, metadata.getLastModifiedTime(), "Last modified time should match");
    }

    @Test
    @DisplayName("Should return last access time")
    void shouldReturnLastAccessTime() {
      final WasiFileMetadata metadata = createTestMetadata(0L);

      assertEquals(TEST_ACCESS_TIME, metadata.getLastAccessTime(), "Last access time should match");
    }

    @Test
    @DisplayName("Should return creation time")
    void shouldReturnCreationTime() {
      final WasiFileMetadata metadata = createTestMetadata(0L);

      assertEquals(TEST_CREATION_TIME, metadata.getCreationTime(), "Creation time should match");
    }
  }

  @Nested
  @DisplayName("Time Seconds Tests")
  class TimeSecondsTests {

    @Test
    @DisplayName("Should return last modified time in seconds")
    void shouldReturnLastModifiedTimeInSeconds() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(
          instant.getEpochSecond(), metadata.getLastModifiedTimeSeconds(), "Seconds should match");
    }

    @Test
    @DisplayName("Should return last access time in seconds")
    void shouldReturnLastAccessTimeInSeconds() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(
          instant.getEpochSecond(), metadata.getLastAccessTimeSeconds(), "Seconds should match");
    }

    @Test
    @DisplayName("Should return creation time in seconds")
    void shouldReturnCreationTimeInSeconds() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(
          instant.getEpochSecond(), metadata.getCreationTimeSeconds(), "Seconds should match");
    }
  }

  @Nested
  @DisplayName("Time Nanos Tests")
  class TimeNanosTests {

    @Test
    @DisplayName("Should return last modified time nanos")
    void shouldReturnLastModifiedTimeNanos() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00.123456789Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(123456789, metadata.getLastModifiedTimeNanos(), "Nanos should match");
    }

    @Test
    @DisplayName("Should return last access time nanos")
    void shouldReturnLastAccessTimeNanos() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00.987654321Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(987654321, metadata.getLastAccessTimeNanos(), "Nanos should match");
    }

    @Test
    @DisplayName("Should return creation time nanos")
    void shouldReturnCreationTimeNanos() {
      final Instant instant = Instant.parse("2025-01-15T10:30:00.555555555Z");
      final FileTime time = FileTime.from(instant);
      final WasiFileMetadata metadata =
          new WasiFileMetadata(0L, time, time, time, true, false, false, true, true, false);

      assertEquals(555555555, metadata.getCreationTimeNanos(), "Nanos should match");
    }
  }

  @Nested
  @DisplayName("File Type Tests")
  class FileTypeTests {

    @Test
    @DisplayName("Regular file should be identifiable")
    void regularFileShouldBeIdentifiable() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1000L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              true,
              true,
              true);

      assertTrue(metadata.isRegularFile(), "Should be regular file");
      assertFalse(metadata.isDirectory(), "Should not be directory");
      assertFalse(metadata.isSymbolicLink(), "Should not be symbolic link");
    }

    @Test
    @DisplayName("Directory should be identifiable")
    void directoryShouldBeIdentifiable() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              false,
              true,
              false,
              true,
              true,
              true);

      assertFalse(metadata.isRegularFile(), "Should not be regular file");
      assertTrue(metadata.isDirectory(), "Should be directory");
      assertFalse(metadata.isSymbolicLink(), "Should not be symbolic link");
    }

    @Test
    @DisplayName("Symbolic link should be identifiable")
    void symbolicLinkShouldBeIdentifiable() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              0L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              false,
              false,
              true,
              true,
              false,
              false);

      assertFalse(metadata.isRegularFile(), "Should not be regular file");
      assertFalse(metadata.isDirectory(), "Should not be directory");
      assertTrue(metadata.isSymbolicLink(), "Should be symbolic link");
    }
  }

  @Nested
  @DisplayName("Permission Tests")
  class PermissionTests {

    @Test
    @DisplayName("Should detect readable file")
    void shouldDetectReadableFile() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1000L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              true,
              false,
              false);

      assertTrue(metadata.isReadable(), "Should be readable");
      assertFalse(metadata.isWritable(), "Should not be writable");
      assertFalse(metadata.isExecutable(), "Should not be executable");
    }

    @Test
    @DisplayName("Should detect writable file")
    void shouldDetectWritableFile() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1000L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              false,
              true,
              false);

      assertFalse(metadata.isReadable(), "Should not be readable");
      assertTrue(metadata.isWritable(), "Should be writable");
      assertFalse(metadata.isExecutable(), "Should not be executable");
    }

    @Test
    @DisplayName("Should detect executable file")
    void shouldDetectExecutableFile() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1000L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              false,
              false,
              true);

      assertFalse(metadata.isReadable(), "Should not be readable");
      assertFalse(metadata.isWritable(), "Should not be writable");
      assertTrue(metadata.isExecutable(), "Should be executable");
    }

    @Test
    @DisplayName("Should detect all permissions")
    void shouldDetectAllPermissions() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1000L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              true,
              true,
              true);

      assertTrue(metadata.isReadable(), "Should be readable");
      assertTrue(metadata.isWritable(), "Should be writable");
      assertTrue(metadata.isExecutable(), "Should be executable");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain all fields")
    void toStringShouldContainAllFields() {
      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              1024L,
              TEST_MODIFIED_TIME,
              TEST_ACCESS_TIME,
              TEST_CREATION_TIME,
              true,
              false,
              false,
              true,
              true,
              false);

      final String str = metadata.toString();

      assertTrue(str.contains("size=1024"), "Should contain size");
      assertTrue(str.contains("regularFile=true"), "Should contain regularFile");
      assertTrue(str.contains("directory=false"), "Should contain directory");
      assertTrue(str.contains("symbolicLink=false"), "Should contain symbolicLink");
      assertTrue(str.contains("readable=true"), "Should contain readable");
      assertTrue(str.contains("writable=true"), "Should contain writable");
      assertTrue(str.contains("executable=false"), "Should contain executable");
    }
  }

  /** Helper to create test metadata. */
  private WasiFileMetadata createTestMetadata(final long size) {
    return new WasiFileMetadata(
        size,
        TEST_MODIFIED_TIME,
        TEST_ACCESS_TIME,
        TEST_CREATION_TIME,
        true,
        false,
        false,
        true,
        true,
        false);
  }
}
