package ai.tegmentum.wasmtime4j.experimental;

/**
 * Enumeration of experimental WebAssembly features and cutting-edge Wasmtime capabilities.
 *
 * <p><b>WARNING:</b> These features are experimental and subject to change.
 * They may be unstable, have incomplete implementations, or change significantly
 * in future versions. Use only for testing, development, and research.
 *
 * <p>Features are categorized by their experimental status:
 * <ul>
 *   <li><b>Committee Stage:</b> WebAssembly proposals currently in committee review</li>
 *   <li><b>Beta Features:</b> Wasmtime features in beta testing</li>
 *   <li><b>Research:</b> Cutting-edge features for research and experimentation</li>
 * </ul>
 *
 * @since 1.0.0
 */
public enum ExperimentalFeature {

    // WebAssembly committee-stage proposals

    /** WebAssembly stack switching proposal for coroutines and fibers. */
    STACK_SWITCHING("stack-switching", "WebAssembly stack switching proposal", FeatureCategory.COMMITTEE_STAGE),

    /** WebAssembly call/cc (call-with-current-continuation) proposal. */
    CALL_CC("call-cc", "WebAssembly call/cc proposal", FeatureCategory.COMMITTEE_STAGE),

    /** Extended constant expressions allowing more complex initialization. */
    EXTENDED_CONST_EXPRESSIONS("extended-const-expressions", "Extended constant expressions", FeatureCategory.COMMITTEE_STAGE),

    /** Memory64 extended operations and optimizations. */
    MEMORY64_EXTENDED("memory64-extended", "Memory64 extended operations", FeatureCategory.COMMITTEE_STAGE),

    /** Custom page sizes for memory management optimization. */
    CUSTOM_PAGE_SIZES("custom-page-sizes", "Custom page sizes", FeatureCategory.COMMITTEE_STAGE),

    /** Shared-everything threads proposal for advanced concurrency. */
    SHARED_EVERYTHING_THREADS("shared-everything-threads", "Shared-everything threads", FeatureCategory.COMMITTEE_STAGE),

    /** Type imports for module composition and linking. */
    TYPE_IMPORTS("type-imports", "Type imports", FeatureCategory.COMMITTEE_STAGE),

    /** String imports for efficient string handling. */
    STRING_IMPORTS("string-imports", "String imports", FeatureCategory.COMMITTEE_STAGE),

    /** Resource types for better resource management. */
    RESOURCE_TYPES("resource-types", "Resource types", FeatureCategory.COMMITTEE_STAGE),

    /** Interface types for cross-language interoperability. */
    INTERFACE_TYPES("interface-types", "Interface types", FeatureCategory.COMMITTEE_STAGE),

    /** Flexible vectors for dynamic SIMD operations. */
    FLEXIBLE_VECTORS("flexible-vectors", "Flexible vectors", FeatureCategory.COMMITTEE_STAGE),

    /** Branch hinting for better prediction and optimization. */
    BRANCH_HINTING("branch-hinting", "Branch hinting", FeatureCategory.COMMITTEE_STAGE),

    /** Wide arithmetic for extended integer operations. */
    WIDE_ARITHMETIC("wide-arithmetic", "Wide arithmetic operations", FeatureCategory.COMMITTEE_STAGE),

    /** Memory control for advanced memory management. */
    MEMORY_CONTROL("memory-control", "Memory control operations", FeatureCategory.COMMITTEE_STAGE),

    // Wasmtime beta features

    /** Advanced JIT compilation optimizations. */
    ADVANCED_JIT_OPTIMIZATIONS("advanced-jit", "Advanced JIT optimizations", FeatureCategory.BETA),

    /** Machine code caching for faster startup times. */
    MACHINE_CODE_CACHING("code-caching", "Machine code caching", FeatureCategory.BETA),

    /** Cross-module optimization techniques. */
    CROSS_MODULE_OPTIMIZATIONS("cross-module-opt", "Cross-module optimizations", FeatureCategory.BETA),

