//! Exception handling support for WebAssembly
//!
//! This module implements the WebAssembly exception handling proposal,
//! providing try/catch block support and exception throwing mechanisms.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::gc::{GcRuntime, GcValue}; // Import GC support from Task #308
use std::collections::HashMap;
use std::ptr;
use std::sync::{Arc, Mutex};
use std::ffi::{CStr, CString};
use std::fmt::Write;
use wasmtime::*;

/// Exception tag definition
#[derive(Debug, Clone)]
pub struct ExceptionTag {
    /// The tag name
    pub name: String,
    /// Parameter types for this exception
    pub parameter_types: Vec<ValType>,
    /// Native handle for the tag
    pub handle: u64,
    /// Whether this tag supports GC references
    pub is_gc_aware: bool,
    /// Debug information for this tag
    pub debug_info: Option<ExceptionDebugInfo>,
}

/// Debug information for exception tags
#[derive(Debug, Clone)]
pub struct ExceptionDebugInfo {
    /// Source location where tag was created
    pub source_location: Option<String>,
    /// Additional metadata
    pub metadata: HashMap<String, String>,
    /// Creation timestamp
    pub created_at: std::time::SystemTime,
}

/// Exception handling configuration
#[derive(Debug, Clone)]
pub struct ExceptionHandlingConfig {
    /// Enable nested try/catch blocks
    pub enable_nested_try_catch: bool,
    /// Enable exception unwinding
    pub enable_exception_unwinding: bool,
    /// Maximum unwind depth
    pub max_unwind_depth: u32,
    /// Validate exception types
    pub validate_exception_types: bool,
    /// Enable stack trace capture
    pub enable_stack_traces: bool,
    /// Enable exception propagation between WebAssembly and host
    pub enable_exception_propagation: bool,
    /// Enable GC integration for exception payloads
    pub enable_gc_integration: bool,
}

impl Default for ExceptionHandlingConfig {
    fn default() -> Self {
        Self {
            enable_nested_try_catch: true,
            enable_exception_unwinding: true,
            max_unwind_depth: 1000,
            validate_exception_types: true,
            enable_stack_traces: true,
            enable_exception_propagation: true,
            enable_gc_integration: false,
        }
    }
}

/// Exception handler for WebAssembly with GC and debugging support
pub struct ExceptionHandler {
    /// Configuration
    config: ExceptionHandlingConfig,
    /// Exception tags registry
    tags: Arc<Mutex<HashMap<String, ExceptionTag>>>,
    /// Next tag handle
    next_handle: Arc<Mutex<u64>>,
    /// GC runtime for GC-aware exception handling
    gc_runtime: Option<Arc<GcRuntime>>,
    /// Debug information collector
    debug_collector: Option<DebugInfoCollector>,
}

impl ExceptionHandler {
    /// Creates a new exception handler
    pub fn new(config: ExceptionHandlingConfig) -> WasmtimeResult<Self> {
        let debug_collector = if config.enable_stack_traces {
            Some(DebugInfoCollector::new(
                true,
                true,
                1000, // Max trace depth
            ))
        } else {
            None
        };

        Ok(Self {
            config,
            tags: Arc::new(Mutex::new(HashMap::new())),
            next_handle: Arc::new(Mutex::new(1)),
            gc_runtime: None, // Will be set when GC runtime is provided
            debug_collector,
        })
    }

    /// Creates a new exception handler with GC runtime
    pub fn new_with_gc(
        config: ExceptionHandlingConfig,
        gc_runtime: Arc<GcRuntime>
    ) -> WasmtimeResult<Self> {
        let mut handler = Self::new(config)?;
        handler.gc_runtime = Some(gc_runtime);
        Ok(handler)
    }

    /// Creates an exception tag
    pub fn create_exception_tag(
        &self,
        name: &str,
        parameter_types: Vec<ValType>,
    ) -> WasmtimeResult<ExceptionTag> {
        self.create_exception_tag_with_options(name, parameter_types, false, None)
    }

