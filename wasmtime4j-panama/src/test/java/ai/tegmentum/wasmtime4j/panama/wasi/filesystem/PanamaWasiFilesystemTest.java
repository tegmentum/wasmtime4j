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

package ai.tegmentum.wasmtime4j.panama.wasi.filesystem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.filesystem.WasiDescriptor;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.invoke.MethodHandle;
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
 * Comprehensive tests for Panama WASI Filesystem implementation classes.
 *
 * <p>Tests cover class structure, interface compliance, Panama FFI patterns, and MethodHandle field
 * verification for:
 *
 * <ul>
 *   <li>PanamaWasiDescriptor - WASI filesystem descriptor operations
 * </ul>
 *
 * <p>Note: These tests use Class.forName with initialize=false to load classes without triggering
 * static initializers, which would attempt to load native libraries. This allows testing the class
 * structure without runtime dependencies.
 */
@DisplayName("Panama WASI Filesystem Tests")
class PanamaWasiFilesystemTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiFilesystemTest.class.getName());

  private static final String DESCRIPTOR_CLASS =
      "ai.tegmentum.wasmtime4j.panama.wasi.filesystem.PanamaWasiDescriptor";

  /**
   * Loads a class without initializing it (no static initializer runs). This prevents native
   * library loading attempts.
   */
  private static Class<?> loadClassWithoutInit(final String className)
      throws ClassNotFoundException {
    return Class.forName(className, false, PanamaWasiFilesystemTest.class.getClassLoader());
  }

  @Nested
  @DisplayName("PanamaWasiDescriptor Class Structure Tests")
  class DescriptorClassStructureTests {

    @Test
    @DisplayName("PanamaWasiDescriptor should exist and be public final")
    void descriptorClassShouldExistAndBePublic() {
      final Class<?> clazz =
          assertDoesNotThrow(
              () -> loadClassWithoutInit(DESCRIPTOR_CLASS),
              "PanamaWasiDescriptor class should exist");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PanamaWasiDescriptor should be public");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaWasiDescriptor should be final");
      LOGGER.info(
          "PanamaWasiDescriptor class verified: public="
              + Modifier.isPublic(clazz.getModifiers())
              + ", final="
              + Modifier.isFinal(clazz.getModifiers()));
    }

    @Test
    @DisplayName("PanamaWasiDescriptor should implement WasiDescriptor interface")
    void descriptorShouldImplementInterface() {
      final Class<?> clazz = assertDoesNotThrow(() -> loadClassWithoutInit(DESCRIPTOR_CLASS));
      assertTrue(
          WasiDescriptor.class.isAssignableFrom(clazz),
          "PanamaWasiDescriptor should implement WasiDescriptor");
      LOGGER.info(
          "PanamaWasiDescriptor implements WasiDescriptor: "
              + WasiDescriptor.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("PanamaWasiDescriptor should extend PanamaResource")
    void descriptorShouldExtendPanamaResource() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      final Class<?> superclass = clazz.getSuperclass();
      assertNotNull(superclass, "PanamaWasiDescriptor should have a superclass");
      assertEquals(
          "PanamaResource",
          superclass.getSimpleName(),
          "PanamaWasiDescriptor should extend PanamaResource");
      LOGGER.info("PanamaWasiDescriptor extends: " + superclass.getName());
    }

    @Test
    @DisplayName("PanamaWasiDescriptor should have required filesystem methods")
    void descriptorShouldHaveRequiredMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      // Note: close is inherited from PanamaResource
      final Set<String> requiredMethods = new HashSet<>();
      // Stream operations
      requiredMethods.add("readViaStream");
      requiredMethods.add("writeViaStream");
      requiredMethods.add("appendViaStream");
      // Metadata operations
      requiredMethods.add("getDescriptorType");
      requiredMethods.add("getFlags");
      requiredMethods.add("setSize");
      requiredMethods.add("syncData");
      requiredMethods.add("sync");
      // Directory operations
      requiredMethods.add("openAt");
      requiredMethods.add("createDirectoryAt");
      requiredMethods.add("readDirectory");
      requiredMethods.add("readLinkAt");
      // File operations
      requiredMethods.add("unlinkFileAt");
      requiredMethods.add("removeDirectoryAt");
      // Path operations
      requiredMethods.add("renameAt");
      requiredMethods.add("symlinkAt");
      requiredMethods.add("linkAt");
      // Utility operations
      requiredMethods.add("isSameObject");
      requiredMethods.add("doClose"); // Protected method that implements actual close

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : requiredMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiDescriptor should have method: " + methodName);
        LOGGER.info("Found required method: " + methodName);
      }
    }

    @Test
    @DisplayName("PanamaWasiDescriptor should have MethodHandle fields for FFI")
    void descriptorShouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      int methodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be static");
          assertTrue(
              Modifier.isFinal(field.getModifiers()),
              "MethodHandle field " + field.getName() + " should be final");
          methodHandleCount++;
          LOGGER.info("Found MethodHandle field: " + field.getName());
        }
      }
      // PanamaWasiDescriptor has many FFI operations
      assertTrue(
          methodHandleCount >= 15,
          "PanamaWasiDescriptor should have at least 15 MethodHandle fields for FFI, found: "
              + methodHandleCount);
      LOGGER.info("Total MethodHandle fields: " + methodHandleCount);
    }

    @Test
    @DisplayName("PanamaWasiDescriptor should have contextHandle field")
    void descriptorShouldHaveContextHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasContextHandle = false;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getName().equals("contextHandle")) {
          hasContextHandle = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "contextHandle should be private");
          assertTrue(Modifier.isFinal(field.getModifiers()), "contextHandle should be final");
          LOGGER.info("Found contextHandle field with type: " + field.getType().getName());
          break;
        }
      }
      assertTrue(hasContextHandle, "PanamaWasiDescriptor should have contextHandle field");
    }
  }

  @Nested
  @DisplayName("Descriptor Stream API Tests")
  class DescriptorStreamApiTests {

    @Test
    @DisplayName("readViaStream should return WasiInputStream")
    void readViaStreamShouldReturnInputStream() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasReadViaStream = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("readViaStream")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == long.class) {
          hasReadViaStream = true;
          assertTrue(
              WasiInputStream.class.isAssignableFrom(method.getReturnType()),
              "readViaStream(long) should return WasiInputStream");
          LOGGER.info("Found readViaStream(long) with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasReadViaStream, "WasiDescriptor should have readViaStream(long) method");
    }

    @Test
    @DisplayName("writeViaStream should return WasiOutputStream")
    void writeViaStreamShouldReturnOutputStream() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasWriteViaStream = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("writeViaStream")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == long.class) {
          hasWriteViaStream = true;
          assertTrue(
              WasiOutputStream.class.isAssignableFrom(method.getReturnType()),
              "writeViaStream(long) should return WasiOutputStream");
          LOGGER.info("Found writeViaStream(long) with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasWriteViaStream, "WasiDescriptor should have writeViaStream(long) method");
    }

    @Test
    @DisplayName("appendViaStream should return WasiOutputStream")
    void appendViaStreamShouldReturnOutputStream() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasAppendViaStream = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("appendViaStream") && method.getParameterCount() == 0) {
          hasAppendViaStream = true;
          assertTrue(
              WasiOutputStream.class.isAssignableFrom(method.getReturnType()),
              "appendViaStream() should return WasiOutputStream");
          LOGGER.info("Found appendViaStream() with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasAppendViaStream, "WasiDescriptor should have appendViaStream() method");
    }
  }

  @Nested
  @DisplayName("Descriptor Metadata API Tests")
  class DescriptorMetadataApiTests {

    @Test
    @DisplayName("getDescriptorType should return DescriptorType enum")
    void getDescriptorTypeShouldReturnEnumType() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasGetDescriptorType = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("getDescriptorType") && method.getParameterCount() == 0) {
          hasGetDescriptorType = true;
          assertTrue(
              method.getReturnType().isEnum()
                  || method.getReturnType().getName().contains("DescriptorType"),
              "getDescriptorType() should return DescriptorType enum");
          LOGGER.info("Found getDescriptorType() with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasGetDescriptorType, "WasiDescriptor should have getDescriptorType() method");
    }

    @Test
    @DisplayName("getFlags should return Set of DescriptorFlags")
    void getFlagsShouldReturnSetOfFlags() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasGetFlags = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("getFlags") && method.getParameterCount() == 0) {
          hasGetFlags = true;
          assertTrue(
              Set.class.isAssignableFrom(method.getReturnType()), "getFlags() should return Set");
          LOGGER.info("Found getFlags() with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasGetFlags, "WasiDescriptor should have getFlags() method");
    }

    @Test
    @DisplayName("setSize should accept long parameter")
    void setSizeShouldAcceptLong() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasSetSize = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("setSize")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == long.class) {
          hasSetSize = true;
          assertEquals(void.class, method.getReturnType(), "setSize(long) should return void");
          LOGGER.info("Found setSize(long) method");
          break;
        }
      }
      assertTrue(hasSetSize, "WasiDescriptor should have setSize(long) method");
    }

    @Test
    @DisplayName("sync methods should return void")
    void syncMethodsShouldReturnVoid() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasSync = false;
      boolean hasSyncData = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("sync") && method.getParameterCount() == 0) {
          hasSync = true;
          assertEquals(void.class, method.getReturnType(), "sync() should return void");
          LOGGER.info("Found sync() method");
        }
        if (method.getName().equals("syncData") && method.getParameterCount() == 0) {
          hasSyncData = true;
          assertEquals(void.class, method.getReturnType(), "syncData() should return void");
          LOGGER.info("Found syncData() method");
        }
      }
      assertTrue(hasSync, "WasiDescriptor should have sync() method");
      assertTrue(hasSyncData, "WasiDescriptor should have syncData() method");
    }
  }

  @Nested
  @DisplayName("Descriptor Directory API Tests")
  class DescriptorDirectoryApiTests {

    @Test
    @DisplayName("openAt should return WasiDescriptor")
    void openAtShouldReturnDescriptor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasOpenAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("openAt")) {
          hasOpenAt = true;
          assertTrue(
              WasiDescriptor.class.isAssignableFrom(method.getReturnType()),
              "openAt should return WasiDescriptor");
          LOGGER.info(
              "Found openAt with return type: "
                  + method.getReturnType()
                  + ", params: "
                  + Arrays.toString(method.getParameterTypes()));
          break;
        }
      }
      assertTrue(hasOpenAt, "WasiDescriptor should have openAt method");
    }

    @Test
    @DisplayName("createDirectoryAt should accept String path")
    void createDirectoryAtShouldAcceptPath() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasCreateDirectoryAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("createDirectoryAt")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == String.class) {
          hasCreateDirectoryAt = true;
          assertEquals(
              void.class, method.getReturnType(), "createDirectoryAt(String) should return void");
          LOGGER.info("Found createDirectoryAt(String) method");
          break;
        }
      }
      assertTrue(hasCreateDirectoryAt, "WasiDescriptor should have createDirectoryAt(String)");
    }

    @Test
    @DisplayName("readDirectory should return List")
    void readDirectoryShouldReturnList() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasReadDirectory = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("readDirectory") && method.getParameterCount() == 0) {
          hasReadDirectory = true;
          assertTrue(
              List.class.isAssignableFrom(method.getReturnType()),
              "readDirectory() should return List");
          LOGGER.info("Found readDirectory() with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasReadDirectory, "WasiDescriptor should have readDirectory() method");
    }
  }

  @Nested
  @DisplayName("Descriptor File Operations Tests")
  class DescriptorFileOperationsTests {

    @Test
    @DisplayName("unlinkFileAt should accept String path")
    void unlinkFileAtShouldAcceptPath() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasUnlinkFileAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("unlinkFileAt")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == String.class) {
          hasUnlinkFileAt = true;
          assertEquals(
              void.class, method.getReturnType(), "unlinkFileAt(String) should return void");
          LOGGER.info("Found unlinkFileAt(String) method");
          break;
        }
      }
      assertTrue(hasUnlinkFileAt, "WasiDescriptor should have unlinkFileAt(String)");
    }

    @Test
    @DisplayName("removeDirectoryAt should accept String path")
    void removeDirectoryAtShouldAcceptPath() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasRemoveDirectoryAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("removeDirectoryAt")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == String.class) {
          hasRemoveDirectoryAt = true;
          assertEquals(
              void.class, method.getReturnType(), "removeDirectoryAt(String) should return void");
          LOGGER.info("Found removeDirectoryAt(String) method");
          break;
        }
      }
      assertTrue(hasRemoveDirectoryAt, "WasiDescriptor should have removeDirectoryAt(String)");
    }
  }

  @Nested
  @DisplayName("Descriptor Path Operations Tests")
  class DescriptorPathOperationsTests {

    @Test
    @DisplayName("renameAt should accept paths and descriptor")
    void renameAtShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasRenameAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("renameAt") && method.getParameterCount() == 3) {
          hasRenameAt = true;
          final Class<?>[] paramTypes = method.getParameterTypes();
          assertEquals(String.class, paramTypes[0], "First param should be String (oldPath)");
          assertTrue(
              WasiDescriptor.class.isAssignableFrom(paramTypes[1]),
              "Second param should be WasiDescriptor");
          assertEquals(String.class, paramTypes[2], "Third param should be String (newPath)");
          LOGGER.info("Found renameAt with params: " + Arrays.toString(paramTypes));
          break;
        }
      }
      assertTrue(hasRenameAt, "WasiDescriptor should have renameAt method");
    }

    @Test
    @DisplayName("symlinkAt should accept two String paths")
    void symlinkAtShouldAcceptTwoPaths() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasSymlinkAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("symlinkAt") && method.getParameterCount() == 2) {
          final Class<?>[] paramTypes = method.getParameterTypes();
          if (paramTypes[0] == String.class && paramTypes[1] == String.class) {
            hasSymlinkAt = true;
            assertEquals(
                void.class, method.getReturnType(), "symlinkAt(String, String) should return void");
            LOGGER.info("Found symlinkAt(String, String) method");
            break;
          }
        }
      }
      assertTrue(hasSymlinkAt, "WasiDescriptor should have symlinkAt(String, String)");
    }

    @Test
    @DisplayName("linkAt should have correct signature with path flags")
    void linkAtShouldHaveCorrectSignature() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasLinkAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("linkAt") && method.getParameterCount() == 4) {
          hasLinkAt = true;
          LOGGER.info("Found linkAt with params: " + Arrays.toString(method.getParameterTypes()));
          break;
        }
      }
      assertTrue(hasLinkAt, "WasiDescriptor should have linkAt method with 4 parameters");
    }

    @Test
    @DisplayName("readLinkAt should return String")
    void readLinkAtShouldReturnString() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasReadLinkAt = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("readLinkAt")
            && method.getParameterCount() == 1
            && method.getParameterTypes()[0] == String.class) {
          hasReadLinkAt = true;
          assertEquals(
              String.class, method.getReturnType(), "readLinkAt(String) should return String");
          LOGGER.info("Found readLinkAt(String) with return type: " + method.getReturnType());
          break;
        }
      }
      assertTrue(hasReadLinkAt, "WasiDescriptor should have readLinkAt(String)");
    }
  }

  @Nested
  @DisplayName("Descriptor Utility Operations Tests")
  class DescriptorUtilityOperationsTests {

    @Test
    @DisplayName("isSameObject should return boolean")
    void isSameObjectShouldReturnBoolean() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasIsSameObject = false;
      for (Method method : clazz.getMethods()) {
        if (method.getName().equals("isSameObject") && method.getParameterCount() == 1) {
          hasIsSameObject = true;
          assertEquals(boolean.class, method.getReturnType(), "isSameObject should return boolean");
          assertTrue(
              WasiDescriptor.class.isAssignableFrom(method.getParameterTypes()[0]),
              "isSameObject parameter should be WasiDescriptor");
          LOGGER.info(
              "Found isSameObject with param type: " + method.getParameterTypes()[0].getName());
          break;
        }
      }
      assertTrue(hasIsSameObject, "WasiDescriptor should have isSameObject(WasiDescriptor)");
    }
  }

  @Nested
  @DisplayName("Panama FFI Pattern Tests")
  class PanamaFfiPatternTests {

    @Test
    @DisplayName("Descriptor should have static MethodHandle fields initialized in static block")
    void descriptorShouldHaveStaticMethodHandles() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      int staticFinalMethodHandleCount = 0;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == MethodHandle.class
            && Modifier.isStatic(field.getModifiers())
            && Modifier.isFinal(field.getModifiers())) {
          staticFinalMethodHandleCount++;
        }
      }
      assertTrue(
          staticFinalMethodHandleCount >= 15,
          "PanamaWasiDescriptor should have at least 15 static final MethodHandle fields, found: "
              + staticFinalMethodHandleCount);
      LOGGER.info("Total static final MethodHandle fields: " + staticFinalMethodHandleCount);
    }

    @Test
    @DisplayName("Descriptor should have Logger field")
    void descriptorShouldHaveLoggerField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasLogger = false;
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == Logger.class) {
          hasLogger = true;
          assertTrue(Modifier.isPrivate(field.getModifiers()), "Logger should be private");
          assertTrue(Modifier.isStatic(field.getModifiers()), "Logger should be static");
          assertTrue(Modifier.isFinal(field.getModifiers()), "Logger should be final");
          LOGGER.info("Found Logger field: " + field.getName());
          break;
        }
      }
      assertTrue(hasLogger, "PanamaWasiDescriptor should have a Logger field");
    }

    @Test
    @DisplayName("Descriptor should have helper methods for encoding/decoding flags")
    void descriptorShouldHaveFlagEncodingMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      final Set<String> expectedHelpers = new HashSet<>();
      expectedHelpers.add("encodePathFlags");
      expectedHelpers.add("encodeOpenFlags");
      expectedHelpers.add("encodeDescriptorFlags");
      expectedHelpers.add("decodeDescriptorFlags");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getDeclaredMethods()) {
        if (expectedHelpers.contains(method.getName())) {
          foundMethods.add(method.getName());
          assertTrue(
              Modifier.isPrivate(method.getModifiers()) || Modifier.isStatic(method.getModifiers()),
              method.getName() + " should be private or static");
          LOGGER.info("Found helper method: " + method.getName());
        }
      }

      for (String helper : expectedHelpers) {
        assertTrue(
            foundMethods.contains(helper),
            "PanamaWasiDescriptor should have " + helper + " helper method");
      }
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Descriptor should implement AutoCloseable")
    void descriptorShouldImplementAutoCloseable() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      assertTrue(
          AutoCloseable.class.isAssignableFrom(clazz),
          "PanamaWasiDescriptor should implement AutoCloseable");
      LOGGER.info(
          "PanamaWasiDescriptor implements AutoCloseable: "
              + AutoCloseable.class.isAssignableFrom(clazz));
    }

    @Test
    @DisplayName("Descriptor should have WasiResource methods")
    void descriptorShouldHaveWasiResourceMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      final Set<String> wasiResourceMethods = new HashSet<>();
      wasiResourceMethods.add("getId");
      wasiResourceMethods.add("getType");
      wasiResourceMethods.add("isValid");
      wasiResourceMethods.add("getAvailableOperations");

      final Set<String> foundMethods = new HashSet<>();
      for (Method method : clazz.getMethods()) {
        foundMethods.add(method.getName());
      }

      for (String methodName : wasiResourceMethods) {
        assertTrue(
            foundMethods.contains(methodName),
            "PanamaWasiDescriptor should have WasiResource method: " + methodName);
      }
      LOGGER.info("PanamaWasiDescriptor has all WasiResource methods");
    }

    @Test
    @DisplayName("Descriptor should have doClose and getResourceType protected methods")
    void descriptorShouldHaveProtectedMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(DESCRIPTOR_CLASS);
      boolean hasDoClose = false;
      boolean hasGetResourceType = false;
      for (Method method : clazz.getDeclaredMethods()) {
        if (method.getName().equals("doClose")) {
          hasDoClose = true;
          assertTrue(Modifier.isProtected(method.getModifiers()), "doClose should be protected");
          LOGGER.info("Found doClose method");
        }
        if (method.getName().equals("getResourceType")
            && method.getParameterCount() == 0
            && method.getReturnType() == String.class) {
          hasGetResourceType = true;
          assertTrue(
              Modifier.isProtected(method.getModifiers()), "getResourceType should be protected");
          LOGGER.info("Found getResourceType method");
        }
      }
      assertTrue(hasDoClose, "PanamaWasiDescriptor should have doClose method");
      assertTrue(hasGetResourceType, "PanamaWasiDescriptor should have getResourceType method");
    }
  }
}
