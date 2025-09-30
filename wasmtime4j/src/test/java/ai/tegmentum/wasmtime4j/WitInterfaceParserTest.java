/*
 * Copyright 2024 Tegmentum AI
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Comprehensive unit tests for WIT interface parser. */
class WitInterfaceParserTest {

  private WitInterfaceParser parser;

  @BeforeEach
  void setUp() {
    parser = new WitInterfaceParser();
  }

  @Test
  @DisplayName("Test simple interface parsing")
  void testSimpleInterfaceParsing() throws WasmException {
    final String witText =
        """
        interface calculator {
            add: func(a: s32, b: s32) -> s32;
            subtract: func(a: s32, b: s32) -> s32;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "math");

    assertEquals("calculator", definition.getName());
    assertEquals("math", definition.getPackageName());
    assertEquals("1.0", definition.getVersion());

    final var functionNames = definition.getFunctionNames();
    assertEquals(2, functionNames.size());
    assertTrue(functionNames.contains("add"));
    assertTrue(functionNames.contains("subtract"));

    final var exportNames = definition.getExportNames();
    assertEquals(2, exportNames.size());
    assertTrue(exportNames.contains("add"));
    assertTrue(exportNames.contains("subtract"));
  }

  @Test
  @DisplayName("Test interface with types parsing")
  void testInterfaceWithTypesParsing() throws WasmException {
    final String witText =
        """
        interface shapes {
            type point = record {
                x: f64,
                y: f64
            };

            type color = enum {
                red,
                green,
                blue
            };

            create-point: func(x: f64, y: f64) -> point;
            get-distance: func(p1: point, p2: point) -> f64;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "graphics");

    assertEquals("shapes", definition.getName());
    assertEquals("graphics", definition.getPackageName());

    final var typeNames = definition.getTypeNames();
    assertEquals(2, typeNames.size());
    assertTrue(typeNames.contains("point"));
    assertTrue(typeNames.contains("color"));

    final var functionNames = definition.getFunctionNames();
    assertEquals(2, functionNames.size());
    assertTrue(functionNames.contains("create-point"));
    assertTrue(functionNames.contains("get-distance"));
  }

  @Test
  @DisplayName("Test complex types parsing")
  void testComplexTypesParsing() throws WasmException {
    final String witText =
        """
        interface advanced {
            type result-type = result<string, s32>;
            type optional-value = option<f64>;
            type string-list = list<string>;
            type permissions = flags {
                read,
                write,
                execute
            };

            process-data: func(input: string-list, perms: permissions) -> result-type;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "advanced");

    assertEquals("advanced", definition.getName());

    final var typeNames = definition.getTypeNames();
    assertEquals(4, typeNames.size());
    assertTrue(typeNames.contains("result-type"));
    assertTrue(typeNames.contains("optional-value"));
    assertTrue(typeNames.contains("string-list"));
    assertTrue(typeNames.contains("permissions"));

    final var functionNames = definition.getFunctionNames();
    assertEquals(1, functionNames.size());
    assertTrue(functionNames.contains("process-data"));
  }

  @Test
  @DisplayName("Test variant type parsing")
  void testVariantTypeParsing() throws WasmException {
    final String witText =
        """
        interface messaging {
            type message = variant {
                text(string),
                binary(list<u8>),
                empty
            };

            send-message: func(msg: message);
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "messaging");

    assertEquals("messaging", definition.getName());

    final var typeNames = definition.getTypeNames();
    assertTrue(typeNames.contains("message"));

    final var functionNames = definition.getFunctionNames();
    assertTrue(functionNames.contains("send-message"));
  }

  @Test
  @DisplayName("Test function with no parameters")
  void testFunctionWithNoParameters() throws WasmException {
    final String witText =
        """
        interface service {
            get-status: func() -> string;
            reset: func();
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "service");

    assertEquals("service", definition.getName());

    final var functionNames = definition.getFunctionNames();
    assertEquals(2, functionNames.size());
    assertTrue(functionNames.contains("get-status"));
    assertTrue(functionNames.contains("reset"));
  }

  @Test
  @DisplayName("Test function with no return type")
  void testFunctionWithNoReturnType() throws WasmException {
    final String witText =
        """
        interface logger {
            log-message: func(level: string, message: string);
            clear-log: func();
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "logging");

    assertEquals("logger", definition.getName());

    final var functionNames = definition.getFunctionNames();
    assertEquals(2, functionNames.size());
    assertTrue(functionNames.contains("log-message"));
    assertTrue(functionNames.contains("clear-log"));
  }

