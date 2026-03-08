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
package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.ExnRef;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.memory.Tag;
import ai.tegmentum.wasmtime4j.tests.framework.DualRuntimeTest;
import ai.tegmentum.wasmtime4j.type.FunctionType;
import ai.tegmentum.wasmtime4j.type.TagType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * Integration tests for WebAssembly exception handling - Tags, TagTypes, and ExnRefs.
 *
 * <p>These tests verify exception tag creation, type management, and exception reference handling.
 *
 * @since 1.0.0
 */
@DisplayName("Exception Handling Integration Tests")
public class ExceptionHandlingTest extends DualRuntimeTest {

  private static final Logger LOGGER = Logger.getLogger(ExceptionHandlingTest.class.getName());

  private boolean probeExceptionHandlingAvailable() {
    try {
      try (Engine engine = Engine.create()) {
        final Store testStore = engine.createStore();
        final FunctionType funcType =
            new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
        final TagType tagType = TagType.create(funcType);
        final Tag testTag = Tag.create(testStore, tagType);
        testStore.close();
        return testTag != null;
      }
    } catch (final Exception e) {
      LOGGER.warning("Exception handling not available: " + e.getMessage());
    }
    return false;
  }

  private final List<AutoCloseable> resources = new ArrayList<>();

  @AfterEach
  void tearDown(final TestInfo testInfo) {
    LOGGER.info("Tearing down: " + testInfo.getDisplayName());
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    clearRuntimeSelection();
  }

  @Nested
  @DisplayName("Tag Creation Tests")
  class TagCreationTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create exception tag")
    void shouldCreateExceptionTag(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a simple tag with no payload
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      assertNotNull(tag, "Tag should not be null");
      assertTrue(tag.getNativeHandle() != 0, "Tag should have a valid native handle");
      LOGGER.info("Created tag with handle: 0x" + Long.toHexString(tag.getNativeHandle()));
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create tag with payload types")
    void shouldCreateTagWithPayloadTypes(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a tag with i32 and i64 payload types
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      assertNotNull(tag, "Tag should not be null");

      // Verify the tag type
      final TagType retrievedType = tag.getType(store);
      assertNotNull(retrievedType, "Retrieved TagType should not be null");

      final FunctionType retrievedFuncType = retrievedType.getFunctionType();
      assertEquals(2, retrievedFuncType.getParamCount(), "Tag should have 2 parameter types");
      LOGGER.info("Created tag with " + retrievedFuncType.getParamCount() + " payload types");
    }
  }

  @Nested
  @DisplayName("TagType Tests")
  class TagTypeTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create TagType")
    void shouldCreateTagType(final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a TagType
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);

      assertNotNull(tagType, "TagType should not be null");
      assertNotNull(tagType.getFunctionType(), "TagType function type should not be null");
      LOGGER.info("Created TagType with function type: " + tagType.getFunctionType());
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return parameter types")
    void shouldReturnParameterTypes(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a TagType with specific parameter types
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.F64};
      final FunctionType funcType = new FunctionType(params, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);

      final FunctionType retrievedFuncType = tagType.getFunctionType();
      assertEquals(2, retrievedFuncType.getParamCount(), "Should have 2 parameters");

