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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link Extern} interface.
 *
 * <p>Verifies default method implementations for type checking (isFunction, isMemory, etc.),
 * casting methods (asFunction, asMemory, etc.), and getExternTypeInfo logic. Uses anonymous
 * implementations of the interface rather than mocks.
 */
@DisplayName("Extern Interface Tests")
class ExternTest {

  /**
   * Minimal Extern implementation that returns a fixed ExternType. All casting methods use the
   * interface defaults (return null).
   */
  private static Extern externOf(final ExternType type) {
    return new Extern() {
      @Override
      public ExternType getType() {
        return type;
      }
    };
  }

  /**
   * Extern implementation that also provides a detailed WasmType, enabling getExternTypeInfo
   * testing.
   */
  private static Extern externWithDetailedType(final ExternType type, final WasmType detailedType) {
    return new Extern() {
      @Override
      public ExternType getType() {
        return type;
      }

      @Override
      public WasmType getDetailedType() {
        return detailedType;
      }
    };
  }

  @Nested
  @DisplayName("Type Checking Default Methods")
  class TypeCheckingTests {

    @Test
    @DisplayName("FUNC extern should report isFunction true and all others false")
    void funcExternShouldReportIsFunctionTrue() {
      final Extern extern = externOf(ExternType.FUNC);

      assertTrue(extern.isFunction(), "FUNC extern should return true for isFunction()");
      assertFalse(extern.isTable(), "FUNC extern should return false for isTable()");
      assertFalse(extern.isMemory(), "FUNC extern should return false for isMemory()");
      assertFalse(extern.isGlobal(), "FUNC extern should return false for isGlobal()");
      assertFalse(extern.isTag(), "FUNC extern should return false for isTag()");
      assertFalse(extern.isSharedMemory(), "FUNC extern should return false for isSharedMemory()");
    }

    @Test
    @DisplayName("TABLE extern should report isTable true and all others false")
    void tableExternShouldReportIsTableTrue() {
      final Extern extern = externOf(ExternType.TABLE);

      assertFalse(extern.isFunction(), "TABLE extern should return false for isFunction()");
      assertTrue(extern.isTable(), "TABLE extern should return true for isTable()");
      assertFalse(extern.isMemory(), "TABLE extern should return false for isMemory()");
      assertFalse(extern.isGlobal(), "TABLE extern should return false for isGlobal()");
      assertFalse(extern.isTag(), "TABLE extern should return false for isTag()");
      assertFalse(extern.isSharedMemory(), "TABLE extern should return false for isSharedMemory()");
    }

    @Test
    @DisplayName("MEMORY extern should report isMemory true and all others false")
    void memoryExternShouldReportIsMemoryTrue() {
      final Extern extern = externOf(ExternType.MEMORY);

      assertFalse(extern.isFunction(), "MEMORY extern should return false for isFunction()");
      assertFalse(extern.isTable(), "MEMORY extern should return false for isTable()");
      assertTrue(extern.isMemory(), "MEMORY extern should return true for isMemory()");
      assertFalse(extern.isGlobal(), "MEMORY extern should return false for isGlobal()");
      assertFalse(extern.isTag(), "MEMORY extern should return false for isTag()");
      assertFalse(
          extern.isSharedMemory(), "MEMORY extern should return false for isSharedMemory()");
    }

    @Test
    @DisplayName("GLOBAL extern should report isGlobal true and all others false")
    void globalExternShouldReportIsGlobalTrue() {
      final Extern extern = externOf(ExternType.GLOBAL);

      assertFalse(extern.isFunction(), "GLOBAL extern should return false for isFunction()");
      assertFalse(extern.isTable(), "GLOBAL extern should return false for isTable()");
      assertFalse(extern.isMemory(), "GLOBAL extern should return false for isMemory()");
      assertTrue(extern.isGlobal(), "GLOBAL extern should return true for isGlobal()");
      assertFalse(extern.isTag(), "GLOBAL extern should return false for isTag()");
      assertFalse(
          extern.isSharedMemory(), "GLOBAL extern should return false for isSharedMemory()");
    }

    @Test
    @DisplayName("TAG extern should report isTag true and all others false")
    void tagExternShouldReportIsTagTrue() {
      final Extern extern = externOf(ExternType.TAG);

      assertFalse(extern.isFunction(), "TAG extern should return false for isFunction()");
      assertFalse(extern.isTable(), "TAG extern should return false for isTable()");
      assertFalse(extern.isMemory(), "TAG extern should return false for isMemory()");
      assertFalse(extern.isGlobal(), "TAG extern should return false for isGlobal()");
      assertTrue(extern.isTag(), "TAG extern should return true for isTag()");
      assertFalse(extern.isSharedMemory(), "TAG extern should return false for isSharedMemory()");
    }

