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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiPreview1Operations} class.
 *
 * <p>WasiPreview1Operations provides synchronous WASI Preview 1 system interface operations. These
 * tests verify class structure, method signatures, and API contracts without native library
 * loading.
 */
@DisplayName("WasiPreview1Operations Tests")
class WasiPreview1OperationsTest {

  private static final Logger LOGGER = Logger.getLogger(WasiPreview1OperationsTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.wasi.WasiPreview1Operations",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview1Operations class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "WasiPreview1Operations should be final");
      LOGGER.info("WasiPreview1Operations is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview1Operations visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          Modifier.isPublic(clazz.getModifiers()), "WasiPreview1Operations should be public");
      LOGGER.info("WasiPreview1Operations is correctly marked as public");
    }

    @Test
    @DisplayName("should have constructor with WasiContext parameter")
    void shouldHaveConstructorWithWasiContextParameter() throws ClassNotFoundException {
      LOGGER.info("Testing WasiPreview1Operations constructor");
      final Class<?> clazz = loadClassWithoutInit();
      final Class<?> wasiContextClass =
          Class.forName(
              "ai.tegmentum.wasmtime4j.jni.wasi.WasiContext", false, getClass().getClassLoader());

      boolean hasRequiredConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0] == wasiContextClass) {
          hasRequiredConstructor = true;
          break;
        }
      }

      assertTrue(hasRequiredConstructor, "Should have constructor with WasiContext parameter");
      LOGGER.info("WasiPreview1Operations has required constructor");
    }
  }

  @Nested
  @DisplayName("WASI Error Code Constants Tests")
  class WasiErrorCodeConstantsTests {

    @Test
    @DisplayName("should have WASI_ESUCCESS constant with value 0")
    void shouldHaveEsuccessConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing WASI_ESUCCESS constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getField("WASI_ESUCCESS");

      assertNotNull(field, "WASI_ESUCCESS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "WASI_ESUCCESS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "WASI_ESUCCESS should be final");
      assertTrue(Modifier.isPublic(field.getModifiers()), "WASI_ESUCCESS should be public");
      assertEquals(int.class, field.getType(), "WASI_ESUCCESS should be int type");
      LOGGER.info("WASI_ESUCCESS constant verified");
    }

    @Test
    @DisplayName("should have WASI_EINVAL constant with value 28")
    void shouldHaveEinvalConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing WASI_EINVAL constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getField("WASI_EINVAL");

      assertNotNull(field, "WASI_EINVAL field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "WASI_EINVAL should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "WASI_EINVAL should be final");
      assertEquals(int.class, field.getType(), "WASI_EINVAL should be int type");
      LOGGER.info("WASI_EINVAL constant verified");
    }

    @Test
    @DisplayName("should have WASI_EBADF constant with value 9")
    void shouldHaveEbadfConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing WASI_EBADF constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getField("WASI_EBADF");

      assertNotNull(field, "WASI_EBADF field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "WASI_EBADF should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "WASI_EBADF should be final");
      assertEquals(int.class, field.getType(), "WASI_EBADF should be int type");
      LOGGER.info("WASI_EBADF constant verified");
    }

    @Test
    @DisplayName("should have WASI_ENOENT constant with value 44")
    void shouldHaveEnoentConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing WASI_ENOENT constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getField("WASI_ENOENT");

      assertNotNull(field, "WASI_ENOENT field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "WASI_ENOENT should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "WASI_ENOENT should be final");
      assertEquals(int.class, field.getType(), "WASI_ENOENT should be int type");
      LOGGER.info("WASI_ENOENT constant verified");
    }

    @Test
    @DisplayName("should have WASI_EACCES constant with value 2")
    void shouldHaveEaccesConstant() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing WASI_EACCES constant");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getField("WASI_EACCES");

      assertNotNull(field, "WASI_EACCES field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "WASI_EACCES should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "WASI_EACCES should be final");
      assertEquals(int.class, field.getType(), "WASI_EACCES should be int type");
      LOGGER.info("WASI_EACCES constant verified");
    }
  }

  @Nested
  @DisplayName("File Descriptor Method Tests")
  class FileDescriptorMethodTests {

    @Test
    @DisplayName("should have fdRead method returning int")
    void shouldHaveFdReadMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing fdRead method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("fdRead", int.class, List.class);

      assertNotNull(method, "fdRead method should exist");
      assertEquals(int.class, method.getReturnType(), "fdRead should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fdRead should be public");
      LOGGER.info("fdRead method signature verified: " + method);
    }

    @Test
    @DisplayName("should have fdWrite method returning int")
    void shouldHaveFdWriteMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing fdWrite method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("fdWrite", int.class, List.class);

      assertNotNull(method, "fdWrite method should exist");
      assertEquals(int.class, method.getReturnType(), "fdWrite should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fdWrite should be public");
      LOGGER.info("fdWrite method signature verified: " + method);
    }

    @Test
    @DisplayName("should have fdSeek method returning long")
    void shouldHaveFdSeekMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing fdSeek method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("fdSeek", int.class, long.class, int.class);

      assertNotNull(method, "fdSeek method should exist");
      assertEquals(long.class, method.getReturnType(), "fdSeek should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fdSeek should be public");
      LOGGER.info("fdSeek method signature verified: " + method);
    }

    @Test
    @DisplayName("should have fdClose method returning void")
    void shouldHaveFdCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing fdClose method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("fdClose", int.class);

      assertNotNull(method, "fdClose method should exist");
      assertEquals(void.class, method.getReturnType(), "fdClose should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "fdClose should be public");
      LOGGER.info("fdClose method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Path Operation Method Tests")
  class PathOperationMethodTests {

    @Test
    @DisplayName("should have pathOpen method returning int")
    void shouldHavePathOpenMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing pathOpen method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method =
          clazz.getMethod(
              "pathOpen",
              int.class,
              int.class,
              String.class,
              int.class,
              long.class,
              long.class,
              int.class);

      assertNotNull(method, "pathOpen method should exist");
      assertEquals(int.class, method.getReturnType(), "pathOpen should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "pathOpen should be public");
      assertEquals(7, method.getParameterCount(), "pathOpen should have 7 parameters");
      LOGGER.info("pathOpen method signature verified: " + method);
    }

    @Test
    @DisplayName("should have pathCreateDirectory method returning void")
    void shouldHavePathCreateDirectoryMethod()
        throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing pathCreateDirectory method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("pathCreateDirectory", int.class, String.class);

      assertNotNull(method, "pathCreateDirectory method should exist");
      assertEquals(void.class, method.getReturnType(), "pathCreateDirectory should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "pathCreateDirectory should be public");
      LOGGER.info("pathCreateDirectory method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Environment Method Tests")
  class EnvironmentMethodTests {

    @Test
    @DisplayName("should have environGet method returning int")
    void shouldHaveEnvironGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing environGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("environGet", ByteBuffer.class, ByteBuffer.class);

      assertNotNull(method, "environGet method should exist");
      assertEquals(int.class, method.getReturnType(), "environGet should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "environGet should be public");
      LOGGER.info("environGet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have environSizesGet method returning int array")
    void shouldHaveEnvironSizesGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing environSizesGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("environSizesGet");

      assertNotNull(method, "environSizesGet method should exist");
      assertEquals(int[].class, method.getReturnType(), "environSizesGet should return int[]");
      assertTrue(Modifier.isPublic(method.getModifiers()), "environSizesGet should be public");
      LOGGER.info("environSizesGet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have argsGet method returning int")
    void shouldHaveArgsGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing argsGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("argsGet", ByteBuffer.class, ByteBuffer.class);

      assertNotNull(method, "argsGet method should exist");
      assertEquals(int.class, method.getReturnType(), "argsGet should return int");
      assertTrue(Modifier.isPublic(method.getModifiers()), "argsGet should be public");
      LOGGER.info("argsGet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have argsSizesGet method returning int array")
    void shouldHaveArgsSizesGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing argsSizesGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("argsSizesGet");

      assertNotNull(method, "argsSizesGet method should exist");
      assertEquals(int[].class, method.getReturnType(), "argsSizesGet should return int[]");
      assertTrue(Modifier.isPublic(method.getModifiers()), "argsSizesGet should be public");
      LOGGER.info("argsSizesGet method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Time Operation Method Tests")
  class TimeOperationMethodTests {

    @Test
    @DisplayName("should have clockTimeGet method returning long")
    void shouldHaveClockTimeGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing clockTimeGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("clockTimeGet", int.class, long.class);

      assertNotNull(method, "clockTimeGet method should exist");
      assertEquals(long.class, method.getReturnType(), "clockTimeGet should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "clockTimeGet should be public");
      LOGGER.info("clockTimeGet method signature verified: " + method);
    }

    @Test
    @DisplayName("should have clockResGet method returning long")
    void shouldHaveClockResGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing clockResGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("clockResGet", int.class);

      assertNotNull(method, "clockResGet method should exist");
      assertEquals(long.class, method.getReturnType(), "clockResGet should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "clockResGet should be public");
      LOGGER.info("clockResGet method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Random Operation Method Tests")
  class RandomOperationMethodTests {

    @Test
    @DisplayName("should have randomGet method returning void")
    void shouldHaveRandomGetMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing randomGet method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("randomGet", ByteBuffer.class);

      assertNotNull(method, "randomGet method should exist");
      assertEquals(void.class, method.getReturnType(), "randomGet should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "randomGet should be public");
      LOGGER.info("randomGet method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Process Operation Method Tests")
  class ProcessOperationMethodTests {

    @Test
    @DisplayName("should have procExit method returning void")
    void shouldHaveProcExitMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing procExit method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("procExit", int.class);

      assertNotNull(method, "procExit method should exist");
      assertEquals(void.class, method.getReturnType(), "procExit should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "procExit should be public");
      LOGGER.info("procExit method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Private Field Tests")
  class PrivateFieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing LOGGER field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("LOGGER");

      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      LOGGER.info("LOGGER field verified");
    }

    @Test
    @DisplayName("should have wasiContext field")
    void shouldHaveWasiContextField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing wasiContext field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("wasiContext");

      assertNotNull(field, "wasiContext field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "wasiContext should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "wasiContext should be final");
      LOGGER.info("wasiContext field verified");
    }

    @Test
    @DisplayName("should have fileSystem field")
    void shouldHaveFileSystemField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing fileSystem field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("fileSystem");

      assertNotNull(field, "fileSystem field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "fileSystem should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "fileSystem should be final");
      LOGGER.info("fileSystem field verified");
    }

    @Test
    @DisplayName("should have timeOperations field")
    void shouldHaveTimeOperationsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing timeOperations field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("timeOperations");

      assertNotNull(field, "timeOperations field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "timeOperations should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "timeOperations should be final");
      LOGGER.info("timeOperations field verified");
    }

    @Test
    @DisplayName("should have randomOperations field")
    void shouldHaveRandomOperationsField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing randomOperations field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("randomOperations");

      assertNotNull(field, "randomOperations field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "randomOperations should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "randomOperations should be final");
      LOGGER.info("randomOperations field verified");
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountVerificationTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() throws ClassNotFoundException {
      LOGGER.info("Testing public method count");
      final Class<?> clazz = loadClassWithoutInit();

      final Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "fdRead",
                  "fdWrite",
                  "fdSeek",
                  "fdClose",
                  "pathOpen",
                  "pathCreateDirectory",
                  "environGet",
                  "environSizesGet",
                  "argsGet",
                  "argsSizesGet",
                  "clockTimeGet",
                  "clockResGet",
                  "randomGet",
                  "procExit"));

      int foundMethodCount = 0;
      for (final Method method : clazz.getMethods()) {
        if (method.getDeclaringClass() == clazz && Modifier.isPublic(method.getModifiers())) {
          foundMethodCount++;
          LOGGER.info("Found public method: " + method.getName());
        }
      }

      assertTrue(
          foundMethodCount >= expectedMethods.size(),
          "Should have at least "
              + expectedMethods.size()
              + " public methods, found: "
              + foundMethodCount);
      LOGGER.info("Public method count verified: " + foundMethodCount);
    }
  }
}
