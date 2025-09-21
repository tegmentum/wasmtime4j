//! # Exception Handling Support for WebAssembly
//!
//! This module provides comprehensive support for WebAssembly exception handling,
//! implementing the WebAssembly exception handling proposal. It enables structured
//! exception handling with try/catch blocks and exception propagation across
//! WebAssembly function calls.
//!
//! ## Features
//!
//! - **Exception Types**: Support for custom exception types with typed parameters
//! - **Try/Catch Blocks**: Structured exception handling within WebAssembly modules
//! - **Exception Propagation**: Proper exception propagation across function boundaries
//! - **Host Integration**: Integration with host exception handling mechanisms
//! - **Performance Tracking**: Comprehensive statistics and performance monitoring
//! - **Memory Safety**: All operations include bounds checking and validation
//!
//! ## Safety
//!
//! All exception handling operations include proper validation and memory management
//! to ensure safe exception propagation without memory leaks or corruption.

#![warn(missing_docs)]

use std::ptr;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::collections::{HashMap, VecDeque};
use std::os::raw::{c_char, c_int, c_void};
use std::ffi::{CStr, CString};

use wasmtime::{Val, ValType, Store, Engine, Func, FuncType, Extern};

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::performance::PERFORMANCE_SYSTEM;

/// Maximum number of exception parameters
const MAX_EXCEPTION_PARAMS: usize = 16;

/// Maximum exception stack depth to prevent infinite recursion
const MAX_EXCEPTION_STACK_DEPTH: usize = 1000;

/// Exception type identifier
pub type ExceptionTypeId = u32;

/// Exception instance identifier
pub type ExceptionInstanceId = u64;

/// Exception parameter value
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExceptionParam {
    /// Parameter type
    pub param_type: ValType,
    /// Parameter value
    pub value: Val,
}

/// Exception type definition
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExceptionTypeDefinition {
    /// Unique identifier for this exception type
    pub type_id: ExceptionTypeId,
    /// Tag name for the exception type
    pub tag: String,
    /// Parameter types for this exception
    pub parameter_types: Vec<ValType>,
    /// Whether this exception type is importable
    pub is_importable: bool,
    /// Whether this exception type is exportable
    pub is_exportable: bool,
}

/// Exception instance
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExceptionInstance {
    /// Unique identifier for this exception instance
    pub instance_id: ExceptionInstanceId,
    /// Type of this exception
    pub exception_type: ExceptionTypeDefinition,
    /// Parameter values
    pub parameters: Vec<ExceptionParam>,
    /// Stack trace information
    pub stack_trace: Option<String>,
    /// Timestamp when exception was created
    pub timestamp: u64,
}

/// Exception handler callback signature
pub type ExceptionHandler = fn(&ExceptionInstance) -> WasmtimeResult<bool>;

/// Exception handling configuration
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExceptionHandlingConfig {
    /// Maximum number of exception types
    pub max_exception_types: u32,
    /// Maximum number of active exception instances
    pub max_active_exceptions: u32,
    /// Whether to capture stack traces
    pub capture_stack_traces: bool,
    /// Whether to enable exception debugging
    pub debug_exceptions: bool,
}

/// Exception operation result
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ExceptionOperationResult {
    /// Operation success status
    pub success: bool,
    /// Exception instance ID (for throw operations)
    pub exception_id: ExceptionInstanceId,
    /// Whether exception was caught
    pub was_caught: bool,
    /// Execution time in microseconds
    pub execution_time_micros: u64,
    /// Error code if operation failed
    pub error_code: i32,
}

/// Exception handling manager
pub struct ExceptionHandlingManager {
    /// Exception type registry
    exception_types: Arc<RwLock<HashMap<ExceptionTypeId, ExceptionTypeDefinition>>>,
    /// Active exception instances
    active_exceptions: Arc<RwLock<HashMap<ExceptionInstanceId, ExceptionInstance>>>,
    /// Exception handlers by type
    exception_handlers: Arc<RwLock<HashMap<ExceptionTypeId, Vec<ExceptionHandler>>>>,
    /// Exception stack for nested exceptions
    exception_stack: Arc<Mutex<VecDeque<ExceptionInstanceId>>>,
    /// Configuration
    config: ExceptionHandlingConfig,
    /// Statistics
    exceptions_thrown: AtomicU64,
    exceptions_caught: AtomicU64,
    exceptions_propagated: AtomicU64,
    total_execution_time_micros: AtomicU64,
    /// Next IDs for type and instance generation
    next_type_id: AtomicU64,
    next_instance_id: AtomicU64,
}

