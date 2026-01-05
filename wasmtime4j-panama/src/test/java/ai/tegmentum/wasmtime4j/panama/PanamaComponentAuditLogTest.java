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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.ComponentAuditLog;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentAuditLog} class.
 *
 * <p>PanamaComponentAuditLog provides audit logging for WebAssembly components.
 */
@DisplayName("PanamaComponentAuditLog Tests")
class PanamaComponentAuditLogTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaComponentAuditLog.class.getModifiers()),
          "PanamaComponentAuditLog should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaComponentAuditLog.class.getModifiers()),
          "PanamaComponentAuditLog should be final");
    }

    @Test
    @DisplayName("should implement ComponentAuditLog interface")
    void shouldImplementComponentAuditLogInterface() {
      assertTrue(
          ComponentAuditLog.class.isAssignableFrom(PanamaComponentAuditLog.class),
          "PanamaComponentAuditLog should implement ComponentAuditLog");
    }
  }

  @Nested
  @DisplayName("ComponentAuditLog Method Tests")
  class ComponentAuditLogMethodTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentAuditLog.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getEntries method")
    void shouldHaveGetEntriesMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentAuditLog.class.getMethod("getEntries");
      assertNotNull(method, "getEntries method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have addEntry method")
    void shouldHaveAddEntryMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentAuditLog.class.getMethod("addEntry", ComponentAuditLog.AuditEntry.class);
      assertNotNull(method, "addEntry method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getEntriesByType method")
    void shouldHaveGetEntriesByTypeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentAuditLog.class.getMethod(
              "getEntriesByType", ComponentAuditLog.AuditEntryType.class);
      assertNotNull(method, "getEntriesByType method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getEntriesInRange method")
    void shouldHaveGetEntriesInRangeMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentAuditLog.class.getMethod("getEntriesInRange", long.class, long.class);
      assertNotNull(method, "getEntriesInRange method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentAuditLog.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentAuditLog.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentAuditLog.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have export method")
    void shouldHaveExportMethod() throws NoSuchMethodException {
      final Method method =
          PanamaComponentAuditLog.class.getMethod("export", ComponentAuditLog.ExportFormat.class);
      assertNotNull(method, "export method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with String parameter")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaComponentAuditLog.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 1
            && constructor.getParameterTypes()[0] == String.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have constructor with String parameter");
    }
  }

  @Nested
  @DisplayName("DefaultAuditEntry Inner Class Tests")
  class DefaultAuditEntryTests {

    @Test
    @DisplayName("should have DefaultAuditEntry inner class")
    void shouldHaveDefaultAuditEntryInnerClass() {
      boolean hasInnerClass = false;
      for (var innerClass : PanamaComponentAuditLog.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultAuditEntry")) {
          hasInnerClass = true;
          break;
        }
      }
      assertTrue(hasInnerClass, "Should have DefaultAuditEntry inner class");
    }

    @Test
    @DisplayName("DefaultAuditEntry should implement AuditEntry interface")
    void defaultAuditEntryShouldImplementAuditEntryInterface() {
      for (var innerClass : PanamaComponentAuditLog.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("DefaultAuditEntry")) {
          assertTrue(
              ComponentAuditLog.AuditEntry.class.isAssignableFrom(innerClass),
              "DefaultAuditEntry should implement AuditEntry");
          break;
        }
      }
    }
  }
}
