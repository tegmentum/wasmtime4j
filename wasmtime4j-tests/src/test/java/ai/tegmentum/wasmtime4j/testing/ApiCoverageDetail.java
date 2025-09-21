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
 * Detailed coverage information for a specific API.
 *
 * <p>This interface provides comprehensive analysis of an individual API's implementation
 * status, including backing implementation details and test coverage information.
 */
public interface ApiCoverageDetail {

    /**
     * Gets the name of the API.
     *
     * @return API name
     */
    String getApiName();

    /**
     * Checks if the API is fully implemented.
     *
     * @return true if API is completely implemented
     */
    boolean isImplemented();

    /**
     * Checks if the API has native backing implementation.
     *
     * @return true if native implementation exists
     */
    boolean hasNativeBacking();

    /**
     * Checks if the API has JNI implementation.
     *
     * @return true if JNI implementation exists
     */
    boolean hasJniImplementation();

    /**
     * Checks if the API has Panama implementation.
     *
     * @return true if Panama implementation exists
     */
    boolean hasPanamaImplementation();

    /**
     * Gets list of methods that are missing from this API.
     *
     * @return list of missing method names
     */
    List<String> getMissingMethods();

    /**
     * Gets test coverage information for this API.
     *
     * @return test coverage details
     */
    TestCoverageInfo getTestCoverage();

    /**
     * Gets the implementation completeness percentage for this API.
     *
     * @return completeness percentage from 0.0 to 100.0
     */
    double getCompletenessPercentage();

    /**
     * Gets the module this API belongs to.
     *
     * @return module name
     */
    String getModule();

    /**
     * Gets any implementation notes or warnings for this API.
     *
     * @return list of notes or empty list if none
     */
    List<String> getImplementationNotes();

    /**
     * Checks if this API is critical for basic functionality.
     *
     * @return true if API is considered critical
     */
    boolean isCritical();

    /**
     * Gets the current implementation status.
     *
     * @return implementation status
     */
    ImplementationStatus getStatus();

    /**
     * Implementation status enumeration.
     */
    enum ImplementationStatus {
        /** API is fully implemented and tested */
        COMPLETE,
        /** API is partially implemented */
        PARTIAL,
        /** API is declared but not implemented */
        STUB,
        /** API is completely missing */
        MISSING,
        /** API implementation is broken */
        BROKEN
    }
}