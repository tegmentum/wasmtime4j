package ai.tegmentum.wasmtime4j.jni.wasi.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiFileOperation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiPermissionException.PermissionViolationType;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityPolicyEngine.AuditEvent;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityPolicyEngine.SecurityPolicy;
import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityPolicyEngine.SecurityStatistics;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JNI WASI security classes.
 *
 * <p>This test class validates the API contracts for the following classes in the
 * ai.tegmentum.wasmtime4j.jni.wasi.security package:
 *
 * <ul>
 *   <li>WasiSecurityValidator - Path validation, environment access, resource access
 *   <li>WasiSecurityPolicyEngine - Security policy enforcement and audit logging
 *   <li>WasiFileOperation - File operation types and permissions
 *   <li>WasiErrorCode - WASI errno values and categorization
 *   <li>WasiException - Base WASI exception class
 *   <li>WasiPermissionException - Permission violation exceptions
 *   <li>WasiFileSystemException - File system operation exceptions
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("JNI WASI Security Tests")
class JniWasiSecurityTest {

  // ==================== WasiSecurityValidator Tests ====================

  @Nested
  @DisplayName("WasiSecurityValidator Tests")
  class WasiSecurityValidatorTests {

    @Test
    @DisplayName("should have static defaultValidator method")
    void shouldHaveDefaultValidatorMethod() throws Exception {
      Method method = WasiSecurityValidator.class.getDeclaredMethod("defaultValidator");
      assertTrue(Modifier.isStatic(method.getModifiers()), "defaultValidator should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "defaultValidator should be public");
      assertEquals(
          WasiSecurityValidator.class,
          method.getReturnType(),
          "defaultValidator should return WasiSecurityValidator");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveBuilderMethod() throws Exception {
      Method method = WasiSecurityValidator.class.getDeclaredMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "builder should be public");
      assertEquals(
          WasiSecurityValidator.Builder.class,
          method.getReturnType(),
          "builder should return WasiSecurityValidator.Builder");
    }

    @Test
    @DisplayName("should have validatePath method accepting Path")
    void shouldHaveValidatePathMethod() throws Exception {
      Method method = WasiSecurityValidator.class.getDeclaredMethod("validatePath", Path.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "validatePath should be public");
      assertEquals(void.class, method.getReturnType(), "validatePath should return void");
    }

    @Test
    @DisplayName("should have validateEnvironmentAccess method accepting String")
    void shouldHaveValidateEnvironmentAccessMethod() throws Exception {
      Method method =
          WasiSecurityValidator.class.getDeclaredMethod("validateEnvironmentAccess", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateEnvironmentAccess should be public");
      assertEquals(
          void.class, method.getReturnType(), "validateEnvironmentAccess should return void");
    }

    @Test
    @DisplayName("should have validateResourceAccess method accepting String")
    void shouldHaveValidateResourceAccessMethod() throws Exception {
      Method method =
          WasiSecurityValidator.class.getDeclaredMethod("validateResourceAccess", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateResourceAccess should be public");
      assertEquals(void.class, method.getReturnType(), "validateResourceAccess should return void");
    }

    @Test
    @DisplayName("should return singleton default validator instance")
    void shouldReturnSingletonDefaultValidator() {
      WasiSecurityValidator validator1 = WasiSecurityValidator.defaultValidator();
      WasiSecurityValidator validator2 = WasiSecurityValidator.defaultValidator();
      assertNotNull(validator1, "Default validator should not be null");
      assertSame(validator1, validator2, "Default validator should return singleton instance");
    }

    @Test
    @DisplayName("builder should create new validator instance")
    void builderShouldCreateNewValidatorInstance() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertNotNull(builder, "Builder should not be null");
      WasiSecurityValidator validator = builder.build();
      assertNotNull(validator, "Built validator should not be null");
    }

    @Test
    @DisplayName("builder should return self for method chaining")
    void builderShouldReturnSelfForMethodChaining() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      WasiSecurityValidator.Builder result =
          builder
              .withMaxPathLength(1024)
              .withAllowAbsolutePaths(true)
              .withAllowSymbolicLinks(false)
              .withForbiddenPathComponent("forbidden")
              .withAllowedEnvironmentPattern("^TEST_.*$")
              .withForbiddenEnvironmentName("SECRET");
      assertSame(builder, result, "Builder methods should return self for chaining");
    }

    @Test
    @DisplayName("validatePath should accept valid relative path")
    void validatePathShouldAcceptValidRelativePath() {
      WasiSecurityValidator validator =
          WasiSecurityValidator.builder().withAllowAbsolutePaths(false).build();
      Path validPath = Paths.get("valid", "relative", "path.txt");
      assertDoesNotThrow(
          () -> validator.validatePath(validPath),
          "Valid relative path should not throw exception");
    }

    @Test
    @DisplayName("validatePath should reject null path")
    void validatePathShouldRejectNullPath() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          JniException.class,
          () -> validator.validatePath(null),
          "Null path should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("validatePath should reject path traversal attempts")
    void validatePathShouldRejectPathTraversalAttempts() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      Path traversalPath = Paths.get("..", "parent", "file.txt");
      assertThrows(
          WasiPermissionException.class,
          () -> validator.validatePath(traversalPath),
          "Path traversal attempt should throw WasiPermissionException");
    }

    @Test
    @DisplayName("validatePath should reject absolute paths when disallowed")
    void validatePathShouldRejectAbsolutePathsWhenDisallowed() {
      WasiSecurityValidator validator =
          WasiSecurityValidator.builder().withAllowAbsolutePaths(false).build();
      Path absolutePath = Paths.get("/absolute/path/file.txt");
      // On Windows this might be "C:\\absolute\\path"
      if (absolutePath.isAbsolute()) {
        assertThrows(
            WasiPermissionException.class,
            () -> validator.validatePath(absolutePath),
            "Absolute path should throw when disallowed");
      }
    }

    @Test
    @DisplayName("validatePath should reject paths exceeding max length")
    void validatePathShouldRejectPathsExceedingMaxLength() {
      WasiSecurityValidator validator =
          WasiSecurityValidator.builder().withMaxPathLength(10).build();
      Path longPath = Paths.get("this_path_exceeds_the_maximum_length_configured");
      assertThrows(
          WasiPermissionException.class,
          () -> validator.validatePath(longPath),
          "Path exceeding max length should throw");
    }

