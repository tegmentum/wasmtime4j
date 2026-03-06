//! JNI bindings for Module operations
//!
//! This module provides JNI bindings for WebAssembly module operations including
//! instantiation, introspection, serialization, and metadata access.
//!
//! It exports `VecByteArrayConverter` and `JStringConverter` for use by other modules.

use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::strings::JavaStr;
use jni::sys::{jboolean, jbyteArray, jint, jlong, jlongArray, jobject, jobjectArray, jstring};
use jni::JNIEnv;

use crate::error::{jni_utils, WasmtimeError};
use crate::module::core;
use crate::shared_ffi::module::{ByteArrayConverter, StringConverter};

/// Vec<u8> byte array converter implementation for JNI
pub struct VecByteArrayConverter {
    data: Vec<u8>,
}

impl VecByteArrayConverter {
    /// Creates a new VecByteArrayConverter
    pub fn new(data: Vec<u8>) -> Self {
        Self { data }
    }
}

impl ByteArrayConverter for VecByteArrayConverter {
    unsafe fn get_bytes(&self) -> crate::error::WasmtimeResult<&[u8]> {
        Ok(&self.data)
    }

    fn len(&self) -> usize {
        self.data.len()
    }
}

/// String converter implementation for JNI
pub struct JStringConverter {
    data: String,
}

impl JStringConverter {
    /// Creates a new JStringConverter from JavaStr
    pub fn new(java_str: JavaStr) -> crate::error::WasmtimeResult<Self> {
        let string = java_str.to_str().map(|s| s.to_string()).map_err(|e| {
            crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java string to Rust string: {}", e),
            }
        })?;
        Ok(Self { data: string })
    }
}

impl StringConverter for JStringConverter {
    unsafe fn get_string(&self) -> crate::error::WasmtimeResult<String> {
        Ok(self.data.clone())
    }

    fn is_empty(&self) -> bool {
        self.data.is_empty()
    }
}

/// Instantiate a module within a store context
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModule(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    store_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
        let store =
            unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        crate::instance::core::create_instance(store, module)
    }) as jlong
}

/// Instantiate a module with specific imports
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModuleWithImports(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    store_ptr: jlong,
    _import_map_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
        let store =
            unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
        // For now, ignore imports - this would need proper ImportMap implementation
        crate::instance::core::create_instance(store, module)
    }) as jlong
}