    @Test
    @DisplayName("SHARED_MEMORY extern should report isSharedMemory true and all others false")
    void sharedMemoryExternShouldReportIsSharedMemoryTrue() {
      final Extern extern = externOf(ExternType.SHARED_MEMORY);

      assertFalse(extern.isFunction(), "SHARED_MEMORY extern should return false for isFunction()");
      assertFalse(extern.isTable(), "SHARED_MEMORY extern should return false for isTable()");
      assertFalse(extern.isMemory(), "SHARED_MEMORY extern should return false for isMemory()");
      assertFalse(extern.isGlobal(), "SHARED_MEMORY extern should return false for isGlobal()");
      assertFalse(extern.isTag(), "SHARED_MEMORY extern should return false for isTag()");
      assertTrue(
          extern.isSharedMemory(), "SHARED_MEMORY extern should return true for isSharedMemory()");
    }

    @Test
    @DisplayName("each ExternType should match exactly one type-check method")
    void eachExternTypeShouldMatchExactlyOneCheck() {
      for (final ExternType type : ExternType.values()) {
        final Extern extern = externOf(type);
        int trueCount = 0;
        if (extern.isFunction()) {
          trueCount++;
        }
        if (extern.isTable()) {
          trueCount++;
        }
        if (extern.isMemory()) {
          trueCount++;
        }
        if (extern.isGlobal()) {
          trueCount++;
        }
        if (extern.isTag()) {
          trueCount++;
        }
        if (extern.isSharedMemory()) {
          trueCount++;
        }
        assertEquals(
            1,
            trueCount,
            "ExternType "
                + type.name()
                + " should match exactly one type-check method,"
                + " but matched "
                + trueCount);
      }
    }
  }

  @Nested
  @DisplayName("Casting Default Methods")
  class CastingTests {

    @Test
    @DisplayName("default asFunction should return null")
    void defaultAsFunctionShouldReturnNull() {
      final Extern extern = externOf(ExternType.FUNC);
      assertNull(extern.asFunction(), "Default asFunction() should return null");
    }

    @Test
    @DisplayName("default asTable should return null")
    void defaultAsTableShouldReturnNull() {
      final Extern extern = externOf(ExternType.TABLE);
      assertNull(extern.asTable(), "Default asTable() should return null");
    }

    @Test
    @DisplayName("default asMemory should return null")
    void defaultAsMemoryShouldReturnNull() {
      final Extern extern = externOf(ExternType.MEMORY);
      assertNull(extern.asMemory(), "Default asMemory() should return null");
    }

    @Test
    @DisplayName("default asGlobal should return null")
    void defaultAsGlobalShouldReturnNull() {
      final Extern extern = externOf(ExternType.GLOBAL);
      assertNull(extern.asGlobal(), "Default asGlobal() should return null");
    }

    @Test
    @DisplayName("default asTag should return null")
    void defaultAsTagShouldReturnNull() {
      final Extern extern = externOf(ExternType.TAG);
      assertNull(extern.asTag(), "Default asTag() should return null");
    }

    @Test
    @DisplayName("default asSharedMemory should return null")
    void defaultAsSharedMemoryShouldReturnNull() {
      final Extern extern = externOf(ExternType.SHARED_MEMORY);
      assertNull(extern.asSharedMemory(), "Default asSharedMemory() should return null");
    }

    @Test
    @DisplayName("all casting defaults return null regardless of ExternType")
    void allCastingDefaultsReturnNullRegardlessOfType() {
      for (final ExternType type : ExternType.values()) {
        final Extern extern = externOf(type);
        assertNull(
            extern.asFunction(),
            "asFunction() default should be null for ExternType." + type.name());
        assertNull(
            extern.asTable(), "asTable() default should be null for ExternType." + type.name());
        assertNull(
            extern.asMemory(), "asMemory() default should be null for ExternType." + type.name());
        assertNull(
            extern.asGlobal(), "asGlobal() default should be null for ExternType." + type.name());
        assertNull(extern.asTag(), "asTag() default should be null for ExternType." + type.name());
        assertNull(
            extern.asSharedMemory(),
            "asSharedMemory() default should be null for ExternType." + type.name());
      }
    }
  }

