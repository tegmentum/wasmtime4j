//! # Wasmtime4j Native Library
//!
//! This crate provides unified native bindings for the Wasmtime WebAssembly runtime,
//! supporting both JNI (Java Native Interface) for Java 8-22 and Panama Foreign
//! Function Interface for Java 23+.
//!
//! The library is designed to be consumed by both wasmtime4j-jni and wasmtime4j-panama
//! modules, providing a single source of truth for native Wasmtime functionality.
//!
//! ## Architecture
//!
//! - **Core Modules**: Engine, Module, Store, Instance - Core WebAssembly runtime functionality
//! - **Interface Modules**: JNI and Panama FFI bindings for Java integration  
//! - **Error System**: Comprehensive error handling with defensive programming patterns
//! - **WASI Support**: WebAssembly System Interface for file I/O and system operations
//!
//! ## Safety and Defensive Programming
//!
//! This library implements defensive programming patterns throughout to prevent JVM crashes
//! and ensure robust operation in production environments. All public APIs validate inputs
//! and handle errors gracefully.

#![warn(missing_docs)]
#![warn(clippy::all)]

use std::os::raw::c_char;

// Core modules - foundational WebAssembly runtime components
pub mod error;
pub mod engine;
pub mod module;
pub mod store;
pub mod instance;
pub mod hostfunc;
pub mod memory;
pub mod global;
pub mod table;
pub mod linker;
pub mod caller;
pub mod serialization;
pub mod threading;
pub mod streaming_compiler;

// Additional core functionality for comprehensive API coverage
pub mod caller;
pub mod serialization;

// Advanced threading optimizations and work-stealing refinements
pub mod work_stealing;
pub mod thread_affinity;
pub mod lockfree_structures;
pub mod adaptive_scaling;
pub mod sync_primitives;
pub mod thread_profiler;
pub mod memory_coordination;
pub mod deadlock_prevention;

// Comprehensive integration tests for threading optimizations
#[cfg(test)]
pub mod threading_integration_tests;

// Advanced execution control with fuel and epoch management
pub mod execution_control;

// Shared FFI architecture with trait-based conversions
pub mod shared_ffi;

// Common FFI utilities shared between JNI and Panama implementations
pub mod ffi_common;

// Test modules for runtime completion validation
#[cfg(test)]
pub mod test_runtime_completion;

// Interface modules - will be implemented in later streams
#[cfg(feature = "jni-bindings")]
pub mod jni_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_gc_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_snapshot_bindings;
#[cfg(feature = "jni-bindings")]
pub mod jni_hot_reload_bindings;
#[cfg(feature = "jni-bindings")]
pub mod platform_memory_jni;
#[cfg(feature = "jni-bindings")]
pub mod jni_experimental_bindings;
#[cfg(feature = "panama-ffi")]
pub mod panama_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_gc_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_hot_reload_ffi;
#[cfg(feature = "panama-ffi")]
pub mod panama_experimental_ffi;

// Advanced modules - will be implemented in later tasks
#[cfg(feature = "wasi")]
pub mod wasi;

// Advanced configuration modules for comprehensive optimization control
// pub mod config_cranelift;
// pub mod platform_config;
pub mod engine_config;

// Experimental WebAssembly features (committee-stage proposals)
pub mod experimental_features;

// Advanced experimental features for cutting-edge capabilities
pub mod advanced_experimental;

// Security module for enterprise features
pub mod security;

// Sandbox module for advanced isolation
pub mod sandbox;

// Access control module for authorization
pub mod access_control;

// Audit module for compliance and logging
pub mod audit;

// Production-ready enterprise features
pub mod pooling_allocator;
pub mod module_cache;
pub mod profiler;
pub mod resource_manager;
pub mod error_recovery;

// Async runtime for async WebAssembly operations
pub mod async_runtime;

// Real async operations implementation
pub mod async_ops;

// Enhanced WASI Preview 2 implementation
pub mod wasi_preview2;

// Real networking operations
pub mod networking;