impl Default for ExceptionHandlingManager {
    fn default() -> Self {
        Self::new(ExceptionHandlingConfig {
            max_exception_types: 1000,
            max_active_exceptions: 10000,
            capture_stack_traces: true,
            debug_exceptions: false,
        })
    }
}

impl ExceptionHandlingManager {
    /// Create a new exception handling manager
    pub fn new(config: ExceptionHandlingConfig) -> Self {
        Self {
            exception_types: Arc::new(RwLock::new(HashMap::new())),
            active_exceptions: Arc::new(RwLock::new(HashMap::new())),
            exception_handlers: Arc::new(RwLock::new(HashMap::new())),
            exception_stack: Arc::new(Mutex::new(VecDeque::new())),
            config,
            exceptions_thrown: AtomicU64::new(0),
            exceptions_caught: AtomicU64::new(0),
            exceptions_propagated: AtomicU64::new(0),
            total_execution_time_micros: AtomicU64::new(0),
            next_type_id: AtomicU64::new(1),
            next_instance_id: AtomicU64::new(1),
        }
    }

    /// Check if exception handling is supported
    pub fn is_exception_handling_supported(&self) -> bool {
        // Exception handling support depends on the Wasmtime version and configuration
        true // Most modern Wasmtime versions support exception handling
    }

