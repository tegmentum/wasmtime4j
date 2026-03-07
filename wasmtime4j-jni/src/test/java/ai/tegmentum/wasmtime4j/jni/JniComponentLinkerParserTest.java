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
package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the private {@code parseJsonStringArray} method in {@link JniComponentLinker}.
 *
 * <p>This method parses JSON arrays of strings returned by native FFI functions into Java Sets. It
 * is tested via reflection since it is a private static utility method.
 */
@DisplayName("JniComponentLinker parseJsonStringArray Tests")
class JniComponentLinkerParserTest {

  private static Method parseMethod;

  @BeforeAll
  static void setUp() throws NoSuchMethodException {
    parseMethod = JniComponentLinker.class.getDeclaredMethod("parseJsonStringArray", String.class);
    parseMethod.setAccessible(true);
  }

  @SuppressWarnings("unchecked")
  private static Set<String> parse(final String json) {
    try {
      return (Set<String>) parseMethod.invoke(null, json);
    } catch (final IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException("Failed to invoke parseJsonStringArray", e);
    }
  }

  @Nested
  @DisplayName("Empty and Null Inputs")
  class EmptyAndNullTests {

    @Test
    @DisplayName("should return empty set for null input")
    void shouldReturnEmptyForNull() {
      final Set<String> result = parse(null);
      assertTrue(result.isEmpty(), "null input should produce empty set");
    }

    @Test
    @DisplayName("should return empty set for empty string")
    void shouldReturnEmptyForEmptyString() {
      final Set<String> result = parse("");
      assertTrue(result.isEmpty(), "Empty string should produce empty set");
    }

    @Test
    @DisplayName("should return empty set for single character")
    void shouldReturnEmptyForSingleChar() {
      final Set<String> result = parse("[");
      assertTrue(result.isEmpty(), "Single char should produce empty set (length < 2)");
    }

    @Test
    @DisplayName("should return empty set for empty JSON array")
    void shouldReturnEmptyForEmptyArray() {
      final Set<String> result = parse("[]");
      assertTrue(result.isEmpty(), "Empty JSON array should produce empty set");
    }

    @Test
    @DisplayName("should return empty set for whitespace-only JSON array")
    void shouldReturnEmptyForWhitespaceArray() {
      final Set<String> result = parse("[  ]");
      assertTrue(result.isEmpty(), "Whitespace-only JSON array should produce empty set");
    }
  }

  @Nested
  @DisplayName("Single Element Arrays")
  class SingleElementTests {

    @Test
    @DisplayName("should parse single element array")
    void shouldParseSingleElement() {
      final Set<String> result = parse("[\"hello\"]");
      assertEquals(1, result.size(), "Should have 1 element");
      assertTrue(result.contains("hello"), "Should contain 'hello'");
    }

    @Test
    @DisplayName("should parse single element with special characters")
    void shouldParseSpecialCharacters() {
      final Set<String> result = parse("[\"wasi:cli/stdout@0.2.0\"]");
      assertEquals(1, result.size(), "Should have 1 element");
      assertTrue(
          result.contains("wasi:cli/stdout@0.2.0"), "Should contain 'wasi:cli/stdout@0.2.0'");
    }
  }

  @Nested
  @DisplayName("Multiple Element Arrays")
  class MultipleElementTests {

    @Test
    @DisplayName("should parse two elements")
    void shouldParseTwoElements() {
      final Set<String> result = parse("[\"foo\",\"bar\"]");
      assertEquals(2, result.size(), "Should have 2 elements");
      assertTrue(result.contains("foo"), "Should contain 'foo'");
      assertTrue(result.contains("bar"), "Should contain 'bar'");
    }

    @Test
    @DisplayName("should parse multiple elements with spaces")
    void shouldParseWithSpaces() {
      final Set<String> result = parse("[\"a\" , \"b\" , \"c\"]");
      assertEquals(3, result.size(), "Should have 3 elements");
      assertTrue(result.contains("a"), "Should contain 'a'");
      assertTrue(result.contains("b"), "Should contain 'b'");
      assertTrue(result.contains("c"), "Should contain 'c'");
    }

    @Test
    @DisplayName("should parse WIT-style interface names")
    void shouldParseWitInterfaceNames() {
      final Set<String> result =
          parse("[\"wasi:cli/stdout\",\"wasi:io/streams\",\"wasi:http/types\"]");
      assertEquals(3, result.size(), "Should have 3 interfaces");
      assertTrue(result.contains("wasi:cli/stdout"), "Should contain 'wasi:cli/stdout'");
      assertTrue(result.contains("wasi:io/streams"), "Should contain 'wasi:io/streams'");
      assertTrue(result.contains("wasi:http/types"), "Should contain 'wasi:http/types'");
    }

    @Test
    @DisplayName("should deduplicate identical elements")
    void shouldDeduplicateIdentical() {
      final Set<String> result = parse("[\"dup\",\"dup\",\"dup\"]");
      assertEquals(1, result.size(), "Duplicates should be deduplicated");
      assertTrue(result.contains("dup"), "Should contain 'dup'");
    }
  }

  @Nested
  @DisplayName("Escaped Quotes")
  class EscapedQuoteTests {

    @Test
    @DisplayName("should handle escaped quotes in values")
    void shouldHandleEscapedQuotes() {
      final Set<String> result = parse("[\"hello \\\"world\\\"\"]");
      assertEquals(1, result.size(), "Should have 1 element");
      assertTrue(
          result.contains("hello \"world\""), "Should contain unescaped value: hello \"world\"");
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle empty string element")
    void shouldHandleEmptyStringElement() {
      final Set<String> result = parse("[\"\"]");
      assertEquals(1, result.size(), "Should have 1 element");
      assertTrue(result.contains(""), "Should contain empty string");
    }

    @Test
    @DisplayName("should handle mix of empty and non-empty elements")
    void shouldHandleMixedElements() {
      final Set<String> result = parse("[\"\",\"notempty\",\"\"]");
      assertEquals(2, result.size(), "Should have 2 unique elements (empty + notempty)");
      assertTrue(result.contains(""), "Should contain empty string");
      assertTrue(result.contains("notempty"), "Should contain 'notempty'");
    }

    @Test
    @DisplayName("should handle large number of elements")
    void shouldHandleManyElements() {
      final StringBuilder json = new StringBuilder("[");
      for (int i = 0; i < 100; i++) {
        if (i > 0) {
          json.append(",");
        }
        json.append("\"item").append(i).append("\"");
      }
      json.append("]");

      final Set<String> result = parse(json.toString());
      assertEquals(100, result.size(), "Should have 100 elements");
      assertTrue(result.contains("item0"), "Should contain 'item0'");
      assertTrue(result.contains("item99"), "Should contain 'item99'");
    }
  }
}
