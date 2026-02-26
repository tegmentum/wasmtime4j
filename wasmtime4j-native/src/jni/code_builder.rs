//! JNI bindings for CodeBuilder operations

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jbyteArray, jint, jlong};
use jni::JNIEnv;

use crate::code_builder;
use crate::engine::Engine;
use crate::error::jni_utils;

/// Create a new CodeBuilder for the given engine handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { &*(engine_handle as *const Engine) };
        code_builder::code_builder_new(engine)
    }) as jlong
}

/// Set wasm binary bytes on the builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeWasmBinary(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    wasm_bytes: JByteArray,
) -> jint {
    // Extract bytes before passing env into closure
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(b) => b,
        Err(_) => return -1,
    };
    jni_utils::jni_try_code(&mut env, || {
        let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
        code_builder::code_builder_wasm_binary(builder, bytes)?;
        Ok(())
    })
}

/// Set wasm binary or text bytes on the builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeWasmBinaryOrText(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    wasm_bytes: JByteArray,
) -> jint {
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(b) => b,
        Err(_) => return -1,
    };
    jni_utils::jni_try_code(&mut env, || {
        let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
        code_builder::code_builder_wasm_binary_or_text(builder, bytes)?;
        Ok(())
    })
}

/// Set DWARF package bytes on the builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeDwarfPackage(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    dwarf_bytes: JByteArray,
) -> jint {
    let bytes = match env.convert_byte_array(&dwarf_bytes) {
        Ok(b) => b,
        Err(_) => return -1,
    };
    jni_utils::jni_try_code(&mut env, || {
        let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
        code_builder::code_builder_dwarf_package(builder, bytes)?;
        Ok(())
    })
}

/// Set hint on the builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeHint(
    _env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    hint_ordinal: jint,
) {
    let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
    code_builder::code_builder_hint(builder, hint_ordinal);
}

/// Compile module from builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileModule(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let builder = unsafe { &*(builder_handle as *const code_builder::CodeBuilderState) };
        code_builder::code_builder_compile_module(builder)
    }) as jlong
}

/// Compile module serialized from builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileModuleSerialized(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
) -> jbyteArray {
    let builder = unsafe { &*(builder_handle as *const code_builder::CodeBuilderState) };
    let serialized = match code_builder::code_builder_compile_module_serialized(builder) {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };
    match env.byte_array_from_slice(&serialized) {
        Ok(array) => array.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Compile component from builder.
#[no_mangle]
#[cfg(feature = "component-model")]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileComponent(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let builder = unsafe { &*(builder_handle as *const code_builder::CodeBuilderState) };
        code_builder::code_builder_compile_component(builder)
    }) as jlong
}

/// Compile component serialized from builder.
#[no_mangle]
#[cfg(feature = "component-model")]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileComponentSerialized(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
) -> jbyteArray {
    let builder = unsafe { &*(builder_handle as *const code_builder::CodeBuilderState) };
    let serialized = match code_builder::code_builder_compile_component_serialized(builder) {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };
    match env.byte_array_from_slice(&serialized) {
        Ok(array) => array.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Add compile-time builtins from binary bytes.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileTimeBuiltinsBinary(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    name: JString,
    wasm_bytes: JByteArray,
) {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(b) => b,
        Err(_) => return,
    };
    let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
    code_builder::code_builder_compile_time_builtins_binary(builder, name_str, bytes);
}

/// Add compile-time builtins from binary or text bytes.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeCompileTimeBuiltinsBinaryOrText(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    name: JString,
    wasm_bytes: JByteArray,
) {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let bytes = match env.convert_byte_array(&wasm_bytes) {
        Ok(b) => b,
        Err(_) => return,
    };
    let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
    code_builder::code_builder_compile_time_builtins_binary_or_text(builder, name_str, bytes);
}

/// Set expose unsafe intrinsics import name on the builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeExposeUnsafeIntrinsics(
    mut env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
    import_name: JString,
) {
    let name_str: String = match env.get_string(&import_name) {
        Ok(s) => s.into(),
        Err(_) => return,
    };
    let builder = unsafe { &mut *(builder_handle as *mut code_builder::CodeBuilderState) };
    code_builder::code_builder_expose_unsafe_intrinsics(builder, name_str);
}

/// Destroy the code builder.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCodeBuilder_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    builder_handle: jlong,
) {
    if builder_handle != 0 {
        let builder =
            unsafe { Box::from_raw(builder_handle as *mut code_builder::CodeBuilderState) };
        code_builder::code_builder_destroy(builder);
    }
}