    /** Speculative execution optimizations. */
    SPECULATIVE_OPTIMIZATIONS("speculative-opt", "Speculative optimizations", FeatureCategory.BETA),

    /** Adaptive tier-up compilation strategies. */
    ADAPTIVE_TIER_UP("adaptive-tier-up", "Adaptive tier-up compilation", FeatureCategory.BETA),

    /** Profile-guided optimization. */
    PROFILE_GUIDED_OPTIMIZATION("pgo", "Profile-guided optimization", FeatureCategory.BETA),

    // Security and sandboxing features

    /** Advanced sandboxing capabilities. */
    ADVANCED_SANDBOXING("advanced-sandbox", "Advanced sandboxing", FeatureCategory.SECURITY),

    /** Fine-grained resource limiting. */
    RESOURCE_LIMITING("resource-limits", "Resource limiting", FeatureCategory.SECURITY),

    /** Capability-based security model. */
    CAPABILITY_BASED_SECURITY("capability-security", "Capability-based security", FeatureCategory.SECURITY),

    /** Cryptographic validation of modules. */
    CRYPTOGRAPHIC_VALIDATION("crypto-validation", "Cryptographic validation", FeatureCategory.SECURITY),

    /** Memory protection and isolation. */
    MEMORY_PROTECTION("memory-protection", "Memory protection", FeatureCategory.SECURITY),

    /** Control flow integrity. */
    CONTROL_FLOW_INTEGRITY("cfi", "Control flow integrity", FeatureCategory.SECURITY),

    // Performance analysis and debugging

    /** Hardware performance counter integration. */
    HARDWARE_PERFORMANCE_COUNTERS("perf-counters", "Hardware performance counters", FeatureCategory.PROFILING),

    /** Runtime instrumentation for detailed analysis. */
    RUNTIME_INSTRUMENTATION("instrumentation", "Runtime instrumentation", FeatureCategory.PROFILING),

    /** Memory usage profiling and analysis. */
    MEMORY_PROFILING("memory-profiling", "Memory profiling", FeatureCategory.PROFILING),

    /** Execution tracing and replay. */
    EXECUTION_TRACING("exec-tracing", "Execution tracing", FeatureCategory.PROFILING),

    /** Function-level profiling with call graphs. */
    FUNCTION_PROFILING("function-profiling", "Function profiling", FeatureCategory.PROFILING),

    /** Instruction-level profiling for hotspot analysis. */
    INSTRUCTION_PROFILING("instruction-profiling", "Instruction profiling", FeatureCategory.PROFILING),

    // WASI extensions and capabilities

    /** WASI Preview 2 support. */
    WASI_PREVIEW2("wasi-preview2", "WASI Preview 2", FeatureCategory.WASI_EXTENSION),

    /** WASI networking capabilities. */
    WASI_NETWORKING("wasi-networking", "WASI networking", FeatureCategory.WASI_EXTENSION),

    /** Extended WASI filesystem operations. */
    WASI_FILESYSTEM_EXTENDED("wasi-filesystem-ext", "WASI filesystem extended", FeatureCategory.WASI_EXTENSION),

    /** WASI socket support. */
    WASI_SOCKETS("wasi-sockets", "WASI sockets", FeatureCategory.WASI_EXTENSION),

    /** WASI HTTP support. */
    WASI_HTTP("wasi-http", "WASI HTTP", FeatureCategory.WASI_EXTENSION),

    /** WASI cryptographic randomness. */
    WASI_RANDOM("wasi-random", "WASI random", FeatureCategory.WASI_EXTENSION),

    /** WASI key-value store. */
    WASI_KEYVALUE("wasi-keyvalue", "WASI key-value", FeatureCategory.WASI_EXTENSION),

    /** WASI blob store. */
    WASI_BLOBSTORE("wasi-blobstore", "WASI blob store", FeatureCategory.WASI_EXTENSION),

    // Research and cutting-edge features

