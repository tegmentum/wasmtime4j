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
package ai.tegmentum.wasmtime4j.panama.wasi.cli;

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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Direct integration tests for Panama WASI CLI implementation.
 */
@DisplayName("Panama WASI CLI Direct Tests")
public class PanamaWasiCliDirectTest {

    private static final Logger LOGGER =
            Logger.getLogger(PanamaWasiCliDirectTest.class.getName());
    private final List<AutoCloseable> resources = new ArrayList<>();

    @BeforeAll
    static void loadNativeLibrary() {
        LOGGER.info("Loading native library for WASI CLI tests");
        final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
        assertTrue(loader.isLoaded(), "Native library should be loaded");
    }

    @AfterEach
    void tearDown() {
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (final Exception e) {
                LOGGER.warning("Error closing resource: " + e.getMessage());
            }
        }
        resources.clear();
    }

    @Nested
    @DisplayName("PanamaWasiStdio Tests")
    class StdioTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiStdio class structure");

            final Class<?> clazz = PanamaWasiStdio.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have standard stream methods")
        void shouldHaveStandardStreamMethods() {
            LOGGER.info("Testing standard stream methods");

            final Class<?> clazz = PanamaWasiStdio.class;
            final String[] expectedMethods = {
                "getStdin",
                "getStdout",
                "getStderr"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found stream method: " + methodName);
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }

        @Test
        @DisplayName("Stream methods should return correct types")
        void streamMethodsShouldReturnCorrectTypes() {
            LOGGER.info("Testing stream method return types");

            final Class<?> clazz = PanamaWasiStdio.class;

            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("getStdin")) {
                    assertTrue(method.getReturnType().getSimpleName().contains("InputStream")
                            || method.getReturnType().getSimpleName().contains("Wasi"),
                            "getStdin should return an input stream type");
                }
                if (method.getName().equals("getStdout")
                        || method.getName().equals("getStderr")) {
                    assertTrue(method.getReturnType().getSimpleName().contains("OutputStream")
                            || method.getReturnType().getSimpleName().contains("Wasi"),
                            method.getName() + " should return an output stream type");
                }
            }
        }
    }

    @Nested
    @DisplayName("PanamaWasiEnvironment Tests")
    class EnvironmentTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiEnvironment class structure");

            final Class<?> clazz = PanamaWasiEnvironment.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have environment methods")
        void shouldHaveEnvironmentMethods() {
            LOGGER.info("Testing environment methods");

            final Class<?> clazz = PanamaWasiEnvironment.class;
            final String[] expectedMethods = {
                "getEnvironmentVariables",
                "getVariable",
                "getArguments",
                "getInitialCwd"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found environment method: " + methodName);
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }

        @Test
        @DisplayName("getVariable should take String parameter")
        void getVariableShouldTakeStringParameter() {
            LOGGER.info("Testing getVariable parameter type");

            final Class<?> clazz = PanamaWasiEnvironment.class;

            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("getVariable")
                        && method.getParameterCount() == 1) {
                    assertEquals(String.class, method.getParameterTypes()[0],
                            "getVariable should take String parameter");
                }
            }
        }
    }

    @Nested
    @DisplayName("PanamaWasiExit Tests")
    class ExitTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiExit class structure");

            final Class<?> clazz = PanamaWasiExit.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have exit method")
        void shouldHaveExitMethod() {
            LOGGER.info("Testing exit method");

            final Class<?> clazz = PanamaWasiExit.class;

            boolean found = false;
            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("exit")) {
                    found = true;
                    LOGGER.info("Found exit method with "
                            + method.getParameterCount() + " parameters");
                    break;
                }
            }
            assertTrue(found, "Should have exit method");
        }

        @Test
        @DisplayName("Exit method should take int status code")
        void exitMethodShouldTakeIntStatusCode() {
            LOGGER.info("Testing exit method parameter");

            final Class<?> clazz = PanamaWasiExit.class;

            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("exit")
                        && method.getParameterCount() == 1) {
                    assertEquals(int.class, method.getParameterTypes()[0],
                            "exit should take int status code");
                }
            }
        }
    }
}