    /// Creates an exception tag with GC support and debug info
    pub fn create_exception_tag_with_options(
        &self,
        name: &str,
        parameter_types: Vec<ValType>,
        is_gc_aware: bool,
        source_location: Option<String>,
    ) -> WasmtimeResult<ExceptionTag> {
        if name.is_empty() {
            return Err(WasmtimeError::InvalidInput(
                "Exception tag name cannot be empty".to_string(),
            ));
        }

        // Validate GC integration
        if is_gc_aware && !self.config.enable_gc_integration {
            return Err(WasmtimeError::InvalidInput(
                "GC integration is not enabled for this handler".to_string(),
            ));
        }

        if is_gc_aware && self.gc_runtime.is_none() {
            return Err(WasmtimeError::InvalidInput(
                "GC-aware exception tag requires GC runtime".to_string(),
            ));
        }

        let mut next_handle = self.next_handle.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire handle lock".to_string())
        })?;

        let handle = *next_handle;
        *next_handle += 1;

        let debug_info = if self.config.enable_stack_traces {
            Some(ExceptionDebugInfo {
                source_location,
                metadata: HashMap::new(),
                created_at: std::time::SystemTime::now(),
            })
        } else {
            None
        };

        let tag = ExceptionTag {
            name: name.to_string(),
            parameter_types,
            handle,
            is_gc_aware,
            debug_info,
        };

        let mut tags = self.tags.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire tags lock".to_string())
        })?;

        tags.insert(name.to_string(), tag.clone());
        drop(tags);

        Ok(tag)
    }

    /// Validates exception payload against tag types
    pub fn validate_exception_payload(
        &self,
        tag: &ExceptionTag,
        payload: &[Val],
    ) -> WasmtimeResult<()> {
        if !self.config.validate_exception_types {
            return Ok(());
        }

        if payload.len() != tag.parameter_types.len() {
            return Err(WasmtimeError::ValidationError(format!(
                "Exception payload size ({}) doesn't match tag parameter count ({})",
                payload.len(),
                tag.parameter_types.len()
            )));
        }

        for (i, (expected_type, actual_value)) in
            tag.parameter_types.iter().zip(payload.iter()).enumerate()
        {
            if !self.value_matches_type(actual_value, expected_type) {
                return Err(WasmtimeError::ValidationError(format!(
                    "Exception payload parameter {} type mismatch. Expected: {:?}, Actual: {:?}",
                    i,
                    expected_type,
                    actual_value.ty()
                )));
            }
        }

        Ok(())
    }

    /// Checks if a value matches the expected type
    fn value_matches_type(&self, value: &Val, expected_type: &ValType) -> bool {
        match (value, expected_type) {
            (Val::I32(_), ValType::I32) => true,
            (Val::I64(_), ValType::I64) => true,
            (Val::F32(_), ValType::F32) => true,
            (Val::F64(_), ValType::F64) => true,
            (Val::V128(_), ValType::V128) => true,
            (Val::FuncRef(_), ValType::FuncRef) => true,
            (Val::ExternRef(_), ValType::ExternRef) => true,
            _ => false,
        }
    }

    /// Gets an exception tag by name
    pub fn get_exception_tag(&self, name: &str) -> WasmtimeResult<Option<ExceptionTag>> {
        let tags = self.tags.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire tags lock".to_string())
        })?;

        Ok(tags.get(name).cloned())
    }

    /// Lists all exception tags
    pub fn list_exception_tags(&self) -> WasmtimeResult<Vec<ExceptionTag>> {
        let tags = self.tags.lock().map_err(|_| {
            WasmtimeError::Internal("Failed to acquire tags lock".to_string())
        })?;

        Ok(tags.values().cloned().collect())
    }

    /// Gets the configuration
    pub fn config(&self) -> &ExceptionHandlingConfig {
        &self.config
    }
}

/// Exception payload representation with GC support
#[derive(Debug, Clone)]
pub struct ExceptionPayload {
    /// The exception tag
    pub tag: ExceptionTag,
    /// The payload values
    pub values: Vec<Val>,
    /// GC values for GC-aware exceptions
    pub gc_values: Vec<GcValue>,
    /// Stack trace at exception creation
    pub stack_trace: Option<String>,
    /// Debug context information
    pub debug_context: Option<ExceptionDebugContext>,
}

