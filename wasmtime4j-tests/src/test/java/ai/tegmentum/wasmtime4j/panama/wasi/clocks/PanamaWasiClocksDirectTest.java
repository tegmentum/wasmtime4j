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
package ai.tegmentum.wasmtime4j.panama.wasi.clocks;

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
 * Direct integration tests for Panama WASI Clocks implementation.
 */
@DisplayName("Panama WASI Clocks Direct Tests")
public class PanamaWasiClocksDirectTest {

    private static final Logger LOGGER =
            Logger.getLogger(PanamaWasiClocksDirectTest.class.getName());
    private final List<AutoCloseable> resources = new ArrayList<>();

    @BeforeAll
    static void loadNativeLibrary() {
        LOGGER.info("Loading native library for WASI clocks tests");
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
    @DisplayName("PanamaWasiWallClock Tests")
    class WallClockTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiWallClock class structure");

            final Class<?> clazz = PanamaWasiWallClock.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have wall clock methods")
        void shouldHaveWallClockMethods() {
            LOGGER.info("Testing wall clock methods");

            final Class<?> clazz = PanamaWasiWallClock.class;
            final String[] expectedMethods = {
                "now",
                "resolution"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found wall clock method: " + methodName
                                + " returns " + method.getReturnType().getSimpleName());
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }
    }

    @Nested
    @DisplayName("PanamaWasiMonotonicClock Tests")
    class MonotonicClockTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiMonotonicClock class structure");

            final Class<?> clazz = PanamaWasiMonotonicClock.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have monotonic clock methods")
        void shouldHaveMonotonicClockMethods() {
            LOGGER.info("Testing monotonic clock methods");

            final Class<?> clazz = PanamaWasiMonotonicClock.class;
            final String[] expectedMethods = {
                "now",
                "resolution"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found monotonic clock method: " + methodName
                                + " returns " + method.getReturnType().getSimpleName());
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }

        @Test
        @DisplayName("Should have subscription methods")
        void shouldHaveSubscriptionMethods() {
            LOGGER.info("Testing subscription methods");

            final Class<?> clazz = PanamaWasiMonotonicClock.class;
            final String[] expectedMethods = {
                "subscribeInstant",
                "subscribeDuration"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found subscription method: " + methodName);
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }

        @Test
        @DisplayName("Subscription methods should take long parameter")
        void subscriptionMethodsShouldTakeLongParameter() {
            LOGGER.info("Testing subscription method parameters");

            final Class<?> clazz = PanamaWasiMonotonicClock.class;

            for (final Method method : clazz.getMethods()) {
                if (method.getName().equals("subscribeInstant")
                        && method.getParameterCount() == 1) {
                    assertEquals(long.class, method.getParameterTypes()[0],
                            "subscribeInstant should take long parameter");
                }
                if (method.getName().equals("subscribeDuration")
                        && method.getParameterCount() == 1) {
                    assertEquals(long.class, method.getParameterTypes()[0],
                            "subscribeDuration should take long parameter");
                }
            }
        }
    }

    @Nested
    @DisplayName("PanamaWasiTimezone Tests")
    class TimezoneTests {

        @Test
        @DisplayName("Should have correct class structure")
        void shouldHaveCorrectClassStructure() {
            LOGGER.info("Testing PanamaWasiTimezone class structure");

            final Class<?> clazz = PanamaWasiTimezone.class;

            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()),
                    "Class should be public");
            assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
                    "Class should be final");

            LOGGER.info("Class structure verified");
        }

        @Test
        @DisplayName("Should have timezone methods")
        void shouldHaveTimezoneMethods() {
            LOGGER.info("Testing timezone methods");

            final Class<?> clazz = PanamaWasiTimezone.class;
            final String[] expectedMethods = {
                "display",
                "utcOffset"
            };

            for (final String methodName : expectedMethods) {
                boolean found = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals(methodName)) {
                        found = true;
                        LOGGER.info("Found timezone method: " + methodName);
                        break;
                    }
                }
                assertTrue(found, "Should have method: " + methodName);
            }
        }
    }

    @Nested
    @DisplayName("Cross-Clock Integration Tests")
    class CrossClockTests {

        @Test
        @DisplayName("All clocks should have now method")
        void allClocksShouldHaveNowMethod() {
            LOGGER.info("Testing now method across all clocks");

            final Class<?>[] clockClasses = {
                PanamaWasiWallClock.class,
                PanamaWasiMonotonicClock.class
            };

            for (final Class<?> clazz : clockClasses) {
                boolean hasNow = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals("now")
                            && method.getParameterCount() == 0) {
                        hasNow = true;
                        break;
                    }
                }
                assertTrue(hasNow, clazz.getSimpleName() + " should have now() method");
            }
        }

        @Test
        @DisplayName("All clocks should have resolution method")
        void allClocksShouldHaveResolutionMethod() {
            LOGGER.info("Testing resolution method across all clocks");

            final Class<?>[] clockClasses = {
                PanamaWasiWallClock.class,
                PanamaWasiMonotonicClock.class
            };

            for (final Class<?> clazz : clockClasses) {
                boolean hasResolution = false;
                for (final Method method : clazz.getMethods()) {
                    if (method.getName().equals("resolution")
                            && method.getParameterCount() == 0) {
                        hasResolution = true;
                        break;
                    }
                }
                assertTrue(hasResolution, clazz.getSimpleName()
                        + " should have resolution() method");
            }
        }
    }
}
