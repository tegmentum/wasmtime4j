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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmType} interface.
 *
 * <p>This test class verifies WasmType interface contract and implementations.
 */
@DisplayName("WasmType Interface Tests")
class WasmTypeInterfaceTest {

  /** A mock implementation of WasmType for testing. */
  private static class MockFunctionType implements WasmType {
    @Override
    public WasmTypeKind getKind() {
      return WasmTypeKind.FUNCTION;
    }
  }

  /** A mock implementation of WasmType for testing. */
  private static class MockGlobalType implements WasmType {
    @Override
    public WasmTypeKind getKind() {
      return WasmTypeKind.GLOBAL;
    }
  }

  /** A mock implementation of WasmType for testing. */
  private static class MockMemoryType implements WasmType {
    @Override
    public WasmTypeKind getKind() {
      return WasmTypeKind.MEMORY;
    }
  }

  /** A mock implementation of WasmType for testing. */
  private static class MockTableType implements WasmType {
    @Override
    public WasmTypeKind getKind() {
      return WasmTypeKind.TABLE;
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return FUNCTION for function type implementation")
    void getKindShouldReturnFunctionForFunctionTypeImpl() {
      final WasmType type = new MockFunctionType();
      assertEquals(WasmTypeKind.FUNCTION, type.getKind(), "Kind should be FUNCTION");
    }

    @Test
    @DisplayName("getKind should return GLOBAL for global type implementation")
    void getKindShouldReturnGlobalForGlobalTypeImpl() {
      final WasmType type = new MockGlobalType();
      assertEquals(WasmTypeKind.GLOBAL, type.getKind(), "Kind should be GLOBAL");
    }

    @Test
    @DisplayName("getKind should return MEMORY for memory type implementation")
    void getKindShouldReturnMemoryForMemoryTypeImpl() {
      final WasmType type = new MockMemoryType();
      assertEquals(WasmTypeKind.MEMORY, type.getKind(), "Kind should be MEMORY");
    }

    @Test
    @DisplayName("getKind should return TABLE for table type implementation")
    void getKindShouldReturnTableForTableTypeImpl() {
      final WasmType type = new MockTableType();
      assertEquals(WasmTypeKind.TABLE, type.getKind(), "Kind should be TABLE");
    }
  }

  @Nested
  @DisplayName("Polymorphism Tests")
  class PolymorphismTests {

    @Test
    @DisplayName("Different implementations should be distinguishable by kind")
    void differentImplementationsShouldBeDistinguishableByKind() {
      final WasmType[] types = {
        new MockFunctionType(), new MockGlobalType(), new MockMemoryType(), new MockTableType()
      };

      final WasmTypeKind[] expectedKinds = {
        WasmTypeKind.FUNCTION, WasmTypeKind.GLOBAL, WasmTypeKind.MEMORY, WasmTypeKind.TABLE
      };

      for (int i = 0; i < types.length; i++) {
        assertEquals(
            expectedKinds[i], types[i].getKind(), "Kind should match for type at index " + i);
      }
    }

    @Test
    @DisplayName("WasmType array should work polymorphically")
    void wasmTypeArrayShouldWorkPolymorphically() {
      final WasmType[] types = new WasmType[4];
      types[0] = new MockFunctionType();
      types[1] = new MockGlobalType();
      types[2] = new MockMemoryType();
      types[3] = new MockTableType();

      for (WasmType type : types) {
        assertNotNull(type.getKind(), "Kind should not be null");
      }
    }

    @Test
    @DisplayName("Switch on WasmType kind should work")
    void switchOnWasmTypeKindShouldWork() {
      final WasmType type = new MockFunctionType();

      final String result;
      switch (type.getKind()) {
        case FUNCTION:
          result = "function";
          break;
        case GLOBAL:
          result = "global";
          break;
        case MEMORY:
          result = "memory";
          break;
        case TABLE:
          result = "table";
          break;
        default:
          result = "unknown";
          break;
      }

      assertEquals("function", result, "Switch should work correctly");
    }
  }

  @Nested
  @DisplayName("Extended Interface Tests")
  class ExtendedInterfaceTests {

    @Test
    @DisplayName("FuncType should extend WasmType")
    void funcTypeShouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should extend WasmType");
    }

    @Test
    @DisplayName("GlobalType should extend WasmType")
    void globalTypeShouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class), "GlobalType should extend WasmType");
    }

    @Test
    @DisplayName("MemoryType should extend WasmType")
    void memoryTypeShouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class), "MemoryType should extend WasmType");
    }

    @Test
    @DisplayName("TableType should extend WasmType")
    void tableTypeShouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(TableType.class), "TableType should extend WasmType");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("FuncType default getKind should return FUNCTION")
    void funcTypeDefaultGetKindShouldReturnFunction() {
      // Create anonymous implementation to test default method
      final FuncType funcType =
          new FuncType() {
            @Override
            public java.util.List<WasmValueType> getParams() {
              return java.util.Collections.emptyList();
            }

            @Override
            public java.util.List<WasmValueType> getResults() {
              return java.util.Collections.emptyList();
            }
          };

      assertEquals(
          WasmTypeKind.FUNCTION, funcType.getKind(), "Default getKind should return FUNCTION");
    }

    @Test
    @DisplayName("GlobalType default getKind should return GLOBAL")
    void globalTypeDefaultGetKindShouldReturnGlobal() {
      final GlobalType globalType =
          new GlobalType() {
            @Override
            public WasmValueType getValueType() {
              return WasmValueType.I32;
            }

            @Override
            public boolean isMutable() {
              return false;
            }
          };

      assertEquals(
          WasmTypeKind.GLOBAL, globalType.getKind(), "Default getKind should return GLOBAL");
    }

    @Test
    @DisplayName("MemoryType default getKind should return MEMORY")
    void memoryTypeDefaultGetKindShouldReturnMemory() {
      final MemoryType memoryType =
          new MemoryType() {
            @Override
            public long getMinimum() {
              return 1;
            }

            @Override
            public java.util.Optional<Long> getMaximum() {
              return java.util.Optional.empty();
            }

            @Override
            public boolean is64Bit() {
              return false;
            }

            @Override
            public boolean isShared() {
              return false;
            }
          };

      assertEquals(
          WasmTypeKind.MEMORY, memoryType.getKind(), "Default getKind should return MEMORY");
    }

    @Test
    @DisplayName("TableType default getKind should return TABLE")
    void tableTypeDefaultGetKindShouldReturnTable() {
      final TableType tableType =
          new TableType() {
            @Override
            public WasmValueType getElementType() {
              return WasmValueType.FUNCREF;
            }

            @Override
            public long getMinimum() {
              return 0;
            }

            @Override
            public java.util.Optional<Long> getMaximum() {
              return java.util.Optional.empty();
            }
          };

      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Default getKind should return TABLE");
    }
  }
}
