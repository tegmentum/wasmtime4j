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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentItemKind}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentItemKind")
class ComponentItemKindTest {

  @Test
  @DisplayName("has all expected values")
  void hasAllValues() {
    assertEquals(7, ComponentItemKind.values().length);
  }

  @Test
  @DisplayName("valueOf roundtrips all values")
  void valueOfRoundtrips() {
    for (final ComponentItemKind kind : ComponentItemKind.values()) {
      assertEquals(kind, ComponentItemKind.valueOf(kind.name()));
    }
  }

  @Test
  @DisplayName("contains expected variants")
  void containsExpectedVariants() {
    ComponentItemKind.valueOf("COMPONENT_FUNC");
    ComponentItemKind.valueOf("CORE_FUNC");
    ComponentItemKind.valueOf("MODULE");
    ComponentItemKind.valueOf("COMPONENT");
    ComponentItemKind.valueOf("COMPONENT_INSTANCE");
    ComponentItemKind.valueOf("TYPE");
    ComponentItemKind.valueOf("RESOURCE");
  }
}
