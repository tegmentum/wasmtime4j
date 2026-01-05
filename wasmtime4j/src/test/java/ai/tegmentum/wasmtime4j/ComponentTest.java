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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Component} interface.
 *
 * <p>Component is the core WebAssembly Component Model interface that extends ComponentSimple with
 * enterprise management capabilities.
 */
@DisplayName("Component Tests")
class ComponentTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(Component.class.getModifiers()), "Component should be public");
      assertTrue(Component.class.isInterface(), "Component should be an interface");
    }

    @Test
    @DisplayName("should extend ComponentSimple")
    void shouldExtendComponentSimple() {
      assertTrue(
          ComponentSimple.class.isAssignableFrom(Component.class),
          "Component should extend ComponentSimple");
    }
  }

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should inherit methods from ComponentSimple")
    void shouldInheritMethodsFromComponentSimple() {
      // Component extends ComponentSimple, so it should have all methods from ComponentSimple
      // We verify by checking that Component is a subtype of ComponentSimple
      final Class<?>[] interfaces = Component.class.getInterfaces();
      boolean extendsComponentSimple = false;
      for (Class<?> iface : interfaces) {
        if (iface == ComponentSimple.class) {
          extendsComponentSimple = true;
          break;
        }
      }
      assertTrue(
          extendsComponentSimple, "Component should directly extend ComponentSimple interface");
    }
  }
}
