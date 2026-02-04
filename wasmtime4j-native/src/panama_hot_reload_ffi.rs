//! Panama Foreign Function Interface bindings for Hot Reload operations (Java 23+)

use crate::hot_reload::{HotReloadManager, HotReloadConfig, SwapStrategy, LoadRequest, LoadPriority, ValidationConfig, SemanticVersion};
use crate::engine::Engine;

use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::os::raw::{c_char, c_int, c_void, c_float, c_double};
use std::ffi::{CStr, CString};
use std::ptr;

/// Panama FFI function to create a hot reload manager
#[no_mangle]
pub extern "C" fn panama_create_hot_reload_manager(
    engine_ptr: *const c_void,
    validation_enabled: c_int,
    state_preservation_enabled: c_int,
    debounce_delay_ms: i64,
    precompilation_enabled: c_int,
    max_reload_attempts: c_int,
    health_check_interval_secs: i64,
    loader_thread_count: c_int,
    cache_size: c_int,
) -> *mut c_void {
    if engine_ptr.is_null() {
        return ptr::null_mut();
    }

    unsafe {
        let engine_ptr = engine_ptr as *const Engine;
        if engine_ptr.is_null() {
            return ptr::null_mut();
        }

        let engine = Arc::new(std::ptr::read(engine_ptr));

        let config = HotReloadConfig {
            validation_enabled: validation_enabled != 0,
            state_preservation_enabled: state_preservation_enabled != 0,
            debounce_delay_ms: debounce_delay_ms as u64,
            precompilation_enabled: precompilation_enabled != 0,
            max_reload_attempts: max_reload_attempts as u32,
            health_check_interval: Duration::from_secs(health_check_interval_secs as u64),
            default_swap_strategy: SwapStrategy::Canary {
                initial_percentage: 10.0,
                increment_percentage: 25.0,
                increment_interval: Duration::from_secs(60),
                success_threshold: 0.99,
            },
            loader_thread_count: loader_thread_count as usize,
            cache_size: cache_size as usize,
        };

        match HotReloadManager::new(engine, config) {
            Ok(manager) => Box::into_raw(Box::new(manager)) as *mut c_void,
            Err(_) => ptr::null_mut(),
        }
    }
}

/// Panama FFI function to destroy a hot reload manager
#[no_mangle]
pub extern "C" fn panama_destroy_hot_reload_manager(manager_ptr: *mut c_void) {
    if !manager_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(manager_ptr as *mut HotReloadManager);
        }
    }
}

/// Panama FFI function to start a hot swap operation
#[no_mangle]
pub extern "C" fn panama_start_hot_swap(
    manager_ptr: *const c_void,
    component_name: *const c_char,
    version_string: *const c_char,
    swap_strategy_type: c_int,
    strategy_param1: i64,
    strategy_param2: i64,
    strategy_param3: c_double,
    operation_id_out: *mut *mut c_char,
) -> c_int {
    if manager_ptr.is_null() || component_name.is_null() || version_string.is_null() || operation_id_out.is_null() {
        return -1;
    }

    unsafe {
        let manager = &*(manager_ptr as *const HotReloadManager);
        let name_cstr = CStr::from_ptr(component_name);
        let version_cstr = CStr::from_ptr(version_string);

        let name = match name_cstr.to_str() {
            Ok(s) => s.to_string(),
            Err(_) => return -1,
        };

        let version_str = match version_cstr.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        let version = match SemanticVersion::parse(version_str) {
            Ok(v) => v,
            Err(_) => return -1,
        };

        // Convert swap strategy based on type
        let strategy = match swap_strategy_type {
            0 => Some(SwapStrategy::Immediate),
            1 => Some(SwapStrategy::Canary {
                initial_percentage: strategy_param1 as f32 / 100.0,
                increment_percentage: strategy_param2 as f32 / 100.0,
                increment_interval: Duration::from_secs(60),
                success_threshold: strategy_param3 as f32,
            }),
            2 => Some(SwapStrategy::BlueGreen),
            3 => Some(SwapStrategy::RollingUpdate {
                batch_size: strategy_param1 as usize,
                batch_interval: Duration::from_secs(strategy_param2 as u64),
            }),
            4 => Some(SwapStrategy::ABTest {
                test_percentage: strategy_param1 as f32 / 100.0,
                test_duration: Duration::from_secs(strategy_param2 as u64),
                success_metrics: vec!["response_time".to_string(), "error_rate".to_string()],
            }),
            _ => None,
        };

        match manager.start_hot_swap(name, version, strategy) {
            Ok(operation_id) => {
                let c_string = CString::new(operation_id)
                    .unwrap_or_else(|_| CString::new("invalid").unwrap());
                *operation_id_out = c_string.into_raw();
                0
            }
            Err(_) => -1,
        }
    }
}

