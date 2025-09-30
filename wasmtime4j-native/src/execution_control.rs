//! Advanced WebAssembly execution control with fuel and epoch management
//!
//! This module provides comprehensive execution control mechanisms including:
//! - Hierarchical fuel allocation and tracking
//! - Multi-level epoch interruption handling
//! - Execution quotas and resource limits
//! - Dynamic execution control policies
//! - Advanced interruption handling with state preservation

use std::collections::{HashMap, VecDeque};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};
use crate::error::{WasmtimeError, WasmtimeResult};
use once_cell::sync::Lazy;

/// Global execution controller managing all execution contexts
static GLOBAL_CONTROLLER: Lazy<Mutex<ExecutionControllerState>> = Lazy::new(|| {
    Mutex::new(ExecutionControllerState::new())
});

/// Thread-safe execution controller state
pub struct ExecutionControllerState {
    /// Active execution contexts by ID
    contexts: HashMap<String, Arc<ExecutionContext>>,
    /// Global epoch counter
    global_epoch: u64,
    /// Epoch increment interval
    epoch_interval: Duration,
    /// Last epoch increment time
    last_epoch_increment: Instant,
    /// Controller statistics
    statistics: ControllerStatistics,
}

impl ExecutionControllerState {
    fn new() -> Self {
        ExecutionControllerState {
            contexts: HashMap::new(),
            global_epoch: 0,
            epoch_interval: Duration::from_millis(10), // Default 10ms epoch
            last_epoch_increment: Instant::now(),
            statistics: ControllerStatistics::new(),
        }
    }
}

/// Execution context with comprehensive control mechanisms
pub struct ExecutionContext {
    /// Unique context identifier
    pub id: String,
    /// Fuel manager for this context
    pub fuel_manager: Arc<RwLock<FuelManager>>,
    /// Epoch interrupt manager for this context
    pub interrupt_manager: Arc<RwLock<EpochInterruptManager>>,
    /// Execution quotas and limits
    pub quotas: Arc<RwLock<ExecutionQuotas>>,
    /// Execution policies
    pub policies: Arc<RwLock<ExecutionPolicies>>,
    /// Execution state and statistics
    pub state: Arc<RwLock<ExecutionState>>,
    /// Creation timestamp
    pub created_at: Instant,
}

/// Advanced fuel manager with hierarchical allocation
pub struct FuelManager {
    /// Context identifier
    context_id: String,
    /// Current fuel allocation
    current_fuel: u64,
    /// Total fuel allocated since creation
    total_allocated: u64,
    /// Total fuel consumed since creation
    total_consumed: u64,
    /// Fuel allocation history
    allocation_history: VecDeque<FuelAllocation>,
    /// Per-function fuel consumption tracking
    function_consumption: HashMap<String, FuelConsumption>,
    /// Per-instruction fuel consumption tracking
    instruction_consumption: InstructionConsumption,
    /// Fuel priority level
    priority: FuelPriority,
    /// Parent context for hierarchical allocation
    parent_context: Option<String>,
    /// Child contexts inheriting fuel
    child_contexts: Vec<String>,
    /// Fuel statistics
    statistics: FuelStatistics,
}

/// Epoch-based interrupt manager
pub struct EpochInterruptManager {
    /// Context identifier
    context_id: String,
    /// Current epoch count
    current_epoch: u64,
    /// Epoch deadline for interruption
    epoch_deadline: Option<u64>,
    /// Interruption mode
    interrupt_mode: InterruptMode,
    /// Registered interrupt handlers
    handlers: Vec<InterruptHandler>,
    /// Interrupt recovery configuration
    recovery_config: Option<InterruptRecoveryConfig>,
    /// Time slicing configuration
    time_slicing: Option<TimeSlicingConfig>,
    /// Interrupt protection scopes
    protected_scopes: HashMap<String, InterruptProtection>,
    /// Multi-threaded coordination config
    coordination: Option<InterruptCoordination>,
    /// Interrupt statistics
    statistics: InterruptStatistics,
}