/// Get the names of functions exported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedFunctions(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let exports = core::get_function_exports(module);
            let mut function_names = Vec::new();

            for export in exports {
                function_names.push(export.name.as_str());
            }

            // Convert to Java String array
            match env.new_object_array(
                function_names.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, name) in function_names.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(name) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the names of memories exported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedMemories(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let exports = core::get_memory_exports(module);
            let mut memory_names = Vec::new();

            for export in exports {
                memory_names.push(export.name.as_str());
            }

            // Convert to Java String array
            match env.new_object_array(
                memory_names.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, name) in memory_names.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(name) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the names of tables exported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedTables(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let metadata = core::get_metadata(module);
            let mut table_names = Vec::new();

            for export in &metadata.exports {
                if matches!(
                    export.export_type,
                    crate::module::ExportKind::Table(_, _, _)
                ) {
                    table_names.push(export.name.as_str());
                }
            }

            // Convert to Java String array
            match env.new_object_array(
                table_names.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, name) in table_names.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(name) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the names of globals exported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedGlobals(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let metadata = core::get_metadata(module);
            let mut global_names = Vec::new();

            for export in &metadata.exports {
                if matches!(export.export_type, crate::module::ExportKind::Global(_, _)) {
                    global_names.push(export.name.as_str());
                }
            }

            // Convert to Java String array
            match env.new_object_array(
                global_names.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, name) in global_names.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(name) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the names of functions imported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetImportedFunctions(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let imports = core::get_required_imports(module);
            let mut function_names = Vec::new();

            for import in imports {
                if matches!(import.import_type, crate::module::ImportKind::Function(_)) {
                    let full_name = format!("{}::{}", import.module, import.name);
                    function_names.push(full_name);
                }
            }

            // Convert to Java String array
            match env.new_object_array(
                function_names.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, name) in function_names.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(&name) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Validate WebAssembly bytecode without compiling
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeValidateModule(
    mut env: JNIEnv,
    _class: JClass,
    bytecode: jbyteArray,
) -> jboolean {
    // Extract data first
    let wasm_data_result = env
        .convert_byte_array(unsafe { JByteArray::from_raw(bytecode) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    let data = match wasm_data_result {
        Ok(data) => data,
        Err(_) => return 0, // Invalid on error
    };

    let byte_converter = VecByteArrayConverter::new(data);
    match crate::shared_ffi::module::validate_module_shared(byte_converter) {
        Ok(()) => 1, // Valid
        Err(_) => 0, // Invalid
    }
}

/// Get the size of a compiled module in bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleSize(
    _env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jlong {
    let result =
        crate::shared_ffi::module::get_module_size_shared(module_ptr as *mut std::os::raw::c_void);
    let (_, size) = crate::shared_ffi::module::size_result_to_ffi_result(result);
    size as jlong
}

/// Get comprehensive export metadata for a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportMetadata(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let metadata = core::get_metadata(module);
            let mut export_data = Vec::new();

            for export in &metadata.exports {
                let type_str = match &export.export_type {
                    crate::module::ExportKind::Function(_) => "function",
                    crate::module::ExportKind::Global(_, _) => "global",
                    crate::module::ExportKind::Memory(_, _, _, _, _) => "memory",
                    crate::module::ExportKind::Table(_, _, _) => "table",
                    crate::module::ExportKind::Tag(_) => "tag",
                };

                let entry = format!("{}|{}", export.name, type_str);
                export_data.push(entry);
            }

            // Convert to Java String array
            match env.new_object_array(
                export_data.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, data) in export_data.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(data) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get comprehensive import metadata for a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetImportMetadata(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            let imports = core::get_required_imports(module);
            let mut import_data = Vec::new();

            for import in imports {
                let type_str = match &import.import_type {
                    crate::module::ImportKind::Function(_) => "function",
                    crate::module::ImportKind::Global(_, _) => "global",
                    crate::module::ImportKind::Memory(_, _, _, _, _) => "memory",
                    crate::module::ImportKind::Table(_, _, _) => "table",
                    crate::module::ImportKind::Tag(_) => "tag",
                };

                let entry = format!("{}|{}|{}", import.module, import.name, type_str);
                import_data.push(entry);
            }

            // Convert to Java String array
            match env.new_object_array(
                import_data.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, data) in import_data.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(data) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the name of a module if it has one
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleName(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jstring {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(module) => {
            if let Some(name) = core::get_module_name(module) {
                match env.new_string(name) {
                    Ok(jstr) => jstr.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            } else {
                std::ptr::null_mut()
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get WebAssembly features supported by a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleFeatures(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(_module) => {
            // For now, return empty array - feature detection would need more sophisticated analysis
            let features: Vec<String> = vec![];

            // Convert to Java String array
            match env.new_object_array(
                features.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, feature) in features.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(feature) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get module linking information
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleLinkingInfo(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobjectArray {
    match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
        Ok(_module) => {
            // For now, return empty array - linking info would need more sophisticated analysis
            let linking_info: Vec<String> = vec![];

            // Convert to Java String array
            match env.new_object_array(
                linking_info.len() as i32,
                "java/lang/String",
                JString::default(),
            ) {
                Ok(array) => {
                    for (i, info) in linking_info.iter().enumerate() {
                        if let Ok(jstr) = env.new_string(info) {
                            let _ = env.set_object_array_element(&array, i as i32, jstr);
                        }
                    }
                    array.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Serialize a compiled module to bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeSerializeModule(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jbyteArray {
    match crate::shared_ffi::module::serialize_module_shared(
        module_ptr as *mut std::os::raw::c_void,
    ) {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(jarray) => jarray.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Deserialize a module from bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDeserializeModule(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    serialized_data: jbyteArray,
) -> jlong {
    // Convert byte array to Vec<u8> before moving env
    let data = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<Vec<u8>, jni::errors::Error> {
        let byte_array = unsafe { JByteArray::from_raw(serialized_data) };
        let array_elements =
            unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
        let len = env.get_array_length(&byte_array)? as usize;
        let slice =
            unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
        Ok(slice.to_vec())
    })) {
        Ok(Ok(data)) => data,
        Ok(Err(_)) => return 0 as jlong,
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0 as jlong;
        }
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        let byte_converter = VecByteArrayConverter::new(data);
        crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter)
    }) as jlong
}

/// Deserialize a module from a file using memory-mapped I/O
///
/// This is more efficient than reading the file first for large modules.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDeserializeModuleFile(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    path: JString,
) -> jlong {
    // Convert JString path to Rust string before moving env
    let path_str = match env.get_string(&path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0 as jlong,
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        crate::shared_ffi::module::deserialize_module_file_shared(engine, &path_str)
    }) as jlong
}

/// Load a module from a trusted file (skips validation).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeFromTrustedFile(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    path: JString,
) -> jlong {
    let path_str = match env.get_string(&path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0 as jlong,
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        let module = crate::module::Module::from_trusted_file(engine, &path_str)?;
        Ok(Box::new(module))
    }) as jlong
}

/// Deserialize a module from raw bytes (no file format wrapper).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDeserializeRaw(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    serialized_data: jbyteArray,
) -> jlong {
    let data = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<Vec<u8>, jni::errors::Error> {
        let byte_array = unsafe { JByteArray::from_raw(serialized_data) };
        let array_elements =
            unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
        let len = env.get_array_length(&byte_array)? as usize;
        let slice =
            unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
        Ok(slice.to_vec())
    })) {
        Ok(Ok(data)) => data,
        Ok(Err(_)) => return 0 as jlong,
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0 as jlong;
        }
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        let module = crate::module::Module::deserialize_raw(engine, &data)?;
        Ok(Box::new(module))
    }) as jlong
}

/// Deserialize a module from an open file descriptor.
#[cfg(unix)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDeserializeOpenFile(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    fd: jint,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        let module = crate::module::Module::deserialize_open_file(engine, fd)?;
        Ok(Box::new(module))
    }) as jlong
}

/// Create a native import map from serialized data
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeCreateImportMap(
    _env: JNIEnv,
    _class: JClass,
    _store_ptr: jlong,
    _import_data: jbyteArray,
) -> jlong {
    // For now, return 0 - proper ImportMap implementation would be needed
    0
}

/// Destroy a native import map
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyImportMap(
    _env: JNIEnv,
    _class: JClass,
    _import_map_ptr: jlong,
) {
    // For now, do nothing - proper ImportMap implementation would be needed
}

/// Compile a WebAssembly module from a file path
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeCompileFromFile(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    path: JString,
) -> jlong {
    // Convert JString path to Rust string before moving env
    let path_str = match env.get_string(&path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0 as jlong,
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };
        core::compile_module_from_file(engine, std::path::Path::new(&path_str))
    }) as jlong
}

/// Check if two modules are the same underlying compiled module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeModuleSame(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr1: jlong,
    module_ptr2: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        if module_ptr1 == 0 || module_ptr2 == 0 {
            return Ok(false);
        }
        unsafe {
            core::modules_same(
                module_ptr1 as *const std::os::raw::c_void,
                module_ptr2 as *const std::os::raw::c_void,
            )
        }
    }) as jboolean
}

