/*
 * Copyright (c) 2024 Tegmentum AI. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */

package ai.tegmentum.wasmtime4j.wasi.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for WASI IO package interfaces.
 *
 * <p>This test class validates WasiInputStream, WasiOutputStream, WasiPollable, and
 * WasiStreamError.
 */
@DisplayName("WASI IO Integration Tests")
public class WasiIoIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(WasiIoIntegrationTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting WASI IO Integration Tests");
  }

  @Nested
  @DisplayName("WasiInputStream Interface Tests")
  class WasiInputStreamTests {

    @Test
    @DisplayName("Should verify WasiInputStream interface exists")
    void shouldVerifyWasiInputStreamInterfaceExists() {
      LOGGER.info("Testing WasiInputStream interface existence");

      assertNotNull(WasiInputStream.class, "WasiInputStream interface should exist");
      assertTrue(WasiInputStream.class.isInterface(), "WasiInputStream should be an interface");

      LOGGER.info("WasiInputStream interface verified");
    }

    @Test
    @DisplayName("Should extend WasiResource")
    void shouldExtendWasiResource() {
      LOGGER.info("Testing WasiInputStream extends WasiResource");

      assertTrue(
          WasiResource.class.isAssignableFrom(WasiInputStream.class),
          "WasiInputStream should extend WasiResource");

      LOGGER.info("WasiInputStream inheritance verified");
    }

    @Test
    @DisplayName("Should have read method")
    void shouldHaveReadMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiInputStream read method signature");

      Method readMethod = WasiInputStream.class.getMethod("read", long.class);
      assertNotNull(readMethod, "read method should exist");
      assertEquals(byte[].class, readMethod.getReturnType(), "read should return byte[]");

      LOGGER.info("read method signature verified");
    }

    @Test
    @DisplayName("Should have blockingRead method")
    void shouldHaveBlockingReadMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiInputStream blockingRead method signature");

      Method method = WasiInputStream.class.getMethod("blockingRead", long.class);
      assertNotNull(method, "blockingRead method should exist");
      assertEquals(byte[].class, method.getReturnType(), "blockingRead should return byte[]");

      LOGGER.info("blockingRead method signature verified");
    }

    @Test
    @DisplayName("Should have skip method")
    void shouldHaveSkipMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiInputStream skip method signature");

      Method method = WasiInputStream.class.getMethod("skip", long.class);
      assertNotNull(method, "skip method should exist");
      assertEquals(long.class, method.getReturnType(), "skip should return long");

      LOGGER.info("skip method signature verified");
    }

    @Test
    @DisplayName("Should have blockingSkip method")
    void shouldHaveBlockingSkipMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiInputStream blockingSkip method signature");

      Method method = WasiInputStream.class.getMethod("blockingSkip", long.class);
      assertNotNull(method, "blockingSkip method should exist");
      assertEquals(long.class, method.getReturnType(), "blockingSkip should return long");

      LOGGER.info("blockingSkip method signature verified");
    }

    @Test
    @DisplayName("Should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiInputStream subscribe method signature");

      Method method = WasiInputStream.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "subscribe should return WasiPollable");

      LOGGER.info("subscribe method signature verified");
    }

    @Test
    @DisplayName("Should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      LOGGER.info("Testing WasiInputStream has all expected methods");

      Set<String> methodNames =
          Arrays.stream(WasiInputStream.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(methodNames.contains("read"), "Should have read method");
      assertTrue(methodNames.contains("blockingRead"), "Should have blockingRead method");
      assertTrue(methodNames.contains("skip"), "Should have skip method");
      assertTrue(methodNames.contains("blockingSkip"), "Should have blockingSkip method");
      assertTrue(methodNames.contains("subscribe"), "Should have subscribe method");

      LOGGER.info("All expected methods verified: " + methodNames);
    }
  }

  @Nested
  @DisplayName("WasiOutputStream Interface Tests")
  class WasiOutputStreamTests {

    @Test
    @DisplayName("Should verify WasiOutputStream interface exists")
    void shouldVerifyWasiOutputStreamInterfaceExists() {
      LOGGER.info("Testing WasiOutputStream interface existence");

      assertNotNull(WasiOutputStream.class, "WasiOutputStream interface should exist");
      assertTrue(WasiOutputStream.class.isInterface(), "WasiOutputStream should be an interface");

      LOGGER.info("WasiOutputStream interface verified");
    }

    @Test
    @DisplayName("Should extend WasiResource")
    void shouldExtendWasiResource() {
      LOGGER.info("Testing WasiOutputStream extends WasiResource");

      assertTrue(
          WasiResource.class.isAssignableFrom(WasiOutputStream.class),
          "WasiOutputStream should extend WasiResource");

      LOGGER.info("WasiOutputStream inheritance verified");
    }

    @Test
    @DisplayName("Should have checkWrite method")
    void shouldHaveCheckWriteMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream checkWrite method signature");

      Method method = WasiOutputStream.class.getMethod("checkWrite");
      assertNotNull(method, "checkWrite method should exist");
      assertEquals(long.class, method.getReturnType(), "checkWrite should return long");

      LOGGER.info("checkWrite method signature verified");
    }

    @Test
    @DisplayName("Should have write method")
    void shouldHaveWriteMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream write method signature");

      Method method = WasiOutputStream.class.getMethod("write", byte[].class);
      assertNotNull(method, "write method should exist");
      assertEquals(void.class, method.getReturnType(), "write should return void");

      LOGGER.info("write method signature verified");
    }

    @Test
    @DisplayName("Should have blockingWriteAndFlush method")
    void shouldHaveBlockingWriteAndFlushMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream blockingWriteAndFlush method signature");

      Method method = WasiOutputStream.class.getMethod("blockingWriteAndFlush", byte[].class);
      assertNotNull(method, "blockingWriteAndFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "blockingWriteAndFlush should return void");

      LOGGER.info("blockingWriteAndFlush method signature verified");
    }

    @Test
    @DisplayName("Should have flush method")
    void shouldHaveFlushMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream flush method signature");

      Method method = WasiOutputStream.class.getMethod("flush");
      assertNotNull(method, "flush method should exist");
      assertEquals(void.class, method.getReturnType(), "flush should return void");

      LOGGER.info("flush method signature verified");
    }

    @Test
    @DisplayName("Should have blockingFlush method")
    void shouldHaveBlockingFlushMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream blockingFlush method signature");

      Method method = WasiOutputStream.class.getMethod("blockingFlush");
      assertNotNull(method, "blockingFlush method should exist");
      assertEquals(void.class, method.getReturnType(), "blockingFlush should return void");

      LOGGER.info("blockingFlush method signature verified");
    }

    @Test
    @DisplayName("Should have writeZeroes method")
    void shouldHaveWriteZeroesMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream writeZeroes method signature");

      Method method = WasiOutputStream.class.getMethod("writeZeroes", long.class);
      assertNotNull(method, "writeZeroes method should exist");
      assertEquals(void.class, method.getReturnType(), "writeZeroes should return void");

      LOGGER.info("writeZeroes method signature verified");
    }

    @Test
    @DisplayName("Should have blockingWriteZeroesAndFlush method")
    void shouldHaveBlockingWriteZeroesAndFlushMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream blockingWriteZeroesAndFlush method signature");

      Method method = WasiOutputStream.class.getMethod("blockingWriteZeroesAndFlush", long.class);
      assertNotNull(method, "blockingWriteZeroesAndFlush method should exist");
      assertEquals(
          void.class, method.getReturnType(), "blockingWriteZeroesAndFlush should return void");

      LOGGER.info("blockingWriteZeroesAndFlush method signature verified");
    }

    @Test
    @DisplayName("Should have splice method")
    void shouldHaveSpliceMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream splice method signature");

      Method method = WasiOutputStream.class.getMethod("splice", WasiInputStream.class, long.class);
      assertNotNull(method, "splice method should exist");
      assertEquals(long.class, method.getReturnType(), "splice should return long");

      LOGGER.info("splice method signature verified");
    }

    @Test
    @DisplayName("Should have blockingSplice method")
    void shouldHaveBlockingSpliceMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream blockingSplice method signature");

      Method method =
          WasiOutputStream.class.getMethod("blockingSplice", WasiInputStream.class, long.class);
      assertNotNull(method, "blockingSplice method should exist");
      assertEquals(long.class, method.getReturnType(), "blockingSplice should return long");

      LOGGER.info("blockingSplice method signature verified");
    }

    @Test
    @DisplayName("Should have subscribe method")
    void shouldHaveSubscribeMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiOutputStream subscribe method signature");

      Method method = WasiOutputStream.class.getMethod("subscribe");
      assertNotNull(method, "subscribe method should exist");
      assertEquals(
          WasiPollable.class, method.getReturnType(), "subscribe should return WasiPollable");

      LOGGER.info("subscribe method signature verified");
    }

    @Test
    @DisplayName("Should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      LOGGER.info("Testing WasiOutputStream has all expected methods");

      Set<String> methodNames =
          Arrays.stream(WasiOutputStream.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(methodNames.contains("checkWrite"), "Should have checkWrite method");
      assertTrue(methodNames.contains("write"), "Should have write method");
      assertTrue(
          methodNames.contains("blockingWriteAndFlush"),
          "Should have blockingWriteAndFlush method");
      assertTrue(methodNames.contains("flush"), "Should have flush method");
      assertTrue(methodNames.contains("blockingFlush"), "Should have blockingFlush method");
      assertTrue(methodNames.contains("writeZeroes"), "Should have writeZeroes method");
      assertTrue(
          methodNames.contains("blockingWriteZeroesAndFlush"),
          "Should have blockingWriteZeroesAndFlush method");
      assertTrue(methodNames.contains("splice"), "Should have splice method");
      assertTrue(methodNames.contains("blockingSplice"), "Should have blockingSplice method");
      assertTrue(methodNames.contains("subscribe"), "Should have subscribe method");

      LOGGER.info("All expected methods verified: " + methodNames);
    }
  }

  @Nested
  @DisplayName("WasiPollable Interface Tests")
  class WasiPollableTests {

    @Test
    @DisplayName("Should verify WasiPollable interface exists")
    void shouldVerifyWasiPollableInterfaceExists() {
      LOGGER.info("Testing WasiPollable interface existence");

      assertNotNull(WasiPollable.class, "WasiPollable interface should exist");
      assertTrue(WasiPollable.class.isInterface(), "WasiPollable should be an interface");

      LOGGER.info("WasiPollable interface verified");
    }

    @Test
    @DisplayName("Should extend WasiResource")
    void shouldExtendWasiResource() {
      LOGGER.info("Testing WasiPollable extends WasiResource");

      assertTrue(
          WasiResource.class.isAssignableFrom(WasiPollable.class),
          "WasiPollable should extend WasiResource");

      LOGGER.info("WasiPollable inheritance verified");
    }

    @Test
    @DisplayName("Should have block method")
    void shouldHaveBlockMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiPollable block method signature");

      Method method = WasiPollable.class.getMethod("block");
      assertNotNull(method, "block method should exist");
      assertEquals(void.class, method.getReturnType(), "block should return void");

      LOGGER.info("block method signature verified");
    }

    @Test
    @DisplayName("Should have ready method")
    void shouldHaveReadyMethod() throws NoSuchMethodException {
      LOGGER.info("Testing WasiPollable ready method signature");

      Method method = WasiPollable.class.getMethod("ready");
      assertNotNull(method, "ready method should exist");
      assertEquals(boolean.class, method.getReturnType(), "ready should return boolean");

      LOGGER.info("ready method signature verified");
    }

    @Test
    @DisplayName("Should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      LOGGER.info("Testing WasiPollable has all expected methods");

      Set<String> methodNames =
          Arrays.stream(WasiPollable.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      assertTrue(methodNames.contains("block"), "Should have block method");
      assertTrue(methodNames.contains("ready"), "Should have ready method");

      LOGGER.info("All expected methods verified: " + methodNames);
    }
  }

  @Nested
  @DisplayName("WasiStreamError Tests")
  class WasiStreamErrorTests {

    @Test
    @DisplayName("Should verify WasiStreamError class exists")
    void shouldVerifyWasiStreamErrorClassExists() {
      LOGGER.info("Testing WasiStreamError class existence");

      assertNotNull(WasiStreamError.class, "WasiStreamError class should exist");
      assertTrue(
          WasmException.class.isAssignableFrom(WasiStreamError.class),
          "WasiStreamError should extend WasmException");

      LOGGER.info("WasiStreamError class verified");
    }

    @Test
    @DisplayName("Should have ErrorType enum")
    void shouldHaveErrorTypeEnum() {
      LOGGER.info("Testing WasiStreamError.ErrorType enum");

      WasiStreamError.ErrorType[] types = WasiStreamError.ErrorType.values();

      assertEquals(2, types.length, "Should have 2 error types");
      assertNotNull(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED, "LAST_OPERATION_FAILED should exist");
      assertNotNull(WasiStreamError.ErrorType.CLOSED, "CLOSED should exist");

      LOGGER.info("ErrorType enum verified");
    }

    @Test
    @DisplayName("Should create lastOperationFailed error with message and details")
    void shouldCreateLastOperationFailedErrorWithMessageAndDetails() {
      LOGGER.info("Testing lastOperationFailed factory method");

      Object details = "Detailed error info";
      WasiStreamError error = WasiStreamError.lastOperationFailed("Operation failed", details);

      assertNotNull(error, "Error should not be null");
      assertEquals("Operation failed", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED,
          error.getErrorType(),
          "Error type should be LAST_OPERATION_FAILED");
      assertTrue(error.getErrorDetails().isPresent(), "Error details should be present");
      assertEquals("Detailed error info", error.getErrorDetails().get(), "Details should match");
      assertTrue(error.isOperationFailed(), "isOperationFailed should return true");
      assertFalse(error.isClosed(), "isClosed should return false");

      LOGGER.info("lastOperationFailed with details verified");
    }

    @Test
    @DisplayName("Should create lastOperationFailed error with message only")
    void shouldCreateLastOperationFailedErrorWithMessageOnly() {
      LOGGER.info("Testing lastOperationFailed factory method with message only");

      WasiStreamError error = WasiStreamError.lastOperationFailed("Simple error");

      assertNotNull(error, "Error should not be null");
      assertEquals("Simple error", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.LAST_OPERATION_FAILED,
          error.getErrorType(),
          "Error type should be LAST_OPERATION_FAILED");
      assertTrue(error.getErrorDetails().isEmpty(), "Error details should be empty");

      LOGGER.info("lastOperationFailed with message only verified");
    }

    @Test
    @DisplayName("Should create closed error with message")
    void shouldCreateClosedErrorWithMessage() {
      LOGGER.info("Testing closed factory method with message");

      WasiStreamError error = WasiStreamError.closed("Custom closed message");

      assertNotNull(error, "Error should not be null");
      assertEquals("Custom closed message", error.getMessage(), "Message should match");
      assertEquals(
          WasiStreamError.ErrorType.CLOSED, error.getErrorType(), "Error type should be CLOSED");
      assertTrue(error.isClosed(), "isClosed should return true");
      assertFalse(error.isOperationFailed(), "isOperationFailed should return false");

      LOGGER.info("closed with message verified");
    }

    @Test
    @DisplayName("Should create closed error with default message")
    void shouldCreateClosedErrorWithDefaultMessage() {
      LOGGER.info("Testing closed factory method with default message");

      WasiStreamError error = WasiStreamError.closed();

      assertNotNull(error, "Error should not be null");
      assertEquals("Stream is closed", error.getMessage(), "Default message should be used");
      assertEquals(
          WasiStreamError.ErrorType.CLOSED, error.getErrorType(), "Error type should be CLOSED");

      LOGGER.info("closed with default message verified");
    }

    @Test
    @DisplayName("Should have proper toString implementation")
    void shouldHaveProperToStringImplementation() {
      LOGGER.info("Testing WasiStreamError toString");

      WasiStreamError errorWithDetails =
          WasiStreamError.lastOperationFailed("Write failed", "IO error");
      String stringWithDetails = errorWithDetails.toString();
      assertTrue(
          stringWithDetails.contains("LAST_OPERATION_FAILED"),
          "toString should contain error type");
      assertTrue(stringWithDetails.contains("Write failed"), "toString should contain message");
      assertTrue(stringWithDetails.contains("IO error"), "toString should contain details");

      WasiStreamError errorWithoutDetails = WasiStreamError.closed("Stream terminated");
      String stringWithoutDetails = errorWithoutDetails.toString();
      assertTrue(stringWithoutDetails.contains("CLOSED"), "toString should contain error type");
      assertTrue(
          stringWithoutDetails.contains("Stream terminated"), "toString should contain message");

      LOGGER.info("toString implementation verified");
    }

    @Test
    @DisplayName("Should be throwable as exception")
    void shouldBeThrowableAsException() {
      LOGGER.info("Testing WasiStreamError as throwable exception");

      WasiStreamError error = WasiStreamError.lastOperationFailed("Test error");

      assertThrows(
          WasiStreamError.class,
          () -> {
            throw error;
          },
          "WasiStreamError should be throwable");

      LOGGER.info("Throwable exception verified");
    }

    @Test
    @DisplayName("Should have correct error type ordinal values")
    void shouldHaveCorrectErrorTypeOrdinalValues() {
      LOGGER.info("Testing ErrorType ordinal values");

      assertEquals(0, WasiStreamError.ErrorType.LAST_OPERATION_FAILED.ordinal());
      assertEquals(1, WasiStreamError.ErrorType.CLOSED.ordinal());

      LOGGER.info("ErrorType ordinal values verified");
    }
  }
}
