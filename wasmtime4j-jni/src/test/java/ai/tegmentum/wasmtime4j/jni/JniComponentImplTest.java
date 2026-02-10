package ai.tegmentum.wasmtime4j.jni;

import static org.assertj.core.api.Assertions.assertThat;

import ai.tegmentum.wasmtime4j.ComponentAuditLog;
import ai.tegmentum.wasmtime4j.ComponentBackup;
import ai.tegmentum.wasmtime4j.ComponentBackupConfig;
import ai.tegmentum.wasmtime4j.ComponentDebugInfo;
import ai.tegmentum.wasmtime4j.ComponentLifecycleState;
import ai.tegmentum.wasmtime4j.ComponentMetadata;
import ai.tegmentum.wasmtime4j.ComponentMetrics;
import ai.tegmentum.wasmtime4j.ComponentOptimizationResult;
import ai.tegmentum.wasmtime4j.ComponentResourceLimits;
import ai.tegmentum.wasmtime4j.ComponentResourceUsage;
import ai.tegmentum.wasmtime4j.ComponentVersion;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceIntrospection;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for JniComponentImpl class.
 *
 * <p>These tests verify the class structure, method signatures, and behavior without triggering
 * native library initialization.
 *
 * @since 1.0.0
 */
@DisplayName("JniComponentImpl Tests")
class JniComponentImplTest {

