//! JNI bindings for Engine operations

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jlongArray};
use jni::JNIEnv;

use crate::engine::core;
use crate::error::{jni_utils, WasmtimeError};
use crate::ffi_common::parameter_conversion;

/// Create a new Wasmtime engine with default configuration (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngine(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || core::create_engine()) as jlong
}

/// Create a new Wasmtime engine with custom configuration (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineWithConfig(
    mut env: JNIEnv,
    _class: JClass,
    strategy: jint,
    opt_level: jint,
    debug_info: jboolean,
    wasm_threads: jboolean,
    wasm_simd: jboolean,
    wasm_reference_types: jboolean,
    wasm_bulk_memory: jboolean,
    wasm_multi_value: jboolean,
    fuel_enabled: jboolean,
    max_memory_pages: jint,
    max_stack_size: jint,
    epoch_interruption: jboolean,
    max_instances: jint,
    async_support: jboolean,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let strategy_opt = parameter_conversion::convert_strategy(strategy);
        let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
        let max_memory_pages_opt =
            parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
        let max_stack_size_opt =
            parameter_conversion::convert_int_to_optional_usize(max_stack_size);
        let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);

        core::create_engine_with_config(
            strategy_opt,
            opt_level_opt,
            parameter_conversion::convert_int_to_bool(debug_info as i32),
            parameter_conversion::convert_int_to_bool(wasm_threads as i32),
            parameter_conversion::convert_int_to_bool(wasm_simd as i32),
            parameter_conversion::convert_int_to_bool(wasm_reference_types as i32),
            parameter_conversion::convert_int_to_bool(wasm_bulk_memory as i32),
            parameter_conversion::convert_int_to_bool(wasm_multi_value as i32),
            parameter_conversion::convert_int_to_bool(fuel_enabled as i32),
            max_memory_pages_opt,
            max_stack_size_opt,
            parameter_conversion::convert_int_to_bool(epoch_interruption as i32),
            max_instances_opt,
            parameter_conversion::convert_int_to_bool(async_support as i32),
        )
    }) as jlong
}