    /** Quantum-resistant cryptographic operations. */
    QUANTUM_RESISTANT_CRYPTO("quantum-crypto", "Quantum-resistant cryptography", FeatureCategory.RESEARCH),

    /** AI/ML acceleration hooks. */
    ML_ACCELERATION("ml-acceleration", "ML acceleration", FeatureCategory.RESEARCH),

    /** GPU compute integration. */
    GPU_COMPUTE("gpu-compute", "GPU compute", FeatureCategory.RESEARCH),

    /** Distributed execution coordination. */
    DISTRIBUTED_EXECUTION("distributed-exec", "Distributed execution", FeatureCategory.RESEARCH),

    /** Hot code replacement and live updates. */
    HOT_CODE_REPLACEMENT("hot-code-replacement", "Hot code replacement", FeatureCategory.RESEARCH),

    /** Persistent execution state. */
    PERSISTENT_STATE("persistent-state", "Persistent execution state", FeatureCategory.RESEARCH),

    /** Transactional memory operations. */
    TRANSACTIONAL_MEMORY("transactional-memory", "Transactional memory", FeatureCategory.RESEARCH),

    /** Advanced garbage collection integration. */
    ADVANCED_GC("advanced-gc", "Advanced garbage collection", FeatureCategory.RESEARCH);

    private final String key;
    private final String description;
    private final FeatureCategory category;

    ExperimentalFeature(final String key, final String description, final FeatureCategory category) {
        this.key = key;
        this.description = description;
        this.category = category;
    }

    /**
     * Gets the unique key identifier for this feature.
     *
     * @return the feature key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the human-readable description of this feature.
     *
     * @return the feature description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the category of this experimental feature.
     *
     * @return the feature category
     */
    public FeatureCategory getCategory() {
        return category;
    }

    /**
     * Checks if this feature is a WebAssembly committee-stage proposal.
     *
     * @return true if this is a committee-stage proposal
     */
    public boolean isCommitteeStageProposal() {
        return category == FeatureCategory.COMMITTEE_STAGE;
    }

    /**
     * Checks if this feature is a Wasmtime beta feature.
     *
     * @return true if this is a beta feature
     */
    public boolean isBetaFeature() {
        return category == FeatureCategory.BETA;
    }

    /**
     * Checks if this feature is security-related.
     *
     * @return true if this is a security feature
     */
    public boolean isSecurityFeature() {
        return category == FeatureCategory.SECURITY;
    }

    /**
     * Checks if this feature is profiling-related.
     *
     * @return true if this is a profiling feature
     */
    public boolean isProfilingFeature() {
        return category == FeatureCategory.PROFILING;
    }

    /**
     * Checks if this feature is a WASI extension.
     *
     * @return true if this is a WASI extension
     */
    public boolean isWasiExtension() {
        return category == FeatureCategory.WASI_EXTENSION;
    }

    /**
     * Checks if this feature is research-level.
     *
     * @return true if this is a research feature
     */
    public boolean isResearchFeature() {
        return category == FeatureCategory.RESEARCH;
    }

    /**
     * Finds an experimental feature by its key.
     *
     * @param key the feature key
     * @return the experimental feature, or null if not found
     */
    public static ExperimentalFeature findByKey(final String key) {
        if (key == null) {
            return null;
        }
        for (final ExperimentalFeature feature : values()) {
            if (feature.key.equals(key)) {
                return feature;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return key + " (" + description + ")";
    }

    /**
     * Categories of experimental features.
     */
    public enum FeatureCategory {
        /** WebAssembly proposals currently in committee stage. */
        COMMITTEE_STAGE,

        /** Wasmtime features in beta testing. */
        BETA,

        /** Security and sandboxing features. */
        SECURITY,

        /** Profiling and debugging features. */
        PROFILING,

        /** WASI extensions and capabilities. */
        WASI_EXTENSION,

        /** Research and cutting-edge features. */
        RESEARCH
    }
}