/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI Filesystem package classes.
 *
 * <p>This test class validates the filesystem enums and flags.
 */
@DisplayName("WASI Filesystem Integration Tests")
public class WasiFilesystemIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WasiFilesystemIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI Filesystem Integration Tests");
  }

  @Nested
  @DisplayName("DescriptorType Tests")
  class DescriptorTypeTests {

    @Test
    @DisplayName("Should have all expected descriptor types")
    void shouldHaveAllExpectedDescriptorTypes() {
      LOGGER.info("Testing DescriptorType enum values");

      DescriptorType[] types = DescriptorType.values();
      assertEquals(8, types.length, "Should have 8 descriptor types");

      assertNotNull(DescriptorType.UNKNOWN, "UNKNOWN should exist");
      assertNotNull(DescriptorType.BLOCK_DEVICE, "BLOCK_DEVICE should exist");
      assertNotNull(DescriptorType.CHARACTER_DEVICE, "CHARACTER_DEVICE should exist");
      assertNotNull(DescriptorType.DIRECTORY, "DIRECTORY should exist");
      assertNotNull(DescriptorType.FIFO, "FIFO should exist");
      assertNotNull(DescriptorType.SYMBOLIC_LINK, "SYMBOLIC_LINK should exist");
      assertNotNull(DescriptorType.REGULAR_FILE, "REGULAR_FILE should exist");
      assertNotNull(DescriptorType.SOCKET, "SOCKET should exist");

      LOGGER.info("DescriptorType enum verified: " + types.length + " types");
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing DescriptorType ordinal values");

      assertEquals(0, DescriptorType.UNKNOWN.ordinal());
      assertEquals(3, DescriptorType.DIRECTORY.ordinal());
      assertEquals(6, DescriptorType.REGULAR_FILE.ordinal());

      LOGGER.info("Ordinal values verified");
    }

    @Test
    @DisplayName("Should support valueOf")
    void shouldSupportValueOf() {
      LOGGER.info("Testing DescriptorType valueOf");

      assertEquals(DescriptorType.DIRECTORY, DescriptorType.valueOf("DIRECTORY"));
      assertEquals(DescriptorType.REGULAR_FILE, DescriptorType.valueOf("REGULAR_FILE"));

      LOGGER.info("valueOf verified");
    }
  }

  @Nested
  @DisplayName("DescriptorFlags Tests")
  class DescriptorFlagsTests {

    @Test
    @DisplayName("Should have all expected descriptor flags")
    void shouldHaveAllExpectedDescriptorFlags() {
      LOGGER.info("Testing DescriptorFlags enum values");

      DescriptorFlags[] flags = DescriptorFlags.values();
      assertEquals(6, flags.length, "Should have 6 descriptor flags");

      assertNotNull(DescriptorFlags.READ, "READ should exist");
      assertNotNull(DescriptorFlags.WRITE, "WRITE should exist");
      assertNotNull(DescriptorFlags.FILE_INTEGRITY_SYNC, "FILE_INTEGRITY_SYNC should exist");
      assertNotNull(DescriptorFlags.DATA_INTEGRITY_SYNC, "DATA_INTEGRITY_SYNC should exist");
      assertNotNull(DescriptorFlags.REQUESTED_WRITE_SYNC, "REQUESTED_WRITE_SYNC should exist");
      assertNotNull(DescriptorFlags.MUTATE_DIRECTORY, "MUTATE_DIRECTORY should exist");

      LOGGER.info("DescriptorFlags enum verified");
    }

    @Test
    @DisplayName("Should create set with of() method")
    void shouldCreateSetWithOfMethod() {
      LOGGER.info("Testing DescriptorFlags.of()");

      Set<DescriptorFlags> readOnly = DescriptorFlags.of(DescriptorFlags.READ);
      assertEquals(1, readOnly.size());
      assertTrue(readOnly.contains(DescriptorFlags.READ));

      Set<DescriptorFlags> readWrite =
          DescriptorFlags.of(DescriptorFlags.READ, DescriptorFlags.WRITE);
      assertEquals(2, readWrite.size());
      assertTrue(readWrite.contains(DescriptorFlags.READ));
      assertTrue(readWrite.contains(DescriptorFlags.WRITE));

      LOGGER.info("of() method verified");
    }

    @Test
    @DisplayName("Should create empty set with none()")
    void shouldCreateEmptySetWithNone() {
      LOGGER.info("Testing DescriptorFlags.none()");

      Set<DescriptorFlags> empty = DescriptorFlags.none();

      assertNotNull(empty);
      assertTrue(empty.isEmpty());

      LOGGER.info("none() method verified");
    }

    @Test
    @DisplayName("Should create full set with all()")
    void shouldCreateFullSetWithAll() {
      LOGGER.info("Testing DescriptorFlags.all()");

      Set<DescriptorFlags> all = DescriptorFlags.all();

      assertNotNull(all);
      assertEquals(6, all.size());
      assertTrue(all.contains(DescriptorFlags.READ));
      assertTrue(all.contains(DescriptorFlags.WRITE));
      assertTrue(all.contains(DescriptorFlags.FILE_INTEGRITY_SYNC));
      assertTrue(all.contains(DescriptorFlags.DATA_INTEGRITY_SYNC));
      assertTrue(all.contains(DescriptorFlags.REQUESTED_WRITE_SYNC));
      assertTrue(all.contains(DescriptorFlags.MUTATE_DIRECTORY));

      LOGGER.info("all() method verified");
    }
  }

  @Nested
  @DisplayName("OpenFlags Tests")
  class OpenFlagsTests {

    @Test
    @DisplayName("Should have all expected open flags")
    void shouldHaveAllExpectedOpenFlags() {
      LOGGER.info("Testing OpenFlags enum values");

      OpenFlags[] flags = OpenFlags.values();
      assertEquals(4, flags.length, "Should have 4 open flags");

      assertNotNull(OpenFlags.CREATE, "CREATE should exist");
      assertNotNull(OpenFlags.DIRECTORY, "DIRECTORY should exist");
      assertNotNull(OpenFlags.EXCLUSIVE, "EXCLUSIVE should exist");
      assertNotNull(OpenFlags.TRUNCATE, "TRUNCATE should exist");

      LOGGER.info("OpenFlags enum verified");
    }

    @Test
    @DisplayName("Should create set with of() method")
    void shouldCreateSetWithOfMethod() {
      LOGGER.info("Testing OpenFlags.of()");

      Set<OpenFlags> createOnly = OpenFlags.of(OpenFlags.CREATE);
      assertEquals(1, createOnly.size());
      assertTrue(createOnly.contains(OpenFlags.CREATE));

      Set<OpenFlags> createExclusive = OpenFlags.of(OpenFlags.CREATE, OpenFlags.EXCLUSIVE);
      assertEquals(2, createExclusive.size());

      // Empty varargs should return empty set
      Set<OpenFlags> empty = OpenFlags.of();
      assertTrue(empty.isEmpty());

      LOGGER.info("of() method verified");
    }

    @Test
    @DisplayName("Should create empty set with none()")
    void shouldCreateEmptySetWithNone() {
      LOGGER.info("Testing OpenFlags.none()");

      Set<OpenFlags> empty = OpenFlags.none();

      assertNotNull(empty);
      assertTrue(empty.isEmpty());

      LOGGER.info("none() method verified");
    }

    @Test
    @DisplayName("Should create full set with all()")
    void shouldCreateFullSetWithAll() {
      LOGGER.info("Testing OpenFlags.all()");

      Set<OpenFlags> all = OpenFlags.all();

      assertNotNull(all);
      assertEquals(4, all.size());
      assertTrue(all.contains(OpenFlags.CREATE));
      assertTrue(all.contains(OpenFlags.DIRECTORY));
      assertTrue(all.contains(OpenFlags.EXCLUSIVE));
      assertTrue(all.contains(OpenFlags.TRUNCATE));

      LOGGER.info("all() method verified");
    }

    @Test
    @DisplayName("Should combine flags correctly")
    void shouldCombineFlagsCorrectly() {
      LOGGER.info("Testing OpenFlags combination");

      // Typical create new file flags
      Set<OpenFlags> createNew = OpenFlags.of(OpenFlags.CREATE, OpenFlags.EXCLUSIVE);
      assertTrue(createNew.contains(OpenFlags.CREATE));
      assertTrue(createNew.contains(OpenFlags.EXCLUSIVE));
      assertFalse(createNew.contains(OpenFlags.TRUNCATE));

      // Typical overwrite file flags
      Set<OpenFlags> overwrite = OpenFlags.of(OpenFlags.CREATE, OpenFlags.TRUNCATE);
      assertTrue(overwrite.contains(OpenFlags.CREATE));
      assertTrue(overwrite.contains(OpenFlags.TRUNCATE));
      assertFalse(overwrite.contains(OpenFlags.EXCLUSIVE));

      LOGGER.info("Flag combination verified");
    }
  }

  @Nested
  @DisplayName("PathFlags Tests")
  class PathFlagsTests {

    @Test
    @DisplayName("Should have all expected path flags")
    void shouldHaveAllExpectedPathFlags() {
      LOGGER.info("Testing PathFlags enum values");

      PathFlags[] flags = PathFlags.values();
      assertEquals(1, flags.length, "Should have 1 path flag");

      assertNotNull(PathFlags.SYMLINK_FOLLOW, "SYMLINK_FOLLOW should exist");

      LOGGER.info("PathFlags enum verified");
    }

    @Test
    @DisplayName("Should create set with of() method")
    void shouldCreateSetWithOfMethod() {
      LOGGER.info("Testing PathFlags.of()");

      Set<PathFlags> follow = PathFlags.of(PathFlags.SYMLINK_FOLLOW);
      assertEquals(1, follow.size());
      assertTrue(follow.contains(PathFlags.SYMLINK_FOLLOW));

      // Empty varargs should return empty set
      Set<PathFlags> empty = PathFlags.of();
      assertTrue(empty.isEmpty());

      LOGGER.info("of() method verified");
    }

    @Test
    @DisplayName("Should create empty set with none()")
    void shouldCreateEmptySetWithNone() {
      LOGGER.info("Testing PathFlags.none()");

      Set<PathFlags> empty = PathFlags.none();

      assertNotNull(empty);
      assertTrue(empty.isEmpty());

      LOGGER.info("none() method verified");
    }

    @Test
    @DisplayName("Should create full set with all()")
    void shouldCreateFullSetWithAll() {
      LOGGER.info("Testing PathFlags.all()");

      Set<PathFlags> all = PathFlags.all();

      assertNotNull(all);
      assertEquals(1, all.size());
      assertTrue(all.contains(PathFlags.SYMLINK_FOLLOW));

      LOGGER.info("all() method verified");
    }
  }
}
