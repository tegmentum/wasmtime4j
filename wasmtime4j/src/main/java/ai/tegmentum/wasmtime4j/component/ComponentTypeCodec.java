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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Codec for deserializing component type JSON produced by the native layer into {@link
 * ComponentTypeInfo} with full {@link ComponentItemInfo} type descriptors.
 *
 * <p>This codec uses a manual recursive-descent JSON parser (no external JSON library) matching the
 * pattern used in {@link ConcurrentCallCodec}.
 *
 * <p>Expected JSON format:
 *
 * <pre>{@code
 * {
 *   "imports": {"name": {"kind":"component_func", "params":[...], "results":[...]}},
 *   "exports": {"name": {"kind":"component_instance", "exports":{...}}}
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ComponentTypeCodec {

  /** Private constructor to prevent instantiation. */
  private ComponentTypeCodec() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Deserializes a full component type JSON string into a {@link ComponentTypeInfo}.
   *
   * @param json the JSON string from native layer
   * @return the deserialized ComponentTypeInfo with full type information
   * @throws IllegalArgumentException if the JSON is malformed
   */
  public static ComponentTypeInfo deserialize(final String json) {
    final JsonParser parser = new JsonParser(json);
    final Map<String, Object> root = parser.parseObject();

    @SuppressWarnings("unchecked")
    final Map<String, Object> importsRaw =
        (Map<String, Object>) root.getOrDefault("imports", Collections.emptyMap());
    @SuppressWarnings("unchecked")
    final Map<String, Object> exportsRaw =
        (Map<String, Object>) root.getOrDefault("exports", Collections.emptyMap());

    final Map<String, ComponentItemInfo> importItems = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : importsRaw.entrySet()) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> itemObj = (Map<String, Object>) entry.getValue();
      importItems.put(entry.getKey(), parseComponentItem(itemObj));
    }

    final Map<String, ComponentItemInfo> exportItems = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : exportsRaw.entrySet()) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> itemObj = (Map<String, Object>) entry.getValue();
      exportItems.put(entry.getKey(), parseComponentItem(itemObj));
    }

    return new ComponentTypeInfo(importItems, exportItems);
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo parseComponentItem(final Map<String, Object> obj) {
    final String kind = (String) obj.get("kind");
    if (kind == null) {
      throw new IllegalArgumentException("Missing 'kind' in component item");
    }

    switch (kind) {
      case "component_func":
        return parseComponentFunc(obj);
      case "core_func":
        return parseCoreFunc(obj);
      case "module":
        return new ComponentItemInfo.ModuleInfo(null);
      case "component":
        return parseComponent(obj);
      case "component_instance":
        return parseComponentInstance(obj);
      case "type":
        return parseTypeItem(obj);
      case "resource":
        return new ComponentItemInfo.ResourceInfo(null, 0);
      case "truncated":
        return new ComponentItemInfo.ModuleInfo(null);
      default:
        throw new IllegalArgumentException("Unknown component item kind: " + kind);
    }
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo.ComponentFuncInfo parseComponentFunc(
      final Map<String, Object> obj) {
    final List<Object> paramsRaw =
        (List<Object>) obj.getOrDefault("params", Collections.emptyList());
    final List<Object> resultsRaw =
        (List<Object>) obj.getOrDefault("results", Collections.emptyList());

    final List<ComponentItemInfo.NamedType> params = new ArrayList<>();
    for (final Object paramObj : paramsRaw) {
      final Map<String, Object> paramMap = (Map<String, Object>) paramObj;
      final String name = (String) paramMap.get("name");
      final ComponentTypeDescriptor type = parseTypeDescriptor(paramMap.get("type"));
      params.add(new ComponentItemInfo.NamedType(name, type));
    }

    final List<ComponentTypeDescriptor> results = new ArrayList<>();
    for (final Object resultObj : resultsRaw) {
      results.add(parseTypeDescriptor(resultObj));
    }

    return new ComponentItemInfo.ComponentFuncInfo(params, results, false);
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo.CoreFuncInfo parseCoreFunc(final Map<String, Object> obj) {
    final List<Object> paramsRaw =
        (List<Object>) obj.getOrDefault("params", Collections.emptyList());
    final List<Object> resultsRaw =
        (List<Object>) obj.getOrDefault("results", Collections.emptyList());

    final List<String> params = new ArrayList<>();
    for (final Object p : paramsRaw) {
      params.add(String.valueOf(p));
    }

    final List<String> results = new ArrayList<>();
    for (final Object r : resultsRaw) {
      results.add(String.valueOf(r));
    }

    return new ComponentItemInfo.CoreFuncInfo(params, results);
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo.ComponentInfo parseComponent(final Map<String, Object> obj) {
    final Map<String, Object> importsRaw =
        (Map<String, Object>) obj.getOrDefault("imports", Collections.emptyMap());
    final Map<String, Object> exportsRaw =
        (Map<String, Object>) obj.getOrDefault("exports", Collections.emptyMap());

    final Map<String, ComponentItemInfo> imports = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : importsRaw.entrySet()) {
      imports.put(entry.getKey(), parseComponentItem((Map<String, Object>) entry.getValue()));
    }

    final Map<String, ComponentItemInfo> exports = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : exportsRaw.entrySet()) {
      exports.put(entry.getKey(), parseComponentItem((Map<String, Object>) entry.getValue()));
    }

    return new ComponentItemInfo.ComponentInfo(imports, exports);
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo.ComponentInstanceInfo parseComponentInstance(
      final Map<String, Object> obj) {
    final Map<String, Object> exportsRaw =
        (Map<String, Object>) obj.getOrDefault("exports", Collections.emptyMap());

    final Map<String, ComponentItemInfo> exports = new LinkedHashMap<>();
    for (final Map.Entry<String, Object> entry : exportsRaw.entrySet()) {
      exports.put(entry.getKey(), parseComponentItem((Map<String, Object>) entry.getValue()));
    }

    return new ComponentItemInfo.ComponentInstanceInfo(exports);
  }

  @SuppressWarnings("unchecked")
  private static ComponentItemInfo.TypeInfo parseTypeItem(final Map<String, Object> obj) {
    final Object descriptorRaw = obj.get("descriptor");
    final ComponentTypeDescriptor descriptor = parseTypeDescriptor(descriptorRaw);
    return new ComponentItemInfo.TypeInfo(descriptor);
  }

  @SuppressWarnings("unchecked")
  private static ComponentTypeDescriptor parseTypeDescriptor(final Object raw) {
    if (raw instanceof String) {
      return parsePrimitiveType((String) raw);
    }
    if (raw instanceof Map) {
      return parseCompoundType((Map<String, Object>) raw);
    }
    throw new IllegalArgumentException("Unexpected type descriptor format: " + raw);
  }

  private static ComponentTypeDescriptor parsePrimitiveType(final String typeStr) {
    switch (typeStr) {
      case "bool":
        return ComponentTypeDescriptor.bool();
      case "s8":
        return ComponentTypeDescriptor.s8();
      case "u8":
        return ComponentTypeDescriptor.u8();
      case "s16":
        return ComponentTypeDescriptor.s16();
      case "u16":
        return ComponentTypeDescriptor.u16();
      case "s32":
        return ComponentTypeDescriptor.s32();
      case "u32":
        return ComponentTypeDescriptor.u32();
      case "s64":
        return ComponentTypeDescriptor.s64();
      case "u64":
        return ComponentTypeDescriptor.u64();
      case "f32":
        return ComponentTypeDescriptor.f32();
      case "f64":
        return ComponentTypeDescriptor.f64();
      case "char":
        return ComponentTypeDescriptor.char_();
      case "string":
        return ComponentTypeDescriptor.string();
      default:
        // Treat unknown string types as primitives
        return new ComponentTypeDescriptor.PrimitiveImpl(ComponentType.STRING, typeStr);
    }
  }

  @SuppressWarnings("unchecked")
  private static ComponentTypeDescriptor parseCompoundType(final Map<String, Object> obj) {
    final String type = (String) obj.get("type");
    if (type == null) {
      throw new IllegalArgumentException("Missing 'type' in compound type descriptor");
    }

    switch (type) {
      case "list":
        return ComponentTypeDescriptor.list(parseTypeDescriptor(obj.get("element")));

      case "record":
        {
          final List<Object> fieldsRaw =
              (List<Object>) obj.getOrDefault("fields", Collections.emptyList());
          final Map<String, ComponentTypeDescriptor> fields = new LinkedHashMap<>();
          for (final Object fieldObj : fieldsRaw) {
            final Map<String, Object> field = (Map<String, Object>) fieldObj;
            fields.put((String) field.get("name"), parseTypeDescriptor(field.get("type")));
          }
          return ComponentTypeDescriptor.record(fields);
        }

      case "tuple":
        {
          final List<Object> elementsRaw =
              (List<Object>) obj.getOrDefault("elements", Collections.emptyList());
          final List<ComponentTypeDescriptor> elements = new ArrayList<>();
          for (final Object elem : elementsRaw) {
            elements.add(parseTypeDescriptor(elem));
          }
          return ComponentTypeDescriptor.tuple(elements);
        }

      case "variant":
        {
          final List<Object> casesRaw =
              (List<Object>) obj.getOrDefault("cases", Collections.emptyList());
          final Map<String, Optional<ComponentTypeDescriptor>> cases = new LinkedHashMap<>();
          for (final Object caseObj : casesRaw) {
            final Map<String, Object> caseMap = (Map<String, Object>) caseObj;
            final String name = (String) caseMap.get("name");
            final Optional<ComponentTypeDescriptor> payload =
                caseMap.containsKey("type")
                    ? Optional.of(parseTypeDescriptor(caseMap.get("type")))
                    : Optional.empty();
            cases.put(name, payload);
          }
          return ComponentTypeDescriptor.variant(cases);
        }

      case "enum":
        {
          final List<Object> namesRaw =
              (List<Object>) obj.getOrDefault("names", Collections.emptyList());
          final List<String> names = new ArrayList<>();
          for (final Object n : namesRaw) {
            names.add((String) n);
          }
          return ComponentTypeDescriptor.enum_(names);
        }

      case "option":
        return ComponentTypeDescriptor.option(parseTypeDescriptor(obj.get("inner")));

      case "result":
        {
          final ComponentTypeDescriptor okType =
              obj.containsKey("ok") ? parseTypeDescriptor(obj.get("ok")) : null;
          final ComponentTypeDescriptor errType =
              obj.containsKey("err") ? parseTypeDescriptor(obj.get("err")) : null;
          return ComponentTypeDescriptor.result(okType, errType);
        }

      case "flags":
        {
          final List<Object> namesRaw =
              (List<Object>) obj.getOrDefault("names", Collections.emptyList());
          final List<String> names = new ArrayList<>();
          for (final Object n : namesRaw) {
            names.add((String) n);
          }
          return ComponentTypeDescriptor.flags(names);
        }

      case "own":
        return ComponentTypeDescriptor.own("resource", 0);

      case "borrow":
        return ComponentTypeDescriptor.borrow("resource", 0);

      case "future":
        {
          final ComponentTypeDescriptor payload =
              obj.containsKey("payload") ? parseTypeDescriptor(obj.get("payload")) : null;
          return ComponentTypeDescriptor.future(payload);
        }

      case "stream":
        {
          final ComponentTypeDescriptor payload =
              obj.containsKey("payload") ? parseTypeDescriptor(obj.get("payload")) : null;
          return ComponentTypeDescriptor.stream(payload);
        }

      case "error_context":
        return ComponentTypeDescriptor.errorContext();

      default:
        throw new IllegalArgumentException("Unknown compound type: " + type);
    }
  }

  /** Recursive-descent JSON parser (same pattern as {@link ConcurrentCallCodec.JsonParser}). */
  static final class JsonParser {
    private final String json;
    private int pos;

    JsonParser(final String json) {
      this.json = json;
      this.pos = 0;
    }

    Map<String, Object> parseObject() {
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
