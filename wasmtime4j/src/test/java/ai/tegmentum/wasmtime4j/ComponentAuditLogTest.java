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

import ai.tegmentum.wasmtime4j.ComponentAuditLog.AuditEntryType;
import ai.tegmentum.wasmtime4j.ComponentAuditLog.ExportFormat;
import ai.tegmentum.wasmtime4j.ComponentAuditLog.SeverityLevel;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentAuditLog} interface.
 *
 * <p>ComponentAuditLog provides audit logging for WebAssembly components.
 */
@DisplayName("ComponentAuditLog Tests")
class ComponentAuditLogTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentAuditLog.class.getModifiers()),
          "ComponentAuditLog should be public");
      assertTrue(ComponentAuditLog.class.isInterface(), "ComponentAuditLog should be an interface");
    }

    @Test
    @DisplayName("should have AuditEntry nested interface")
    void shouldHaveAuditEntryNestedInterface() {
      final var nestedClasses = ComponentAuditLog.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("AuditEntry")) {
          found = true;
          assertTrue(nestedClass.isInterface(), "AuditEntry should be an interface");
          break;
        }
      }
      assertTrue(found, "Should have AuditEntry nested interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getEntries method")
    void shouldHaveGetEntriesMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("getEntries");
      assertNotNull(method, "getEntries method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have addEntry method")
    void shouldHaveAddEntryMethod() throws NoSuchMethodException {
      final Method method =
          ComponentAuditLog.class.getMethod("addEntry", ComponentAuditLog.AuditEntry.class);
      assertNotNull(method, "addEntry method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getEntriesByType method")
    void shouldHaveGetEntriesByTypeMethod() throws NoSuchMethodException {
      final Method method =
          ComponentAuditLog.class.getMethod("getEntriesByType", AuditEntryType.class);
      assertNotNull(method, "getEntriesByType method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getEntriesInRange method")
    void shouldHaveGetEntriesInRangeMethod() throws NoSuchMethodException {
      final Method method =
          ComponentAuditLog.class.getMethod("getEntriesInRange", long.class, long.class);
      assertNotNull(method, "getEntriesInRange method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isEmpty method")
    void shouldHaveIsEmptyMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("isEmpty");
      assertNotNull(method, "isEmpty method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have export method")
    void shouldHaveExportMethod() throws NoSuchMethodException {
      final Method method = ComponentAuditLog.class.getMethod("export", ExportFormat.class);
      assertNotNull(method, "export method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }
  }

  @Nested
  @DisplayName("AuditEntryType Enum Tests")
  class AuditEntryTypeEnumTests {

    @Test
    @DisplayName("should have all entry types")
    void shouldHaveAllEntryTypes() {
      final var types = AuditEntryType.values();
      assertEquals(10, types.length, "Should have 10 entry types");
    }

    @Test
    @DisplayName("should have COMPONENT_CREATED type")
    void shouldHaveComponentCreatedType() {
      assertEquals(AuditEntryType.COMPONENT_CREATED, AuditEntryType.valueOf("COMPONENT_CREATED"));
    }

    @Test
    @DisplayName("should have COMPONENT_INSTANTIATED type")
    void shouldHaveComponentInstantiatedType() {
      assertEquals(
          AuditEntryType.COMPONENT_INSTANTIATED, AuditEntryType.valueOf("COMPONENT_INSTANTIATED"));
    }

    @Test
    @DisplayName("should have COMPONENT_EXECUTED type")
    void shouldHaveComponentExecutedType() {
      assertEquals(AuditEntryType.COMPONENT_EXECUTED, AuditEntryType.valueOf("COMPONENT_EXECUTED"));
    }

    @Test
    @DisplayName("should have COMPONENT_MODIFIED type")
    void shouldHaveComponentModifiedType() {
      assertEquals(AuditEntryType.COMPONENT_MODIFIED, AuditEntryType.valueOf("COMPONENT_MODIFIED"));
    }

    @Test
    @DisplayName("should have COMPONENT_DELETED type")
    void shouldHaveComponentDeletedType() {
      assertEquals(AuditEntryType.COMPONENT_DELETED, AuditEntryType.valueOf("COMPONENT_DELETED"));
    }

    @Test
    @DisplayName("should have SECURITY_EVENT type")
    void shouldHaveSecurityEventType() {
      assertEquals(AuditEntryType.SECURITY_EVENT, AuditEntryType.valueOf("SECURITY_EVENT"));
    }

    @Test
    @DisplayName("should have RESOURCE_ACCESS type")
    void shouldHaveResourceAccessType() {
      assertEquals(AuditEntryType.RESOURCE_ACCESS, AuditEntryType.valueOf("RESOURCE_ACCESS"));
    }

    @Test
    @DisplayName("should have CONFIGURATION_CHANGE type")
    void shouldHaveConfigurationChangeType() {
      assertEquals(
          AuditEntryType.CONFIGURATION_CHANGE, AuditEntryType.valueOf("CONFIGURATION_CHANGE"));
    }

    @Test
    @DisplayName("should have ERROR_EVENT type")
    void shouldHaveErrorEventType() {
      assertEquals(AuditEntryType.ERROR_EVENT, AuditEntryType.valueOf("ERROR_EVENT"));
    }

    @Test
    @DisplayName("should have SYSTEM_EVENT type")
    void shouldHaveSystemEventType() {
      assertEquals(AuditEntryType.SYSTEM_EVENT, AuditEntryType.valueOf("SYSTEM_EVENT"));
    }
  }

  @Nested
  @DisplayName("SeverityLevel Enum Tests")
  class SeverityLevelEnumTests {

    @Test
    @DisplayName("should have all severity levels")
    void shouldHaveAllSeverityLevels() {
      final var levels = SeverityLevel.values();
      assertEquals(4, levels.length, "Should have 4 severity levels");
    }

    @Test
    @DisplayName("should have INFO level")
    void shouldHaveInfoLevel() {
      assertEquals(SeverityLevel.INFO, SeverityLevel.valueOf("INFO"));
    }

    @Test
    @DisplayName("should have WARNING level")
    void shouldHaveWarningLevel() {
      assertEquals(SeverityLevel.WARNING, SeverityLevel.valueOf("WARNING"));
    }

    @Test
    @DisplayName("should have ERROR level")
    void shouldHaveErrorLevel() {
      assertEquals(SeverityLevel.ERROR, SeverityLevel.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have CRITICAL level")
    void shouldHaveCriticalLevel() {
      assertEquals(SeverityLevel.CRITICAL, SeverityLevel.valueOf("CRITICAL"));
    }
  }

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatEnumTests {

    @Test
    @DisplayName("should have all export formats")
    void shouldHaveAllExportFormats() {
      final var formats = ExportFormat.values();
      assertEquals(4, formats.length, "Should have 4 export formats");
    }

    @Test
    @DisplayName("should have JSON format")
    void shouldHaveJsonFormat() {
      assertEquals(ExportFormat.JSON, ExportFormat.valueOf("JSON"));
    }

    @Test
    @DisplayName("should have CSV format")
    void shouldHaveCsvFormat() {
      assertEquals(ExportFormat.CSV, ExportFormat.valueOf("CSV"));
    }

    @Test
    @DisplayName("should have XML format")
    void shouldHaveXmlFormat() {
      assertEquals(ExportFormat.XML, ExportFormat.valueOf("XML"));
    }

    @Test
    @DisplayName("should have TEXT format")
    void shouldHaveTextFormat() {
      assertEquals(ExportFormat.TEXT, ExportFormat.valueOf("TEXT"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have AuditEntry nested interface")
    void shouldHaveAuditEntryNestedInterface() {
      final var nestedClasses = ComponentAuditLog.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(classNames.contains("AuditEntry"), "Should have AuditEntry");
    }

    @Test
    @DisplayName("should have all expected enums")
    void shouldHaveAllExpectedEnums() {
      final var nestedClasses = ComponentAuditLog.class.getDeclaredClasses();
      final var enumNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isEnum)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(enumNames.contains("AuditEntryType"), "Should have AuditEntryType enum");
      assertTrue(enumNames.contains("SeverityLevel"), "Should have SeverityLevel enum");
      assertTrue(enumNames.contains("ExportFormat"), "Should have ExportFormat enum");
    }
  }

  @Nested
  @DisplayName("AuditEntry Interface Tests")
  class AuditEntryInterfaceTests {

    @Test
    @DisplayName("AuditEntry should have required methods")
    void auditEntryShouldHaveRequiredMethods() throws NoSuchMethodException {
      final Class<?> auditEntryClass = ComponentAuditLog.AuditEntry.class;

      assertNotNull(auditEntryClass.getMethod("getId"), "Should have getId");
      assertNotNull(auditEntryClass.getMethod("getTimestamp"), "Should have getTimestamp");
      assertNotNull(auditEntryClass.getMethod("getType"), "Should have getType");
      assertNotNull(auditEntryClass.getMethod("getMessage"), "Should have getMessage");
      assertNotNull(auditEntryClass.getMethod("getUser"), "Should have getUser");
      assertNotNull(auditEntryClass.getMethod("getAction"), "Should have getAction");
      assertNotNull(auditEntryClass.getMethod("getResource"), "Should have getResource");
      assertNotNull(auditEntryClass.getMethod("getMetadata"), "Should have getMetadata");
      assertNotNull(auditEntryClass.getMethod("getSeverity"), "Should have getSeverity");
    }
  }
}