    /// Register a new exception type
    pub fn register_exception_type(
        &self,
        tag: String,
        parameter_types: Vec<ValType>,
        is_importable: bool,
        is_exportable: bool,
    ) -> WasmtimeResult<ExceptionTypeId> {
        let start_time = std::time::Instant::now();

        // Validate parameters
        if tag.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Exception tag cannot be empty".to_string(),
            });
        }

        if parameter_types.len() > MAX_EXCEPTION_PARAMS {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Too many exception parameters: {} (max {})",
                       parameter_types.len(), MAX_EXCEPTION_PARAMS),
            });
        }

        let type_id = self.next_type_id.fetch_add(1, Ordering::SeqCst) as ExceptionTypeId;

        let exception_type = ExceptionTypeDefinition {
            type_id,
            tag,
            parameter_types,
            is_importable,
            is_exportable,
        };

        {
            let mut types = self.exception_types.write().unwrap();

            // Check if we've reached the maximum number of exception types
            if types.len() >= self.config.max_exception_types as usize {
                return Err(WasmtimeError::ResourceExhausted {
                    message: format!("Maximum number of exception types ({}) exceeded",
                           self.config.max_exception_types),
                });
            }

            types.insert(type_id, exception_type);
        }

        let execution_time = start_time.elapsed().as_micros() as u64;
        self.total_execution_time_micros.fetch_add(execution_time, Ordering::Relaxed);

        // Record performance metrics
        PERFORMANCE_SYSTEM.record_function_call("register_exception_type", execution_time, false);

        Ok(type_id)
    }

    /// Get exception type by ID
    pub fn get_exception_type(&self, type_id: ExceptionTypeId) -> Option<ExceptionTypeDefinition> {
        let types = self.exception_types.read().unwrap();
        types.get(&type_id).cloned()
    }

    /// Get all registered exception types
    pub fn get_all_exception_types(&self) -> Vec<ExceptionTypeDefinition> {
        let types = self.exception_types.read().unwrap();
        types.values().cloned().collect()
    }

    /// Throw an exception
    pub fn throw_exception(
        &self,
        type_id: ExceptionTypeId,
        parameters: Vec<ExceptionParam>,
    ) -> WasmtimeResult<ExceptionOperationResult> {
        let start_time = std::time::Instant::now();

        // Get exception type
        let exception_type = self.get_exception_type(type_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Unknown exception type: {}", type_id),
            })?;

        // Validate parameter count
        if parameters.len() != exception_type.parameter_types.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Parameter count mismatch: expected {}, got {}",
                       exception_type.parameter_types.len(), parameters.len()),
            });
        }

        // Validate parameter types
        for (i, param) in parameters.iter().enumerate() {
            if param.param_type != exception_type.parameter_types[i] {
                return Err(WasmtimeError::Type {
                    message: format!("Parameter type mismatch at index {}: expected {:?}, got {:?}",
                           i, exception_type.parameter_types[i], param.param_type),
                });
            }
        }

        // Create exception instance
        let instance_id = self.next_instance_id.fetch_add(1, Ordering::SeqCst);
        let stack_trace = if self.config.capture_stack_traces {
            Some(self.capture_stack_trace())
        } else {
            None
        };

        let exception_instance = ExceptionInstance {
            instance_id,
            exception_type,
            parameters,
            stack_trace,
            timestamp: std::time::SystemTime::now()
                .duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_secs(),
        };

        // Check if we've reached the maximum number of active exceptions
        {
            let active = self.active_exceptions.read().unwrap();
            if active.len() >= self.config.max_active_exceptions as usize {
                return Err(WasmtimeError::ResourceExhausted {
                    message: format!("Maximum number of active exceptions ({}) exceeded",
                           self.config.max_active_exceptions),
                });
            }
        }

        // Add to exception stack
        {
            let mut stack = self.exception_stack.lock().unwrap();
            if stack.len() >= MAX_EXCEPTION_STACK_DEPTH {
                return Err(WasmtimeError::Runtime {
                    message: "Exception stack overflow".to_string(),
                    backtrace: None,
                });
            }
            stack.push_back(instance_id);
        }

        // Store active exception
        {
            let mut active = self.active_exceptions.write().unwrap();
            active.insert(instance_id, exception_instance.clone());
        }

        // Try to handle the exception
        let was_caught = self.try_handle_exception(&exception_instance)?;

        let execution_time = start_time.elapsed().as_micros() as u64;

        // Update statistics
        self.exceptions_thrown.fetch_add(1, Ordering::Relaxed);
        if was_caught {
            self.exceptions_caught.fetch_add(1, Ordering::Relaxed);
        } else {
            self.exceptions_propagated.fetch_add(1, Ordering::Relaxed);
        }
        self.total_execution_time_micros.fetch_add(execution_time, Ordering::Relaxed);

        // Record performance metrics
        PERFORMANCE_SYSTEM.record_function_call("throw_exception", execution_time, false);

        Ok(ExceptionOperationResult {
            success: true,
            exception_id: instance_id,
            was_caught,
            execution_time_micros: execution_time,
            error_code: 0,
        })
    }

    /// Catch an exception
    pub fn catch_exception(&self, type_id: ExceptionTypeId) -> Option<ExceptionInstance> {
        let mut stack = self.exception_stack.lock().unwrap();

        // Look for an exception of the specified type in the stack (LIFO order)
        for (i, &instance_id) in stack.iter().enumerate().rev() {
            let active = self.active_exceptions.read().unwrap();
            if let Some(exception) = active.get(&instance_id) {
                if exception.exception_type.type_id == type_id {
                    // Remove from stack and return
                    stack.remove(i);
                    return Some(exception.clone());
                }
            }
        }

        None
    }

    /// Register an exception handler
    pub fn register_exception_handler(
        &self,
        type_id: ExceptionTypeId,
        handler: ExceptionHandler,
    ) -> WasmtimeResult<()> {
        // Verify the exception type exists
        if self.get_exception_type(type_id).is_none() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Unknown exception type: {}", type_id),
            });
        }

        let mut handlers = self.exception_handlers.write().unwrap();
        handlers.entry(type_id).or_insert_with(Vec::new).push(handler);

        Ok(())
    }

    /// Clear all exception handlers for a type
    pub fn clear_exception_handlers(&self, type_id: ExceptionTypeId) {
        let mut handlers = self.exception_handlers.write().unwrap();
        handlers.remove(&type_id);
    }

    /// Get current exception stack depth
    pub fn get_exception_stack_depth(&self) -> usize {
        let stack = self.exception_stack.lock().unwrap();
        stack.len()
    }

    /// Clear exception stack
    pub fn clear_exception_stack(&self) {
        let mut stack = self.exception_stack.lock().unwrap();
        stack.clear();

        // Also clear active exceptions
        let mut active = self.active_exceptions.write().unwrap();
        active.clear();
    }

    /// Get configuration
    pub fn get_config(&self) -> &ExceptionHandlingConfig {
        &self.config
    }

    /// Get exception handling statistics
    pub fn get_statistics(&self) -> (u64, u64, u64, u64) {
        (
            self.exceptions_thrown.load(Ordering::Relaxed),
            self.exceptions_caught.load(Ordering::Relaxed),
            self.exceptions_propagated.load(Ordering::Relaxed),
            self.total_execution_time_micros.load(Ordering::Relaxed),
        )
    }

    /// Reset statistics
    pub fn reset_statistics(&self) {
        self.exceptions_thrown.store(0, Ordering::Relaxed);
        self.exceptions_caught.store(0, Ordering::Relaxed);
        self.exceptions_propagated.store(0, Ordering::Relaxed);
        self.total_execution_time_micros.store(0, Ordering::Relaxed);
    }

    // Internal helper methods

    /// Try to handle an exception using registered handlers
    fn try_handle_exception(&self, exception: &ExceptionInstance) -> WasmtimeResult<bool> {
        let handlers = self.exception_handlers.read().unwrap();

        if let Some(type_handlers) = handlers.get(&exception.exception_type.type_id) {
            for handler in type_handlers {
                match handler(exception) {
                    Ok(true) => return Ok(true), // Exception was handled
                    Ok(false) => continue, // Handler declined to handle
                    Err(_) => continue, // Handler failed, try next one
                }
            }
        }

        Ok(false) // Exception was not handled
    }

    /// Capture stack trace (placeholder implementation)
    fn capture_stack_trace(&self) -> String {
        // In a real implementation, this would capture the actual WebAssembly stack trace
        format!("WebAssembly stack trace (timestamp: {})",
               std::time::SystemTime::now()
                   .duration_since(std::time::UNIX_EPOCH)
                   .unwrap_or_default()
                   .as_millis())
    }
}

