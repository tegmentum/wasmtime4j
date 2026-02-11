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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ResourcesRequired} class.
 *
 * <p>ResourcesRequired describes the resources needed to instantiate a WebAssembly module. Has an
 * 8-param constructor, an empty() static factory, getters for all fields, and toString. No
 * equals/hashCode.
 */
@DisplayName("ResourcesRequired Tests")
class ResourcesRequiredTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ResourcesRequired.class.getModifiers()),
          "ResourcesRequired should be public");
      assertTrue(
          Modifier.isFinal(ResourcesRequired.class.getModifiers()),
          "ResourcesRequired should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final ResourcesRequired resources =
          new ResourcesRequired(65536L, 1048576L, 10, 100, 1, 2, 5, 20);

      assertNotNull(resources, "ResourcesRequired should be created");
    }

    @Test
    @DisplayName("should accept zero values for all parameters")
    void shouldAcceptZeroValuesForAllParameters() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 0, 0, 0, 0, 0);

      assertEquals(0L, resources.getMinimumMemoryBytes(), "Minimum memory bytes should be 0");
      assertEquals(0L, resources.getMaximumMemoryBytes(), "Maximum memory bytes should be 0");
      assertEquals(0, resources.getMinimumTableElements(), "Minimum table elements should be 0");
      assertEquals(0, resources.getMaximumTableElements(), "Maximum table elements should be 0");
      assertEquals(0, resources.getNumMemories(), "Num memories should be 0");
      assertEquals(0, resources.getNumTables(), "Num tables should be 0");
      assertEquals(0, resources.getNumGlobals(), "Num globals should be 0");
      assertEquals(0, resources.getNumFunctions(), "Num functions should be 0");
    }

    @Test
    @DisplayName("should accept large values for long parameters")
    void shouldAcceptLargeValuesForLongParameters() {
      final ResourcesRequired resources =
          new ResourcesRequired(Long.MAX_VALUE, Long.MAX_VALUE, 0, 0, 0, 0, 0, 0);

      assertEquals(
          Long.MAX_VALUE,
          resources.getMinimumMemoryBytes(),
          "Should handle Long.MAX_VALUE for minimumMemoryBytes");
      assertEquals(
          Long.MAX_VALUE,
          resources.getMaximumMemoryBytes(),
          "Should handle Long.MAX_VALUE for maximumMemoryBytes");
    }

    @Test
    @DisplayName("should accept negative values without validation")
    void shouldAcceptNegativeValuesWithoutValidation() {
      final ResourcesRequired resources = new ResourcesRequired(-1L, -1L, -1, -1, -1, -1, -1, -1);

      assertEquals(
          -1L,
          resources.getMinimumMemoryBytes(),
          "Constructor does not validate; -1 for unlimited memory");
      assertEquals(
          -1L,
          resources.getMaximumMemoryBytes(),
          "Constructor does not validate; -1 for unlimited max memory");
      assertEquals(
          -1,
          resources.getMinimumTableElements(),
          "Constructor does not validate; -1 for unlimited table elements");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getMinimumMemoryBytes should return constructor value")
    void getMinimumMemoryBytesShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(65536L, 0L, 0, 0, 0, 0, 0, 0);

      assertEquals(
          65536L, resources.getMinimumMemoryBytes(), "Minimum memory bytes should be 65536");
    }

    @Test
    @DisplayName("getMaximumMemoryBytes should return constructor value")
    void getMaximumMemoryBytesShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 1048576L, 0, 0, 0, 0, 0, 0);

      assertEquals(
          1048576L, resources.getMaximumMemoryBytes(), "Maximum memory bytes should be 1048576");
    }

    @Test
    @DisplayName("getMinimumTableElements should return constructor value")
    void getMinimumTableElementsShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 10, 0, 0, 0, 0, 0);

      assertEquals(10, resources.getMinimumTableElements(), "Minimum table elements should be 10");
    }

    @Test
    @DisplayName("getMaximumTableElements should return constructor value")
    void getMaximumTableElementsShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 100, 0, 0, 0, 0);

      assertEquals(
          100, resources.getMaximumTableElements(), "Maximum table elements should be 100");
    }

    @Test
    @DisplayName("getNumMemories should return constructor value")
    void getNumMemoriesShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 0, 3, 0, 0, 0);

      assertEquals(3, resources.getNumMemories(), "Num memories should be 3");
    }

    @Test
    @DisplayName("getNumTables should return constructor value")
    void getNumTablesShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 0, 0, 4, 0, 0);

      assertEquals(4, resources.getNumTables(), "Num tables should be 4");
    }

    @Test
    @DisplayName("getNumGlobals should return constructor value")
    void getNumGlobalsShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 0, 0, 0, 7, 0);

      assertEquals(7, resources.getNumGlobals(), "Num globals should be 7");
    }

    @Test
    @DisplayName("getNumFunctions should return constructor value")
    void getNumFunctionsShouldReturnConstructorValue() {
      final ResourcesRequired resources = new ResourcesRequired(0L, 0L, 0, 0, 0, 0, 0, 42);

      assertEquals(42, resources.getNumFunctions(), "Num functions should be 42");
    }

    @Test
    @DisplayName("all getters should return their respective constructor values")
    void allGettersShouldReturnTheirRespectiveValues() {
      final ResourcesRequired resources =
          new ResourcesRequired(65536L, 1048576L, 10, 100, 1, 2, 5, 20);

      assertEquals(
          65536L, resources.getMinimumMemoryBytes(), "Minimum memory bytes should be 65536");
      assertEquals(
          1048576L, resources.getMaximumMemoryBytes(), "Maximum memory bytes should be 1048576");
      assertEquals(10, resources.getMinimumTableElements(), "Minimum table elements should be 10");
      assertEquals(
          100, resources.getMaximumTableElements(), "Maximum table elements should be 100");
      assertEquals(1, resources.getNumMemories(), "Num memories should be 1");
      assertEquals(2, resources.getNumTables(), "Num tables should be 2");
      assertEquals(5, resources.getNumGlobals(), "Num globals should be 5");
      assertEquals(20, resources.getNumFunctions(), "Num functions should be 20");
    }
  }

  @Nested
  @DisplayName("Empty Factory Tests")
  class EmptyFactoryTests {

    @Test
    @DisplayName("empty should create instance with all zeros")
    void emptyShouldCreateInstanceWithAllZeros() {
      final ResourcesRequired resources = ResourcesRequired.empty();

      assertNotNull(resources, "Empty resources should not be null");
      assertEquals(0L, resources.getMinimumMemoryBytes(), "Empty minimum memory bytes should be 0");
      assertEquals(0L, resources.getMaximumMemoryBytes(), "Empty maximum memory bytes should be 0");
      assertEquals(
          0, resources.getMinimumTableElements(), "Empty minimum table elements should be 0");
      assertEquals(
          0, resources.getMaximumTableElements(), "Empty maximum table elements should be 0");
      assertEquals(0, resources.getNumMemories(), "Empty num memories should be 0");
      assertEquals(0, resources.getNumTables(), "Empty num tables should be 0");
      assertEquals(0, resources.getNumGlobals(), "Empty num globals should be 0");
      assertEquals(0, resources.getNumFunctions(), "Empty num functions should be 0");
    }

    @Test
    @DisplayName("empty should be equivalent to all-zero constructor")
    void emptyShouldBeEquivalentToAllZeroConstructor() {
      final ResourcesRequired empty = ResourcesRequired.empty();
      final ResourcesRequired allZeros = new ResourcesRequired(0L, 0L, 0, 0, 0, 0, 0, 0);

      assertEquals(
          empty.getMinimumMemoryBytes(),
          allZeros.getMinimumMemoryBytes(),
          "Empty and all-zeros should have same minimumMemoryBytes");
      assertEquals(
          empty.getMaximumMemoryBytes(),
          allZeros.getMaximumMemoryBytes(),
          "Empty and all-zeros should have same maximumMemoryBytes");
      assertEquals(
          empty.getMinimumTableElements(),
          allZeros.getMinimumTableElements(),
          "Empty and all-zeros should have same minimumTableElements");
      assertEquals(
          empty.getMaximumTableElements(),
          allZeros.getMaximumTableElements(),
          "Empty and all-zeros should have same maximumTableElements");
      assertEquals(
          empty.getNumMemories(),
          allZeros.getNumMemories(),
          "Empty and all-zeros should have same numMemories");
      assertEquals(
          empty.getNumTables(),
          allZeros.getNumTables(),
          "Empty and all-zeros should have same numTables");
      assertEquals(
          empty.getNumGlobals(),
          allZeros.getNumGlobals(),
          "Empty and all-zeros should have same numGlobals");
      assertEquals(
          empty.getNumFunctions(),
          allZeros.getNumFunctions(),
          "Empty and all-zeros should have same numFunctions");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain ResourcesRequired prefix")
    void toStringShouldContainPrefix() {
      final ResourcesRequired resources = ResourcesRequired.empty();

      assertTrue(
          resources.toString().startsWith("ResourcesRequired{"),
          "toString should start with 'ResourcesRequired{'");
    }

    @Test
    @DisplayName("toString should contain all field names")
    void toStringShouldContainAllFieldNames() {
      final ResourcesRequired resources =
          new ResourcesRequired(65536L, 1048576L, 10, 100, 1, 2, 5, 20);

      final String str = resources.toString();

      assertTrue(
          str.contains("minimumMemoryBytes"), "toString should contain 'minimumMemoryBytes'");
      assertTrue(
          str.contains("maximumMemoryBytes"), "toString should contain 'maximumMemoryBytes'");
      assertTrue(
          str.contains("minimumTableElements"), "toString should contain 'minimumTableElements'");
      assertTrue(
          str.contains("maximumTableElements"), "toString should contain 'maximumTableElements'");
      assertTrue(str.contains("numMemories"), "toString should contain 'numMemories'");
      assertTrue(str.contains("numTables"), "toString should contain 'numTables'");
      assertTrue(str.contains("numGlobals"), "toString should contain 'numGlobals'");
      assertTrue(str.contains("numFunctions"), "toString should contain 'numFunctions'");
    }

    @Test
    @DisplayName("toString should contain field values")
    void toStringShouldContainFieldValues() {
      final ResourcesRequired resources =
          new ResourcesRequired(65536L, 1048576L, 10, 100, 1, 2, 5, 20);

      final String str = resources.toString();

      assertTrue(str.contains("65536"), "toString should contain minimumMemoryBytes value");
      assertTrue(str.contains("1048576"), "toString should contain maximumMemoryBytes value");
    }
  }
}