/// Get the index of an export by name
/// Returns -1 if the export is not found
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportIndex(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    export_name: JString,
) -> jlong {
    if module_ptr == 0 {
        return -1;
    }

    let name = match env.get_string(&export_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return -1,
    };

    jni_utils::jni_try_with_default(&mut env, -1, || {
        let idx = unsafe { core::get_export_index(module_ptr as *const std::os::raw::c_void, &name)? };
        Ok(idx as jlong)
    })
}

/// Get a ModuleExport handle for O(1) export lookups (JNI version)
///
/// Returns a native pointer to a boxed wasmtime::ModuleExport, or 0 if not found.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleExport(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    export_name: JString,
) -> jlong {
    if module_ptr == 0 {
        return 0;
    }

    let name = match env.get_string(&export_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let ptr = unsafe {
            core::get_wasmtime_module_export(module_ptr as *const std::os::raw::c_void, &name)?
        };
        Ok(ptr as jlong)
    })
}

/// Destroy a ModuleExport handle (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModuleExport(
    mut env: JNIEnv,
    _class: JClass,
    module_export_ptr: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if module_export_ptr != 0 {
            unsafe {
                core::destroy_module_export(module_export_ptr as *mut std::os::raw::c_void);
            }
        }
        Ok(())
    });
}

