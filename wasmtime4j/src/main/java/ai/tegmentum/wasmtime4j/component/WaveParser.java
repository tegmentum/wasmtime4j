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
package ai.tegmentum.wasmtime4j.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Parser for the WAVE (WebAssembly Value Encoding) text format.
 *
 * <p>WAVE is a human-oriented text encoding of WebAssembly Component Model values, designed to be
 * consistent with the WIT IDL format. This parser implements type-directed parsing using a {@link
 * ComponentTypeDescriptor} for disambiguation.
 *
 * @since 1.1.0
 */
final class WaveParser {

  private final String input;
  private int pos;

  private WaveParser(final String input) {
    this.input = input;
    this.pos = 0;
  }

  /**
   * Parses a WAVE-encoded string into a ComponentVal.
   *
   * @param wave the WAVE-encoded string
   * @param type the expected type descriptor
   * @return the parsed ComponentVal
   * @throws IllegalArgumentException if the string cannot be parsed
   */
  static ComponentVal parse(final String wave, final ComponentTypeDescriptor type) {
    final WaveParser parser = new WaveParser(wave.trim());
    final ComponentVal result = parser.parseValue(type);
    parser.skipWhitespace();
    if (parser.pos < parser.input.length()) {
      throw new IllegalArgumentException(
          "Unexpected trailing content at position "
              + parser.pos
              + ": '"
              + parser.input.substring(parser.pos)
              + "'");
    }
    return result;
  }

  private ComponentVal parseValue(final ComponentTypeDescriptor type) {
    skipWhitespace();
    final ComponentType componentType = type.getType();
    final ComponentValFactory factory = ComponentValFactory.INSTANCE;

    switch (componentType) {
      case BOOL:
        return parseBool(factory);
      case S8:
        return factory.createS8((byte) parseLong());
      case S16:
        return factory.createS16((short) parseLong());
      case S32:
        return factory.createS32((int) parseLong());
      case S64:
        return factory.createS64(parseLong());
      case U8:
        return factory.createU8((short) parseLong());
      case U16:
        return factory.createU16((int) parseLong());
      case U32:
        return factory.createU32(parseLong());
      case U64:
        return factory.createU64(parseLong());
      case F32:
        return factory.createF32((float) parseDouble());
      case F64:
        return factory.createF64(parseDouble());
      case CHAR:
        return factory.createChar(parseChar());
      case STRING:
        return factory.createString(parseString());
      case LIST:
        return parseList(type, factory);
      case RECORD:
        return parseRecord(type, factory);
      case TUPLE:
        return parseTuple(type, factory);
      case VARIANT:
        return parseVariant(type, factory);
      case ENUM:
        return parseEnum(factory);
      case OPTION:
        return parseOption(type, factory);
      case RESULT:
        return parseResult(type, factory);
      case FLAGS:
        return parseFlags(factory);
      default:
        throw new UnsupportedOperationException(
            "WAVE parsing not supported for type: " + componentType);
    }
  }

  private ComponentVal parseBool(final ComponentValFactory factory) {
    if (tryConsume("true")) {
      return factory.createBool(true);
    }
    if (tryConsume("false")) {
      return factory.createBool(false);
    }
    throw parseError("Expected 'true' or 'false'");
  }

  private long parseLong() {
    skipWhitespace();
    final int start = pos;
    boolean negative = false;

    if (pos < input.length() && input.charAt(pos) == '-') {
      negative = true;
      pos++;
    } else if (pos < input.length() && input.charAt(pos) == '+') {
      pos++;
    }

    if (pos + 1 < input.length()
        && input.charAt(pos) == '0'
        && (input.charAt(pos + 1) == 'x' || input.charAt(pos + 1) == 'X')) {
      pos += 2;
      final int hexStart = pos;
      while (pos < input.length() && isHexDigit(input.charAt(pos))) {
        pos++;
      }
      if (pos == hexStart) {
        throw parseError("Expected hex digits after '0x'");
      }
      final long value = Long.parseUnsignedLong(input.substring(hexStart, pos), 16);
      return negative ? -value : value;
    }

    while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
      pos++;
    }

    if (pos == start || (negative && pos == start + 1)) {
      throw parseError("Expected integer literal");
    }

