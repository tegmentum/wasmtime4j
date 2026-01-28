/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.WitEvolutionChange;
import ai.tegmentum.wasmtime4j.WitEvolutionMetrics;
import ai.tegmentum.wasmtime4j.WitEvolutionOperation;
import ai.tegmentum.wasmtime4j.WitEvolutionResult;
import ai.tegmentum.wasmtime4j.WitEvolutionValidation;
import ai.tegmentum.wasmtime4j.WitFunctionBinder;
import ai.tegmentum.wasmtime4j.WitInterfaceBindings;
import ai.tegmentum.wasmtime4j.WitInterfaceDefinition;
import ai.tegmentum.wasmtime4j.WitInterfaceEvolution;
import ai.tegmentum.wasmtime4j.WitInterfaceIntrospection;
import ai.tegmentum.wasmtime4j.WitInterfaceLinker;
import ai.tegmentum.wasmtime4j.WitInterfaceMigrationPlan;
import ai.tegmentum.wasmtime4j.WitInterfaceParser;
import ai.tegmentum.wasmtime4j.WitInterfaceVersion;
import ai.tegmentum.wasmtime4j.WitSupportInfo;
import ai.tegmentum.wasmtime4j.WitTypeAdapter;
import ai.tegmentum.wasmtime4j.WitTypeValidator;
import ai.tegmentum.wasmtime4j.WitValueMarshaler;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WIT (WebAssembly Interface Types) interface system classes.
 *
 * <p>Tests WitInterfaceDefinition, WitInterfaceBindings, WitInterfaceParser, WitInterfaceLinker,
 * WitEvolution*, and related classes.
 */