/// Get compiled machine code text from module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleText(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jbyteArray {
    if module_ptr == 0 {
        return std::ptr::null_mut();
    }

    let text = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Vec<u8>> {
        let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
        Ok(core::get_module_text(module))
    })) {
        Ok(Ok(t)) => t,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    match env.byte_array_from_slice(&text) {
        Ok(jarray) => jarray.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get module address map as a long array of pairs [codeOffset, wasmOffset, ...]
/// wasmOffset is -1 when there is no corresponding wasm offset.
/// Returns null if address map is not available.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleAddressMap(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jlongArray {
    if module_ptr == 0 {
        return std::ptr::null_mut();
    }

    let entries_opt = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Option<Vec<(usize, Option<u32>)>>> {
        let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
        Ok(core::get_module_address_map(module))
    })) {
        Ok(Ok(opt)) => opt,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    match entries_opt {
        Some(entries) => {
            // Pack as interleaved pairs: [code0, wasm0, code1, wasm1, ...]
            let mut flat = Vec::with_capacity(entries.len() * 2);
            for (code_offset, wasm_offset) in entries {
                flat.push(code_offset as i64);
                flat.push(wasm_offset.map(|o| o as i64).unwrap_or(-1));
            }
            match env.new_long_array(flat.len() as i32) {
                Ok(arr) => {
                    let _ = env.set_long_array_region(&arr, 0, &flat);
                    arr.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        None => std::ptr::null_mut(),
    }
}

/// Destroy a module
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModule(
    _env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) {
    crate::shared_ffi::module::destroy_module_shared(module_ptr as *mut std::os::raw::c_void);
}

/// Helper: Convert ModuleValueType to WasmValueType enum name
fn module_value_type_to_java_enum(value_type: &crate::module::ModuleValueType) -> &'static str {
    use crate::module::ModuleValueType;
    match value_type {
        ModuleValueType::I32 => "I32",
        ModuleValueType::I64 => "I64",
        ModuleValueType::F32 => "F32",
        ModuleValueType::F64 => "F64",
        ModuleValueType::V128 => "V128",
        ModuleValueType::FuncRef => "FUNCREF",
        ModuleValueType::ExternRef => "EXTERNREF",
        // WasmGC reference types
        ModuleValueType::AnyRef => "ANYREF",
        ModuleValueType::EqRef => "EQREF",
        ModuleValueType::I31Ref => "I31REF",
        ModuleValueType::StructRef => "STRUCTREF",
        ModuleValueType::ArrayRef => "ARRAYREF",
        ModuleValueType::NullRef => "NULLREF",
        ModuleValueType::NullFuncRef => "NULLFUNCREF",
        ModuleValueType::NullExternRef => "NULLEXTERNREF",
    }
}

/// Helper: Get WasmValueType enum value
fn get_wasm_value_type_enum<'a>(
    env: &mut JNIEnv<'a>,
    value_type: &crate::module::ModuleValueType,
) -> Result<JObject<'a>, String> {
    let enum_class = env
        .find_class("ai/tegmentum/wasmtime4j/WasmValueType")
        .map_err(|e| format!("Failed to find WasmValueType class: {}", e))?;

    let enum_name = module_value_type_to_java_enum(value_type);
    let enum_value = env
        .get_static_field(
            enum_class,
            enum_name,
            "Lai/tegmentum/wasmtime4j/WasmValueType;",
        )
        .map_err(|e| format!("Failed to get enum value {}: {}", enum_name, e))?;

    match enum_value {
        jni::objects::JValueGen::Object(obj) => Ok(obj),
        _ => Err("Unexpected JValue type for enum".to_string()),
    }
}

