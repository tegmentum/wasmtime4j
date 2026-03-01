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
package ai.tegmentum.wasmtime4j.spi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for SPI (Service Provider Interface) package.
 *
 * <p>This test class validates the SPI components for caller context.
 */
@DisplayName("SPI Integration Tests")
public class SpiTest {

  private static final Logger LOGGER = Logger.getLogger(SpiTest.class.getName());

  @BeforeAll
  static void setUpClass() {
    LOGGER.info("Starting SPI Integration Tests");
  }

  @Nested
  @DisplayName("CallerContextProvider Interface Tests")
  class CallerContextProviderInterfaceTests {

    @Test
    @DisplayName("Should verify CallerContextProvider interface exists")
    void shouldVerifyCallerContextProviderInterfaceExists() {
      LOGGER.info("Testing CallerContextProvider interface existence");

      assertTrue(
          CallerContextProvider.class.isInterface(),
          "CallerContextProvider should be an interface");

      LOGGER.info("CallerContextProvider interface verified");
    }

    @Test
    @DisplayName("Should have getCurrentCaller method")
    void shouldHaveGetCurrentCallerMethod() throws Exception {
      LOGGER.info("Testing CallerContextProvider getCurrentCaller method");

      Method getCurrentCaller = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertNotNull(getCurrentCaller, "getCurrentCaller method should exist");

      LOGGER.info("CallerContextProvider getCurrentCaller method verified");
    }

    @Test
    @DisplayName("Should return generic Caller type")
    void shouldReturnGenericCallerType() throws Exception {
      LOGGER.info("Testing CallerContextProvider generic return type");

      Method getCurrentCaller = CallerContextProvider.class.getMethod("getCurrentCaller");
      Type returnType = getCurrentCaller.getGenericReturnType();

      assertNotNull(returnType, "Return type should not be null");
      assertTrue(
          returnType instanceof ParameterizedType,
          "Return type should be parameterized (Caller<T>)");

      ParameterizedType paramType = (ParameterizedType) returnType;
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertTrue(typeArgs.length > 0, "Should have type arguments");

      Type typeArg = typeArgs[0];
      assertTrue(typeArg instanceof TypeVariable, "Type argument should be a type variable (T)");

      LOGGER.info("CallerContextProvider generic return type verified");
    }

    @Test
    @DisplayName("Should be suitable for ServiceLoader")
    void shouldBeSuitableForServiceLoader() {
      LOGGER.info("Testing CallerContextProvider ServiceLoader compatibility");

      assertTrue(
          CallerContextProvider.class.isInterface(),
          "CallerContextProvider should be an interface for ServiceLoader");

      var methods = CallerContextProvider.class.getMethods();
      assertTrue(methods.length > 0, "CallerContextProvider should have methods");

      LOGGER.info("CallerContextProvider ServiceLoader compatibility verified");
    }
  }
}
