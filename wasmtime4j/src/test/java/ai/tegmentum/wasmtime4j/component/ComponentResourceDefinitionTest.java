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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentResourceDefinition}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentResourceDefinition")
class ComponentResourceDefinitionTest {

  @Nested
  @DisplayName("builder")
  class BuilderTests {

    @Test
    @DisplayName("creates resource with name only")
    void createsWithNameOnly() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("myResource").build();
      assertEquals("myResource", def.getName());
      assertFalse(def.getConstructor().isPresent());
      assertFalse(def.getDestructor().isPresent());
      assertTrue(def.getMethods().isEmpty());
    }

    @Test
    @DisplayName("creates resource with constructor")
    void createsWithConstructor() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("res")
              .constructor(params -> "created")
              .build();
      assertTrue(def.getConstructor().isPresent());
    }

    @Test
    @DisplayName("creates resource with supplier constructor")
    void createsWithSupplierConstructor() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("res").constructor(() -> "default").build();
      assertTrue(def.getConstructor().isPresent());
    }

    @Test
    @DisplayName("creates resource with destructor")
    void createsWithDestructor() {
      final AtomicBoolean destroyed = new AtomicBoolean(false);
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("res")
              .destructor(s -> destroyed.set(true))
              .build();
      assertTrue(def.getDestructor().isPresent());
      def.getDestructor().get().accept("test");
      assertTrue(destroyed.get());
    }

    @Test
    @DisplayName("creates resource with methods")
    void createsWithMethods() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("res")
              .method("read", (instance, params) -> List.of(ComponentVal.s32(42)))
              .method("write", (instance, params) -> Collections.emptyList())
              .build();
      assertEquals(2, def.getMethods().size());
      assertTrue(def.getMethod("read").isPresent());
      assertTrue(def.getMethod("write").isPresent());
      assertFalse(def.getMethod("nonexistent").isPresent());
    }

    @Test
    @DisplayName("rejects null resource name")
    void rejectsNullName() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentResourceDefinition.<String>builder(null));
    }

    @Test
    @DisplayName("rejects empty resource name")
    void rejectsEmptyName() {
      assertThrows(
          IllegalArgumentException.class, () -> ComponentResourceDefinition.<String>builder(""));
    }

    @Test
    @DisplayName("rejects null method name")
    void rejectsNullMethodName() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              ComponentResourceDefinition.<String>builder("res")
                  .method(null, (instance, params) -> Collections.emptyList()));
    }

    @Test
    @DisplayName("rejects empty method name")
    void rejectsEmptyMethodName() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              ComponentResourceDefinition.<String>builder("res")
                  .method("", (instance, params) -> Collections.emptyList()));
    }
  }

  @Nested
  @DisplayName("methods map is immutable")
  class MethodsImmutability {

    @Test
    @DisplayName("getMethods returns unmodifiable map")
    void methodsUnmodifiable() {
      final ComponentResourceDefinition<String> def =
          ComponentResourceDefinition.<String>builder("res")
              .method("read", (instance, params) -> Collections.emptyList())
              .build();
      assertThrows(
          UnsupportedOperationException.class,
          () -> def.getMethods().put("hack", (instance, params) -> Collections.emptyList()));
    }
  }
}