/// Create a new Wasmtime engine with extended configuration including GC and memory options (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineWithExtendedConfig(
    mut env: JNIEnv,
    _class: JClass,
    strategy: jint,
    opt_level: jint,
    debug_info: jboolean,
    wasm_threads: jboolean,
    wasm_simd: jboolean,
    wasm_reference_types: jboolean,
    wasm_bulk_memory: jboolean,
    wasm_multi_value: jboolean,
    fuel_enabled: jboolean,
    max_memory_pages: jint,
    max_stack_size: jint,
    epoch_interruption: jboolean,
    max_instances: jint,
    async_support: jboolean,
    // GC configuration
    wasm_gc: jboolean,
    wasm_function_references: jboolean,
    wasm_exceptions: jboolean,
    // Memory configuration (0 = use default)
    memory_reservation: jlong,
    memory_guard_size: jlong,
    memory_reservation_for_growth: jlong,
    // Additional features
    wasm_tail_call: jboolean,
    wasm_relaxed_simd: jboolean,
    wasm_multi_memory: jboolean,
    wasm_memory64: jboolean,
    wasm_extended_const: jboolean,
    wasm_component_model: jboolean,
    coredump_on_trap: jboolean,
    cranelift_nan_canonicalization: jboolean,
    // Experimental features
    wasm_custom_page_sizes: jboolean,
    wasm_wide_arithmetic: jboolean,
    // Profiling and debug
    profiling_strategy: jint,
    native_unwind_info: jboolean,
    // Extended config options
    cranelift_debug_verifier: jboolean,
    async_stack_size: jlong,
    memory_may_move: jboolean,
    guard_before_linear_memory: jboolean,
    parallel_compilation: jboolean,
    pooling_allocator: jboolean,
    table_lazy_init: jboolean,
    relaxed_simd_deterministic: jboolean,
    memory_init_cow: jboolean,
    async_stack_zeroing: jboolean,
    gc_support: jboolean,
    // Cranelift flags (JSON string, nullable)
    cranelift_flags_json: JString,
    // Module version: 0=WasmtimeVersion, 1=None, 2=Custom
    module_version_strategy: jint,
    module_version_custom: JString,
) -> jlong {
    // Extract JNI strings before entering the closure (to avoid borrow issues)
    let cranelift_flags_str: String = if cranelift_flags_json.is_null() {
        String::new()
    } else {
        match env.get_string(&cranelift_flags_json) {
            Ok(s) => s.into(),
            Err(_) => String::new(),
        }
    };

    let module_version_custom_str: Option<String> = if module_version_custom.is_null() {
        None
    } else {
        match env.get_string(&module_version_custom) {
            Ok(s) => {
                let s: String = s.into();
                if s.is_empty() { None } else { Some(s) }
            }
            Err(_) => None,
        }
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let strategy_opt = parameter_conversion::convert_strategy(strategy);
        let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
        let max_memory_pages_opt =
            parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
        let max_stack_size_opt =
            parameter_conversion::convert_int_to_optional_usize(max_stack_size);
        let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);

        // Memory config: 0 means use default
        let memory_reservation_opt = if memory_reservation > 0 {
            Some(memory_reservation as u64)
        } else {
            None
        };
        let memory_guard_size_opt = if memory_guard_size > 0 {
            Some(memory_guard_size as u64)
        } else {
            None
        };
        let memory_reservation_for_growth_opt = if memory_reservation_for_growth > 0 {
            Some(memory_reservation_for_growth as u64)
        } else {
            None
        };

        let async_stack_size_opt = if async_stack_size > 0 {
            Some(async_stack_size as usize)
        } else {
            None
        };

        // Parse cranelift flags from JSON string
        let cranelift_flags_vec = parse_cranelift_flags_json(&cranelift_flags_str);

        core::create_engine_with_extended_config(
            strategy_opt,
            opt_level_opt,
            parameter_conversion::convert_int_to_bool(debug_info as i32),
            parameter_conversion::convert_int_to_bool(wasm_threads as i32),
            parameter_conversion::convert_int_to_bool(wasm_simd as i32),
            parameter_conversion::convert_int_to_bool(wasm_reference_types as i32),
            parameter_conversion::convert_int_to_bool(wasm_bulk_memory as i32),
            parameter_conversion::convert_int_to_bool(wasm_multi_value as i32),
            parameter_conversion::convert_int_to_bool(fuel_enabled as i32),
            max_memory_pages_opt,
            max_stack_size_opt,
            parameter_conversion::convert_int_to_bool(epoch_interruption as i32),
            max_instances_opt,
            parameter_conversion::convert_int_to_bool(async_support as i32),
            // GC configuration
            parameter_conversion::convert_int_to_bool(wasm_gc as i32),
            parameter_conversion::convert_int_to_bool(wasm_function_references as i32),
            parameter_conversion::convert_int_to_bool(wasm_exceptions as i32),
            // Memory configuration
            memory_reservation_opt,
            memory_guard_size_opt,
            memory_reservation_for_growth_opt,
            // Additional features
            parameter_conversion::convert_int_to_bool(wasm_tail_call as i32),
            parameter_conversion::convert_int_to_bool(wasm_relaxed_simd as i32),
            parameter_conversion::convert_int_to_bool(wasm_multi_memory as i32),
            parameter_conversion::convert_int_to_bool(wasm_memory64 as i32),
            parameter_conversion::convert_int_to_bool(wasm_extended_const as i32),
            parameter_conversion::convert_int_to_bool(wasm_component_model as i32),
            parameter_conversion::convert_int_to_bool(coredump_on_trap as i32),
            parameter_conversion::convert_int_to_bool(cranelift_nan_canonicalization as i32),
            // Experimental features
            parameter_conversion::convert_int_to_bool(wasm_custom_page_sizes as i32),
            parameter_conversion::convert_int_to_bool(wasm_wide_arithmetic as i32),
            // Profiling and debug
            parameter_conversion::convert_profiling_strategy(profiling_strategy),
            parameter_conversion::convert_int_to_bool(native_unwind_info as i32),
            // Extended config
            parameter_conversion::convert_int_to_bool(cranelift_debug_verifier as i32),
            async_stack_size_opt,
            parameter_conversion::convert_int_to_bool(memory_may_move as i32),
            parameter_conversion::convert_int_to_bool(guard_before_linear_memory as i32),
            parameter_conversion::convert_int_to_bool(parallel_compilation as i32),
            parameter_conversion::convert_int_to_bool(pooling_allocator as i32),
            parameter_conversion::convert_int_to_bool(table_lazy_init as i32),
            parameter_conversion::convert_int_to_bool(relaxed_simd_deterministic as i32),
            parameter_conversion::convert_int_to_bool(memory_init_cow as i32),
            parameter_conversion::convert_int_to_bool(async_stack_zeroing as i32),
            parameter_conversion::convert_int_to_bool(gc_support as i32),
            // Cranelift flags
            &cranelift_flags_vec,
            // Module version
            module_version_strategy,
            module_version_custom_str.as_deref(),
        )
    }) as jlong
}