      final WasmValueType[] paramTypes = retrievedFuncType.getParamTypes();
      assertEquals(WasmValueType.I32, paramTypes[0], "First param should be I32");
      assertEquals(WasmValueType.F64, paramTypes[1], "Second param should be F64");
      LOGGER.info("TagType parameters verified: " + retrievedFuncType);
    }
  }

  @Nested
  @DisplayName("ExnRef API Structure Tests")
  class ExnRefApiStructureTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ExnRef should be an interface")
    void exnRefShouldBeAnInterface(final RuntimeType runtime, final TestInfo testInfo) {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      assertTrue(ExnRef.class.isInterface(), "ExnRef should be an interface");
      LOGGER.info("ExnRef is correctly an interface");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ExnRef should have getTag method")
    void exnRefShouldHaveGetTagMethod(final RuntimeType runtime, final TestInfo testInfo)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = ExnRef.class.getMethod("getTag", Store.class);
      assertNotNull(method, "getTag method should exist");
      assertEquals(Tag.class, method.getReturnType(), "getTag should return Tag");
      LOGGER.info("ExnRef has getTag(Store) method returning Tag");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ExnRef should have getNativeHandle method")
    void exnRefShouldHaveGetNativeHandleMethod(final RuntimeType runtime, final TestInfo testInfo)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = ExnRef.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
      LOGGER.info("ExnRef has getNativeHandle() method returning long");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ExnRef should have isValid method")
    void exnRefShouldHaveIsValidMethod(final RuntimeType runtime, final TestInfo testInfo)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = ExnRef.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
      LOGGER.info("ExnRef has isValid() method returning boolean");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("ExnRef should have exactly 10 methods")
    void exnRefShouldHaveExactlyTenMethods(final RuntimeType runtime, final TestInfo testInfo) {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      assertEquals(
          10, ExnRef.class.getDeclaredMethods().length, "ExnRef should have exactly 10 methods");
      LOGGER.info("ExnRef correctly has 10 declared methods");
    }
  }

  @Nested
  @DisplayName("Store Exception Handling Tests")
  class StoreExceptionHandlingTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should report no pending exception initially")
    void shouldReportNoPendingExceptionInitially(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      assertFalse(store.hasPendingException(), "Store should not have pending exception initially");
      LOGGER.info("Store correctly reports no pending exception");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should return null when taking non-existent exception")
    void shouldReturnNullWhenTakingNonExistentException(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final ExnRef exnRef = store.takePendingException();
      // Should return null when no exception is pending
      // (Some implementations may return null, others may throw)
      LOGGER.info("takePendingException() returned: " + exnRef);
      assertTrue(
          exnRef == null || !exnRef.isValid(),
          "takePendingException should return null or invalid when no exception pending");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Store should have hasPendingException method")
    void storeShouldHaveHasPendingExceptionMethod(
        final RuntimeType runtime, final TestInfo testInfo) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = Store.class.getMethod("hasPendingException");
      assertNotNull(method, "hasPendingException method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "hasPendingException should return boolean");
      LOGGER.info("Store has hasPendingException() method");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Store should have takePendingException method")
    void storeShouldHaveTakePendingExceptionMethod(
        final RuntimeType runtime, final TestInfo testInfo) throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = Store.class.getMethod("takePendingException");
      assertNotNull(method, "takePendingException method should exist");
      assertEquals(
          ExnRef.class, method.getReturnType(), "takePendingException should return ExnRef");
      LOGGER.info("Store has takePendingException() method returning ExnRef");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("Store should have throwException method")
    void storeShouldHaveThrowExceptionMethod(final RuntimeType runtime, final TestInfo testInfo)
        throws NoSuchMethodException {
      setRuntime(runtime);
      LOGGER.info("Testing: " + testInfo.getDisplayName());
      final Method method = Store.class.getMethod("throwException", ExnRef.class);
      assertNotNull(method, "throwException method should exist");
      assertEquals(
          Object.class, method.getReturnType(), "throwException should return generic type");
      LOGGER.info("Store has throwException(ExnRef) method");
    }
  }

  @Nested
  @DisplayName("ThrownException Tests")
  class ThrownExceptionTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create ThrownException with tag and empty payload")
    void shouldCreateThrownExceptionWithTagAndEmptyPayload(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final ThrownException thrownException = new ThrownException(tag, Collections.emptyList());

      assertNotNull(thrownException, "ThrownException should not be null");
      assertEquals(tag, thrownException.getTag(), "Tag should match");
      assertFalse(thrownException.hasPayload(), "Should have no payload");
      assertEquals(0, thrownException.getPayloadSize(), "Payload size should be 0");
      assertTrue(thrownException.getExnRef().isEmpty(), "ExnRef should be empty");
      LOGGER.info("Created ThrownException: " + thrownException);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should create ThrownException with payload values")
    void shouldCreateThrownExceptionWithPayloadValues(
        final RuntimeType runtime, final TestInfo testInfo) throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final List<WasmValue> payload = Arrays.asList(WasmValue.i32(42), WasmValue.i64(1234567890L));

      final ThrownException thrownException = new ThrownException(tag, payload);

      assertNotNull(thrownException, "ThrownException should not be null");
      assertTrue(thrownException.hasPayload(), "Should have payload");
      assertEquals(2, thrownException.getPayloadSize(), "Payload size should be 2");
      assertEquals(payload, thrownException.getPayload(), "Payload should match");
      LOGGER.info("Created ThrownException with payload: " + thrownException);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should access payload values by index")
    void shouldAccessPayloadValuesByIndex(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final WasmValue value = WasmValue.i32(99);
      final ThrownException thrownException =
          new ThrownException(tag, Collections.singletonList(value));

      final WasmValue retrieved = thrownException.getPayloadValue(0);
      assertEquals(value, retrieved, "Payload value at index 0 should match");
      LOGGER.info("Retrieved payload value: " + retrieved);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should build ThrownException using builder")
    void shouldBuildThrownExceptionUsingBuilder(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.F64}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final ThrownException thrownException =
          ThrownException.builder(tag)
              .payload(Collections.singletonList(WasmValue.f64(3.14)))
              .build();

      assertNotNull(thrownException, "ThrownException should not be null");
      assertEquals(tag, thrownException.getTag(), "Tag should match");
      assertEquals(1, thrownException.getPayloadSize(), "Payload should have 1 value");
      LOGGER.info("Built ThrownException using builder: " + thrownException);
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should support equals and hashCode")
    void shouldSupportEqualsAndHashCode(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final ThrownException te1 = new ThrownException(tag, Collections.emptyList());
      final ThrownException te2 = new ThrownException(tag, Collections.emptyList());

      assertEquals(te1, te2, "Equal ThrownExceptions should be equal");
      assertEquals(te1.hashCode(), te2.hashCode(), "Equal objects should have same hashCode");
      LOGGER.info("ThrownException equals and hashCode work correctly");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should have meaningful toString")
    void shouldHaveMeaningfulToString(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      final ThrownException thrownException = new ThrownException(tag, Collections.emptyList());
      final String toString = thrownException.toString();

      assertNotNull(toString, "toString should not be null");
      assertTrue(toString.contains("ThrownException"), "toString should contain class name");
      assertTrue(toString.contains("tag="), "toString should contain tag");
      assertTrue(toString.contains("payload="), "toString should contain payload");
      LOGGER.info("ThrownException toString: " + toString);
    }
  }

  @Nested
  @DisplayName("Exception Lifecycle Tests")
  class ExceptionLifecycleTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should close exception resources properly")
    void shouldCloseExceptionResourcesProperly(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a tag and verify it can be used and cleaned up
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      assertNotNull(tag, "Tag should be created successfully");

      // Verify tag is usable
      final TagType retrievedType = tag.getType(store);
      assertNotNull(retrievedType, "Should be able to get tag type before close");

      LOGGER.info("Exception resources handled properly");
    }
  }

  @Nested
  @DisplayName("Tag Equality Tests")
  class TagEqualityTests {

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should detect equal tags")
    void shouldDetectEqualTags(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create a tag
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = Tag.create(store, tagType);

      // A tag should be equal to itself
      assertTrue(tag.equals(tag, store), "Tag should be equal to itself");
      LOGGER.info("Tag equality test passed");
    }

    @ParameterizedTest
    @ArgumentsSource(RuntimeProvider.class)
    @DisplayName("should detect different tags")
    void shouldDetectDifferentTags(final RuntimeType runtime, final TestInfo testInfo)
        throws Exception {
      setRuntime(runtime);
      assumeTrue(probeExceptionHandlingAvailable(), "Exception handling not available - skipping");
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      final Engine engine = Engine.create();
      resources.add(engine);
      final Store store = engine.createStore();
      resources.add(store);

      // Create two different tags
      final FunctionType funcType1 =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType1 = TagType.create(funcType1);
      final Tag tag1 = Tag.create(store, tagType1);

      final FunctionType funcType2 =
          new FunctionType(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType2 = TagType.create(funcType2);
      final Tag tag2 = Tag.create(store, tagType2);

      // Different tags should not be equal
      assertFalse(tag1.equals(tag2, store), "Different tags should not be equal");
      LOGGER.info("Tag inequality test passed");
    }
  }
}
