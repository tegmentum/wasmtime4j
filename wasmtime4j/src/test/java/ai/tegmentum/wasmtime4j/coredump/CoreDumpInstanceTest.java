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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpInstance} class.
 *
 * <p>CoreDumpInstance is the default implementation of the CoreDumpInstance interface.
 */
@DisplayName("CoreDumpInstance Tests")
class CoreDumpInstanceTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final CoreDumpInstance.Builder builder = CoreDumpInstance.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with index")
    void shouldBuildWithIndex() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().index(5).build();
      assertEquals(5, instance.getIndex(), "Index should match");
    }

    @Test
    @DisplayName("should build with moduleIndex")
    void shouldBuildWithModuleIndex() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().moduleIndex(2).build();
      assertEquals(2, instance.getModuleIndex(), "ModuleIndex should match");
    }

    @Test
    @DisplayName("should build with name")
    void shouldBuildWithName() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().name("test-instance").build();
      assertTrue(instance.getName().isPresent(), "Name should be present");
      assertEquals("test-instance", instance.getName().get(), "Name should match");
    }

    @Test
    @DisplayName("should build without name")
    void shouldBuildWithoutName() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertFalse(instance.getName().isPresent(), "Name should not be present");
    }

    @Test
    @DisplayName("should build with memoryCount")
    void shouldBuildWithMemoryCount() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().memoryCount(2).build();
      assertEquals(2, instance.getMemoryCount(), "MemoryCount should match");
    }

    @Test
    @DisplayName("should build with globalCount")
    void shouldBuildWithGlobalCount() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().globalCount(5).build();
      assertEquals(5, instance.getGlobalCount(), "GlobalCount should match");
    }

    @Test
    @DisplayName("should build with tableCount")
    void shouldBuildWithTableCount() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder().tableCount(1).build();
      assertEquals(1, instance.getTableCount(), "TableCount should match");
    }
  }

  @Nested
  @DisplayName("Default Values Tests")
  class DefaultValuesTests {

    @Test
    @DisplayName("should have zero index by default")
    void shouldHaveZeroIndexByDefault() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertEquals(0, instance.getIndex(), "Index should default to 0");
    }

    @Test
    @DisplayName("should have zero moduleIndex by default")
    void shouldHaveZeroModuleIndexByDefault() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertEquals(0, instance.getModuleIndex(), "ModuleIndex should default to 0");
    }

    @Test
    @DisplayName("should have zero memoryCount by default")
    void shouldHaveZeroMemoryCountByDefault() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertEquals(0, instance.getMemoryCount(), "MemoryCount should default to 0");
    }

    @Test
    @DisplayName("should have zero globalCount by default")
    void shouldHaveZeroGlobalCountByDefault() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertEquals(0, instance.getGlobalCount(), "GlobalCount should default to 0");
    }

    @Test
    @DisplayName("should have zero tableCount by default")
    void shouldHaveZeroTableCountByDefault() {
      final CoreDumpInstance instance = CoreDumpInstance.builder().build();
      assertEquals(0, instance.getTableCount(), "TableCount should default to 0");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder()
              .index(0)
              .moduleIndex(0)
              .name("main-instance")
              .memoryCount(1)
              .globalCount(3)
              .tableCount(2)
              .build();
      final String str = instance.toString();
      assertTrue(str.contains("CoreDumpInstance"), "Should contain class name");
      assertTrue(str.contains("main-instance"), "Should contain name");
      assertTrue(str.contains("memoryCount=1"), "Should contain memory count");
      assertTrue(str.contains("globalCount=3"), "Should contain global count");
      assertTrue(str.contains("tableCount=2"), "Should contain table count");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete instance")
    void shouldBuildCompleteInstance() {
      final CoreDumpInstance instance =
          CoreDumpInstance.builder()
              .index(1)
              .moduleIndex(0)
              .name("calculator")
              .memoryCount(1)
              .globalCount(5)
              .tableCount(2)
              .build();

      assertEquals(1, instance.getIndex(), "Index should match");
      assertEquals(0, instance.getModuleIndex(), "ModuleIndex should match");
      assertEquals("calculator", instance.getName().get(), "Name should match");
      assertEquals(1, instance.getMemoryCount(), "MemoryCount should match");
      assertEquals(5, instance.getGlobalCount(), "GlobalCount should match");
      assertEquals(2, instance.getTableCount(), "TableCount should match");
    }

    @Test
    @DisplayName("should support multiple instances with different indices")
    void shouldSupportMultipleInstancesWithDifferentIndices() {
      final CoreDumpInstance instance1 =
          CoreDumpInstance.builder().index(0).name("instance0").build();
      final CoreDumpInstance instance2 =
          CoreDumpInstance.builder().index(1).name("instance1").build();
      final CoreDumpInstance instance3 =
          CoreDumpInstance.builder().index(2).name("instance2").build();

      assertEquals(0, instance1.getIndex(), "First instance index should be 0");
      assertEquals(1, instance2.getIndex(), "Second instance index should be 1");
      assertEquals(2, instance3.getIndex(), "Third instance index should be 2");
    }
  }
}