/// Panama FFI function to get the status of a hot swap operation
/// Returns a pointer to a SwapOperationStatus struct or null if not found
#[no_mangle]
pub extern "C" fn panama_get_swap_status(
    manager_ptr: *const c_void,
    operation_id: *const c_char,
    status_out: *mut SwapOperationStatus,
) -> c_int {
    if manager_ptr.is_null() || operation_id.is_null() || status_out.is_null() {
        return -1;
    }

    unsafe {
        let manager = &*(manager_ptr as *const HotReloadManager);
        let op_id_cstr = CStr::from_ptr(operation_id);

        let op_id = match op_id_cstr.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        match manager.get_swap_status(op_id) {
            Ok(Some(operation)) => {
                // Copy operation data to output struct
                let component_name = CString::new(operation.component_name)
                    .unwrap_or_else(|_| CString::new("invalid").unwrap());
                let from_version = CString::new(format!("{}", operation.from_version))
                    .unwrap_or_else(|_| CString::new("0.0.0").unwrap());
                let to_version = CString::new(format!("{}", operation.to_version))
                    .unwrap_or_else(|_| CString::new("0.0.0").unwrap());

                (*status_out).component_name = component_name.into_raw();
                (*status_out).from_version = from_version.into_raw();
                (*status_out).to_version = to_version.into_raw();
                (*status_out).status = operation.status as u8 as c_int;
                (*status_out).progress = operation.progress;
                (*status_out).started_at_secs = operation.started_at.elapsed().as_secs();
                (*status_out).total_requests = operation.traffic_stats.total_requests;
                (*status_out).successful_requests = operation.traffic_stats.successful_requests;
                (*status_out).failed_requests = operation.traffic_stats.failed_requests;
                (*status_out).error_rate = operation.traffic_stats.error_rate;

                0 // Success
            }
            Ok(None) => 1, // Not found
            Err(_) => -1, // Error
        }
    }
}

/// Panama FFI function to cancel a hot swap operation
#[no_mangle]
pub extern "C" fn panama_cancel_hot_swap(
    manager_ptr: *const c_void,
    operation_id: *const c_char,
) -> c_int {
    if manager_ptr.is_null() || operation_id.is_null() {
        return -1;
    }

    unsafe {
        let manager = &*(manager_ptr as *const HotReloadManager);
        let op_id_cstr = CStr::from_ptr(operation_id);

        let op_id = match op_id_cstr.to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        match manager.cancel_hot_swap(op_id) {
            Ok(_) => 0,
            Err(_) => -1,
        }
    }
}

