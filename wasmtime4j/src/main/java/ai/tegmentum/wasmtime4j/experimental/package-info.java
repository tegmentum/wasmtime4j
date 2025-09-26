/**
 * Experimental WebAssembly features and cutting-edge Wasmtime capabilities.
 *
 * <p><b>WARNING:</b> This package contains experimental features that are unstable
 * and subject to change. These features may:
 * <ul>
 *   <li>Have incomplete implementations</li>
 *   <li>Change significantly in future versions</li>
 *   <li>Be removed without notice</li>
 *   <li>Cause performance degradation</li>
 *   <li>Have security implications</li>
 * </ul>
 *
 * <p><b>Use only for testing, development, and research purposes.</b>
 * Production use is strongly discouraged unless you fully understand the risks.
 *
 * <h2>Feature Categories</h2>
 *
 * <h3>Committee-Stage WebAssembly Proposals</h3>
 * <p>These features implement WebAssembly proposals that are currently in various
 * stages of the W3C standardization process. They may change as the proposals evolve.
 *
 * <h3>Beta Wasmtime Features</h3>
 * <p>These features are specific to the Wasmtime runtime and are in beta testing.
 * They provide advanced optimization and execution capabilities.
 *
 * <h3>Security and Sandboxing</h3>
 * <p>Experimental security features that provide enhanced sandboxing, validation,
 * and protection mechanisms beyond standard WebAssembly security.
 *
 * <h3>Performance Analysis</h3>
 * <p>Advanced profiling, debugging, and performance analysis capabilities for
 * detailed runtime introspection and optimization.
 *
 * <h3>WASI Extensions</h3>
 * <p>Experimental WASI (WebAssembly System Interface) extensions that provide
 * additional system capabilities and APIs.
 *
 * <h3>Research Features</h3>
 * <p>Cutting-edge experimental features for research and exploration of future
 * WebAssembly and runtime capabilities.
 *
 * <h2>Usage Guidelines</h2>
 *
 * <pre>{@code
 * // Create experimental configuration
 * ExperimentalFeatureConfig config = new ExperimentalFeatureConfig();
 *
 * // Enable specific experimental features
 * config.setFeature(ExperimentalFeature.STACK_SWITCHING, true);
 * config.setFeature(ExperimentalFeature.ADVANCED_JIT_OPTIMIZATIONS, true);
 *
 * // Configure feature-specific parameters
 * config.configureStackSwitching(1024 * 1024, 100, StackSwitchingStrategy.COOPERATIVE);
 * config.configureSecurity(SecurityLevel.HIGH, true, true);
 *
 * // Apply to engine configuration
 * EngineConfig engineConfig = new EngineConfig();
 * engineConfig.setExperimentalFeatures(config);
 * }</pre>
 *
 * @since 1.0.0
 * @author wasmtime4j
 */
package ai.tegmentum.wasmtime4j.experimental;