  private static final String CLASS_NAME = "ai.tegmentum.wasmtime4j.jni.JniComponentImpl";

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      assertThat(Modifier.isFinal(clazz.getModifiers()))
          .as("JniComponentImpl should be final")
          .isTrue();
    }

    @Test
    @DisplayName("should implement Component interface")
    void shouldImplementComponentInterface() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> componentSimpleClass = Class.forName("ai.tegmentum.wasmtime4j.Component");
      assertThat(componentSimpleClass.isAssignableFrom(clazz))
          .as("JniComponentImpl should implement Component")
          .isTrue();
    }

    @Test
    @DisplayName("should have Logger field")
    void shouldHaveLoggerField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field loggerField = clazz.getDeclaredField("LOGGER");

      assertThat(Modifier.isPrivate(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(loggerField.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(loggerField.getModifiers())).isTrue();
      assertThat(loggerField.getType()).isEqualTo(Logger.class);
    }

    @Test
    @DisplayName("should have nativeComponent field")
    void shouldHaveNativeComponentField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("nativeComponent");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have engine field")
    void shouldHaveEngineField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("engine");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(JniComponentEngine.class);
    }

    @Test
    @DisplayName("should have metadata field")
    void shouldHaveMetadataField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("metadata");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(ComponentMetadata.class);
    }

    @Test
    @DisplayName("should have componentId field")
    void shouldHaveComponentIdField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("componentId");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should have version field")
    void shouldHaveVersionField() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Field field = clazz.getDeclaredField("version");

      assertThat(Modifier.isPrivate(field.getModifiers())).isTrue();
      assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
      assertThat(field.getType()).isEqualTo(ComponentVersion.class);
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have two-argument constructor")
    void shouldHaveTwoArgumentConstructor() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> handleClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.JniComponent$JniComponentHandle");

      Constructor<?> constructor = clazz.getConstructor(handleClass, JniComponentEngine.class);

      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have three-argument constructor with metadata")
    void shouldHaveThreeArgumentConstructor() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> handleClass =
          Class.forName("ai.tegmentum.wasmtime4j.jni.JniComponent$JniComponentHandle");

      Constructor<?> constructor =
          clazz.getConstructor(handleClass, JniComponentEngine.class, ComponentMetadata.class);

      assertThat(constructor).isNotNull();
      assertThat(Modifier.isPublic(constructor.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Core Methods Tests")
  class CoreMethodsTests {

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getId");

      assertThat(method.getReturnType()).isEqualTo(String.class);
    }

    @Test
    @DisplayName("should have getVersion method")
    void shouldHaveGetVersionMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getVersion");

      assertThat(method.getReturnType()).isEqualTo(ComponentVersion.class);
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getSize");

      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getMetadata");

      assertThat(method.getReturnType()).isEqualTo(ComponentMetadata.class);
    }

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getEngine");

      assertThat(method.getReturnType()).isEqualTo(JniComponentEngine.class);
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("isValid");

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getNativeHandle");

      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("close");

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Interface Methods Tests")
  class InterfaceMethodsTests {

    @Test
    @DisplayName("should have exportsInterface method")
    void shouldHaveExportsInterfaceMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("exportsInterface", String.class);

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("should have importsInterface method")
    void shouldHaveImportsInterfaceMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("importsInterface", String.class);

      assertThat(method.getReturnType()).isEqualTo(boolean.class);
    }

    @Test
    @DisplayName("should have getExportedInterfaces method")
    void shouldHaveGetExportedInterfacesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getExportedInterfaces");

      assertThat(method.getReturnType()).isEqualTo(java.util.Set.class);
    }

    @Test
    @DisplayName("should have getImportedInterfaces method")
    void shouldHaveGetImportedInterfacesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getImportedInterfaces");

      assertThat(method.getReturnType()).isEqualTo(java.util.Set.class);
    }
  }

  @Nested
  @DisplayName("Instantiation Methods Tests")
  class InstantiationMethodsTests {

    @Test
    @DisplayName("should have parameterless instantiate method")
    void shouldHaveParameterlessInstantiateMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("instantiate");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.ComponentInstance");
    }

    @Test
    @DisplayName("should have instantiate method with config")
    void shouldHaveInstantiateWithConfigMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentInstanceConfig");
      Method method = clazz.getMethod("instantiate", configClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.ComponentInstance");
    }
  }

  @Nested
  @DisplayName("Dependency Graph Methods Tests")
  class DependencyGraphMethodsTests {

    @Test
    @DisplayName("should have getDependencyGraph method")
    void shouldHaveGetDependencyGraphMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getDependencyGraph");

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.ComponentDependencyGraph");
    }

    @Test
    @DisplayName("should have resolveDependencies method")
    void shouldHaveResolveDependenciesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> registryClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentRegistry");
      Method method = clazz.getMethod("resolveDependencies", registryClass);

      assertThat(method.getReturnType()).isEqualTo(java.util.Set.class);
    }
  }

  @Nested
  @DisplayName("Hot Swap Methods Tests")
  class HotSwapMethodsTests {

    @Test
    @DisplayName("should have hotSwap method")
    void shouldHaveHotSwapMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> strategyClass = Class.forName("ai.tegmentum.wasmtime4j.HotSwapStrategy");
      Class<?> componentSimpleClass = Class.forName("ai.tegmentum.wasmtime4j.Component");
      Method method = clazz.getMethod("hotSwap", componentSimpleClass, strategyClass);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("should have checkCompatibility method")
    void shouldHaveCheckCompatibilityMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> simpleClass = Class.forName("ai.tegmentum.wasmtime4j.Component");
      Method method = clazz.getMethod("checkCompatibility", simpleClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.ComponentCompatibility");
    }
  }

  @Nested
  @DisplayName("WIT Interface Methods Tests")
  class WitInterfaceMethodsTests {

    @Test
    @DisplayName("should have getWitInterface method")
    void shouldHaveGetWitInterfaceMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getWitInterface");

      assertThat(WitInterfaceDefinition.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have checkWitCompatibility method")
    void shouldHaveCheckWitCompatibilityMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> simpleClass = Class.forName("ai.tegmentum.wasmtime4j.Component");
      Method method = clazz.getMethod("checkWitCompatibility", simpleClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.WitCompatibilityResult");
    }

    @Test
    @DisplayName("should have migrateWitInterface method")
    void shouldHaveMigrateWitInterfaceMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> versionClass = Class.forName("ai.tegmentum.wasmtime4j.WitInterfaceVersion");
      Class<?> planClass = Class.forName("ai.tegmentum.wasmtime4j.WitInterfaceMigrationPlan");
      Method method = clazz.getMethod("migrateWitInterface", versionClass, planClass);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("should have getWitIntrospection method")
    void shouldHaveGetWitIntrospectionMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getWitIntrospection");

      assertThat(WitInterfaceIntrospection.class.isAssignableFrom(method.getReturnType())).isTrue();
    }
  }

  @Nested
  @DisplayName("Enterprise Management Methods Tests")
  class EnterpriseManagementMethodsTests {

    @Test
    @DisplayName("should have getAuditLog method")
    void shouldHaveGetAuditLogMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getAuditLog");

      assertThat(ComponentAuditLog.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have applySecurityPolicies method")
    void shouldHaveApplySecurityPoliciesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("applySecurityPolicies", java.util.Set.class);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have getSecurityPolicies method")
    void shouldHaveGetSecurityPoliciesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getSecurityPolicies");

      assertThat(method.getReturnType()).isEqualTo(java.util.Set.class);
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getMetrics");

      assertThat(ComponentMetrics.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have startMonitoring method")
    void shouldHaveStartMonitoringMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentMonitoringConfig");
      Method method = clazz.getMethod("startMonitoring", configClass);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have stopMonitoring method")
    void shouldHaveStopMonitoringMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("stopMonitoring");

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }
  }

  @Nested
  @DisplayName("Backup Methods Tests")
  class BackupMethodsTests {

    @Test
    @DisplayName("should have createBackup method")
    void shouldHaveCreateBackupMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("createBackup", ComponentBackupConfig.class);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }

    @Test
    @DisplayName("should have restoreFromBackup method")
    void shouldHaveRestoreFromBackupMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> optionsClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentRestoreOptions");
      Method method = clazz.getMethod("restoreFromBackup", ComponentBackup.class, optionsClass);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }
  }

  @Nested
  @DisplayName("Resource Management Methods Tests")
  class ResourceManagementMethodsTests {

    @Test
    @DisplayName("should have getResourceUsage method")
    void shouldHaveGetResourceUsageMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getResourceUsage");

      assertThat(ComponentResourceUsage.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have setResourceLimits method")
    void shouldHaveSetResourceLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("setResourceLimits", ComponentResourceLimits.class);

      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have getResourceLimits method")
    void shouldHaveGetResourceLimitsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getResourceLimits");

      assertThat(ComponentResourceLimits.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have optimizeResources method")
    void shouldHaveOptimizeResourcesMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentOptimizationConfig");
      Method method = clazz.getMethod("optimizeResources", configClass);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }
  }

  @Nested
  @DisplayName("Lifecycle Methods Tests")
  class LifecycleMethodsTests {

    @Test
    @DisplayName("should have getLifecycleState method")
    void shouldHaveGetLifecycleStateMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getLifecycleState");

      assertThat(method.getReturnType()).isEqualTo(ComponentLifecycleState.class);
    }

    @Test
    @DisplayName("should have transitionTo method")
    void shouldHaveTransitionToMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass =
          Class.forName("ai.tegmentum.wasmtime4j.ComponentStateTransitionConfig");
      Method method = clazz.getMethod("transitionTo", ComponentLifecycleState.class, configClass);

      assertThat(method.getReturnType()).isEqualTo(CompletableFuture.class);
    }
  }

  @Nested
  @DisplayName("Debug and Validation Methods Tests")
  class DebugAndValidationMethodsTests {

    @Test
    @DisplayName("should have getDebugInfo method")
    void shouldHaveGetDebugInfoMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getMethod("getDebugInfo");

      assertThat(ComponentDebugInfo.class.isAssignableFrom(method.getReturnType())).isTrue();
    }

    @Test
    @DisplayName("should have validate method")
    void shouldHaveValidateMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> configClass = Class.forName("ai.tegmentum.wasmtime4j.ComponentValidationConfig");
      Method method = clazz.getMethod("validate", configClass);

      assertThat(method.getReturnType().getName())
          .isEqualTo("ai.tegmentum.wasmtime4j.ComponentValidationResult");
    }
  }

  @Nested
  @DisplayName("Native Methods Tests")
  class NativeMethodsTests {

    @Test
    @DisplayName("should have nativeGetComponentsLoaded method")
    void shouldHaveNativeGetComponentsLoadedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetComponentsLoaded", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(long.class);
    }

    @Test
    @DisplayName("should have nativeGetInstancesCreated method")
    void shouldHaveNativeGetInstancesCreatedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetInstancesCreated", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetInstancesDestroyed method")
    void shouldHaveNativeGetInstancesDestroyedMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetInstancesDestroyed", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetAvgInstantiationTimeNanos method")
    void shouldHaveNativeGetAvgInstantiationTimeNanosMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetAvgInstantiationTimeNanos", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetPeakMemoryUsage method")
    void shouldHaveNativeGetPeakMemoryUsageMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetPeakMemoryUsage", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetFunctionCalls method")
    void shouldHaveNativeGetFunctionCallsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetFunctionCalls", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetErrorCount method")
    void shouldHaveNativeGetErrorCountMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetErrorCount", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("should have nativeGetMetrics method")
    void shouldHaveNativeGetMetricsMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("nativeGetMetrics", long.class);

      assertThat(Modifier.isNative(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(java.util.Map.class);
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentAuditLog Tests")
  class JniComponentAuditLogTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentAuditLog")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentAuditLog.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentAuditLog inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement ComponentAuditLog interface methods")
    void shouldImplementAuditLogMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentAuditLog")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getEntries",
              "addEntry",
              "getEntriesByType",
              "getEntriesInRange",
              "export",
              "isEmpty",
              "size",
              "clear",
              "getComponentId");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod).as("JniComponentAuditLog should have method: " + methodName).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentMetrics Tests")
  class JniComponentMetricsTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentMetrics")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentMetrics.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentMetrics inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement ComponentMetrics interface methods")
    void shouldImplementMetricsMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentMetrics")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getComponentId",
              "getExecutionMetrics",
              "getMemoryMetrics",
              "getPerformanceMetrics",
              "getResourceMetrics",
              "getErrorMetrics",
              "getStartTime",
              "getEndTime",
              "reset",
              "snapshot");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod).as("JniComponentMetrics should have method: " + methodName).isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentResourceLimitsImpl Tests")
  class JniComponentResourceLimitsImplTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentResourceLimitsImpl")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentResourceLimits.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentResourceLimitsImpl inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement ComponentResourceLimits interface methods")
    void shouldImplementResourceLimitsMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentResourceLimitsImpl")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getMemoryLimits",
              "getExecutionLimits",
              "getIoLimits",
              "getNetworkLimits",
              "getFileSystemLimits",
              "validate");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod)
            .as("JniComponentResourceLimitsImpl should have method: " + methodName)
            .isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentOptimizationResultImpl Tests")
  class JniComponentOptimizationResultImplTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentOptimizationResultImpl")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentOptimizationResult.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentOptimizationResultImpl inner class should exist").isTrue();
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentDebugInfoImpl Tests")
  class JniComponentDebugInfoImplTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentDebugInfoImpl")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentDebugInfo.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentDebugInfoImpl inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement ComponentDebugInfo interface methods")
    void shouldImplementDebugInfoMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentDebugInfoImpl")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getComponentId",
              "getComponentName",
              "getSymbols",
              "getSourceMaps",
              "getExecutionState",
              "getVariables",
              "getFunctions",
              "getMemoryLayout",
              "getStackTrace",
              "getBreakpoints");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod)
            .as("JniComponentDebugInfoImpl should have method: " + methodName)
            .isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Inner Class JniWitInterfaceIntrospection Tests")
  class JniWitInterfaceIntrospectionTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniWitInterfaceIntrospection")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(WitInterfaceIntrospection.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniWitInterfaceIntrospection inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement WitInterfaceIntrospection interface methods")
    void shouldImplementIntrospectionMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniWitInterfaceIntrospection")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getInterfaceName",
              "getVersion",
              "getFunctions",
              "getTypes",
              "getResources",
              "getDocumentation",
              "getMetadata",
              "isCompatibleWith",
              "getDependencies",
              "getExports",
              "getImports");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod)
            .as("JniWitInterfaceIntrospection should have method: " + methodName)
            .isTrue();
      }
    }
  }

  @Nested
  @DisplayName("Inner Class JniComponentBackup Tests")
  class JniComponentBackupTests {

    @Test
    @DisplayName("should exist as private static inner class")
    void shouldExistAsPrivateStaticInnerClass() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = outerClass.getDeclaredClasses();

      boolean found = false;
      for (Class<?> inner : declaredClasses) {
        if (inner.getSimpleName().equals("JniComponentBackup")) {
          found = true;
          assertThat(Modifier.isPrivate(inner.getModifiers())).isTrue();
          assertThat(Modifier.isStatic(inner.getModifiers())).isTrue();
          assertThat(ComponentBackup.class.isAssignableFrom(inner)).isTrue();
          break;
        }
      }
      assertThat(found).as("JniComponentBackup inner class should exist").isTrue();
    }

    @Test
    @DisplayName("should implement ComponentBackup interface methods")
    void shouldImplementBackupMethods() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentBackup")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedMethods =
          Arrays.asList(
              "getBackupId",
              "getComponentId",
              "getTimestamp",
              "getType",
              "getSize",
              "getChecksum",
              "getMetadata",
              "getStatus",
              "getLocation",
              "verify",
              "restore",
              "delete",
              "getCompressionInfo",
              "getEncryptionInfo");

      for (String methodName : expectedMethods) {
        boolean hasMethod =
            Arrays.stream(innerClass.getDeclaredMethods())
                .anyMatch(m -> m.getName().equals(methodName));
        assertThat(hasMethod).as("JniComponentBackup should have method: " + methodName).isTrue();
      }
    }

    @Test
    @DisplayName("should have required fields")
    void shouldHaveRequiredFields() throws Exception {
      Class<?> outerClass = Class.forName(CLASS_NAME);
      Class<?> innerClass = null;
      for (Class<?> inner : outerClass.getDeclaredClasses()) {
        if (inner.getSimpleName().equals("JniComponentBackup")) {
          innerClass = inner;
          break;
        }
      }
      assertThat(innerClass).isNotNull();

      List<String> expectedFields =
          Arrays.asList("backupId", "componentId", "timestamp", "type", "location", "status");

      for (String fieldName : expectedFields) {
        Field field = innerClass.getDeclaredField(fieldName);
        assertThat(field).as("JniComponentBackup should have field: " + fieldName).isNotNull();
      }
    }
  }

  @Nested
  @DisplayName("ComponentVersion Tests")
  class ComponentVersionTests {

    @Test
    @DisplayName("should create valid ComponentVersion")
    void shouldCreateValidComponentVersion() {
      ComponentVersion version = new ComponentVersion(1, 2, 3);

      assertThat(version.getMajor()).isEqualTo(1);
      assertThat(version.getMinor()).isEqualTo(2);
      assertThat(version.getPatch()).isEqualTo(3);
    }

    @Test
    @DisplayName("should compare versions correctly")
    void shouldCompareVersionsCorrectly() {
      ComponentVersion v1 = new ComponentVersion(1, 0, 0);
      ComponentVersion v2 = new ComponentVersion(2, 0, 0);
      ComponentVersion v1Same = new ComponentVersion(1, 0, 0);

      assertThat(v1.compareTo(v2)).isLessThan(0);
      assertThat(v2.compareTo(v1)).isGreaterThan(0);
      assertThat(v1.compareTo(v1Same)).isEqualTo(0);
    }
  }

  @Nested
  @DisplayName("ComponentMetadata Tests")
  class ComponentMetadataTests {

    @Test
    @DisplayName("should create valid ComponentMetadata")
    void shouldCreateValidComponentMetadata() {
      ComponentVersion version = new ComponentVersion(1, 0, 0);
      ComponentMetadata metadata = new ComponentMetadata("test", version, "description");

      assertThat(metadata.getName()).isEqualTo("test");
      assertThat(metadata.getVersion()).isEqualTo(version);
      assertThat(metadata.getDescription()).isEqualTo("description");
    }
  }

  @Nested
  @DisplayName("Private Method Tests")
  class PrivateMethodTests {

    @Test
    @DisplayName("should have ensureValid method")
    void shouldHaveEnsureValidMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("ensureValid");

      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(void.class);
    }

    @Test
    @DisplayName("should have createDefaultMetadata method")
    void shouldHaveCreateDefaultMetadataMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Method method = clazz.getDeclaredMethod("createDefaultMetadata");

      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
      assertThat(method.getReturnType()).isEqualTo(ComponentMetadata.class);
    }

    @Test
    @DisplayName("should have mapStrategyToBackupType method")
    void shouldHaveMapStrategyToBackupTypeMethod() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?> strategyClass =
          Class.forName("ai.tegmentum.wasmtime4j.ComponentBackupConfig$BackupStrategy");
      Method method = clazz.getDeclaredMethod("mapStrategyToBackupType", strategyClass);

      assertThat(Modifier.isPrivate(method.getModifiers())).isTrue();
      assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
    }
  }

  @Nested
  @DisplayName("Inner Class Count Tests")
  class InnerClassCountTests {

    @Test
    @DisplayName("should have all expected inner classes")
    void shouldHaveAllExpectedInnerClasses() throws Exception {
      Class<?> clazz = Class.forName(CLASS_NAME);
      Class<?>[] declaredClasses = clazz.getDeclaredClasses();

      List<String> expectedInnerClasses =
          Arrays.asList(
              "JniComponentAuditLog",
              "JniComponentMetrics",
              "JniComponentResourceLimitsImpl",
              "JniComponentOptimizationResultImpl",
              "JniComponentDebugInfoImpl",
              "JniWitInterfaceIntrospection",
              "JniComponentBackup");

      List<String> actualInnerClassNames =
          Arrays.stream(declaredClasses)
              .map(Class::getSimpleName)
              .collect(java.util.stream.Collectors.toList());

      for (String expected : expectedInnerClasses) {
        assertThat(actualInnerClassNames)
            .as("Should contain inner class: " + expected)
            .contains(expected);
      }
    }
  }
}