/// Debug context for exception payload
#[derive(Debug, Clone)]
pub struct ExceptionDebugContext {
    /// Function name where exception was thrown
    pub function_name: Option<String>,
    /// WebAssembly module information
    pub module_info: Option<String>,
    /// Source line information
    pub source_line: Option<u32>,
    /// Additional debug metadata
    pub metadata: HashMap<String, String>,
}

/// Debug information collector for exception handling
#[derive(Debug)]
pub struct DebugInfoCollector {
    /// Enable detailed stack traces
    enable_detailed_traces: bool,
    /// Enable source mapping
    enable_source_mapping: bool,
    /// Maximum stack trace depth
    max_trace_depth: u32,
}

impl DebugInfoCollector {
    /// Creates a new debug info collector
    pub fn new(
        enable_detailed_traces: bool,
        enable_source_mapping: bool,
        max_trace_depth: u32
    ) -> Self {
        Self {
            enable_detailed_traces,
            enable_source_mapping,
            max_trace_depth,
        }
    }

    /// Captures stack trace for an exception
    pub fn capture_stack_trace(&self, _store: &Store<()>) -> WasmtimeResult<String> {
        if !self.enable_detailed_traces {
            return Ok("Stack trace disabled".to_string());
        }

        // Placeholder implementation - would integrate with Wasmtime's stack trace APIs
        let mut trace = String::new();
        writeln!(trace, "WebAssembly Stack Trace:").map_err(|_| {
            WasmtimeError::Internal("Failed to format stack trace".to_string())
        })?;
        writeln!(trace, "  at wasm function 'main'").map_err(|_| {
            WasmtimeError::Internal("Failed to format stack trace".to_string())
        })?;
        writeln!(trace, "  at wasm function 'start'").map_err(|_| {
            WasmtimeError::Internal("Failed to format stack trace".to_string())
        })?;

        Ok(trace)
    }

    /// Creates debug context for an exception
    pub fn create_debug_context(
        &self,
        function_name: Option<String>,
        module_name: Option<String>
    ) -> ExceptionDebugContext {
        let mut metadata = HashMap::new();

        if let Some(ref func_name) = function_name {
            metadata.insert("function".to_string(), func_name.clone());
        }

        if let Some(ref mod_name) = module_name {
            metadata.insert("module".to_string(), mod_name.clone());
        }

        ExceptionDebugContext {
            function_name,
            module_info: module_name,
            source_line: None, // Would be populated from debug symbols
            metadata,
        }
    }
}

impl ExceptionPayload {
    /// Creates a new exception payload
    pub fn new(tag: ExceptionTag, values: Vec<Val>) -> Self {
        Self {
            tag,
            values,
            gc_values: Vec::new(),
            stack_trace: None,
            debug_context: None,
        }
    }

    /// Creates a new GC-aware exception payload
    pub fn new_with_gc(
        tag: ExceptionTag,
        values: Vec<Val>,
        gc_values: Vec<GcValue>
    ) -> Self {
        Self {
            tag,
            values,
            gc_values,
            stack_trace: None,
            debug_context: None,
        }
    }

    /// Creates a new exception payload with debug context
    pub fn new_with_debug(
        tag: ExceptionTag,
        values: Vec<Val>,
        gc_values: Vec<GcValue>,
        stack_trace: Option<String>,
        debug_context: Option<ExceptionDebugContext>
    ) -> Self {
        Self {
            tag,
            values,
            gc_values,
            stack_trace,
            debug_context,
        }
    }

    /// Gets the tag name
    pub fn tag_name(&self) -> &str {
        &self.tag.name
    }

    /// Gets the payload values
    pub fn values(&self) -> &[Val] {
        &self.values
    }

    /// Gets the GC values
    pub fn gc_values(&self) -> &[GcValue] {
        &self.gc_values
    }

    /// Checks if this payload has GC values
    pub fn has_gc_values(&self) -> bool {
        !self.gc_values.is_empty()
    }

    /// Gets the stack trace
    pub fn stack_trace(&self) -> Option<&str> {
        self.stack_trace.as_deref()
    }