/// Parse cranelift flags from a JSON string like {"key":"value","key2":"value2"}
fn parse_cranelift_flags_json(json_str: &str) -> Vec<(String, String)> {
    let trimmed = json_str.trim();
    if trimmed.is_empty() || trimmed == "{}" {
        return Vec::new();
    }

    let inner = trimmed
        .strip_prefix('{')
        .and_then(|s| s.strip_suffix('}'))
        .unwrap_or("");

    if inner.is_empty() {
        return Vec::new();
    }

    let mut flags = Vec::new();
    for pair in inner.split(',') {
        let pair = pair.trim();
        if let Some((key, value)) = pair.split_once(':') {
            let key = key.trim().trim_matches('"');
            let value = value.trim().trim_matches('"');
            if !key.is_empty() {
                flags.push((key.to_string(), value.to_string()));
            }
        }
    }

    flags
}

/// Create a new engine from JSON configuration (JNI version)
///
/// This is the preferred engine creation method that accepts all configuration
/// options as a JSON byte array, avoiding the 44-parameter positional approach.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineFromJsonConfig(
    mut env: JNIEnv,
    _class: JClass,
    json_bytes: jni::objects::JByteArray,
) -> jlong {
    // Convert byte array before the closure to avoid borrow conflict
    let bytes = match env.convert_byte_array(&json_bytes) {
        Ok(b) => b,
        Err(_) => return 0,
    };

    jni_utils::jni_try_ptr(&mut env, || {
        core::create_engine_from_json_config(&bytes)
    }) as jlong
}

/// Detect if a host CPU feature is available
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDetectHostFeature(
    mut env: JNIEnv,
    _class: JClass,
    feature_name: JString,
) -> jboolean {
    use jni::sys::{JNI_FALSE, JNI_TRUE};

    // Convert Java string first (outside the closure to avoid borrow issues)
    let feature_str: String = match env.get_string(&feature_name) {
        Ok(s) => s.into(),
        Err(_) => return JNI_FALSE as jboolean,
    };

    let result = core::detect_host_feature(&feature_str);
    if result {
        JNI_TRUE as jboolean
    } else {
        JNI_FALSE as jboolean
    }
}

/// Destroy a Wasmtime engine (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDestroyEngine(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) {
    unsafe {
        core::destroy_engine(engine_ptr as *mut std::os::raw::c_void);
    }
}

/// Clear the global handle registries for memory and store validation (JNI version)
///
/// This is intended for test cleanup to prevent stale handles from causing
/// validation failures in subsequent tests.
///
/// # Returns
/// 0 on success, -1 on failure
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeClearHandleRegistries(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    match crate::memory::core::clear_handle_registries() {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Query if epoch interruption is enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsEpochInterruptionEnabled(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.epoch_interruption_enabled())
    }) as jboolean
}

