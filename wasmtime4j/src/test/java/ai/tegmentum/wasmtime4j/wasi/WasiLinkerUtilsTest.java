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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link WasiLinkerUtils} constants and null-argument validation. */
@DisplayName("WasiLinkerUtils Tests")
class WasiLinkerUtilsTest {

  @Nested
  @DisplayName("WASI P1 Module Constant")
  class WasiP1ModuleConstant {

    @Test
    @DisplayName("should define wasi_snapshot_preview1 module name")
    void shouldDefineWasiP1ModuleName() {
      assertEquals("wasi_snapshot_preview1", WasiLinkerUtils.WASI_P1_MODULE);
    }
  }

  @Nested
  @DisplayName("WASI P1 Import Constants")
  class WasiP1ImportConstants {

    @Test
    @DisplayName("should have P1 imports array")
    void shouldHaveP1ImportsArray() {
      assertNotNull(WasiLinkerUtils.WASI_P1_IMPORTS);
      assertTrue(WasiLinkerUtils.WASI_P1_IMPORTS.length > 0);
    }

    @Test
    @DisplayName("each P1 import should have module and field name")
    void eachP1ImportShouldHaveModuleAndFieldName() {
      for (String[] entry : WasiLinkerUtils.WASI_P1_IMPORTS) {
        assertNotNull(entry);
        assertEquals(2, entry.length, "Each P1 import entry should have 2 elements");
        assertNotNull(entry[0], "Module name should not be null");
        assertNotNull(entry[1], "Field name should not be null");
      }
    }

    @Test
    @DisplayName("all P1 imports should use wasi_snapshot_preview1 module")
    void allP1ImportsShouldUseCorrectModule() {
      for (String[] entry : WasiLinkerUtils.WASI_P1_IMPORTS) {
        assertEquals(WasiLinkerUtils.WASI_P1_MODULE, entry[0]);
      }
    }

    @Test
    @DisplayName("P1 imports should include fd_write")
    void p1ImportsShouldIncludeFdWrite() {
      boolean found = false;
      for (String[] entry : WasiLinkerUtils.WASI_P1_IMPORTS) {
        if ("fd_write".equals(entry[1])) {
          found = true;
          break;
        }
      }
      assertTrue(found, "P1 imports should include fd_write");
    }

    @Test
    @DisplayName("P1 imports should include proc_exit")
    void p1ImportsShouldIncludeProcExit() {
      boolean found = false;
      for (String[] entry : WasiLinkerUtils.WASI_P1_IMPORTS) {
        if ("proc_exit".equals(entry[1])) {
          found = true;
          break;
        }
      }
      assertTrue(found, "P1 imports should include proc_exit");
    }
  }

  @Nested
  @DisplayName("WASI P2 Import Constants")
  class WasiP2ImportConstants {

    @Test
    @DisplayName("should have P2 imports array")
    void shouldHaveP2ImportsArray() {
      assertNotNull(WasiLinkerUtils.WASI_P2_IMPORTS);
      assertTrue(WasiLinkerUtils.WASI_P2_IMPORTS.length > 0);
    }

    @Test
    @DisplayName("each P2 import should have module and field name")
    void eachP2ImportShouldHaveModuleAndFieldName() {
      for (String[] entry : WasiLinkerUtils.WASI_P2_IMPORTS) {
        assertNotNull(entry);
        assertEquals(2, entry.length, "Each P2 import entry should have 2 elements");
        assertNotNull(entry[0], "Module name should not be null");
        assertNotNull(entry[1], "Field name should not be null");
      }
    }
  }

  @Nested
  @DisplayName("Null Argument Validation")
  class NullArgumentValidation {

    @Test
    @DisplayName("addToLinker should throw for null linker")
    void addToLinkerShouldThrowForNullLinker() {
      assertThrows(IllegalArgumentException.class, () -> WasiLinkerUtils.addToLinker(null, null));
    }

    @Test
    @DisplayName("addToLinkerAsync should throw for null linker")
    void addToLinkerAsyncShouldThrowForNullLinker() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.addToLinkerAsync(null, null));
    }

    @Test
    @DisplayName("addPreview2ToLinker should throw for null linker")
    void addPreview2ToLinkerShouldThrowForNullLinker() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.addPreview2ToLinker(null, null));
    }

    @Test
    @DisplayName("addComponentModelToLinker should throw for null linker")
    void addComponentModelToLinkerShouldThrowForNullLinker() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.addComponentModelToLinker(null));
    }

    @Test
    @DisplayName("hasWasiImports should throw for null linker")
    void hasWasiImportsShouldThrowForNullLinker() {
      assertThrows(IllegalArgumentException.class, () -> WasiLinkerUtils.hasWasiImports(null));
    }

    @Test
    @DisplayName("hasWasiPreview2Imports should throw for null linker")
    void hasWasiPreview2ImportsShouldThrowForNullLinker() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.hasWasiPreview2Imports(null));
    }

    @Test
    @DisplayName("createLinker should throw for null engine")
    void createLinkerShouldThrowForNullEngine() {
      assertThrows(IllegalArgumentException.class, () -> WasiLinkerUtils.createLinker(null, null));
    }

    @Test
    @DisplayName("createPreview2Linker should throw for null engine")
    void createPreview2LinkerShouldThrowForNullEngine() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.createPreview2Linker(null, null));
    }

    @Test
    @DisplayName("createFullLinker should throw for null engine")
    void createFullLinkerShouldThrowForNullEngine() {
      assertThrows(
          IllegalArgumentException.class, () -> WasiLinkerUtils.createFullLinker(null, null));
    }
  }

  @Nested
  @DisplayName("Runtime Component Model Support")
  class RuntimeComponentModelSupport {

    @Test
    @DisplayName("runtimeSupportsComponentModel should not throw")
    void runtimeSupportsComponentModelShouldNotThrow() {
      // In forbid mode, this should return false without throwing
      WasiLinkerUtils.runtimeSupportsComponentModel();
    }
  }
}