/// Panama FFI function to load a component asynchronously
#[no_mangle]
pub extern "C" fn panama_load_component_async(
    manager_ptr: *const c_void,
    component_name: *const c_char,
    component_path: *const c_char,
    version_string: *const c_char,
    priority: c_int,
    validate_interfaces: c_int,
    validate_dependencies: c_int,
    validate_security: c_int,
    validate_performance: c_int,
    timeout_secs: i64,
    request_id_out: *mut *mut c_char,
) -> c_int {
    if manager_ptr.is_null() || component_name.is_null() || component_path.is_null()
        || version_string.is_null() || request_id_out.is_null() {
        return -1;
    }

    unsafe {
        let manager = &*(manager_ptr as *const HotReloadManager);

        let name = match CStr::from_ptr(component_name).to_str() {
            Ok(s) => s.to_string(),
            Err(_) => return -1,
        };

        let path = match CStr::from_ptr(component_path).to_str() {
            Ok(s) => s.to_string(),
            Err(_) => return -1,
        };

        let version_str = match CStr::from_ptr(version_string).to_str() {
            Ok(s) => s,
            Err(_) => return -1,
        };

        let version = match SemanticVersion::parse(version_str) {
            Ok(v) => v,
            Err(_) => return -1,
        };

        let load_priority = match priority {
            0 => LoadPriority::Low,
            1 => LoadPriority::Normal,
            2 => LoadPriority::High,
            3 => LoadPriority::Critical,
            _ => LoadPriority::Normal,
        };

        let validation_config = ValidationConfig {
            validate_interfaces: validate_interfaces != 0,
            validate_dependencies: validate_dependencies != 0,
            validate_security: validate_security != 0,
            validate_performance: validate_performance != 0,
            timeout: Duration::from_secs(timeout_secs as u64),
        };

        let request = LoadRequest {
            request_id: generate_uuid(),
            component_name: name,
            component_path: path,
            version,
            priority: load_priority,
            validation_config,
            requested_at: Instant::now(),
        };

        match manager.load_component_async(request) {
            Ok(request_id) => {
                let c_string = CString::new(request_id)
                    .unwrap_or_else(|_| CString::new("invalid").unwrap());
                *request_id_out = c_string.into_raw();
                0
            }
            Err(_) => -1,
        }
    }
}

/// Panama FFI function to get hot reload metrics
#[no_mangle]
pub extern "C" fn panama_get_hot_reload_metrics(
    manager_ptr: *const c_void,
    metrics_out: *mut HotReloadMetricsC,
) -> c_int {
    if manager_ptr.is_null() || metrics_out.is_null() {
        return -1;
    }

    unsafe {
        let manager = &*(manager_ptr as *const HotReloadManager);
        match manager.get_metrics() {
            Ok(metrics) => {
                (*metrics_out).total_swaps = metrics.total_swaps;
                (*metrics_out).successful_swaps = metrics.successful_swaps;
                (*metrics_out).failed_swaps = metrics.failed_swaps;
                (*metrics_out).rollbacks = metrics.rollbacks;
                (*metrics_out).avg_swap_time_ms = metrics.avg_swap_time.as_millis() as u64;
                (*metrics_out).current_active_swaps = metrics.current_active_swaps;
                (*metrics_out).components_loaded = metrics.components_loaded;
                (*metrics_out).cache_efficiency = metrics.cache_efficiency;
                0
            }
            Err(_) => -1,
        }
    }
}

/// Panama FFI function to free a C string allocated by this library
#[no_mangle]
pub extern "C" fn panama_free_string(ptr: *mut c_char) {
    if !ptr.is_null() {
        unsafe {
            let _ = CString::from_raw(ptr);
        }
    }
}

/// Panama FFI function to free a SwapOperationStatus struct
#[no_mangle]
pub extern "C" fn panama_free_swap_status(status: *mut SwapOperationStatus) {
    if !status.is_null() {
        unsafe {
            if !(*status).component_name.is_null() {
                let _ = CString::from_raw((*status).component_name);
            }
            if !(*status).from_version.is_null() {
                let _ = CString::from_raw((*status).from_version);
            }
            if !(*status).to_version.is_null() {
                let _ = CString::from_raw((*status).to_version);
            }
        }
    }
}

// C struct definitions for Panama FFI

/// C-compatible struct for swap operation status
#[repr(C)]
pub struct SwapOperationStatus {
    pub component_name: *mut c_char,
    pub from_version: *mut c_char,
    pub to_version: *mut c_char,
    pub status: c_int,
    pub progress: c_float,
    pub started_at_secs: u64,
    pub total_requests: u64,
    pub successful_requests: u64,
    pub failed_requests: u64,
    pub error_rate: c_float,
}

/// C-compatible struct for hot reload metrics
#[repr(C)]
pub struct HotReloadMetricsC {
    pub total_swaps: u64,
    pub successful_swaps: u64,
    pub failed_swaps: u64,
    pub rollbacks: u64,
    pub avg_swap_time_ms: u64,
    pub current_active_swaps: u32,
    pub components_loaded: u64,
    pub cache_efficiency: c_float,
}

// Helper function to generate UUID-like strings for request IDs
fn generate_uuid() -> String {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    format!("panama-hotreload-{}", timestamp)
}