//! JNI bindings for WebAssembly Exception Handler
//!
//! This module provides JNI-compatible functions for the JniExceptionHandlerImpl class,
//! implementing WebAssembly exception handling support.

#![allow(unused_variables)]

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JList};
use jni::sys::{jlong, jint, jboolean, jstring};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::collections::HashMap;
use std::sync::RwLock;

/// Exception handler configuration
#[derive(Debug, Clone)]
pub struct ExceptionHandlerConfig {
    /// Whether nested try/catch is enabled
    pub nested_try_catch_enabled: bool,
    /// Whether exception unwinding is enabled
    pub exception_unwinding_enabled: bool,
    /// Maximum unwind depth
    pub max_unwind_depth: i32,
    /// Whether exception type validation is enabled
    pub type_validation_enabled: bool,
    /// Whether stack traces are enabled
    pub stack_traces_enabled: bool,
    /// Whether exception propagation is enabled
    pub exception_propagation_enabled: bool,
    /// Whether GC integration is enabled
    pub gc_integration_enabled: bool,
}

impl Default for ExceptionHandlerConfig {
    fn default() -> Self {
        Self {
            nested_try_catch_enabled: true,
            exception_unwinding_enabled: true,
            max_unwind_depth: 64,
            type_validation_enabled: true,
            stack_traces_enabled: true,
            exception_propagation_enabled: true,
            gc_integration_enabled: false,
        }
    }
}

/// Exception tag definition
#[derive(Debug, Clone)]
pub struct ExceptionTagDef {
    /// Tag handle (unique ID)
    pub handle: u64,
    /// Tag name
    pub name: String,
    /// Number of parameters
    pub param_count: usize,
}

/// Exception handler wrapper for JNI
pub struct JniExceptionHandlerWrapper {
    /// Configuration
    config: ExceptionHandlerConfig,
    /// Whether the handler is closed
    closed: AtomicBool,
    /// Next tag ID
    next_tag_id: AtomicU64,
    /// Registered exception tags
    tags: RwLock<HashMap<String, ExceptionTagDef>>,
}

impl JniExceptionHandlerWrapper {
    /// Create a new exception handler
    pub fn new(config: ExceptionHandlerConfig) -> Self {
        Self {
            config,
            closed: AtomicBool::new(false),
            next_tag_id: AtomicU64::new(1),
            tags: RwLock::new(HashMap::new()),
        }
    }

    /// Create an exception tag
    pub fn create_tag(&self, name: String, param_count: usize) -> Option<u64> {
        if self.closed.load(Ordering::Acquire) {
            return None;
        }

        let mut tags = self.tags.write().ok()?;
        if tags.contains_key(&name) {
            return None; // Tag already exists
        }

        let handle = self.next_tag_id.fetch_add(1, Ordering::Relaxed);
        let tag = ExceptionTagDef {
            handle,
            name: name.clone(),
            param_count,
        };
        tags.insert(name, tag);
        Some(handle)
    }

    /// Capture stack trace for a tag
    pub fn capture_stack_trace(&self, tag_handle: u64) -> Option<String> {
        if self.closed.load(Ordering::Acquire) {
            return None;
        }

        if !self.config.stack_traces_enabled {
            return None;
        }

        // Return a simulated stack trace with wasm function info
        Some(format!(
            "WebAssembly exception stack trace for tag {}:\n  at wasm function <unknown>",
            tag_handle
        ))
    }

    /// Perform unwinding
    pub fn perform_unwinding(&self, current_depth: i32) -> bool {
        if self.closed.load(Ordering::Acquire) {
            return false;
        }

        if !self.config.exception_unwinding_enabled {
            return false;
        }

        // Check if we should continue unwinding
        current_depth < self.config.max_unwind_depth
    }

    /// Close the handler
    pub fn close(&self) {
        self.closed.store(true, Ordering::Release);
    }

    /// Check if closed
    pub fn is_closed(&self) -> bool {
        self.closed.load(Ordering::Acquire)
    }
}

// =============================================================================
// JNI Bindings
// =============================================================================

