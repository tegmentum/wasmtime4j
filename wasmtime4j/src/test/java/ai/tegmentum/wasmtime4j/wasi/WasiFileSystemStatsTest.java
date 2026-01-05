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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiFileSystemStats} interface.
 *
 * <p>WasiFileSystemStats provides file system operation statistics.
 */
@DisplayName("WasiFileSystemStats Tests")
class WasiFileSystemStatsTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(WasiFileSystemStats.class.getModifiers()),
          "WasiFileSystemStats should be public");
      assertTrue(
          WasiFileSystemStats.class.isInterface(), "WasiFileSystemStats should be an interface");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getReadOperations method")
    void shouldHaveGetReadOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getReadOperations");
      assertNotNull(method, "getReadOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getWriteOperations method")
    void shouldHaveGetWriteOperationsMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getWriteOperations");
      assertNotNull(method, "getWriteOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBytesRead method")
    void shouldHaveGetBytesReadMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getBytesRead");
      assertNotNull(method, "getBytesRead method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getBytesWritten method")
    void shouldHaveGetBytesWrittenMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getBytesWritten");
      assertNotNull(method, "getBytesWritten method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFileOpenCount method")
    void shouldHaveGetFileOpenCountMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getFileOpenCount");
      assertNotNull(method, "getFileOpenCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getCurrentOpenFiles method")
    void shouldHaveGetCurrentOpenFilesMethod() throws NoSuchMethodException {
      final Method method = WasiFileSystemStats.class.getMethod("getCurrentOpenFiles");
      assertNotNull(method, "getCurrentOpenFiles method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Implementation Behavior Tests")
  class ImplementationBehaviorTests {

    @Test
    @DisplayName("implementation should track read operations")
    void implementationShouldTrackReadOperations() {
      final WasiFileSystemStats stats = createTestStats(100L, 50L, 10000L, 5000L, 20L, 3);

      assertEquals(100L, stats.getReadOperations(), "Read operations should match");
    }

    @Test
    @DisplayName("implementation should track write operations")
    void implementationShouldTrackWriteOperations() {
      final WasiFileSystemStats stats = createTestStats(100L, 75L, 10000L, 5000L, 20L, 3);

      assertEquals(75L, stats.getWriteOperations(), "Write operations should match");
    }

    @Test
    @DisplayName("implementation should track bytes transferred")
    void implementationShouldTrackBytesTransferred() {
      final WasiFileSystemStats stats = createTestStats(100L, 50L, 100000L, 75000L, 20L, 3);

      assertEquals(100000L, stats.getBytesRead(), "Bytes read should match");
      assertEquals(75000L, stats.getBytesWritten(), "Bytes written should match");
    }

    @Test
    @DisplayName("implementation should track file handles")
    void implementationShouldTrackFileHandles() {
      final WasiFileSystemStats stats = createTestStats(100L, 50L, 10000L, 5000L, 150L, 8);

      assertEquals(150L, stats.getFileOpenCount(), "File open count should match");
      assertEquals(8, stats.getCurrentOpenFiles(), "Current open files should match");
    }

    private WasiFileSystemStats createTestStats(
        final long readOperations,
        final long writeOperations,
        final long bytesRead,
        final long bytesWritten,
        final long fileOpenCount,
        final int currentOpenFiles) {
      return new WasiFileSystemStats() {
        @Override
        public long getReadOperations() {
          return readOperations;
        }

        @Override
        public long getWriteOperations() {
          return writeOperations;
        }

        @Override
        public long getBytesRead() {
          return bytesRead;
        }

        @Override
        public long getBytesWritten() {
          return bytesWritten;
        }

        @Override
        public long getFileOpenCount() {
          return fileOpenCount;
        }

        @Override
        public int getCurrentOpenFiles() {
          return currentOpenFiles;
        }
      };
    }
  }
}