/// Query if coredump generation on trap is enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsCoredumpOnTrapEnabled(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.coredump_on_trap())
    }) as jboolean
}

/// Query if fuel consumption is enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsFuelEnabled(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.fuel_enabled())
    }) as jboolean
}

/// Query if async support is enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsAsync(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.async_support_enabled())
    }) as jboolean
}

/// Check if two engines are the same (share the same underlying Wasmtime engine)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeEngineSame(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr1: jlong,
    engine_ptr2: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let engine1 = unsafe { core::get_engine_ref(engine_ptr1 as *const std::os::raw::c_void)? };
        let engine2 = unsafe { core::get_engine_ref(engine_ptr2 as *const std::os::raw::c_void)? };
        Ok(engine1.same(engine2))
    }) as jboolean
}

/// Increment the epoch counter for epoch-based interruption
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIncrementEpoch(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        engine.increment_epoch();
        Ok(())
    });
}

/// Get stack size limit in bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetStackSizeLimit(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlong {
    jni_utils::jni_try(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.stack_size_limit().unwrap_or(0) as jlong)
    })
    .1
}

/// Get memory limit in pages (64KB per page)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetMemoryLimitPages(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jint {
    jni_utils::jni_try(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        Ok(engine.memory_limit_pages().unwrap_or(0) as jint)
    })
    .1
}

/// Query if a specific WebAssembly feature is supported (by feature name)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSupportsFeature(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    feature_name: JString,
) -> jboolean {
    // Convert Java string first (outside the closure to avoid borrow issues)
    let feature_str: String = match env.get_string(&feature_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

        // Parse feature name to WasmFeature enum
        // Note: Some Java enum names have aliases for compatibility
        let feature = match feature_str.as_str() {
            "THREADS" => crate::engine::WasmFeature::Threads,
            "REFERENCE_TYPES" => crate::engine::WasmFeature::ReferenceTypes,
            "SIMD" => crate::engine::WasmFeature::Simd,
            "BULK_MEMORY" => crate::engine::WasmFeature::BulkMemory,
            "MULTI_VALUE" => crate::engine::WasmFeature::MultiValue,
            "MULTI_MEMORY" => crate::engine::WasmFeature::MultiMemory,
            "TAIL_CALL" => crate::engine::WasmFeature::TailCall,
            "RELAXED_SIMD" => crate::engine::WasmFeature::RelaxedSimd,
            // Accept both Java enum name and native name
            "FUNCTION_REFERENCES" | "TYPED_FUNCTION_REFERENCES" => {
                crate::engine::WasmFeature::FunctionReferences
            }
            "GC" => crate::engine::WasmFeature::Gc,
            "EXCEPTIONS" => crate::engine::WasmFeature::Exceptions,
            "MEMORY64" => crate::engine::WasmFeature::Memory64,
            // Accept both Java enum name and native name
            "EXTENDED_CONST" | "EXTENDED_CONST_EXPRESSIONS" => {
                crate::engine::WasmFeature::ExtendedConst
            }
            "COMPONENT_MODEL" => crate::engine::WasmFeature::ComponentModel,
            "CUSTOM_PAGE_SIZES" => crate::engine::WasmFeature::CustomPageSizes,
            "WIDE_ARITHMETIC" => crate::engine::WasmFeature::WideArithmetic,
            "STACK_SWITCHING" => crate::engine::WasmFeature::StackSwitching,
            "SHARED_EVERYTHING_THREADS" => crate::engine::WasmFeature::SharedEverythingThreads,
            "COMPONENT_MODEL_ASYNC" => crate::engine::WasmFeature::ComponentModelAsync,
            "COMPONENT_MODEL_ASYNC_BUILTINS" => {
                crate::engine::WasmFeature::ComponentModelAsyncBuiltins
            }
            "COMPONENT_MODEL_ASYNC_STACKFUL" => {
                crate::engine::WasmFeature::ComponentModelAsyncStackful
            }
            "COMPONENT_MODEL_ERROR_CONTEXT" => {
                crate::engine::WasmFeature::ComponentModelErrorContext
            }
            "COMPONENT_MODEL_GC" => crate::engine::WasmFeature::ComponentModelGc,
            _ => {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown feature: {}", feature_str),
                })
            }
        };

        Ok(engine.supports_feature(feature))
    }) as jboolean
}