// Platform-specific optimizations for maximum performance
pub mod numa_topology;
pub mod cpu_cache_management;
pub mod memory_bandwidth_optimization;
pub mod cpu_microarchitecture_detection;
pub mod platform_config;

// Advanced networking protocols (WebSocket, HTTP/2, gRPC)
#[cfg(feature = "advanced-networking")]
pub mod advanced_networking;

// Enhanced filesystem operations
pub mod filesystem;

// Advanced filesystem snapshot operations with versioning and rollback
pub mod filesystem_snapshots;

// Full process integration
pub mod process;

// WASI security and capabilities
pub mod wasi_security;

// Type introspection system
pub mod type_introspection;

// Source map integration for debugging
pub mod sourcemap;

// Component model support for WASI Preview 2
#[cfg(feature = "component-model")]
pub mod component;
#[cfg(feature = "component-model")]
pub mod component_core;
#[cfg(feature = "component-model")]
pub mod wit_interfaces;
#[cfg(feature = "component-model")]
pub mod component_orchestration;
#[cfg(feature = "component-model")]
pub mod component_resources;
#[cfg(feature = "component-model")]
pub mod distributed_components;
#[cfg(feature = "component-model")]
pub mod component_composition;

// Experimental modules for cutting-edge WebAssembly proposals
// pub mod exceptions;
pub mod simd;
pub mod simd_crypto;
pub mod simd_ml;
pub mod simd_domain;
// pub mod multi_value;

// Development tooling modules for developer experience
// pub mod module_analyzer;
pub mod hot_reload;
// pub mod debug_server;

// Debugging support module
pub mod debug;

// WebAssembly GC implementation
pub mod gc_types;
pub mod gc_heap;
pub mod gc_operations;
pub mod gc;

// Re-export core types for convenience
pub use engine::{Engine, EngineBuilder, WasmFeature};
pub use error::{WasmtimeError, WasmtimeResult, ErrorCode};
pub use module::{Module, ModuleMetadata, ModuleValueType, ImportKind, ExportKind, FunctionSignature};
pub use store::{Store, StoreBuilder, StoreData, StoreMetadata, ResourceLimits, ExecutionState, MemoryUsage};
pub use instance::{Instance, InstanceMetadata, ImportBinding, ExportBinding, WasmValue, ExecutionResult};
pub use hostfunc::{HostFunction, HostFunctionBuilder, HostFunctionCallback, MarshallingResult};
pub use memory::{Memory, MemoryBuilder, MemoryConfig, MemoryMetadata, MemoryUsage as MemUsage, MemoryDataType, MemoryRegistry, MemoryError};
pub use global::{Global, GlobalValue, GlobalMetadata, ReferenceType as GlobalReferenceType};
pub use table::{Table, TableElement, TableMetadata};
pub use linker::{Linker, LinkerMetadata, LinkerConfig, LinkerInstantiationResult, HostFunctionDefinition, ImportDefinition, ImportType};

// Re-export additional core functionality
pub use caller::{CallerContext, CallableWithContext, CallerContextCallback, create_host_function_with_context};
pub use serialization::{
    ModuleSerializer, SerializationConfig, SerializedModule, SerializedModuleMetadata, CacheStatistics
};