    return Long.parseLong(input.substring(start, pos));
  }

  private double parseDouble() {
    skipWhitespace();

    if (tryConsume("nan")) {
      return Double.NaN;
    }
    if (tryConsume("inf")) {
      return Double.POSITIVE_INFINITY;
    }
    if (tryConsume("-inf")) {
      return Double.NEGATIVE_INFINITY;
    }

    final int start = pos;
    if (pos < input.length() && (input.charAt(pos) == '-' || input.charAt(pos) == '+')) {
      pos++;
    }
    while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
      pos++;
    }
    if (pos < input.length() && input.charAt(pos) == '.') {
      pos++;
      while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
        pos++;
      }
    }
    if (pos < input.length() && (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {
      pos++;
      if (pos < input.length() && (input.charAt(pos) == '-' || input.charAt(pos) == '+')) {
        pos++;
      }
      while (pos < input.length() && Character.isDigit(input.charAt(pos))) {
        pos++;
      }
    }

    if (pos == start) {
      throw parseError("Expected float literal");
    }

    return Double.parseDouble(input.substring(start, pos));
  }

  private char parseChar() {
    expect('\'');
    final char ch;
    if (peek() == '\\') {
      pos++;
      ch = parseEscapeChar();
    } else {
      ch = input.charAt(pos++);
    }
    expect('\'');
    return ch;
  }

  private String parseString() {
    expect('"');
    final StringBuilder sb = new StringBuilder();
    while (pos < input.length() && input.charAt(pos) != '"') {
      if (input.charAt(pos) == '\\') {
        pos++;
        sb.append(parseEscapeChar());
      } else {
        sb.append(input.charAt(pos++));
      }
    }
    expect('"');
    return sb.toString();
  }

  private char parseEscapeChar() {
    if (pos >= input.length()) {
      throw parseError("Unexpected end of input in escape sequence");
    }
    final char c = input.charAt(pos++);
    switch (c) {
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 't':
        return '\t';
      case '\\':
        return '\\';
      case '\'':
        return '\'';
      case '"':
        return '"';
      case 'u':
        expect('{');
        final int hexStart = pos;
        while (pos < input.length() && input.charAt(pos) != '}') {
          pos++;
        }
        final int codePoint = Integer.parseInt(input.substring(hexStart, pos), 16);
        expect('}');
        return (char) codePoint;
      default:
        throw parseError("Unknown escape sequence: \\" + c);
    }
  }

  private ComponentVal parseList(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    expect('[');
    final List<ComponentVal> elements = new ArrayList<>();
    skipWhitespace();
    if (peek() != ']') {
      elements.add(parseValue(type.getElementType()));
      skipWhitespace();
      while (tryConsume(",")) {
        elements.add(parseValue(type.getElementType()));
        skipWhitespace();
      }
    }
    expect(']');
    return factory.createList(elements);
  }

  private ComponentVal parseRecord(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    expect('{');
    final Map<String, ComponentVal> fields = new LinkedHashMap<>();
    final Map<String, ComponentTypeDescriptor> fieldTypes = type.getRecordFields();
    skipWhitespace();
    if (peek() != '}') {
      parseRecordField(fields, fieldTypes);
      skipWhitespace();
      while (tryConsume(",")) {
        parseRecordField(fields, fieldTypes);
        skipWhitespace();
      }
    }
    expect('}');
    return factory.createRecord(fields);
  }

  private void parseRecordField(
      final Map<String, ComponentVal> fields,
      final Map<String, ComponentTypeDescriptor> fieldTypes) {
    skipWhitespace();
    final String name = parseIdentifier();
    skipWhitespace();
    expect(':');
    final ComponentTypeDescriptor fieldType = fieldTypes.get(name);
    if (fieldType == null) {
      throw parseError("Unknown record field: " + name);
    }
    fields.put(name, parseValue(fieldType));
  }

  private ComponentVal parseTuple(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    expect('(');
    final List<ComponentVal> elements = new ArrayList<>();
    final List<ComponentTypeDescriptor> elementTypes = type.getTupleElements();
    skipWhitespace();
    if (peek() != ')') {
      int idx = 0;
      elements.add(parseValue(elementTypes.get(idx++)));
      skipWhitespace();
      while (tryConsume(",")) {
        elements.add(parseValue(elementTypes.get(idx++)));
        skipWhitespace();
      }
    }
    expect(')');
    return factory.createTuple(elements);
  }

  private ComponentVal parseVariant(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    expect('%');
    final String caseName = parseIdentifier();
    final Map<String, Optional<ComponentTypeDescriptor>> cases = type.getVariantCases();
    final Optional<ComponentTypeDescriptor> payloadType = cases.get(caseName);
    if (payloadType == null) {
      throw parseError("Unknown variant case: " + caseName);
    }
    skipWhitespace();
    if (payloadType.isPresent() && peek() == '(') {
      expect('(');
      final ComponentVal payload = parseValue(payloadType.get());
      expect(')');
      return factory.createVariant(caseName, payload);
    }
    return factory.createVariant(caseName, null);
  }

  private ComponentVal parseEnum(final ComponentValFactory factory) {
    expect('%');
    final String caseName = parseIdentifier();
    return factory.createEnum(caseName);
  }

  private ComponentVal parseOption(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    if (tryConsume("none")) {
      return factory.createNone();
    }
    if (tryConsume("some(")) {
      final ComponentVal value = parseValue(type.getOptionType());
      expect(')');
      return factory.createSome(value);
    }
    throw parseError("Expected 'some(...)' or 'none'");
  }

  private ComponentVal parseResult(
      final ComponentTypeDescriptor type, final ComponentValFactory factory) {
    if (tryConsume("ok(")) {
      final Optional<ComponentTypeDescriptor> okType = type.getResultOkType();
      final ComponentVal value = okType.isPresent() ? parseValue(okType.get()) : null;
      expect(')');
      return factory.createOk(value);
    }
    if (tryConsume("ok")) {
      return factory.createOk(null);
    }
    if (tryConsume("err(")) {
      final Optional<ComponentTypeDescriptor> errType = type.getResultErrType();
      final ComponentVal value = errType.isPresent() ? parseValue(errType.get()) : null;
      expect(')');
      return factory.createErr(value);
    }
    if (tryConsume("err")) {
      return factory.createErr(null);
    }
    throw parseError("Expected 'ok(...)' or 'err(...)'");
  }

  private ComponentVal parseFlags(final ComponentValFactory factory) {
    expect('{');
    final Set<String> flags = new LinkedHashSet<>();
    skipWhitespace();
    if (peek() != '}') {
      flags.add(parseIdentifier());
      skipWhitespace();
      while (tryConsume(",")) {
        flags.add(parseIdentifier());
        skipWhitespace();
      }
    }
    expect('}');
    return factory.createFlags(flags);
  }

  private String parseIdentifier() {
    skipWhitespace();
    final int start = pos;
    while (pos < input.length()
        && (Character.isLetterOrDigit(input.charAt(pos))
            || input.charAt(pos) == '-'
            || input.charAt(pos) == '_')) {
      pos++;
    }
    if (pos == start) {
      throw parseError("Expected identifier");
    }
    return input.substring(start, pos);
  }

  private void skipWhitespace() {
    while (pos < input.length() && Character.isWhitespace(input.charAt(pos))) {
      pos++;
    }
  }

  private char peek() {
    if (pos >= input.length()) {
      return '\0';
    }
    return input.charAt(pos);
  }

  private void expect(final char ch) {
    skipWhitespace();
    if (pos >= input.length() || input.charAt(pos) != ch) {
      throw parseError("Expected '" + ch + "'");
    }
    pos++;
  }

  private boolean tryConsume(final String token) {
    skipWhitespace();
    if (input.startsWith(token, pos)) {
      pos += token.length();
      return true;
    }
    return false;
  }

  private static boolean isHexDigit(final char c) {
    return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
  }

  private IllegalArgumentException parseError(final String message) {
    final String context =
        pos < input.length()
            ? "'" + input.substring(pos, Math.min(pos + 20, input.length())) + "'"
            : "end of input";
    return new IllegalArgumentException(
        "WAVE parse error at position " + pos + " near " + context + ": " + message);
  }
}