  @Nested
  @DisplayName("getDetailedType Default Method")
  class DetailedTypeTests {

    @Test
    @DisplayName("default getDetailedType should return null")
    void defaultGetDetailedTypeShouldReturnNull() {
      final Extern extern = externOf(ExternType.FUNC);
      assertNull(extern.getDetailedType(), "Default getDetailedType() should return null");
    }
  }

  @Nested
  @DisplayName("getExternTypeInfo Default Method")
  class ExternTypeInfoTests {

    @Test
    @DisplayName("should return null when getDetailedType returns null")
    void shouldReturnNullWhenDetailedTypeIsNull() {
      final Extern extern = externOf(ExternType.FUNC);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when getDetailedType() is null");
    }

    @Test
    @DisplayName("should return null when detailed type does not match expected class for FUNC")
    void shouldReturnNullWhenDetailedTypeMismatchesFunc() {
      // Provide a WasmType that is NOT a FuncType for a FUNC extern
      final WasmType nonFuncType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.MEMORY;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.FUNC, nonFuncType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when detailed type is not FuncType for FUNC"
              + " extern");
    }

    @Test
    @DisplayName("should return null when detailed type does not match expected class for TABLE")
    void shouldReturnNullWhenDetailedTypeMismatchesTable() {
      final WasmType nonTableType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.TABLE, nonTableType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when detailed type is not TableType for TABLE"
              + " extern");
    }

    @Test
    @DisplayName("should return null when detailed type does not match expected class for MEMORY")
    void shouldReturnNullWhenDetailedTypeMismatchesMemory() {
      final WasmType nonMemoryType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.MEMORY, nonMemoryType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when detailed type is not MemoryType for MEMORY"
              + " extern");
    }

    @Test
    @DisplayName("should return null when detailed type does not match expected class for GLOBAL")
    void shouldReturnNullWhenDetailedTypeMismatchesGlobal() {
      final WasmType nonGlobalType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.GLOBAL, nonGlobalType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when detailed type is not GlobalType for GLOBAL"
              + " extern");
    }

    @Test
    @DisplayName(
        "should return null when detailed type does not match expected class for SHARED_MEMORY")
    void shouldReturnNullWhenDetailedTypeMismatchesSharedMemory() {
      final WasmType nonMemoryType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.SHARED_MEMORY, nonMemoryType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null when detailed type is not MemoryType for"
              + " SHARED_MEMORY extern");
    }

    @Test
    @DisplayName("should return null for TAG extern with non-FuncType detailed type")
    void shouldReturnNullForTagWithNonFuncType() {
      // TAG falls into the default case in the switch, so it always returns null
      final WasmType nonFuncType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.MEMORY;
            }
          };
      final Extern extern = externWithDetailedType(ExternType.TAG, nonFuncType);
      assertNull(
          extern.getExternTypeInfo(),
          "getExternTypeInfo() should return null for TAG extern (default case in switch)");
    }
  }

  @Nested
  @DisplayName("getType Consistency Tests")
  class GetTypeConsistencyTests {

    @Test
    @DisplayName("getType should return the exact ExternType provided")
    void getTypeShouldReturnExactType() {
      for (final ExternType type : ExternType.values()) {
        final Extern extern = externOf(type);
        assertEquals(
            type,
            extern.getType(),
            "getType() should return " + type.name() + " for an extern created with that type");
      }
    }

    @Test
    @DisplayName("type-check methods should be consistent with getType")
    void typeCheckMethodsShouldBeConsistentWithGetType() {
      for (final ExternType type : ExternType.values()) {
        final Extern extern = externOf(type);
        assertEquals(
            type == ExternType.FUNC,
            extern.isFunction(),
            "isFunction() consistency with getType() for " + type.name());
        assertEquals(
            type == ExternType.TABLE,
            extern.isTable(),
            "isTable() consistency with getType() for " + type.name());
        assertEquals(
            type == ExternType.MEMORY,
            extern.isMemory(),
            "isMemory() consistency with getType() for " + type.name());
        assertEquals(
            type == ExternType.GLOBAL,
            extern.isGlobal(),
            "isGlobal() consistency with getType() for " + type.name());
        assertEquals(
            type == ExternType.TAG,
            extern.isTag(),
            "isTag() consistency with getType() for " + type.name());
        assertEquals(
            type == ExternType.SHARED_MEMORY,
            extern.isSharedMemory(),
            "isSharedMemory() consistency with getType() for " + type.name());
      }
    }
  }
}