// Re-export advanced threading functionality
pub use work_stealing::{
    WorkStealingScheduler, WorkStealingConfig, WorkStealingTask, TaskId, TaskPriority,
    CpuAffinityHint, MemoryLocalityHint, WorkStealingStatistics, LoadBalancer, PerformanceMonitor
};
pub use thread_affinity::{
    ThreadAffinityManager, AffinityConfig, CoreAssignment, ThreadPriority,
    PerformanceHint, CoreAssignmentStrategy, PerformanceCounters
};
pub use lockfree_structures::{
    LockFreeQueue, LockFreeHashTable, WaitFreeRingBuffer, AtomicRefCounter,
    HazardPointerManager, MemoryOrderingOptimizer, AtomicBatch
};
pub use adaptive_scaling::{
    AdaptiveScalingManager, ScalingConfig, WorkloadPredictor, ScalingDecision, ScalingAction
};
pub use sync_primitives::{
    AdvancedRwLock, AdvancedCondvar, AdvancedSemaphore, AdvancedBarrier,
    FairnessPolicy, ThreadPriority as SyncThreadPriority, MemoryOrderingOptimizer as SyncMemoryOptimizer
};
pub use thread_profiler::{
    ThreadProfiler, ProfilerConfig, ThreadMonitor, FunctionExecutionTracker,
    MemoryAccessAnalyzer, ContentionAnalyzer, PerformanceDashboard, PerformanceReport
};
pub use memory_coordination::{
    MemoryCoordinator, CoordinatorConfig, SharedMemoryManager, ThreadSafeAllocator,
    AtomicOperationManager, MemoryBarrierManager, GcCoordinator, MemoryAccessTracker
};

// Optional re-exports based on features
#[cfg(feature = "wasi")]
pub use wasi::{
    WasiContext, WasiConfig, EnvironmentPolicy, DirectoryMapping,
    WasiDirPermissions, WasiFilePermissions, StdioConfig, StdioSource, StdioSink,
    WasiExecutionResult, WasiCapabilitiesSummary, WasiFileDescriptorManager,
    WasiFileDescriptor, WasiDirectoryDescriptor, WasiDirectoryEntry, WasiFilestat
};

// Re-export async runtime functionality
pub use async_runtime::{
    MultiRuntimeSystem, WorkStealingPool, AsyncOperationManager,
    AsyncOperationHandle, AsyncOperationType, AsyncOperationStatus,
    ExecutionTask, TaskType, TaskPayload, ExecutionContext, Priority,
    CompilationOptions, GCType, ProgressInfo,
    AsyncCallback, ProgressCallback,
    get_multi_runtime_system, get_work_stealing_pool, get_async_operation_manager,
    get_runtime_handle, compile_module_async, execute_function_parallel,
    instantiate_module_concurrent, execute_parallel_gc
};

// Re-export async operations functionality
pub use async_ops::{
    AsyncOperationsManager, AsyncFileIOOperation, AsyncFileIOType,
    AsyncNetworkConnection, AsyncTimer, AsyncTimerType, AsyncOperationResult
};

// Re-export WASI Preview 2 functionality
pub use wasi_preview2::{
    WasiPreview2Context, WasiPreview2Config, WasiStream, WasiStreamType,
    WasiFuture, WasiFutureType, AsyncWasiOperation, AsyncWasiOperationType
};

// Re-export networking functionality
pub use networking::{
    NetworkManager, NetworkConfig, TcpConnection, UdpSocketWrapper,
    TcpListenerWrapper, HttpConnection, ConnectionStatus, HttpVersion,
    NetworkStats, HttpRequest, HttpResponse
};

// Re-export advanced networking functionality
#[cfg(feature = "advanced-networking")]
pub use advanced_networking::{
    AdvancedNetworkManager, AdvancedNetworkConfig, WebSocketConnection, WebSocketConnectionType,
    Http2Connection, Http2ConnectionType, GrpcConnection, ProtocolNegotiator, SupportedProtocol,
    NetworkPerformanceMonitor, ProtocolMetrics, ConnectionMetrics, ThroughputMetrics,
    AdvancedConnectionPool, TlsConfiguration
};

// Re-export filesystem functionality
pub use filesystem::{
    FileSystemManager, FileSystemConfig, FileHandle, DirectoryHandle,
    EnhancedFileMetadata, FileBasicMetadata, FileExtendedMetadata,
    FileSecurityMetadata, FileType, DirectoryEntry, FileSystemStats
};

// Re-export filesystem snapshot functionality
pub use filesystem_snapshots::{
    FilesystemSnapshotManager, SnapshotConfig, SnapshotOptions, RestoreOptions,
    ValidationOptions, Snapshot, SnapshotType, SnapshotMetadata, SnapshotEntry,
    SnapshotStatus, ValidationResult as SnapshotValidationResult, SnapshotMetrics, PerformanceMetrics
};

