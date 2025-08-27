// Build script for Wasmtime4j native library
// This script handles platform-specific compilation configuration

use std::env;
use std::path::PathBuf;

fn main() {
    let target = env::var("TARGET").unwrap();
    let out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());
    
    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-changed=src/");
    
    // Print build information
    println!("cargo:rustc-env=BUILD_TARGET={}", target);
    println!("cargo:rustc-env=BUILD_PROFILE={}", env::var("PROFILE").unwrap_or_else(|_| "unknown".to_string()));
    
    // Configure target-specific settings
    configure_target(&target);
    
    // Configure linking
    configure_linking(&target);
    
    println!("cargo:rustc-link-lib=static=wasmtime");
}

fn configure_target(target: &str) {
    match target {
        t if t.contains("linux") => {
            println!("cargo:rustc-cfg=target_os=\"linux\"");
            if t.contains("aarch64") {
                println!("cargo:rustc-cfg=target_arch=\"aarch64\"");
            } else {
                println!("cargo:rustc-cfg=target_arch=\"x86_64\"");
            }
        }
        t if t.contains("windows") => {
            println!("cargo:rustc-cfg=target_os=\"windows\"");
            println!("cargo:rustc-cfg=target_arch=\"x86_64\"");
        }
        t if t.contains("darwin") => {
            println!("cargo:rustc-cfg=target_os=\"macos\"");
            if t.contains("aarch64") {
                println!("cargo:rustc-cfg=target_arch=\"aarch64\"");
            } else {
                println!("cargo:rustc-cfg=target_arch=\"x86_64\"");
            }
        }
        _ => {
            println!("cargo:warning=Unknown target: {}", target);
        }
    }
}

fn configure_linking(target: &str) {
    // Platform-specific linking configuration
    if target.contains("windows") {
        // Windows-specific linking
        println!("cargo:rustc-link-lib=dylib=kernel32");
        println!("cargo:rustc-link-lib=dylib=user32");
    } else if target.contains("darwin") {
        // macOS-specific linking
        println!("cargo:rustc-link-lib=framework=Foundation");
        println!("cargo:rustc-link-lib=framework=Security");
    } else if target.contains("linux") {
        // Linux-specific linking
        println!("cargo:rustc-link-lib=dylib=dl");
        println!("cargo:rustc-link-lib=dylib=pthread");
    }
}