/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.jni.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfig;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpConfigBuilder;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for JNI WASI HTTP implementation classes.
 *
 * <p>This test class verifies the structural correctness and API contracts of the JNI WASI HTTP
 * implementations without loading native libraries. Tests focus on:
 *
 * <ul>
 *   <li>Class structure and inheritance
 *   <li>Interface implementation compliance
 *   <li>Builder pattern functionality
 *   <li>Configuration immutability
 *   <li>Statistics tracking with atomic operations
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("JNI WASI HTTP Implementation Tests")
public class JniWasiHttpTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiHttpTest.class.getName());

  /** Tests for JniWasiHttpConfig class structure and API. */
  @Nested
  @DisplayName("JniWasiHttpConfig Tests")
  class JniWasiHttpConfigTests {

    @Test
    @DisplayName("JniWasiHttpConfig should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiHttpConfig.class.getModifiers()),
          "JniWasiHttpConfig should be final");
    }

    @Test
    @DisplayName("JniWasiHttpConfig should implement WasiHttpConfig interface")
    void shouldImplementWasiHttpConfig() {
      assertTrue(
          WasiHttpConfig.class.isAssignableFrom(JniWasiHttpConfig.class),
          "JniWasiHttpConfig should implement WasiHttpConfig");
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor =
          JniWasiHttpConfig.class.getDeclaredConstructor(
              Set.class,
              Set.class,
              Duration.class,
              Duration.class,
              Duration.class,
              Integer.class,
              Integer.class,
              Long.class,
              Long.class,
              List.class,
              boolean.class,
              boolean.class,
              boolean.class,
              boolean.class,
              boolean.class,
              Integer.class,
              String.class);
      assertNotNull(constructor, "Should have constructor with all parameters");
      assertFalse(
          Modifier.isPublic(constructor.getModifiers()), "Constructor should not be public");
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have all expected fields")
    void shouldHaveAllExpectedFields() {
      Set<String> expectedFields =
          new HashSet<>(
              Arrays.asList(
                  "allowedHosts",
                  "blockedHosts",
                  "connectTimeout",
                  "readTimeout",
                  "writeTimeout",
                  "maxConnections",
                  "maxConnectionsPerHost",
                  "maxRequestBodySize",
                  "maxResponseBodySize",
                  "allowedMethods",
                  "httpsRequired",
                  "certificateValidationEnabled",
                  "http2Enabled",
                  "connectionPoolingEnabled",
                  "followRedirects",
                  "maxRedirects",
                  "userAgent"));

      Set<String> actualFields = new HashSet<>();
      for (Field field : JniWasiHttpConfig.class.getDeclaredFields()) {
        actualFields.add(field.getName());
      }

      for (String expected : expectedFields) {
        assertTrue(
            actualFields.contains(expected), "JniWasiHttpConfig should have field: " + expected);
      }
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have required WasiHttpConfig methods")
    void shouldHaveRequiredMethods() {
      Set<String> requiredMethods =
          new HashSet<>(
              Arrays.asList(
                  "getAllowedHosts",
                  "getBlockedHosts",
                  "getConnectTimeout",
                  "getReadTimeout",
                  "getWriteTimeout",
                  "getMaxConnections",
                  "getMaxConnectionsPerHost",
                  "getMaxRequestBodySize",
                  "getMaxResponseBodySize",
                  "getAllowedMethods",
                  "isHttpsRequired",
                  "isCertificateValidationEnabled",
                  "isHttp2Enabled",
                  "isConnectionPoolingEnabled",
                  "isFollowRedirects",
                  "getMaxRedirects",
                  "getUserAgent",
                  "toBuilder",
                  "validate"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiHttpConfig.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String required : requiredMethods) {
        assertTrue(
            actualMethods.contains(required), "JniWasiHttpConfig should have method: " + required);
      }
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have proper equals and hashCode methods")
    void shouldHaveEqualsAndHashCode() throws NoSuchMethodException {
      Method equals = JniWasiHttpConfig.class.getMethod("equals", Object.class);
      Method hashCode = JniWasiHttpConfig.class.getMethod("hashCode");

      assertNotNull(equals, "Should have equals method");
      assertNotNull(hashCode, "Should have hashCode method");

      // Verify they are overridden (declared in this class)
      assertEquals(
          JniWasiHttpConfig.class,
          equals.getDeclaringClass(),
          "equals should be declared in JniWasiHttpConfig");
      assertEquals(
          JniWasiHttpConfig.class,
          hashCode.getDeclaringClass(),
          "hashCode should be declared in JniWasiHttpConfig");
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      Method toString = JniWasiHttpConfig.class.getMethod("toString");
      assertNotNull(toString, "Should have toString method");
      assertEquals(
          JniWasiHttpConfig.class,
          toString.getDeclaringClass(),
          "toString should be declared in JniWasiHttpConfig");
    }

    @Test
    @DisplayName("JniWasiHttpConfig timeout methods should return Optional<Duration>")
    void timeoutMethodsShouldReturnOptionalDuration() throws NoSuchMethodException {
      Method getConnectTimeout = JniWasiHttpConfig.class.getMethod("getConnectTimeout");
      Method getReadTimeout = JniWasiHttpConfig.class.getMethod("getReadTimeout");
      Method getWriteTimeout = JniWasiHttpConfig.class.getMethod("getWriteTimeout");

      assertEquals(Optional.class, getConnectTimeout.getReturnType());
      assertEquals(Optional.class, getReadTimeout.getReturnType());
      assertEquals(Optional.class, getWriteTimeout.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpConfig getAllowedHosts should return Set<String>")
    void getAllowedHostsShouldReturnSetString() throws NoSuchMethodException {
      Method method = JniWasiHttpConfig.class.getMethod("getAllowedHosts");
      assertEquals(Set.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpConfig getAllowedMethods should return List<String>")
    void getAllowedMethodsShouldReturnListString() throws NoSuchMethodException {
      Method method = JniWasiHttpConfig.class.getMethod("getAllowedMethods");
      assertEquals(List.class, method.getReturnType());
    }
  }

  /** Tests for JniWasiHttpConfigBuilder class structure and API. */
  @Nested
  @DisplayName("JniWasiHttpConfigBuilder Tests")
  class JniWasiHttpConfigBuilderTests {

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiHttpConfigBuilder.class.getModifiers()),
          "JniWasiHttpConfigBuilder should be final");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should implement WasiHttpConfigBuilder interface")
    void shouldImplementWasiHttpConfigBuilder() {
      assertTrue(
          WasiHttpConfigBuilder.class.isAssignableFrom(JniWasiHttpConfigBuilder.class),
          "JniWasiHttpConfigBuilder should implement WasiHttpConfigBuilder");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should have public no-arg constructor")
    void shouldHavePublicNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiHttpConfigBuilder.class.getConstructor();
      assertNotNull(constructor, "Should have no-arg constructor");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should have all fluent builder methods")
    void shouldHaveFluentBuilderMethods() {
      Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "allowHost",
                  "allowHosts",
                  "allowAllHosts",
                  "blockHost",
                  "blockHosts",
                  "withConnectTimeout",
                  "withReadTimeout",
                  "withWriteTimeout",
                  "withMaxConnections",
                  "withMaxConnectionsPerHost",
                  "withMaxRequestBodySize",
                  "withMaxResponseBodySize",
                  "allowMethods",
                  "requireHttps",
                  "withCertificateValidation",
                  "withHttp2",
                  "withConnectionPooling",
                  "followRedirects",
                  "withMaxRedirects",
                  "withUserAgent",
                  "build"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiHttpConfigBuilder.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "JniWasiHttpConfigBuilder should have method: " + expected);
      }
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder builder methods should return WasiHttpConfigBuilder")
    void builderMethodsShouldReturnBuilder() throws NoSuchMethodException {
      Method allowHost = JniWasiHttpConfigBuilder.class.getMethod("allowHost", String.class);
      Method withConnectTimeout =
          JniWasiHttpConfigBuilder.class.getMethod("withConnectTimeout", Duration.class);
      Method requireHttps = JniWasiHttpConfigBuilder.class.getMethod("requireHttps", boolean.class);

      assertEquals(WasiHttpConfigBuilder.class, allowHost.getReturnType());
      assertEquals(WasiHttpConfigBuilder.class, withConnectTimeout.getReturnType());
      assertEquals(WasiHttpConfigBuilder.class, requireHttps.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder build method should return WasiHttpConfig")
    void buildMethodShouldReturnWasiHttpConfig() throws NoSuchMethodException {
      Method build = JniWasiHttpConfigBuilder.class.getMethod("build");
      assertEquals(WasiHttpConfig.class, build.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder allowHosts should accept Collection")
    void allowHostsShouldAcceptCollection() throws NoSuchMethodException {
      Method method = JniWasiHttpConfigBuilder.class.getMethod("allowHosts", Collection.class);
      assertNotNull(method, "Should have allowHosts(Collection) method");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder blockHosts should accept Collection")
    void blockHostsShouldAcceptCollection() throws NoSuchMethodException {
      Method method = JniWasiHttpConfigBuilder.class.getMethod("blockHosts", Collection.class);
      assertNotNull(method, "Should have blockHosts(Collection) method");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder allowMethods should accept varargs String")
    void allowMethodsShouldAcceptVarargs() throws NoSuchMethodException {
      Method method = JniWasiHttpConfigBuilder.class.getMethod("allowMethods", String[].class);
      assertNotNull(method, "Should have allowMethods(String...) method");
    }
  }

  /** Tests for JniWasiHttpContext class structure and API. */
  @Nested
  @DisplayName("JniWasiHttpContext Tests")
  class JniWasiHttpContextTests {

    @Test
    @DisplayName("JniWasiHttpContext should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiHttpContext.class.getModifiers()),
          "JniWasiHttpContext should be final");
    }

    @Test
    @DisplayName("JniWasiHttpContext should implement WasiHttpContext interface")
    void shouldImplementWasiHttpContext() {
      assertTrue(
          WasiHttpContext.class.isAssignableFrom(JniWasiHttpContext.class),
          "JniWasiHttpContext should implement WasiHttpContext");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have public constructor with WasiHttpConfig")
    void shouldHavePublicConstructorWithConfig() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiHttpContext.class.getConstructor(WasiHttpConfig.class);
      assertNotNull(constructor, "Should have constructor(WasiHttpConfig)");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have all WasiHttpContext methods")
    void shouldHaveAllContextMethods() {
      Set<String> requiredMethods =
          new HashSet<>(
              Arrays.asList(
                  "addToLinker",
                  "getConfig",
                  "isValid",
                  "isHostAllowed",
                  "close"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiHttpContext.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String required : requiredMethods) {
        assertTrue(
            actualMethods.contains(required), "JniWasiHttpContext should have method: " + required);
      }
    }

    @Test
    @DisplayName("JniWasiHttpContext should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniWasiHttpContext.class.getDeclaredField("LOGGER");
      assertNotNull(field, "Should have LOGGER field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have closed AtomicBoolean field")
    void shouldHaveClosedAtomicBooleanField() throws NoSuchFieldException {
      Field field = JniWasiHttpContext.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");
      assertEquals(AtomicBoolean.class, field.getType(), "closed should be AtomicBoolean");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      Set<String> expectedNative =
          new HashSet<>(
              Arrays.asList("nativeCreate", "nativeIsValid", "nativeFree"));

      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.fine("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 3, "Should have at least 3 native methods");
    }

    @Test
    @DisplayName("JniWasiHttpContext getConfig should return WasiHttpConfig")
    void getConfigShouldReturnWasiHttpConfig() throws NoSuchMethodException {
      Method method = JniWasiHttpContext.class.getMethod("getConfig");
      assertEquals(WasiHttpConfig.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpContext isHostAllowed should accept String and return boolean")
    void isHostAllowedShouldAcceptStringReturnBoolean() throws NoSuchMethodException {
      Method method = JniWasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertEquals(boolean.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpContext should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      Method method = JniWasiHttpContext.class.getMethod("getNativeHandle");
      assertNotNull(method, "Should have getNativeHandle method");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpContext should have getContextId method")
    void shouldHaveGetContextIdMethod() throws NoSuchMethodException {
      Method method = JniWasiHttpContext.class.getMethod("getContextId");
      assertNotNull(method, "Should have getContextId method");
      assertEquals(long.class, method.getReturnType());
    }

    @Test
    @DisplayName("JniWasiHttpContext should have toString method")
    void shouldHaveToStringMethod() throws NoSuchMethodException {
      Method toString = JniWasiHttpContext.class.getMethod("toString");
      assertNotNull(toString, "Should have toString method");
      assertEquals(
          JniWasiHttpContext.class,
          toString.getDeclaringClass(),
          "toString should be declared in JniWasiHttpContext");
    }

    @Test
    @DisplayName("JniWasiHttpContext should implement AutoCloseable via close method")
    void shouldImplementAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(JniWasiHttpContext.class),
          "JniWasiHttpContext should implement AutoCloseable");
    }
  }

  /** Tests for package consistency across HTTP classes. */
  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("JniWasiHttpConfig should be in wasi.http package")
    void configShouldBeInHttpPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.http",
          JniWasiHttpConfig.class.getPackage().getName(),
          "JniWasiHttpConfig should be in wasi.http package");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should be in wasi.http package")
    void configBuilderShouldBeInHttpPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.http",
          JniWasiHttpConfigBuilder.class.getPackage().getName(),
          "JniWasiHttpConfigBuilder should be in wasi.http package");
    }

    @Test
    @DisplayName("JniWasiHttpContext should be in wasi.http package")
    void contextShouldBeInHttpPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.http",
          JniWasiHttpContext.class.getPackage().getName(),
          "JniWasiHttpContext should be in wasi.http package");
    }

    @Test
    @DisplayName("All HTTP classes should follow Jni prefix naming convention")
    void allClassesShouldFollowNamingConvention() {
      assertTrue(
          JniWasiHttpConfig.class.getSimpleName().startsWith("Jni"),
          "JniWasiHttpConfig should start with Jni prefix");
      assertTrue(
          JniWasiHttpConfigBuilder.class.getSimpleName().startsWith("Jni"),
          "JniWasiHttpConfigBuilder should start with Jni prefix");
      assertTrue(
          JniWasiHttpContext.class.getSimpleName().startsWith("Jni"),
          "JniWasiHttpContext should start with Jni prefix");
    }
  }

  /** Tests for interface compliance across HTTP classes. */
  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniWasiHttpConfig should implement all WasiHttpConfig methods")
    void configShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiHttpConfig.class;

      assertMethodExists(clazz, "getAllowedHosts");
      assertMethodExists(clazz, "getBlockedHosts");
      assertMethodExists(clazz, "getConnectTimeout");
      assertMethodExists(clazz, "getReadTimeout");
      assertMethodExists(clazz, "getWriteTimeout");
      assertMethodExists(clazz, "getMaxConnections");
      assertMethodExists(clazz, "getMaxConnectionsPerHost");
      assertMethodExists(clazz, "isHttpsRequired");
      assertMethodExists(clazz, "isCertificateValidationEnabled");
      assertMethodExists(clazz, "isHttp2Enabled");
      assertMethodExists(clazz, "isConnectionPoolingEnabled");
      assertMethodExists(clazz, "validate");
      assertMethodExists(clazz, "toBuilder");
    }

    @Test
    @DisplayName("JniWasiHttpConfigBuilder should implement all WasiHttpConfigBuilder methods")
    void configBuilderShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiHttpConfigBuilder.class;

      assertMethodExists(clazz, "allowHost", String.class);
      assertMethodExists(clazz, "allowHosts", Collection.class);
      assertMethodExists(clazz, "allowAllHosts");
      assertMethodExists(clazz, "blockHost", String.class);
      assertMethodExists(clazz, "blockHosts", Collection.class);
      assertMethodExists(clazz, "withConnectTimeout", Duration.class);
      assertMethodExists(clazz, "withReadTimeout", Duration.class);
      assertMethodExists(clazz, "withWriteTimeout", Duration.class);
      assertMethodExists(clazz, "build");
    }

    @Test
    @DisplayName("JniWasiHttpContext should implement all WasiHttpContext methods")
    void contextShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiHttpContext.class;

      assertMethodExists(clazz, "getConfig");
      assertMethodExists(clazz, "isValid");
      assertMethodExists(clazz, "isHostAllowed", String.class);
      assertMethodExists(clazz, "close");
    }

    private void assertMethodExists(Class<?> clazz, String methodName, Class<?>... paramTypes) {
      try {
        Method method = clazz.getMethod(methodName, paramTypes);
        assertNotNull(method, clazz.getSimpleName() + " should have method " + methodName);
      } catch (NoSuchMethodException e) {
        throw new AssertionError(
            clazz.getSimpleName()
                + " should have method "
                + methodName
                + " with params "
                + Arrays.toString(paramTypes));
      }
    }
  }

  /** Tests for HTTP-specific functionality. */
  @Nested
  @DisplayName("HTTP Specific Tests")
  class HttpSpecificTests {

    @Test
    @DisplayName("JniWasiHttpContext should have matchesAnyPattern helper method")
    void contextShouldHaveMatchingHelperMethod() {
      boolean foundMethod = false;
      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (method.getName().equals("matchesAnyPattern")
            || method.getName().equals("matchesPattern")) {
          foundMethod = true;
          assertTrue(
              Modifier.isPrivate(method.getModifiers()),
              "Pattern matching helper should be private");
        }
      }
      assertTrue(foundMethod, "Should have pattern matching helper method");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have CONTEXT_ID_GENERATOR field")
    void contextShouldHaveContextIdGenerator() throws NoSuchFieldException {
      Field field = JniWasiHttpContext.class.getDeclaredField("CONTEXT_ID_GENERATOR");
      assertNotNull(field, "Should have CONTEXT_ID_GENERATOR field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "Should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "Should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "Should be final");
      assertEquals(AtomicLong.class, field.getType(), "Should be AtomicLong");
    }

    @Test
    @DisplayName("JniWasiHttpConfig should have all immutable collection fields")
    void configShouldHaveImmutableCollectionFields() throws NoSuchFieldException {
      final Field allowedHosts = JniWasiHttpConfig.class.getDeclaredField("allowedHosts");
      assertTrue(Modifier.isPrivate(allowedHosts.getModifiers()));
      assertTrue(Modifier.isFinal(allowedHosts.getModifiers()));

      final Field blockedHosts = JniWasiHttpConfig.class.getDeclaredField("blockedHosts");
      assertTrue(Modifier.isPrivate(blockedHosts.getModifiers()));
      assertTrue(Modifier.isFinal(blockedHosts.getModifiers()));

      final Field allowedMethods = JniWasiHttpConfig.class.getDeclaredField("allowedMethods");
      assertTrue(Modifier.isPrivate(allowedMethods.getModifiers()));
      assertTrue(Modifier.isFinal(allowedMethods.getModifiers()));
    }
  }

  /** Tests for native method signature validation. */
  @Nested
  @DisplayName("Native Method Signature Tests")
  class NativeMethodSignatureTests {

    @Test
    @DisplayName("All native methods should be private static")
    void nativeMethodsShouldBePrivateStatic() {
      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              Modifier.isPrivate(method.getModifiers()), method.getName() + " should be private");
          assertTrue(
              Modifier.isStatic(method.getModifiers()), method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("All native methods should start with 'native' prefix")
    void nativeMethodsShouldHaveNativePrefix() {
      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          assertTrue(
              method.getName().startsWith("native"),
              method.getName() + " should start with 'native' prefix");
        }
      }
    }

    @Test
    @DisplayName("JniWasiHttpContext should have nativeFree method")
    void contextShouldHaveNativeFreeMethod() {
      boolean foundNativeFree = false;
      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeFree") && Modifier.isNative(method.getModifiers())) {
          foundNativeFree = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(1, paramTypes.length, "nativeFree should have 1 parameter");
          assertEquals(long.class, paramTypes[0], "Parameter should be long (contextHandle)");
          assertEquals(void.class, method.getReturnType(), "nativeFree should return void");
        }
      }
      assertTrue(foundNativeFree, "Should have nativeFree native method");
    }

    @Test
    @DisplayName("JniWasiHttpContext should have nativeCreate method")
    void contextShouldHaveNativeCreateMethod() {
      boolean foundNativeCreate = false;
      for (Method method : JniWasiHttpContext.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeCreate") && Modifier.isNative(method.getModifiers())) {
          foundNativeCreate = true;
          assertEquals(long.class, method.getReturnType(), "nativeCreate should return long");
        }
      }
      assertTrue(foundNativeCreate, "Should have nativeCreate native method");
    }
  }
}