@DisplayName("WIT Interface System Integration Tests")
public class WitInterfaceSystemIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(WitInterfaceSystemIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WIT Interface System Integration Tests");
  }

  @Nested
  @DisplayName("WitInterfaceDefinition Interface Tests")
  class WitInterfaceDefinitionTests {

    @Test
    @DisplayName("Should verify WitInterfaceDefinition is an interface")
    void shouldVerifyWitInterfaceDefinitionIsInterface() {
      LOGGER.info("Testing WitInterfaceDefinition interface structure");

      assertTrue(
          WitInterfaceDefinition.class.isInterface(),
          "WitInterfaceDefinition should be an interface");

      LOGGER.info("WitInterfaceDefinition interface structure verified");
    }

    @Test
    @DisplayName("Should have metadata methods")
    void shouldHaveMetadataMethods() throws Exception {
      LOGGER.info("Testing WitInterfaceDefinition metadata methods");

      assertNotNull(WitInterfaceDefinition.class.getMethod("getName"), "getName should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getVersion"), "getVersion should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getPackageName"), "getPackageName should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getWitText"), "getWitText should exist");

      LOGGER.info("WitInterfaceDefinition metadata methods verified");
    }

    @Test
    @DisplayName("Should have function and type methods")
    void shouldHaveFunctionAndTypeMethods() throws Exception {
      LOGGER.info("Testing WitInterfaceDefinition function/type methods");

      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getFunctionNames"),
          "getFunctionNames should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getTypeNames"), "getTypeNames should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getDependencies"),
          "getDependencies should exist");

      LOGGER.info("WitInterfaceDefinition function/type methods verified");
    }

    @Test
    @DisplayName("Should have import/export methods")
    void shouldHaveImportExportMethods() throws Exception {
      LOGGER.info("Testing WitInterfaceDefinition import/export methods");

      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getImportNames"), "getImportNames should exist");
      assertNotNull(
          WitInterfaceDefinition.class.getMethod("getExportNames"), "getExportNames should exist");

      LOGGER.info("WitInterfaceDefinition import/export methods verified");
    }

    @Test
    @DisplayName("Should have compatibility method")
    void shouldHaveCompatibilityMethod() throws Exception {
      LOGGER.info("Testing WitInterfaceDefinition compatibility method");

      Method method =
          WitInterfaceDefinition.class.getMethod("isCompatibleWith", WitInterfaceDefinition.class);
      assertNotNull(method, "isCompatibleWith should exist");
      assertEquals(
          WitCompatibilityResult.class,
          method.getReturnType(),
          "Should return WitCompatibilityResult");

      LOGGER.info("WitInterfaceDefinition compatibility method verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceBindings Interface Tests")
  class WitInterfaceBindingsTests {

    @Test
    @DisplayName("Should verify WitInterfaceBindings is an interface")
    void shouldVerifyWitInterfaceBindingsIsInterface() {
      LOGGER.info("Testing WitInterfaceBindings interface structure");

      assertTrue(
          WitInterfaceBindings.class.isInterface(), "WitInterfaceBindings should be an interface");

      LOGGER.info("WitInterfaceBindings interface structure verified");
    }

    @Test
    @DisplayName("Should have binding methods")
    void shouldHaveBindingMethods() throws Exception {
      LOGGER.info("Testing WitInterfaceBindings methods");

      Method[] methods = WitInterfaceBindings.class.getMethods();
      assertTrue(methods.length > 0, "Should have methods");

      LOGGER.info("WitInterfaceBindings methods verified: " + methods.length);
    }
  }

  @Nested
  @DisplayName("WitInterfaceParser Class Tests")
  class WitInterfaceParserTests {

    @Test
    @DisplayName("Should verify WitInterfaceParser is a class")
    void shouldVerifyWitInterfaceParserIsClass() {
      LOGGER.info("Testing WitInterfaceParser class structure");

      assertFalse(WitInterfaceParser.class.isInterface(), "WitInterfaceParser should be a class");
      assertFalse(WitInterfaceParser.class.isEnum(), "WitInterfaceParser should not be an enum");

      LOGGER.info("WitInterfaceParser class structure verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceLinker Class Tests")
  class WitInterfaceLinkerTests {

    @Test
    @DisplayName("Should verify WitInterfaceLinker is a class")
    void shouldVerifyWitInterfaceLinkerIsClass() {
      LOGGER.info("Testing WitInterfaceLinker class structure");

      assertFalse(WitInterfaceLinker.class.isInterface(), "WitInterfaceLinker should be a class");
      assertFalse(WitInterfaceLinker.class.isEnum(), "WitInterfaceLinker should not be an enum");

      LOGGER.info("WitInterfaceLinker class structure verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceEvolution Interface Tests")
  class WitInterfaceEvolutionTests {

    @Test
    @DisplayName("Should verify WitInterfaceEvolution is an interface")
    void shouldVerifyWitInterfaceEvolutionIsInterface() {
      LOGGER.info("Testing WitInterfaceEvolution interface structure");

      assertTrue(
          WitInterfaceEvolution.class.isInterface(),
          "WitInterfaceEvolution should be an interface");

      LOGGER.info("WitInterfaceEvolution interface structure verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceIntrospection Interface Tests")
  class WitInterfaceIntrospectionTests {

    @Test
    @DisplayName("Should verify WitInterfaceIntrospection is an interface")
    void shouldVerifyWitInterfaceIntrospectionIsInterface() {
      LOGGER.info("Testing WitInterfaceIntrospection interface structure");

      assertTrue(
          WitInterfaceIntrospection.class.isInterface(),
          "WitInterfaceIntrospection should be an interface");

      LOGGER.info("WitInterfaceIntrospection interface structure verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceMigrationPlan Interface Tests")
  class WitInterfaceMigrationPlanTests {

    @Test
    @DisplayName("Should verify WitInterfaceMigrationPlan is an interface")
    void shouldVerifyWitInterfaceMigrationPlanIsInterface() {
      LOGGER.info("Testing WitInterfaceMigrationPlan interface structure");

      assertTrue(
          WitInterfaceMigrationPlan.class.isInterface(),
          "WitInterfaceMigrationPlan should be an interface");

      LOGGER.info("WitInterfaceMigrationPlan interface structure verified");
    }
  }

  @Nested
  @DisplayName("WitInterfaceVersion Class Tests")
  class WitInterfaceVersionTests {

    @Test
    @DisplayName("Should verify WitInterfaceVersion is a class")
    void shouldVerifyWitInterfaceVersionIsClass() {
      LOGGER.info("Testing WitInterfaceVersion class structure");

      assertFalse(WitInterfaceVersion.class.isInterface(), "WitInterfaceVersion should be a class");
      assertFalse(WitInterfaceVersion.class.isEnum(), "WitInterfaceVersion should not be an enum");

      LOGGER.info("WitInterfaceVersion class structure verified");
    }
  }

  @Nested
  @DisplayName("WitCompatibilityResult Class Tests")
  class WitCompatibilityResultTests {

    @Test
    @DisplayName("Should verify WitCompatibilityResult is a class")
    void shouldVerifyWitCompatibilityResultIsClass() {
      LOGGER.info("Testing WitCompatibilityResult class structure");

      assertFalse(
          WitCompatibilityResult.class.isInterface(), "WitCompatibilityResult should be a class");
      assertFalse(
          WitCompatibilityResult.class.isEnum(), "WitCompatibilityResult should not be an enum");

      LOGGER.info("WitCompatibilityResult class structure verified");
    }
  }

  @Nested
  @DisplayName("WitEvolution Classes Tests")
  class WitEvolutionClassesTests {

    @Test
    @DisplayName("Should verify WitEvolutionChange is a class")
    void shouldVerifyWitEvolutionChangeIsClass() {
      LOGGER.info("Testing WitEvolutionChange class structure");

      assertFalse(WitEvolutionChange.class.isInterface(), "WitEvolutionChange should be a class");
      assertFalse(WitEvolutionChange.class.isEnum(), "WitEvolutionChange should not be an enum");

      LOGGER.info("WitEvolutionChange class structure verified");
    }

    @Test
    @DisplayName("Should verify WitEvolutionMetrics is a class")
    void shouldVerifyWitEvolutionMetricsIsClass() {
      LOGGER.info("Testing WitEvolutionMetrics class structure");

      assertFalse(WitEvolutionMetrics.class.isInterface(), "WitEvolutionMetrics should be a class");
      assertFalse(WitEvolutionMetrics.class.isEnum(), "WitEvolutionMetrics should not be an enum");

      LOGGER.info("WitEvolutionMetrics class structure verified");
    }

    @Test
    @DisplayName("Should verify WitEvolutionOperation is an enum")
    void shouldVerifyWitEvolutionOperationIsEnum() {
      LOGGER.info("Testing WitEvolutionOperation enum structure");

      assertTrue(WitEvolutionOperation.class.isEnum(), "WitEvolutionOperation should be an enum");

      LOGGER.info("WitEvolutionOperation enum structure verified");
    }

    @Test
    @DisplayName("Should verify WitEvolutionResult is a class")
    void shouldVerifyWitEvolutionResultIsClass() {
      LOGGER.info("Testing WitEvolutionResult class structure");

      assertFalse(WitEvolutionResult.class.isInterface(), "WitEvolutionResult should be a class");
      assertFalse(WitEvolutionResult.class.isEnum(), "WitEvolutionResult should not be an enum");

      LOGGER.info("WitEvolutionResult class structure verified");
    }

    @Test
    @DisplayName("Should verify WitEvolutionValidation is a class")
    void shouldVerifyWitEvolutionValidationIsClass() {
      LOGGER.info("Testing WitEvolutionValidation class structure");

      assertFalse(
          WitEvolutionValidation.class.isInterface(), "WitEvolutionValidation should be a class");
      assertFalse(
          WitEvolutionValidation.class.isEnum(), "WitEvolutionValidation should not be an enum");

      LOGGER.info("WitEvolutionValidation class structure verified");
    }
  }

  @Nested
  @DisplayName("WitFunctionBinder Class Tests")
  class WitFunctionBinderTests {

    @Test
    @DisplayName("Should verify WitFunctionBinder is a class")
    void shouldVerifyWitFunctionBinderIsClass() {
      LOGGER.info("Testing WitFunctionBinder class structure");

      assertFalse(WitFunctionBinder.class.isInterface(), "WitFunctionBinder should be a class");
      assertFalse(WitFunctionBinder.class.isEnum(), "WitFunctionBinder should not be an enum");

      LOGGER.info("WitFunctionBinder class structure verified");
    }
  }

  @Nested
  @DisplayName("WitSupportInfo Class Tests")
  class WitSupportInfoTests {

    @Test
    @DisplayName("Should verify WitSupportInfo is a class")
    void shouldVerifyWitSupportInfoIsClass() {
      LOGGER.info("Testing WitSupportInfo class structure");

      assertFalse(WitSupportInfo.class.isInterface(), "WitSupportInfo should be a class");
      assertFalse(WitSupportInfo.class.isEnum(), "WitSupportInfo should not be an enum");

      LOGGER.info("WitSupportInfo class structure verified");
    }
  }

  @Nested
  @DisplayName("WitTypeAdapter Interface Tests")
  class WitTypeAdapterTests {

    @Test
    @DisplayName("Should verify WitTypeAdapter is an interface")
    void shouldVerifyWitTypeAdapterIsInterface() {
      LOGGER.info("Testing WitTypeAdapter interface structure");

      assertTrue(WitTypeAdapter.class.isInterface(), "WitTypeAdapter should be an interface");

      LOGGER.info("WitTypeAdapter interface structure verified");
    }
  }

  @Nested
  @DisplayName("WitTypeValidator Class Tests")
  class WitTypeValidatorTests {

    @Test
    @DisplayName("Should verify WitTypeValidator is a class")
    void shouldVerifyWitTypeValidatorIsClass() {
      LOGGER.info("Testing WitTypeValidator class structure");

      assertFalse(WitTypeValidator.class.isInterface(), "WitTypeValidator should be a class");
      assertFalse(WitTypeValidator.class.isEnum(), "WitTypeValidator should not be an enum");

      LOGGER.info("WitTypeValidator class structure verified");
    }
  }

  @Nested
  @DisplayName("WitValueMarshaler Class Tests")
  class WitValueMarshalerTests {

    @Test
    @DisplayName("Should verify WitValueMarshaler is a class")
    void shouldVerifyWitValueMarshalerIsClass() {
      LOGGER.info("Testing WitValueMarshaler class structure");

      assertFalse(WitValueMarshaler.class.isInterface(), "WitValueMarshaler should be a class");
      assertFalse(WitValueMarshaler.class.isEnum(), "WitValueMarshaler should not be an enum");

      LOGGER.info("WitValueMarshaler class structure verified");
    }
  }
}