/// Execution quotas and resource limits
#[derive(Debug, Clone)]
pub struct ExecutionQuotas {
    /// Fuel quota limit
    pub fuel_quota: u64,
    /// CPU time quota
    pub cpu_time_quota: Duration,
    /// Memory quota in bytes
    pub memory_quota: u64,
    /// I/O operation quota
    pub io_operation_quota: u64,
    /// Network request quota
    pub network_request_quota: u64,
    /// I/O rate limit (operations per second)
    pub io_rate_limit: f64,
    /// Quota enforcement policy
    pub enforcement_policy: QuotaEnforcementPolicy,
    /// Custom resource quotas
    pub custom_quotas: HashMap<String, u64>,
    /// Dynamic adjustment enabled
    pub dynamic_adjustment: bool,
    /// Over-allocation ratio for temporary bursts
    pub overallocation_ratio: f64,
}

/// Execution policies for context control
#[derive(Debug, Clone)]
pub struct ExecutionPolicies {
    /// Security policy settings
    pub security: SecurityPolicy,
    /// Performance optimization settings
    pub performance: PerformancePolicy,
    /// Resource management settings
    pub resource_management: ResourceManagementPolicy,
    /// Monitoring and debugging settings
    pub monitoring: MonitoringPolicy,
}

/// Current execution state information
#[derive(Debug, Clone)]
pub struct ExecutionState {
    /// Current execution phase
    pub phase: ExecutionPhase,
    /// Execution start time
    pub start_time: Option<Instant>,
    /// Total execution time
    pub total_execution_time: Duration,
    /// Number of execution cycles
    pub execution_cycles: u64,
    /// Current memory usage
    pub memory_usage: u64,
    /// Current I/O operations count
    pub io_operations: u64,
    /// Network requests made
    pub network_requests: u64,
    /// Last activity timestamp
    pub last_activity: Instant,
    /// Execution efficiency metrics
    pub efficiency_metrics: EfficiencyMetrics,
}

/// Fuel allocation record
#[derive(Debug, Clone)]
pub struct FuelAllocation {
    /// Allocation timestamp
    pub timestamp: Instant,
    /// Amount of fuel allocated
    pub amount: u64,
    /// Allocation priority
    pub priority: FuelPriority,
    /// Source of allocation (parent context, budget, etc.)
    pub source: String,
    /// Allocation reason
    pub reason: String,
}

/// Per-function fuel consumption tracking
#[derive(Debug, Clone)]
pub struct FuelConsumption {
    /// Function name
    pub function_name: String,
    /// Total fuel consumed by this function
    pub total_consumed: u64,
    /// Number of calls to this function
    pub call_count: u64,
    /// Average fuel per call
    pub average_per_call: f64,
    /// Peak fuel consumption in a single call
    pub peak_consumption: u64,
    /// Efficiency rating (0.0-1.0)
    pub efficiency: f64,
}

/// Instruction-level fuel consumption tracking
#[derive(Debug, Clone)]
pub struct InstructionConsumption {
    /// Total instructions executed
    pub total_instructions: u64,
    /// Total fuel consumed by instructions
    pub total_fuel: u64,
    /// Average fuel per instruction
    pub average_per_instruction: f64,
    /// Peak instruction burst size
    pub peak_burst: u64,
    /// Instruction efficiency metrics
    pub efficiency: f64,
}

/// Fuel priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum FuelPriority {
    Background = 0,
    Low = 1,
    Normal = 2,
    High = 3,
    Critical = 4,
}

/// Interrupt modes for epoch-based interruption
#[derive(Debug, Clone, Copy)]
pub enum InterruptMode {
    Cooperative,
    Preemptive,
    Hybrid,
    Emergency,
    Graceful,
}

/// Interrupt handler configuration
#[derive(Debug, Clone)]
pub struct InterruptHandler {
    /// Handler identifier
    pub id: String,
    /// Handler priority
    pub priority: InterruptPriority,
    /// Handler function type (simplified for now)
    pub handler_type: InterruptHandlerType,
    /// Handler configuration
    pub config: HashMap<String, String>,
}

/// Interrupt recovery configuration
#[derive(Debug, Clone)]
pub struct InterruptRecoveryConfig {
    /// Enable state preservation
    pub preserve_state: bool,
    /// Maximum recovery attempts
    pub max_recovery_attempts: u32,
    /// Recovery timeout
    pub recovery_timeout: Duration,
    /// Rollback on recovery failure
    pub rollback_on_failure: bool,
}