// Re-export process functionality
pub use process::{
    ProcessManager, ProcessConfig, ProcessHandle, ProcessStatus,
    ProcessResourceUsage, ProcessStdioConfig, EnvironmentInheritance,
    ProcessPriority, ProcessSignal, ProcessSpawnOptions, EnvironmentOperation,
    ProcessStats
};

// Re-export WASI security functionality
pub use wasi_security::{
    WasiSecurityManager, WasiSecurityConfig, WasiCapability, WasiCapabilitySet,
    WasiSecurityPolicy, WasiSecuritySession, SecurityAction, PathPattern,
    NetworkPattern, EnvironmentPattern, ProcessPattern, WasiSecurityStats
};

// Component model re-exports
#[cfg(feature = "component-model")]
pub use component::{
    ComponentEngine, Component, ComponentMetadata, ComponentStoreData,
    InterfaceDefinition, FunctionDefinition, Parameter, TypeDefinition, ResourceDefinition,
    ComponentValueType, ComponentTypeKind, FieldType, CaseType,
    ResourceManager, HostInterface, InstanceInfo
};

#[cfg(feature = "component-model")]
pub use component_core::{
    EnhancedComponentEngine, ComponentInstanceInfo, ComponentMetrics,
    ComponentInterface, ComponentFunction
};

#[cfg(feature = "component-model")]
pub use wit_interfaces::{
    WitInterfaceManager, WitInterface, WitMethod, WitParameter, WitType,
    WitTypeKind, PrimitiveType, CompositeType, ValidationResult, ValidationStatus
};

#[cfg(feature = "component-model")]
pub use component_orchestration::{
    ComponentOrchestrator, ComponentGraph, ComponentNode, ComponentConfiguration,
    ManagedComponent, ComponentState, HealthStatus, ComponentChannel,
    ComponentMessage, MessagePayload, LoadBalancingStrategy
};

#[cfg(feature = "component-model")]
pub use component_resources::{
    ComponentResourceManager, ManagedResource, ResourceHandle, ResourcePermissions,
    ResourceState, AccessType, ResourceQuotas, UsageTracking
};

#[cfg(feature = "component-model")]
pub use distributed_components::{
    DistributedComponentManager, ComponentDiscoveryService, ComponentAdvertisement,
    NodeInfo, NodeCapabilities, SecureCommunicationManager, DistributedSyncService,
    ComponentBackupService, NetworkTopologyManager
};

#[cfg(feature = "component-model")]
pub use component_composition::{
    ComponentCompositionManager, ComponentDependencyGraph, GraphNode, GraphEdge,
    CompositionEngine, DependencyInjectionContainer, DependencyGraphAnalyzer,
    ComponentHierarchyManager, RuntimeComposer, CompositionOptimizer,
    CompositionSpecification, CompositionResult, ComposedApplication,
    GraphAnalysisResult, OptimizationGoals, OptimizationResults
};
//
// pub use type_introspection::{
//     IntrospectionValueType, MemoryTypeInfo, TableTypeInfo, GlobalTypeInfo, FuncTypeInfo,
//     TypeKind, ImportDescriptorInfo, ExportDescriptorInfo,
//     ModuleTypeIntrospector, InstanceTypeIntrospector
// };
//
// pub use async_runtime::{
//     AsyncOperation, AsyncOperationType, AsyncOperationStatus,
//     AsyncFunctionCallContext, AsyncCompilationContext, CompilationOptions,
//     AsyncCallback, ProgressCallback,
//     get_async_runtime, get_runtime_handle,
//     execute_async_function_call, compile_module_async,
//     cancel_async_operation, get_operation_status, wait_for_operation
// };
//
// pub use security::{
//     ModuleSigner, ModuleVerifier, ModuleSignature, SignatureAlgorithm,
//     TrustStore, CertificateInfo, SecurityPolicy
// };
//
// pub use sandbox::{
//     SandboxManager, SandboxConfig, SandboxConfigBuilder, SecurityContext,
//     SandboxedInstance, Capability, FilePermissions, ResourceLimits as SandboxResourceLimits,
//     ResourceUsage, CapabilityAuditor, AuditEntry
// };

