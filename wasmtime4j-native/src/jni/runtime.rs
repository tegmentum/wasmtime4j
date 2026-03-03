//! JNI bindings for JniWasmRuntime operations

use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jint, jlong, jstring};
use jni::JNIEnv;

use crate::error::jni_utils;
use std::ptr;

/// Create a new WebAssembly runtime (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateRuntime(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        log::debug!("Creating new JNI WebAssembly runtime");

        // For now, return a placeholder handle since we don't need a specific runtime object
        // The actual work is done by the engines and modules
        let runtime_placeholder = Box::new(0u64);

        log::debug!("Created JNI WebAssembly runtime");
        Ok(runtime_placeholder)
    }) as jlong
}

/// Create a new Wasmtime engine for the runtime (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateEngine(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeCreateEngine: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Creating new engine for runtime handle: 0x{:x}",
            runtime_handle
        );

        // Create a new engine with default configuration
        crate::engine::core::create_engine()
    }) as jlong
}

/// Compile a WebAssembly module (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCompileModule(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    wasm_bytes: jni::sys::jbyteArray,
) -> jlong {
    // Extract data before moving env into jni_try_ptr
    let wasm_data_result = env
        .convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(wasm_bytes) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    let data = match wasm_data_result {
        Ok(data) => data,
        Err(_) => return 0 as jlong, // Return null on error
    };

    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeCompileModule: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Compiling module for runtime handle: 0x{:x}, bytes length: {}",
            runtime_handle,
            data.len()
        );

        // Create a default engine for compilation
        let engine = crate::engine::core::create_engine()?;

        // Compile the module
        crate::module::core::compile_module(&engine, &data)
    }) as jlong
}

/// Instantiate a WebAssembly module (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeInstantiateModule(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    module_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeInstantiateModule: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if module_handle == 0 {
            log::error!("JNI Runtime.nativeInstantiateModule: null module handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Module handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Instantiating module 0x{:x} for runtime 0x{:x}",
            module_handle,
            runtime_handle
        );

        // Get the module reference
        let module = unsafe {
            crate::module::core::get_module_ref(module_handle as *const std::os::raw::c_void)?
        };

        // Create a default engine and store for instantiation
        let engine = crate::engine::core::create_engine()?;
        let mut store = crate::store::core::create_store(&engine)?;

        // Instantiate the module
        crate::instance::core::instantiate_module(&mut store, &module, &[])
    }) as jlong
}

/// Get the Wasmtime version string (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeGetWasmtimeVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    match env.new_string(crate::WASMTIME_VERSION) {
        Ok(version_str) => version_str.into_raw(),
        Err(e) => {
            log::error!("Failed to create Java string for Wasmtime version: {}", e);
            ptr::null_mut()
        }
    }
}

/// Destroy a WebAssembly runtime (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeDestroyRuntime(
    _env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) {
    if runtime_handle == 0 {
        log::warn!("JNI Runtime.nativeDestroyRuntime: attempt to destroy null runtime handle");
        return;
    }

    log::debug!("Destroying runtime handle: 0x{:x}", runtime_handle);

    // Clean up the runtime handle
    unsafe {
        let _runtime = Box::from_raw(runtime_handle as *mut u64);
        // The runtime object is automatically dropped here
    }

    log::debug!(
        "Successfully destroyed runtime handle: 0x{:x}",
        runtime_handle
    );
}

/// Create a new WASI context for the runtime (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateWasiContext(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeCreateWasiContext: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Creating WASI context for runtime handle: 0x{:x}",
            runtime_handle
        );

        // Create a new WASI context with default configuration
        let ctx = crate::wasi::WasiContext::new()?;
        let fd_manager = crate::wasi::WasiFileDescriptorManager::new();
        let combined = Box::new((ctx, fd_manager));

        log::debug!("Created WASI context for runtime 0x{:x}", runtime_handle);
        Ok(combined)
    }) as jlong
}