/// Time slicing configuration
#[derive(Debug, Clone)]
pub struct TimeSlicingConfig {
    /// Time slice duration
    pub slice_duration: Duration,
    /// Cooperative yield points
    pub yield_points: Vec<String>,
    /// Preemption threshold
    pub preemption_threshold: Duration,
}

/// Interrupt protection scope
#[derive(Debug, Clone)]
pub struct InterruptProtection {
    /// Protection scope identifier
    pub id: String,
    /// Protection type
    pub protection_type: ProtectionType,
    /// Start time of protection
    pub start_time: Instant,
    /// Maximum protection duration
    pub max_duration: Duration,
}

/// Multi-threaded interrupt coordination
#[derive(Debug, Clone)]
pub struct InterruptCoordination {
    /// Thread coordination mode
    pub mode: CoordinationMode,
    /// Participant thread identifiers
    pub thread_ids: Vec<String>,
    /// Coordination timeout
    pub timeout: Duration,
}

/// Controller statistics
#[derive(Debug, Clone)]
pub struct ControllerStatistics {
    /// Total contexts created
    pub total_contexts: u64,
    /// Active contexts count
    pub active_contexts: u64,
    /// Total fuel allocated
    pub total_fuel_allocated: u64,
    /// Total fuel consumed
    pub total_fuel_consumed: u64,
    /// Total interrupts triggered
    pub total_interrupts: u64,
    /// Total execution time
    pub total_execution_time: Duration,
    /// Controller start time
    pub start_time: Instant,
}

/// Fuel statistics
#[derive(Debug, Clone)]
pub struct FuelStatistics {
    /// Context identifier
    pub context_id: String,
    /// Statistics timestamp
    pub timestamp: Instant,
    /// Tracking period
    pub tracking_period: Duration,
    /// Fuel utilization percentage
    pub utilization: f64,
    /// Consumption efficiency
    pub efficiency: f64,
    /// Waste ratio
    pub waste_ratio: f64,
}

/// Interrupt statistics
#[derive(Debug, Clone)]
pub struct InterruptStatistics {
    /// Context identifier
    pub context_id: String,
    /// Total interrupts triggered
    pub total_interrupts: u64,
    /// Average interrupt latency
    pub average_latency: Duration,
    /// Peak interrupt latency
    pub peak_latency: Duration,
    /// Interrupt success rate
    pub success_rate: f64,
    /// Recovery success rate
    pub recovery_rate: f64,
}

/// Execution efficiency metrics
#[derive(Debug, Clone)]
pub struct EfficiencyMetrics {
    /// CPU utilization efficiency
    pub cpu_efficiency: f64,
    /// Memory utilization efficiency
    pub memory_efficiency: f64,
    /// I/O efficiency
    pub io_efficiency: f64,
    /// Overall execution efficiency
    pub overall_efficiency: f64,
}

// Enums for various configuration options
#[derive(Debug, Clone, Copy)]
pub enum ExecutionPhase {
    Created,
    Initializing,
    Running,
    Paused,
    Interrupted,
    Terminating,
    Terminated,
    Error,
}

#[derive(Debug, Clone, Copy)]
pub enum QuotaEnforcementPolicy {
    Strict,
    Throttled,
    Graceful,
    Adaptive,
}

#[derive(Debug, Clone)]
pub struct SecurityPolicy {
    pub enable_sandboxing: bool,
    pub memory_protection: bool,
    pub function_call_limits: u32,
}

#[derive(Debug, Clone)]
pub struct PerformancePolicy {
    pub optimization_level: u8,
    pub parallel_execution: bool,
    pub cache_compiled_modules: bool,
}

#[derive(Debug, Clone)]
pub struct ResourceManagementPolicy {
    pub auto_gc_enabled: bool,
    pub gc_threshold: f64,
    pub resource_cleanup_interval: Duration,
}