/// Helper: Create JniFuncType from parameter and return types
fn create_jni_func_type<'a>(
    env: &mut JNIEnv<'a>,
    params: &[crate::module::ModuleValueType],
    returns: &[crate::module::ModuleValueType],
) -> Option<JObject<'a>> {
    // Create ArrayList for params
    let list_class = env.find_class("java/util/ArrayList").ok()?;
    let params_list = env.new_object(&list_class, "()V", &[]).ok()?;
    let results_list = env.new_object(&list_class, "()V", &[]).ok()?;

    // Add param types
    for param in params {
        if let Ok(enum_val) = get_wasm_value_type_enum(env, param) {
            let _ = env.call_method(
                &params_list,
                "add",
                "(Ljava/lang/Object;)Z",
                &[JValue::Object(&enum_val)],
            );
        }
    }

    // Add return types
    for ret in returns {
        if let Ok(enum_val) = get_wasm_value_type_enum(env, ret) {
            let _ = env.call_method(
                &results_list,
                "add",
                "(Ljava/lang/Object;)Z",
                &[JValue::Object(&enum_val)],
            );
        }
    }

    // Create JniFuncType
    let func_type_class = env
        .find_class("ai/tegmentum/wasmtime4j/jni/type/JniFuncType")
        .ok()?;
    env.new_object(
        func_type_class,
        "(Ljava/util/List;Ljava/util/List;)V",
        &[JValue::Object(&params_list), JValue::Object(&results_list)],
    )
    .ok()
}

/// Helper: Create JniGlobalType from value type and mutability
fn create_jni_global_type<'a>(
    env: &mut JNIEnv<'a>,
    val_type: &crate::module::ModuleValueType,
    mutable: bool,
) -> Option<JObject<'a>> {
    let enum_val = get_wasm_value_type_enum(env, val_type).ok()?;
    let global_type_class = env
        .find_class("ai/tegmentum/wasmtime4j/jni/type/JniGlobalType")
        .ok()?;
    env.new_object(
        global_type_class,
        "(Lai/tegmentum/wasmtime4j/WasmValueType;Z)V",
        &[JValue::Object(&enum_val), JValue::Bool(mutable as jboolean)],
    )
    .ok()
}

/// Helper: Create JniMemoryType from initial, max, is_64, and shared
fn create_jni_memory_type<'a>(
    env: &mut JNIEnv<'a>,
    initial: u64,
    max: Option<u64>,
    is_64: bool,
    shared: bool,
) -> Option<JObject<'a>> {
    let memory_type_class = env
        .find_class("ai/tegmentum/wasmtime4j/jni/type/JniMemoryType")
        .ok()?;

    // Create boxed Long for maximum, or null if None
    let max_obj: JObject = if let Some(max_val) = max {
        let long_class = env.find_class("java/lang/Long").ok()?;
        env.new_object(long_class, "(J)V", &[JValue::Long(max_val as i64)])
            .ok()?
    } else {
        JObject::null()
    };

    // JniMemoryType(long minimum, Long maximum, boolean is64Bit, boolean isShared)
    env.new_object(
        memory_type_class,
        "(JLjava/lang/Long;ZZ)V",
        &[
            JValue::Long(initial as i64),
            JValue::Object(&max_obj),
            JValue::Bool(is_64 as jboolean),
            JValue::Bool(shared as jboolean),
        ],
    )
    .ok()
}

/// Helper: Create JniTableType from element type, initial, and max
fn create_jni_table_type<'a>(
    env: &mut JNIEnv<'a>,
    elem_type: &crate::module::ModuleValueType,
    initial: u32,
    max: Option<u32>,
) -> Option<JObject<'a>> {
    let enum_val = get_wasm_value_type_enum(env, elem_type).ok()?;
    let table_type_class = env
        .find_class("ai/tegmentum/wasmtime4j/jni/type/JniTableType")
        .ok()?;

    // Create boxed Long for maximum using valueOf (null if None)
    let max_obj: JObject = match max {
        Some(m) => {
            let long_class = env.find_class("java/lang/Long").ok()?;
            // Use Long.valueOf(long) instead of constructor (more compatible)
            let result = env
                .call_static_method(
                    long_class,
                    "valueOf",
                    "(J)Ljava/lang/Long;",
                    &[JValue::Long(m as i64)],
                )
                .ok()?;
            match result {
                jni::objects::JValueGen::Object(obj) => obj,
                _ => return None,
            }
        }
        None => JObject::null(),
    };

    // JniTableType constructor: (WasmValueType elementType, long minimum, Long maximum)
    env.new_object(
        table_type_class,
        "(Lai/tegmentum/wasmtime4j/WasmValueType;JLjava/lang/Long;)V",
        &[
            JValue::Object(&enum_val),
            JValue::Long(initial as i64),
            JValue::Object(&max_obj),
        ],
    )
    .ok()
}

