/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//! # JNI Bindings for WIT Value Marshalling
//!
//! This module provides JNI (Java Native Interface) bindings for WIT value marshalling
//! operations, bridging between Java WIT values and native Wasmtime component values.
//!
//! ## Safety and Error Handling
//!
//! All JNI functions implement comprehensive defensive programming patterns to prevent
//! JVM crashes and ensure robust error handling. Input validation and null checks are
//! performed for all parameters.

use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jbyteArray, jint};
use jni::JNIEnv;

use crate::wit_value_marshal::{deserialize_to_val, serialize_from_val};

/// JNI binding for serializing a WIT value to binary format.
///
/// # Safety
///
/// This function uses JNI which requires careful handling of Java objects and memory.
/// All Java byte arrays are properly converted and cleaned up.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWitValueMarshaller_witValueSerializeNative(
    mut env: JNIEnv,
    _class: JClass,
    type_discriminator: jint,
    data: JByteArray,
) -> jbyteArray {
    // Validate inputs
    if data.is_null() {
        return std::ptr::null_mut();
    }

    // Convert Java byte array to Rust Vec<u8>
    let data_vec = match env.convert_byte_array(&data) {
        Ok(vec) => vec,
        Err(_) => return std::ptr::null_mut(),
    };

    // Deserialize to Val
    let val = match deserialize_to_val(type_discriminator, &data_vec) {
        Ok(v) => v,
        Err(_) => return std::ptr::null_mut(),
    };

    // Serialize back (validation round-trip)
    let (_, serialized) = match serialize_from_val(&val) {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    // Convert result to Java byte array
    match env.byte_array_from_slice(&serialized) {
        Ok(result) => result.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// JNI binding for deserializing a WIT value from binary format.
///
/// # Safety
///
/// This function uses JNI which requires careful handling of Java objects and memory.
/// All Java byte arrays are properly converted and cleaned up.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWitValueMarshaller_witValueDeserializeNative(
    mut env: JNIEnv,
    _class: JClass,
    type_discriminator: jint,
    data: JByteArray,
) -> jbyteArray {
    // Validate inputs
    if data.is_null() {
        return std::ptr::null_mut();
    }

    // Convert Java byte array to Rust Vec<u8>
    let data_vec = match env.convert_byte_array(&data) {
        Ok(vec) => vec,
        Err(_) => return std::ptr::null_mut(),
    };

    // Deserialize to Val
    let val = match deserialize_to_val(type_discriminator, &data_vec) {
        Ok(v) => v,
        Err(_) => return std::ptr::null_mut(),
    };

    // Serialize to output format
    let (_, serialized) = match serialize_from_val(&val) {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    // Convert result to Java byte array
    match env.byte_array_from_slice(&serialized) {
        Ok(result) => result.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// JNI binding for validating a type discriminator.
///
/// # Safety
///
/// This function is safe as it only validates an integer parameter.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWitValueMarshaller_witValueValidateDiscriminatorNative(
    _env: JNIEnv,
    _class: JClass,
    type_discriminator: jint,
) -> jboolean {
    match type_discriminator {
        1..=23 => 1, // true in JNI — matches all WIT value type discriminators
        _ => 0,      // false in JNI
    }
}
