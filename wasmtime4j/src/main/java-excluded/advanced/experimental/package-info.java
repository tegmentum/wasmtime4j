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

/**
 * Experimental WebAssembly features and cutting-edge proposals.
 *
 * <h2>⚠️ EXPERIMENTAL FEATURES WARNING ⚠️</h2>
 *
 * <p>This package contains experimental implementations of cutting-edge WebAssembly proposals that
 * are still in development. These APIs are subject to change and may be removed in future versions
 * without notice. They should not be used in production environments without thorough testing and
 * understanding of the risks involved.
 *
 * <h3>Feature Flag Requirements</h3>
 *
 * <p>All experimental features require explicit enablement through feature flags and are disabled
 * by default to ensure stability of production code. Features can be enabled via system properties
 * or programmatically:
 *
 * <pre>{@code
 * // Enable via system property
 * -Dwasmtime4j.experimental.exceptions=true
 * -Dwasmtime4j.experimental.simd=true
 * -Dwasmtime4j.experimental.multivalue=true
 * -Dwasmtime4j.experimental.reftypes=true
 * -Dwasmtime4j.experimental.relaxed_simd=true
 *
 * // Enable programmatically
 * ExperimentalFeatures.enableFeature(ExperimentalFeatures.Feature.EXCEPTION_HANDLING);
 * }</pre>
 *
 * <h3>Available Experimental Features</h3>
 *
 * <ul>
 *   <li><strong>Exception Handling</strong> - WebAssembly exception handling proposal with
 *       try/catch blocks and exception throwing mechanisms
 *   <li><strong>Advanced SIMD</strong> - Extended SIMD vector operations beyond basic v128 support
 *       with platform-specific optimizations
 *   <li><strong>Multi-Value</strong> - Functions returning multiple values directly rather than
 *       through memory or global variables
 *   <li><strong>Extended Reference Types</strong> - Reference types beyond funcref and externref
 *       with typed function references and subtyping
 *   <li><strong>Relaxed SIMD</strong> - Platform-specific SIMD optimizations that may produce
 *       slightly different results across hardware
 * </ul>
 *
 * <h3>Safety and Compatibility</h3>
 *
 * <p>Experimental features implement additional safety checks and validation to prevent runtime
 * errors. However, they may have reduced performance compared to stable APIs and may not be
 * compatible across all platforms.
 *
 * <h3>Migration and Deprecation</h3>
 *
 * <p>As WebAssembly proposals mature and become standardized, experimental features may be moved to
 * the main API or deprecated entirely. Applications using experimental features should be prepared
 * for API changes and have migration plans in place.
 *
 * @since 1.0.0
 */
@ExperimentalApi(
    feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING,
    description = "Entire experimental package",
    timeline = "Varies by feature")
package ai.tegmentum.wasmtime4j.experimental;