/// Create a new Linker for the runtime (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateLinker(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeCreateLinker: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if engine_handle == 0 {
            log::error!("JNI Runtime.nativeCreateLinker: null engine handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Engine handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Creating Linker for runtime 0x{:x}, engine 0x{:x}",
            runtime_handle,
            engine_handle
        );

        // Get the engine reference
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_handle as *const std::os::raw::c_void)?
        };

        // Create a new Linker with the engine
        let linker = crate::linker::Linker::new(&engine)?;

        log::debug!("Created Linker for runtime 0x{:x}", runtime_handle);
        Ok(Box::new(linker))
    }) as jlong
}

/// Create a linker with configuration options
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateLinkerWithConfig(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    engine_handle: jlong,
    allow_shadowing: jboolean,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeCreateLinkerWithConfig: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if engine_handle == 0 {
            log::error!("JNI Runtime.nativeCreateLinkerWithConfig: null engine handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Engine handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Creating Linker with config for runtime 0x{:x}, engine 0x{:x}, allow_shadowing: {}",
            runtime_handle,
            engine_handle,
            allow_shadowing != 0
        );

        // Get the engine reference
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_handle as *const std::os::raw::c_void)?
        };

        // Create LinkerConfig
        let config = crate::linker::LinkerConfig {
            enable_wasi: false,
            allow_shadowing: allow_shadowing != 0,
            max_host_functions: None,
            validate_signatures: true,
        };

        // Create a new Linker with the engine and config
        let linker = crate::linker::core::create_linker_with_config(&engine, config)?;

        log::debug!(
            "Created Linker with config for runtime 0x{:x}",
            runtime_handle
        );
        Ok(linker)
    }) as jlong
}

/// Add WASI context to a Linker (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddWasiToLinker(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    linker_handle: jlong,
    wasi_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiToLinker: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if linker_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiToLinker: null linker handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Linker handle cannot be null".to_string(),
            });
        }

        if wasi_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiToLinker: null WASI handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "WASI handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Adding WASI 0x{:x} to Linker 0x{:x} for runtime 0x{:x}",
            wasi_handle,
            linker_handle,
            runtime_handle
        );

        // Get the linker wrapper reference
        let linker_wrapper = unsafe { &mut *(linker_handle as *mut crate::linker::Linker) };

        // Get the WASI context from the raw pointer
        // The pointer points to a (WasiContext, WasiFileDescriptorManager) tuple
        let wasi_ctx_ptr = wasi_handle
            as *const (
                crate::wasi::WasiContext,
                crate::wasi::WasiFileDescriptorManager,
            );
        let wasi_tuple = unsafe { &*wasi_ctx_ptr };

        // Clone the WASI context configuration and store in linker
        let wasi_context = wasi_tuple.0.clone();
        linker_wrapper.set_wasi_context(wasi_context);

        // Call enable_wasi() which adds WASI Preview 1 imports to the linker
        linker_wrapper.enable_wasi()?;

        log::debug!(
            "WASI Preview 1 imports successfully added to Linker for runtime 0x{:x}",
            runtime_handle
        );
        Ok(0)
    })
}

