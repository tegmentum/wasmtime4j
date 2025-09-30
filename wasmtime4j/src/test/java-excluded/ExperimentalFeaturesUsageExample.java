/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Usage examples for experimental WebAssembly features.
 *
 * <p><strong>⚠️ EXPERIMENTAL FEATURES</strong> - These examples demonstrate cutting-edge
 * WebAssembly proposals that are subject to change.
 */
final class ExperimentalFeaturesUsageExample {

  @Test
  void demonstrateFeatureManagement() {
    // Enable experimental features programmatically
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);

    // Check which features are enabled
    assertTrue(
        ExperimentalFeatures.isFeatureEnabled(ExperimentalFeatures.Feature.EXCEPTION_HANDLING));
    assertTrue(ExperimentalFeatures.hasEnabledFeatures());

    System.out.println(
        "Enabled experimental features: " + ExperimentalFeatures.getEnabledFeatures());

    // Alternative: Enable via system properties
    // -Dwasmtime4j.experimental.exceptions=true
    // -Dwasmtime4j.experimental.simd=true
  }

  @Test
  void demonstrateExceptionHandling() {
    // Enable exception handling feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);

    // Configure exception handling
    final ExceptionHandler.ExceptionHandlingConfig config =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(true)
            .enableExceptionUnwinding(true)
            .maxUnwindDepth(1000)
            .validateExceptionTypes(true)
            .build();

    // Create exception handler
    final ExceptionHandler handler = new ExceptionHandler(config);

    // Create exception tags
    final List<WasmValueType> errorTypes = Arrays.asList(WasmValueType.I32, WasmValueType.F64);
    final ExceptionHandler.ExceptionTag errorTag =
        handler.createExceptionTag("runtime_error", errorTypes);

    System.out.println("Created exception tag: " + errorTag.getName());

    // Throw an exception (in a real scenario, this would be caught by WebAssembly)
    final List<WasmValue> payload = Arrays.asList(WasmValue.i32(404), WasmValue.fromF64(1.23));

    assertThrows(
        ExceptionHandler.WasmException.class,
        () -> {
          handler.throwException(errorTag, payload);
        });

    handler.close();
  }

  @Test
  void demonstrateAdvancedSimd() {
    // Enable advanced SIMD feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);

    // Configure SIMD operations
    final SimdOperations.SIMDConfig config =
        SimdOperations.SIMDConfig.builder()
            .enablePlatformOptimizations(true)
            .enableRelaxedOperations(false)
            .validateVectorOperands(true)
            .maxVectorWidth(128)
            .build();

    // Create SIMD operations handler
    final SimdOperations simd = new SimdOperations(config);

    // Create vectors
    final SimdOperations.V128 vector1 = SimdOperations.V128.fromInts(1, 2, 3, 4);
    final SimdOperations.V128 vector2 = SimdOperations.V128.fromInts(5, 6, 7, 8);

    // Perform vector operations
    final SimdOperations.V128 sum = simd.add(vector1, vector2);
    final SimdOperations.V128 product = simd.multiply(vector1, vector2);

    System.out.println("Vector sum: " + Arrays.toString(sum.getAsInts()));
    System.out.println("Vector product: " + Arrays.toString(product.getAsInts()));

    // Demonstrate vector shuffle
    final byte[] shuffleIndices = {3, 2, 1, 0, 7, 6, 5, 4, 11, 10, 9, 8, 15, 14, 13, 12};
    final SimdOperations.V128 shuffled = simd.shuffle(vector1, vector2, shuffleIndices);
    System.out.println("Shuffled vector: " + Arrays.toString(shuffled.getAsInts()));

    simd.close();
  }

  @Test
  void demonstrateMultiValueFunctions() {
    // Enable multi-value feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);

    // Configure multi-value operations
    final MultiValueFunction.MultiValueConfig config =
        MultiValueFunction.MultiValueConfig.builder()
            .validateReturnTypes(true)
            .enableParameterValidation(true)
            .maxReturnValues(8)
            .allowEmptyReturns(false)
            .build();

    // Create multi-value function handler
    final MultiValueFunction handler = new MultiValueFunction(config);

    // Define a function signature that returns multiple values
    final List<WasmValueType> paramTypes = Arrays.asList(WasmValueType.I32, WasmValueType.I32);
    final List<WasmValueType> returnTypes =
        Arrays.asList(WasmValueType.I32, WasmValueType.I32, WasmValueType.I32);

    final MultiValueFunction.MultiValueSignature signature =
        new MultiValueFunction.MultiValueSignature(
            "arithmetic_operations", paramTypes, returnTypes);

    // Create a host function that returns multiple values
    final MultiValueFunction.MultiValueHostFunction implementation =
        parameters -> {
          final int a = parameters.get(0).asI32();
          final int b = parameters.get(1).asI32();

          // Return sum, difference, and product
          return new MultiValueFunction.MultiValueResult(
              Arrays.asList(
                  WasmValue.i32(a + b), // sum
                  WasmValue.i32(a - b), // difference
                  WasmValue.i32(a * b) // product
                  ));
        };

    final long functionHandle = handler.createHostFunction(signature, implementation);
    System.out.println("Created multi-value host function with handle: " + functionHandle);

    // Test the function
    final List<WasmValue> inputs = Arrays.asList(WasmValue.i32(10), WasmValue.i32(3));
    final MultiValueFunction.MultiValueResult result = implementation.invoke(inputs);

    System.out.println("Multi-value function results:");
    System.out.println("  Sum: " + result.getValue(0).asI32());
    System.out.println("  Difference: " + result.getValue(1).asI32());
    System.out.println("  Product: " + result.getValue(2).asI32());

    handler.close();
  }

  @Test
  void demonstrateExtendedReferenceTypes() {
    // Enable extended reference types feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.REFERENCE_TYPES_EXTENDED);

    // Configure reference type operations
    final ReferenceTypesExtended.ReferenceTypeConfig config =
        ReferenceTypesExtended.ReferenceTypeConfig.builder()
            .enableSubtyping(true)
            .validateNullability(true)
            .strictTypeChecking(true)
            .maxReferenceDepth(50)
            .build();

    // Create reference types handler
    final ReferenceTypesExtended refTypes = new ReferenceTypesExtended(config);

    // Create typed function references (would use actual function types in practice)
    // This is a simplified example showing the API structure

    System.out.println("Extended reference types handler created");
    System.out.println("Subtyping enabled: " + config.isSubtypingEnabled());
    System.out.println("Nullability validation: " + config.isNullabilityValidationEnabled());

    // Demonstrate reference type validation
    final ReferenceTypesExtended.ReferenceTypeValidator validator = refTypes.getValidator();
    final boolean isSubtype =
        validator.isSubtype(
            ReferenceTypesExtended.ExtendedReferenceType.TYPED_FUNCREF,
            ReferenceTypesExtended.ExtendedReferenceType.TYPED_FUNCREF);

    System.out.println("Type relationship check: " + isSubtype);

    refTypes.close();
  }

  @Test
  void demonstrateRelaxedSimd() {
    // Enable relaxed SIMD feature
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.RELAXED_SIMD);

    // Configure relaxed SIMD with platform detection
    final RelaxedSimd.RelaxedSimdConfig config =
        RelaxedSimd.RelaxedSimdConfig.builder()
            .enableAllOperations()
            .autoDetectCapabilities()
            .allowPlatformSpecificResults(true)
            .validateOperands(true)
            .enableFallbackImplementations(true)
            .build();

    // Create relaxed SIMD handler
    final RelaxedSimd relaxedSimd = new RelaxedSimd(config);

    // Create vectors for relaxed operations
    final SimdOperations.V128 a = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
    final SimdOperations.V128 b = SimdOperations.V128.fromFloats(0.5f, 1.5f, 2.5f, 3.5f);
    final SimdOperations.V128 c = SimdOperations.V128.fromFloats(0.1f, 0.2f, 0.3f, 0.4f);

    // Perform relaxed multiply-add operation
    final RelaxedSimd.RelaxedResult fmaddResult = relaxedSIMD.relaxedFmadd(a, b, c);

    System.out.println("Relaxed FMADD result:");
    System.out.println("  Vector: " + Arrays.toString(fmaddResult.getResult().getAsFloats()));
    System.out.println("  Platform optimized: " + fmaddResult.isPlatformOptimized());
    System.out.println("  Used capability: " + fmaddResult.getUsedCapability());

    // Check available platform capabilities
    System.out.println("Available capabilities: " + relaxedSIMD.getAvailableCapabilities());

    relaxedSIMD.close();
  }

  @Test
  void demonstrateCompleteWorkflow() {
    System.out.println("=== Experimental WebAssembly Features Workflow ===");

    // 1. Check and enable features
    System.out.println("\n1. Enabling experimental features...");
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.ADVANCED_SIMD);
    ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.MULTI_VALUE);

    System.out.println("Enabled features: " + ExperimentalFeatures.getEnabledFeatures().size());

    // 2. Set up exception handling for error management
    System.out.println("\n2. Setting up exception handling...");
    final ExceptionHandler.ExceptionHandlingConfig exceptionConfig =
        ExceptionHandler.ExceptionHandlingConfig.builder()
            .enableNestedTryCatch(true)
            .validateExceptionTypes(true)
            .build();

    try (final ExceptionHandler exceptionHandler = new ExceptionHandler(exceptionConfig)) {
      // Create error tags for different error types
      final ExceptionHandler.ExceptionTag arithmeticError =
          exceptionHandler.createExceptionTag(
              "arithmetic_error", Collections.singletonList(WasmValueType.I32));

      System.out.println("Created exception tag: " + arithmeticError.getName());

      // 3. Set up advanced SIMD for high-performance computing
      System.out.println("\n3. Setting up advanced SIMD operations...");
      final SimdOperations.SIMDConfig simdConfig =
          SimdOperations.SIMDConfig.builder()
              .enablePlatformOptimizations(true)
              .validateVectorOperands(true)
              .build();

      try (final SimdOperations simdOps = new SimdOperations(simdConfig)) {
        // Perform vectorized operations
        final SimdOperations.V128 data1 = SimdOperations.V128.fromFloats(1.0f, 2.0f, 3.0f, 4.0f);
        final SimdOperations.V128 data2 = SimdOperations.V128.fromFloats(2.0f, 3.0f, 4.0f, 5.0f);

        final SimdOperations.V128 result = simdOps.add(data1, data2);
        System.out.println("SIMD addition result: " + Arrays.toString(result.getAsFloats()));

        // 4. Set up multi-value functions for complex return types
        System.out.println("\n4. Setting up multi-value functions...");
        final MultiValueFunction.MultiValueConfig mvConfig =
            MultiValueFunction.MultiValueConfig.builder()
                .validateReturnTypes(true)
                .maxReturnValues(4)
                .build();

        try (final MultiValueFunction mvHandler = new MultiValueFunction(mvConfig)) {
          // Create a statistical analysis function that returns multiple metrics
          final MultiValueFunction.MultiValueHostFunction statsFunction =
              parameters -> {
                final float[] values = new float[parameters.size()];
                for (int i = 0; i < parameters.size(); i++) {
                  values[i] = parameters.get(i).asF32();
                }

                // Calculate statistics
                float sum = 0;
                float min = Float.MAX_VALUE;
                float max = Float.MIN_VALUE;
                for (final float value : values) {
                  sum += value;
                  min = Math.min(min, value);
                  max = Math.max(max, value);
                }
                final float avg = sum / values.length;

                return new MultiValueFunction.MultiValueResult(
                    Arrays.asList(
                        WasmValue.f32(sum), // total
                        WasmValue.f32(avg), // average
                        WasmValue.f32(min), // minimum
                        WasmValue.f32(max) // maximum
                        ));
              };

          // Test the multi-value function
          final List<WasmValue> testData =
              Arrays.asList(
                  WasmValue.f32(1.5f),
                  WasmValue.f32(2.7f),
                  WasmValue.f32(3.1f),
                  WasmValue.f32(4.9f));

          final MultiValueFunction.MultiValueResult stats = statsFunction.invoke(testData);

          System.out.println("Statistical analysis results:");
          System.out.println("  Sum: " + stats.getValue(0).asF32());
          System.out.println("  Average: " + stats.getValue(1).asF32());
          System.out.println("  Minimum: " + stats.getValue(2).asF32());
          System.out.println("  Maximum: " + stats.getValue(3).asF32());
        }
      }
    }

    System.out.println("\n=== Workflow completed successfully! ===");

    // Clean up
    ExperimentalFeatures.reset();
  }
}
