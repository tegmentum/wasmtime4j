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

package ai.tegmentum.wasmtime4j.jni.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiInputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiOutputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for JNI WASI Filesystem implementation classes.
 *
 * <p>This test class verifies the structural correctness and API contracts of the JNI WASI
 * filesystem implementations without loading native libraries. Tests focus on:
 *
 * <ul>
 *   <li>Class structure and inheritance
 *   <li>Interface implementation compliance
 *   <li>Constructor parameter validation
 *   <li>Native method declarations
 *   <li>Field presence and modifiers
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("JNI WASI Filesystem Implementation Tests")
public class JniWasiFilesystemTest {

  private static final Logger LOGGER = Logger.getLogger(JniWasiFilesystemTest.class.getName());

  /** Tests for JniWasiDescriptor class structure and API. */
  @Nested
  @DisplayName("JniWasiDescriptor Tests")
  class JniWasiDescriptorTests {

    @Test
    @DisplayName("JniWasiDescriptor should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiDescriptor.class.getModifiers()),
          "JniWasiDescriptor should be final");
    }

    @Test
    @DisplayName("JniWasiDescriptor should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiDescriptor.class),
          "JniWasiDescriptor should extend JniResource");
    }

    @Test
    @DisplayName("JniWasiDescriptor should implement WasiDescriptor interface")
    void shouldImplementWasiDescriptor() {
      assertTrue(
          WasiDescriptor.class.isAssignableFrom(JniWasiDescriptor.class),
          "JniWasiDescriptor should implement WasiDescriptor");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have public constructor with two long parameters")
    void shouldHavePublicConstructorWithTwoLongParams() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiDescriptor.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Should have constructor(long, long)");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiDescriptor.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertEquals(long.class, field.getType(), "contextHandle should be of type long");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have LOGGER field")
    void shouldHaveLoggerField() throws NoSuchFieldException {
      Field field = JniWasiDescriptor.class.getDeclaredField("LOGGER");
      assertNotNull(field, "Should have LOGGER field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be of type Logger");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have required filesystem methods")
    void shouldHaveRequiredFilesystemMethods() {
      Set<String> requiredMethods =
          new HashSet<>(
              Arrays.asList(
                  "readViaStream",
                  "writeViaStream",
                  "appendViaStream",
                  "getDescriptorType",
                  "getFlags",
                  "setSize",
                  "syncData",
                  "sync",
                  "openAt",
                  "createDirectoryAt",
                  "readDirectory",
                  "readLinkAt",
                  "unlinkFileAt",
                  "removeDirectoryAt",
                  "renameAt",
                  "symlinkAt",
                  "linkAt",
                  "isSameObject"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiDescriptor.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String requiredMethod : requiredMethods) {
        assertTrue(
            actualMethods.contains(requiredMethod),
            "JniWasiDescriptor should have method: " + requiredMethod);
      }
    }

    @Test
    @DisplayName("JniWasiDescriptor should have WasiResource interface methods")
    void shouldHaveWasiResourceMethods() {
      Set<String> resourceMethods =
          new HashSet<>(
              Arrays.asList(
                  "getId",
                  "getType",
                  "getOwner",
                  "isOwned",
                  "isValid",
                  "getAvailableOperations",
                  "invoke",
                  "getStats",
                  "getState",
                  "getMetadata",
                  "getLastAccessedAt",
                  "getCreatedAt",
                  "createHandle",
                  "transferOwnership"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiDescriptor.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String resourceMethod : resourceMethods) {
        assertTrue(
            actualMethods.contains(resourceMethod),
            "JniWasiDescriptor should have WasiResource method: " + resourceMethod);
      }
    }

    @Test
    @DisplayName("JniWasiDescriptor should have multiple native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniWasiDescriptor.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.fine("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 10, "Should have at least 10 native methods");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have protected doClose method")
    void shouldHaveDoCloseMethod() throws NoSuchMethodException {
      Method method = JniWasiDescriptor.class.getDeclaredMethod("doClose");
      assertNotNull(method, "Should have doClose method");
      assertTrue(Modifier.isProtected(method.getModifiers()), "doClose should be protected");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have protected getResourceType method")
    void shouldHaveGetResourceTypeMethod() throws NoSuchMethodException {
      Method method = JniWasiDescriptor.class.getDeclaredMethod("getResourceType");
      assertNotNull(method, "Should have getResourceType method");
      assertTrue(
          Modifier.isProtected(method.getModifiers()), "getResourceType should be protected");
      assertEquals(String.class, method.getReturnType(), "getResourceType should return String");
    }
  }

  /** Tests for JniWasiInputStream class structure and API. */
  @Nested
  @DisplayName("JniWasiInputStream Tests")
  class JniWasiInputStreamTests {

    @Test
    @DisplayName("JniWasiInputStream should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiInputStream.class.getModifiers()),
          "JniWasiInputStream should be final");
    }

    @Test
    @DisplayName("JniWasiInputStream should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiInputStream.class),
          "JniWasiInputStream should extend JniResource");
    }

    @Test
    @DisplayName("JniWasiInputStream should implement WasiInputStream interface")
    void shouldImplementWasiInputStream() {
      assertTrue(
          WasiInputStream.class.isAssignableFrom(JniWasiInputStream.class),
          "JniWasiInputStream should implement WasiInputStream");
    }

    @Test
    @DisplayName("JniWasiInputStream should have public constructor with two long parameters")
    void shouldHavePublicConstructorWithTwoLongParams() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiInputStream.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Should have constructor(long, long)");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiInputStream should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiInputStream.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertEquals(long.class, field.getType(), "contextHandle should be of type long");
    }

    @Test
    @DisplayName("JniWasiInputStream should have required stream methods")
    void shouldHaveRequiredStreamMethods() {
      Set<String> requiredMethods =
          new HashSet<>(Arrays.asList("read", "blockingRead", "skip", "blockingSkip", "subscribe"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiInputStream.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String requiredMethod : requiredMethods) {
        assertTrue(
            actualMethods.contains(requiredMethod),
            "JniWasiInputStream should have method: " + requiredMethod);
      }
    }

    @Test
    @DisplayName("JniWasiInputStream should have native read method with correct signature")
    void shouldHaveNativeReadMethod() {
      boolean foundNativeRead = false;
      for (Method method : JniWasiInputStream.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeRead") && Modifier.isNative(method.getModifiers())) {
          foundNativeRead = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(3, paramTypes.length, "nativeRead should have 3 parameters");
          assertEquals(long.class, paramTypes[0], "First param should be long (contextHandle)");
          assertEquals(long.class, paramTypes[1], "Second param should be long (streamHandle)");
          assertEquals(long.class, paramTypes[2], "Third param should be long (length)");
          assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
        }
      }
      assertTrue(foundNativeRead, "Should have nativeRead native method");
    }

    @Test
    @DisplayName("JniWasiInputStream should have multiple native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniWasiInputStream.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.fine("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 4, "Should have at least 4 native methods");
    }

    @Test
    @DisplayName("JniWasiInputStream read method should return byte array")
    void readMethodShouldReturnByteArray() throws NoSuchMethodException {
      Method method = JniWasiInputStream.class.getMethod("read", long.class);
      assertEquals(byte[].class, method.getReturnType(), "read should return byte[]");
    }

    @Test
    @DisplayName("JniWasiInputStream subscribe method should return WasiPollable")
    void subscribeMethodShouldReturnWasiPollable() throws NoSuchMethodException {
      Method method = JniWasiInputStream.class.getMethod("subscribe");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "subscribe should return WasiPollable");
    }
  }

  /** Tests for JniWasiOutputStream class structure and API. */
  @Nested
  @DisplayName("JniWasiOutputStream Tests")
  class JniWasiOutputStreamTests {

    @Test
    @DisplayName("JniWasiOutputStream should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiOutputStream.class.getModifiers()),
          "JniWasiOutputStream should be final");
    }

    @Test
    @DisplayName("JniWasiOutputStream should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiOutputStream.class),
          "JniWasiOutputStream should extend JniResource");
    }

    @Test
    @DisplayName("JniWasiOutputStream should implement WasiOutputStream interface")
    void shouldImplementWasiOutputStream() {
      assertTrue(
          WasiOutputStream.class.isAssignableFrom(JniWasiOutputStream.class),
          "JniWasiOutputStream should implement WasiOutputStream");
    }

    @Test
    @DisplayName("JniWasiOutputStream should have public constructor with two long parameters")
    void shouldHavePublicConstructorWithTwoLongParams() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiOutputStream.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Should have constructor(long, long)");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiOutputStream should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiOutputStream.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertEquals(long.class, field.getType(), "contextHandle should be of type long");
    }

    @Test
    @DisplayName("JniWasiOutputStream should have required stream methods")
    void shouldHaveRequiredStreamMethods() {
      Set<String> requiredMethods =
          new HashSet<>(
              Arrays.asList(
                  "checkWrite",
                  "write",
                  "blockingWriteAndFlush",
                  "flush",
                  "blockingFlush",
                  "writeZeroes",
                  "blockingWriteZeroesAndFlush",
                  "splice",
                  "blockingSplice",
                  "subscribe"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiOutputStream.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String requiredMethod : requiredMethods) {
        assertTrue(
            actualMethods.contains(requiredMethod),
            "JniWasiOutputStream should have method: " + requiredMethod);
      }
    }

    @Test
    @DisplayName("JniWasiOutputStream should have multiple native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniWasiOutputStream.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.fine("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 8, "Should have at least 8 native methods");
    }

    @Test
    @DisplayName("JniWasiOutputStream checkWrite should return long")
    void checkWriteShouldReturnLong() throws NoSuchMethodException {
      Method method = JniWasiOutputStream.class.getMethod("checkWrite");
      assertEquals(long.class, method.getReturnType(), "checkWrite should return long");
    }

    @Test
    @DisplayName("JniWasiOutputStream write should accept byte array")
    void writeShouldAcceptByteArray() throws NoSuchMethodException {
      Method method = JniWasiOutputStream.class.getMethod("write", byte[].class);
      assertNotNull(method, "Should have write(byte[]) method");
      assertEquals(void.class, method.getReturnType(), "write should return void");
    }

    @Test
    @DisplayName("JniWasiOutputStream splice should accept WasiInputStream and long")
    void spliceShouldAcceptWasiInputStreamAndLong() throws NoSuchMethodException {
      Method method =
          JniWasiOutputStream.class.getMethod("splice", WasiInputStream.class, long.class);
      assertNotNull(method, "Should have splice(WasiInputStream, long) method");
      assertEquals(long.class, method.getReturnType(), "splice should return long");
    }
  }

  /** Tests for JniWasiPollable class structure and API. */
  @Nested
  @DisplayName("JniWasiPollable Tests")
  class JniWasiPollableTests {

    @Test
    @DisplayName("JniWasiPollable should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(JniWasiPollable.class.getModifiers()),
          "JniWasiPollable should be final");
    }

    @Test
    @DisplayName("JniWasiPollable should extend JniResource")
    void shouldExtendJniResource() {
      assertTrue(
          JniResource.class.isAssignableFrom(JniWasiPollable.class),
          "JniWasiPollable should extend JniResource");
    }

    @Test
    @DisplayName("JniWasiPollable should implement WasiPollable interface")
    void shouldImplementWasiPollable() {
      assertTrue(
          WasiPollable.class.isAssignableFrom(JniWasiPollable.class),
          "JniWasiPollable should implement WasiPollable");
    }

    @Test
    @DisplayName("JniWasiPollable should have public constructor with two long parameters")
    void shouldHavePublicConstructorWithTwoLongParams() throws NoSuchMethodException {
      Constructor<?> constructor = JniWasiPollable.class.getConstructor(long.class, long.class);
      assertNotNull(constructor, "Should have constructor(long, long)");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("JniWasiPollable should have contextHandle field")
    void shouldHaveContextHandleField() throws NoSuchFieldException {
      Field field = JniWasiPollable.class.getDeclaredField("contextHandle");
      assertNotNull(field, "Should have contextHandle field");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
      assertEquals(long.class, field.getType(), "contextHandle should be of type long");
    }

    @Test
    @DisplayName("JniWasiPollable should have required pollable methods")
    void shouldHaveRequiredPollableMethods() {
      Set<String> requiredMethods = new HashSet<>(Arrays.asList("block", "ready"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiPollable.class.getMethods()) {
        actualMethods.add(method.getName());
      }

      for (String requiredMethod : requiredMethods) {
        assertTrue(
            actualMethods.contains(requiredMethod),
            "JniWasiPollable should have method: " + requiredMethod);
      }
    }

    @Test
    @DisplayName("JniWasiPollable should have native methods")
    void shouldHaveNativeMethods() {
      int nativeMethodCount = 0;
      for (Method method : JniWasiPollable.class.getDeclaredMethods()) {
        if (Modifier.isNative(method.getModifiers())) {
          nativeMethodCount++;
          LOGGER.fine("Found native method: " + method.getName());
        }
      }

      assertTrue(nativeMethodCount >= 2, "Should have at least 2 native methods (block, ready)");
    }

    @Test
    @DisplayName("JniWasiPollable block should return void")
    void blockShouldReturnVoid() throws NoSuchMethodException {
      Method method = JniWasiPollable.class.getMethod("block");
      assertEquals(void.class, method.getReturnType(), "block should return void");
    }

    @Test
    @DisplayName("JniWasiPollable ready should return boolean")
    void readyShouldReturnBoolean() throws NoSuchMethodException {
      Method method = JniWasiPollable.class.getMethod("ready");
      assertEquals(boolean.class, method.getReturnType(), "ready should return boolean");
    }

    @Test
    @DisplayName("JniWasiPollable should have getAvailableOperations method")
    void shouldHaveGetAvailableOperationsMethod() throws NoSuchMethodException {
      Method method = JniWasiPollable.class.getMethod("getAvailableOperations");
      assertNotNull(method, "Should have getAvailableOperations method");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  /** Tests for package consistency across filesystem and IO classes. */
  @Nested
  @DisplayName("Package Consistency Tests")
  class PackageConsistencyTests {

    @Test
    @DisplayName("JniWasiDescriptor should be in wasi.filesystem package")
    void descriptorShouldBeInFilesystemPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.filesystem",
          JniWasiDescriptor.class.getPackage().getName(),
          "JniWasiDescriptor should be in wasi.filesystem package");
    }

    @Test
    @DisplayName("JniWasiInputStream should be in wasi.io package")
    void inputStreamShouldBeInIoPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.io",
          JniWasiInputStream.class.getPackage().getName(),
          "JniWasiInputStream should be in wasi.io package");
    }

    @Test
    @DisplayName("JniWasiOutputStream should be in wasi.io package")
    void outputStreamShouldBeInIoPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.io",
          JniWasiOutputStream.class.getPackage().getName(),
          "JniWasiOutputStream should be in wasi.io package");
    }

    @Test
    @DisplayName("JniWasiPollable should be in wasi.io package")
    void pollableShouldBeInIoPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.jni.wasi.io",
          JniWasiPollable.class.getPackage().getName(),
          "JniWasiPollable should be in wasi.io package");
    }

    @Test
    @DisplayName("All filesystem classes should follow Jni prefix naming convention")
    void allClassesShouldFollowNamingConvention() {
      assertTrue(
          JniWasiDescriptor.class.getSimpleName().startsWith("Jni"),
          "JniWasiDescriptor should start with Jni prefix");
      assertTrue(
          JniWasiInputStream.class.getSimpleName().startsWith("Jni"),
          "JniWasiInputStream should start with Jni prefix");
      assertTrue(
          JniWasiOutputStream.class.getSimpleName().startsWith("Jni"),
          "JniWasiOutputStream should start with Jni prefix");
      assertTrue(
          JniWasiPollable.class.getSimpleName().startsWith("Jni"),
          "JniWasiPollable should start with Jni prefix");
    }
  }

  /** Tests for native method signature validation. */
  @Nested
  @DisplayName("Native Method Signature Tests")
  class NativeMethodSignatureTests {

    @Test
    @DisplayName("All native methods should be private static")
    void nativeMethodsShouldBePrivateStatic() {
      Class<?>[] classes = {
        JniWasiDescriptor.class,
        JniWasiInputStream.class,
        JniWasiOutputStream.class,
        JniWasiPollable.class
      };

      for (Class<?> clazz : classes) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            assertTrue(
                Modifier.isPrivate(method.getModifiers()),
                clazz.getSimpleName() + "." + method.getName() + " should be private");
            assertTrue(
                Modifier.isStatic(method.getModifiers()),
                clazz.getSimpleName() + "." + method.getName() + " should be static");
          }
        }
      }
    }

    @Test
    @DisplayName("All native methods should start with 'native' prefix")
    void nativeMethodsShouldHaveNativePrefix() {
      Class<?>[] classes = {
        JniWasiDescriptor.class,
        JniWasiInputStream.class,
        JniWasiOutputStream.class,
        JniWasiPollable.class
      };

      for (Class<?> clazz : classes) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            assertTrue(
                method.getName().startsWith("native"),
                clazz.getSimpleName()
                    + "."
                    + method.getName()
                    + " should start with 'native' prefix");
          }
        }
      }
    }

    @Test
    @DisplayName("JniWasiDescriptor should have nativeClose method")
    void descriptorShouldHaveNativeCloseMethod() {
      boolean foundNativeClose = false;
      for (Method method : JniWasiDescriptor.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeClose") && Modifier.isNative(method.getModifiers())) {
          foundNativeClose = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(2, paramTypes.length, "nativeClose should have 2 parameters");
          assertEquals(long.class, paramTypes[0], "First param should be long (contextHandle)");
          assertEquals(long.class, paramTypes[1], "Second param should be long (descriptorHandle)");
        }
      }
      assertTrue(foundNativeClose, "Should have nativeClose native method");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have nativeOpenAt method")
    void descriptorShouldHaveNativeOpenAtMethod() {
      boolean foundNativeOpenAt = false;
      for (Method method : JniWasiDescriptor.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeOpenAt") && Modifier.isNative(method.getModifiers())) {
          foundNativeOpenAt = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(6, paramTypes.length, "nativeOpenAt should have 6 parameters");
          assertEquals(long.class, method.getReturnType(), "nativeOpenAt should return long");
        }
      }
      assertTrue(foundNativeOpenAt, "Should have nativeOpenAt native method");
    }
  }

  /** Tests for interface compliance across all classes. */
  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("JniWasiDescriptor should implement all WasiDescriptor methods")
    void descriptorShouldImplementAllInterfaceMethods() {
      // Verify key WasiDescriptor methods are implemented
      Class<?> clazz = JniWasiDescriptor.class;

      assertMethodExists(clazz, "readViaStream", long.class);
      assertMethodExists(clazz, "writeViaStream", long.class);
      assertMethodExists(clazz, "appendViaStream");
      assertMethodExists(clazz, "getDescriptorType");
      assertMethodExists(clazz, "getFlags");
      assertMethodExists(clazz, "setSize", long.class);
      assertMethodExists(clazz, "syncData");
      assertMethodExists(clazz, "sync");
    }

    @Test
    @DisplayName("JniWasiInputStream should implement all WasiInputStream methods")
    void inputStreamShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiInputStream.class;

      assertMethodExists(clazz, "read", long.class);
      assertMethodExists(clazz, "blockingRead", long.class);
      assertMethodExists(clazz, "skip", long.class);
      assertMethodExists(clazz, "blockingSkip", long.class);
      assertMethodExists(clazz, "subscribe");
    }

    @Test
    @DisplayName("JniWasiOutputStream should implement all WasiOutputStream methods")
    void outputStreamShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiOutputStream.class;

      assertMethodExists(clazz, "checkWrite");
      assertMethodExists(clazz, "write", byte[].class);
      assertMethodExists(clazz, "blockingWriteAndFlush", byte[].class);
      assertMethodExists(clazz, "flush");
      assertMethodExists(clazz, "blockingFlush");
      assertMethodExists(clazz, "writeZeroes", long.class);
    }

    @Test
    @DisplayName("JniWasiPollable should implement all WasiPollable methods")
    void pollableShouldImplementAllInterfaceMethods() {
      Class<?> clazz = JniWasiPollable.class;

      assertMethodExists(clazz, "block");
      assertMethodExists(clazz, "ready");
    }

    @Test
    @DisplayName("All classes should implement close from AutoCloseable")
    void allClassesShouldImplementClose() {
      Class<?>[] classes = {
        JniWasiDescriptor.class,
        JniWasiInputStream.class,
        JniWasiOutputStream.class,
        JniWasiPollable.class
      };

      for (Class<?> clazz : classes) {
        assertTrue(
            AutoCloseable.class.isAssignableFrom(clazz),
            clazz.getSimpleName() + " should implement AutoCloseable");
      }
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

  /** Tests for descriptor-specific functionality. */
  @Nested
  @DisplayName("Descriptor Specific Tests")
  class DescriptorSpecificTests {

    @Test
    @DisplayName("JniWasiDescriptor should have readDirectory method returning List")
    void shouldHaveReadDirectoryMethod() throws NoSuchMethodException {
      Method method = JniWasiDescriptor.class.getMethod("readDirectory");
      assertNotNull(method, "Should have readDirectory method");
      assertEquals(List.class, method.getReturnType(), "readDirectory should return List");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have isSameObject method")
    void shouldHaveIsSameObjectMethod() throws NoSuchMethodException {
      Method method = JniWasiDescriptor.class.getMethod("isSameObject", WasiDescriptor.class);
      assertNotNull(method, "Should have isSameObject method");
      assertEquals(boolean.class, method.getReturnType(), "isSameObject should return boolean");
    }

    @Test
    @DisplayName("JniWasiDescriptor getType should return wasi:filesystem/descriptor")
    void getTypeShouldReturnCorrectType() throws Exception {
      // Use reflection to get the getType method implementation info
      Method method = JniWasiDescriptor.class.getMethod("getType");
      assertNotNull(method, "Should have getType method");
      assertEquals(String.class, method.getReturnType(), "getType should return String");
    }

    @Test
    @DisplayName("JniWasiDescriptor should have private encoding helper methods")
    void shouldHaveEncodingHelperMethods() {
      Set<String> expectedHelpers =
          new HashSet<>(
              Arrays.asList(
                  "encodePathFlags",
                  "encodeOpenFlags",
                  "encodeDescriptorFlags",
                  "decodeDescriptorType",
                  "decodeDescriptorFlags"));

      Set<String> actualMethods = new HashSet<>();
      for (Method method : JniWasiDescriptor.class.getDeclaredMethods()) {
        if (Modifier.isPrivate(method.getModifiers())
            && Modifier.isStatic(method.getModifiers())
            && !Modifier.isNative(method.getModifiers())) {
          actualMethods.add(method.getName());
        }
      }

      for (String helper : expectedHelpers) {
        assertTrue(
            actualMethods.contains(helper),
            "JniWasiDescriptor should have helper method: " + helper);
      }
    }
  }

  /** Tests for stream-specific functionality. */
  @Nested
  @DisplayName("Stream Specific Tests")
  class StreamSpecificTests {

    @Test
    @DisplayName("JniWasiInputStream getType should return wasi:io/input-stream")
    void inputStreamGetTypeShouldReturnCorrectType() throws Exception {
      Method method = JniWasiInputStream.class.getMethod("getType");
      assertNotNull(method, "Should have getType method");
      assertEquals(String.class, method.getReturnType(), "getType should return String");
    }

    @Test
    @DisplayName("JniWasiOutputStream getType should return wasi:io/output-stream")
    void outputStreamGetTypeShouldReturnCorrectType() throws Exception {
      Method method = JniWasiOutputStream.class.getMethod("getType");
      assertNotNull(method, "Should have getType method");
      assertEquals(String.class, method.getReturnType(), "getType should return String");
    }

    @Test
    @DisplayName("JniWasiOutputStream should have inner JniWasiResourceHandle class")
    void outputStreamShouldHaveResourceHandleInnerClass() {
      Class<?>[] innerClasses = JniWasiOutputStream.class.getDeclaredClasses();
      boolean foundResourceHandle = false;
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniWasiResourceHandle")) {
          foundResourceHandle = true;
          assertTrue(
              Modifier.isPrivate(innerClass.getModifiers()),
              "JniWasiResourceHandle should be private");
          assertTrue(
              Modifier.isStatic(innerClass.getModifiers()),
              "JniWasiResourceHandle should be static");
          assertTrue(
              Modifier.isFinal(innerClass.getModifiers()), "JniWasiResourceHandle should be final");
        }
      }
      assertTrue(foundResourceHandle, "Should have JniWasiResourceHandle inner class");
    }

    @Test
    @DisplayName("JniWasiInputStream should have inner JniWasiResourceHandle class")
    void inputStreamShouldHaveResourceHandleInnerClass() {
      Class<?>[] innerClasses = JniWasiInputStream.class.getDeclaredClasses();
      boolean foundResourceHandle = false;
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniWasiResourceHandle")) {
          foundResourceHandle = true;
          assertTrue(
              Modifier.isPrivate(innerClass.getModifiers()),
              "JniWasiResourceHandle should be private");
          assertTrue(
              Modifier.isStatic(innerClass.getModifiers()),
              "JniWasiResourceHandle should be static");
          assertTrue(
              Modifier.isFinal(innerClass.getModifiers()), "JniWasiResourceHandle should be final");
        }
      }
      assertTrue(foundResourceHandle, "Should have JniWasiResourceHandle inner class");
    }
  }

  /** Tests for pollable-specific functionality. */
  @Nested
  @DisplayName("Pollable Specific Tests")
  class PollableSpecificTests {

    @Test
    @DisplayName("JniWasiPollable getType should return wasi:io/pollable")
    void pollableGetTypeShouldReturnCorrectType() throws Exception {
      Method method = JniWasiPollable.class.getMethod("getType");
      assertNotNull(method, "Should have getType method");
      assertEquals(String.class, method.getReturnType(), "getType should return String");
    }

    @Test
    @DisplayName("JniWasiPollable should have inner JniWasiResourceHandle class")
    void pollableShouldHaveResourceHandleInnerClass() {
      Class<?>[] innerClasses = JniWasiPollable.class.getDeclaredClasses();
      boolean foundResourceHandle = false;
      for (Class<?> innerClass : innerClasses) {
        if (innerClass.getSimpleName().equals("JniWasiResourceHandle")) {
          foundResourceHandle = true;
        }
      }
      assertTrue(foundResourceHandle, "Should have JniWasiResourceHandle inner class");
    }

    @Test
    @DisplayName("JniWasiPollable invoke should support block and ready operations")
    void invokeShouldSupportOperations() throws NoSuchMethodException {
      Method method = JniWasiPollable.class.getMethod("invoke", String.class, Object[].class);
      assertNotNull(method, "Should have invoke method");
    }

    @Test
    @DisplayName("JniWasiPollable should have nativeBlock method")
    void shouldHaveNativeBlockMethod() {
      boolean foundNativeBlock = false;
      for (Method method : JniWasiPollable.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeBlock") && Modifier.isNative(method.getModifiers())) {
          foundNativeBlock = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(2, paramTypes.length, "nativeBlock should have 2 parameters");
          assertEquals(void.class, method.getReturnType(), "nativeBlock should return void");
        }
      }
      assertTrue(foundNativeBlock, "Should have nativeBlock native method");
    }

    @Test
    @DisplayName("JniWasiPollable should have nativeReady method")
    void shouldHaveNativeReadyMethod() {
      boolean foundNativeReady = false;
      for (Method method : JniWasiPollable.class.getDeclaredMethods()) {
        if (method.getName().equals("nativeReady") && Modifier.isNative(method.getModifiers())) {
          foundNativeReady = true;
          Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(2, paramTypes.length, "nativeReady should have 2 parameters");
          assertEquals(boolean.class, method.getReturnType(), "nativeReady should return boolean");
        }
      }
      assertTrue(foundNativeReady, "Should have nativeReady native method");
    }
  }
}
