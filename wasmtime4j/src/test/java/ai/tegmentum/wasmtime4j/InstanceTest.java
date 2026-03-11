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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Instance} default methods.
 *
 * <p>These tests use anonymous implementations to exercise default method behavior without
 * requiring a native runtime.
 */
@DisplayName("Instance Default Method Tests")
class InstanceTest {

  /** Creates a stub instance with no exports. */
  private Instance createEmptyInstance() {
    return new Instance() {
      @Override
      public Optional<WasmFunction> getFunction(String name) {
        return Optional.empty();
      }

      @Override
      public Optional<WasmGlobal> getGlobal(String name) {
        return Optional.empty();
      }

      @Override
      public Optional<WasmMemory> getMemory(String name) {
        return Optional.empty();
      }

      @Override
      public Optional<WasmTable> getTable(String name) {
        return Optional.empty();
      }

      @Override
      public Optional<WasmMemory> getSharedMemory(String name) {
        return Optional.empty();
      }

      @Override
      public String[] getExportNames() {
        return new String[0];
      }

      @Override
      public boolean hasExport(String name) {
        return false;
      }

      @Override
      public Optional<Extern> getExport(String name) {
        return Optional.empty();
      }

      @Override
      public Optional<Extern> getExport(Store store, ModuleExport moduleExport) {
        return Optional.empty();
      }

      @Override
      public Module getModule() {
        return null;
      }

      @Override
      public Store getStore() {
        return null;
      }

      @Override
      public WasmValue[] callFunction(String functionName, WasmValue... params)
          throws WasmException {
        throw new WasmException("Not implemented");
      }

      @Override
      public boolean isValid() {
        return true;
      }

      @Override
      public void close() {}
    };
  }

  @Nested
  @DisplayName("getTag() Default Method")
  class GetTagTests {

    @Test
    @DisplayName("should return empty by default")
    void shouldReturnEmptyByDefault() {
      Instance instance = createEmptyInstance();
      assertFalse(instance.getTag("myTag").isPresent());
    }
  }

  @Nested
  @DisplayName("getTypedFunc() Default Method")
  class GetTypedFuncTests {

    @Test
    @DisplayName("should throw for null name")
    void shouldThrowForNullName() {
      Instance instance = createEmptyInstance();
      assertThrows(
          IllegalArgumentException.class, () -> instance.getTypedFunc(null, WasmValueType.I32));
    }

    @Test
    @DisplayName("should throw for null paramTypes")
    void shouldThrowForNullParamTypes() {
      Instance instance = createEmptyInstance();
      assertThrows(
          IllegalArgumentException.class,
          () -> instance.getTypedFunc("add", (WasmValueType[]) null));
    }

    @Test
    @DisplayName("should return empty for non-existent function")
    void shouldReturnEmptyForMissing() {
      Instance instance = createEmptyInstance();
      assertFalse(instance.getTypedFunc("missing", WasmValueType.I32).isPresent());
    }
  }

  @Nested
  @DisplayName("Debug Default Methods")
  class DebugDefaultTests {

    @Test
    @DisplayName("debugFunction() should return empty by default")
    void debugFunctionShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugFunction(0).isPresent());
    }

    @Test
    @DisplayName("debugGlobal() should return empty by default")
    void debugGlobalShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugGlobal(0).isPresent());
    }

    @Test
    @DisplayName("debugMemory() should return empty by default")
    void debugMemoryShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugMemory(0).isPresent());
    }

    @Test
    @DisplayName("debugSharedMemory() should return empty by default")
    void debugSharedMemoryShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugSharedMemory(0).isPresent());
    }

    @Test
    @DisplayName("debugTable() should return empty by default")
    void debugTableShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugTable(0).isPresent());
    }

    @Test
    @DisplayName("debugTag() should return empty by default")
    void debugTagShouldReturnEmpty() {
      assertFalse(createEmptyInstance().debugTag(0).isPresent());
    }
  }

  @Nested
  @DisplayName("getDefaultMemory() Default Method")
  class GetDefaultMemoryTests {

    @Test
    @DisplayName("should return empty when no memories exported")
    void shouldReturnEmptyWhenNoMemories() {
      Instance instance = createEmptyInstance();
      assertFalse(instance.getDefaultMemory().isPresent());
    }
  }

  @Nested
  @DisplayName("getExports() Default Method")
  class GetExportsTests {

    @Test
    @DisplayName("should return empty list when no exports")
    void shouldReturnEmptyListWhenNoExports() {
      Instance instance = createEmptyInstance();
      assertTrue(instance.getExports().isEmpty());
    }
  }

  @Nested
  @DisplayName("create() Static Method")
  class CreateTests {

    @Test
    @DisplayName("createAsync should throw for null store")
    void createAsyncShouldThrowForNullStore() {
      // Instance.createAsync delegates to store.createInstanceAsync which would NPE
      // The static method itself does not null-check, so we verify it does something sensible
      assertThrows(NullPointerException.class, () -> Instance.createAsync(null, null));
    }
  }
}