/// Compile WebAssembly module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileModule(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wasm_bytes: jbyteArray,
) -> jlong {
    // Extract data before moving env into jni_try_ptr
    let wasm_data_result = env
        .convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    let data = match wasm_data_result {
        Ok(data) => data,
        Err(_) => return 0 as jlong, // Return null on error
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        let byte_converter = crate::jni::module::VecByteArrayConverter::new(data);
        crate::shared_ffi::module::compile_module_shared(engine, byte_converter)
    }) as jlong
}

/// Compile WAT to module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileWat(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wat_string: JString,
) -> jlong {
    // Extract string before moving env into jni_try_ptr
    let wat_data_result =
        env.get_string(&wat_string)
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java string: {}", e),
            });

    let wat_jstr = match wat_data_result {
        Ok(jstr) => jstr,
        Err(_) => return 0 as jlong, // Return null on error
    };

    // Convert JavaStr to String immediately
    let string_converter = match crate::jni::module::JStringConverter::new(wat_jstr) {
        Ok(converter) => converter,
        Err(_) => return 0 as jlong, // Return null on error
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        crate::shared_ffi::module::compile_module_wat_shared(engine, string_converter)
    }) as jlong
}

/// Precompile module for AOT usage
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativePrecompileModule(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wasm_bytes: JByteArray,
) -> jbyteArray {
    // Get the byte array data
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    // Precompile the module
    let engine = match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
        Ok(engine) => engine,
        Err(_) => return std::ptr::null_mut(),
    };

    let precompiled = match core::precompile_module(engine, &bytes) {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };

    // Convert result to Java byte array
    match env.byte_array_from_slice(&precompiled) {
        Ok(array) => array.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Precompile a WebAssembly component for AOT usage (JNI)
#[cfg(feature = "component-model")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativePrecompileComponent(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wasm_bytes: JByteArray,
) -> jbyteArray {
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    let engine = match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
        Ok(engine) => engine,
        Err(_) => return std::ptr::null_mut(),
    };

    let precompiled = match core::precompile_component(engine, &bytes) {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };

    match env.byte_array_from_slice(&precompiled) {
        Ok(array) => array.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get pooling allocator metrics (JNI)
///
/// Returns a long[12] array with metrics, or null if pooling is not enabled.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetPoolingAllocatorMetrics(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlongArray {
    let engine = match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
        Ok(engine) => engine,
        Err(_) => return std::ptr::null_mut(),
    };

    match core::pooling_allocator_metrics(engine) {
        Some(metrics) => {
            match env.new_long_array(12) {
                Ok(array) => {
                    if env.set_long_array_region(&array, 0, &metrics).is_err() {
                        return std::ptr::null_mut();
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        None => std::ptr::null_mut(),
    }
}

/// Create a new store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateStore(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        let store = crate::store::core::create_store(engine)?;
        let store_ptr = store.as_ref() as *const _ as *const std::os::raw::c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
    }) as jlong
}

/// Set optimization level
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSetOptimizationLevel(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    level: jint,
) -> jboolean {
    // Note: Wasmtime engines are immutable after creation, so optimization level
    // cannot be changed. This method validates the request but returns false
    // to indicate the operation is not supported.
    jni_utils::jni_try_bool(&mut env, || {
        let _engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        // Validate the level parameter
        if level < 0 || level > 2 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Invalid optimization level: {}", level),
            });
        }
        // Return false to indicate optimization level cannot be changed after engine creation
        Ok(false)
    }) as jboolean
}

