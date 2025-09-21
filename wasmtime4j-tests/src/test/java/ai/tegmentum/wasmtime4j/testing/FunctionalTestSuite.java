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

/**
 * Comprehensive functional testing suite that validates all WebAssembly operations.
 *
 * <p>This interface provides systematic functional testing of all WebAssembly APIs,
 * from core operations to advanced features, ensuring production-ready reliability.
 */
public interface FunctionalTestSuite {

    /**
     * Creates a new functional test suite instance.
     *
     * @return a new test suite instance
     */
    static FunctionalTestSuite create() {
        return new DefaultFunctionalTestSuite();
    }

    /**
     * Tests core WebAssembly operations.
     *
     * @return test results for core operations
     */
    TestResults testCoreWasmOperations();

    /**
     * Tests module lifecycle operations.
     *
     * @return test results for module lifecycle
     */
    TestResults testModuleLifecycle();

    /**
     * Tests instance operations.
     *
     * @return test results for instance operations
     */
    TestResults testInstanceOperations();

    /**
     * Tests memory operations.
     *
     * @return test results for memory operations
     */
    TestResults testMemoryOperations();

    /**
     * Tests table operations.
     *
     * @return test results for table operations
     */
    TestResults testTableOperations();

    /**
     * Tests global operations.
     *
     * @return test results for global operations
     */
    TestResults testGlobalOperations();

    /**
     * Tests linker operations.
     *
     * @return test results for linker operations
     */
    TestResults testLinkerOperations();

    /**
     * Tests type introspection capabilities.
     *
     * @return test results for type introspection
     */
    TestResults testTypeIntrospection();

    /**
     * Tests serialization and AOT compilation.
     *
     * @return test results for serialization and AOT
     */
    TestResults testSerializationAndAot();

    /**
     * Tests asynchronous operations.
     *
     * @return test results for async operations
     */
    TestResults testAsyncOperations();

    /**
     * Tests component model functionality.
     *
     * @return test results for component model
     */
    TestResults testComponentModel();

    /**
     * Tests WASI integration.
     *
     * @return test results for WASI integration
     */
    TestResults testWasiIntegration();

    /**
     * Tests error handling and edge cases.
     *
     * @return test results for error handling
     */
    TestResults testErrorHandling();

    /**
     * Tests resource limits and constraints.
     *
     * @return test results for resource limits
     */
    TestResults testResourceLimits();

    /**
     * Tests security boundaries and isolation.
     *
     * @return test results for security testing
     */
    TestResults testSecurityBoundaries();

    /**
     * Tests for memory leaks and resource management.
     *
     * @return test results for memory leak detection
     */
    TestResults testMemoryLeaks();

    /**
     * Gets the results from the last test execution.
     *
     * @return last test results or null if no tests have been run
     */
    TestResults getLastResults();

    /**
     * Runs all functional tests in sequence.
     *
     * @return combined test results from all test categories
     */
    default TestResults runAllTests() {
        final TestResultsBuilder builder = TestResults.builder();

        builder.addResults(testCoreWasmOperations());
        builder.addResults(testModuleLifecycle());
        builder.addResults(testInstanceOperations());
        builder.addResults(testMemoryOperations());
        builder.addResults(testTableOperations());
        builder.addResults(testGlobalOperations());
        builder.addResults(testLinkerOperations());
        builder.addResults(testTypeIntrospection());
        builder.addResults(testSerializationAndAot());
        builder.addResults(testAsyncOperations());
        builder.addResults(testComponentModel());
        builder.addResults(testWasiIntegration());
        builder.addResults(testErrorHandling());
        builder.addResults(testResourceLimits());
        builder.addResults(testSecurityBoundaries());
        builder.addResults(testMemoryLeaks());

        return builder.build();
    }
}