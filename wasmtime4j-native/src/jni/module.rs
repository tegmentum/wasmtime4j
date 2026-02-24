//! JNI bindings for Module operations
//!
//! This module provides JNI bindings for WebAssembly module operations including
//! instantiation, introspection, serialization, and metadata access.
//!
//! It exports `VecByteArrayConverter` and `JStringConverter` for use by other modules.

use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::strings::JavaStr;
use jni::sys::{jboolean, jbyteArray, jlong, jlongArray, jobject, jobjectArray, jstring};
use jni::JNIEnv;

use crate::error::jni_utils;
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
                    crate::module::ExportKind::Memory(_, _, _, _) => "memory",
                    crate::module::ExportKind::Table(_, _, _) => "table",
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
                    crate::module::ImportKind::Memory(_, _, _, _) => "memory",
                    crate::module::ImportKind::Table(_, _, _) => "table",
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
    _env: JNIEnv,
    _class: JClass,
    module_ptr1: jlong,
    module_ptr2: jlong,
) -> jboolean {
    if module_ptr1 == 0 || module_ptr2 == 0 {
        return jni::sys::JNI_FALSE;
    }
    match unsafe {
        core::modules_same(
            module_ptr1 as *const std::os::raw::c_void,
            module_ptr2 as *const std::os::raw::c_void,
        )
    } {
        Ok(same) => {
            if same {
                jni::sys::JNI_TRUE
            } else {
                jni::sys::JNI_FALSE
            }
        }
        Err(_) => jni::sys::JNI_FALSE,
    }
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

    match unsafe { core::get_export_index(module_ptr as *const std::os::raw::c_void, &name) } {
        Ok(idx) => idx as jlong,
        Err(_) => -1,
    }
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

    match unsafe {
        core::get_wasmtime_module_export(module_ptr as *const std::os::raw::c_void, &name)
    } {
        Ok(ptr) => ptr as jlong,
        Err(_) => 0,
    }
}

/// Destroy a ModuleExport handle (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModuleExport(
    _env: JNIEnv,
    _class: JClass,
    module_export_ptr: jlong,
) {
    if module_export_ptr != 0 {
        unsafe {
            core::destroy_module_export(module_export_ptr as *mut std::os::raw::c_void);
        }
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

    // Extract export data with type information
    let exports_data: Vec<(String, crate::module::ExportKind)> = {
        let module = match unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)
        } {
            Ok(m) => m,
            Err(_) => return std::ptr::null_mut(),
        };
        let metadata = crate::module::core::get_metadata(module);
        // Extract name and type info, then drop the module reference
        metadata
            .exports
            .iter()
            .map(|exp| (exp.name.clone(), exp.export_type.clone()))
            .collect()
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
            crate::module::ExportKind::Memory(initial, max, is_64, shared) => {
                create_jni_memory_type(&mut env, *initial, *max, *is_64, *shared)
            }
            crate::module::ExportKind::Table(elem_type, initial, max) => {
                create_jni_table_type(&mut env, elem_type, *initial, *max)
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

    // Extract import data with type information
    let imports_data: Vec<(String, String, crate::module::ImportKind)> = {
        let module = match unsafe {
            crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)
        } {
            Ok(m) => m,
            Err(_) => return std::ptr::null_mut(),
        };
        let metadata = crate::module::core::get_metadata(module);
        // Extract module name, field name, and type info
        metadata
            .imports
            .iter()
            .map(|imp| {
                (
                    imp.module.clone(),
                    imp.name.clone(),
                    imp.import_type.clone(),
                )
            })
            .collect()
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
            crate::module::ImportKind::Memory(initial, max, is_64, shared) => {
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

    match env.get_string(&export_name) {
        Ok(name_str) => {
            let name: String = name_str.into();
            match unsafe {
                crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)
            } {
                Ok(module) => {
                    let metadata = crate::module::core::get_metadata(module);
                    for export in &metadata.exports {
                        if export.name == name {
                            return jni::sys::JNI_TRUE;
                        }
                    }
                    jni::sys::JNI_FALSE
                }
                Err(_) => jni::sys::JNI_FALSE,
            }
        }
        Err(_) => jni::sys::JNI_FALSE,
    }
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

    match (env.get_string(&module_name), env.get_string(&field_name)) {
        (Ok(mod_name_str), Ok(fld_name_str)) => {
            let mod_name: String = mod_name_str.into();
            let fld_name: String = fld_name_str.into();
            match unsafe {
                crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)
            } {
                Ok(module) => {
                    let metadata = crate::module::core::get_metadata(module);
                    for import in &metadata.imports {
                        if import.module == mod_name && import.name == fld_name {
                            return jni::sys::JNI_TRUE;
                        }
                    }
                    jni::sys::JNI_FALSE
                }
                Err(_) => jni::sys::JNI_FALSE,
            }
        }
        _ => jni::sys::JNI_FALSE,
    }
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

    let data = (|| -> crate::error::WasmtimeResult<[i64; 8]> {
        let (min_mem, max_mem, min_tab, max_tab, n_mem, n_tab, n_glob, n_func) = unsafe {
            crate::module::core::get_module_resources_required(
                module_ptr as *const std::os::raw::c_void,
            )?
        };

        Ok([
            min_mem,
            max_mem,
            min_tab,
            max_tab,
            n_mem as i64,
            n_tab as i64,
            n_glob as i64,
            n_func as i64,
        ])
    })();

    match data {
        Ok(values) => {
            let result = env.new_long_array(8);
            match result {
                Ok(arr) => {
                    let _ = env.set_long_array_region(&arr, 0, &values);
                    arr.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(e) => {
            let _ = jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}
