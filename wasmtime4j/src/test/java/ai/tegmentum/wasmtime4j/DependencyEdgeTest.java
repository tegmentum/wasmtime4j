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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DependencyEdge} class.
 *
 * <p>DependencyEdge represents a dependency relationship between two WebAssembly modules, including
 * the dependent module, dependency module, import module/name, dependency type, and resolution
 * status.
 */
@DisplayName("DependencyEdge Tests")
class DependencyEdgeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(DependencyEdge.class.getModifiers()),
          "DependencyEdge should be public");
      assertTrue(
          Modifier.isFinal(DependencyEdge.class.getModifiers()),
          "DependencyEdge should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Validation Tests")
  class ConstructorValidationTests {

    @Test
    @DisplayName("should reject null dependent")
    void shouldRejectNullDependent() {
      final Module dep = createMockModule();
      assertThrows(
          NullPointerException.class,
          () -> new DependencyEdge(
              null, dep, "env", "func",
              DependencyEdge.DependencyType.FUNCTION, true),
          "Null dependent should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null dependency")
    void shouldRejectNullDependency() {
      final Module dep = createMockModule();
      assertThrows(
          NullPointerException.class,
          () -> new DependencyEdge(
              dep, null, "env", "func",
              DependencyEdge.DependencyType.FUNCTION, true),
          "Null dependency should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null importModule")
    void shouldRejectNullImportModule() {
      final Module dep = createMockModule();
      assertThrows(
          NullPointerException.class,
          () -> new DependencyEdge(
              dep, dep, null, "func",
              DependencyEdge.DependencyType.FUNCTION, true),
          "Null importModule should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null importName")
    void shouldRejectNullImportName() {
      final Module dep = createMockModule();
      assertThrows(
          NullPointerException.class,
          () -> new DependencyEdge(
              dep, dep, "env", null,
              DependencyEdge.DependencyType.FUNCTION, true),
          "Null importName should throw NullPointerException");
    }

    @Test
    @DisplayName("should reject null dependencyType")
    void shouldRejectNullDependencyType() {
      final Module dep = createMockModule();
      assertThrows(
          NullPointerException.class,
          () -> new DependencyEdge(
              dep, dep, "env", "func",
              null, true),
          "Null dependencyType should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("should return all fields correctly")
    void shouldReturnAllFields() {
      final Module dependent = createMockModule();
      final Module dependency = createMockModule();
      final DependencyEdge edge = new DependencyEdge(
          dependent, dependency, "env", "memory",
          DependencyEdge.DependencyType.MEMORY, true);

      assertNotNull(edge.getDependent(), "getDependent should not be null");
      assertNotNull(edge.getDependency(), "getDependency should not be null");
      assertEquals("env", edge.getImportModule(), "importModule should be 'env'");
      assertEquals("memory", edge.getImportName(), "importName should be 'memory'");
      assertEquals(
          DependencyEdge.DependencyType.MEMORY, edge.getDependencyType(),
          "dependencyType should be MEMORY");
      assertTrue(edge.isResolved(), "resolved should be true");
    }

    @Test
    @DisplayName("should return false for unresolved edge")
    void shouldReturnFalseForUnresolved() {
      final Module dependent = createMockModule();
      final Module dependency = createMockModule();
      final DependencyEdge edge = new DependencyEdge(
          dependent, dependency, "wasi", "fd_write",
          DependencyEdge.DependencyType.FUNCTION, false);

      assertFalse(edge.isResolved(), "resolved should be false for unresolved edge");
    }
  }

  @Nested
  @DisplayName("getDependencyString Tests")
  class GetDependencyStringTests {

    @Test
    @DisplayName("should format dependency string with import info")
    void shouldFormatDependencyString() {
      final Module dependent = createMockModule();
      final Module dependency = createMockModule();
      final DependencyEdge edge = new DependencyEdge(
          dependent, dependency, "env", "log",
          DependencyEdge.DependencyType.FUNCTION, true);

      final String result = edge.getDependencyString();
      assertNotNull(result, "getDependencyString should not return null");
      assertTrue(result.contains("env"), "Should contain import module name");
      assertTrue(result.contains("log"), "Should contain import name");
    }
  }

  @Nested
  @DisplayName("DependencyType Enum Tests")
  class DependencyTypeEnumTests {

    @Test
    @DisplayName("should have all expected dependency types")
    void shouldHaveAllExpectedTypes() {
      final DependencyEdge.DependencyType[] values =
          DependencyEdge.DependencyType.values();
      assertEquals(5, values.length, "DependencyType should have 5 values");
      assertNotNull(
          DependencyEdge.DependencyType.valueOf("FUNCTION"),
          "FUNCTION should exist");
      assertNotNull(
          DependencyEdge.DependencyType.valueOf("MEMORY"),
          "MEMORY should exist");
      assertNotNull(
          DependencyEdge.DependencyType.valueOf("TABLE"),
          "TABLE should exist");
      assertNotNull(
          DependencyEdge.DependencyType.valueOf("GLOBAL"),
          "GLOBAL should exist");
      assertNotNull(
          DependencyEdge.DependencyType.valueOf("INSTANCE"),
          "INSTANCE should exist");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equal edges should be equal")
    void equalEdgesShouldBeEqual() {
      final Module module = createMockModule();
      final DependencyEdge edge1 = new DependencyEdge(
          module, module, "env", "func",
          DependencyEdge.DependencyType.FUNCTION, true);
      final DependencyEdge edge2 = new DependencyEdge(
          module, module, "env", "func",
          DependencyEdge.DependencyType.FUNCTION, true);

      assertEquals(edge1, edge2, "Identical edges should be equal");
      assertEquals(
          edge1.hashCode(), edge2.hashCode(),
          "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("edges with different import names should not be equal")
    void differentImportNamesShouldNotBeEqual() {
      final Module module = createMockModule();
      final DependencyEdge edge1 = new DependencyEdge(
          module, module, "env", "funcA",
          DependencyEdge.DependencyType.FUNCTION, true);
      final DependencyEdge edge2 = new DependencyEdge(
          module, module, "env", "funcB",
          DependencyEdge.DependencyType.FUNCTION, true);

      assertNotEquals(edge1, edge2, "Different import names should not be equal");
    }

    @Test
    @DisplayName("edges with different dependency types should not be equal")
    void differentTypesShouldNotBeEqual() {
      final Module module = createMockModule();
      final DependencyEdge edge1 = new DependencyEdge(
          module, module, "env", "resource",
          DependencyEdge.DependencyType.MEMORY, true);
      final DependencyEdge edge2 = new DependencyEdge(
          module, module, "env", "resource",
          DependencyEdge.DependencyType.TABLE, true);

      assertNotEquals(edge1, edge2, "Different dependency types should not be equal");
    }

    @Test
    @DisplayName("edges with different resolved status should not be equal")
    void differentResolvedStatusShouldNotBeEqual() {
      final Module module = createMockModule();
      final DependencyEdge edge1 = new DependencyEdge(
          module, module, "env", "func",
          DependencyEdge.DependencyType.FUNCTION, true);
      final DependencyEdge edge2 = new DependencyEdge(
          module, module, "env", "func",
          DependencyEdge.DependencyType.FUNCTION, false);

      assertNotEquals(edge1, edge2, "Different resolved status should not be equal");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain key information")
    void toStringShouldContainKeyInfo() {
      final Module module = createMockModule();
      final DependencyEdge edge = new DependencyEdge(
          module, module, "wasi_snapshot_preview1", "fd_read",
          DependencyEdge.DependencyType.FUNCTION, true);

      final String result = edge.toString();
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("DependencyEdge"), "toString should contain class name");
      assertTrue(result.contains("FUNCTION"), "toString should contain dependency type");
      assertTrue(result.contains("true"), "toString should contain resolved status");
    }
  }

  /** Creates a minimal Module proxy for testing DependencyEdge. */
  private Module createMockModule() {
    return (Module) java.lang.reflect.Proxy.newProxyInstance(
        Module.class.getClassLoader(),
        new Class<?>[] {Module.class},
        (proxy, method, args) -> {
          if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(proxy);
          }
          if ("equals".equals(method.getName())) {
            return proxy == args[0];
          }
          if ("toString".equals(method.getName())) {
            return "MockModule@" + Integer.toHexString(System.identityHashCode(proxy));
          }
          return null;
        });
  }
}