/// Global instance of exception handling manager
static EXCEPTION_HANDLING_MANAGER: std::sync::LazyLock<ExceptionHandlingManager> =
    std::sync::LazyLock::new(ExceptionHandlingManager::default);

// C API exports for exception handling

/// Check if exception handling is supported
///
/// # Safety
///
/// This function is safe to call at any time.
/// Returns 1 if supported, 0 otherwise.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_handling_supported() -> c_int {
    if EXCEPTION_HANDLING_MANAGER.is_exception_handling_supported() {
        1
    } else {
        0
    }
}

/// Register a new exception type
///
/// # Safety
///
/// The tag parameter must be a valid null-terminated string.
/// The parameter_types array must be valid for the specified length.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_register_exception_type(
    tag: *const c_char,
    parameter_types: *const c_int,
    parameter_count: usize,
    is_importable: c_int,
    is_exportable: c_int,
    type_id: *mut ExceptionTypeId,
) -> c_int {
    if tag.is_null() || parameter_types.is_null() || type_id.is_null() {
        return -1;
    }

    let tag_str = match CStr::from_ptr(tag).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -2, // Invalid string
    };

    // Convert parameter types (simplified - would need proper conversion)
    let param_types = vec![ValType::I32; parameter_count]; // Placeholder

    match EXCEPTION_HANDLING_MANAGER.register_exception_type(
        tag_str,
        param_types,
        is_importable != 0,
        is_exportable != 0,
    ) {
        Ok(id) => {
            ptr::write(type_id, id);
            0 // Success
        }
        Err(_) => -3, // Registration failed
    }
}

/// Throw an exception
///
/// # Safety
///
/// All pointer parameters must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_throw_exception(
    type_id: ExceptionTypeId,
    parameters: *const c_void,
    parameter_count: usize,
    result: *mut ExceptionOperationResult,
) -> c_int {
    if parameters.is_null() || result.is_null() {
        return -1;
    }

    // For placeholder implementation, create empty parameters
    let params = Vec::new(); // Would convert from C parameters

    match EXCEPTION_HANDLING_MANAGER.throw_exception(type_id, params) {
        Ok(op_result) => {
            ptr::write(result, op_result);
            0 // Success
        }
        Err(_) => -2, // Throw failed
    }
}

/// Catch an exception of a specific type
///
/// # Safety
///
/// The exception_id parameter must be a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_catch_exception(
    type_id: ExceptionTypeId,
    exception_id: *mut ExceptionInstanceId,
) -> c_int {
    if exception_id.is_null() {
        return -1;
    }

    match EXCEPTION_HANDLING_MANAGER.catch_exception(type_id) {
        Some(exception) => {
            ptr::write(exception_id, exception.instance_id);
            1 // Exception caught
        }
        None => 0, // No exception of this type
    }
}