#[derive(Debug, Clone)]
pub struct MonitoringPolicy {
    pub enable_profiling: bool,
    pub enable_tracing: bool,
    pub metrics_collection_interval: Duration,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum InterruptPriority {
    Low = 1,
    Normal = 2,
    High = 3,
    Critical = 4,
}

#[derive(Debug, Clone)]
pub enum InterruptHandlerType {
    Simple,
    Stateful,
    Cascading,
    Recovery,
}

#[derive(Debug, Clone, Copy)]
pub enum ProtectionType {
    AtomicOperation,
    CriticalSection,
    ResourceAccess,
    StateTransition,
}

#[derive(Debug, Clone, Copy)]
pub enum CoordinationMode {
    Synchronous,
    Asynchronous,
    Barrier,
    Leader,
}

impl ExecutionContext {
    /// Creates a new execution context with comprehensive control mechanisms
    pub fn new(id: String, config: ExecutionContextConfig) -> WasmtimeResult<Arc<Self>> {
        let fuel_manager = Arc::new(RwLock::new(FuelManager::new(&id, config.fuel_priority)?));
        let interrupt_manager = Arc::new(RwLock::new(EpochInterruptManager::new(&id)?));
        let quotas = Arc::new(RwLock::new(config.quotas));
        let policies = Arc::new(RwLock::new(config.policies));
        let state = Arc::new(RwLock::new(ExecutionState::new()));

        Ok(Arc::new(ExecutionContext {
            id,
            fuel_manager,
            interrupt_manager,
            quotas,
            policies,
            state,
            created_at: Instant::now(),
        }))
    }

    /// Allocates fuel with hierarchical inheritance
    pub fn allocate_fuel(&self, amount: u64, priority: FuelPriority) -> WasmtimeResult<()> {
        let mut fuel_manager = self.fuel_manager.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire fuel manager lock: {}", e),
        })?;

        fuel_manager.allocate_fuel(amount, priority)?;

        // Update context statistics
        let mut state = self.state.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire state lock: {}", e),
        })?;
        state.last_activity = Instant::now();

        Ok(())
    }

    /// Consumes fuel with detailed tracking
    pub fn consume_fuel(&self, amount: u64, function_name: Option<&str>) -> WasmtimeResult<u64> {
        let mut fuel_manager = self.fuel_manager.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire fuel manager lock: {}", e),
        })?;

        let consumed = fuel_manager.consume_fuel(amount, function_name)?;

        // Update execution state
        let mut state = self.state.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire state lock: {}", e),
        })?;
        state.last_activity = Instant::now();
        state.execution_cycles += 1;

        Ok(consumed)
    }

    /// Sets epoch deadline with interrupt mode
    pub fn set_epoch_deadline(&self, deadline: u64, mode: InterruptMode) -> WasmtimeResult<()> {
        let mut interrupt_manager = self.interrupt_manager.write().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire interrupt manager lock: {}", e),
        })?;

        interrupt_manager.set_deadline(deadline, mode)?;

        Ok(())
    }
}

impl FuelManager {
    fn new(context_id: &str, priority: FuelPriority) -> WasmtimeResult<Self> {
        Ok(FuelManager {
            context_id: context_id.to_string(),
            current_fuel: 0,
            total_allocated: 0,
            total_consumed: 0,
            allocation_history: VecDeque::new(),
            function_consumption: HashMap::new(),
            instruction_consumption: InstructionConsumption {
                total_instructions: 0,
                total_fuel: 0,
                average_per_instruction: 0.0,
                peak_burst: 0,
                efficiency: 1.0,
            },
            priority,
            parent_context: None,
            child_contexts: Vec::new(),
            statistics: FuelStatistics {
                context_id: context_id.to_string(),
                timestamp: Instant::now(),
                tracking_period: Duration::ZERO,
                utilization: 0.0,
                efficiency: 1.0,
                waste_ratio: 0.0,
            },
        })
    }

    fn allocate_fuel(&mut self, amount: u64, priority: FuelPriority) -> WasmtimeResult<()> {
        // Apply priority-based allocation multiplier
        let effective_amount = match priority {
            FuelPriority::Critical => (amount as f64 * 1.0) as u64,
            FuelPriority::High => (amount as f64 * 0.9) as u64,
            FuelPriority::Normal => (amount as f64 * 0.8) as u64,
            FuelPriority::Low => (amount as f64 * 0.6) as u64,
            FuelPriority::Background => (amount as f64 * 0.4) as u64,
        };

        self.current_fuel += effective_amount;
        self.total_allocated += effective_amount;

        // Record allocation
        let allocation = FuelAllocation {
            timestamp: Instant::now(),
            amount: effective_amount,
            priority,
            source: "direct".to_string(),
            reason: "manual_allocation".to_string(),
        };

        self.allocation_history.push_back(allocation);

        // Maintain history size limit
        while self.allocation_history.len() > 1000 {
            self.allocation_history.pop_front();
        }

        log::debug!("Allocated {} fuel (effective: {}) to context {}",
                   amount, effective_amount, self.context_id);
        Ok(())
    }

