/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExportType;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for the WebAssembly type system.
 *
 * <p>This test class validates the type hierarchy and type metadata classes.
 */
@DisplayName("Type System Integration Tests")
public class TypeSystemIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(TypeSystemIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting Type System Integration Tests");
  }

  @Nested
  @DisplayName("WasmTypeKind Enum Tests")
  class WasmTypeKindTests {

    @Test
    @DisplayName("Should have all expected type kinds")
    void shouldHaveAllExpectedTypeKinds() {
      LOGGER.info("Testing WasmTypeKind enum values");

      WasmTypeKind[] kinds = WasmTypeKind.values();
      assertEquals(4, kinds.length, "Should have 4 type kinds");

      assertNotNull(WasmTypeKind.FUNCTION, "FUNCTION should exist");
      assertNotNull(WasmTypeKind.GLOBAL, "GLOBAL should exist");
      assertNotNull(WasmTypeKind.MEMORY, "MEMORY should exist");
      assertNotNull(WasmTypeKind.TABLE, "TABLE should exist");

      LOGGER.info("WasmTypeKind enum values verified: " + kinds.length);
    }

    @Test
    @DisplayName("Should have correct ordinal values")
    void shouldHaveCorrectOrdinalValues() {
      LOGGER.info("Testing WasmTypeKind ordinal values");

      assertEquals(0, WasmTypeKind.FUNCTION.ordinal(), "FUNCTION should have ordinal 0");
      assertEquals(1, WasmTypeKind.GLOBAL.ordinal(), "GLOBAL should have ordinal 1");
      assertEquals(2, WasmTypeKind.MEMORY.ordinal(), "MEMORY should have ordinal 2");
      assertEquals(3, WasmTypeKind.TABLE.ordinal(), "TABLE should have ordinal 3");

      LOGGER.info("WasmTypeKind ordinal values verified");
    }

    @Test
    @DisplayName("Should support valueOf lookup")
    void shouldSupportValueOfLookup() {
      LOGGER.info("Testing WasmTypeKind valueOf");

      assertEquals(WasmTypeKind.FUNCTION, WasmTypeKind.valueOf("FUNCTION"));
      assertEquals(WasmTypeKind.GLOBAL, WasmTypeKind.valueOf("GLOBAL"));
      assertEquals(WasmTypeKind.MEMORY, WasmTypeKind.valueOf("MEMORY"));
      assertEquals(WasmTypeKind.TABLE, WasmTypeKind.valueOf("TABLE"));

      LOGGER.info("WasmTypeKind valueOf verified");
    }
  }

  @Nested
  @DisplayName("ExternType Enum Tests")
  class ExternTypeTests {

    @Test
    @DisplayName("Should have all expected extern types")
    void shouldHaveAllExpectedExternTypes() {
      LOGGER.info("Testing ExternType enum values");

      ExternType[] types = ExternType.values();
      assertEquals(4, types.length, "Should have 4 extern types");

      assertNotNull(ExternType.FUNC, "FUNC should exist");
      assertNotNull(ExternType.TABLE, "TABLE should exist");
      assertNotNull(ExternType.MEMORY, "MEMORY should exist");
      assertNotNull(ExternType.GLOBAL, "GLOBAL should exist");

      LOGGER.info("ExternType enum values verified: " + types.length);
    }

    @Test
    @DisplayName("Should have correct numeric codes")
    void shouldHaveCorrectNumericCodes() {
      LOGGER.info("Testing ExternType numeric codes");

      assertEquals(0, ExternType.FUNC.getCode(), "FUNC should have code 0");
      assertEquals(1, ExternType.TABLE.getCode(), "TABLE should have code 1");
      assertEquals(2, ExternType.MEMORY.getCode(), "MEMORY should have code 2");
      assertEquals(3, ExternType.GLOBAL.getCode(), "GLOBAL should have code 3");

      LOGGER.info("ExternType numeric codes verified");
    }

    @Test
    @DisplayName("Should support fromCode lookup")
    void shouldSupportFromCodeLookup() {
      LOGGER.info("Testing ExternType fromCode");

      assertEquals(ExternType.FUNC, ExternType.fromCode(0), "Code 0 should return FUNC");
      assertEquals(ExternType.TABLE, ExternType.fromCode(1), "Code 1 should return TABLE");
      assertEquals(ExternType.MEMORY, ExternType.fromCode(2), "Code 2 should return MEMORY");
      assertEquals(ExternType.GLOBAL, ExternType.fromCode(3), "Code 3 should return GLOBAL");

      LOGGER.info("ExternType fromCode verified");
    }

    @Test
    @DisplayName("Should throw for invalid codes")
    void shouldThrowForInvalidCodes() {
      LOGGER.info("Testing ExternType invalid code handling");

      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(-1),
          "Should throw for negative code");
      assertThrows(
          IllegalArgumentException.class, () -> ExternType.fromCode(4), "Should throw for code 4");
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(100),
          "Should throw for code 100");

      LOGGER.info("ExternType invalid code handling verified");
    }
  }

  @Nested
  @DisplayName("FuncType Interface Tests")
  class FuncTypeInterfaceTests {

    @Test
    @DisplayName("Should verify FuncType interface exists")
    void shouldVerifyFuncTypeInterfaceExists() {
      LOGGER.info("Testing FuncType interface existence");

      assertTrue(FuncType.class.isInterface(), "FuncType should be an interface");
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should extend WasmType");

      LOGGER.info("FuncType interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing FuncType interface methods");

      Method getParams = FuncType.class.getMethod("getParams");
      assertNotNull(getParams, "getParams method should exist");

      Method getResults = FuncType.class.getMethod("getResults");
      assertNotNull(getResults, "getResults method should exist");

      Method getParamCount = FuncType.class.getMethod("getParamCount");
      assertNotNull(getParamCount, "getParamCount method should exist");

      Method getResultCount = FuncType.class.getMethod("getResultCount");
      assertNotNull(getResultCount, "getResultCount method should exist");

      LOGGER.info("FuncType interface methods verified");
    }

    @Test
    @DisplayName("Should have correct default WasmTypeKind")
    void shouldHaveCorrectDefaultWasmTypeKind() throws Exception {
      LOGGER.info("Testing FuncType default getKind method");

      Method getKind = FuncType.class.getMethod("getKind");
      assertNotNull(getKind, "getKind method should exist");
      assertTrue(getKind.isDefault(), "getKind should be a default method");

      LOGGER.info("FuncType default getKind method verified");
    }
  }

  @Nested
  @DisplayName("MemoryType Interface Tests")
  class MemoryTypeInterfaceTests {

    @Test
    @DisplayName("Should verify MemoryType interface exists")
    void shouldVerifyMemoryTypeInterfaceExists() {
      LOGGER.info("Testing MemoryType interface existence");

      assertTrue(MemoryType.class.isInterface(), "MemoryType should be an interface");
      assertTrue(
          WasmType.class.isAssignableFrom(MemoryType.class), "MemoryType should extend WasmType");

      LOGGER.info("MemoryType interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing MemoryType interface methods");

      Method getMinimum = MemoryType.class.getMethod("getMinimum");
      assertNotNull(getMinimum, "getMinimum method should exist");

      Method getMaximum = MemoryType.class.getMethod("getMaximum");
      assertNotNull(getMaximum, "getMaximum method should exist");

      Method is64Bit = MemoryType.class.getMethod("is64Bit");
      assertNotNull(is64Bit, "is64Bit method should exist");

      Method isShared = MemoryType.class.getMethod("isShared");
      assertNotNull(isShared, "isShared method should exist");

      LOGGER.info("MemoryType interface methods verified");
    }

    @Test
    @DisplayName("Should have correct default WasmTypeKind")
    void shouldHaveCorrectDefaultWasmTypeKind() throws Exception {
      LOGGER.info("Testing MemoryType default getKind method");

      Method getKind = MemoryType.class.getMethod("getKind");
      assertNotNull(getKind, "getKind method should exist");
      assertTrue(getKind.isDefault(), "getKind should be a default method");

      LOGGER.info("MemoryType default getKind method verified");
    }
  }

  @Nested
  @DisplayName("GlobalType Interface Tests")
  class GlobalTypeInterfaceTests {

    @Test
    @DisplayName("Should verify GlobalType interface exists")
    void shouldVerifyGlobalTypeInterfaceExists() {
      LOGGER.info("Testing GlobalType interface existence");

      assertTrue(GlobalType.class.isInterface(), "GlobalType should be an interface");
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class), "GlobalType should extend WasmType");

      LOGGER.info("GlobalType interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing GlobalType interface methods");

      Method getValueType = GlobalType.class.getMethod("getValueType");
      assertNotNull(getValueType, "getValueType method should exist");

      Method isMutable = GlobalType.class.getMethod("isMutable");
      assertNotNull(isMutable, "isMutable method should exist");

      LOGGER.info("GlobalType interface methods verified");
    }

    @Test
    @DisplayName("Should have correct default WasmTypeKind")
    void shouldHaveCorrectDefaultWasmTypeKind() throws Exception {
      LOGGER.info("Testing GlobalType default getKind method");

      Method getKind = GlobalType.class.getMethod("getKind");
      assertNotNull(getKind, "getKind method should exist");
      assertTrue(getKind.isDefault(), "getKind should be a default method");

      LOGGER.info("GlobalType default getKind method verified");
    }
  }

  @Nested
  @DisplayName("TableType Interface Tests")
  class TableTypeInterfaceTests {

    @Test
    @DisplayName("Should verify TableType interface exists")
    void shouldVerifyTableTypeInterfaceExists() {
      LOGGER.info("Testing TableType interface existence");

      assertTrue(TableType.class.isInterface(), "TableType should be an interface");
      assertTrue(
          WasmType.class.isAssignableFrom(TableType.class), "TableType should extend WasmType");

      LOGGER.info("TableType interface verified");
    }

    @Test
    @DisplayName("Should have required methods")
    void shouldHaveRequiredMethods() throws Exception {
      LOGGER.info("Testing TableType interface methods");

      Method getElementType = TableType.class.getMethod("getElementType");
      assertNotNull(getElementType, "getElementType method should exist");

      Method getMinimum = TableType.class.getMethod("getMinimum");
      assertNotNull(getMinimum, "getMinimum method should exist");

      Method getMaximum = TableType.class.getMethod("getMaximum");
      assertNotNull(getMaximum, "getMaximum method should exist");

      LOGGER.info("TableType interface methods verified");
    }

    @Test
    @DisplayName("Should have correct default WasmTypeKind")
    void shouldHaveCorrectDefaultWasmTypeKind() throws Exception {
      LOGGER.info("Testing TableType default getKind method");

      Method getKind = TableType.class.getMethod("getKind");
      assertNotNull(getKind, "getKind method should exist");
      assertTrue(getKind.isDefault(), "getKind should be a default method");

      LOGGER.info("TableType default getKind method verified");
    }
  }

  @Nested
  @DisplayName("ImportType Class Tests")
  class ImportTypeClassTests {

    @Test
    @DisplayName("Should verify ImportType class exists")
    void shouldVerifyImportTypeClassExists() {
      LOGGER.info("Testing ImportType class existence");

      assertNotNull(ImportType.class, "ImportType class should exist");
      assertFalse(ImportType.class.isInterface(), "ImportType should be a class");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ImportType.class.getModifiers()),
          "ImportType should be final");

      LOGGER.info("ImportType class verified");
    }

    @Test
    @DisplayName("Should create ImportType with valid parameters")
    void shouldCreateImportTypeWithValidParameters() {
      LOGGER.info("Testing ImportType creation");

      // Using a minimal mock for WasmType
      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };

      ImportType importType = new ImportType("env", "print", mockType);

      assertEquals("env", importType.getModuleName(), "Module name should match");
      assertEquals("print", importType.getName(), "Name should match");
      assertEquals(mockType, importType.getType(), "Type should match");

      LOGGER.info("ImportType creation verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing ImportType equals");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }

            @Override
            public boolean equals(final Object obj) {
              return this == obj;
            }

            @Override
            public int hashCode() {
              return System.identityHashCode(this);
            }
          };

      ImportType importType1 = new ImportType("env", "print", mockType);
      ImportType importType2 = new ImportType("env", "print", mockType);
      ImportType importType3 = new ImportType("wasi", "print", mockType);

      assertEquals(importType1, importType2, "Same values should be equal");
      assertNotEquals(importType1, importType3, "Different module names should not be equal");
      assertNotEquals(importType1, null, "Should not equal null");

      LOGGER.info("ImportType equals verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing ImportType hashCode");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.MEMORY;
            }

            @Override
            public int hashCode() {
              return 42;
            }
          };

      ImportType importType1 = new ImportType("env", "memory", mockType);
      ImportType importType2 = new ImportType("env", "memory", mockType);

      assertEquals(
          importType1.hashCode(), importType2.hashCode(), "Same values should have same hashCode");

      LOGGER.info("ImportType hashCode verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing ImportType toString");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.GLOBAL;
            }

            @Override
            public String toString() {
              return "MockGlobalType";
            }
          };

      ImportType importType = new ImportType("env", "global_var", mockType);
      String result = importType.toString();

      assertTrue(result.contains("env"), "toString should contain module name");
      assertTrue(result.contains("global_var"), "toString should contain name");

      LOGGER.info("ImportType toString verified: " + result);
    }
  }

  @Nested
  @DisplayName("ExportType Class Tests")
  class ExportTypeClassTests {

    @Test
    @DisplayName("Should verify ExportType class exists")
    void shouldVerifyExportTypeClassExists() {
      LOGGER.info("Testing ExportType class existence");

      assertNotNull(ExportType.class, "ExportType class should exist");
      assertFalse(ExportType.class.isInterface(), "ExportType should be a class");
      assertTrue(
          java.lang.reflect.Modifier.isFinal(ExportType.class.getModifiers()),
          "ExportType should be final");

      LOGGER.info("ExportType class verified");
    }

    @Test
    @DisplayName("Should create ExportType with valid parameters")
    void shouldCreateExportTypeWithValidParameters() {
      LOGGER.info("Testing ExportType creation");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }
          };

      ExportType exportType = new ExportType("main", mockType);

      assertEquals("main", exportType.getName(), "Name should match");
      assertEquals(mockType, exportType.getType(), "Type should match");

      LOGGER.info("ExportType creation verified");
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      LOGGER.info("Testing ExportType equals");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.FUNCTION;
            }

            @Override
            public boolean equals(final Object obj) {
              return this == obj;
            }

            @Override
            public int hashCode() {
              return System.identityHashCode(this);
            }
          };

      ExportType exportType1 = new ExportType("main", mockType);
      ExportType exportType2 = new ExportType("main", mockType);
      ExportType exportType3 = new ExportType("start", mockType);

      assertEquals(exportType1, exportType2, "Same values should be equal");
      assertNotEquals(exportType1, exportType3, "Different names should not be equal");
      assertNotEquals(exportType1, null, "Should not equal null");

      LOGGER.info("ExportType equals verified");
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      LOGGER.info("Testing ExportType hashCode");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.MEMORY;
            }

            @Override
            public int hashCode() {
              return 42;
            }
          };

      ExportType exportType1 = new ExportType("memory", mockType);
      ExportType exportType2 = new ExportType("memory", mockType);

      assertEquals(
          exportType1.hashCode(), exportType2.hashCode(), "Same values should have same hashCode");

      LOGGER.info("ExportType hashCode verified");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      LOGGER.info("Testing ExportType toString");

      WasmType mockType =
          new WasmType() {
            @Override
            public WasmTypeKind getKind() {
              return WasmTypeKind.TABLE;
            }

            @Override
            public String toString() {
              return "MockTableType";
            }
          };

      ExportType exportType = new ExportType("indirect_table", mockType);
      String result = exportType.toString();

      assertTrue(result.contains("indirect_table"), "toString should contain name");

      LOGGER.info("ExportType toString verified: " + result);
    }
  }
}
