package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * Configuration properties container for WebAssembly runtime settings.
 *
 * <p>Provides a type-safe way to access configuration properties with default values, type
 * conversion, and validation.
 *
 * @since 1.0.0
 */
public final class ConfigProperties {

  private final Map<String, String> properties;

  /**
   * Creates configuration properties from a map.
   *
   * @param properties the properties map
   */
  public ConfigProperties(final Map<String, String> properties) {
    this.properties =
        Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(properties)));
  }

  /**
   * Creates configuration properties from Java Properties.
   *
   * @param properties the Java properties
   */
  public ConfigProperties(final Properties properties) {
    Objects.requireNonNull(properties);
    final Map<String, String> map = new HashMap<>();
    for (final String key : properties.stringPropertyNames()) {
      map.put(key, properties.getProperty(key));
    }
    this.properties = Collections.unmodifiableMap(map);
  }

  /**
   * Creates empty configuration properties.
   *
   * @return empty configuration
   */
  public static ConfigProperties empty() {
    return new ConfigProperties(Collections.emptyMap());
  }

  /**
   * Creates configuration properties from system properties.
   *
   * @param prefix the property name prefix to filter by
   * @return configuration with matching system properties
   */
  public static ConfigProperties fromSystemProperties(final String prefix) {
    final Map<String, String> map = new HashMap<>();
    final Properties systemProps = System.getProperties();

    for (final String key : systemProps.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        map.put(key, systemProps.getProperty(key));
      }
    }

    return new ConfigProperties(map);
  }

  /**
   * Gets a string property value.
   *
   * @param key the property key
   * @return the property value, or empty if not found
   */
  public Optional<String> getString(final String key) {
    return Optional.ofNullable(properties.get(key));
  }

  /**
   * Gets a string property value with a default.
   *
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value, or default if not found
   */
  public String getString(final String key, final String defaultValue) {
    return properties.getOrDefault(key, defaultValue);
  }

  /**
   * Gets an integer property value.
   *
   * @param key the property key
   * @return the property value as integer, or empty if not found or invalid
   */
  public Optional<Integer> getInt(final String key) {
    return getString(key)
        .map(
            value -> {
              try {
                return Integer.parseInt(value);
              } catch (final NumberFormatException e) {
                return null;
              }
            });
  }

  /**
   * Gets an integer property value with a default.
   *
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value as integer, or default if not found or invalid
   */
  public int getInt(final String key, final int defaultValue) {
    return getInt(key).orElse(defaultValue);
  }

  /**
   * Gets a long property value.
   *
   * @param key the property key
   * @return the property value as long, or empty if not found or invalid
   */
  public Optional<Long> getLong(final String key) {
    return getString(key)
        .map(
            value -> {
              try {
                return Long.parseLong(value);
              } catch (final NumberFormatException e) {
                return null;
              }
            });
  }

  /**
   * Gets a long property value with a default.
   *
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value as long, or default if not found or invalid
   */
  public long getLong(final String key, final long defaultValue) {
    return getLong(key).orElse(defaultValue);
  }

  /**
   * Gets a double property value.
   *
   * @param key the property key
   * @return the property value as double, or empty if not found or invalid
   */
  public Optional<Double> getDouble(final String key) {
    return getString(key)
        .map(
            value -> {
              try {
                return Double.parseDouble(value);
              } catch (final NumberFormatException e) {
                return null;
              }
            });
  }

  /**
   * Gets a double property value with a default.
   *
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value as double, or default if not found or invalid
   */
  public double getDouble(final String key, final double defaultValue) {
    return getDouble(key).orElse(defaultValue);
  }

  /**
   * Gets a boolean property value.
   *
   * @param key the property key
   * @return the property value as boolean, or empty if not found
   */
  public Optional<Boolean> getBoolean(final String key) {
    return getString(key)
        .map(
            value -> {
              final String lowerValue = value.toLowerCase(Locale.ROOT);
              return "true".equals(lowerValue)
                  || "yes".equals(lowerValue)
                  || "1".equals(value);
            });
  }

  /**
   * Gets a boolean property value with a default.
   *
   * @param key the property key
   * @param defaultValue the default value
   * @return the property value as boolean, or default if not found
   */
  public boolean getBoolean(final String key, final boolean defaultValue) {
    return getBoolean(key).orElse(defaultValue);
  }

  /**
   * Checks if a property exists.
   *
   * @param key the property key
   * @return true if the property exists, false otherwise
   */
  public boolean containsKey(final String key) {
    return properties.containsKey(key);
  }

  /**
   * Gets all property keys.
   *
   * @return set of all property keys
   */
  public java.util.Set<String> keySet() {
    return properties.keySet();
  }

  /**
   * Gets the number of properties.
   *
   * @return property count
   */
  public int size() {
    return properties.size();
  }

  /**
   * Checks if there are no properties.
   *
   * @return true if empty, false otherwise
   */
  public boolean isEmpty() {
    return properties.isEmpty();
  }

  /**
   * Creates a new ConfigProperties with an additional property.
   *
   * @param key the property key
   * @param value the property value
   * @return new ConfigProperties with the added property
   */
  public ConfigProperties with(final String key, final String value) {
    final Map<String, String> newProps = new HashMap<>(properties);
    newProps.put(key, value);
    return new ConfigProperties(newProps);
  }

  /**
   * Creates a new ConfigProperties without a specific property.
   *
   * @param key the property key to remove
   * @return new ConfigProperties without the specified property
   */
  public ConfigProperties without(final String key) {
    final Map<String, String> newProps = new HashMap<>(properties);
    newProps.remove(key);
    return new ConfigProperties(newProps);
  }

  /**
   * Converts to a Map.
   *
   * @return immutable map of all properties
   */
  public Map<String, String> toMap() {
    return properties;
  }

  @Override
  public String toString() {
    return "ConfigProperties{size=" + properties.size() + "}";
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ConfigProperties that = (ConfigProperties) obj;
    return Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(properties);
  }
}