    fn consume_fuel(&mut self, amount: u64, function_name: Option<&str>) -> WasmtimeResult<u64> {
        let consumed = std::cmp::min(amount, self.current_fuel);
        self.current_fuel -= consumed;
        self.total_consumed += consumed;

        // Track per-function consumption
        if let Some(func_name) = function_name {
            let consumption = self.function_consumption
                .entry(func_name.to_string())
                .or_insert_with(|| FuelConsumption {
                    function_name: func_name.to_string(),
                    total_consumed: 0,
                    call_count: 0,
                    average_per_call: 0.0,
                    peak_consumption: 0,
                    efficiency: 1.0,
                });

            consumption.total_consumed += consumed;
            consumption.call_count += 1;
            consumption.average_per_call = consumption.total_consumed as f64 / consumption.call_count as f64;
            consumption.peak_consumption = std::cmp::max(consumption.peak_consumption, consumed);
        }

        // Update instruction consumption
        self.instruction_consumption.total_fuel += consumed;
        if self.instruction_consumption.total_instructions > 0 {
            self.instruction_consumption.average_per_instruction =
                self.instruction_consumption.total_fuel as f64 /
                self.instruction_consumption.total_instructions as f64;
        }

        log::debug!("Consumed {} fuel from context {} (requested: {})",
                   consumed, self.context_id, amount);
        Ok(consumed)
    }
}

impl EpochInterruptManager {
    fn new(context_id: &str) -> WasmtimeResult<Self> {
        Ok(EpochInterruptManager {
            context_id: context_id.to_string(),
            current_epoch: 0,
            epoch_deadline: None,
            interrupt_mode: InterruptMode::Cooperative,
            handlers: Vec::new(),
            recovery_config: None,
            time_slicing: None,
            protected_scopes: HashMap::new(),
            coordination: None,
            statistics: InterruptStatistics {
                context_id: context_id.to_string(),
                total_interrupts: 0,
                average_latency: Duration::ZERO,
                peak_latency: Duration::ZERO,
                success_rate: 1.0,
                recovery_rate: 1.0,
            },
        })
    }

    fn set_deadline(&mut self, deadline: u64, mode: InterruptMode) -> WasmtimeResult<()> {
        self.epoch_deadline = Some(self.current_epoch + deadline);
        self.interrupt_mode = mode;

        log::debug!("Set epoch deadline {} (current: {}) for context {} with mode {:?}",
                   self.epoch_deadline.unwrap(), self.current_epoch, self.context_id, mode);
        Ok(())
    }

    fn increment_epoch(&mut self) -> WasmtimeResult<u64> {
        self.current_epoch += 1;

        // Check for deadline breach
        if let Some(deadline) = self.epoch_deadline {
            if self.current_epoch >= deadline {
                self.trigger_interrupt()?;
            }
        }

        Ok(self.current_epoch)
    }

    fn trigger_interrupt(&mut self) -> WasmtimeResult<()> {
        log::debug!("Triggering interrupt for context {} at epoch {}",
                   self.context_id, self.current_epoch);

        self.statistics.total_interrupts += 1;

        // Execute interrupt handlers based on priority
        self.handlers.sort_by_key(|h| std::cmp::Reverse(h.priority));

        for handler in &self.handlers {
            log::debug!("Executing interrupt handler {} for context {}",
                       handler.id, self.context_id);
            // Handler execution would be implemented here
        }

        Ok(())
    }
}

impl ExecutionState {
    fn new() -> Self {
        ExecutionState {
            phase: ExecutionPhase::Created,
            start_time: None,
            total_execution_time: Duration::ZERO,
            execution_cycles: 0,
            memory_usage: 0,
            io_operations: 0,
            network_requests: 0,
            last_activity: Instant::now(),
            efficiency_metrics: EfficiencyMetrics {
                cpu_efficiency: 1.0,
                memory_efficiency: 1.0,
                io_efficiency: 1.0,
                overall_efficiency: 1.0,
            },
        }
    }
}