/// Get module exports with type information
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleExports(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobject {
    if module_ptr == 0 {
        return std::ptr::null_mut();
    }

    // Extract export data with type information (panic-safe)
    let exports_data: Vec<(String, crate::module::ExportKind)> = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Vec<(String, crate::module::ExportKind)>> {
        let module = unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)?
        };
        let metadata = crate::module::core::get_metadata(module);
        Ok(metadata
            .exports
            .iter()
            .map(|exp| (exp.name.clone(), exp.export_type.clone()))
            .collect())
    })) {
        Ok(Ok(data)) => data,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    // Create ArrayList
    let array_list_class = match env.find_class("java/util/ArrayList") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    let array_list = match env.new_object(array_list_class, "()V", &[]) {
        Ok(list) => list,
        Err(_) => return std::ptr::null_mut(),
    };

    // For each export, create ExportType object
    for (export_name, export_kind) in &exports_data {
        // Create export name string
        let name_jstring = match env.new_string(export_name) {
            Ok(s) => s,
            Err(_) => continue,
        };

        // Create WasmType based on export kind
        let wasm_type_obj = match export_kind {
            crate::module::ExportKind::Function(sig) => {
                create_jni_func_type(&mut env, &sig.params, &sig.returns)
            }
            crate::module::ExportKind::Global(val_type, mutable) => {
                create_jni_global_type(&mut env, val_type, *mutable)
            }
            crate::module::ExportKind::Memory(initial, max, is_64, shared, _page_size_log2) => {
                create_jni_memory_type(&mut env, *initial, *max, *is_64, *shared)
            }
            crate::module::ExportKind::Table(elem_type, initial, max) => {
                create_jni_table_type(&mut env, elem_type, *initial, *max)
            }
            crate::module::ExportKind::Tag(sig) => {
                create_jni_func_type(&mut env, &sig.params, &sig.returns)
            }
        };

        let wasm_type_obj = match wasm_type_obj {
            Some(obj) => obj,
            None => continue,
        };

        // Create ExportType(String name, WasmType type)
        let export_type_class = match env.find_class("ai/tegmentum/wasmtime4j/type/ExportType") {
            Ok(c) => c,
            Err(_) => continue,
        };
        let export_type_obj = match env.new_object(
            export_type_class,
            "(Ljava/lang/String;Lai/tegmentum/wasmtime4j/type/WasmType;)V",
            &[
                JValue::Object(&name_jstring),
                JValue::Object(&wasm_type_obj),
            ],
        ) {
            Ok(obj) => obj,
            Err(_) => continue,
        };

        // Add ExportType directly to ArrayList
        let _ = env.call_method(
            &array_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&export_type_obj)],
        );
    }

    array_list.into_raw()
}

