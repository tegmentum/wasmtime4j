/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.testing;

import java.util.List;

/**
 * Comprehensive API coverage analysis interface that validates 100% API coverage claim and ensures
 * all implemented APIs function correctly.
 *
 * <p>This interface provides the foundation for validating that the wasmtime4j project has truly
 * achieved complete API coverage of the Wasmtime WebAssembly runtime.
 */
public interface ApiCoverageValidator {

  /**
   * Creates a new API coverage validator instance.
   *
   * @return a new validator instance
   */
  static ApiCoverageValidator create() {
    return new DefaultApiCoverageValidator();
  }

  /**
   * Validates complete API coverage across all modules.
   *
   * @return comprehensive coverage report
   */
  CoverageReport validateApiCoverage();

  /**
   * Gets list of APIs that are missing implementation.
   *
   * @return list of missing API names
   */
  List<String> getMissingApis();

  /**
   * Gets list of methods that are declared but not implemented.
   *
   * @return list of unimplemented method names
   */
  List<String> getUnimplementedMethods();

  /**
   * Validates runtime behavior of all implemented APIs.
   *
   * @return functionality validation report
   */
  FunctionalityReport validateFunctionality();

  /**
   * Validates all API endpoints with comprehensive testing.
   *
   * @return list of validation results for each API
   */
  List<ApiValidationResult> validateAllEndpoints();

  /**
   * Validates parity with native Wasmtime implementation.
   *
   * @return parity validation report
   */
  ParityReport validateWasmtimeParity();

  /**
   * Gets list of parity violations between Java API and native Wasmtime.
   *
   * @return list of parity violation descriptions
   */
  List<String> getParityViolations();
}