// Disabled re-exports for modules not currently compiling
// pub use access_control::{
//     AuthorizationEngine, RbacEngine, AbacEngine, UserIdentity, Role, Permission,
//     AccessRequest, AuthorizationDecision, SessionToken, SessionManager,
//     AbacPolicy, AbacCondition, CombiningAlgorithm
// };
//
// pub use audit::{
//     AuditLogger, AuditLoggerConfig, AuditEvent, AuditEventType, AuditSeverity,
//     ComplianceReport, ComplianceReportConfig, ComplianceFramework,
//     EventCorrelator, AlertManager, Alert
// };
//
// pub use exceptions::{
//     ExceptionHandler, ExceptionTag, ExceptionPayload, ExceptionHandlingConfig,
//     UnwindContext
// };
//
// pub use simd::{
//     SIMDOperations, SIMDConfig, V128 as SIMDVector
// };
//
// pub use multi_value::{
//     MultiValueFunction, MultiValueSignature, MultiValueResult, MultiValueConfig,
//     MultiValueHostFunction, ClosureHostFunction
// };

// Re-export WebAssembly GC types for garbage collection support
pub use gc::{
    WasmGcRuntime, StructOperationResult, ArrayOperationResult, RefOperationResult, WasmtimeGcRef
};

// Re-export source map integration functionality
pub use sourcemap::{
    SourceMapIntegration, SourceMapParser, DwarfParser, SymbolResolver, StackTraceMapper,
    SourceFileCache, ValidationEngine, SourcePosition, WasmAddress, FunctionSymbol,
    VariableSymbol, SourceMappedFrame, SourceMap, DwarfInfo, ValidationResult as SourceMapValidationResult
};
pub use gc_types::{
    GcReferenceType, StructTypeDefinition, ArrayTypeDefinition, FieldDefinition, FieldType as GcFieldType,
    GcObject, GcValue, GcTypeRegistry, GcTypeConverter
};
pub use gc_heap::{
    GcHeap, GcHeapConfig, GcHeapStats, GcObjectEntry, ObjectId, Generation,
    GcCollectionResult, GcWeakReference
};

// Re-export shared FFI utilities for interface implementations
pub use shared_ffi::{
    ParameterConverter, ReturnValueConverter,
    FFI_SUCCESS, FFI_ERROR,
    FfiStrategy, FfiOptLevel, FfiWasmFeature,
    BooleanReturnConverter, IntegerReturnConverter, PointerReturnConverter,
    convert_wasm_features, validate_wasm_features,
    validation, error_mapping
};

// Re-export debugging functionality
pub use debug::{
    DebugSession, DebugSessionId, Breakpoint, BreakpointId, BreakpointType,
    ExecutionState as DebugExecutionState, ExecutionStateType, StackFrame, Variable, VariableValue, VariableScope,
    MemoryInspector, MemoryInfo, VariableInspector, ProfilingData, FunctionProfile,
    DebugEventHandler, ExecutionResult, StepType, EvaluationResult,
    create_debug_session, get_debug_session, close_debug_session, get_active_debug_sessions
};

// Re-export enterprise features for production use
pub use pooling_allocator::{
    PoolingAllocator, PoolingAllocatorConfig, PoolStatistics
};
pub use module_cache::{
    ModuleCache, ModuleCacheConfig, CacheStatistics, CacheEntryMetadata
};
pub use profiler::{
    PerformanceProfiler, ProfilerConfig, FunctionProfile, RealTimeMetrics,
    CompilationMetrics, PerformanceDashboard, RegressionDetection, RegressionSeverity
};
pub use resource_manager::{
    ResourceManager as ProductionResourceManager, ResourceQuota, ResourceUsage, ResourceViolation,
    ResourceViolationType, ResourceAction, ResourceManagerStatistics
};
pub use error_recovery::{
    ErrorRecoverySystem, ErrorCategory, ErrorSeverity, ErrorEvent,
    RecoveryAction, RetryStrategy, RecoveryStatistics, ChaosConfig
};

