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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.FrameSymbol;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FrameSymbol} class.
 *
 * <p>FrameSymbol provides source-level debugging information extracted from DWARF debug symbols.
 */
@DisplayName("FrameSymbol Tests")
class FrameSymbolTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all non-null parameters")
    void shouldCreateInstanceWithAllNonNullParameters() {
      final FrameSymbol symbol = new FrameSymbol("main", "test.c", 42, 10);

      assertNotNull(symbol, "FrameSymbol instance should not be null");
      assertTrue(symbol.getName().isPresent(), "name should be present");
      assertEquals("main", symbol.getName().get(), "name should be 'main'");
      assertTrue(symbol.getFile().isPresent(), "file should be present");
      assertEquals("test.c", symbol.getFile().get(), "file should be 'test.c'");
      assertTrue(symbol.getLine().isPresent(), "line should be present");
      assertEquals(42, symbol.getLine().get(), "line should be 42");
      assertTrue(symbol.getColumn().isPresent(), "column should be present");
      assertEquals(10, symbol.getColumn().get(), "column should be 10");
    }

    @Test
    @DisplayName("should create instance with all null parameters")
    void shouldCreateInstanceWithAllNullParameters() {
      final FrameSymbol symbol = new FrameSymbol(null, null, null, null);

      assertFalse(symbol.getName().isPresent(), "name should be empty");
      assertFalse(symbol.getFile().isPresent(), "file should be empty");
      assertFalse(symbol.getLine().isPresent(), "line should be empty");
      assertFalse(symbol.getColumn().isPresent(), "column should be empty");
    }

    @Test
    @DisplayName("should create instance with partial null parameters")
    void shouldCreateInstanceWithPartialNullParameters() {
      final FrameSymbol symbol = new FrameSymbol("func", null, 15, null);

      assertTrue(symbol.getName().isPresent(), "name should be present");
      assertFalse(symbol.getFile().isPresent(), "file should be empty");
      assertTrue(symbol.getLine().isPresent(), "line should be present");
      assertFalse(symbol.getColumn().isPresent(), "column should be empty");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getName should return Optional with name value")
    void getNameShouldReturnOptionalWithNameValue() {
      final FrameSymbol symbol = new FrameSymbol("compute", null, null, null);

      assertEquals(
          Optional.of("compute"), symbol.getName(), "getName should return Optional of 'compute'");
    }

    @Test
    @DisplayName("getFile should return Optional with file value")
    void getFileShouldReturnOptionalWithFileValue() {
      final FrameSymbol symbol = new FrameSymbol(null, "/src/main.rs", null, null);

      assertEquals(
          Optional.of("/src/main.rs"),
          symbol.getFile(),
          "getFile should return Optional of '/src/main.rs'");
    }

    @Test
    @DisplayName("getLine should return Optional with line value")
    void getLineShouldReturnOptionalWithLineValue() {
      final FrameSymbol symbol = new FrameSymbol(null, null, 100, null);

      assertEquals(Optional.of(100), symbol.getLine(), "getLine should return Optional of 100");
    }

    @Test
    @DisplayName("getColumn should return Optional with column value")
    void getColumnShouldReturnOptionalWithColumnValue() {
      final FrameSymbol symbol = new FrameSymbol(null, null, null, 25);

      assertEquals(Optional.of(25), symbol.getColumn(), "getColumn should return Optional of 25");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final FrameSymbol sym1 = new FrameSymbol("main", "test.c", 10, 5);
      final FrameSymbol sym2 = new FrameSymbol("main", "test.c", 10, 5);

      assertEquals(sym1, sym2, "Two FrameSymbol with same values should be equal");
    }

    @Test
    @DisplayName("equals should return true for both null values")
    void equalsShouldReturnTrueForBothNullValues() {
      final FrameSymbol sym1 = new FrameSymbol(null, null, null, null);
      final FrameSymbol sym2 = new FrameSymbol(null, null, null, null);

      assertEquals(sym1, sym2, "Two FrameSymbol with all nulls should be equal");
    }

    @Test
    @DisplayName("equals should return false for different name")
    void equalsShouldReturnFalseForDifferentName() {
      final FrameSymbol sym1 = new FrameSymbol("funcA", "file.c", 1, 1);
      final FrameSymbol sym2 = new FrameSymbol("funcB", "file.c", 1, 1);

      assertNotEquals(sym1, sym2, "FrameSymbol with different name should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different file")
    void equalsShouldReturnFalseForDifferentFile() {
      final FrameSymbol sym1 = new FrameSymbol("fn", "a.c", 1, 1);
      final FrameSymbol sym2 = new FrameSymbol("fn", "b.c", 1, 1);

      assertNotEquals(sym1, sym2, "FrameSymbol with different file should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different line")
    void equalsShouldReturnFalseForDifferentLine() {
      final FrameSymbol sym1 = new FrameSymbol("fn", "a.c", 1, 1);
      final FrameSymbol sym2 = new FrameSymbol("fn", "a.c", 2, 1);

      assertNotEquals(sym1, sym2, "FrameSymbol with different line should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different column")
    void equalsShouldReturnFalseForDifferentColumn() {
      final FrameSymbol sym1 = new FrameSymbol("fn", "a.c", 1, 1);
      final FrameSymbol sym2 = new FrameSymbol("fn", "a.c", 1, 2);

      assertNotEquals(sym1, sym2, "FrameSymbol with different column should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same object")
    void equalsShouldReturnTrueForSameObject() {
      final FrameSymbol sym = new FrameSymbol("fn", "a.c", 1, 1);

      assertEquals(sym, sym, "Same object should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final FrameSymbol sym = new FrameSymbol("fn", "a.c", 1, 1);

      assertNotEquals(null, sym, "FrameSymbol should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final FrameSymbol sym = new FrameSymbol("fn", "a.c", 1, 1);

      assertNotEquals("string", sym, "FrameSymbol should not be equal to a String");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal instances")
    void hashCodeShouldBeConsistentForEqualInstances() {
      final FrameSymbol sym1 = new FrameSymbol("main", "test.c", 10, 5);
      final FrameSymbol sym2 = new FrameSymbol("main", "test.c", 10, 5);

      assertEquals(
          sym1.hashCode(),
          sym2.hashCode(),
          "Equal FrameSymbol instances should have same hashCode");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return '<unknown>' when all fields are null")
    void toStringShouldReturnUnknownWhenAllFieldsNull() {
      final FrameSymbol symbol = new FrameSymbol(null, null, null, null);

      assertEquals("<unknown>", symbol.toString(), "toString should return '<unknown>'");
    }

    @Test
    @DisplayName("toString should format as 'name at file:line:column' when all present")
    void toStringShouldFormatFullyWhenAllPresent() {
      final FrameSymbol symbol = new FrameSymbol("main", "test.c", 42, 10);

      final String result = symbol.toString();

      assertEquals("main at test.c:42:10", result, "toString should format with all components");
    }

    @Test
    @DisplayName("toString should format with name and file:line when column is null")
    void toStringShouldFormatWithoutColumnWhenNull() {
      final FrameSymbol symbol = new FrameSymbol("main", "test.c", 42, null);

      final String result = symbol.toString();

      assertEquals("main at test.c:42", result, "toString should omit column when null");
    }

    @Test
    @DisplayName("toString should format with name only when file and line are null")
    void toStringShouldFormatNameOnlyWhenFileAndLineNull() {
      final FrameSymbol symbol = new FrameSymbol("main", null, null, null);

      assertEquals("main", symbol.toString(), "toString should show only name");
    }

    @Test
    @DisplayName("toString should format with file when name is null but file is present")
    void toStringShouldFormatWithFileWhenNameNull() {
      final FrameSymbol symbol = new FrameSymbol(null, "test.c", null, null);

      final String result = symbol.toString();

      assertTrue(result.contains("test.c"), "toString should contain file when name is null");
    }

    @Test
    @DisplayName("toString should format with line when name and file null but line present")
    void toStringShouldFormatWithLineWhenNameAndFileNull() {
      final FrameSymbol symbol = new FrameSymbol(null, null, 42, null);

      final String result = symbol.toString();

      assertTrue(result.contains(":42"), "toString should contain line number");
    }

    @Test
    @DisplayName("toString should include file and line:column when name is null")
    void toStringShouldIncludeFileAndLineColumnWhenNameNull() {
      final FrameSymbol symbol = new FrameSymbol(null, "src.c", 10, 3);

      final String result = symbol.toString();

      assertTrue(result.contains("src.c"), "toString should contain file");
      assertTrue(result.contains(":10:3"), "toString should contain line:column");
    }
  }
}