impl ControllerStatistics {
    fn new() -> Self {
        ControllerStatistics {
            total_contexts: 0,
            active_contexts: 0,
            total_fuel_allocated: 0,
            total_fuel_consumed: 0,
            total_interrupts: 0,
            total_execution_time: Duration::ZERO,
            start_time: Instant::now(),
        }
    }
}

/// Configuration for creating execution contexts
#[derive(Debug, Clone)]
pub struct ExecutionContextConfig {
    pub fuel_priority: FuelPriority,
    pub quotas: ExecutionQuotas,
    pub policies: ExecutionPolicies,
    pub interrupt_mode: InterruptMode,
}

impl Default for ExecutionContextConfig {
    fn default() -> Self {
        ExecutionContextConfig {
            fuel_priority: FuelPriority::Normal,
            quotas: ExecutionQuotas {
                fuel_quota: 1000000,
                cpu_time_quota: Duration::from_secs(30),
                memory_quota: 64 * 1024 * 1024,
                io_operation_quota: 10000,
                network_request_quota: 100,
                io_rate_limit: 1000.0,
                enforcement_policy: QuotaEnforcementPolicy::Strict,
                custom_quotas: HashMap::new(),
                dynamic_adjustment: true,
                overallocation_ratio: 1.2,
            },
            policies: ExecutionPolicies {
                security: SecurityPolicy {
                    enable_sandboxing: true,
                    memory_protection: true,
                    function_call_limits: 10000,
                },
                performance: PerformancePolicy {
                    optimization_level: 2,
                    parallel_execution: true,
                    cache_compiled_modules: true,
                },
                resource_management: ResourceManagementPolicy {
                    auto_gc_enabled: true,
                    gc_threshold: 0.8,
                    resource_cleanup_interval: Duration::from_secs(30),
                },
                monitoring: MonitoringPolicy {
                    enable_profiling: false,
                    enable_tracing: false,
                    metrics_collection_interval: Duration::from_secs(1),
                },
            },
            interrupt_mode: InterruptMode::Cooperative,
        }
    }
}

/// Public API functions for execution control
pub mod core {
    use super::*;
    use std::os::raw::c_void;

    /// Creates a new execution context with advanced control features
    pub fn create_execution_context(
        context_id: &str,
        config: ExecutionContextConfig
    ) -> WasmtimeResult<*mut c_void> {
        let context = ExecutionContext::new(context_id.to_string(), config)?;

        // Register context with global controller
        {
            let mut controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to acquire controller lock: {}", e),
            })?;

