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
 * Serializes and deserializes {@link ConcurrentCall} and {@link ComponentVal} to/from JSON.
 *
 * <p>The JSON format matches the Rust {@code concurrent_call_json} module's serde format, using
 * tagged enums with {@code "type"} and {@code "value"} fields. This codec is used by both JNI and
 * Panama implementations to marshal concurrent call data across the FFI boundary.
 *
 * <p>Example JSON for a call list:
 *
 * <pre>{@code
 * [{"name":"add","args":[{"type":"S32","value":1},{"type":"S32","value":2}]}]
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ConcurrentCallCodec {

  private ConcurrentCallCodec() {}

  /**
   * Serializes a list of concurrent calls to JSON.
   *
   * @param calls the concurrent calls to serialize
   * @return the JSON string
   * @throws IllegalArgumentException if calls is null
   */
  public static String serializeCalls(final List<ConcurrentCall> calls) {
    if (calls == null) {
      throw new IllegalArgumentException("calls cannot be null");
    }
    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < calls.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      final ConcurrentCall call = calls.get(i);
      sb.append("{\"name\":");
      appendJsonString(sb, call.getFunctionName());
      sb.append(",\"args\":[");
      final List<ComponentVal> args = call.getArgs();
      for (int j = 0; j < args.size(); j++) {
        if (j > 0) {
          sb.append(',');
        }
        serializeVal(sb, args.get(j));
      }
      sb.append("]}");
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Deserializes concurrent call results from JSON.
   *
   * <p>The expected format is a JSON array of arrays, where each inner array contains the result
   * values for one call.
   *
   * @param json the JSON string to deserialize
   * @return a list of result lists
   * @throws IllegalArgumentException if json is null or malformed
   */
  public static List<List<ComponentVal>> deserializeResults(final String json) {
    if (json == null) {
      throw new IllegalArgumentException("json cannot be null");
    }
    final JsonParser parser = new JsonParser(json);
    return parser.parseResultsArray();
  }

  /**
   * Serializes a list of component values to a JSON array string.
   *
   * <p>This is used by Panama FFI to marshal host function callback results across the boundary.
   *
   * @param vals the component values to serialize
   * @return the JSON array string
   * @throws IllegalArgumentException if vals is null
   */
  public static String serializeVals(final List<ComponentVal> vals) {
    if (vals == null) {
      throw new IllegalArgumentException("vals cannot be null");
    }
    final StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < vals.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      serializeVal(sb, vals.get(i));
    }
    sb.append(']');
    return sb.toString();
  }

  /**
   * Deserializes a JSON array string to a list of component values.
   *
   * <p>This is used by Panama FFI to unmarshal host function callback parameters.
   *
   * @param json the JSON array string to deserialize
   * @return the list of component values
   * @throws IllegalArgumentException if json is null or malformed
   */
  public static List<ComponentVal> deserializeVals(final String json) {
    if (json == null) {
      throw new IllegalArgumentException("json cannot be null");
    }
    final JsonParser parser = new JsonParser(json);
    return parser.parseValArray();
  }

  private static void serializeVal(final StringBuilder sb, final ComponentVal val) {
    final ComponentType type = val.getType();
    switch (type) {
      case BOOL:
        sb.append("{\"type\":\"Bool\",\"value\":").append(val.asBool()).append('}');
        break;
      case S8:
        sb.append("{\"type\":\"S8\",\"value\":").append(val.asS8()).append('}');
        break;
      case S16:
        sb.append("{\"type\":\"S16\",\"value\":").append(val.asS16()).append('}');
        break;
      case S32:
        sb.append("{\"type\":\"S32\",\"value\":").append(val.asS32()).append('}');
        break;
      case S64:
        sb.append("{\"type\":\"S64\",\"value\":").append(val.asS64()).append('}');
        break;
      case U8:
        sb.append("{\"type\":\"U8\",\"value\":").append(val.asU8()).append('}');
        break;
      case U16:
        sb.append("{\"type\":\"U16\",\"value\":").append(val.asU16()).append('}');
        break;
      case U32:
        sb.append("{\"type\":\"U32\",\"value\":").append(val.asU32()).append('}');
        break;
      case U64:
        sb.append("{\"type\":\"U64\",\"value\":").append(val.asU64()).append('}');
        break;
      case F32:
        sb.append("{\"type\":\"Float32\",\"value\":").append(val.asF32()).append('}');
        break;
      case F64:
        sb.append("{\"type\":\"Float64\",\"value\":").append(val.asF64()).append('}');
        break;
      case CHAR:
        sb.append("{\"type\":\"Char\",\"value\":");
        appendJsonString(sb, String.valueOf(val.asChar()));
        sb.append('}');
        break;
      case STRING:
        sb.append("{\"type\":\"String\",\"value\":");
        appendJsonString(sb, val.asString());
        sb.append('}');
        break;
      case LIST:
        sb.append("{\"type\":\"List\",\"value\":[");
        final List<ComponentVal> listItems = val.asList();
        for (int i = 0; i < listItems.size(); i++) {
          if (i > 0) {
            sb.append(',');
          }
          serializeVal(sb, listItems.get(i));
        }
        sb.append("]}");
        break;
      case RECORD:
        sb.append("{\"type\":\"Record\",\"value\":[");
        final Map<String, ComponentVal> fields = val.asRecord();
        int fieldIdx = 0;
        for (final Map.Entry<String, ComponentVal> entry : fields.entrySet()) {
          if (fieldIdx > 0) {
            sb.append(',');
          }
          sb.append('[');
          appendJsonString(sb, entry.getKey());
          sb.append(',');
          serializeVal(sb, entry.getValue());
          sb.append(']');
          fieldIdx++;
        }
        sb.append("]}");
        break;
      case TUPLE:
        sb.append("{\"type\":\"Tuple\",\"value\":[");
        final List<ComponentVal> tupleItems = val.asTuple();
        for (int i = 0; i < tupleItems.size(); i++) {
          if (i > 0) {
            sb.append(',');
          }
          serializeVal(sb, tupleItems.get(i));
        }
        sb.append("]}");
        break;
      case VARIANT:
        final ComponentVariant variant = val.asVariant();
        sb.append("{\"type\":\"Variant\",\"value\":{\"discriminant\":");
        appendJsonString(sb, variant.getCaseName());
        sb.append(",\"value\":");
        if (variant.hasPayload()) {
          serializeVal(sb, variant.getPayload().get());
        } else {
          sb.append("null");
        }
        sb.append("}}");
        break;
      case ENUM:
        sb.append("{\"type\":\"Enum\",\"value\":");
        appendJsonString(sb, val.asEnum());
        sb.append('}');
        break;
      case OPTION:
        final Optional<ComponentVal> optVal = val.asSome();
        if (optVal.isPresent()) {
          sb.append("{\"type\":\"Option\",\"value\":");
          serializeVal(sb, optVal.get());
          sb.append('}');
        } else {
          sb.append("{\"type\":\"Option\",\"value\":null}");
        }
        break;
      case RESULT:
        final ComponentResult result = val.asResult();
        sb.append("{\"type\":\"Result\",\"value\":{\"ok\":");
        if (result.isOk()) {
          final Optional<ComponentVal> okInner = result.getOk();
          if (okInner.isPresent()) {
            serializeVal(sb, okInner.get());
          } else {
            sb.append("null");
          }
          sb.append(",\"err\":null,\"is_ok\":true}}");
        } else {
          sb.append("null,\"err\":");
          final Optional<ComponentVal> errInner = result.getErr();
          if (errInner.isPresent()) {
            serializeVal(sb, errInner.get());
          } else {
            sb.append("null");
          }
          sb.append(",\"is_ok\":false}}");
        }
        break;
      case FLAGS:
        final Set<String> flagSet = val.asFlags();
        sb.append("{\"type\":\"Flags\",\"value\":[");
        int flagIdx = 0;
        for (final String flag : flagSet) {
          if (flagIdx > 0) {
            sb.append(',');
          }
          appendJsonString(sb, flag);
          flagIdx++;
        }
        sb.append("]}");
        break;
      default:
        throw new IllegalArgumentException(
            "Cannot serialize ComponentVal type for concurrent calls: " + type);
    }
  }

  private static void appendJsonString(final StringBuilder sb, final String value) {
    sb.append('"');
    for (int i = 0; i < value.length(); i++) {
      final char c = value.charAt(i);
      switch (c) {
        case '"':
          sb.append("\\\"");
          break;
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        default:
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
  }

  /**
   * Minimal JSON parser for deserializing concurrent call results.
   *
   * <p>Parses the specific JSON format produced by the Rust {@code serialize_results} function:
   * arrays of arrays of tagged JsonVal objects.
   */
  static final class JsonParser {
    private final String json;
    private int pos;

    JsonParser(final String json) {
      this.json = json;
      this.pos = 0;
    }

    List<List<ComponentVal>> parseResultsArray() {
      skipWhitespace();
      expect('[');
      final List<List<ComponentVal>> results = new ArrayList<>();
      skipWhitespace();
      if (peek() != ']') {
        results.add(parseValArray());
        skipWhitespace();
        while (peek() == ',') {
          advance();
          results.add(parseValArray());
          skipWhitespace();
        }
      }
      expect(']');
      return results;
    }

    private List<ComponentVal> parseValArray() {
      skipWhitespace();
      expect('[');
      final List<ComponentVal> vals = new ArrayList<>();
      skipWhitespace();
      if (peek() != ']') {
        vals.add(parseVal());
        skipWhitespace();
        while (peek() == ',') {
          advance();
          vals.add(parseVal());
          skipWhitespace();
        }
      }
      expect(']');
      return vals;
    }

    private ComponentVal parseVal() {
      skipWhitespace();
      expect('{');
      // Parse {"type":"...", "value":...}
      String typeStr = null;
      Object value = null;
      boolean valueParsed = false;

      skipWhitespace();
      while (peek() != '}') {
        if (typeStr != null || valueParsed) {
          expect(',');
          skipWhitespace();
        }
        final String key = parseString();
        skipWhitespace();
        expect(':');
        skipWhitespace();
        if ("type".equals(key)) {
          typeStr = parseString();
        } else if ("value".equals(key)) {
          // Defer parsing value until we know the type
          // But we need to parse it now to advance position
          value = parseRawValue();
          valueParsed = true;
        } else {
          // Unknown key - skip value
          parseRawValue();
        }
        skipWhitespace();
      }
      expect('}');

      if (typeStr == null) {
        throw new IllegalArgumentException("Missing 'type' field in JSON val at position " + pos);
      }

      return constructVal(typeStr, value);
    }

    @SuppressWarnings("unchecked")
    private ComponentVal constructVal(final String typeStr, final Object rawValue) {
      switch (typeStr) {
        case "Bool":
          return ComponentVal.bool((Boolean) rawValue);
        case "S8":
          return ComponentVal.s8(((Number) rawValue).byteValue());
        case "S16":
          return ComponentVal.s16(((Number) rawValue).shortValue());
        case "S32":
          return ComponentVal.s32(((Number) rawValue).intValue());
        case "S64":
          return ComponentVal.s64(((Number) rawValue).longValue());
        case "U8":
          return ComponentVal.u8(((Number) rawValue).shortValue());
        case "U16":
          return ComponentVal.u16(((Number) rawValue).intValue());
        case "U32":
          return ComponentVal.u32(((Number) rawValue).longValue());
        case "U64":
          return ComponentVal.u64(((Number) rawValue).longValue());
        case "Float32":
          return ComponentVal.f32(((Number) rawValue).floatValue());
        case "Float64":
          return ComponentVal.f64(((Number) rawValue).doubleValue());
        case "Char":
          {
            final String charStr = (String) rawValue;
            if (charStr.isEmpty()) {
              throw new IllegalArgumentException("Empty char value in JSON");
            }
            return ComponentVal.char_(charStr.charAt(0));
          }
        case "String":
          return ComponentVal.string((String) rawValue);
        case "List":
          {
            final List<Object> items = (List<Object>) rawValue;
            final List<ComponentVal> vals = new ArrayList<>(items.size());
            for (final Object item : items) {
              vals.add(reconstructVal(item));
            }
            return ComponentVal.list(vals);
          }
        case "Record":
          {
            final List<Object> fieldPairs = (List<Object>) rawValue;
            final Map<String, ComponentVal> fields = new LinkedHashMap<>();
            for (final Object pair : fieldPairs) {
              final List<Object> pairList = (List<Object>) pair;
              final String fieldName = (String) pairList.get(0);
              final ComponentVal fieldVal = reconstructVal(pairList.get(1));
              fields.put(fieldName, fieldVal);
            }
            return ComponentVal.record(fields);
          }
        case "Tuple":
          {
            final List<Object> items = (List<Object>) rawValue;
            final List<ComponentVal> vals = new ArrayList<>(items.size());
            for (final Object item : items) {
              vals.add(reconstructVal(item));
            }
            return ComponentVal.tuple(vals.toArray(new ComponentVal[0]));
          }
        case "Variant":
          {
            final Map<String, Object> variantMap = (Map<String, Object>) rawValue;
            final String discriminant = (String) variantMap.get("discriminant");
            final Object payload = variantMap.get("value");
            if (payload == null) {
              return ComponentVal.variant(discriminant);
            }
            return ComponentVal.variant(discriminant, reconstructVal(payload));
          }
        case "Enum":
          return ComponentVal.enum_((String) rawValue);
        case "Option":
          {
            if (rawValue == null) {
              return ComponentVal.none();
            }
            return ComponentVal.some(reconstructVal(rawValue));
          }
        case "Result":
          {
            final Map<String, Object> resultMap = (Map<String, Object>) rawValue;
            final Boolean isOk = (Boolean) resultMap.get("is_ok");
            if (isOk != null && isOk) {
              final Object okVal = resultMap.get("ok");
              if (okVal == null) {
                return ComponentVal.ok();
              }
              return ComponentVal.ok(reconstructVal(okVal));
            } else {
              final Object errVal = resultMap.get("err");
              if (errVal == null) {
                return ComponentVal.err();
              }
              return ComponentVal.err(reconstructVal(errVal));
            }
          }
        case "Flags":
          {
            final List<Object> flagNames = (List<Object>) rawValue;
            final Set<String> flags = new LinkedHashSet<>();
            for (final Object flag : flagNames) {
              flags.add((String) flag);
            }
            return ComponentVal.flags(flags);
          }
        default:
          throw new IllegalArgumentException("Unknown JsonVal type: " + typeStr);
      }
    }

    @SuppressWarnings("unchecked")
    private ComponentVal reconstructVal(final Object raw) {
      if (raw instanceof Map) {
        final Map<String, Object> map = (Map<String, Object>) raw;
        final String typeStr = (String) map.get("type");
        final Object value = map.get("value");
        return constructVal(typeStr, value);
      }
      throw new IllegalArgumentException("Expected JSON object for ComponentVal, got: " + raw);
    }

    private Object parseRawValue() {
      skipWhitespace();
      final char c = peek();
      if (c == '"') {
        return parseString();
      } else if (c == '{') {
        return parseObject();
      } else if (c == '[') {
        return parseArray();
      } else if (c == 't' || c == 'f') {
        return parseBoolean();
      } else if (c == 'n') {
        return parseNull();
      } else {
        return parseNumber();
      }
    }

    private String parseString() {
      skipWhitespace();
      expect('"');
      final StringBuilder sb = new StringBuilder();
      while (peek() != '"') {
        final char c = advance();
        if (c == '\\') {
          final char escaped = advance();
          switch (escaped) {
            case '"':
              sb.append('"');
              break;
            case '\\':
              sb.append('\\');
              break;
            case '/':
              sb.append('/');
              break;
            case 'b':
              sb.append('\b');
              break;
            case 'f':
              sb.append('\f');
              break;
            case 'n':
              sb.append('\n');
              break;
            case 'r':
              sb.append('\r');
              break;
            case 't':
              sb.append('\t');
              break;
            case 'u':
              final String hex = json.substring(pos, pos + 4);
              sb.append((char) Integer.parseInt(hex, 16));
              pos += 4;
              break;
            default:
              sb.append(escaped);
          }
        } else {
          sb.append(c);
        }
      }
      expect('"');
      return sb.toString();
    }

    private Map<String, Object> parseObject() {
      skipWhitespace();
      expect('{');
      final Map<String, Object> map = new LinkedHashMap<>();
      skipWhitespace();
      if (peek() != '}') {
        final String key = parseString();
        skipWhitespace();
        expect(':');
        map.put(key, parseRawValue());
        skipWhitespace();
        while (peek() == ',') {
          advance();
          skipWhitespace();
          final String nextKey = parseString();
          skipWhitespace();
          expect(':');
          map.put(nextKey, parseRawValue());
          skipWhitespace();
        }
      }
      expect('}');
      return map;
    }

    private List<Object> parseArray() {
      skipWhitespace();
      expect('[');
      final List<Object> list = new ArrayList<>();
      skipWhitespace();
      if (peek() != ']') {
        list.add(parseRawValue());
        skipWhitespace();
        while (peek() == ',') {
          advance();
          list.add(parseRawValue());
          skipWhitespace();
        }
      }
      expect(']');
      return list;
    }

    private Boolean parseBoolean() {
      if (json.startsWith("true", pos)) {
        pos += 4;
        return Boolean.TRUE;
      } else if (json.startsWith("false", pos)) {
        pos += 5;
        return Boolean.FALSE;
      }
      throw new IllegalArgumentException("Expected boolean at position " + pos);
    }

    private Object parseNull() {
      if (json.startsWith("null", pos)) {
        pos += 4;
        return null;
      }
      throw new IllegalArgumentException("Expected null at position " + pos);
    }

    private Number parseNumber() {
      skipWhitespace();
      final int start = pos;
      boolean isFloat = false;
      if (peek() == '-') {
        advance();
      }
      while (pos < json.length()) {
        final char c = json.charAt(pos);
        if (c == '.' || c == 'e' || c == 'E') {
          isFloat = true;
          pos++;
        } else if (c == '+' || c == '-' || (c >= '0' && c <= '9')) {
          pos++;
        } else {
          break;
        }
      }
      final String numStr = json.substring(start, pos);
      if (isFloat) {
        return Double.parseDouble(numStr);
      }
      final long longVal = Long.parseLong(numStr);
      if (longVal >= Integer.MIN_VALUE && longVal <= Integer.MAX_VALUE) {
        return (int) longVal;
      }
      return longVal;
    }

    private void skipWhitespace() {
      while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
        pos++;
      }
    }

    private char peek() {
      if (pos >= json.length()) {
        throw new IllegalArgumentException("Unexpected end of JSON at position " + pos);
      }
      return json.charAt(pos);
    }

    private char advance() {
      if (pos >= json.length()) {
        throw new IllegalArgumentException("Unexpected end of JSON at position " + pos);
      }
      return json.charAt(pos++);
    }

    private void expect(final char expected) {
      skipWhitespace();
      final char actual = advance();
      if (actual != expected) {
        throw new IllegalArgumentException(
            "Expected '" + expected + "' but got '" + actual + "' at position " + (pos - 1));
      }
    }
  }
}