/// Get engine reference count for debugging (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetReferenceCount(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jint {
    match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
        Ok(engine) => core::get_reference_count(engine) as jint,
        Err(_) => -1,
    }
}

/// Check if debug info is enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsDebugInfo(
    _env: JNIEnv,
    _class: JClass,
    _engine_ptr: jlong,
) -> jboolean {
    // Return false by default
    0
}

/// Check if the engine is using Pulley interpreter (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsPulley(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jboolean {
    match unsafe { core::get_engine_ref(engine_ptr as *mut std::ffi::c_void) } {
        Ok(engine) => {
            if engine.inner().is_pulley() { 1 } else { 0 }
        }
        Err(_) => 0,
    }
}

/// Get the precompile compatibility hash for the engine (JNI version)
///
/// Returns the hash as a byte array (8 bytes, big-endian u64).
/// Uses wasmtime 41.0.1 Engine::precompile_compatibility_hash() API.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativePrecompileCompatibilityHash(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jbyteArray {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};

    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *mut std::ffi::c_void) }
            .map_err(|e| format!("Invalid engine pointer: {:?}", e))?;

        let mut hasher = DefaultHasher::new();
        engine
            .inner()
            .precompile_compatibility_hash()
            .hash(&mut hasher);
        let hash_value = hasher.finish();

        Ok::<u64, String>(hash_value)
    }));

    match result {
        Ok(Ok(hash_value)) => {
            let bytes = hash_value.to_be_bytes();
            match env.byte_array_from_slice(&bytes) {
                Ok(arr) => arr.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        _ => std::ptr::null_mut(),
    }
}

/// Create a weak reference to an engine (JNI version)
///
/// Returns a pointer to a WeakEngine, or 0 on error.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateWeakEngine(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || unsafe {
        core::create_weak_engine(engine_ptr as *const std::os::raw::c_void)
    }) as jlong
}

/// Upgrade a weak engine reference to a strong engine (JNI version)
///
/// Returns a pointer to a new Engine, or 0 if the engine has been dropped.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWeakEngine_nativeUpgrade(
    mut env: JNIEnv,
    _class: JClass,
    weak_ptr: jlong,
) -> jlong {
    let result = jni_utils::jni_try(&mut env, || unsafe {
        let opt = core::upgrade_weak_engine(weak_ptr as *const std::os::raw::c_void)?;
        match opt {
            Some(engine) => Ok(Box::into_raw(engine) as jlong),
            None => Ok(0 as jlong),
        }
    });
    result.1
}

/// Destroy a weak engine reference (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWeakEngine_nativeDestroyWeakEngine(
    _env: JNIEnv,
    _class: JClass,
    weak_ptr: jlong,
) {
    unsafe {
        core::destroy_weak_engine(weak_ptr as *mut std::os::raw::c_void);
    }
}

/// Detect if bytes are a precompiled WebAssembly module or component
///
/// Returns:
/// - -1 if not precompiled
/// - 0 if precompiled MODULE
/// - 1 if precompiled COMPONENT
/// - -2 on error
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDetectPrecompiled(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    bytes: JByteArray,
) -> jint {
    let result: Result<jint, crate::error::WasmtimeError> = (|| {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

        let byte_vec = env.convert_byte_array(bytes).map_err(|e| {
            crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert byte array: {}", e),
            }
        })?;

        match engine.detect_precompiled(&byte_vec) {
            Some(value) => Ok(value),
            None => Ok(-1),
        }
    })();

    match result {
        Ok(value) => value,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            -2 // Error indicator
        }
    }
}

/// Create a standalone shared memory from an engine (JNI version)
///
/// Shared memory does not require a Store, only an Engine with threads support enabled.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateSharedMemory(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    initial_pages: jint,
    max_pages: jint,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
        let validated_ptr =
            core::create_shared_memory(engine, initial_pages as u64, max_pages as u64)?;
        Ok(validated_ptr as jlong)
    })
}
