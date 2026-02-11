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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.config.ConfigProperties;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConfigProperties} class.
 *
 * <p>ConfigProperties is a type-safe configuration properties container supporting string, int,
 * long, double, and boolean access with defaults, immutability, and functional with/without methods.
 */
@DisplayName("ConfigProperties Tests")
class ConfigPropertiesTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create from Map")
    void shouldCreateFromMap() {
      final Map<String, String> map = Map.of("key1", "value1", "key2", "value2");
      final ConfigProperties props = new ConfigProperties(map);

      assertNotNull(props, "ConfigProperties should be created from map");
      assertEquals(2, props.size(), "Size should be 2");
    }

    @Test
    @DisplayName("should create from Java Properties")
    void shouldCreateFromJavaProperties() {
      final Properties javaProps = new Properties();
      javaProps.setProperty("foo", "bar");
      javaProps.setProperty("baz", "42");
      final ConfigProperties props = new ConfigProperties(javaProps);

      assertEquals(2, props.size(), "Size should be 2");
      assertEquals("bar", props.getString("foo", "default"), "Should read property 'foo'");
    }

    @Test
    @DisplayName("should reject null map")
    void shouldRejectNullMap() {
      assertThrows(
          NullPointerException.class,
          () -> new ConfigProperties((Map<String, String>) null),
          "Null map should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null Properties")
    void shouldRejectNullProperties() {
      assertThrows(
          NullPointerException.class,
          () -> new ConfigProperties((Properties) null),
          "Null Properties should throw NullPointerException");
    }

    @Test
    @DisplayName("should create defensive copy of input map")
    void shouldCreateDefensiveCopy() {
      final Map<String, String> mutable = new HashMap<>();
      mutable.put("key", "value");
      final ConfigProperties props = new ConfigProperties(mutable);
      mutable.put("key2", "value2");

      assertEquals(1, props.size(), "Should not reflect mutations in original map");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("empty() should return empty config")
    void emptyShouldReturnEmptyConfig() {
      final ConfigProperties props = ConfigProperties.empty();

      assertNotNull(props, "empty() should return non-null");
      assertTrue(props.isEmpty(), "empty() config should be empty");
      assertEquals(0, props.size(), "empty() config should have size 0");
    }

    @Test
    @DisplayName("fromSystemProperties should filter by prefix")
    void fromSystemPropertiesShouldFilterByPrefix() {
      final String prefix = "wasmtime4j.test.configprops.";
      System.setProperty(prefix + "testkey", "testvalue");
      try {
        final ConfigProperties props = ConfigProperties.fromSystemProperties(prefix);
        assertTrue(
            props.containsKey(prefix + "testkey"),
            "Should contain the system property with matching prefix");
      } finally {
        System.clearProperty(prefix + "testkey");
      }
    }
  }

  @Nested
  @DisplayName("String Access Tests")
  class StringAccessTests {

    @Test
    @DisplayName("getString should return Optional of value when present")
    void getStringShouldReturnValueWhenPresent() {
      final ConfigProperties props = new ConfigProperties(Map.of("key", "hello"));
      final Optional<String> result = props.getString("key");

      assertTrue(result.isPresent(), "getString should return present Optional");
      assertEquals("hello", result.get(), "getString value should be 'hello'");
    }

    @Test
    @DisplayName("getString should return empty Optional when missing")
    void getStringShouldReturnEmptyWhenMissing() {
      final ConfigProperties props = ConfigProperties.empty();
      final Optional<String> result = props.getString("missing");

      assertFalse(result.isPresent(), "getString should return empty Optional for missing key");
    }

    @Test
    @DisplayName("getString with default should return default when missing")
    void getStringWithDefaultShouldReturnDefaultWhenMissing() {
      final ConfigProperties props = ConfigProperties.empty();
      final String result = props.getString("missing", "fallback");

      assertEquals("fallback", result, "Should return default value 'fallback'");
    }
  }

  @Nested
  @DisplayName("Integer Access Tests")
  class IntegerAccessTests {

    @Test
    @DisplayName("getInt should parse valid integer")
    void getIntShouldParseValidInteger() {
      final ConfigProperties props = new ConfigProperties(Map.of("count", "42"));
      final Optional<Integer> result = props.getInt("count");

      assertTrue(result.isPresent(), "getInt should parse '42' successfully");
      assertEquals(42, result.get(), "getInt should return 42");
    }

    @Test
    @DisplayName("getInt should return empty for invalid integer")
    void getIntShouldReturnEmptyForInvalid() {
      final ConfigProperties props = new ConfigProperties(Map.of("bad", "not-a-number"));
      final Optional<Integer> result = props.getInt("bad");

      assertFalse(result.isPresent(), "getInt should return empty for non-numeric value");
    }

    @Test
    @DisplayName("getInt with default should return default for missing key")
    void getIntWithDefaultShouldReturnDefault() {
      final ConfigProperties props = ConfigProperties.empty();
      final int result = props.getInt("missing", 99);

      assertEquals(99, result, "getInt should return default value 99");
    }
  }

  @Nested
  @DisplayName("Long Access Tests")
  class LongAccessTests {

    @Test
    @DisplayName("getLong should parse valid long")
    void getLongShouldParseValidLong() {
      final ConfigProperties props =
          new ConfigProperties(Map.of("bignum", "9999999999"));
      final Optional<Long> result = props.getLong("bignum");

      assertTrue(result.isPresent(), "getLong should parse large number");
      assertEquals(9999999999L, result.get(), "getLong should return 9999999999");
    }

    @Test
    @DisplayName("getLong with default should return default for invalid")
    void getLongWithDefaultShouldReturnDefault() {
      final ConfigProperties props = new ConfigProperties(Map.of("bad", "xyz"));
      final long result = props.getLong("bad", 100L);

      assertEquals(100L, result, "getLong should return default for invalid input");
    }
  }

  @Nested
  @DisplayName("Double Access Tests")
  class DoubleAccessTests {

    @Test
    @DisplayName("getDouble should parse valid double")
    void getDoubleShouldParseValidDouble() {
      final ConfigProperties props = new ConfigProperties(Map.of("ratio", "3.14"));
      final Optional<Double> result = props.getDouble("ratio");

      assertTrue(result.isPresent(), "getDouble should parse '3.14'");
      assertEquals(3.14, result.get(), 0.001, "getDouble should return 3.14");
    }

    @Test
    @DisplayName("getDouble with default should return default for missing")
    void getDoubleWithDefaultShouldReturnDefault() {
      final ConfigProperties props = ConfigProperties.empty();
      final double result = props.getDouble("missing", 1.5);

      assertEquals(1.5, result, 0.001, "getDouble should return default 1.5");
    }
  }

  @Nested
  @DisplayName("Boolean Access Tests")
  class BooleanAccessTests {

    @Test
    @DisplayName("getBoolean should parse 'true'")
    void getBooleanShouldParseTrue() {
      final ConfigProperties props = new ConfigProperties(Map.of("flag", "true"));
      final Optional<Boolean> result = props.getBoolean("flag");

      assertTrue(result.isPresent(), "getBoolean should parse 'true'");
      assertTrue(result.get(), "getBoolean should return true for 'true'");
    }

    @Test
    @DisplayName("getBoolean should parse 'yes' as true")
    void getBooleanShouldParseYes() {
      final ConfigProperties props = new ConfigProperties(Map.of("flag", "YES"));
      final Optional<Boolean> result = props.getBoolean("flag");

      assertTrue(result.isPresent(), "getBoolean should parse 'YES'");
      assertTrue(result.get(), "getBoolean should return true for 'YES'");
    }

    @Test
    @DisplayName("getBoolean should parse '1' as true")
    void getBooleanShouldParseOne() {
      final ConfigProperties props = new ConfigProperties(Map.of("flag", "1"));
      final Optional<Boolean> result = props.getBoolean("flag");

      assertTrue(result.isPresent(), "getBoolean should parse '1'");
      assertTrue(result.get(), "getBoolean should return true for '1'");
    }

    @Test
    @DisplayName("getBoolean should parse 'false' as false")
    void getBooleanShouldParseFalse() {
      final ConfigProperties props = new ConfigProperties(Map.of("flag", "false"));
      final Optional<Boolean> result = props.getBoolean("flag");

      assertTrue(result.isPresent(), "getBoolean should parse 'false'");
      assertFalse(result.get(), "getBoolean should return false for 'false'");
    }

    @Test
    @DisplayName("getBoolean with default should return default for missing")
    void getBooleanWithDefaultShouldReturnDefault() {
      final ConfigProperties props = ConfigProperties.empty();
      final boolean result = props.getBoolean("missing", true);

      assertTrue(result, "getBoolean should return default true");
    }
  }

  @Nested
  @DisplayName("Collection Operation Tests")
  class CollectionOperationTests {

    @Test
    @DisplayName("containsKey should return true for existing key")
    void containsKeyShouldReturnTrueForExistingKey() {
      final ConfigProperties props = new ConfigProperties(Map.of("key", "val"));

      assertTrue(props.containsKey("key"), "containsKey should return true for existing key");
    }

    @Test
    @DisplayName("containsKey should return false for missing key")
    void containsKeyShouldReturnFalseForMissingKey() {
      final ConfigProperties props = ConfigProperties.empty();

      assertFalse(props.containsKey("missing"), "containsKey should return false for missing key");
    }

    @Test
    @DisplayName("keySet should return all keys")
    void keySetShouldReturnAllKeys() {
      final ConfigProperties props = new ConfigProperties(Map.of("a", "1", "b", "2"));

      assertEquals(2, props.keySet().size(), "keySet should have 2 entries");
      assertTrue(props.keySet().contains("a"), "keySet should contain 'a'");
      assertTrue(props.keySet().contains("b"), "keySet should contain 'b'");
    }

    @Test
    @DisplayName("toMap should return immutable map of properties")
    void toMapShouldReturnMap() {
      final ConfigProperties props = new ConfigProperties(Map.of("x", "y"));
      final Map<String, String> map = props.toMap();

      assertNotNull(map, "toMap should not return null");
      assertEquals("y", map.get("x"), "toMap should contain the property");
    }
  }

  @Nested
  @DisplayName("Immutable Transformation Tests")
  class ImmutableTransformationTests {

    @Test
    @DisplayName("with() should return new ConfigProperties with added property")
    void withShouldAddProperty() {
      final ConfigProperties original = new ConfigProperties(Map.of("a", "1"));
      final ConfigProperties modified = original.with("b", "2");

      assertEquals(1, original.size(), "Original should remain unchanged");
      assertEquals(2, modified.size(), "Modified should have 2 entries");
      assertTrue(modified.containsKey("b"), "Modified should contain the new key");
    }

    @Test
    @DisplayName("without() should return new ConfigProperties without the key")
    void withoutShouldRemoveProperty() {
      final ConfigProperties original = new ConfigProperties(Map.of("a", "1", "b", "2"));
      final ConfigProperties modified = original.without("b");

      assertEquals(2, original.size(), "Original should remain unchanged");
      assertEquals(1, modified.size(), "Modified should have 1 entry");
      assertFalse(modified.containsKey("b"), "Modified should not contain the removed key");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal ConfigProperties should be equal")
    void equalConfigPropertiesShouldBeEqual() {
      final ConfigProperties props1 = new ConfigProperties(Map.of("k", "v"));
      final ConfigProperties props2 = new ConfigProperties(Map.of("k", "v"));

      assertEquals(props1, props2, "Same content should be equal");
      assertEquals(props1.hashCode(), props2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("different ConfigProperties should not be equal")
    void differentConfigPropertiesShouldNotBeEqual() {
      final ConfigProperties props1 = new ConfigProperties(Map.of("k", "v1"));
      final ConfigProperties props2 = new ConfigProperties(Map.of("k", "v2"));

      assertNotEquals(props1, props2, "Different content should not be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include size")
    void toStringShouldIncludeSize() {
      final ConfigProperties props = new ConfigProperties(Map.of("a", "1", "b", "2"));
      final String result = props.toString();

      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("2"), "toString should include size");
      assertTrue(
          result.contains("ConfigProperties"),
          "toString should contain class name");
    }
  }
}