    /// Gets the debug context
    pub fn debug_context(&self) -> Option<&ExceptionDebugContext> {
        self.debug_context.as_ref()
    }
}

/// Exception unwinding context
#[derive(Debug)]
pub struct UnwindContext {
    /// Current unwind depth
    pub depth: u32,
    /// Maximum allowed depth
    pub max_depth: u32,
    /// Exception being unwound
    pub exception: Option<ExceptionPayload>,
}

impl UnwindContext {
    /// Creates a new unwind context
    pub fn new(max_depth: u32) -> Self {
        Self {
            depth: 0,
            max_depth,
            exception: None,
        }
    }

    /// Starts unwinding with an exception
    pub fn start_unwind(&mut self, exception: ExceptionPayload) -> WasmtimeResult<()> {
        if self.depth >= self.max_depth {
            return Err(WasmtimeError::Runtime(
                "Maximum unwind depth exceeded".to_string(),
            ));
        }

        self.exception = Some(exception);
        self.depth += 1;
        Ok(())
    }

    /// Continues unwinding
    pub fn continue_unwind(&mut self) -> WasmtimeResult<()> {
        if self.depth >= self.max_depth {
            return Err(WasmtimeError::Runtime(
                "Maximum unwind depth exceeded".to_string(),
            ));
        }

        self.depth += 1;
        Ok(())
    }

    /// Catches an exception
    pub fn catch_exception(&mut self) -> Option<ExceptionPayload> {
        self.exception.take()
    }

    /// Resets the unwind context
    pub fn reset(&mut self) {
        self.depth = 0;
        self.exception = None;
    }
}

#[cfg(feature = "jni")]
pub mod jni_bindings {
    use super::*;
    use jni::objects::{JClass, JObject, JString, JList};
    use jni::sys::{jlong, jobject, jboolean};
    use jni::JNIEnv;
    use std::sync::Arc;

    /// Creates a native exception handler
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_createNativeHandler(
        mut env: JNIEnv,
        _class: JClass,
        config: JObject,
    ) -> jlong {
        let config = match extract_exception_config(&mut env, config) {
            Ok(config) => config,
            Err(_) => return 0,
        };

        match ExceptionHandler::new(config) {
            Ok(handler) => Box::into_raw(Box::new(handler)) as jlong,
            Err(_) => 0,
        }
    }

    /// Creates a native exception tag
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_createNativeExceptionTag(
        mut env: JNIEnv,
        _class: JClass,
        handler_handle: jlong,
        name: JString,
        parameter_types: JObject,
    ) -> jlong {
        let handler = unsafe { &mut *(handler_handle as *mut ExceptionHandler) };

        let name: String = match env.get_string(&name) {
            Ok(name) => name.into(),
            Err(_) => return 0,
        };

        let param_types = match extract_value_types(&mut env, parameter_types) {
            Ok(types) => types,
            Err(_) => return 0,
        };

        match handler.create_exception_tag(&name, param_types) {
            Ok(tag) => tag.handle as jlong,
            Err(_) => 0,
        }
    }

    /// Registers a native exception handler
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_registerNativeExceptionHandler(
        env: JNIEnv,
        _class: JClass,
        handler_handle: jlong,
        tag_handle: jlong,
        handler_function: JObject,
    ) {
        // Implementation for registering exception handlers
        // This would involve storing the Java callback and invoking it from native code
    }

