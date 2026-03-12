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
package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link NativeLibraryLoader}.
 *
 * <p>These tests verify the singleton pattern, loaded state, and public accessor methods. The
 * existing {@link NativeLibraryDiagnosticTest} covers symbol lookup and method handle creation, so
 * those are not duplicated here.
 */
@DisplayName("NativeLibraryLoader Tests")
final class NativeLibraryLoaderTest {

  private static final Logger LOGGER = Logger.getLogger(NativeLibraryLoaderTest.class.getName());

  @Nested
  @DisplayName("Singleton Pattern Tests")
  class SingletonPatternTests {

    @Test
    @DisplayName("Should return non-null instance from getInstance")
    void shouldReturnNonNullInstance() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertNotNull(loader, "getInstance() should return non-null instance");
      LOGGER.info("getInstance() returned non-null instance");
    }

    @Test
    @DisplayName("Should return same instance on repeated calls")
    void shouldReturnSameInstanceOnRepeatedCalls() {
      NativeLibraryLoader first = NativeLibraryLoader.getInstance();
      NativeLibraryLoader second = NativeLibraryLoader.getInstance();
      assertSame(first, second, "getInstance() should return the same singleton instance");
      LOGGER.info("getInstance() returned same instance on repeated calls");
    }
  }

  @Nested
  @DisplayName("Loaded State Tests")
  class LoadedStateTests {

    @Test
    @DisplayName("Should report loaded after successful initialization")
    void shouldReportLoadedAfterInit() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "isLoaded() should return true after successful init");
      LOGGER.info("isLoaded() = " + loader.isLoaded());
    }

    @Test
    @DisplayName("Should have non-null load info")
    void shouldHaveNonNullLoadInfo() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertNotNull(loader.getLoadInfo(), "getLoadInfo() should not be null");
      assertTrue(loader.getLoadInfo().isSuccessful(), "Load info should report successful load");
      LOGGER.info("Load info: " + loader.getLoadInfo());
    }

    @Test
    @DisplayName("Should return empty or absent loading error on success")
    void shouldReturnEmptyLoadingErrorOnSuccess() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      Optional<String> error = loader.getLoadingError();
      assertNotNull(error, "getLoadingError() should not return null Optional");
      // On a successful load, the error message should be absent or empty
      assertTrue(
          error.isEmpty() || error.get().isEmpty(),
          "Loading error should be absent on successful load, got: " + error.orElse("(empty)"));
      LOGGER.info("Loading error on success: " + error.orElse("(empty)"));
    }
  }

  @Nested
  @DisplayName("Symbol Lookup Accessor Tests")
  class SymbolLookupAccessorTests {

    @Test
    @DisplayName("Should return non-null symbol lookup")
    void shouldReturnNonNullSymbolLookup() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertNotNull(
          loader.getSymbolLookup(),
          "getSymbolLookup() should return non-null after successful load");
      LOGGER.info("getSymbolLookup() returned non-null");
    }

    @Test
    @DisplayName("Should return non-null library arena")
    void shouldReturnNonNullLibraryArena() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertNotNull(loader.getLibraryArena(), "getLibraryArena() should return non-null");
      assertTrue(loader.getLibraryArena().scope().isAlive(), "Library arena should be alive");
      LOGGER.info("Library arena is alive");
    }
  }

  @Nested
  @DisplayName("Method Handle Cache Tests")
  class MethodHandleCacheTests {

    @Test
    @DisplayName("Should clear method handle cache without error")
    void shouldClearMethodHandleCacheWithoutError() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertDoesNotThrow(
          loader::clearMethodHandleCache, "clearMethodHandleCache() should not throw");
      LOGGER.info("clearMethodHandleCache() completed without error");
    }

    @Test
    @DisplayName("Should handle repeated cache clears")
    void shouldHandleRepeatedCacheClears() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertDoesNotThrow(loader::clearMethodHandleCache, "First clear should not throw");
      assertDoesNotThrow(loader::clearMethodHandleCache, "Second clear should not throw");
      LOGGER.info("Repeated cache clears handled without error");
    }
  }

  @Nested
  @DisplayName("Diagnostic Info Tests")
  class DiagnosticInfoTests {

    @Test
    @DisplayName("Should return non-empty diagnostic info")
    void shouldReturnNonEmptyDiagnosticInfo() {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      String info = loader.getDiagnosticInfo();
      assertNotNull(info, "getDiagnosticInfo() should not return null");
      assertTrue(!info.isEmpty(), "getDiagnosticInfo() should not return empty string");
      assertTrue(info.contains("Loaded"), "Diagnostic info should contain load status");
      LOGGER.info("Diagnostic info length: " + info.length());
    }
  }
}