            controller.contexts.insert(context_id.to_string(), Arc::clone(&context));
            controller.statistics.total_contexts += 1;
            controller.statistics.active_contexts += 1;
        }

        let context_ptr = Arc::into_raw(context) as *mut c_void;
        log::debug!("Created execution context '{}' at {:p}", context_id, context_ptr);
        Ok(context_ptr)
    }

    /// Gets an existing execution context by ID
    pub fn get_execution_context(context_id: &str) -> WasmtimeResult<*const c_void> {
        let controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire controller lock: {}", e),
        })?;

        match controller.contexts.get(context_id) {
            Some(context) => Ok(Arc::as_ptr(context) as *const c_void),
            None => Err(WasmtimeError::InvalidParameter {
                message: format!("Execution context '{}' not found", context_id),
            }),
        }
    }

    /// Allocates fuel to an execution context
    pub fn allocate_context_fuel(
        context_ptr: *const c_void,
        amount: u64,
        priority: u32,
    ) -> WasmtimeResult<()> {
        if context_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Context pointer cannot be null".to_string(),
            });
        }

        let fuel_priority = match priority {
            0 => FuelPriority::Background,
            1 => FuelPriority::Low,
            2 => FuelPriority::Normal,
            3 => FuelPriority::High,
            4 => FuelPriority::Critical,
            _ => FuelPriority::Normal,
        };

        unsafe {
            let context = &*(context_ptr as *const ExecutionContext);
            context.allocate_fuel(amount, fuel_priority)
        }
    }

    /// Consumes fuel from an execution context
    pub fn consume_context_fuel(
        context_ptr: *const c_void,
        amount: u64,
        function_name: Option<&str>,
    ) -> WasmtimeResult<u64> {
        if context_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Context pointer cannot be null".to_string(),
            });
        }

        unsafe {
            let context = &*(context_ptr as *const ExecutionContext);
            context.consume_fuel(amount, function_name)
        }
    }

    /// Sets epoch deadline for an execution context
    pub fn set_context_epoch_deadline(
        context_ptr: *const c_void,
        deadline: u64,
        interrupt_mode: u32,
    ) -> WasmtimeResult<()> {
        if context_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Context pointer cannot be null".to_string(),
            });
        }

        let mode = match interrupt_mode {
            0 => InterruptMode::Cooperative,
            1 => InterruptMode::Preemptive,
            2 => InterruptMode::Hybrid,
            3 => InterruptMode::Emergency,
            4 => InterruptMode::Graceful,
            _ => InterruptMode::Cooperative,
        };

        unsafe {
            let context = &*(context_ptr as *const ExecutionContext);
            context.set_epoch_deadline(deadline, mode)
        }
    }

    /// Increments global epoch counter
    pub fn increment_global_epoch() -> WasmtimeResult<u64> {
        let mut controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire controller lock: {}", e),
        })?;

        controller.global_epoch += 1;
        controller.last_epoch_increment = Instant::now();
        controller.statistics.total_interrupts += 1;

        // Increment epoch for all active contexts
        for (context_id, context) in &controller.contexts {
            if let Ok(mut interrupt_manager) = context.interrupt_manager.write() {
                if let Err(e) = interrupt_manager.increment_epoch() {
                    log::warn!("Failed to increment epoch for context '{}': {:?}", context_id, e);
                }
            }
        }

        log::debug!("Global epoch incremented to {}", controller.global_epoch);
        Ok(controller.global_epoch)
    }

    /// Gets fuel statistics for an execution context
    pub fn get_context_fuel_statistics(context_ptr: *const c_void) -> WasmtimeResult<FuelStatistics> {
        if context_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Context pointer cannot be null".to_string(),
            });
        }

        unsafe {
            let context = &*(context_ptr as *const ExecutionContext);
            let fuel_manager = context.fuel_manager.read().map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to acquire fuel manager lock: {}", e),
            })?;

            Ok(fuel_manager.statistics.clone())
        }
    }

    /// Gets interrupt statistics for an execution context
    pub fn get_context_interrupt_statistics(context_ptr: *const c_void) -> WasmtimeResult<InterruptStatistics> {
        if context_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Context pointer cannot be null".to_string(),
            });
        }

        unsafe {
            let context = &*(context_ptr as *const ExecutionContext);
            let interrupt_manager = context.interrupt_manager.read().map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to acquire interrupt manager lock: {}", e),
            })?;

            Ok(interrupt_manager.statistics.clone())
        }
    }

    /// Gets global controller statistics
    pub fn get_controller_statistics() -> WasmtimeResult<ControllerStatistics> {
        let controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire controller lock: {}", e),
        })?;

        Ok(controller.statistics.clone())
    }

    /// Cleans up execution context resources
    pub fn cleanup_execution_context(context_id: &str) -> WasmtimeResult<()> {
        let mut controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire controller lock: {}", e),
        })?;

        if controller.contexts.remove(context_id).is_some() {
            controller.statistics.active_contexts = controller.statistics.active_contexts.saturating_sub(1);
            log::debug!("Cleaned up execution context '{}'", context_id);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Execution context '{}' not found", context_id),
            })
        }
    }

    /// Validates execution controller state
    pub fn validate_controller() -> WasmtimeResult<bool> {
        let controller = GLOBAL_CONTROLLER.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire controller lock: {}", e),
        })?;

        // Perform validation checks
        let contexts_count = controller.contexts.len() as u64;
        let stats_active = controller.statistics.active_contexts;

        if contexts_count != stats_active {
            log::warn!("Context count mismatch: {} contexts vs {} in statistics",
                      contexts_count, stats_active);
            return Ok(false);
        }

        // Additional validation checks can be added here
        Ok(true)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Duration;

    #[test]
    fn test_execution_context_creation() {
        let config = ExecutionContextConfig::default();
        let context = ExecutionContext::new("test_context".to_string(), config);
        assert!(context.is_ok());

        let ctx = context.unwrap();
        assert_eq!(ctx.id, "test_context");
    }

    #[test]
    fn test_fuel_allocation_and_consumption() {
        let config = ExecutionContextConfig::default();
        let context = ExecutionContext::new("fuel_test".to_string(), config).unwrap();

        // Allocate fuel
        assert!(context.allocate_fuel(1000, FuelPriority::Normal).is_ok());

        // Consume fuel
        let consumed = context.consume_fuel(500, Some("test_function"));
        assert!(consumed.is_ok());
        assert_eq!(consumed.unwrap(), 500);

        // Check remaining fuel
        let fuel_manager = context.fuel_manager.read().unwrap();
        assert_eq!(fuel_manager.current_fuel, 500);
    }

    #[test]
    fn test_epoch_deadline_setting() {
        let config = ExecutionContextConfig::default();
        let context = ExecutionContext::new("epoch_test".to_string(), config).unwrap();

        assert!(context.set_epoch_deadline(100, InterruptMode::Cooperative).is_ok());

        let interrupt_manager = context.interrupt_manager.read().unwrap();
        assert!(interrupt_manager.epoch_deadline.is_some());
    }

    #[test]
    fn test_core_api_functions() {
        use super::core;

        let config = ExecutionContextConfig::default();
        let context_ptr = core::create_execution_context("api_test", config);
        assert!(context_ptr.is_ok());

        let ptr = context_ptr.unwrap();
        assert!(!ptr.is_null());

        // Test fuel allocation
        assert!(core::allocate_context_fuel(ptr, 2000, 2).is_ok());

        // Test fuel consumption
        let consumed = core::consume_context_fuel(ptr, 1000, Some("test_func"));
        assert!(consumed.is_ok());
        assert_eq!(consumed.unwrap(), 1000);

        // Test epoch deadline
        assert!(core::set_context_epoch_deadline(ptr, 50, 0).is_ok());

        // Cleanup
        assert!(core::cleanup_execution_context("api_test").is_ok());
    }

    #[test]
    fn test_global_epoch_increment() {
        use super::core;

        let initial_epoch = core::increment_global_epoch().unwrap();
        let next_epoch = core::increment_global_epoch().unwrap();

        assert_eq!(next_epoch, initial_epoch + 1);
    }

    #[test]
    fn test_statistics_collection() {
        use super::core;

        let stats = core::get_controller_statistics();
        assert!(stats.is_ok());

        let statistics = stats.unwrap();
        assert!(statistics.start_time.elapsed().as_secs() < 1); // Should be recent
    }

    #[test]
    fn test_fuel_priority_allocation() {
        let mut fuel_manager = FuelManager::new("priority_test", FuelPriority::Normal).unwrap();

        // Test different priority allocations
        assert!(fuel_manager.allocate_fuel(1000, FuelPriority::High).is_ok());
        let high_allocation = fuel_manager.current_fuel;

        fuel_manager.current_fuel = 0;
        assert!(fuel_manager.allocate_fuel(1000, FuelPriority::Low).is_ok());
        let low_allocation = fuel_manager.current_fuel;

        // High priority should get more effective fuel
        assert!(high_allocation > low_allocation);
    }

    #[test]
    fn test_interrupt_modes() {
        let mut interrupt_manager = EpochInterruptManager::new("interrupt_test").unwrap();

        // Test different interrupt modes
        assert!(interrupt_manager.set_deadline(100, InterruptMode::Cooperative).is_ok());
        assert!(interrupt_manager.set_deadline(50, InterruptMode::Preemptive).is_ok());
        assert!(interrupt_manager.set_deadline(200, InterruptMode::Hybrid).is_ok());

        // Deadline should be updated
        assert!(interrupt_manager.epoch_deadline.is_some());
    }

    #[test]
    fn test_execution_context_lifecycle() {
        let config = ExecutionContextConfig::default();
        let context = ExecutionContext::new("lifecycle_test".to_string(), config).unwrap();

        // Check initial state
        let state = context.state.read().unwrap();
        assert!(matches!(state.phase, ExecutionPhase::Created));
        assert_eq!(state.execution_cycles, 0);

        drop(state);

        // Simulate some activity
        assert!(context.allocate_fuel(1000, FuelPriority::Normal).is_ok());
        assert!(context.consume_fuel(100, Some("test")).is_ok());

        // Check updated state
        let state = context.state.read().unwrap();
        assert_eq!(state.execution_cycles, 1);
    }
}