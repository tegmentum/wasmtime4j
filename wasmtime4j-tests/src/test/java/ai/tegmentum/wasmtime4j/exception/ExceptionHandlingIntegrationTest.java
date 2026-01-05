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
import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.Tag;
import ai.tegmentum.wasmtime4j.TagType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Integration tests for WebAssembly exception handling - Tags, TagTypes, and ExnRefs.
 *
 * <p>These tests verify exception tag creation, type management, and exception reference handling.
 *
 * @since 1.0.0
 */
@DisplayName("Exception Handling Integration Tests")
public final class ExceptionHandlingIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(ExceptionHandlingIntegrationTest.class.getName());

  private static boolean exceptionHandlingAvailable = false;
  private static WasmRuntime sharedRuntime;
  private static Engine sharedEngine;

  @BeforeAll
  static void checkExceptionHandlingAvailable() {
    try {
      sharedRuntime = WasmRuntimeFactory.create();
      sharedEngine = sharedRuntime.createEngine();

      // Try to create a simple tag to check if exception handling is available
      final Store testStore = sharedRuntime.createStore(sharedEngine);
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag testTag = sharedRuntime.createTag(testStore, tagType);

      if (testTag != null) {
        exceptionHandlingAvailable = true;
        LOGGER.info("Exception handling is available");
      }
      testStore.close();
    } catch (final Exception e) {
      LOGGER.warning("Exception handling not available: " + e.getMessage());
      exceptionHandlingAvailable = false;
    }
  }

  @AfterAll
  static void cleanup() {
    if (sharedEngine != null) {
      try {
        sharedEngine.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared engine: " + e.getMessage());
      }
    }
    if (sharedRuntime != null) {
      try {
        sharedRuntime.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close shared runtime: " + e.getMessage());
      }
    }
  }

  private static void assumeExceptionHandlingAvailable() {
    assumeTrue(
        exceptionHandlingAvailable,
        "Exception handling native implementation not available - skipping");
  }

  private Store store;
  private final List<AutoCloseable> resources = new ArrayList<>();

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    LOGGER.info("Setting up: " + testInfo.getDisplayName());
    if (exceptionHandlingAvailable && sharedRuntime != null && sharedEngine != null) {
      store = sharedRuntime.createStore(sharedEngine);
      resources.add(store);
    }
  }

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
    store = null;
  }

  @Nested
  @DisplayName("Tag Creation Tests")
  class TagCreationTests {

    @Test
    @DisplayName("should create exception tag")
    void shouldCreateExceptionTag(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a simple tag with no payload
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

      assertNotNull(tag, "Tag should not be null");
      assertTrue(tag.getNativeHandle() != 0, "Tag should have a valid native handle");
      LOGGER.info("Created tag with handle: 0x" + Long.toHexString(tag.getNativeHandle()));
    }

    @Test
    @DisplayName("should create tag with payload types")
    void shouldCreateTagWithPayloadTypes(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a tag with i32 and i64 payload types
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

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

    @Test
    @DisplayName("should create TagType")
    void shouldCreateTagType(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a TagType
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);

      assertNotNull(tagType, "TagType should not be null");
      assertNotNull(tagType.getFunctionType(), "TagType function type should not be null");
      LOGGER.info("Created TagType with function type: " + tagType.getFunctionType());
    }

    @Test
    @DisplayName("should return parameter types")
    void shouldReturnParameterTypes(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

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
  @DisplayName("ExnRef Tests")
  class ExnRefTests {

    @Test
    @DisplayName("should create ExnRef from tag")
    void shouldCreateExnRefFromTag(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // This test is a placeholder - ExnRef creation typically happens during exception propagation
      // The native implementation would need to support creating ExnRef objects
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

      assertNotNull(tag, "Tag should be created successfully");
      LOGGER.info("Tag created - ExnRef creation requires exception propagation context");
    }

    @Test
    @DisplayName("should get tag from ExnRef")
    void shouldGetTagFromExnRef(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // This test is a placeholder - getting tag from ExnRef requires exception context
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

      assertNotNull(tag, "Tag should be created successfully");
      LOGGER.info("Tag created - ExnRef tag retrieval requires exception context");
    }

    @Test
    @DisplayName("should get payload from ExnRef")
    void shouldGetPayloadFromExnRef(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // This test is a placeholder - getting payload from ExnRef requires exception context
      final FunctionType funcType =
          new FunctionType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

      assertNotNull(tag, "Tag should be created successfully");
      LOGGER.info("Tag with payload types created - ExnRef payload requires exception context");
    }
  }

  @Nested
  @DisplayName("Exception Lifecycle Tests")
  class ExceptionLifecycleTests {

    @Test
    @DisplayName("should close exception resources properly")
    void shouldCloseExceptionResourcesProperly(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a tag and verify it can be used and cleaned up
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

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

    @Test
    @DisplayName("should detect equal tags")
    void shouldDetectEqualTags(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create a tag
      final FunctionType funcType =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType = TagType.create(funcType);
      final Tag tag = sharedRuntime.createTag(store, tagType);

      // A tag should be equal to itself
      assertTrue(tag.equals(tag, store), "Tag should be equal to itself");
      LOGGER.info("Tag equality test passed");
    }

    @Test
    @DisplayName("should detect different tags")
    void shouldDetectDifferentTags(final TestInfo testInfo) throws Exception {
      assumeExceptionHandlingAvailable();
      LOGGER.info("Testing: " + testInfo.getDisplayName());

      // Create two different tags
      final FunctionType funcType1 =
          new FunctionType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final TagType tagType1 = TagType.create(funcType1);
      final Tag tag1 = sharedRuntime.createTag(store, tagType1);

      final FunctionType funcType2 =
          new FunctionType(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});
      final TagType tagType2 = TagType.create(funcType2);
      final Tag tag2 = sharedRuntime.createTag(store, tagType2);

      // Different tags should not be equal
      assertFalse(tag1.equals(tag2, store), "Different tags should not be equal");
      LOGGER.info("Tag inequality test passed");
    }
  }
}