/// Get module imports with type information
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleImports(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jobject {
    if module_ptr == 0 {
        return std::ptr::null_mut();
    }

    // Extract import data with type information (panic-safe)
    let imports_data: Vec<(String, String, crate::module::ImportKind)> = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Vec<(String, String, crate::module::ImportKind)>> {
        let module = unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)?
        };
        let metadata = crate::module::core::get_metadata(module);
        Ok(metadata
            .imports
            .iter()
            .map(|imp| {
                (
                    imp.module.clone(),
                    imp.name.clone(),
                    imp.import_type.clone(),
                )
            })
            .collect())
    })) {
        Ok(Ok(data)) => data,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    // Create ArrayList
    let array_list_class = match env.find_class("java/util/ArrayList") {
        Ok(c) => c,
        Err(_) => return std::ptr::null_mut(),
    };
    let array_list = match env.new_object(array_list_class, "()V", &[]) {
        Ok(list) => list,
        Err(_) => return std::ptr::null_mut(),
    };

    // For each import, create ImportType object with proper type
    for (module_name, field_name, import_kind) in &imports_data {
        // Create import strings
        let module_name_jstring = match env.new_string(module_name) {
            Ok(s) => s,
            Err(_) => continue,
        };
        let field_name_jstring = match env.new_string(field_name) {
            Ok(s) => s,
            Err(_) => continue,
        };

        // Create WasmType based on the actual import kind
        let wasm_type_obj = match import_kind {
            crate::module::ImportKind::Function(sig) => {
                match create_jni_func_type(&mut env, &sig.params, &sig.returns) {
                    Some(obj) => obj,
                    None => continue,
                }
            }
            crate::module::ImportKind::Global(val_type, mutable) => {
                match create_jni_global_type(&mut env, val_type, *mutable) {
                    Some(obj) => obj,
                    None => continue,
                }
            }
            crate::module::ImportKind::Memory(initial, max, is_64, shared, _page_size_log2) => {
                match create_jni_memory_type(&mut env, *initial, *max, *is_64, *shared) {
                    Some(obj) => obj,
                    None => continue,
                }
            }
            crate::module::ImportKind::Table(elem_type, initial, max) => {
                match create_jni_table_type(&mut env, elem_type, *initial, *max) {
                    Some(obj) => obj,
                    None => continue,
                }
            }
            crate::module::ImportKind::Tag(sig) => {
                match create_jni_func_type(&mut env, &sig.params, &sig.returns) {
                    Some(obj) => obj,
                    None => continue,
                }
            }
        };

        // Create ImportType(String moduleName, String name, WasmType type)
        let import_type_class = match env.find_class("ai/tegmentum/wasmtime4j/type/ImportType") {
            Ok(c) => c,
            Err(_) => continue,
        };
        let import_type_obj = match env.new_object(
            import_type_class,
            "(Ljava/lang/String;Ljava/lang/String;Lai/tegmentum/wasmtime4j/type/WasmType;)V",
            &[
                JValue::Object(&module_name_jstring),
                JValue::Object(&field_name_jstring),
                JValue::Object(&wasm_type_obj),
            ],
        ) {
            Ok(obj) => obj,
            Err(_) => continue,
        };

        // Add ImportType directly to ArrayList
        let _ = env.call_method(
            &array_list,
            "add",
            "(Ljava/lang/Object;)Z",
            &[JValue::Object(&import_type_obj)],
        );
    }

    array_list.into_raw()
}

/// Check if module has a specific export
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeHasExport(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    export_name: JString,
) -> jboolean {
    if module_ptr == 0 {
        return jni::sys::JNI_FALSE;
    }

    let name = match env.get_string(&export_name) {
        Ok(name_str) => name_str.to_string_lossy().into_owned(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let module = unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)?
        };
        let metadata = crate::module::core::get_metadata(module);
        for export in &metadata.exports {
            if export.name == name {
                return Ok(true);
            }
        }
        Ok(false)
    }) as jboolean
}

/// Check if module has a specific import
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeHasImport(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
    module_name: JString,
    field_name: JString,
) -> jboolean {
    if module_ptr == 0 {
        return jni::sys::JNI_FALSE;
    }

    let mod_name = match env.get_string(&module_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return jni::sys::JNI_FALSE,
    };
    let fld_name = match env.get_string(&field_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return jni::sys::JNI_FALSE,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let module = unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)?
        };
        let metadata = crate::module::core::get_metadata(module);
        for import in &metadata.imports {
            if import.module == mod_name && import.name == fld_name {
                return Ok(true);
            }
        }
        Ok(false)
    }) as jboolean
}