// Disabled development tooling re-exports
// pub use module_analyzer::{
//     ModuleAnalyzer, FunctionInfo, ImportInfo, ExportInfo, MemoryInfo, TableInfo, GlobalInfo,
//     SecurityAnalysis, PerformanceAnalysis, SizeAnalysis
// };

// Re-export hot-reload functionality
pub use hot_reload::{
    HotReloadManager, HotReloadConfig, SwapStrategy, LoadRequest, LoadPriority, ValidationConfig,
    ComponentVersion, SwapOperation, SwapStatus, TrafficStats, RollbackPlan, RollbackTrigger,
    HealthCheckResult, ComponentStateSnapshot, HotReloadMetrics, BackgroundComponentLoader
};

// Re-export platform optimization functionality
pub use numa_topology::{
    AdvancedNumaTopology, NumaBindingStrategy, WorkloadType, MemoryAllocationPolicy,
    CpuAffinityPolicy, NumaLoadBalancing, MigrationPolicies
};
pub use cpu_cache_management::{
    CachePartitioningManager, CachePartition, CacheAffinityPolicy, PartitionType,
    AffinityStrategy, CacheTopology, InterferenceDetectionAlgorithm
};
pub use memory_bandwidth_optimization::{
    MemoryBandwidthOptimizer, BandwidthAllocationPolicy, CacheAwareAllocationStrategy,
    MemoryTopology, BandwidthOptimizationReport, OptimizationRecommendation
};
pub use cpu_microarchitecture_detection::{
    CpuMicroarchitectureDetector, ArchitectureInfo, FeatureDetectionResults,
    PerformanceCharacteristics, X86FeatureSet, ArmFeatureSet, RiscVFeatureSet
};
pub use platform_config::{
    PlatformConfig, PlatformInfo, CpuOptimization, MemoryHierarchyConfig, NumaConfig,
    ThreadAffinityConfig, PowerManagementConfig, Architecture, OsType
};

// pub use debug_server::{
//     DebugServer, DebugSession, Breakpoint, ExecutionContext, StackFrame,
//     VariableValue, WatchResult, ExecutionState as DebugExecutionState, DebugEvent
// };

/// Library version information
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

/// Wasmtime version this library is built against
pub const WASMTIME_VERSION: &str = "36.0.2";

/// Initialize the native library
///
/// This function should be called once before using any other functionality.
/// It sets up logging and performs any necessary one-time initialization.
///
/// # Safety
///
/// This function is safe to call multiple times, but should be called before
/// any other library functions are used.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_init() -> i32 {
    env_logger::try_init().unwrap_or(());
    log::info!("Wasmtime4j native library initialized (version {})", VERSION);
    0 // Success
}

/// Get the library version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_version() -> *const c_char {
    VERSION.as_ptr() as *const c_char
}

/// Get the Wasmtime version as a C string
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the program and should not be freed.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasmtime_version() -> *const c_char {
    WASMTIME_VERSION.as_ptr() as *const c_char
}

/// Cleanup and shutdown the native library
///
/// This function performs any necessary cleanup before the library is unloaded.
///
/// # Safety
///
/// This function should only be called once, typically during library unload.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_shutdown() -> i32 {
    log::info!("Wasmtime4j native library shutting down");
    0 // Success
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_init() {
        unsafe {
            assert_eq!(wasmtime4j_init(), 0);
        }
    }

    #[test]
    fn test_version() {
        unsafe {
            let version_ptr = wasmtime4j_version();
            assert!(!version_ptr.is_null());
        }
    }

    #[test]
    fn test_wasmtime_version() {
        unsafe {
            let version_ptr = wasmtime4j_wasmtime_version();
            assert!(!version_ptr.is_null());
        }
    }

    #[test]
    fn test_shutdown() {
        unsafe {
            assert_eq!(wasmtime4j_shutdown(), 0);
        }
    }
}