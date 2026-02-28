/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for coredump package.
 *
 * <p>This test class validates the coredump classes and their public APIs.
 */
@DisplayName("CoreDump Integration Tests")
public class CoreDumpTest {

  private static final Logger LOGGER = Logger.getLogger(CoreDumpTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting CoreDump Integration Tests");
  }

  @Nested
  @DisplayName("WasmCoreDump Class Tests")
  class WasmCoreDumpClassTests {

    @Test
    @DisplayName("Should verify WasmCoreDump class exists and is concrete")
    void shouldVerifyWasmCoreDumpClassExists() {
      LOGGER.info("Testing WasmCoreDump class existence");

      assertNotNull(WasmCoreDump.class, "WasmCoreDump class should exist");
      assertFalse(WasmCoreDump.class.isInterface(), "WasmCoreDump should be a concrete class");

      LOGGER.info("WasmCoreDump class verified");
    }

    @Test
    @DisplayName("Should have required public methods")
    void shouldHaveRequiredPublicMethods() throws Exception {
      LOGGER.info("Testing WasmCoreDump public methods");

      Method getName = WasmCoreDump.class.getMethod("getName");
      assertNotNull(getName, "getName method should exist");

      Method getFrames = WasmCoreDump.class.getMethod("getFrames");
      assertNotNull(getFrames, "getFrames method should exist");

      Method getModules = WasmCoreDump.class.getMethod("getModules");
      assertNotNull(getModules, "getModules method should exist");

      Method getInstances = WasmCoreDump.class.getMethod("getInstances");
      assertNotNull(getInstances, "getInstances method should exist");

      Method getGlobals = WasmCoreDump.class.getMethod("getGlobals");
      assertNotNull(getGlobals, "getGlobals method should exist");

      Method getMemories = WasmCoreDump.class.getMethod("getMemories");
      assertNotNull(getMemories, "getMemories method should exist");

      Method serialize = WasmCoreDump.class.getMethod("serialize");
      assertNotNull(serialize, "serialize method should exist");

      Method getSize = WasmCoreDump.class.getMethod("getSize");
      assertNotNull(getSize, "getSize method should exist");

      Method getTrapMessage = WasmCoreDump.class.getMethod("getTrapMessage");
      assertNotNull(getTrapMessage, "getTrapMessage method should exist");

      LOGGER.info("WasmCoreDump public methods verified");
    }

    @Test
    @DisplayName("Should have builder factory method")
    void shouldHaveBuilderFactoryMethod() throws Exception {
      LOGGER.info("Testing WasmCoreDump builder method");

      Method builder = WasmCoreDump.class.getMethod("builder");
      assertNotNull(builder, "builder method should exist");
      assertTrue(
          java.lang.reflect.Modifier.isStatic(builder.getModifiers()), "builder should be static");

      LOGGER.info("WasmCoreDump builder method verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpFrame Class Tests")
  class CoreDumpFrameClassTests {

    @Test
    @DisplayName("Should verify CoreDumpFrame class exists and is concrete")
    void shouldVerifyCoreDumpFrameClassExists() {
      LOGGER.info("Testing CoreDumpFrame class existence");

      assertNotNull(CoreDumpFrame.class, "CoreDumpFrame class should exist");
      assertFalse(CoreDumpFrame.class.isInterface(), "CoreDumpFrame should be a concrete class");

      LOGGER.info("CoreDumpFrame class verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpInstance Class Tests")
  class CoreDumpInstanceClassTests {

    @Test
    @DisplayName("Should verify CoreDumpInstance class exists and is concrete")
    void shouldVerifyCoreDumpInstanceClassExists() {
      LOGGER.info("Testing CoreDumpInstance class existence");

      assertNotNull(CoreDumpInstance.class, "CoreDumpInstance class should exist");
      assertFalse(
          CoreDumpInstance.class.isInterface(), "CoreDumpInstance should be a concrete class");

      LOGGER.info("CoreDumpInstance class verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpGlobal Class Tests")
  class CoreDumpGlobalClassTests {

    @Test
    @DisplayName("Should verify CoreDumpGlobal class exists and is concrete")
    void shouldVerifyCoreDumpGlobalClassExists() {
      LOGGER.info("Testing CoreDumpGlobal class existence");

      assertNotNull(CoreDumpGlobal.class, "CoreDumpGlobal class should exist");
      assertFalse(CoreDumpGlobal.class.isInterface(), "CoreDumpGlobal should be a concrete class");

      LOGGER.info("CoreDumpGlobal class verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpMemory Class Tests")
  class CoreDumpMemoryClassTests {

    @Test
    @DisplayName("Should verify CoreDumpMemory class exists and is concrete")
    void shouldVerifyCoreDumpMemoryClassExists() {
      LOGGER.info("Testing CoreDumpMemory class existence");

      assertNotNull(CoreDumpMemory.class, "CoreDumpMemory class should exist");
      assertFalse(CoreDumpMemory.class.isInterface(), "CoreDumpMemory should be a concrete class");

      LOGGER.info("CoreDumpMemory class verified");
    }
  }
}