/// Add WASI Preview 2 context to a Linker (JNI version).
///
/// For core WebAssembly modules, Preview 1 and Preview 2 share the same linker
/// setup via `enable_wasi()`. The separate function exists for API completeness
/// and to support future divergence when component-model-based Preview 2 differs.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddWasiPreview2ToLinker(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    linker_handle: jlong,
    wasi_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if runtime_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null runtime handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if linker_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null linker handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Linker handle cannot be null".to_string(),
            });
        }

        if wasi_handle == 0 {
            log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null WASI handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "WASI handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Adding WASI Preview2 0x{:x} to Linker 0x{:x} for runtime 0x{:x}",
            wasi_handle,
            linker_handle,
            runtime_handle
        );

        // Get the linker wrapper reference
        let linker_wrapper = unsafe { &mut *(linker_handle as *mut crate::linker::Linker) };

        // Get the WASI context from the raw pointer
        // The pointer points to a (WasiContext, WasiFileDescriptorManager) tuple
        let wasi_ctx_ptr = wasi_handle
            as *const (
                crate::wasi::WasiContext,
                crate::wasi::WasiFileDescriptorManager,
            );
        let wasi_tuple = unsafe { &*wasi_ctx_ptr };

        // Clone the WASI context configuration and store in linker
        let wasi_context = wasi_tuple.0.clone();
        linker_wrapper.set_wasi_context(wasi_context);

        // For core modules, Preview 1 and Preview 2 use the same enable_wasi() path.
        // Component-model Preview 2 uses wasmtime::component::Linker (separate API).
        linker_wrapper.enable_wasi()?;

        log::debug!(
            "WASI imports added (P1/P2 compatible for core modules) to Linker for runtime 0x{:x}",
            runtime_handle
        );
        Ok(0)
    })
}

/// Add Component Model to a Linker (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddComponentModelToLinker(
    mut env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
    linker_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if runtime_handle == 0 {
            log::error!(
                "JNI Runtime.nativeAddComponentModelToLinker: null runtime handle provided"
            );
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Runtime handle cannot be null".to_string(),
            });
        }

        if linker_handle == 0 {
            log::error!("JNI Runtime.nativeAddComponentModelToLinker: null linker handle provided");
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Linker handle cannot be null".to_string(),
            });
        }

        log::debug!(
            "Adding Component Model to Linker 0x{:x} for runtime 0x{:x}",
            linker_handle,
            runtime_handle
        );

        // Get the linker wrapper reference
        let linker_wrapper = unsafe { &mut *(linker_handle as *mut crate::linker::Linker) };

        // For core module linkers, "component model support" means having the WASI
        // imports available. The actual component model path is through ComponentLinker.
        linker_wrapper.enable_wasi()?;

        log::debug!(
            "Component Model imports successfully added to Linker for runtime 0x{:x}",
            runtime_handle
        );
        Ok(0)
    })
}

/// Deserialize a module from bytes using the runtime's engine
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeDeserializeModule(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    serialized_data: jni::sys::jbyteArray,
) -> jlong {
    // Convert byte array to Vec<u8> before moving env
    let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
        let byte_array = unsafe { JByteArray::from_raw(serialized_data) };
        let array_elements =
            unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
        let len = env.get_array_length(&byte_array)? as usize;
        let slice =
            unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
        Ok(slice.to_vec())
    })() {
        Ok(data) => data,
        Err(_) => return 0 as jlong, // Return null on error
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        let byte_converter = crate::jni::module::VecByteArrayConverter::new(data);
        crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter)
    }) as jlong
}

/// Check if WASI-NN is available (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeIsNnAvailable(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    // WASI-NN is not currently compiled into the native library
    0
}

/// Check if the component model is supported (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSupportsComponentModel(
    _env: JNIEnv,
    _class: JClass,
    _runtime_handle: jlong,
) -> jboolean {
    // Component model is compiled into the native library when the feature is enabled
    #[cfg(feature = "component-model")]
    {
        1
    }
    #[cfg(not(feature = "component-model"))]
    {
        0
    }
}

/// Create a WASI-enabled linker with the specified configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateWasiLinker(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        use crate::engine::core;
        use crate::linker::Linker as WasmtimeLinker;

        // Get engine reference
        let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

        // Create a new linker
        let mut linker = WasmtimeLinker::new(engine)?;

        // Add WASI Preview 1 imports to the linker
        // The linker will have all WASI functions defined, and they will work
        // when a store with a WASI context is used for instantiation
        linker.enable_wasi()?;
        log::debug!("Created WASI-enabled linker with WASI Preview 1 imports");

        Ok(Box::new(linker))
    }) as jlong
}
