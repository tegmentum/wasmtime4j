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
package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ModuleExport} interface.
 *
 * <p>Verifies the interface contract using a stub implementation and tests creation and accessor
 * behavior.
 */
@DisplayName("ModuleExport Tests")
class ModuleExportTest {

  /** Simple implementation of ModuleExport for testing. */
  private static final class StubModuleExport implements ModuleExport {

    private final String name;
    private final long nativeHandle;

    StubModuleExport(final String name, final long nativeHandle) {
      this.name = name;
      this.nativeHandle = nativeHandle;
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public long nativeHandle() {
      return nativeHandle;
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ModuleExport.class.isInterface(), "ModuleExport should be an interface");
    }

    @Test
    @DisplayName("should declare name method")
    void shouldDeclareNameMethod() throws NoSuchMethodException {
      assertNotNull(
          ModuleExport.class.getMethod("name"), "ModuleExport should declare a name() method");
    }

    @Test
    @DisplayName("should declare nativeHandle method")
    void shouldDeclareNativeHandleMethod() throws NoSuchMethodException {
      assertNotNull(
          ModuleExport.class.getMethod("nativeHandle"),
          "ModuleExport should declare a nativeHandle() method");
    }
  }

  @Nested
  @DisplayName("Accessor Tests")
  class AccessorTests {

    @Test
    @DisplayName("name should return the export name")
    void nameShouldReturnExportName() {
      final ModuleExport export = new StubModuleExport("memory", 42L);

      assertEquals("memory", export.name(), "name() should return the export name");
    }

    @Test
    @DisplayName("nativeHandle should return the handle value")
    void nativeHandleShouldReturnHandleValue() {
      final ModuleExport export = new StubModuleExport("func", 12345L);

      assertEquals(12345L, export.nativeHandle(), "nativeHandle() should return the handle value");
    }

    @Test
    @DisplayName("should support null name")
    void shouldSupportNullName() {
      final ModuleExport export = new StubModuleExport(null, 0L);

      assertNull(export.name(), "name() should return null when set to null");
    }

    @Test
    @DisplayName("should support zero native handle")
    void shouldSupportZeroNativeHandle() {
      final ModuleExport export = new StubModuleExport("test", 0L);

      assertEquals(0L, export.nativeHandle(), "nativeHandle() should return 0");
    }

    @Test
    @DisplayName("should support negative native handle")
    void shouldSupportNegativeNativeHandle() {
      final ModuleExport export = new StubModuleExport("test", -1L);

      assertEquals(-1L, export.nativeHandle(), "nativeHandle() should return -1");
    }

    @Test
    @DisplayName("should support max long native handle")
    void shouldSupportMaxLongNativeHandle() {
      final ModuleExport export = new StubModuleExport("test", Long.MAX_VALUE);

      assertEquals(
          Long.MAX_VALUE, export.nativeHandle(), "nativeHandle() should return Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Multiple Export Tests")
  class MultipleExportTests {

    @Test
    @DisplayName("different exports should have independent values")
    void differentExportsShouldHaveIndependentValues() {
      final ModuleExport memory = new StubModuleExport("memory", 1L);
      final ModuleExport func = new StubModuleExport("_start", 2L);
      final ModuleExport table = new StubModuleExport("__indirect_function_table", 3L);

      assertEquals("memory", memory.name(), "memory export name should match");
      assertEquals(1L, memory.nativeHandle(), "memory handle should match");

      assertEquals("_start", func.name(), "func export name should match");
      assertEquals(2L, func.nativeHandle(), "func handle should match");

      assertEquals("__indirect_function_table", table.name(), "table export name should match");
      assertEquals(3L, table.nativeHandle(), "table handle should match");
    }
  }
}