    @Test
    @DisplayName("validateEnvironmentAccess should reject null name")
    void validateEnvironmentAccessShouldRejectNullName() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          JniException.class,
          () -> validator.validateEnvironmentAccess(null),
          "Null environment variable name should throw");
    }

    @Test
    @DisplayName("validateEnvironmentAccess should reject empty name")
    void validateEnvironmentAccessShouldRejectEmptyName() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          JniException.class,
          () -> validator.validateEnvironmentAccess(""),
          "Empty environment variable name should throw");
    }

    @Test
    @DisplayName("validateEnvironmentAccess should reject forbidden environment variables")
    void validateEnvironmentAccessShouldRejectForbiddenEnvVars() {
      WasiSecurityValidator validator =
          WasiSecurityValidator.builder().withForbiddenEnvironmentName("FORBIDDEN_VAR").build();
      assertThrows(
          WasiPermissionException.class,
          () -> validator.validateEnvironmentAccess("FORBIDDEN_VAR"),
          "Forbidden environment variable should throw");
    }

    @Test
    @DisplayName("validateResourceAccess should reject null resource ID")
    void validateResourceAccessShouldRejectNullResourceId() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess(null),
          "Null resource ID should throw");
    }

    @Test
    @DisplayName("validateResourceAccess should reject empty resource ID")
    void validateResourceAccessShouldRejectEmptyResourceId() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          JniException.class,
          () -> validator.validateResourceAccess(""),
          "Empty resource ID should throw");
    }

    @Test
    @DisplayName("validateResourceAccess should reject resource ID with null bytes")
    void validateResourceAccessShouldRejectResourceIdWithNullBytes() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      assertThrows(
          WasiPermissionException.class,
          () -> validator.validateResourceAccess("resource\0injection"),
          "Resource ID with null bytes should throw");
    }

    @Test
    @DisplayName("validateResourceAccess should reject overly long resource ID")
    void validateResourceAccessShouldRejectOverlyLongResourceId() {
      WasiSecurityValidator validator = WasiSecurityValidator.defaultValidator();
      // Java 8 compatible: build a 256-char string
      StringBuilder sb = new StringBuilder(256);
      for (int i = 0; i < 256; i++) {
        sb.append('a');
      }
      String longResourceId = sb.toString();
      assertThrows(
          WasiPermissionException.class,
          () -> validator.validateResourceAccess(longResourceId),
          "Resource ID exceeding 255 chars should throw");
    }

    @Test
    @DisplayName("Builder should allow Pattern for allowed environment patterns")
    void builderShouldAllowPatternForAllowedEnvironmentPatterns() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      Pattern pattern = Pattern.compile("^ALLOWED_.*$");
      assertDoesNotThrow(
          () -> builder.withAllowedEnvironmentPattern(pattern),
          "Builder should accept Pattern for allowed environment patterns");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityValidator.class.getModifiers()),
          "WasiSecurityValidator should be final");
    }
  }

  // ==================== WasiSecurityValidator.Builder Tests ====================

  @Nested
  @DisplayName("WasiSecurityValidator.Builder Tests")
  class WasiSecurityValidatorBuilderTests {

    @Test
    @DisplayName("Builder should be final class")
    void builderShouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiSecurityValidator.Builder.class.getModifiers()),
          "Builder should be final class");
    }

    @Test
    @DisplayName("Builder should have withMaxPathLength method")
    void builderShouldHaveWithMaxPathLengthMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod("withMaxPathLength", int.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "withMaxPathLength should be public");
      assertEquals(
          WasiSecurityValidator.Builder.class,
          method.getReturnType(),
          "withMaxPathLength should return Builder");
    }

    @Test
    @DisplayName("Builder should have withAllowAbsolutePaths method")
    void builderShouldHaveWithAllowAbsolutePathsMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withAllowAbsolutePaths", boolean.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "withAllowAbsolutePaths should be public");
    }

    @Test
    @DisplayName("Builder should have withAllowSymbolicLinks method")
    void builderShouldHaveWithAllowSymbolicLinksMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withAllowSymbolicLinks", boolean.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "withAllowSymbolicLinks should be public");
    }

    @Test
    @DisplayName("Builder should have withForbiddenPathComponent method")
    void builderShouldHaveWithForbiddenPathComponentMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withForbiddenPathComponent", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "withForbiddenPathComponent should be public");
    }

    @Test
    @DisplayName("Builder should have withAllowedEnvironmentPattern(Pattern) method")
    void builderShouldHaveWithAllowedEnvironmentPatternPatternMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withAllowedEnvironmentPattern", Pattern.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "withAllowedEnvironmentPattern(Pattern) should be public");
    }

    @Test
    @DisplayName("Builder should have withAllowedEnvironmentPattern(String) method")
    void builderShouldHaveWithAllowedEnvironmentPatternStringMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withAllowedEnvironmentPattern", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "withAllowedEnvironmentPattern(String) should be public");
    }

    @Test
    @DisplayName("Builder should have withForbiddenEnvironmentName method")
    void builderShouldHaveWithForbiddenEnvironmentNameMethod() throws Exception {
      Method method =
          WasiSecurityValidator.Builder.class.getDeclaredMethod(
              "withForbiddenEnvironmentName", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "withForbiddenEnvironmentName should be public");
    }

    @Test
    @DisplayName("Builder should have build method")
    void builderShouldHaveBuildMethod() throws Exception {
      Method method = WasiSecurityValidator.Builder.class.getDeclaredMethod("build");
      assertTrue(Modifier.isPublic(method.getModifiers()), "build should be public");
      assertEquals(
          WasiSecurityValidator.class,
          method.getReturnType(),
          "build should return WasiSecurityValidator");
    }

    @Test
    @DisplayName("withMaxPathLength should reject non-positive values")
    void withMaxPathLengthShouldRejectNonPositiveValues() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withMaxPathLength(0),
          "Zero max path length should throw");
      assertThrows(
          JniException.class,
          () -> builder.withMaxPathLength(-1),
          "Negative max path length should throw");
    }

    @Test
    @DisplayName("withForbiddenPathComponent should reject null")
    void withForbiddenPathComponentShouldRejectNull() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withForbiddenPathComponent(null),
          "Null forbidden path component should throw");
    }

    @Test
    @DisplayName("withForbiddenPathComponent should reject empty string")
    void withForbiddenPathComponentShouldRejectEmptyString() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withForbiddenPathComponent(""),
          "Empty forbidden path component should throw");
    }

    @Test
    @DisplayName("withAllowedEnvironmentPattern(Pattern) should reject null")
    void withAllowedEnvironmentPatternPatternShouldRejectNull() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withAllowedEnvironmentPattern((Pattern) null),
          "Null pattern should throw");
    }

    @Test
    @DisplayName("withAllowedEnvironmentPattern(String) should reject null")
    void withAllowedEnvironmentPatternStringShouldRejectNull() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withAllowedEnvironmentPattern((String) null),
          "Null pattern string should throw");
    }

    @Test
    @DisplayName("withAllowedEnvironmentPattern(String) should reject empty string")
    void withAllowedEnvironmentPatternStringShouldRejectEmptyString() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withAllowedEnvironmentPattern(""),
          "Empty pattern string should throw");
    }

    @Test
    @DisplayName("withForbiddenEnvironmentName should reject null")
    void withForbiddenEnvironmentNameShouldRejectNull() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withForbiddenEnvironmentName(null),
          "Null forbidden environment name should throw");
    }

    @Test
    @DisplayName("withForbiddenEnvironmentName should reject empty string")
    void withForbiddenEnvironmentNameShouldRejectEmptyString() {
      WasiSecurityValidator.Builder builder = WasiSecurityValidator.builder();
      assertThrows(
          JniException.class,
          () -> builder.withForbiddenEnvironmentName(""),
          "Empty forbidden environment name should throw");
    }
  }

  // ==================== WasiSecurityPolicyEngine Tests ====================

  @Nested
  @DisplayName("WasiSecurityPolicyEngine Tests")
  class WasiSecurityPolicyEngineTests {

    @Test
    @DisplayName("constructor should reject null security policy")
    void constructorShouldRejectNullSecurityPolicy() {
      assertThrows(
          JniException.class,
          () -> new WasiSecurityPolicyEngine(null),
          "Null security policy should throw");
    }

    @Test
    @DisplayName("constructor should accept valid security policy")
    void constructorShouldAcceptValidSecurityPolicy() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      assertDoesNotThrow(
          () -> new WasiSecurityPolicyEngine(policy), "Valid security policy should not throw");
    }

    @Test
    @DisplayName("should have validateFileSystemAccess method")
    void shouldHaveValidateFileSystemAccessMethod() throws Exception {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateFileSystemAccess", Path.class, WasiFileOperation.class, String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateFileSystemAccess should be public");
    }

    @Test
    @DisplayName("should have validateEnvironmentAccess method")
    void shouldHaveValidateEnvironmentAccessMethod() throws Exception {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateEnvironmentAccess", String.class, String.class, String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateEnvironmentAccess should be public");
    }

    @Test
    @DisplayName("should have validateNetworkAccess method")
    void shouldHaveValidateNetworkAccessMethod() throws Exception {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "validateNetworkAccess", String.class, int.class, String.class, String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "validateNetworkAccess should be public");
    }

    @Test
    @DisplayName("should have getSecurityStatistics method")
    void shouldHaveGetSecurityStatisticsMethod() throws Exception {
      Method method = WasiSecurityPolicyEngine.class.getDeclaredMethod("getSecurityStatistics");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "getSecurityStatistics should be public");
      assertEquals(
          SecurityStatistics.class,
          method.getReturnType(),
          "getSecurityStatistics should return SecurityStatistics");
    }

    @Test
    @DisplayName("should have getRecentAuditEvents method")
    void shouldHaveGetRecentAuditEventsMethod() throws Exception {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod("getRecentAuditEvents", int.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "getRecentAuditEvents should be public");
      assertEquals(List.class, method.getReturnType(), "getRecentAuditEvents should return List");
    }

    @Test
    @DisplayName("should have updateSecurityPolicy method")
    void shouldHaveUpdateSecurityPolicyMethod() throws Exception {
      Method method =
          WasiSecurityPolicyEngine.class.getDeclaredMethod(
              "updateSecurityPolicy", SecurityPolicy.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "updateSecurityPolicy should be public");
    }

    @Test
    @DisplayName("validateFileSystemAccess should reject null path")
    void validateFileSystemAccessShouldRejectNullPath() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          JniException.class,
          () -> engine.validateFileSystemAccess(null, WasiFileOperation.READ, "ctx1"),
          "Null path should throw");
    }

    @Test
    @DisplayName("validateFileSystemAccess should reject null operation")
    void validateFileSystemAccessShouldRejectNullOperation() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      Path path = Paths.get("test.txt");
      assertThrows(
          JniException.class,
          () -> engine.validateFileSystemAccess(path, null, "ctx1"),
          "Null operation should throw");
    }

    @Test
    @DisplayName("validateFileSystemAccess should reject empty contextId")
    void validateFileSystemAccessShouldRejectEmptyContextId() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      Path path = Paths.get("test.txt");
      assertThrows(
          JniException.class,
          () -> engine.validateFileSystemAccess(path, WasiFileOperation.READ, ""),
          "Empty contextId should throw");
    }

    @Test
    @DisplayName("validateEnvironmentAccess should reject empty variable name")
    void validateEnvironmentAccessShouldRejectEmptyVariableName() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          JniException.class,
          () -> engine.validateEnvironmentAccess("", "read", "ctx1"),
          "Empty variable name should throw");
    }

    @Test
    @DisplayName("validateNetworkAccess should reject empty host")
    void validateNetworkAccessShouldRejectEmptyHost() {
      SecurityPolicy policy = SecurityPolicy.builder().allowNetworkAccess(true).build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          JniException.class,
          () -> engine.validateNetworkAccess("", 80, "TCP", "ctx1"),
          "Empty host should throw");
    }

    @Test
    @DisplayName("validateNetworkAccess should reject forbidden protocol")
    void validateNetworkAccessShouldRejectWhenNetworkAccessDisabled() {
      SecurityPolicy policy = SecurityPolicy.builder().allowNetworkAccess(false).build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          WasiPermissionException.class,
          () -> engine.validateNetworkAccess("localhost", 80, "TCP", "ctx1"),
          "Network access when disabled should throw");
    }

    @Test
    @DisplayName("getSecurityStatistics should return valid statistics")
    void getSecurityStatisticsShouldReturnValidStatistics() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      SecurityStatistics stats = engine.getSecurityStatistics();
      assertNotNull(stats, "Security statistics should not be null");
    }

    @Test
    @DisplayName("getRecentAuditEvents should reject non-positive maxEvents")
    void getRecentAuditEventsShouldRejectNonPositiveMaxEvents() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          JniException.class, () -> engine.getRecentAuditEvents(0), "Zero maxEvents should throw");
      assertThrows(
          JniException.class,
          () -> engine.getRecentAuditEvents(-1),
          "Negative maxEvents should throw");
    }

    @Test
    @DisplayName("updateSecurityPolicy should reject null policy")
    void updateSecurityPolicyShouldRejectNullPolicy() {
      SecurityPolicy policy = SecurityPolicy.builder().build();
      WasiSecurityPolicyEngine engine = new WasiSecurityPolicyEngine(policy);
      assertThrows(
          JniException.class, () -> engine.updateSecurityPolicy(null), "Null policy should throw");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiSecurityPolicyEngine.class.getModifiers()),
          "WasiSecurityPolicyEngine should be final");
    }
  }

  // ==================== SecurityPolicy Tests ====================

  @Nested
  @DisplayName("SecurityPolicy Tests")
  class SecurityPolicyTests {

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("builder");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "builder should be public");
    }

    @Test
    @DisplayName("should have isPathAllowed method")
    void shouldHaveIsPathAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isPathAllowed", Path.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "isPathAllowed should be public");
      assertEquals(boolean.class, method.getReturnType(), "isPathAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isOperationAllowed method")
    void shouldHaveIsOperationAllowedMethod() throws Exception {
      Method method =
          SecurityPolicy.class.getDeclaredMethod("isOperationAllowed", WasiFileOperation.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "isOperationAllowed should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isOperationAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isEnvironmentVariableAllowed method")
    void shouldHaveIsEnvironmentVariableAllowedMethod() throws Exception {
      Method method =
          SecurityPolicy.class.getDeclaredMethod("isEnvironmentVariableAllowed", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "isEnvironmentVariableAllowed should be public");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isEnvironmentVariableAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isEnvironmentVariableWritable method")
    void shouldHaveIsEnvironmentVariableWritableMethod() throws Exception {
      Method method =
          SecurityPolicy.class.getDeclaredMethod("isEnvironmentVariableWritable", String.class);
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "isEnvironmentVariableWritable should be public");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isEnvironmentVariableWritable should return boolean");
    }

    @Test
    @DisplayName("should have isNetworkAccessAllowed method")
    void shouldHaveIsNetworkAccessAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isNetworkAccessAllowed");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isNetworkAccessAllowed should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isNetworkAccessAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isHostAllowed method")
    void shouldHaveIsHostAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isHostAllowed", String.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "isHostAllowed should be public");
      assertEquals(boolean.class, method.getReturnType(), "isHostAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isPortAllowed method")
    void shouldHaveIsPortAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isPortAllowed", int.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "isPortAllowed should be public");
      assertEquals(boolean.class, method.getReturnType(), "isPortAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isProtocolAllowed method")
    void shouldHaveIsProtocolAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isProtocolAllowed", String.class);
      assertTrue(Modifier.isPublic(method.getModifiers()), "isProtocolAllowed should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isProtocolAllowed should return boolean");
    }

    @Test
    @DisplayName("should have getMaxFileSize method")
    void shouldHaveGetMaxFileSizeMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("getMaxFileSize");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getMaxFileSize should be public");
      assertEquals(long.class, method.getReturnType(), "getMaxFileSize should return long");
    }

    @Test
    @DisplayName("should have areSymbolicLinksAllowed method")
    void shouldHaveAreSymbolicLinksAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("areSymbolicLinksAllowed");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "areSymbolicLinksAllowed should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "areSymbolicLinksAllowed should return boolean");
    }

    @Test
    @DisplayName("should have isExecuteAllowed method")
    void shouldHaveIsExecuteAllowedMethod() throws Exception {
      Method method = SecurityPolicy.class.getDeclaredMethod("isExecuteAllowed");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isExecuteAllowed should be public");
      assertEquals(boolean.class, method.getReturnType(), "isExecuteAllowed should return boolean");
    }

    @Test
    @DisplayName("builder should configure network access")
    void builderShouldConfigureNetworkAccess() {
      SecurityPolicy policy = SecurityPolicy.builder().allowNetworkAccess(true).build();
      assertTrue(
          policy.isNetworkAccessAllowed(), "Network access should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure allowed hosts")
    void builderShouldConfigureAllowedHosts() {
      SecurityPolicy policy = SecurityPolicy.builder().addAllowedHost("localhost").build();
      assertTrue(policy.isHostAllowed("localhost"), "localhost should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure allowed ports")
    void builderShouldConfigureAllowedPorts() {
      SecurityPolicy policy = SecurityPolicy.builder().addAllowedPort(8080).build();
      assertTrue(policy.isPortAllowed(8080), "Port 8080 should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure allowed protocols")
    void builderShouldConfigureAllowedProtocols() {
      SecurityPolicy policy = SecurityPolicy.builder().addAllowedProtocol("TCP").build();
      assertTrue(policy.isProtocolAllowed("TCP"), "TCP should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure max file size")
    void builderShouldConfigureMaxFileSize() {
      SecurityPolicy policy = SecurityPolicy.builder().setMaxFileSize(1024 * 1024).build();
      assertEquals(
          1024 * 1024, policy.getMaxFileSize(), "Max file size should match configured value");
    }

    @Test
    @DisplayName("builder should configure symbolic links")
    void builderShouldConfigureSymbolicLinks() {
      SecurityPolicy policy = SecurityPolicy.builder().allowSymbolicLinks(true).build();
      assertTrue(
          policy.areSymbolicLinksAllowed(), "Symbolic links should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure execute permission")
    void builderShouldConfigureExecutePermission() {
      SecurityPolicy policy = SecurityPolicy.builder().allowExecute(true).build();
      assertTrue(policy.isExecuteAllowed(), "Execute should be allowed when configured");
    }

    @Test
    @DisplayName("builder should configure allowed directories")
    void builderShouldConfigureAllowedDirectories() {
      Path allowedDir = Paths.get("/allowed/directory");
      SecurityPolicy policy = SecurityPolicy.builder().addAllowedDirectory(allowedDir).build();
      assertTrue(
          policy.isPathAllowed(allowedDir.resolve("file.txt")),
          "Path within allowed directory should be allowed");
    }

    @Test
    @DisplayName("builder should configure allowed operations")
    void builderShouldConfigureAllowedOperations() {
      SecurityPolicy policy =
          SecurityPolicy.builder().addAllowedOperation(WasiFileOperation.READ).build();
      assertTrue(
          policy.isOperationAllowed(WasiFileOperation.READ),
          "READ operation should be allowed when configured");
      assertFalse(
          policy.isOperationAllowed(WasiFileOperation.WRITE),
          "WRITE operation should not be allowed when not configured");
    }

    @Test
    @DisplayName("builder should configure allowed environment variables")
    void builderShouldConfigureAllowedEnvironmentVariables() {
      SecurityPolicy policy =
          SecurityPolicy.builder().addAllowedEnvironmentVariable("HOME").build();
      assertTrue(
          policy.isEnvironmentVariableAllowed("HOME"), "HOME should be allowed when configured");
      assertFalse(
          policy.isEnvironmentVariableAllowed("SECRET"),
          "SECRET should not be allowed when not configured");
    }

    @Test
    @DisplayName("builder should configure writable environment variables")
    void builderShouldConfigureWritableEnvironmentVariables() {
      SecurityPolicy policy =
          SecurityPolicy.builder().addWritableEnvironmentVariable("MY_VAR").build();
      assertTrue(
          policy.isEnvironmentVariableWritable("MY_VAR"),
          "MY_VAR should be writable when configured");
      assertTrue(
          policy.isEnvironmentVariableAllowed("MY_VAR"),
          "MY_VAR should also be readable when configured as writable");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(SecurityPolicy.class.getModifiers()), "SecurityPolicy should be final");
    }
  }

  // ==================== SecurityStatistics Tests ====================

  @Nested
  @DisplayName("SecurityStatistics Tests")
  class SecurityStatisticsTests {

    @Test
    @DisplayName("should have public final fields")
    void shouldHavePublicFinalFields() throws Exception {
      assertTrue(
          Modifier.isPublic(
              SecurityStatistics.class.getDeclaredField("totalEvents").getModifiers()),
          "totalEvents should be public");
      assertTrue(
          Modifier.isPublic(
              SecurityStatistics.class.getDeclaredField("deniedAccesses").getModifiers()),
          "deniedAccesses should be public");
      assertTrue(
          Modifier.isPublic(
              SecurityStatistics.class.getDeclaredField("totalResourceUsage").getModifiers()),
          "totalResourceUsage should be public");
      assertTrue(
          Modifier.isPublic(
              SecurityStatistics.class.getDeclaredField("activeContexts").getModifiers()),
          "activeContexts should be public");
      assertTrue(
          Modifier.isPublic(
              SecurityStatistics.class.getDeclaredField("threatCount").getModifiers()),
          "threatCount should be public");
    }

    @Test
    @DisplayName("constructor should initialize all fields")
    void constructorShouldInitializeAllFields() {
      SecurityStatistics stats = new SecurityStatistics(100, 10, 500, 5, 2);
      assertEquals(100, stats.totalEvents, "totalEvents should be 100");
      assertEquals(10, stats.deniedAccesses, "deniedAccesses should be 10");
      assertEquals(500, stats.totalResourceUsage, "totalResourceUsage should be 500");
      assertEquals(5, stats.activeContexts, "activeContexts should be 5");
      assertEquals(2, stats.threatCount, "threatCount should be 2");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(SecurityStatistics.class.getModifiers()),
          "SecurityStatistics should be final");
    }
  }

  // ==================== AuditEvent Tests ====================

  @Nested
  @DisplayName("AuditEvent Tests")
  class AuditEventTests {

    @Test
    @DisplayName("should have public final fields")
    void shouldHavePublicFinalFields() throws Exception {
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("contextId").getModifiers()),
          "contextId should be public");
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("resource").getModifiers()),
          "resource should be public");
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("operation").getModifiers()),
          "operation should be public");
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("timestamp").getModifiers()),
          "timestamp should be public");
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("granted").getModifiers()),
          "granted should be public");
      assertTrue(
          Modifier.isPublic(AuditEvent.class.getDeclaredField("reason").getModifiers()),
          "reason should be public");
    }

    @Test
    @DisplayName("constructor should initialize all fields")
    void constructorShouldInitializeAllFields() {
      Instant now = Instant.now();
      AuditEvent event = new AuditEvent("ctx1", "/file.txt", "read", now, true, "allowed");
      assertEquals("ctx1", event.contextId, "contextId should be 'ctx1'");
      assertEquals("/file.txt", event.resource, "resource should be '/file.txt'");
      assertEquals("read", event.operation, "operation should be 'read'");
      assertEquals(now, event.timestamp, "timestamp should match");
      assertTrue(event.granted, "granted should be true");
      assertEquals("allowed", event.reason, "reason should be 'allowed'");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(Modifier.isFinal(AuditEvent.class.getModifiers()), "AuditEvent should be final");
    }
  }

  // ==================== WasiFileOperation Tests ====================

  @Nested
  @DisplayName("WasiFileOperation Tests")
  class WasiFileOperationTests {

    @Test
    @DisplayName("should have all expected enum values")
    void shouldHaveAllExpectedEnumValues() {
      WasiFileOperation[] values = WasiFileOperation.values();
      assertEquals(23, values.length, "Should have 23 file operation values");

      List<String> expectedValues =
          Arrays.asList(
              "READ",
              "WRITE",
              "EXECUTE",
              "CREATE_DIRECTORY",
              "DELETE",
              "RENAME",
              "METADATA",
              "CHANGE_PERMISSIONS",
              "CREATE_LINK",
              "FOLLOW_SYMLINKS",
              "LIST_DIRECTORY",
              "SET_TIMES",
              "TRUNCATE",
              "SYNC",
              "SEEK",
              "POLL",
              "OPEN",
              "CLOSE",
              "READ_ONLY",
              "WRITE_ONLY",
              "READ_WRITE",
              "SET_PERMISSIONS",
              "READ_LINK");

      for (String expected : expectedValues) {
        assertNotNull(WasiFileOperation.valueOf(expected), "Should have " + expected + " value");
      }
    }

    @Test
    @DisplayName("should have getOperationId method")
    void shouldHaveGetOperationIdMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("getOperationId");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getOperationId should be public");
      assertEquals(String.class, method.getReturnType(), "getOperationId should return String");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("getDescription");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getDescription should be public");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("should have requiresReadAccess method")
    void shouldHaveRequiresReadAccessMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("requiresReadAccess");
      assertTrue(Modifier.isPublic(method.getModifiers()), "requiresReadAccess should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "requiresReadAccess should return boolean");
    }

    @Test
    @DisplayName("should have requiresWriteAccess method")
    void shouldHaveRequiresWriteAccessMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("requiresWriteAccess");
      assertTrue(Modifier.isPublic(method.getModifiers()), "requiresWriteAccess should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "requiresWriteAccess should return boolean");
    }

    @Test
    @DisplayName("should have requiresExecuteAccess method")
    void shouldHaveRequiresExecuteAccessMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("requiresExecuteAccess");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "requiresExecuteAccess should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "requiresExecuteAccess should return boolean");
    }

    @Test
    @DisplayName("should have isWriteOperation method")
    void shouldHaveIsWriteOperationMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("isWriteOperation");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isWriteOperation should be public");
      assertEquals(boolean.class, method.getReturnType(), "isWriteOperation should return boolean");
    }

    @Test
    @DisplayName("should have isModifyingOperation method")
    void shouldHaveIsModifyingOperationMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("isModifyingOperation");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isModifyingOperation should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isModifyingOperation should return boolean");
    }

    @Test
    @DisplayName("should have isDangerous method")
    void shouldHaveIsDangerousMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("isDangerous");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isDangerous should be public");
      assertEquals(boolean.class, method.getReturnType(), "isDangerous should return boolean");
    }

    @Test
    @DisplayName("should have static fromOperationId method")
    void shouldHaveFromOperationIdMethod() throws Exception {
      Method method = WasiFileOperation.class.getDeclaredMethod("fromOperationId", String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromOperationId should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromOperationId should be public");
      assertEquals(
          WasiFileOperation.class,
          method.getReturnType(),
          "fromOperationId should return WasiFileOperation");
    }

    @Test
    @DisplayName("READ should return correct properties")
    void readShouldReturnCorrectProperties() {
      WasiFileOperation read = WasiFileOperation.READ;
      assertEquals("read", read.getOperationId(), "Operation ID should be 'read'");
      assertNotNull(read.getDescription(), "Description should not be null");
      assertTrue(read.requiresReadAccess(), "READ should require read access");
      assertFalse(read.requiresWriteAccess(), "READ should not require write access");
      assertFalse(read.isDangerous(), "READ should not be dangerous");
    }

    @Test
    @DisplayName("WRITE should return correct properties")
    void writeShouldReturnCorrectProperties() {
      WasiFileOperation write = WasiFileOperation.WRITE;
      assertEquals("write", write.getOperationId(), "Operation ID should be 'write'");
      assertTrue(write.requiresWriteAccess(), "WRITE should require write access");
      assertTrue(write.isWriteOperation(), "WRITE should be a write operation");
    }

    @Test
    @DisplayName("EXECUTE should return correct properties")
    void executeShouldReturnCorrectProperties() {
      WasiFileOperation execute = WasiFileOperation.EXECUTE;
      assertEquals("execute", execute.getOperationId(), "Operation ID should be 'execute'");
      assertTrue(execute.requiresExecuteAccess(), "EXECUTE should require execute access");
      assertTrue(execute.isDangerous(), "EXECUTE should be dangerous");
    }

    @Test
    @DisplayName("DELETE should be dangerous and modifying")
    void deleteShouldBeDangerousAndModifying() {
      WasiFileOperation delete = WasiFileOperation.DELETE;
      assertTrue(delete.isDangerous(), "DELETE should be dangerous");
      assertTrue(delete.isModifyingOperation(), "DELETE should be modifying");
    }

    @Test
    @DisplayName("fromOperationId should return correct enum value")
    void fromOperationIdShouldReturnCorrectEnumValue() {
      assertEquals(
          WasiFileOperation.READ,
          WasiFileOperation.fromOperationId("read"),
          "Should return READ for 'read'");
      assertEquals(
          WasiFileOperation.WRITE,
          WasiFileOperation.fromOperationId("write"),
          "Should return WRITE for 'write'");
      assertEquals(
          WasiFileOperation.EXECUTE,
          WasiFileOperation.fromOperationId("execute"),
          "Should return EXECUTE for 'execute'");
    }

    @Test
    @DisplayName("fromOperationId should throw for null")
    void fromOperationIdShouldThrowForNull() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileOperation.fromOperationId(null),
          "Null operation ID should throw");
    }

    @Test
    @DisplayName("fromOperationId should throw for empty string")
    void fromOperationIdShouldThrowForEmptyString() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileOperation.fromOperationId(""),
          "Empty operation ID should throw");
    }

    @Test
    @DisplayName("fromOperationId should throw for unknown operation ID")
    void fromOperationIdShouldThrowForUnknownOperationId() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiFileOperation.fromOperationId("unknown_operation"),
          "Unknown operation ID should throw");
    }

    @Test
    @DisplayName("toString should contain name and description")
    void toStringShouldContainNameAndDescription() {
      WasiFileOperation read = WasiFileOperation.READ;
      String str = read.toString();
      assertTrue(str.contains("READ"), "toString should contain enum name");
    }

    @Test
    @DisplayName("READ_WRITE should require both read and write access")
    void readWriteShouldRequireBothReadAndWriteAccess() {
      WasiFileOperation readWrite = WasiFileOperation.READ_WRITE;
      assertTrue(readWrite.requiresReadAccess(), "READ_WRITE should require read access");
      assertTrue(readWrite.requiresWriteAccess(), "READ_WRITE should require write access");
    }
  }

  // ==================== WasiErrorCode Tests ====================

  @Nested
  @DisplayName("WasiErrorCode Tests")
  class WasiErrorCodeTests {

    @Test
    @DisplayName("should have expected error code values")
    void shouldHaveExpectedErrorCodeValues() {
      assertNotNull(WasiErrorCode.SUCCESS, "Should have SUCCESS");
      assertNotNull(WasiErrorCode.EPERM, "Should have EPERM");
      assertNotNull(WasiErrorCode.ENOENT, "Should have ENOENT");
      assertNotNull(WasiErrorCode.EIO, "Should have EIO");
      assertNotNull(WasiErrorCode.EBADF, "Should have EBADF");
      assertNotNull(WasiErrorCode.EACCES, "Should have EACCES");
      assertNotNull(WasiErrorCode.EEXIST, "Should have EEXIST");
      assertNotNull(WasiErrorCode.EINVAL, "Should have EINVAL");
      assertNotNull(WasiErrorCode.ENOMEM, "Should have ENOMEM");
      assertNotNull(WasiErrorCode.UNKNOWN, "Should have UNKNOWN");
    }

    @Test
    @DisplayName("should have getErrno method")
    void shouldHaveGetErrnoMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("getErrno");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getErrno should be public");
      assertEquals(int.class, method.getReturnType(), "getErrno should return int");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("getDescription");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getDescription should be public");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("should have isFileSystemError method")
    void shouldHaveIsFileSystemErrorMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("isFileSystemError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isFileSystemError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isFileSystemError should return boolean");
    }

    @Test
    @DisplayName("should have isNetworkError method")
    void shouldHaveIsNetworkErrorMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("isNetworkError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isNetworkError should be public");
      assertEquals(boolean.class, method.getReturnType(), "isNetworkError should return boolean");
    }

    @Test
    @DisplayName("should have isPermissionError method")
    void shouldHaveIsPermissionErrorMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("isPermissionError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isPermissionError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isPermissionError should return boolean");
    }

    @Test
    @DisplayName("should have isResourceLimitError method")
    void shouldHaveIsResourceLimitErrorMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("isResourceLimitError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isResourceLimitError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isResourceLimitError should return boolean");
    }

    @Test
    @DisplayName("should have isRetryable method")
    void shouldHaveIsRetryableMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("isRetryable");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isRetryable should be public");
      assertEquals(boolean.class, method.getReturnType(), "isRetryable should return boolean");
    }

    @Test
    @DisplayName("should have static fromErrno method")
    void shouldHaveFromErrnoMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("fromErrno", int.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromErrno should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromErrno should be public");
      assertEquals(
          WasiErrorCode.class, method.getReturnType(), "fromErrno should return WasiErrorCode");
    }

    @Test
    @DisplayName("should have static fromErrnoOrNull method")
    void shouldHaveFromErrnoOrNullMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("fromErrnoOrNull", int.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fromErrnoOrNull should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fromErrnoOrNull should be public");
    }

    @Test
    @DisplayName("should have static createGeneric method")
    void shouldHaveCreateGenericMethod() throws Exception {
      Method method = WasiErrorCode.class.getDeclaredMethod("createGeneric", int.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "createGeneric should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "createGeneric should be public");
    }

    @Test
    @DisplayName("SUCCESS should have errno 0")
    void successShouldHaveErrnoZero() {
      assertEquals(0, WasiErrorCode.SUCCESS.getErrno(), "SUCCESS should have errno 0");
    }

    @Test
    @DisplayName("EPERM should be a permission error")
    void epermShouldBePermissionError() {
      assertTrue(WasiErrorCode.EPERM.isPermissionError(), "EPERM should be permission error");
      assertEquals(1, WasiErrorCode.EPERM.getErrno(), "EPERM should have errno 1");
    }

    @Test
    @DisplayName("ENOENT should be a file system error")
    void enoentShouldBeFileSystemError() {
      assertTrue(WasiErrorCode.ENOENT.isFileSystemError(), "ENOENT should be file system error");
      assertEquals(2, WasiErrorCode.ENOENT.getErrno(), "ENOENT should have errno 2");
      assertTrue(WasiErrorCode.ENOENT.isRetryable(), "ENOENT should be retryable");
    }

    @Test
    @DisplayName("ECONNREFUSED should be a network error")
    void econnrefusedShouldBeNetworkError() {
      assertTrue(
          WasiErrorCode.ECONNREFUSED.isNetworkError(), "ECONNREFUSED should be network error");
      assertTrue(WasiErrorCode.ECONNREFUSED.isRetryable(), "ECONNREFUSED should be retryable");
    }

    @Test
    @DisplayName("ENOMEM should be a resource limit error")
    void enomemShouldBeResourceLimitError() {
      assertTrue(
          WasiErrorCode.ENOMEM.isResourceLimitError(), "ENOMEM should be resource limit error");
      assertTrue(WasiErrorCode.ENOMEM.isRetryable(), "ENOMEM should be retryable");
    }

    @Test
    @DisplayName("fromErrno should return correct error code")
    void fromErrnoShouldReturnCorrectErrorCode() {
      assertEquals(
          WasiErrorCode.SUCCESS, WasiErrorCode.fromErrno(0), "fromErrno(0) should return SUCCESS");
      assertEquals(
          WasiErrorCode.EPERM, WasiErrorCode.fromErrno(1), "fromErrno(1) should return EPERM");
      assertEquals(
          WasiErrorCode.ENOENT, WasiErrorCode.fromErrno(2), "fromErrno(2) should return ENOENT");
    }

    @Test
    @DisplayName("fromErrno should throw for unknown errno")
    void fromErrnoShouldThrowForUnknownErrno() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiErrorCode.fromErrno(9999),
          "Unknown errno should throw");
    }

    @Test
    @DisplayName("fromErrnoOrNull should return null for unknown errno")
    void fromErrnoOrNullShouldReturnNullForUnknownErrno() {
      assertNull(WasiErrorCode.fromErrnoOrNull(9999), "Unknown errno should return null");
    }

    @Test
    @DisplayName("createGeneric should return UNKNOWN")
    void createGenericShouldReturnUnknown() {
      assertEquals(
          WasiErrorCode.UNKNOWN,
          WasiErrorCode.createGeneric(9999),
          "createGeneric should return UNKNOWN");
    }

    @Test
    @DisplayName("toString should contain name and description")
    void toStringShouldContainNameAndDescription() {
      String str = WasiErrorCode.ENOENT.toString();
      assertTrue(str.contains("ENOENT"), "toString should contain enum name");
      assertTrue(str.contains("2"), "toString should contain errno");
    }
  }

  // ==================== WasiException Tests ====================

  @Nested
  @DisplayName("WasiException Tests")
  class WasiExceptionTests {

    @Test
    @DisplayName("should extend RuntimeException hierarchy")
    void shouldExtendRuntimeExceptionHierarchy() {
      assertTrue(
          RuntimeException.class.isAssignableFrom(WasiException.class),
          "WasiException should extend RuntimeException hierarchy");
    }

    @Test
    @DisplayName("should have getErrorCode method")
    void shouldHaveGetErrorCodeMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("getErrorCode");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getErrorCode should be public");
      assertEquals(
          WasiErrorCode.class, method.getReturnType(), "getErrorCode should return WasiErrorCode");
    }

    @Test
    @DisplayName("should have getOperation method")
    void shouldHaveGetOperationMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("getOperation");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getOperation should be public");
      assertEquals(String.class, method.getReturnType(), "getOperation should return String");
    }

    @Test
    @DisplayName("should have getResource method")
    void shouldHaveGetResourceMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("getResource");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getResource should be public");
      assertEquals(String.class, method.getReturnType(), "getResource should return String");
    }

    @Test
    @DisplayName("should have isFileSystemError method")
    void shouldHaveIsFileSystemErrorMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("isFileSystemError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isFileSystemError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isFileSystemError should return boolean");
    }

    @Test
    @DisplayName("should have isNetworkError method")
    void shouldHaveIsNetworkErrorMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("isNetworkError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isNetworkError should be public");
      assertEquals(boolean.class, method.getReturnType(), "isNetworkError should return boolean");
    }

    @Test
    @DisplayName("should have isPermissionError method")
    void shouldHaveIsPermissionErrorMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("isPermissionError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isPermissionError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isPermissionError should return boolean");
    }

    @Test
    @DisplayName("should have isResourceLimitError method")
    void shouldHaveIsResourceLimitErrorMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("isResourceLimitError");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isResourceLimitError should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isResourceLimitError should return boolean");
    }

    @Test
    @DisplayName("should have isRetryable method")
    void shouldHaveIsRetryableMethod() throws Exception {
      Method method = WasiException.class.getDeclaredMethod("isRetryable");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isRetryable should be public");
      assertEquals(boolean.class, method.getReturnType(), "isRetryable should return boolean");
    }

    @Test
    @DisplayName("constructor should store error code, operation, and resource")
    void constructorShouldStoreErrorCodeOperationAndResource() {
      WasiException exception =
          new WasiException("Test error", WasiErrorCode.ENOENT, "open", "/file.txt");
      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(), "Error code should be ENOENT");
      assertEquals("open", exception.getOperation(), "Operation should be 'open'");
      assertEquals("/file.txt", exception.getResource(), "Resource should be '/file.txt'");
    }

    @Test
    @DisplayName("constructor should format message with details")
    void constructorShouldFormatMessageWithDetails() {
      WasiException exception =
          new WasiException("Test error", WasiErrorCode.ENOENT, "open", "/file.txt");
      String message = exception.getMessage();
      assertTrue(message.contains("Test error"), "Message should contain base message");
      assertTrue(message.contains("open"), "Message should contain operation");
      assertTrue(message.contains("/file.txt"), "Message should contain resource");
      assertTrue(message.contains("2"), "Message should contain errno");
    }

    @Test
    @DisplayName("simple constructor should use defaults")
    void simpleConstructorShouldUseDefaults() {
      WasiException exception = new WasiException("Simple error");
      assertNotNull(exception.getErrorCode(), "Error code should not be null");
      assertNotNull(exception.getMessage(), "Message should not be null");
    }

    @Test
    @DisplayName("isFileSystemError should delegate to error code")
    void isFileSystemErrorShouldDelegateToErrorCode() {
      WasiException exception =
          new WasiException("File error", WasiErrorCode.ENOENT, "open", "/file.txt");
      assertTrue(exception.isFileSystemError(), "ENOENT exception should be file system error");
    }

    @Test
    @DisplayName("isNetworkError should delegate to error code")
    void isNetworkErrorShouldDelegateToErrorCode() {
      WasiException exception =
          new WasiException("Network error", WasiErrorCode.ECONNREFUSED, "connect", "localhost:80");
      assertTrue(exception.isNetworkError(), "ECONNREFUSED exception should be network error");
    }

    @Test
    @DisplayName("isPermissionError should delegate to error code")
    void isPermissionErrorShouldDelegateToErrorCode() {
      WasiException exception =
          new WasiException("Permission error", WasiErrorCode.EPERM, "delete", "/protected.txt");
      assertTrue(exception.isPermissionError(), "EPERM exception should be permission error");
    }
  }

  // ==================== WasiPermissionException Tests ====================

  @Nested
  @DisplayName("WasiPermissionException Tests")
  class WasiPermissionExceptionTests {

    @Test
    @DisplayName("should extend WasiException")
    void shouldExtendWasiException() {
      assertTrue(
          WasiException.class.isAssignableFrom(WasiPermissionException.class),
          "WasiPermissionException should extend WasiException");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiPermissionException.class.getModifiers()),
          "WasiPermissionException should be final");
    }

    @Test
    @DisplayName("should have getViolationType method")
    void shouldHaveGetViolationTypeMethod() throws Exception {
      Method method = WasiPermissionException.class.getDeclaredMethod("getViolationType");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getViolationType should be public");
      assertEquals(
          PermissionViolationType.class,
          method.getReturnType(),
          "getViolationType should return PermissionViolationType");
    }

    @Test
    @DisplayName("should have getAttemptedResource method")
    void shouldHaveGetAttemptedResourceMethod() throws Exception {
      Method method = WasiPermissionException.class.getDeclaredMethod("getAttemptedResource");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getAttemptedResource should be public");
      assertEquals(
          String.class, method.getReturnType(), "getAttemptedResource should return String");
    }

    @Test
    @DisplayName("should have getViolatedPolicy method")
    void shouldHaveGetViolatedPolicyMethod() throws Exception {
      Method method = WasiPermissionException.class.getDeclaredMethod("getViolatedPolicy");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getViolatedPolicy should be public");
      assertEquals(String.class, method.getReturnType(), "getViolatedPolicy should return String");
    }

    @Test
    @DisplayName("should have isFileSystemViolation method")
    void shouldHaveIsFileSystemViolationMethod() throws Exception {
      Method method = WasiPermissionException.class.getDeclaredMethod("isFileSystemViolation");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isFileSystemViolation should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isFileSystemViolation should return boolean");
    }

    @Test
    @DisplayName("should have isDangerousOperationViolation method")
    void shouldHaveIsDangerousOperationViolationMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod("isDangerousOperationViolation");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "isDangerousOperationViolation should be public");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isDangerousOperationViolation should return boolean");
    }

    @Test
    @DisplayName("should have isResourceLimitViolation method")
    void shouldHaveIsResourceLimitViolationMethod() throws Exception {
      Method method = WasiPermissionException.class.getDeclaredMethod("isResourceLimitViolation");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "isResourceLimitViolation should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "isResourceLimitViolation should return boolean");
    }

    @Test
    @DisplayName("should have static fileSystemAccessDenied factory method")
    void shouldHaveFileSystemAccessDeniedFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "fileSystemAccessDenied", String.class, String.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "fileSystemAccessDenied should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "fileSystemAccessDenied should be public");
    }

    @Test
    @DisplayName("should have static sandboxEscape factory method")
    void shouldHaveSandboxEscapeFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "sandboxEscape", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "sandboxEscape should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "sandboxEscape should be public");
    }

    @Test
    @DisplayName("should have static pathTraversal factory method")
    void shouldHavePathTraversalFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "pathTraversal", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "pathTraversal should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "pathTraversal should be public");
    }

    @Test
    @DisplayName("should have static environmentAccessDenied factory method")
    void shouldHaveEnvironmentAccessDeniedFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "environmentAccessDenied", String.class, String.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "environmentAccessDenied should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "environmentAccessDenied should be public");
    }

    @Test
    @DisplayName("should have static dangerousOperation factory method")
    void shouldHaveDangerousOperationFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "dangerousOperation", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "dangerousOperation should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "dangerousOperation should be public");
    }

    @Test
    @DisplayName("should have static resourceLimitExceeded factory method")
    void shouldHaveResourceLimitExceededFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "resourceLimitExceeded", String.class, String.class, String.class);
      assertTrue(
          Modifier.isStatic(method.getModifiers()), "resourceLimitExceeded should be static");
      assertTrue(
          Modifier.isPublic(method.getModifiers()), "resourceLimitExceeded should be public");
    }

    @Test
    @DisplayName("should have static capabilityNotGranted factory method")
    void shouldHaveCapabilityNotGrantedFactoryMethod() throws Exception {
      Method method =
          WasiPermissionException.class.getDeclaredMethod(
              "capabilityNotGranted", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "capabilityNotGranted should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "capabilityNotGranted should be public");
    }

    @Test
    @DisplayName("fileSystemAccessDenied should create exception with correct type")
    void fileSystemAccessDeniedShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.fileSystemAccessDenied("read", "/secret.txt");
      assertEquals(
          PermissionViolationType.FILE_SYSTEM_ACCESS,
          exception.getViolationType(),
          "Violation type should be FILE_SYSTEM_ACCESS");
      assertTrue(exception.isFileSystemViolation(), "Should be file system violation");
      assertEquals(
          "/secret.txt", exception.getAttemptedResource(), "Attempted resource should match");
    }

    @Test
    @DisplayName("sandboxEscape should create exception with correct type")
    void sandboxEscapeShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.sandboxEscape("read", "/../../../etc/passwd");
      assertEquals(
          PermissionViolationType.SANDBOX_ESCAPE,
          exception.getViolationType(),
          "Violation type should be SANDBOX_ESCAPE");
      assertTrue(exception.isFileSystemViolation(), "Should be file system violation");
    }

    @Test
    @DisplayName("pathTraversal should create exception with correct type")
    void pathTraversalShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.pathTraversal("read", "../secret.txt");
      assertEquals(
          PermissionViolationType.PATH_TRAVERSAL,
          exception.getViolationType(),
          "Violation type should be PATH_TRAVERSAL");
      assertTrue(exception.isFileSystemViolation(), "Should be file system violation");
    }

    @Test
    @DisplayName("environmentAccessDenied should create exception with correct type")
    void environmentAccessDeniedShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.environmentAccessDenied("read", "SECRET_KEY");
      assertEquals(
          PermissionViolationType.ENVIRONMENT_ACCESS,
          exception.getViolationType(),
          "Violation type should be ENVIRONMENT_ACCESS");
      assertFalse(exception.isFileSystemViolation(), "Should not be file system violation");
    }

    @Test
    @DisplayName("dangerousOperation should create exception with correct type")
    void dangerousOperationShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.dangerousOperation("execute", "/bin/rm");
      assertEquals(
          PermissionViolationType.DANGEROUS_OPERATION,
          exception.getViolationType(),
          "Violation type should be DANGEROUS_OPERATION");
      assertTrue(
          exception.isDangerousOperationViolation(), "Should be dangerous operation violation");
    }

    @Test
    @DisplayName("resourceLimitExceeded should create exception with correct type")
    void resourceLimitExceededShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.resourceLimitExceeded("write", "file_size", "100MB");
      assertEquals(
          PermissionViolationType.RESOURCE_LIMIT_EXCEEDED,
          exception.getViolationType(),
          "Violation type should be RESOURCE_LIMIT_EXCEEDED");
      assertTrue(exception.isResourceLimitViolation(), "Should be resource limit violation");
    }

    @Test
    @DisplayName("capabilityNotGranted should create exception with correct type")
    void capabilityNotGrantedShouldCreateExceptionWithCorrectType() {
      WasiPermissionException exception =
          WasiPermissionException.capabilityNotGranted("execute", "EXECUTE");
      assertEquals(
          PermissionViolationType.CAPABILITY_NOT_GRANTED,
          exception.getViolationType(),
          "Violation type should be CAPABILITY_NOT_GRANTED");
    }

    @Test
    @DisplayName("simple constructor should use UNKNOWN violation type")
    void simpleConstructorShouldUseUnknownViolationType() {
      WasiPermissionException exception = new WasiPermissionException("Simple error");
      assertEquals(
          PermissionViolationType.UNKNOWN,
          exception.getViolationType(),
          "Simple constructor should use UNKNOWN violation type");
    }
  }

  // ==================== PermissionViolationType Tests ====================

  @Nested
  @DisplayName("PermissionViolationType Tests")
  class PermissionViolationTypeTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      assertNotNull(PermissionViolationType.FILE_SYSTEM_ACCESS, "Should have FILE_SYSTEM_ACCESS");
      assertNotNull(PermissionViolationType.SANDBOX_ESCAPE, "Should have SANDBOX_ESCAPE");
      assertNotNull(PermissionViolationType.PATH_TRAVERSAL, "Should have PATH_TRAVERSAL");
      assertNotNull(PermissionViolationType.ENVIRONMENT_ACCESS, "Should have ENVIRONMENT_ACCESS");
      assertNotNull(PermissionViolationType.DANGEROUS_OPERATION, "Should have DANGEROUS_OPERATION");
      assertNotNull(
          PermissionViolationType.RESOURCE_LIMIT_EXCEEDED, "Should have RESOURCE_LIMIT_EXCEEDED");
      assertNotNull(
          PermissionViolationType.CAPABILITY_NOT_GRANTED, "Should have CAPABILITY_NOT_GRANTED");
      assertNotNull(
          PermissionViolationType.SECURITY_POLICY_VIOLATION,
          "Should have SECURITY_POLICY_VIOLATION");
      assertNotNull(PermissionViolationType.UNKNOWN, "Should have UNKNOWN");
    }

    @Test
    @DisplayName("should have 9 values")
    void shouldHaveNineValues() {
      assertEquals(
          9, PermissionViolationType.values().length, "Should have 9 permission violation types");
    }

    @Test
    @DisplayName("should have getDescription method")
    void shouldHaveGetDescriptionMethod() throws Exception {
      Method method = PermissionViolationType.class.getDeclaredMethod("getDescription");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getDescription should be public");
      assertEquals(String.class, method.getReturnType(), "getDescription should return String");
    }

    @Test
    @DisplayName("each value should have non-null description")
    void eachValueShouldHaveNonNullDescription() {
      for (PermissionViolationType type : PermissionViolationType.values()) {
        assertNotNull(type.getDescription(), type.name() + " should have non-null description");
        assertFalse(
            type.getDescription().isEmpty(), type.name() + " should have non-empty description");
      }
    }

    @Test
    @DisplayName("toString should return description")
    void toStringShouldReturnDescription() {
      for (PermissionViolationType type : PermissionViolationType.values()) {
        assertEquals(
            type.getDescription(),
            type.toString(),
            type.name() + ".toString() should return description");
      }
    }
  }

  // ==================== WasiFileSystemException Tests ====================

  @Nested
  @DisplayName("WasiFileSystemException Tests")
  class WasiFileSystemExceptionTests {

    @Test
    @DisplayName("should extend WasiException")
    void shouldExtendWasiException() {
      assertTrue(
          WasiException.class.isAssignableFrom(WasiFileSystemException.class),
          "WasiFileSystemException should extend WasiException");
    }

    @Test
    @DisplayName("class should be final")
    void classShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(WasiFileSystemException.class.getModifiers()),
          "WasiFileSystemException should be final");
    }

    @Test
    @DisplayName("should have getFilePath method")
    void shouldHaveGetFilePathMethod() throws Exception {
      Method method = WasiFileSystemException.class.getDeclaredMethod("getFilePath");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getFilePath should be public");
      assertEquals(String.class, method.getReturnType(), "getFilePath should return String");
    }

    @Test
    @DisplayName("should have getFileDescriptor method")
    void shouldHaveGetFileDescriptorMethod() throws Exception {
      Method method = WasiFileSystemException.class.getDeclaredMethod("getFileDescriptor");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getFileDescriptor should be public");
      assertEquals(int.class, method.getReturnType(), "getFileDescriptor should return int");
    }

    @Test
    @DisplayName("should have hasFilePath method")
    void shouldHaveHasFilePathMethod() throws Exception {
      Method method = WasiFileSystemException.class.getDeclaredMethod("hasFilePath");
      assertTrue(Modifier.isPublic(method.getModifiers()), "hasFilePath should be public");
      assertEquals(boolean.class, method.getReturnType(), "hasFilePath should return boolean");
    }

    @Test
    @DisplayName("should have hasFileDescriptor method")
    void shouldHaveHasFileDescriptorMethod() throws Exception {
      Method method = WasiFileSystemException.class.getDeclaredMethod("hasFileDescriptor");
      assertTrue(Modifier.isPublic(method.getModifiers()), "hasFileDescriptor should be public");
      assertEquals(
          boolean.class, method.getReturnType(), "hasFileDescriptor should return boolean");
    }

    @Test
    @DisplayName("should have static fileNotFound factory method")
    void shouldHaveFileNotFoundFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "fileNotFound", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fileNotFound should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fileNotFound should be public");
    }

    @Test
    @DisplayName("should have static permissionDenied factory method")
    void shouldHavePermissionDeniedFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "permissionDenied", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "permissionDenied should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "permissionDenied should be public");
    }

    @Test
    @DisplayName("should have static ioError factory method")
    void shouldHaveIoErrorFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod("ioError", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "ioError should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "ioError should be public");
    }

    @Test
    @DisplayName("should have static noSpaceLeft factory method")
    void shouldHaveNoSpaceLeftFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "noSpaceLeft", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "noSpaceLeft should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "noSpaceLeft should be public");
    }

    @Test
    @DisplayName("should have static badFileDescriptor factory method")
    void shouldHaveBadFileDescriptorFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "badFileDescriptor", String.class, int.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "badFileDescriptor should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "badFileDescriptor should be public");
    }

    @Test
    @DisplayName("should have static fileExists factory method")
    void shouldHaveFileExistsFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod("fileExists", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fileExists should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fileExists should be public");
    }

    @Test
    @DisplayName("should have static directoryNotEmpty factory method")
    void shouldHaveDirectoryNotEmptyFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "directoryNotEmpty", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "directoryNotEmpty should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "directoryNotEmpty should be public");
    }

    @Test
    @DisplayName("should have static readOnlyFileSystem factory method")
    void shouldHaveReadOnlyFileSystemFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "readOnlyFileSystem", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "readOnlyFileSystem should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "readOnlyFileSystem should be public");
    }

    @Test
    @DisplayName("should have static fileNameTooLong factory method")
    void shouldHaveFileNameTooLongFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "fileNameTooLong", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "fileNameTooLong should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fileNameTooLong should be public");
    }

    @Test
    @DisplayName("should have static tooManySymbolicLinks factory method")
    void shouldHaveTooManySymbolicLinksFactoryMethod() throws Exception {
      Method method =
          WasiFileSystemException.class.getDeclaredMethod(
              "tooManySymbolicLinks", String.class, String.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "tooManySymbolicLinks should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "tooManySymbolicLinks should be public");
    }

    @Test
    @DisplayName("fileNotFound should create exception with ENOENT error code")
    void fileNotFoundShouldCreateExceptionWithEnoentErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.fileNotFound("open", "/missing.txt");
      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(), "Error code should be ENOENT");
      assertEquals("/missing.txt", exception.getFilePath(), "File path should match");
      assertTrue(exception.hasFilePath(), "Should have file path");
      assertFalse(exception.hasFileDescriptor(), "Should not have file descriptor");
    }

    @Test
    @DisplayName("permissionDenied should create exception with EACCES error code")
    void permissionDeniedShouldCreateExceptionWithEaccesErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.permissionDenied("read", "/protected.txt");
      assertEquals(WasiErrorCode.EACCES, exception.getErrorCode(), "Error code should be EACCES");
    }

    @Test
    @DisplayName("ioError should create exception with EIO error code")
    void ioErrorShouldCreateExceptionWithEioErrorCode() {
      WasiFileSystemException exception = WasiFileSystemException.ioError("read", "/corrupt.txt");
      assertEquals(WasiErrorCode.EIO, exception.getErrorCode(), "Error code should be EIO");
    }

    @Test
    @DisplayName("noSpaceLeft should create exception with ENOSPC error code")
    void noSpaceLeftShouldCreateExceptionWithEnoscErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.noSpaceLeft("write", "/large.txt");
      assertEquals(WasiErrorCode.ENOSPC, exception.getErrorCode(), "Error code should be ENOSPC");
    }

    @Test
    @DisplayName("badFileDescriptor should create exception with EBADF error code")
    void badFileDescriptorShouldCreateExceptionWithEbadfErrorCode() {
      WasiFileSystemException exception = WasiFileSystemException.badFileDescriptor("read", 999);
      assertEquals(WasiErrorCode.EBADF, exception.getErrorCode(), "Error code should be EBADF");
      assertEquals(999, exception.getFileDescriptor(), "File descriptor should match");
      assertTrue(exception.hasFileDescriptor(), "Should have file descriptor");
      assertFalse(exception.hasFilePath(), "Should not have file path");
    }

    @Test
    @DisplayName("fileExists should create exception with EEXIST error code")
    void fileExistsShouldCreateExceptionWithEexistErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.fileExists("create", "/existing.txt");
      assertEquals(WasiErrorCode.EEXIST, exception.getErrorCode(), "Error code should be EEXIST");
    }

    @Test
    @DisplayName("directoryNotEmpty should create exception with ENOTEMPTY error code")
    void directoryNotEmptyShouldCreateExceptionWithEnotemptyErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.directoryNotEmpty("rmdir", "/nonempty/");
      assertEquals(
          WasiErrorCode.ENOTEMPTY, exception.getErrorCode(), "Error code should be ENOTEMPTY");
    }

    @Test
    @DisplayName("readOnlyFileSystem should create exception with EROFS error code")
    void readOnlyFileSystemShouldCreateExceptionWithErofsErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.readOnlyFileSystem("write", "/readonly.txt");
      assertEquals(WasiErrorCode.EROFS, exception.getErrorCode(), "Error code should be EROFS");
    }

    @Test
    @DisplayName("fileNameTooLong should create exception with ENAMETOOLONG error code")
    void fileNameTooLongShouldCreateExceptionWithEnametoolongErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.fileNameTooLong("create", "/very_long_name...");
      assertEquals(
          WasiErrorCode.ENAMETOOLONG,
          exception.getErrorCode(),
          "Error code should be ENAMETOOLONG");
    }

    @Test
    @DisplayName("tooManySymbolicLinks should create exception with ELOOP error code")
    void tooManySymbolicLinksShouldCreateExceptionWithEloopErrorCode() {
      WasiFileSystemException exception =
          WasiFileSystemException.tooManySymbolicLinks("open", "/symlink");
      assertEquals(WasiErrorCode.ELOOP, exception.getErrorCode(), "Error code should be ELOOP");
    }

    @Test
    @DisplayName("constructor with file descriptor should set -1 for file path")
    void constructorWithFileDescriptorShouldSetMinusOneForFilePath() {
      WasiFileSystemException exception =
          new WasiFileSystemException(WasiErrorCode.EBADF, "read", 42);
      assertEquals(42, exception.getFileDescriptor(), "File descriptor should be 42");
      assertNull(exception.getFilePath(), "File path should be null");
      assertEquals(
          -1,
          new WasiFileSystemException(WasiErrorCode.ENOENT, "open", "/file.txt")
              .getFileDescriptor(),
          "File descriptor should be -1 when path is used");
    }

    @Test
    @DisplayName("constructor with string error code should parse correctly")
    void constructorWithStringErrorCodeShouldParseCorrectly() {
      WasiFileSystemException exception = new WasiFileSystemException("Error message", "ENOENT");
      assertEquals(WasiErrorCode.ENOENT, exception.getErrorCode(), "Error code should be ENOENT");
    }

    @Test
    @DisplayName("constructor with invalid string error code should use EIO")
    void constructorWithInvalidStringErrorCodeShouldUseEio() {
      WasiFileSystemException exception = new WasiFileSystemException("Error message", "INVALID");
      assertEquals(
          WasiErrorCode.EIO, exception.getErrorCode(), "Error code should fall back to EIO");
    }
  }

  // ==================== Package Consistency Tests ====================

  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("security package should have expected classes")
    void securityPackageShouldHaveExpectedClasses() {
      assertNotNull(WasiSecurityValidator.class, "WasiSecurityValidator should exist");
      assertNotNull(WasiSecurityPolicyEngine.class, "WasiSecurityPolicyEngine should exist");
    }

    @Test
    @DisplayName("exception package should have expected classes")
    void exceptionPackageShouldHaveExpectedClasses() {
      assertNotNull(WasiException.class, "WasiException should exist");
      assertNotNull(WasiPermissionException.class, "WasiPermissionException should exist");
      assertNotNull(WasiFileSystemException.class, "WasiFileSystemException should exist");
      assertNotNull(WasiErrorCode.class, "WasiErrorCode should exist");
    }

    @Test
    @DisplayName("WasiFileOperation should be in wasi package")
    void wasiFileOperationShouldBeInWasiPackage() {
      String packageName = WasiFileOperation.class.getPackage().getName();
      assertTrue(packageName.endsWith("wasi"), "WasiFileOperation should be in wasi package");
    }

    @Test
    @DisplayName("exception classes should be in exception package")
    void exceptionClassesShouldBeInExceptionPackage() {
      String packageName = WasiException.class.getPackage().getName();
      assertTrue(packageName.endsWith("exception"), "WasiException should be in exception package");

      assertEquals(
          packageName,
          WasiPermissionException.class.getPackage().getName(),
          "WasiPermissionException should be in same package as WasiException");
      assertEquals(
          packageName,
          WasiFileSystemException.class.getPackage().getName(),
          "WasiFileSystemException should be in same package as WasiException");
      assertEquals(
          packageName,
          WasiErrorCode.class.getPackage().getName(),
          "WasiErrorCode should be in same package as WasiException");
    }

    @Test
    @DisplayName("all exceptions should have serialVersionUID")
    void allExceptionsShouldHaveSerialVersionUID() throws Exception {
      // WasiException
      java.lang.reflect.Field wasiExceptionField =
          WasiException.class.getDeclaredField("serialVersionUID");
      assertTrue(
          Modifier.isPrivate(wasiExceptionField.getModifiers()),
          "serialVersionUID should be private");
      assertTrue(
          Modifier.isStatic(wasiExceptionField.getModifiers()),
          "serialVersionUID should be static");
      assertTrue(
          Modifier.isFinal(wasiExceptionField.getModifiers()), "serialVersionUID should be final");

      // WasiPermissionException
      java.lang.reflect.Field permExceptionField =
          WasiPermissionException.class.getDeclaredField("serialVersionUID");
      assertNotNull(permExceptionField, "WasiPermissionException should have serialVersionUID");

      // WasiFileSystemException
      java.lang.reflect.Field fsExceptionField =
          WasiFileSystemException.class.getDeclaredField("serialVersionUID");
      assertNotNull(fsExceptionField, "WasiFileSystemException should have serialVersionUID");
    }
  }
}
