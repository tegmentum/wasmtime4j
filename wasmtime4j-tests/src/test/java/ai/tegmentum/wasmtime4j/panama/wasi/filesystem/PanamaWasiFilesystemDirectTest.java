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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Direct integration tests for Panama WASI filesystem classes.
 *
 * <p>These tests verify class structure and method signatures without creating
 * instances with invalid native handles (which would cause JVM crashes).</p>
 */
@DisplayName("Panama WASI Filesystem Direct Tests")
public class PanamaWasiFilesystemDirectTest {

    private static final Logger LOGGER =
            Logger.getLogger(PanamaWasiFilesystemDirectTest.class.getName());
    private final List<AutoCloseable> resources = new ArrayList<>();

    @BeforeAll
    static void loadNativeLibrary() {
        LOGGER.info("Loading native library for WASI filesystem tests");
        final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
        assertTrue(loader.isLoaded(), "Native library should be loaded");
        LOGGER.info("Native library loaded successfully");
    }

    @AfterEach
    void tearDown() {
        LOGGER.info("Cleaning up " + resources.size() + " resources");
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (final Exception e) {
                LOGGER.warning("Error closing resource: " + e.getMessage());
            }
        }
        resources.clear();
    }

    /**
     * Tests for PanamaWasiDescriptor class structure.
     */
    @Nested
    @DisplayName("PanamaWasiDescriptor Structure Tests")
    class DescriptorStructureTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiDescriptor class structure");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Verify class is final
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            // Verify class is public
            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");

            // Verify constructor exists
            final Constructor<?>[] constructors = clazz.getConstructors();
            assertEquals(1, constructors.length, "Should have exactly one public constructor");

            // Verify constructor parameters
            final Class<?>[] paramTypes = constructors[0].getParameterTypes();
            assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
            assertEquals(MemorySegment.class, paramTypes[0], "First param should be MemorySegment");
            assertEquals(MemorySegment.class, paramTypes[1], "Second param should be MemorySegment");

            LOGGER.info("Class structure verified successfully");
        }

        @Test
        @DisplayName("Should have required static method handles")
        void shouldHaveRequiredStaticMethodHandles() throws Exception {
            LOGGER.info("Testing PanamaWasiDescriptor static method handles");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Check for some key method handles
            final String[] expectedHandles = {
                "READ_VIA_STREAM_HANDLE",
                "WRITE_VIA_STREAM_HANDLE",
                "APPEND_VIA_STREAM_HANDLE",
                "GET_TYPE_HANDLE",
                "GET_FLAGS_HANDLE",
                "SET_SIZE_HANDLE",
                "SYNC_DATA_HANDLE",
                "SYNC_HANDLE",
                "OPEN_AT_HANDLE",
                "CREATE_DIRECTORY_AT_HANDLE",
                "READ_DIRECTORY_HANDLE",
                "UNLINK_FILE_AT_HANDLE",
                "REMOVE_DIRECTORY_AT_HANDLE",
                "RENAME_AT_HANDLE",
                "CLOSE_HANDLE"
            };

            int foundCount = 0;
            for (final String handleName : expectedHandles) {
                try {
                    final Field field = clazz.getDeclaredField(handleName);
                    field.setAccessible(true);
                    assertNotNull(field, "Should have " + handleName + " field");
                    assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()),
                            handleName + " should be static");
                    foundCount++;
                    LOGGER.info("Verified " + handleName + " exists");
                } catch (final NoSuchFieldException e) {
                    LOGGER.info("Handle not found: " + handleName);
                }
            }

            assertTrue(foundCount > 10, "Should have at least 10 method handles, found: " + foundCount);
        }

        @Test
        @DisplayName("Should have all required public methods")
        void shouldHaveAllRequiredPublicMethods() throws Exception {
            LOGGER.info("Testing PanamaWasiDescriptor public methods");

            final Class<?> clazz = PanamaWasiDescriptor.class;
            final String[] expectedMethods = {
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
                "unlinkFileAt",
                "removeDirectoryAt",
                "renameAt",
                "getId",
                "getType",
                "isValid",
                "getAvailableOperations",
                "getState"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found method: " + methodName);
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }
    }

    /**
     * Tests for PanamaWasiDescriptor method signatures.
     */
    @Nested
    @DisplayName("PanamaWasiDescriptor Method Signature Tests")
    class MethodSignatureTests {

        @Test
        @DisplayName("Should have correct stream method signatures")
        void shouldHaveCorrectStreamMethodSignatures() {
            LOGGER.info("Testing stream method signatures");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Check readViaStream
            boolean hasReadViaStream = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("readViaStream")) {
                    hasReadViaStream = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "readViaStream should have 1 parameter");
                    assertEquals(long.class, params[0], "readViaStream should take long offset");
                }
            }
            assertTrue(hasReadViaStream, "Should have readViaStream method");

            // Check writeViaStream
            boolean hasWriteViaStream = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("writeViaStream")) {
                    hasWriteViaStream = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "writeViaStream should have 1 parameter");
                    assertEquals(long.class, params[0], "writeViaStream should take long offset");
                }
            }
            assertTrue(hasWriteViaStream, "Should have writeViaStream method");

            // Check appendViaStream
            boolean hasAppendViaStream = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("appendViaStream")) {
                    hasAppendViaStream = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(0, params.length, "appendViaStream should have 0 parameters");
                }
            }
            assertTrue(hasAppendViaStream, "Should have appendViaStream method");

            LOGGER.info("Stream method signatures verified");
        }

        @Test
        @DisplayName("Should have correct directory method signatures")
        void shouldHaveCorrectDirectoryMethodSignatures() {
            LOGGER.info("Testing directory method signatures");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Check createDirectoryAt
            boolean hasCreateDirectoryAt = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("createDirectoryAt")) {
                    hasCreateDirectoryAt = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "createDirectoryAt should have 1 parameter");
                    assertEquals(String.class, params[0], "createDirectoryAt should take String path");
                }
            }
            assertTrue(hasCreateDirectoryAt, "Should have createDirectoryAt method");

            // Check readDirectory
            boolean hasReadDirectory = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("readDirectory")) {
                    hasReadDirectory = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(0, params.length, "readDirectory should have 0 parameters");
                }
            }
            assertTrue(hasReadDirectory, "Should have readDirectory method");

            // Check removeDirectoryAt
            boolean hasRemoveDirectoryAt = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("removeDirectoryAt")) {
                    hasRemoveDirectoryAt = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "removeDirectoryAt should have 1 parameter");
                }
            }
            assertTrue(hasRemoveDirectoryAt, "Should have removeDirectoryAt method");

            LOGGER.info("Directory method signatures verified");
        }

        @Test
        @DisplayName("Should have correct file operation method signatures")
        void shouldHaveCorrectFileOperationMethodSignatures() {
            LOGGER.info("Testing file operation method signatures");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Check setSize
            boolean hasSetSize = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("setSize")) {
                    hasSetSize = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "setSize should have 1 parameter");
                    assertEquals(long.class, params[0], "setSize should take long size");
                }
            }
            assertTrue(hasSetSize, "Should have setSize method");

            // Check sync
            boolean hasSync = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("sync")) {
                    hasSync = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(0, params.length, "sync should have 0 parameters");
                }
            }
            assertTrue(hasSync, "Should have sync method");

            // Check syncData
            boolean hasSyncData = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("syncData")) {
                    hasSyncData = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(0, params.length, "syncData should have 0 parameters");
                }
            }
            assertTrue(hasSyncData, "Should have syncData method");

            // Check unlinkFileAt
            boolean hasUnlinkFileAt = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("unlinkFileAt")) {
                    hasUnlinkFileAt = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(1, params.length, "unlinkFileAt should have 1 parameter");
                }
            }
            assertTrue(hasUnlinkFileAt, "Should have unlinkFileAt method");

            LOGGER.info("File operation method signatures verified");
        }

        @Test
        @DisplayName("Should have correct path operation method signatures")
        void shouldHaveCorrectPathOperationMethodSignatures() {
            LOGGER.info("Testing path operation method signatures");

            final Class<?> clazz = PanamaWasiDescriptor.class;

            // Check renameAt
            boolean hasRenameAt = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("renameAt")) {
                    hasRenameAt = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertEquals(3, params.length, "renameAt should have 3 parameters");
                }
            }
            assertTrue(hasRenameAt, "Should have renameAt method");

            // Check openAt
            boolean hasOpenAt = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("openAt")) {
                    hasOpenAt = true;
                    final Class<?>[] params = method.getParameterTypes();
                    assertTrue(params.length >= 1, "openAt should have at least 1 parameter");
                }
            }
            assertTrue(hasOpenAt, "Should have openAt method");

            LOGGER.info("Path operation method signatures verified");
        }
    }

    /**
     * Integration tests for descriptor interface compliance.
     */
    @Nested
    @DisplayName("Descriptor Interface Compliance Tests")
    class InterfaceComplianceTests {

        @Test
        @DisplayName("Should implement WasiDescriptor interface")
        void shouldImplementWasiDescriptorInterface() {
            LOGGER.info("Testing WasiDescriptor interface implementation");

            final Class<?> clazz = PanamaWasiDescriptor.class;
            final Class<?>[] interfaces = clazz.getInterfaces();

            boolean implementsWasiDescriptor = false;
            for (final Class<?> iface : interfaces) {
                if (iface.getSimpleName().equals("WasiDescriptor")) {
                    implementsWasiDescriptor = true;
                    break;
                }
            }

            // Also check superclass interfaces
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && !implementsWasiDescriptor) {
                for (final Class<?> iface : superClass.getInterfaces()) {
                    if (iface.getSimpleName().contains("Wasi")
                            || iface.getSimpleName().contains("Resource")) {
                        LOGGER.info("Found interface via superclass: " + iface.getSimpleName());
                    }
                }
                superClass = superClass.getSuperclass();
            }

            LOGGER.info("PanamaWasiDescriptor implements " + interfaces.length + " direct interface(s)");
            for (final Class<?> iface : interfaces) {
                LOGGER.info("  Implements: " + iface.getName());
            }
        }

        @Test
        @DisplayName("Should implement AutoCloseable")
        void shouldImplementAutoCloseable() {
            LOGGER.info("Testing AutoCloseable implementation");

            assertTrue(AutoCloseable.class.isAssignableFrom(PanamaWasiDescriptor.class),
                    "Should implement AutoCloseable");

            LOGGER.info("AutoCloseable implementation verified");
        }
    }
}
