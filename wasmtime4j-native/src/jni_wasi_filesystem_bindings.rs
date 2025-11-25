//! JNI bindings for WASI Preview 2 filesystem operations
//!
//! This module provides JNI functions for wasi:filesystem interfaces including
//! descriptor-based file operations, directory management, and metadata access.

use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray, jint, jlong, jobjectArray, jstring};
use jni::JNIEnv;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_filesystem_helpers;

/// Read from descriptor via stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeReadViaStream(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    offset: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return 0;
    }

    if offset < 0 {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            "Offset cannot be negative",
        );
        return 0;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::read_via_stream(context, descriptor_handle as u64, offset as u64) {
        Ok(stream_id) => stream_id as jlong,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            0
        }
    }
}

/// Write to descriptor via stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeWriteViaStream(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    offset: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return 0;
    }

    if offset < 0 {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            "Offset cannot be negative",
        );
        return 0;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::write_via_stream(context, descriptor_handle as u64, offset as u64) {
        Ok(stream_id) => stream_id as jlong,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            0
        }
    }
}

/// Append to descriptor via stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeAppendViaStream(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return 0;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::append_via_stream(context, descriptor_handle as u64) {
        Ok(stream_id) => stream_id as jlong,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            0
        }
    }
}

/// Get descriptor type
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeGetType(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::get_type(context, descriptor_handle as u64) {
        Ok(type_code) => type_code as jint,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Get descriptor flags
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeGetFlags(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::get_flags(context, descriptor_handle as u64) {
        Ok(flags) => flags as jint,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Set descriptor size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeSetSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    size: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    if size < 0 {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            "Size cannot be negative",
        );
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::set_size(context, descriptor_handle as u64, size as u64) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Sync descriptor data
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeSyncData(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::sync_data(context, descriptor_handle as u64) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Sync descriptor
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeSync(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::sync(context, descriptor_handle as u64) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Open file at path
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeOpenAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    path: JString,
    path_flags: jint,
    open_flags: jint,
    descriptor_flags: jint,
) -> jlong {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return 0;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // Get path string
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid path string: {}", e),
            );
            return 0;
        }
    };

    // Combine flags for helper function
    let combined_flags = (path_flags as u32) | (open_flags as u32) | (descriptor_flags as u32);

    // Call helper function
    match wasi_filesystem_helpers::open_at(context, descriptor_handle as u64, &path_str, combined_flags, 0) {
        Ok(new_descriptor_id) => new_descriptor_id as jlong,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            0
        }
    }
}

/// Create directory at path
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeCreateDirectoryAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get path string
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::create_directory_at(context, descriptor_handle as u64, &path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Read directory entries
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeReadDirectory(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jobjectArray {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Call helper function to get directory entries
    match wasi_filesystem_helpers::read_directory(context, descriptor_handle as u64) {
        Ok(entries) => {
            // Create Java String array
            let string_class = match env.find_class("java/lang/String") {
                Ok(cls) => cls,
                Err(e) => {
                    let _ = env.throw_new("java/lang/RuntimeException", &format!("Failed to find String class: {:?}", e));
                    return JObject::null().into_raw();
                }
            };

            let result = match env.new_object_array(entries.len() as i32, &string_class, JObject::null()) {
                Ok(arr) => arr,
                Err(e) => {
                    let _ = env.throw_new("java/lang/OutOfMemoryError", &format!("Failed to create array: {:?}", e));
                    return JObject::null().into_raw();
                }
            };

            // Populate array with file names (ignoring entry types for now)
            for (i, (name, _entry_type)) in entries.iter().enumerate() {
                let jstring = match env.new_string(name) {
                    Ok(s) => s,
                    Err(e) => {
                        let _ = env.throw_new("java/lang/RuntimeException", &format!("Failed to create string: {:?}", e));
                        return JObject::null().into_raw();
                    }
                };

                if let Err(e) = env.set_object_array_element(&result, i as i32, jstring) {
                    let _ = env.throw_new("java/lang/RuntimeException", &format!("Failed to set array element: {:?}", e));
                    return JObject::null().into_raw();
                }
            }

            result.into_raw()
        }
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            JObject::null().into_raw()
        }
    }
}

/// Read symbolic link at path
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeReadLinkAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    path: JString,
) -> jstring {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Get path string
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid path string: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::read_link_at(context, descriptor_handle as u64, &path_str) {
        Ok(target) => match env.new_string(&target) {
            Ok(jstr) => jstr.into_raw(),
            Err(e) => {
                let _ = env.throw_new("java/lang/RuntimeException", &format!("Failed to create string: {}", e));
                JObject::null().into_raw()
            }
        },
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            JObject::null().into_raw()
        }
    }
}

/// Unlink file at path
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeUnlinkFileAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get path string
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::unlink_file_at(context, descriptor_handle as u64, &path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Remove directory at path
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeRemoveDirectoryAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get path string
    let path_str: String = match env.get_string(&path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::remove_directory_at(context, descriptor_handle as u64, &path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Rename file or directory
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeRenameAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    old_path: JString,
    new_descriptor_handle: jlong,
    new_path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 || new_descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get old path string
    let old_path_str: String = match env.get_string(&old_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid old path string: {}", e),
            );
            return -1;
        }
    };

    // Get new path string
    let new_path_str: String = match env.get_string(&new_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid new path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::rename_at(context, descriptor_handle as u64, &old_path_str, new_descriptor_handle as u64, &new_path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Create symbolic link
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeSymlinkAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    old_path: JString,
    new_path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get old path string
    let old_path_str: String = match env.get_string(&old_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid old path string: {}", e),
            );
            return -1;
        }
    };

    // Get new path string
    let new_path_str: String = match env.get_string(&new_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid new path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function
    match wasi_filesystem_helpers::symlink_at(context, descriptor_handle as u64, &old_path_str, &new_path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Create hard link
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeLinkAt(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    old_path_flags: jint,
    old_path: JString,
    new_descriptor_handle: jlong,
    new_path: JString,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 || new_descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Get old path string
    let old_path_str: String = match env.get_string(&old_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid old path string: {}", e),
            );
            return -1;
        }
    };

    // Get new path string
    let new_path_str: String = match env.get_string(&new_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid new path string: {}", e),
            );
            return -1;
        }
    };

    // Call helper function (note: flags parameter is ignored in MVP)
    let _ = old_path_flags; // Acknowledge flags parameter but don't use it in MVP
    match wasi_filesystem_helpers::link_at(context, descriptor_handle as u64, &old_path_str, new_descriptor_handle as u64, &new_path_str) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}

/// Check if two descriptors refer to the same object
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeIsSameObject(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
    other_descriptor_handle: jlong,
) -> jboolean {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 || other_descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return 0;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::is_same_object(context, descriptor_handle as u64, other_descriptor_handle as u64) {
        Ok(same) => if same { 1 } else { 0 },
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            0
        }
    }
}

/// Close descriptor
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_filesystem_JniWasiDescriptor_nativeClose(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    descriptor_handle: jlong,
) -> jint {
    // Validate parameters
    if context_handle == 0 || descriptor_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid handle");
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_filesystem_helpers::close_descriptor(context, descriptor_handle as u64) {
        Ok(()) => 0,
        Err(e) => {
            let _ = env.throw_new("java/io/IOException", &format!("{:?}", e));
            -1
        }
    }
}
