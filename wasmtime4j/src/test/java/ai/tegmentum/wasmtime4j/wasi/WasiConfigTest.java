package ai.tegmentum.wasmtime4j.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link WasiConfig} interface.
 *
 * <p>Tests verify:
 *
 * <ul>
 *   <li>Interface contract and method signatures
 *   <li>Static factory methods
 *   <li>Configuration getter behavior
 *   <li>Builder integration expectations
 * </ul>
 *
 * <p>Note: These tests focus on the interface contract and expected behavior.
 * Implementation-specific tests are in the JNI/Panama modules.
 */
@DisplayName("WasiConfig Tests")
class WasiConfigTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("WasiConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiConfig.class.isInterface(), "WasiConfig should be an interface");
    }

    @Test
    @DisplayName("should have static builder method")
    void shouldHaveStaticBuilderMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("builder"), "WasiConfig should have static builder() method");
    }

    @Test
    @DisplayName("should have static defaultConfig method")
    void shouldHaveStaticDefaultConfigMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("defaultConfig"),
          "WasiConfig should have static defaultConfig() method");
    }
  }

  @Nested
  @DisplayName("Environment Configuration Method Tests")
  class EnvironmentConfigurationMethodTests {

    @Test
    @DisplayName("should have getEnvironment method")
    void shouldHaveGetEnvironmentMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getEnvironment"),
          "WasiConfig should have getEnvironment() method");
    }

    @Test
    @DisplayName("getEnvironment should return Map")
    void getEnvironmentShouldReturnMap() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getEnvironment").getReturnType();
      assertEquals(Map.class, returnType, "getEnvironment should return Map");
    }

    @Test
    @DisplayName("should have getArguments method")
    void shouldHaveGetArgumentsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getArguments"),
          "WasiConfig should have getArguments() method");
    }

    @Test
    @DisplayName("getArguments should return List")
    void getArgumentsShouldReturnList() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getArguments").getReturnType();
      assertEquals(List.class, returnType, "getArguments should return List");
    }
  }

  @Nested
  @DisplayName("Directory Configuration Method Tests")
  class DirectoryConfigurationMethodTests {

    @Test
    @DisplayName("should have getPreopenDirectories method")
    void shouldHaveGetPreopenDirectoriesMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getPreopenDirectories"),
          "WasiConfig should have getPreopenDirectories() method");
    }

    @Test
    @DisplayName("getPreopenDirectories should return Map")
    void getPreopenDirectoriesShouldReturnMap() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getPreopenDirectories").getReturnType();
      assertEquals(Map.class, returnType, "getPreopenDirectories should return Map");
    }

    @Test
    @DisplayName("should have getWorkingDirectory method")
    void shouldHaveGetWorkingDirectoryMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getWorkingDirectory"),
          "WasiConfig should have getWorkingDirectory() method");
    }

    @Test
    @DisplayName("getWorkingDirectory should return Optional")
    void getWorkingDirectoryShouldReturnOptional() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getWorkingDirectory").getReturnType();
      assertEquals(Optional.class, returnType, "getWorkingDirectory should return Optional");
    }
  }

  @Nested
  @DisplayName("Resource Limit Method Tests")
  class ResourceLimitMethodTests {

    @Test
    @DisplayName("should have getMemoryLimit method")
    void shouldHaveGetMemoryLimitMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getMemoryLimit"),
          "WasiConfig should have getMemoryLimit() method");
    }

    @Test
    @DisplayName("getMemoryLimit should return Optional")
    void getMemoryLimitShouldReturnOptional() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getMemoryLimit").getReturnType();
      assertEquals(Optional.class, returnType, "getMemoryLimit should return Optional");
    }

    @Test
    @DisplayName("should have getExecutionTimeout method")
    void shouldHaveGetExecutionTimeoutMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getExecutionTimeout"),
          "WasiConfig should have getExecutionTimeout() method");
    }

    @Test
    @DisplayName("getExecutionTimeout should return Optional")
    void getExecutionTimeoutShouldReturnOptional() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getExecutionTimeout").getReturnType();
      assertEquals(Optional.class, returnType, "getExecutionTimeout should return Optional");
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getResourceLimits"),
          "WasiConfig should have getResourceLimits() method");
    }
  }

  @Nested
  @DisplayName("Security Configuration Method Tests")
  class SecurityConfigurationMethodTests {

    @Test
    @DisplayName("should have getSecurityPolicy method")
    void shouldHaveGetSecurityPolicyMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getSecurityPolicy"),
          "WasiConfig should have getSecurityPolicy() method");
    }

    @Test
    @DisplayName("getSecurityPolicy should return Optional")
    void getSecurityPolicyShouldReturnOptional() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getSecurityPolicy").getReturnType();
      assertEquals(Optional.class, returnType, "getSecurityPolicy should return Optional");
    }

    @Test
    @DisplayName("should have isValidationEnabled method")
    void shouldHaveIsValidationEnabledMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("isValidationEnabled"),
          "WasiConfig should have isValidationEnabled() method");
    }

    @Test
    @DisplayName("isValidationEnabled should return boolean")
    void isValidationEnabledShouldReturnBoolean() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("isValidationEnabled").getReturnType();
      assertEquals(boolean.class, returnType, "isValidationEnabled should return boolean");
    }

    @Test
    @DisplayName("should have isStrictModeEnabled method")
    void shouldHaveIsStrictModeEnabledMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("isStrictModeEnabled"),
          "WasiConfig should have isStrictModeEnabled() method");
    }
  }

  @Nested
  @DisplayName("Import Resolver Method Tests")
  class ImportResolverMethodTests {

    @Test
    @DisplayName("should have getImportResolvers method")
    void shouldHaveGetImportResolversMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getImportResolvers"),
          "WasiConfig should have getImportResolvers() method");
    }

    @Test
    @DisplayName("getImportResolvers should return Map")
    void getImportResolversShouldReturnMap() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getImportResolvers").getReturnType();
      assertEquals(Map.class, returnType, "getImportResolvers should return Map");
    }
  }

  @Nested
  @DisplayName("Version and Builder Method Tests")
  class VersionAndBuilderMethodTests {

    @Test
    @DisplayName("should have getWasiVersion method")
    void shouldHaveGetWasiVersionMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("getWasiVersion"),
          "WasiConfig should have getWasiVersion() method");
    }

    @Test
    @DisplayName("getWasiVersion should return WasiVersion")
    void getWasiVersionShouldReturnWasiVersion() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("getWasiVersion").getReturnType();
      assertEquals(WasiVersion.class, returnType, "getWasiVersion should return WasiVersion");
    }

    @Test
    @DisplayName("should have toBuilder method")
    void shouldHaveToBuilderMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("toBuilder"), "WasiConfig should have toBuilder() method");
    }

    @Test
    @DisplayName("toBuilder should return WasiConfigBuilder")
    void toBuilderShouldReturnWasiConfigBuilder() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("toBuilder").getReturnType();
      assertEquals(
          WasiConfigBuilder.class, returnType, "toBuilder should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws NoSuchMethodException {
      assertNotNull(
          WasiConfig.class.getMethod("validate"), "WasiConfig should have validate() method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("builder method should be static")
    void builderMethodShouldBeStatic() throws NoSuchMethodException {
      assertTrue(
          java.lang.reflect.Modifier.isStatic(WasiConfig.class.getMethod("builder").getModifiers()),
          "builder method should be static");
    }

    @Test
    @DisplayName("defaultConfig method should be static")
    void defaultConfigMethodShouldBeStatic() throws NoSuchMethodException {
      assertTrue(
          java.lang.reflect.Modifier.isStatic(
              WasiConfig.class.getMethod("defaultConfig").getModifiers()),
          "defaultConfig method should be static");
    }

    @Test
    @DisplayName("builder method should return WasiConfigBuilder")
    void builderMethodShouldReturnWasiConfigBuilder() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("builder").getReturnType();
      assertEquals(
          WasiConfigBuilder.class, returnType, "builder method should return WasiConfigBuilder");
    }

    @Test
    @DisplayName("defaultConfig method should return WasiConfig")
    void defaultConfigMethodShouldReturnWasiConfig() throws NoSuchMethodException {
      Class<?> returnType = WasiConfig.class.getMethod("defaultConfig").getReturnType();
      assertEquals(WasiConfig.class, returnType, "defaultConfig method should return WasiConfig");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("should be implementable by anonymous class")
    void shouldBeImplementableByAnonymousClass() {
      WasiConfig mockConfig = createMockConfig();

      assertNotNull(mockConfig, "Mock WasiConfig should be creatable");
      assertNotNull(mockConfig.getEnvironment(), "Environment should not be null");
      assertTrue(mockConfig.getEnvironment().isEmpty(), "Environment should be empty");
      assertNotNull(mockConfig.getArguments(), "Arguments should not be null");
      assertTrue(mockConfig.getArguments().isEmpty(), "Arguments should be empty");
    }

    @Test
    @DisplayName("mock implementation should return expected defaults")
    void mockImplementationShouldReturnExpectedDefaults() {
      WasiConfig mockConfig = createMockConfig();

      assertNotNull(mockConfig.getPreopenDirectories());
      assertFalse(mockConfig.getWorkingDirectory().isPresent());
      assertFalse(mockConfig.getMemoryLimit().isPresent());
      assertFalse(mockConfig.getExecutionTimeout().isPresent());
      assertFalse(mockConfig.getResourceLimits().isPresent());
      assertFalse(mockConfig.getSecurityPolicy().isPresent());
      assertNotNull(mockConfig.getImportResolvers());
      assertFalse(mockConfig.isValidationEnabled());
      assertFalse(mockConfig.isStrictModeEnabled());
    }

    private WasiConfig createMockConfig() {
      return new WasiConfig() {
        @Override
        public Map<String, String> getEnvironment() {
          return Map.of();
        }

        @Override
        public List<String> getArguments() {
          return List.of();
        }

        @Override
        public Map<String, Path> getPreopenDirectories() {
          return Map.of();
        }

        @Override
        public Optional<String> getWorkingDirectory() {
          return Optional.empty();
        }

        @Override
        public Optional<Long> getMemoryLimit() {
          return Optional.empty();
        }

        @Override
        public Optional<Duration> getExecutionTimeout() {
          return Optional.empty();
        }

        @Override
        public Optional<WasiResourceLimits> getResourceLimits() {
          return Optional.empty();
        }

        @Override
        public Optional<WasiSecurityPolicy> getSecurityPolicy() {
          return Optional.empty();
        }

        @Override
        public Map<String, WasiImportResolver> getImportResolvers() {
          return Map.of();
        }

        @Override
        public boolean isValidationEnabled() {
          return false;
        }

        @Override
        public boolean isStrictModeEnabled() {
          return false;
        }

        @Override
        public WasiConfigBuilder toBuilder() {
          return null;
        }

        @Override
        public WasiVersion getWasiVersion() {
          return null;
        }

        @Override
        public void validate() {
          // No-op for mock
        }
      };
    }
  }

  @Nested
  @DisplayName("Related Type Tests")
  class RelatedTypeTests {

    @Test
    @DisplayName("WasiConfigBuilder should exist")
    void wasiConfigBuilderShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiConfigBuilder");
        assertNotNull(clazz, "WasiConfigBuilder class should exist");
        assertTrue(clazz.isInterface(), "WasiConfigBuilder should be an interface");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiConfigBuilder class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiVersion should exist")
    void wasiVersionShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiVersion");
        assertNotNull(clazz, "WasiVersion class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiVersion class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiResourceLimits should exist")
    void wasiResourceLimitsShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits");
        assertNotNull(clazz, "WasiResourceLimits class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiResourceLimits class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiSecurityPolicy should exist")
    void wasiSecurityPolicyShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiSecurityPolicy");
        assertNotNull(clazz, "WasiSecurityPolicy class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiSecurityPolicy class should exist", e);
      }
    }

    @Test
    @DisplayName("WasiImportResolver should exist")
    void wasiImportResolverShouldExist() {
      try {
        Class<?> clazz = Class.forName("ai.tegmentum.wasmtime4j.wasi.WasiImportResolver");
        assertNotNull(clazz, "WasiImportResolver class should exist");
      } catch (ClassNotFoundException e) {
        throw new AssertionError("WasiImportResolver class should exist", e);
      }
    }
  }

  @Nested
  @DisplayName("Interface Contract Tests")
  class InterfaceContractTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      for (java.lang.reflect.Method method : WasiConfig.class.getDeclaredMethods()) {
        // Skip synthetic methods added by instrumentation (e.g., Jacoco's $jacocoInit)
        if (method.isSynthetic()) {
          continue;
        }
        assertTrue(
            java.lang.reflect.Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }

    @Test
    @DisplayName("interface should have expected method count")
    void interfaceShouldHaveExpectedMethodCount() {
      // Count non-static methods (instance methods that implementations must provide)
      int instanceMethodCount = 0;
      for (java.lang.reflect.Method method : WasiConfig.class.getDeclaredMethods()) {
        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          instanceMethodCount++;
        }
      }
      // getEnvironment, getArguments, getPreopenDirectories, getWorkingDirectory,
      // getMemoryLimit, getExecutionTimeout, getResourceLimits, getSecurityPolicy,
      // getImportResolvers, isValidationEnabled, isStrictModeEnabled, toBuilder,
      // getWasiVersion, validate = 14 methods
      assertEquals(14, instanceMethodCount, "WasiConfig should have 14 instance methods");
    }
  }
}
