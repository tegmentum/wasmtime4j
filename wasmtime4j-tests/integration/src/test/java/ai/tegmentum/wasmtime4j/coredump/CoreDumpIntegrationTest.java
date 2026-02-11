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
 * <p>This test class validates the coredump interfaces and default implementations.
 */
@DisplayName("CoreDump Integration Tests")
public class CoreDumpIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(CoreDumpIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting CoreDump Integration Tests");
  }

  @Nested
  @DisplayName("WasmCoreDump Interface Tests")
  class WasmCoreDumpInterfaceTests {

    @Test
    @DisplayName("Should verify WasmCoreDump interface exists")
    void shouldVerifyWasmCoreDumpInterfaceExists() {
      LOGGER.info("Testing WasmCoreDump interface existence");

      assertTrue(WasmCoreDump.class.isInterface(), "WasmCoreDump should be an interface");

      LOGGER.info("WasmCoreDump interface verified");
    }

    @Test
    @DisplayName("Should have required interface methods")
    void shouldHaveRequiredInterfaceMethods() throws Exception {
      LOGGER.info("Testing WasmCoreDump interface methods");

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

      LOGGER.info("WasmCoreDump interface methods verified");
    }
  }

  @Nested
  @DisplayName("DefaultWasmCoreDump Implementation Tests")
  class DefaultWasmCoreDumpImplementationTests {

    @Test
    @DisplayName("Should verify DefaultWasmCoreDump class exists")
    void shouldVerifyDefaultWasmCoreDumpClassExists() {
      LOGGER.info("Testing DefaultWasmCoreDump class existence");

      assertNotNull(DefaultWasmCoreDump.class, "DefaultWasmCoreDump class should exist");
      assertFalse(
          DefaultWasmCoreDump.class.isInterface(),
          "DefaultWasmCoreDump should not be an interface");

      LOGGER.info("DefaultWasmCoreDump class verified");
    }

    @Test
    @DisplayName("Should implement WasmCoreDump interface")
    void shouldImplementWasmCoreDumpInterface() {
      LOGGER.info("Testing DefaultWasmCoreDump implements WasmCoreDump");

      assertTrue(
          WasmCoreDump.class.isAssignableFrom(DefaultWasmCoreDump.class),
          "DefaultWasmCoreDump should implement WasmCoreDump");

      LOGGER.info("DefaultWasmCoreDump implements WasmCoreDump verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpFrame Interface Tests")
  class CoreDumpFrameInterfaceTests {

    @Test
    @DisplayName("Should verify CoreDumpFrame interface exists")
    void shouldVerifyCoreDumpFrameInterfaceExists() {
      LOGGER.info("Testing CoreDumpFrame interface existence");

      assertTrue(CoreDumpFrame.class.isInterface(), "CoreDumpFrame should be an interface");

      LOGGER.info("CoreDumpFrame interface verified");
    }
  }

  @Nested
  @DisplayName("DefaultCoreDumpFrame Implementation Tests")
  class DefaultCoreDumpFrameImplementationTests {

    @Test
    @DisplayName("Should verify DefaultCoreDumpFrame class exists")
    void shouldVerifyDefaultCoreDumpFrameClassExists() {
      LOGGER.info("Testing DefaultCoreDumpFrame class existence");

      assertNotNull(DefaultCoreDumpFrame.class, "DefaultCoreDumpFrame class should exist");

      LOGGER.info("DefaultCoreDumpFrame class verified");
    }

    @Test
    @DisplayName("Should implement CoreDumpFrame interface")
    void shouldImplementCoreDumpFrameInterface() {
      LOGGER.info("Testing DefaultCoreDumpFrame implements CoreDumpFrame");

      assertTrue(
          CoreDumpFrame.class.isAssignableFrom(DefaultCoreDumpFrame.class),
          "DefaultCoreDumpFrame should implement CoreDumpFrame");

      LOGGER.info("DefaultCoreDumpFrame implements CoreDumpFrame verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpInstance Interface Tests")
  class CoreDumpInstanceInterfaceTests {

    @Test
    @DisplayName("Should verify CoreDumpInstance interface exists")
    void shouldVerifyCoreDumpInstanceInterfaceExists() {
      LOGGER.info("Testing CoreDumpInstance interface existence");

      assertTrue(CoreDumpInstance.class.isInterface(), "CoreDumpInstance should be an interface");

      LOGGER.info("CoreDumpInstance interface verified");
    }
  }

  @Nested
  @DisplayName("DefaultCoreDumpInstance Implementation Tests")
  class DefaultCoreDumpInstanceImplementationTests {

    @Test
    @DisplayName("Should verify DefaultCoreDumpInstance class exists")
    void shouldVerifyDefaultCoreDumpInstanceClassExists() {
      LOGGER.info("Testing DefaultCoreDumpInstance class existence");

      assertNotNull(DefaultCoreDumpInstance.class, "DefaultCoreDumpInstance class should exist");

      LOGGER.info("DefaultCoreDumpInstance class verified");
    }

    @Test
    @DisplayName("Should implement CoreDumpInstance interface")
    void shouldImplementCoreDumpInstanceInterface() {
      LOGGER.info("Testing DefaultCoreDumpInstance implements CoreDumpInstance");

      assertTrue(
          CoreDumpInstance.class.isAssignableFrom(DefaultCoreDumpInstance.class),
          "DefaultCoreDumpInstance should implement CoreDumpInstance");

      LOGGER.info("DefaultCoreDumpInstance implements CoreDumpInstance verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpGlobal Interface Tests")
  class CoreDumpGlobalInterfaceTests {

    @Test
    @DisplayName("Should verify CoreDumpGlobal interface exists")
    void shouldVerifyCoreDumpGlobalInterfaceExists() {
      LOGGER.info("Testing CoreDumpGlobal interface existence");

      assertTrue(CoreDumpGlobal.class.isInterface(), "CoreDumpGlobal should be an interface");

      LOGGER.info("CoreDumpGlobal interface verified");
    }
  }

  @Nested
  @DisplayName("DefaultCoreDumpGlobal Implementation Tests")
  class DefaultCoreDumpGlobalImplementationTests {

    @Test
    @DisplayName("Should verify DefaultCoreDumpGlobal class exists")
    void shouldVerifyDefaultCoreDumpGlobalClassExists() {
      LOGGER.info("Testing DefaultCoreDumpGlobal class existence");

      assertNotNull(DefaultCoreDumpGlobal.class, "DefaultCoreDumpGlobal class should exist");

      LOGGER.info("DefaultCoreDumpGlobal class verified");
    }

    @Test
    @DisplayName("Should implement CoreDumpGlobal interface")
    void shouldImplementCoreDumpGlobalInterface() {
      LOGGER.info("Testing DefaultCoreDumpGlobal implements CoreDumpGlobal");

      assertTrue(
          CoreDumpGlobal.class.isAssignableFrom(DefaultCoreDumpGlobal.class),
          "DefaultCoreDumpGlobal should implement CoreDumpGlobal");

      LOGGER.info("DefaultCoreDumpGlobal implements CoreDumpGlobal verified");
    }
  }

  @Nested
  @DisplayName("CoreDumpMemory Interface Tests")
  class CoreDumpMemoryInterfaceTests {

    @Test
    @DisplayName("Should verify CoreDumpMemory interface exists")
    void shouldVerifyCoreDumpMemoryInterfaceExists() {
      LOGGER.info("Testing CoreDumpMemory interface existence");

      assertTrue(CoreDumpMemory.class.isInterface(), "CoreDumpMemory should be an interface");

      LOGGER.info("CoreDumpMemory interface verified");
    }
  }

  @Nested
  @DisplayName("DefaultCoreDumpMemory Implementation Tests")
  class DefaultCoreDumpMemoryImplementationTests {

    @Test
    @DisplayName("Should verify DefaultCoreDumpMemory class exists")
    void shouldVerifyDefaultCoreDumpMemoryClassExists() {
      LOGGER.info("Testing DefaultCoreDumpMemory class existence");

      assertNotNull(DefaultCoreDumpMemory.class, "DefaultCoreDumpMemory class should exist");

      LOGGER.info("DefaultCoreDumpMemory class verified");
    }

    @Test
    @DisplayName("Should implement CoreDumpMemory interface")
    void shouldImplementCoreDumpMemoryInterface() {
      LOGGER.info("Testing DefaultCoreDumpMemory implements CoreDumpMemory");

      assertTrue(
          CoreDumpMemory.class.isAssignableFrom(DefaultCoreDumpMemory.class),
          "DefaultCoreDumpMemory should implement CoreDumpMemory");

      LOGGER.info("DefaultCoreDumpMemory implements CoreDumpMemory verified");
    }
  }
}