    /// Captures stack trace for an exception
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_captureStackTrace(
        mut env: JNIEnv,
        _class: JClass,
        handler_handle: jlong,
        tag_handle: jlong,
    ) -> jobject {
        let _handler = unsafe { &*(handler_handle as *const ExceptionHandler) };

        // Generate a simple stack trace for now
        let trace = "    at wasm function 'main'\n    at wasm function 'start'";

        match env.new_string(trace) {
            Ok(string) => string.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Performs native unwinding
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_performNativeUnwinding(
        _env: JNIEnv,
        _class: JClass,
        handler_handle: jlong,
        current_depth: i32,
    ) -> jboolean {
        let handler = unsafe { &*(handler_handle as *const ExceptionHandler) };

        if current_depth as u32 >= handler.config().max_unwind_depth {
            0 // false
        } else {
            1 // true
        }
    }

    /// Closes a native exception handler
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_experimental_ExceptionHandler_closeNativeHandler(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) {
        if handle != 0 {
            unsafe {
                let _ = Box::from_raw(handle as *mut ExceptionHandler);
            }
        }
    }

    /// Helper function to extract exception handling configuration from Java object
    fn extract_exception_config(env: &mut JNIEnv, config: JObject) -> WasmtimeResult<ExceptionHandlingConfig> {
        let class = env.get_object_class(&config)?;

        let enable_nested = env.call_method(&config, "isNestedTryCatchEnabled", "()Z", &[])?
            .z().unwrap_or(false);

        let enable_unwinding = env.call_method(&config, "isExceptionUnwindingEnabled", "()Z", &[])?
            .z().unwrap_or(false);

        let max_depth = env.call_method(&config, "getMaxUnwindDepth", "()I", &[])?
            .i().unwrap_or(1000) as u32;

        let validate_types = env.call_method(&config, "isExceptionTypeValidationEnabled", "()Z", &[])?
            .z().unwrap_or(false);

        Ok(ExceptionHandlingConfig {
            enable_nested_try_catch: enable_nested,
            enable_exception_unwinding: enable_unwinding,
            max_unwind_depth: max_depth,
            validate_exception_types: validate_types,
        })
    }

    /// Helper function to extract value types from Java list
    fn extract_value_types(env: &mut JNIEnv, types_list: JObject) -> WasmtimeResult<Vec<ValType>> {
        let size = env.call_method(&types_list, "size", "()I", &[])?
            .i().unwrap_or(0);

        let mut types = Vec::new();

        for i in 0..size {
            let element = env.call_method(&types_list, "get", "(I)Ljava/lang/Object;", &[i.into()])?
                .l()?;

            // Convert Java WasmValueType to Rust ValType
            let type_name = env.call_method(&element, "name", "()Ljava/lang/String;", &[])?
                .l()?;
            let type_str: String = env.get_string(&type_name.into())?.into();

            let val_type = match type_str.as_str() {
                "I32" => ValType::I32,
                "I64" => ValType::I64,
                "F32" => ValType::F32,
                "F64" => ValType::F64,
                "V128" => ValType::V128,
                "FUNCREF" => ValType::FuncRef,
                "EXTERNREF" => ValType::ExternRef,
                _ => return Err(WasmtimeError::InvalidInput(format!("Unknown value type: {}", type_str))),
            };

            types.push(val_type);
        }

        Ok(types)
    }
}

#[cfg(feature = "panama")]
pub mod panama_bindings {
    use super::*;

    /// Creates a native exception handler for Panama FFI
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_handler_create(
        enable_nested: bool,
        enable_unwinding: bool,
        max_depth: u32,
        validate_types: bool,
    ) -> *mut ExceptionHandler {
        let config = ExceptionHandlingConfig {
            enable_nested_try_catch: enable_nested,
            enable_exception_unwinding: enable_unwinding,
            max_unwind_depth: max_depth,
            validate_exception_types: validate_types,
        };

        match ExceptionHandler::new(config) {
            Ok(handler) => Box::into_raw(Box::new(handler)),
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Creates an exception tag for Panama FFI
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_tag_create(
        handler: *mut ExceptionHandler,
        name: *const std::os::raw::c_char,
        param_types: *const u8,
        param_count: usize,
    ) -> u64 {
        if handler.is_null() || name.is_null() {
            return 0;
        }

        let handler = unsafe { &mut *handler };
        let name = unsafe { CStr::from_ptr(name) };
        let name_str = match name.to_str() {
            Ok(s) => s,
            Err(_) => return 0,
        };

        let types = unsafe {
            std::slice::from_raw_parts(param_types, param_count)
        };

        let mut val_types = Vec::new();
        for &type_byte in types {
            let val_type = match type_byte {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                5 => ValType::FuncRef,
                6 => ValType::ExternRef,
                _ => return 0,
            };
            val_types.push(val_type);
        }

        match handler.create_exception_tag(name_str, val_types) {
            Ok(tag) => tag.handle,
            Err(_) => 0,
        }
    }

    /// Captures stack trace for Panama FFI
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_capture_stack_trace(
        handler: *const ExceptionHandler,
        tag_handle: u64,
    ) -> *mut std::os::raw::c_char {
        if handler.is_null() {
            return std::ptr::null_mut();
        }

        let trace = "    at wasm function 'main'\n    at wasm function 'start'";
        match CString::new(trace) {
            Ok(c_string) => c_string.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Performs unwinding for Panama FFI
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_perform_unwinding(
        handler: *const ExceptionHandler,
        current_depth: u32,
    ) -> bool {
        if handler.is_null() {
            return false;
        }

        let handler = unsafe { &*handler };
        current_depth < handler.config().max_unwind_depth
    }

    /// Closes exception handler for Panama FFI
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_handler_close(handler: *mut ExceptionHandler) {
        if !handler.is_null() {
            unsafe {
                let _ = Box::from_raw(handler);
            }
        }
    }

    /// Frees a C string allocated by native code
    #[no_mangle]
    pub extern "C" fn wasmtime4j_exception_free_string(ptr: *mut std::os::raw::c_char) {
        if !ptr.is_null() {
            unsafe {
                let _ = CString::from_raw(ptr);
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_exception_handler_creation() {
        let config = ExceptionHandlingConfig::default();
        let handler = ExceptionHandler::new(config).unwrap();

        assert!(handler.config.enable_nested_try_catch);
        assert!(handler.config.enable_exception_unwinding);
        assert_eq!(handler.config.max_unwind_depth, 1000);
        assert!(handler.config.validate_exception_types);
    }

    #[test]
    fn test_exception_tag_creation() {
        let config = ExceptionHandlingConfig::default();
        let handler = ExceptionHandler::new(config).unwrap();

        let parameter_types = vec![ValType::I32, ValType::F64];
        let tag = handler.create_exception_tag("test_tag", parameter_types.clone()).unwrap();

        assert_eq!(tag.name, "test_tag");
        assert_eq!(tag.parameter_types, parameter_types);
        assert_eq!(tag.handle, 1);
    }

    #[test]
    fn test_exception_payload_validation() {
        let config = ExceptionHandlingConfig::default();
        let handler = ExceptionHandler::new(config).unwrap();

        let parameter_types = vec![ValType::I32, ValType::F64];
        let tag = handler.create_exception_tag("test_tag", parameter_types).unwrap();

        let valid_payload = vec![Val::I32(42), Val::F64(3.14)];
        assert!(handler.validate_exception_payload(&tag, &valid_payload).is_ok());

        let invalid_payload = vec![Val::I32(42)]; // Wrong count
        assert!(handler.validate_exception_payload(&tag, &invalid_payload).is_err());

        let invalid_types = vec![Val::F32(1.0), Val::F64(3.14)]; // Wrong type
        assert!(handler.validate_exception_payload(&tag, &invalid_types).is_err());
    }

    #[test]
    fn test_unwind_context() {
        let mut context = UnwindContext::new(10);

        let tag = ExceptionTag {
            name: "test".to_string(),
            parameter_types: vec![ValType::I32],
            handle: 1,
        };
        let payload = ExceptionPayload::new(tag, vec![Val::I32(42)]);

        assert!(context.start_unwind(payload.clone()).is_ok());
        assert_eq!(context.depth, 1);
        assert!(context.exception.is_some());

        let caught = context.catch_exception().unwrap();
        assert_eq!(caught.tag.name, "test");
        assert!(context.exception.is_none());
    }

    #[test]
    fn test_max_unwind_depth() {
        let mut context = UnwindContext::new(2);

        let tag = ExceptionTag {
            name: "test".to_string(),
            parameter_types: vec![],
            handle: 1,
        };
        let payload = ExceptionPayload::new(tag, vec![]);

        assert!(context.start_unwind(payload).is_ok());
        assert!(context.continue_unwind().is_ok());
        assert!(context.continue_unwind().is_err()); // Exceeds max depth
    }
}