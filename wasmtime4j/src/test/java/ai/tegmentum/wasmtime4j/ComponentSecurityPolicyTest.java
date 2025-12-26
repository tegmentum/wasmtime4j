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

import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.AuditLevel;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.ConditionOperator;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.EnvironmentAction;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.FileAction;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.MemoryPermission;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.OperationType;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.RuleAction;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.RuleType;
import ai.tegmentum.wasmtime4j.ComponentSecurityPolicy.SecurityLevel;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSecurityPolicy} interface.
 *
 * <p>ComponentSecurityPolicy provides security policy configuration for WebAssembly components.
 */
@DisplayName("ComponentSecurityPolicy Tests")
class ComponentSecurityPolicyTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(
          Modifier.isPublic(ComponentSecurityPolicy.class.getModifiers()),
          "ComponentSecurityPolicy should be public");
      assertTrue(
          ComponentSecurityPolicy.class.isInterface(),
          "ComponentSecurityPolicy should be an interface");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getVersion");
      assertNotNull(method, "getVersion method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getAccessRules method")
    void shouldHaveGetAccessRulesMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getAccessRules");
      assertNotNull(method, "getAccessRules method should exist");
      assertEquals(java.util.List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getResourcePermissions method")
    void shouldHaveGetResourcePermissionsMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getResourcePermissions");
      assertNotNull(method, "getResourcePermissions method should exist");
    }

    @Test
    @DisplayName("should have getExecutionRestrictions method")
    void shouldHaveGetExecutionRestrictionsMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getExecutionRestrictions");
      assertNotNull(method, "getExecutionRestrictions method should exist");
    }

    @Test
    @DisplayName("should have getSecurityConstraints method")
    void shouldHaveGetSecurityConstraintsMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getSecurityConstraints");
      assertNotNull(method, "getSecurityConstraints method should exist");
    }

    @Test
    @DisplayName("should have isAllowed method")
    void shouldHaveIsAllowedMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSecurityPolicy.class.getMethod(
              "isAllowed", ComponentSecurityPolicy.SecurityOperation.class);
      assertNotNull(method, "isAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getAuditConfig method")
    void shouldHaveGetAuditConfigMethod() throws NoSuchMethodException {
      final Method method = ComponentSecurityPolicy.class.getMethod("getAuditConfig");
      assertNotNull(method, "getAuditConfig method should exist");
    }
  }

  @Nested
  @DisplayName("RuleType Enum Tests")
  class RuleTypeEnumTests {

    @Test
    @DisplayName("should have all rule types")
    void shouldHaveAllRuleTypes() {
      final var types = RuleType.values();
      assertEquals(4, types.length, "Should have 4 rule types");
    }

    @Test
    @DisplayName("should have ALLOW type")
    void shouldHaveAllowType() {
      assertEquals(RuleType.ALLOW, RuleType.valueOf("ALLOW"));
    }

    @Test
    @DisplayName("should have DENY type")
    void shouldHaveDenyType() {
      assertEquals(RuleType.DENY, RuleType.valueOf("DENY"));
    }

    @Test
    @DisplayName("should have AUDIT type")
    void shouldHaveAuditType() {
      assertEquals(RuleType.AUDIT, RuleType.valueOf("AUDIT"));
    }

    @Test
    @DisplayName("should have RATE_LIMIT type")
    void shouldHaveRateLimitType() {
      assertEquals(RuleType.RATE_LIMIT, RuleType.valueOf("RATE_LIMIT"));
    }
  }

  @Nested
  @DisplayName("RuleAction Enum Tests")
  class RuleActionEnumTests {

    @Test
    @DisplayName("should have all rule actions")
    void shouldHaveAllRuleActions() {
      final var actions = RuleAction.values();
      assertEquals(5, actions.length, "Should have 5 rule actions");
    }

    @Test
    @DisplayName("should have PERMIT action")
    void shouldHavePermitAction() {
      assertEquals(RuleAction.PERMIT, RuleAction.valueOf("PERMIT"));
    }

    @Test
    @DisplayName("should have DENY action")
    void shouldHaveDenyAction() {
      assertEquals(RuleAction.DENY, RuleAction.valueOf("DENY"));
    }

    @Test
    @DisplayName("should have LOG action")
    void shouldHaveLogAction() {
      assertEquals(RuleAction.LOG, RuleAction.valueOf("LOG"));
    }

    @Test
    @DisplayName("should have ALERT action")
    void shouldHaveAlertAction() {
      assertEquals(RuleAction.ALERT, RuleAction.valueOf("ALERT"));
    }

    @Test
    @DisplayName("should have BLOCK action")
    void shouldHaveBlockAction() {
      assertEquals(RuleAction.BLOCK, RuleAction.valueOf("BLOCK"));
    }
  }

  @Nested
  @DisplayName("OperationType Enum Tests")
  class OperationTypeEnumTests {

    @Test
    @DisplayName("should have all operation types")
    void shouldHaveAllOperationTypes() {
      final var types = OperationType.values();
      assertEquals(5, types.length, "Should have 5 operation types");
    }

    @Test
    @DisplayName("should have MEMORY type")
    void shouldHaveMemoryType() {
      assertEquals(OperationType.MEMORY, OperationType.valueOf("MEMORY"));
    }

    @Test
    @DisplayName("should have FILE_SYSTEM type")
    void shouldHaveFileSystemType() {
      assertEquals(OperationType.FILE_SYSTEM, OperationType.valueOf("FILE_SYSTEM"));
    }

    @Test
    @DisplayName("should have NETWORK type")
    void shouldHaveNetworkType() {
      assertEquals(OperationType.NETWORK, OperationType.valueOf("NETWORK"));
    }

    @Test
    @DisplayName("should have FUNCTION_CALL type")
    void shouldHaveFunctionCallType() {
      assertEquals(OperationType.FUNCTION_CALL, OperationType.valueOf("FUNCTION_CALL"));
    }

    @Test
    @DisplayName("should have RESOURCE_ACCESS type")
    void shouldHaveResourceAccessType() {
      assertEquals(OperationType.RESOURCE_ACCESS, OperationType.valueOf("RESOURCE_ACCESS"));
    }
  }

  @Nested
  @DisplayName("FileAction Enum Tests")
  class FileActionEnumTests {

    @Test
    @DisplayName("should have all file actions")
    void shouldHaveAllFileActions() {
      final var actions = FileAction.values();
      assertEquals(5, actions.length, "Should have 5 file actions");
    }

    @Test
    @DisplayName("should have READ action")
    void shouldHaveReadAction() {
      assertEquals(FileAction.READ, FileAction.valueOf("READ"));
    }

    @Test
    @DisplayName("should have WRITE action")
    void shouldHaveWriteAction() {
      assertEquals(FileAction.WRITE, FileAction.valueOf("WRITE"));
    }

    @Test
    @DisplayName("should have EXECUTE action")
    void shouldHaveExecuteAction() {
      assertEquals(FileAction.EXECUTE, FileAction.valueOf("EXECUTE"));
    }

    @Test
    @DisplayName("should have DELETE action")
    void shouldHaveDeleteAction() {
      assertEquals(FileAction.DELETE, FileAction.valueOf("DELETE"));
    }

    @Test
    @DisplayName("should have CREATE action")
    void shouldHaveCreateAction() {
      assertEquals(FileAction.CREATE, FileAction.valueOf("CREATE"));
    }
  }

  @Nested
  @DisplayName("EnvironmentAction Enum Tests")
  class EnvironmentActionEnumTests {

    @Test
    @DisplayName("should have all environment actions")
    void shouldHaveAllEnvironmentActions() {
      final var actions = EnvironmentAction.values();
      assertEquals(2, actions.length, "Should have 2 environment actions");
    }

    @Test
    @DisplayName("should have READ action")
    void shouldHaveReadAction() {
      assertEquals(EnvironmentAction.READ, EnvironmentAction.valueOf("READ"));
    }

    @Test
    @DisplayName("should have WRITE action")
    void shouldHaveWriteAction() {
      assertEquals(EnvironmentAction.WRITE, EnvironmentAction.valueOf("WRITE"));
    }
  }

  @Nested
  @DisplayName("SecurityLevel Enum Tests")
  class SecurityLevelEnumTests {

    @Test
    @DisplayName("should have all security levels")
    void shouldHaveAllSecurityLevels() {
      final var levels = SecurityLevel.values();
      assertEquals(4, levels.length, "Should have 4 security levels");
    }

    @Test
    @DisplayName("should have LOW level")
    void shouldHaveLowLevel() {
      assertEquals(SecurityLevel.LOW, SecurityLevel.valueOf("LOW"));
    }

    @Test
    @DisplayName("should have MEDIUM level")
    void shouldHaveMediumLevel() {
      assertEquals(SecurityLevel.MEDIUM, SecurityLevel.valueOf("MEDIUM"));
    }

    @Test
    @DisplayName("should have HIGH level")
    void shouldHaveHighLevel() {
      assertEquals(SecurityLevel.HIGH, SecurityLevel.valueOf("HIGH"));
    }

    @Test
    @DisplayName("should have MAXIMUM level")
    void shouldHaveMaximumLevel() {
      assertEquals(SecurityLevel.MAXIMUM, SecurityLevel.valueOf("MAXIMUM"));
    }
  }

  @Nested
  @DisplayName("MemoryPermission Enum Tests")
  class MemoryPermissionEnumTests {

    @Test
    @DisplayName("should have all memory permissions")
    void shouldHaveAllMemoryPermissions() {
      final var permissions = MemoryPermission.values();
      assertEquals(3, permissions.length, "Should have 3 memory permissions");
    }

    @Test
    @DisplayName("should have READ permission")
    void shouldHaveReadPermission() {
      assertEquals(MemoryPermission.READ, MemoryPermission.valueOf("READ"));
    }

    @Test
    @DisplayName("should have WRITE permission")
    void shouldHaveWritePermission() {
      assertEquals(MemoryPermission.WRITE, MemoryPermission.valueOf("WRITE"));
    }

    @Test
    @DisplayName("should have EXECUTE permission")
    void shouldHaveExecutePermission() {
      assertEquals(MemoryPermission.EXECUTE, MemoryPermission.valueOf("EXECUTE"));
    }
  }

  @Nested
  @DisplayName("AuditLevel Enum Tests")
  class AuditLevelEnumTests {

    @Test
    @DisplayName("should have all audit levels")
    void shouldHaveAllAuditLevels() {
      final var levels = AuditLevel.values();
      assertEquals(5, levels.length, "Should have 5 audit levels");
    }

    @Test
    @DisplayName("should have NONE level")
    void shouldHaveNoneLevel() {
      assertEquals(AuditLevel.NONE, AuditLevel.valueOf("NONE"));
    }

    @Test
    @DisplayName("should have ERROR level")
    void shouldHaveErrorLevel() {
      assertEquals(AuditLevel.ERROR, AuditLevel.valueOf("ERROR"));
    }

    @Test
    @DisplayName("should have WARNING level")
    void shouldHaveWarningLevel() {
      assertEquals(AuditLevel.WARNING, AuditLevel.valueOf("WARNING"));
    }

    @Test
    @DisplayName("should have INFO level")
    void shouldHaveInfoLevel() {
      assertEquals(AuditLevel.INFO, AuditLevel.valueOf("INFO"));
    }

    @Test
    @DisplayName("should have DEBUG level")
    void shouldHaveDebugLevel() {
      assertEquals(AuditLevel.DEBUG, AuditLevel.valueOf("DEBUG"));
    }
  }

  @Nested
  @DisplayName("ConditionOperator Enum Tests")
  class ConditionOperatorEnumTests {

    @Test
    @DisplayName("should have all condition operators")
    void shouldHaveAllConditionOperators() {
      final var operators = ConditionOperator.values();
      assertEquals(8, operators.length, "Should have 8 condition operators");
    }

    @Test
    @DisplayName("should have EQUALS operator")
    void shouldHaveEqualsOperator() {
      assertEquals(ConditionOperator.EQUALS, ConditionOperator.valueOf("EQUALS"));
    }

    @Test
    @DisplayName("should have NOT_EQUALS operator")
    void shouldHaveNotEqualsOperator() {
      assertEquals(ConditionOperator.NOT_EQUALS, ConditionOperator.valueOf("NOT_EQUALS"));
    }

    @Test
    @DisplayName("should have CONTAINS operator")
    void shouldHaveContainsOperator() {
      assertEquals(ConditionOperator.CONTAINS, ConditionOperator.valueOf("CONTAINS"));
    }

    @Test
    @DisplayName("should have STARTS_WITH operator")
    void shouldHaveStartsWithOperator() {
      assertEquals(ConditionOperator.STARTS_WITH, ConditionOperator.valueOf("STARTS_WITH"));
    }

    @Test
    @DisplayName("should have ENDS_WITH operator")
    void shouldHaveEndsWithOperator() {
      assertEquals(ConditionOperator.ENDS_WITH, ConditionOperator.valueOf("ENDS_WITH"));
    }

    @Test
    @DisplayName("should have MATCHES operator")
    void shouldHaveMatchesOperator() {
      assertEquals(ConditionOperator.MATCHES, ConditionOperator.valueOf("MATCHES"));
    }

    @Test
    @DisplayName("should have IN operator")
    void shouldHaveInOperator() {
      assertEquals(ConditionOperator.IN, ConditionOperator.valueOf("IN"));
    }

    @Test
    @DisplayName("should have NOT_IN operator")
    void shouldHaveNotInOperator() {
      assertEquals(ConditionOperator.NOT_IN, ConditionOperator.valueOf("NOT_IN"));
    }
  }

  @Nested
  @DisplayName("Nested Interface Structure Tests")
  class NestedInterfaceStructureTests {

    @Test
    @DisplayName("should have all expected nested interfaces")
    void shouldHaveAllExpectedNestedInterfaces() {
      final var nestedClasses = ComponentSecurityPolicy.class.getDeclaredClasses();
      final var classNames =
          java.util.Arrays.stream(nestedClasses)
              .filter(Class::isInterface)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toSet());

      assertTrue(classNames.contains("AccessRule"), "Should have AccessRule");
      assertTrue(classNames.contains("ResourcePermissions"), "Should have ResourcePermissions");
      assertTrue(classNames.contains("MemoryPermissions"), "Should have MemoryPermissions");
      assertTrue(classNames.contains("FileSystemPermissions"), "Should have FileSystemPermissions");
      assertTrue(classNames.contains("NetworkPermissions"), "Should have NetworkPermissions");
      assertTrue(
          classNames.contains("EnvironmentPermissions"), "Should have EnvironmentPermissions");
      assertTrue(classNames.contains("ExecutionRestrictions"), "Should have ExecutionRestrictions");
      assertTrue(classNames.contains("SecurityConstraints"), "Should have SecurityConstraints");
      assertTrue(classNames.contains("SecurityRequest"), "Should have SecurityRequest");
      assertTrue(classNames.contains("SecurityOperation"), "Should have SecurityOperation");
      assertTrue(classNames.contains("SecurityContext"), "Should have SecurityContext");
      assertTrue(classNames.contains("ValidationResult"), "Should have ValidationResult");
      assertTrue(classNames.contains("AuditConfig"), "Should have AuditConfig");
    }
  }
}