/// JNI binding for Module.initializeCopyOnWriteImage()
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInitializeCopyOnWriteImage(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jboolean {
    if module_ptr == 0 {
        return 0;
    }

    match crate::shared_ffi::module::initialize_copy_on_write_image_shared(
        module_ptr as *mut std::os::raw::c_void,
    ) {
        Ok(()) => 1,
        Err(e) => {
            let _ = jni_utils::throw_jni_exception(&mut env, &e);
            0
        }
    }
}

/// JNI binding for Module.resourcesRequired() - returns long[8]
///
/// Returns [minMemoryBytes, maxMemoryBytes, minTableElements, maxTableElements,
///          numMemories, numTables, numGlobals, numFunctions]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleResourcesRequired(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jlongArray {
    if module_ptr == 0 {
        let _ = jni_utils::throw_jni_exception(
            &mut env,
            &crate::error::WasmtimeError::InvalidParameter {
                message: "Module pointer cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlongArray> {
        let (min_mem, max_mem, min_tab, max_tab, n_mem, n_tab, n_glob, n_func) = unsafe {
            crate::module::core::get_module_resources_required(
                module_ptr as *const std::os::raw::c_void,
            )?
        };

        let values = [
            min_mem,
            max_mem,
            min_tab,
            max_tab,
            n_mem as i64,
            n_tab as i64,
            n_glob as i64,
            n_func as i64,
        ];

        let arr = env.new_long_array(8).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;
        env.set_long_array_region(&arr, 0, &values)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to set long array region: {}", e),
            })?;
        Ok(arr.into_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for Module.imageRange() - returns long[2] = [start, end]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleImageRange(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jlongArray {
    if module_ptr == 0 {
        let _ = jni_utils::throw_jni_exception(
            &mut env,
            &crate::error::WasmtimeError::InvalidParameter {
                message: "Module pointer cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlongArray> {
        let (start, end) =
            crate::shared_ffi::module::image_range_shared(module_ptr as *mut std::os::raw::c_void)?;
        let values = [start as i64, end as i64];

        let arr = env.new_long_array(2).map_err(|e| WasmtimeError::Memory {
            message: format!("Failed to create long array: {}", e),
        })?;
        env.set_long_array_region(&arr, 0, &values)
            .map_err(|e| WasmtimeError::Memory {
                message: format!("Failed to set long array region: {}", e),
            })?;
        Ok(arr.into_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Get all functions (imports, exports, and internal) as a JSON string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetAllFunctions(
    mut env: JNIEnv,
    _class: JClass,
    module_ptr: jlong,
) -> jstring {
    if module_ptr == 0 {
        return std::ptr::null_mut();
    }

    let result: crate::error::WasmtimeResult<String> = (|| {
        let module = unsafe {
            core::get_module_ref(module_ptr as *const std::os::raw::c_void)?
        };
        let metadata = core::get_metadata(module);

        let mut json_parts: Vec<String> = Vec::with_capacity(metadata.functions.len());
        for func in &metadata.functions {
            let name_json = match &func.name {
                Some(n) => format!("\"{}\"", n.replace('\\', "\\\\").replace('"', "\\\"")),
                None => "null".to_string(),
            };
            let params: Vec<&str> = func.signature.params.iter()
                .map(|p| module_value_type_str(p))
                .collect();
            let returns: Vec<&str> = func.signature.returns.iter()
                .map(|r| module_value_type_str(r))
                .collect();
            json_parts.push(format!(
                "{{\"index\":{},\"name\":{},\"params\":[{}],\"returns\":[{}],\"isImport\":{},\"isExported\":{}}}",
                func.index,
                name_json,
                params.iter().map(|p| format!("\"{}\"", p)).collect::<Vec<_>>().join(","),
                returns.iter().map(|r| format!("\"{}\"", r)).collect::<Vec<_>>().join(","),
                func.is_import,
                func.is_exported,
            ));
        }

        Ok(format!("[{}]", json_parts.join(",")))
    })();

    match result {
        Ok(json) => match env.new_string(&json) {
            Ok(s) => s.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

pub(crate) fn module_value_type_str(vt: &crate::module::ModuleValueType) -> &'static str {
    use crate::module::ModuleValueType;
    match vt {
        ModuleValueType::I32 => "i32",
        ModuleValueType::I64 => "i64",
        ModuleValueType::F32 => "f32",
        ModuleValueType::F64 => "f64",
        ModuleValueType::V128 => "v128",
        ModuleValueType::ExternRef => "externref",
        ModuleValueType::FuncRef => "funcref",
        ModuleValueType::AnyRef => "anyref",
        ModuleValueType::EqRef => "eqref",
        ModuleValueType::I31Ref => "i31ref",
        ModuleValueType::StructRef => "structref",
        ModuleValueType::ArrayRef => "arrayref",
        ModuleValueType::NullRef => "nullref",
        ModuleValueType::NullFuncRef => "nullfuncref",
        ModuleValueType::NullExternRef => "nullexternref",
    }
}
