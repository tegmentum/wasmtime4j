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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyInjectionConfig} class.
 *
 * <p>DependencyInjectionConfig defines how dependencies should be resolved and injected when
 * linking components. Has a no-arg default constructor and a full constructor.
 */
@DisplayName("DependencyInjectionConfig Tests")
class DependencyInjectionConfigTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(DependencyInjectionConfig.class.getModifiers()),
          "DependencyInjectionConfig should be public");
      assertTrue(
          Modifier.isFinal(DependencyInjectionConfig.class.getModifiers()),
          "DependencyInjectionConfig should be final");
    }

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
      final var constructor = DependencyInjectionConfig.class.getConstructor();
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "No-arg constructor should be public");
    }

    @Test
    @DisplayName("should have public full constructor")
    void shouldHavePublicFullConstructor() throws NoSuchMethodException {
      final var constructor = DependencyInjectionConfig.class.getConstructor(
          boolean.class, boolean.class, Map.class);
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "Full constructor should be public");
    }
  }

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("should create config with default values")
    void shouldCreateWithDefaults() {
      final DependencyInjectionConfig config = new DependencyInjectionConfig();

      assertNotNull(config, "Config should not be null");
      assertTrue(
          config.isAutowiringEnabled(),
          "Default autowiring should be enabled");
      assertFalse(
          config.isLazyLoadingEnabled(),
          "Default lazy loading should be disabled");
      assertNotNull(
          config.getExplicitBindings(),
          "Default explicit bindings should not be null");
      assertTrue(
          config.getExplicitBindings().isEmpty(),
          "Default explicit bindings should be empty");
    }
  }

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("should create config with all parameters")
    void shouldCreateWithAllParameters() {
      final Map<String, Object> bindings = Map.of("service", "myService");
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(true, true, bindings);

      assertTrue(config.isAutowiringEnabled(), "autowiring should be true");
      assertTrue(config.isLazyLoadingEnabled(), "lazy loading should be true");
      assertEquals(
          1, config.getExplicitBindings().size(),
          "Explicit bindings should have 1 entry");
      assertEquals(
          "myService", config.getExplicitBindings().get("service"),
          "Binding value should be 'myService'");
    }

    @Test
    @DisplayName("should create config with autowiring disabled")
    void shouldCreateWithAutowiringDisabled() {
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(false, false, Map.of());

      assertFalse(config.isAutowiringEnabled(), "autowiring should be false");
      assertFalse(config.isLazyLoadingEnabled(), "lazy loading should be false");
    }

    @Test
    @DisplayName("should reject null explicitBindings")
    void shouldRejectNullExplicitBindings() {
      assertThrows(
          NullPointerException.class,
          () -> new DependencyInjectionConfig(true, false, null),
          "Null explicitBindings should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("getExplicitBindings should return immutable map")
    void getExplicitBindingsShouldReturnImmutableMap() {
      final Map<String, Object> bindings = Map.of("key", "value");
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(true, false, bindings);

      assertThrows(
          UnsupportedOperationException.class,
          () -> config.getExplicitBindings().put("newKey", "newValue"),
          "Returned bindings map should be immutable");
    }
  }

  @Nested
  @DisplayName("Various Configuration Combinations Tests")
  class ConfigurationCombinationsTests {

    @Test
    @DisplayName("should support autowiring enabled with lazy loading")
    void shouldSupportAutowiringWithLazy() {
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(true, true, Map.of());

      assertTrue(config.isAutowiringEnabled(), "Autowiring should be enabled");
      assertTrue(config.isLazyLoadingEnabled(), "Lazy loading should be enabled");
    }

    @Test
    @DisplayName("should support explicit bindings with autowiring disabled")
    void shouldSupportExplicitBindingsWithoutAutowiring() {
      final Map<String, Object> bindings = Map.of(
          "dbService", "PostgresDB",
          "cacheService", "Redis");
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(false, false, bindings);

      assertFalse(config.isAutowiringEnabled(), "Autowiring should be disabled");
      assertEquals(
          2, config.getExplicitBindings().size(),
          "Should have 2 explicit bindings");
      assertEquals(
          "PostgresDB", config.getExplicitBindings().get("dbService"),
          "dbService binding should be 'PostgresDB'");
    }

    @Test
    @DisplayName("should support empty bindings with lazy loading")
    void shouldSupportEmptyBindingsWithLazy() {
      final DependencyInjectionConfig config =
          new DependencyInjectionConfig(false, true, Map.of());

      assertFalse(config.isAutowiringEnabled(), "Autowiring should be disabled");
      assertTrue(config.isLazyLoadingEnabled(), "Lazy loading should be enabled");
      assertTrue(config.getExplicitBindings().isEmpty(), "Bindings should be empty");
    }
  }
}