/// Create a native exception handler
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_experimental_JniExceptionHandlerImpl_createNativeHandler(
    mut env: JNIEnv,
    _class: JClass,
    config: JObject,
) -> jlong {
    // Extract config from Java object before any closures
    let config_result: Result<ExceptionHandlerConfig, String> = (|| {
        if config.is_null() {
            return Ok(ExceptionHandlerConfig::default());
        }

        // Call getter methods on the config object
        let nested_try_catch = env.call_method(&config, "isNestedTryCatchEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isNestedTryCatchEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        let exception_unwinding = env.call_method(&config, "isExceptionUnwindingEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isExceptionUnwindingEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        let max_unwind_depth = env.call_method(&config, "getMaxUnwindDepth", "()I", &[])
            .map_err(|e| format!("Failed to call getMaxUnwindDepth: {:?}", e))?
            .i()
            .map_err(|e| format!("Failed to get int: {:?}", e))?;

        let type_validation = env.call_method(&config, "isExceptionTypeValidationEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isExceptionTypeValidationEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        let stack_traces = env.call_method(&config, "isStackTracesEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isStackTracesEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        let exception_propagation = env.call_method(&config, "isExceptionPropagationEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isExceptionPropagationEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        let gc_integration = env.call_method(&config, "isGcIntegrationEnabled", "()Z", &[])
            .map_err(|e| format!("Failed to call isGcIntegrationEnabled: {:?}", e))?
            .z()
            .map_err(|e| format!("Failed to get boolean: {:?}", e))?;

        Ok(ExceptionHandlerConfig {
            nested_try_catch_enabled: nested_try_catch,
            exception_unwinding_enabled: exception_unwinding,
            max_unwind_depth,
            type_validation_enabled: type_validation,
            stack_traces_enabled: stack_traces,
            exception_propagation_enabled: exception_propagation,
            gc_integration_enabled: gc_integration,
        })
    })();

    match config_result {
        Ok(cfg) => {
            let handler = Box::new(JniExceptionHandlerWrapper::new(cfg));
            Box::into_raw(handler) as jlong
        }
        Err(e) => {
            log::error!("Failed to create exception handler: {}", e);
            0
        }
    }
}

/// Create a native exception tag
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_experimental_JniExceptionHandlerImpl_createNativeExceptionTag(
    mut env: JNIEnv,
    _class: JClass,
    handler_handle: jlong,
    name: JString,
    parameter_types: JObject,
) -> jlong {
    if handler_handle == 0 {
        return 0;
    }

    // Extract name before any unsafe operations
    let name_str: Result<String, _> = env.get_string(&name).map(|s| s.into());

    // Get parameter list size
    let param_count: Result<usize, String> = (|| {
        if parameter_types.is_null() {
            return Ok(0);
        }

        let list = JList::from_env(&mut env, &parameter_types)
            .map_err(|e| format!("Failed to get list: {:?}", e))?;

        let size = list.size(&mut env)
            .map_err(|e| format!("Failed to get list size: {:?}", e))?;

        Ok(size as usize)
    })();

    let handler = unsafe { &*(handler_handle as *const JniExceptionHandlerWrapper) };

    match (name_str, param_count) {
        (Ok(name), Ok(count)) => {
            match handler.create_tag(name, count) {
                Some(handle) => handle as jlong,
                None => 0,
            }
        }
        _ => 0,
    }
}

/// Capture a native stack trace
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_experimental_JniExceptionHandlerImpl_captureNativeStackTrace(
    mut env: JNIEnv,
    _class: JClass,
    handler_handle: jlong,
    tag_handle: jlong,
) -> jstring {
    if handler_handle == 0 {
        return std::ptr::null_mut();
    }

    let handler = unsafe { &*(handler_handle as *const JniExceptionHandlerWrapper) };

    match handler.capture_stack_trace(tag_handle as u64) {
        Some(trace) => {
            match env.new_string(&trace) {
                Ok(jstr) => jstr.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        None => std::ptr::null_mut(),
    }
}

/// Perform native unwinding
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_experimental_JniExceptionHandlerImpl_performNativeUnwinding(
    _env: JNIEnv,
    _class: JClass,
    handler_handle: jlong,
    current_depth: jint,
) -> jboolean {
    if handler_handle == 0 {
        return 0;
    }

    let handler = unsafe { &*(handler_handle as *const JniExceptionHandlerWrapper) };

    if handler.perform_unwinding(current_depth) { 1 } else { 0 }
}

/// Close a native exception handler
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_experimental_JniExceptionHandlerImpl_closeNativeHandler(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            let handler = Box::from_raw(handle as *mut JniExceptionHandlerWrapper);
            handler.close();
            // handler is dropped here
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_handler_creation() {
        let config = ExceptionHandlerConfig::default();
        let handler = JniExceptionHandlerWrapper::new(config);
        assert!(!handler.is_closed());
    }

    #[test]
    fn test_tag_creation() {
        let config = ExceptionHandlerConfig::default();
        let handler = JniExceptionHandlerWrapper::new(config);

        let tag_handle = handler.create_tag("test_tag".to_string(), 2);
        assert!(tag_handle.is_some());
        assert!(tag_handle.unwrap() > 0);
    }

    #[test]
    fn test_duplicate_tag_fails() {
        let config = ExceptionHandlerConfig::default();
        let handler = JniExceptionHandlerWrapper::new(config);

        let tag1 = handler.create_tag("test_tag".to_string(), 2);
        assert!(tag1.is_some());

        let tag2 = handler.create_tag("test_tag".to_string(), 2);
        assert!(tag2.is_none()); // Should fail - duplicate
    }

    #[test]
    fn test_unwinding() {
        let config = ExceptionHandlerConfig {
            max_unwind_depth: 10,
            exception_unwinding_enabled: true,
            ..Default::default()
        };
        let handler = JniExceptionHandlerWrapper::new(config);

        assert!(handler.perform_unwinding(5));  // Below max depth
        assert!(handler.perform_unwinding(9));  // Still below max depth
        assert!(!handler.perform_unwinding(10)); // At max depth
    }

    #[test]
    fn test_stack_traces() {
        let config = ExceptionHandlerConfig {
            stack_traces_enabled: true,
            ..Default::default()
        };
        let handler = JniExceptionHandlerWrapper::new(config);

        let trace = handler.capture_stack_trace(123);
        assert!(trace.is_some());
    }

    #[test]
    fn test_stack_traces_disabled() {
        let config = ExceptionHandlerConfig {
            stack_traces_enabled: false,
            ..Default::default()
        };
        let handler = JniExceptionHandlerWrapper::new(config);

        let trace = handler.capture_stack_trace(123);
        assert!(trace.is_none());
    }
}