  @Test
  @DisplayName("Test nested record types")
  void testNestedRecordTypes() throws WasmException {
    final String witText =
        """
        interface nested {
            type address = record {
                street: string,
                city: string,
                zip: string
            };

            type person = record {
                name: string,
                age: u32,
                address: address
            };

            create-person: func(name: string, age: u32, addr: address) -> person;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "people");

    assertEquals("nested", definition.getName());

    final var typeNames = definition.getTypeNames();
    assertEquals(2, typeNames.size());
    assertTrue(typeNames.contains("address"));
    assertTrue(typeNames.contains("person"));
  }

  @Test
  @DisplayName("Test invalid interface format")
  void testInvalidInterfaceFormat() {
    final String invalidWitText = "this is not a valid WIT interface";

    assertThrows(WasmException.class, () -> parser.parseInterface(invalidWitText, "invalid"));
  }

  @Test
  @DisplayName("Test empty interface")
  void testEmptyInterface() throws WasmException {
    final String witText = """
        interface empty {
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "empty");

    assertEquals("empty", definition.getName());
    assertTrue(definition.getFunctionNames().isEmpty());
    assertTrue(definition.getTypeNames().isEmpty());
  }

  @Test
  @DisplayName("Test interface compatibility checking")
  void testInterfaceCompatibilityChecking() throws WasmException {
    final String witText1 =
        """
        interface math {
            add: func(a: s32, b: s32) -> s32;
        }
        """;

    final String witText2 =
        """
        interface math {
            add: func(a: s32, b: s32) -> s32;
        }
        """;

    final WitInterfaceDefinition def1 = parser.parseInterface(witText1, "math");
    final WitInterfaceDefinition def2 = parser.parseInterface(witText2, "math");

    final WitCompatibilityResult result = def1.isCompatibleWith(def2);
    assertTrue(result.isCompatible());
    assertEquals("Interfaces are compatible", result.getDetails());
  }

  @Test
  @DisplayName("Test interface incompatibility")
  void testInterfaceIncompatibility() throws WasmException {
    final String witText1 =
        """
        interface math {
            add: func(a: s32, b: s32) -> s32;
        }
        """;

    final String witText2 =
        """
        interface calculator {
            multiply: func(x: f64, y: f64) -> f64;
        }
        """;

    final WitInterfaceDefinition def1 = parser.parseInterface(witText1, "math");
    final WitInterfaceDefinition def2 = parser.parseInterface(witText2, "calc");

    final WitCompatibilityResult result = def1.isCompatibleWith(def2);
    assertFalse(result.isCompatible());
    assertEquals("Interfaces are not compatible", result.getDetails());
  }

  @Test
  @DisplayName("Test WIT text preservation")
  void testWitTextPreservation() throws WasmException {
    final String originalWitText =
        """
        interface test {
            hello: func() -> string;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(originalWitText, "test");
    assertEquals(originalWitText, definition.getWitText());
  }

  @Test
  @DisplayName("Test null parameter handling")
  void testNullParameterHandling() {
    assertThrows(NullPointerException.class, () -> parser.parseInterface(null, "test"));

    assertThrows(
        NullPointerException.class, () -> parser.parseInterface("interface test {}", null));
  }

  @Test
  @DisplayName("Test multiple primitive types")
  void testMultiplePrimitiveTypes() throws WasmException {
    final String witText =
        """
        interface primitives {
            test-bool: func(value: bool) -> bool;
            test-u8: func(value: u8) -> u8;
            test-s8: func(value: s8) -> s8;
            test-u16: func(value: u16) -> u16;
            test-s16: func(value: s16) -> s16;
            test-u32: func(value: u32) -> u32;
            test-s32: func(value: s32) -> s32;
            test-u64: func(value: u64) -> u64;
            test-s64: func(value: s64) -> s64;
            test-float32: func(value: float32) -> float32;
            test-float64: func(value: float64) -> float64;
            test-char: func(value: char) -> char;
            test-string: func(value: string) -> string;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "primitives");

    assertEquals("primitives", definition.getName());

    final var functionNames = definition.getFunctionNames();
    assertEquals(13, functionNames.size());

    // Verify all primitive type functions are present
    assertTrue(functionNames.contains("test-bool"));
    assertTrue(functionNames.contains("test-u8"));
    assertTrue(functionNames.contains("test-s8"));
    assertTrue(functionNames.contains("test-u16"));
    assertTrue(functionNames.contains("test-s16"));
    assertTrue(functionNames.contains("test-u32"));
    assertTrue(functionNames.contains("test-s32"));
    assertTrue(functionNames.contains("test-u64"));
    assertTrue(functionNames.contains("test-s64"));
    assertTrue(functionNames.contains("test-float32"));
    assertTrue(functionNames.contains("test-float64"));
    assertTrue(functionNames.contains("test-char"));
    assertTrue(functionNames.contains("test-string"));
  }

  @Test
  @DisplayName("Test complex function signatures")
  void testComplexFunctionSignatures() throws WasmException {
    final String witText =
        """
        interface complex {
            type data = record {
                values: list<s32>,
                metadata: option<string>
            };

            process: func(
                input: data,
                options: flags { verbose, debug, trace },
                callback: option<string>
            ) -> result<data, string>;
        }
        """;

    final WitInterfaceDefinition definition = parser.parseInterface(witText, "complex");

    assertEquals("complex", definition.getName());

    final var typeNames = definition.getTypeNames();
    assertTrue(typeNames.contains("data"));

    final var functionNames = definition.getFunctionNames();
    assertTrue(functionNames.contains("process"));
  }
}