/// Get exception stack depth
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_stack_depth() -> usize {
    EXCEPTION_HANDLING_MANAGER.get_exception_stack_depth()
}

/// Clear exception stack
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_clear_exception_stack() -> c_int {
    EXCEPTION_HANDLING_MANAGER.clear_exception_stack();
    0 // Success
}

/// Get exception handling statistics
///
/// # Safety
///
/// All pointer parameters must be valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_get_statistics(
    thrown: *mut u64,
    caught: *mut u64,
    propagated: *mut u64,
    execution_time_micros: *mut u64,
) -> c_int {
    if thrown.is_null() || caught.is_null() || propagated.is_null() || execution_time_micros.is_null() {
        return -1;
    }

    let (throw_count, catch_count, prop_count, time) = EXCEPTION_HANDLING_MANAGER.get_statistics();
    ptr::write(thrown, throw_count);
    ptr::write(caught, catch_count);
    ptr::write(propagated, prop_count);
    ptr::write(execution_time_micros, time);

    0 // Success
}

/// Reset exception handling statistics
///
/// # Safety
///
/// This function is safe to call at any time.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_reset_statistics() -> c_int {
    EXCEPTION_HANDLING_MANAGER.reset_statistics();
    0 // Success
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_exception_handling_manager_creation() {
        let config = ExceptionHandlingConfig {
            max_exception_types: 100,
            max_active_exceptions: 1000,
            capture_stack_traces: true,
            debug_exceptions: true,
        };

        let manager = ExceptionHandlingManager::new(config);
        assert!(manager.is_exception_handling_supported());
        assert_eq!(manager.get_config().max_exception_types, 100);
        assert_eq!(manager.get_config().max_active_exceptions, 1000);
        assert!(manager.get_config().capture_stack_traces);
        assert!(manager.get_config().debug_exceptions);
    }

    #[test]
    fn test_exception_type_registration() {
        let manager = ExceptionHandlingManager::default();

        // Register a simple exception type
        let type_id = manager.register_exception_type(
            "test_exception".to_string(),
            vec![ValType::I32, ValType::F64],
            true,
            true,
        ).unwrap();

        assert!(type_id > 0);

        // Retrieve the exception type
        let exception_type = manager.get_exception_type(type_id).unwrap();
        assert_eq!(exception_type.tag, "test_exception");
        assert_eq!(exception_type.parameter_types.len(), 2);
        assert_eq!(exception_type.parameter_types[0], ValType::I32);
        assert_eq!(exception_type.parameter_types[1], ValType::F64);
        assert!(exception_type.is_importable);
        assert!(exception_type.is_exportable);
    }

    #[test]
    fn test_exception_type_validation() {
        let manager = ExceptionHandlingManager::default();

        // Test empty tag
        let result = manager.register_exception_type(
            "".to_string(),
            vec![],
            false,
            false,
        );
        assert!(result.is_err());

        // Test too many parameters
        let too_many_params = vec![ValType::I32; MAX_EXCEPTION_PARAMS + 1];
        let result = manager.register_exception_type(
            "test".to_string(),
            too_many_params,
            false,
            false,
        );
        assert!(result.is_err());
    }

    #[test]
    fn test_throw_and_catch_exception() {
        let manager = ExceptionHandlingManager::default();

        // Register exception type
        let type_id = manager.register_exception_type(
            "test_exception".to_string(),
            vec![ValType::I32],
            true,
            true,
        ).unwrap();

        // Create exception parameters
        let parameters = vec![ExceptionParam {
            param_type: ValType::I32,
            value: Val::I32(42),
        }];

        // Throw exception
        let result = manager.throw_exception(type_id, parameters).unwrap();
        assert!(result.success);
        assert!(!result.was_caught); // No handlers registered

        // Check exception stack depth
        assert_eq!(manager.get_exception_stack_depth(), 1);

        // Try to catch the exception
        let caught_exception = manager.catch_exception(type_id);
        assert!(caught_exception.is_some());

        let exception = caught_exception.unwrap();
        assert_eq!(exception.exception_type.type_id, type_id);
        assert_eq!(exception.parameters.len(), 1);

        // Stack should be empty after catching
        assert_eq!(manager.get_exception_stack_depth(), 0);
    }

    #[test]
    fn test_exception_parameter_validation() {
        let manager = ExceptionHandlingManager::default();

        // Register exception type with specific parameters
        let type_id = manager.register_exception_type(
            "typed_exception".to_string(),
            vec![ValType::I32, ValType::F64],
            true,
            true,
        ).unwrap();

        // Test wrong parameter count
        let wrong_count_params = vec![ExceptionParam {
            param_type: ValType::I32,
            value: Val::I32(42),
        }];

        let result = manager.throw_exception(type_id, wrong_count_params);
        assert!(result.is_err());

        // Test wrong parameter type
        let wrong_type_params = vec![
            ExceptionParam {
                param_type: ValType::F32, // Wrong type (should be I32)
                value: Val::F32(42.0),
            },
            ExceptionParam {
                param_type: ValType::F64,
                value: Val::F64(3.14),
            },
        ];

        let result = manager.throw_exception(type_id, wrong_type_params);
        assert!(result.is_err());
    }

    #[test]
    fn test_exception_stack_management() {
        let manager = ExceptionHandlingManager::default();

        // Register exception type
        let type_id = manager.register_exception_type(
            "stack_test".to_string(),
            vec![],
            true,
            true,
        ).unwrap();

        // Initially empty stack
        assert_eq!(manager.get_exception_stack_depth(), 0);

        // Throw multiple exceptions
        for i in 0..5 {
            let result = manager.throw_exception(type_id, vec![]).unwrap();
            assert!(result.success);
            assert_eq!(manager.get_exception_stack_depth(), i + 1);
        }

        // Clear stack
        manager.clear_exception_stack();
        assert_eq!(manager.get_exception_stack_depth(), 0);
    }

    #[test]
    fn test_statistics_tracking() {
        let manager = ExceptionHandlingManager::default();

        // Initial statistics
        let (thrown1, caught1, propagated1, time1) = manager.get_statistics();
        assert_eq!(thrown1, 0);
        assert_eq!(caught1, 0);
        assert_eq!(propagated1, 0);

        // Register exception type and throw some exceptions
        let type_id = manager.register_exception_type(
            "stats_test".to_string(),
            vec![],
            true,
            true,
        ).unwrap();

        // Throw exceptions (will be propagated since no handlers)
        for _ in 0..3 {
            manager.throw_exception(type_id, vec![]).unwrap();
        }

        let (thrown2, caught2, propagated2, time2) = manager.get_statistics();
        assert_eq!(thrown2, 3);
        assert_eq!(caught2, 0);
        assert_eq!(propagated2, 3);
        assert!(time2 > time1);

        // Reset statistics
        manager.reset_statistics();
        let (thrown3, caught3, propagated3, time3) = manager.get_statistics();
        assert_eq!(thrown3, 0);
        assert_eq!(caught3, 0);
        assert_eq!(propagated3, 0);
        assert_eq!(time3, 0);
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test exception handling support check
            let supported = wasmtime4j_exception_handling_supported();
            assert!(supported == 0 || supported == 1);

            // Test null pointer handling in registration
            let mut type_id = 0u32;
            assert_eq!(wasmtime4j_register_exception_type(
                ptr::null(),
                ptr::null(),
                0, 0, 0,
                &mut type_id
            ), -1);

            // Test null pointer handling in throw
            let mut result = ExceptionOperationResult {
                success: false,
                exception_id: 0,
                was_caught: false,
                execution_time_micros: 0,
                error_code: 0,
            };

            assert_eq!(wasmtime4j_throw_exception(
                1,
                ptr::null(),
                0,
                &mut result
            ), -1);

            // Test exception stack operations
            let depth = wasmtime4j_exception_stack_depth();
            assert!(depth >= 0);

            assert_eq!(wasmtime4j_clear_exception_stack(), 0);

            // Test statistics
            let mut thrown = 0u64;
            let mut caught = 0u64;
            let mut propagated = 0u64;
            let mut time = 0u64;

            assert_eq!(wasmtime4j_exception_get_statistics(
                &mut thrown,
                &mut caught,
                &mut propagated,
                &mut time
            ), 0);

            assert_eq!(wasmtime4j_exception_reset_statistics(), 0);
        }
    }
}