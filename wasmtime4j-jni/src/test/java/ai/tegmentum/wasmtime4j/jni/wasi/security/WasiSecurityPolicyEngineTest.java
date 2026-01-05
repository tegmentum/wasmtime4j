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

package ai.tegmentum.wasmtime4j.jni.wasi.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.WasiFileOperation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiSecurityPolicyEngine class.
 *
 * <p>WasiSecurityPolicyEngine provides capability-based security for WASI operations with
 * comprehensive monitoring, threat detection, and audit logging.
 */
@DisplayName("WasiSecurityPolicyEngine Class Tests")
class WasiSecurityPolicyEngineTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class, not an interface")
    void shouldBeAClass() {
      assertFalse(
          WasiSecurityPolicyEngine.class.isInterface(),
          "WasiSecurityPolicyEngine should be a class");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicyEngine.class.getModifiers()),
          "WasiSecurityPolicyEngine should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityPolicyEngine.class.getModifiers()),
          "WasiSecurityPolicyEngine should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          WasiSecurityPolicyEngine.class.getSuperclass(),
          "WasiSecurityPolicyEngine should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          WasiSecurityPolicyEngine.class.getInterfaces().length,
          "WasiSecurityPolicyEngine should not implement any interfaces");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("LOGGER");
      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("should have MAX_AUDIT_EVENTS constant")
    void shouldHaveMaxAuditEventsConstant() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("MAX_AUDIT_EVENTS");
      assertNotNull(field, "MAX_AUDIT_EVENTS field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have DEFAULT_RATE_LIMIT constant")
    void shouldHaveDefaultRateLimitConstant() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("DEFAULT_RATE_LIMIT");
      assertNotNull(field, "DEFAULT_RATE_LIMIT field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have securityPolicy field")
    void shouldHaveSecurityPolicyField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("securityPolicy");
      assertNotNull(field, "securityPolicy field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(
          WasiSecurityPolicyEngine.SecurityPolicy.class,
          field.getType(),
          "Should be SecurityPolicy type");
    }

    @Test
    @DisplayName("should have auditLogger field")
    void shouldHaveAuditLoggerField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("auditLogger");
      assertNotNull(field, "auditLogger field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have resourceTracker field")
    void shouldHaveResourceTrackerField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("resourceTracker");
      assertNotNull(field, "resourceTracker field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have accessMonitor field")
    void shouldHaveAccessMonitorField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("accessMonitor");
      assertNotNull(field, "accessMonitor field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }

    @Test
    @DisplayName("should have threatDetector field")
    void shouldHaveThreatDetectorField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.class.getDeclaredField("threatDetector");
      assertNotNull(field, "threatDetector field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor taking SecurityPolicy")
    void shouldHaveSecurityPolicyConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiSecurityPolicyEngine.class.getConstructor(
              WasiSecurityPolicyEngine.SecurityPolicy.class);
      assertNotNull(constructor, "Constructor(SecurityPolicy) should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      assertEquals(1, constructor.getParameterCount(), "Constructor should have 1 parameter");
    }

    @Test
    @DisplayName("should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      Constructor<?>[] constructors = WasiSecurityPolicyEngine.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
    }
  }

  // ========================================================================
  // Public Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Public Method Tests")
  class PublicMethodTests {

    @Test
    @DisplayName("should have validateFileSystemAccess method")
    void shouldHaveValidateFileSystemAccessMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getMethod(
              "validateFileSystemAccess", Path.class, WasiFileOperation.class, String.class);
      assertNotNull(method, "validateFileSystemAccess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateEnvironmentAccess method")
    void shouldHaveValidateEnvironmentAccessMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getMethod(
              "validateEnvironmentAccess", String.class, String.class, String.class);
      assertNotNull(method, "validateEnvironmentAccess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateNetworkAccess method")
    void shouldHaveValidateNetworkAccessMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getMethod(
              "validateNetworkAccess", String.class, int.class, String.class, String.class);
      assertNotNull(method, "validateNetworkAccess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getSecurityStatistics method")
    void shouldHaveGetSecurityStatisticsMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyEngine.class.getMethod("getSecurityStatistics");
      assertNotNull(method, "getSecurityStatistics method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(
          WasiSecurityPolicyEngine.SecurityStatistics.class,
          method.getReturnType(),
          "Should return SecurityStatistics");
    }

    @Test
    @DisplayName("should have getRecentAuditEvents method")
    void shouldHaveGetRecentAuditEventsMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyEngine.class.getMethod("getRecentAuditEvents", int.class);
      assertNotNull(method, "getRecentAuditEvents method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have updateSecurityPolicy method")
    void shouldHaveUpdateSecurityPolicyMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getMethod(
              "updateSecurityPolicy", WasiSecurityPolicyEngine.SecurityPolicy.class);
      assertNotNull(method, "updateSecurityPolicy method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Private Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have validateCapabilityAccess method")
    void shouldHaveValidateCapabilityAccessMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateCapabilityAccess", Path.class, WasiFileOperation.class, String.class);
      assertNotNull(method, "validateCapabilityAccess method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
    }

    @Test
    @DisplayName("should have validateResourceQuotas method")
    void shouldHaveValidateResourceQuotasMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateResourceQuotas", Path.class, WasiFileOperation.class, String.class);
      assertNotNull(method, "validateResourceQuotas method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
    }

    @Test
    @DisplayName("should have validateAccessPatterns method")
    void shouldHaveValidateAccessPatternsMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateAccessPatterns", Path.class, WasiFileOperation.class, String.class);
      assertNotNull(method, "validateAccessPatterns method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
    }

    @Test
    @DisplayName("should have detectThreats method")
    void shouldHaveDetectThreatsMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "detectThreats", Path.class, WasiFileOperation.class, String.class);
      assertNotNull(method, "detectThreats method should exist");
      assertTrue(Modifier.isPrivate(method.getModifiers()), "Should be private");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have expected nested classes")
    void shouldHaveExpectedNestedClasses() {
      Set<String> nestedClassNames =
          Arrays.stream(WasiSecurityPolicyEngine.class.getDeclaredClasses())
              .map(Class::getSimpleName)
              .collect(Collectors.toSet());

      assertTrue(nestedClassNames.contains("SecurityPolicy"), "Should have SecurityPolicy");
      assertTrue(nestedClassNames.contains("SecurityStatistics"), "Should have SecurityStatistics");
      assertTrue(nestedClassNames.contains("AuditEvent"), "Should have AuditEvent");
    }
  }

  // ========================================================================
  // SecurityPolicy Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("SecurityPolicy Nested Class Tests")
  class SecurityPolicyNestedClassTests {

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicyEngine.SecurityPolicy.class.getModifiers()),
          "SecurityPolicy should be public");
    }

    @Test
    @DisplayName("should be static")
    void shouldBeStatic() {
      assertTrue(
          Modifier.isStatic(WasiSecurityPolicyEngine.SecurityPolicy.class.getModifiers()),
          "SecurityPolicy should be static");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityPolicyEngine.SecurityPolicy.class.getModifiers()),
          "SecurityPolicy should be final");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("builder");
      assertNotNull(method, "builder() method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("should have isPathAllowed method")
    void shouldHaveIsPathAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("isPathAllowed", Path.class);
      assertNotNull(method, "isPathAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isOperationAllowed method")
    void shouldHaveIsOperationAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod(
              "isOperationAllowed", WasiFileOperation.class);
      assertNotNull(method, "isOperationAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isEnvironmentVariableAllowed method")
    void shouldHaveIsEnvironmentVariableAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod(
              "isEnvironmentVariableAllowed", String.class);
      assertNotNull(method, "isEnvironmentVariableAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isEnvironmentVariableWritable method")
    void shouldHaveIsEnvironmentVariableWritableMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod(
              "isEnvironmentVariableWritable", String.class);
      assertNotNull(method, "isEnvironmentVariableWritable method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isNetworkAccessAllowed method")
    void shouldHaveIsNetworkAccessAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("isNetworkAccessAllowed");
      assertNotNull(method, "isNetworkAccessAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isHostAllowed method")
    void shouldHaveIsHostAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("isHostAllowed", String.class);
      assertNotNull(method, "isHostAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isPortAllowed method")
    void shouldHaveIsPortAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("isPortAllowed", int.class);
      assertNotNull(method, "isPortAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isProtocolAllowed method")
    void shouldHaveIsProtocolAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod(
              "isProtocolAllowed", String.class);
      assertNotNull(method, "isProtocolAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxFileSize method")
    void shouldHaveGetMaxFileSizeMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("getMaxFileSize");
      assertNotNull(method, "getMaxFileSize method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have areSymbolicLinksAllowed method")
    void shouldHaveAreSymbolicLinksAllowedMethod() throws NoSuchMethodException {
      Method method =
          WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("areSymbolicLinksAllowed");
      assertNotNull(method, "areSymbolicLinksAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isExecuteAllowed method")
    void shouldHaveIsExecuteAllowedMethod() throws NoSuchMethodException {
      Method method = WasiSecurityPolicyEngine.SecurityPolicy.class.getMethod("isExecuteAllowed");
      assertNotNull(method, "isExecuteAllowed method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      Class<?>[] nestedClasses = WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses();
      boolean hasBuilder =
          Arrays.stream(nestedClasses).anyMatch(c -> c.getSimpleName().equals("Builder"));
      assertTrue(hasBuilder, "SecurityPolicy should have Builder nested class");
    }
  }

  // ========================================================================
  // SecurityPolicy.Builder Tests
  // ========================================================================

  @Nested
  @DisplayName("SecurityPolicy.Builder Tests")
  class SecurityPolicyBuilderTests {

    private Class<?> builderClass;

    @Test
    @DisplayName("should have addAllowedDirectory method")
    void shouldHaveAddAllowedDirectoryMethod() throws Exception {
      builderClass =
          Arrays.stream(WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Builder class not found"));

      Method method = builderClass.getMethod("addAllowedDirectory", Path.class);
      assertNotNull(method, "addAllowedDirectory method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(builderClass, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have addAllowedOperation method")
    void shouldHaveAddAllowedOperationMethod() throws Exception {
      builderClass =
          Arrays.stream(WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Builder class not found"));

      Method method = builderClass.getMethod("addAllowedOperation", WasiFileOperation.class);
      assertNotNull(method, "addAllowedOperation method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("should have allowNetworkAccess method")
    void shouldHaveAllowNetworkAccessMethod() throws Exception {
      builderClass =
          Arrays.stream(WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Builder class not found"));

      Method method = builderClass.getMethod("allowNetworkAccess", boolean.class);
      assertNotNull(method, "allowNetworkAccess method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("should have setMaxFileSize method")
    void shouldHaveSetMaxFileSizeMethod() throws Exception {
      builderClass =
          Arrays.stream(WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Builder class not found"));

      Method method = builderClass.getMethod("setMaxFileSize", long.class);
      assertNotNull(method, "setMaxFileSize method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
    }

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws Exception {
      builderClass =
          Arrays.stream(WasiSecurityPolicyEngine.SecurityPolicy.class.getDeclaredClasses())
              .filter(c -> c.getSimpleName().equals("Builder"))
              .findFirst()
              .orElseThrow(() -> new AssertionError("Builder class not found"));

      Method method = builderClass.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(
          WasiSecurityPolicyEngine.SecurityPolicy.class,
          method.getReturnType(),
          "Should return SecurityPolicy");
    }
  }

  // ========================================================================
  // SecurityStatistics Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("SecurityStatistics Nested Class Tests")
  class SecurityStatisticsNestedClassTests {

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicyEngine.SecurityStatistics.class.getModifiers()),
          "SecurityStatistics should be public");
    }

    @Test
    @DisplayName("should be static")
    void shouldBeStatic() {
      assertTrue(
          Modifier.isStatic(WasiSecurityPolicyEngine.SecurityStatistics.class.getModifiers()),
          "SecurityStatistics should be static");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityPolicyEngine.SecurityStatistics.class.getModifiers()),
          "SecurityStatistics should be final");
    }

    @Test
    @DisplayName("should have totalEvents field")
    void shouldHaveTotalEventsField() throws NoSuchFieldException {
      Field field =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getDeclaredField("totalEvents");
      assertNotNull(field, "totalEvents field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have deniedAccesses field")
    void shouldHaveDeniedAccessesField() throws NoSuchFieldException {
      Field field =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getDeclaredField("deniedAccesses");
      assertNotNull(field, "deniedAccesses field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have totalResourceUsage field")
    void shouldHaveTotalResourceUsageField() throws NoSuchFieldException {
      Field field =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getDeclaredField("totalResourceUsage");
      assertNotNull(field, "totalResourceUsage field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have activeContexts field")
    void shouldHaveActiveContextsField() throws NoSuchFieldException {
      Field field =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getDeclaredField("activeContexts");
      assertNotNull(field, "activeContexts field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(int.class, field.getType(), "Should be int");
    }

    @Test
    @DisplayName("should have threatCount field")
    void shouldHaveThreatCountField() throws NoSuchFieldException {
      Field field =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getDeclaredField("threatCount");
      assertNotNull(field, "threatCount field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(long.class, field.getType(), "Should be long");
    }

    @Test
    @DisplayName("should have constructor with all statistics")
    void shouldHaveFullConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiSecurityPolicyEngine.SecurityStatistics.class.getConstructor(
              long.class, long.class, long.class, int.class, long.class);
      assertNotNull(constructor, "Full constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Should be public");
      assertEquals(5, constructor.getParameterCount(), "Should have 5 parameters");
    }
  }

  // ========================================================================
  // AuditEvent Nested Class Tests
  // ========================================================================

  @Nested
  @DisplayName("AuditEvent Nested Class Tests")
  class AuditEventNestedClassTests {

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiSecurityPolicyEngine.AuditEvent.class.getModifiers()),
          "AuditEvent should be public");
    }

    @Test
    @DisplayName("should be static")
    void shouldBeStatic() {
      assertTrue(
          Modifier.isStatic(WasiSecurityPolicyEngine.AuditEvent.class.getModifiers()),
          "AuditEvent should be static");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityPolicyEngine.AuditEvent.class.getModifiers()),
          "AuditEvent should be final");
    }

    @Test
    @DisplayName("should have contextId field")
    void shouldHaveContextIdField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("contextId");
      assertNotNull(field, "contextId field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String");
    }

    @Test
    @DisplayName("should have resource field")
    void shouldHaveResourceField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("resource");
      assertNotNull(field, "resource field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String");
    }

    @Test
    @DisplayName("should have operation field")
    void shouldHaveOperationField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("operation");
      assertNotNull(field, "operation field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String");
    }

    @Test
    @DisplayName("should have timestamp field")
    void shouldHaveTimestampField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("timestamp");
      assertNotNull(field, "timestamp field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(Instant.class, field.getType(), "Should be Instant");
    }

    @Test
    @DisplayName("should have granted field")
    void shouldHaveGrantedField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("granted");
      assertNotNull(field, "granted field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(boolean.class, field.getType(), "Should be boolean");
    }

    @Test
    @DisplayName("should have reason field")
    void shouldHaveReasonField() throws NoSuchFieldException {
      Field field = WasiSecurityPolicyEngine.AuditEvent.class.getDeclaredField("reason");
      assertNotNull(field, "reason field should exist");
      assertTrue(Modifier.isPublic(field.getModifiers()), "Should be public");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(String.class, field.getType(), "Should be String");
    }

    @Test
    @DisplayName("should have constructor with all event properties")
    void shouldHaveFullConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          WasiSecurityPolicyEngine.AuditEvent.class.getConstructor(
              String.class, String.class, String.class, Instant.class, boolean.class, String.class);
      assertNotNull(constructor, "Full constructor should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Should be public");
      assertEquals(6, constructor.getParameterCount(), "Should have 6 parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() {
      long publicMethodCount =
          Arrays.stream(WasiSecurityPolicyEngine.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertTrue(publicMethodCount >= 6, "Should have at least 6 public methods");
    }

    @Test
    @DisplayName("should have expected private methods")
    void shouldHaveExpectedPrivateMethods() {
      long privateMethodCount =
          Arrays.stream(WasiSecurityPolicyEngine.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPrivate(m.getModifiers()))
              .count();
      assertTrue(privateMethodCount >= 4, "Should have at least 4 private methods");
    }
  }
}
