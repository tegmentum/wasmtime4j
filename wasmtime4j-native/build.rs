// Build script for Wasmtime4j native library
// This script handles platform-specific compilation configuration

use std::env;
use std::path::PathBuf;

fn main() {
    let target = env::var("TARGET").unwrap();
    let _out_dir = PathBuf::from(env::var("OUT_DIR").unwrap());

    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-changed=src/");

    // Print build information
    println!("cargo:rustc-env=BUILD_TARGET={}", target);
    println!(
        "cargo:rustc-env=BUILD_PROFILE={}",
        env::var("PROFILE").unwrap_or_else(|_| "unknown".to_string())
    );

    // Configure target-specific settings
    configure_target(&target);

    // Configure linking
    configure_linking(&target);
}

fn configure_target(target: &str) {
    // Note: Using rustc-env instead of rustc-cfg to avoid conflicts
    if target.contains("linux") {
        println!("cargo:rustc-env=TARGET_OS=linux");
        if target.contains("aarch64") {
            println!("cargo:rustc-env=TARGET_ARCH=aarch64");
        } else {
            println!("cargo:rustc-env=TARGET_ARCH=x86_64");
        }
    } else if target.contains("windows") {
        println!("cargo:rustc-env=TARGET_OS=windows");
        println!("cargo:rustc-env=TARGET_ARCH=x86_64");
    } else if target.contains("darwin") {
        println!("cargo:rustc-env=TARGET_OS=macos");
        if target.contains("aarch64") {
            println!("cargo:rustc-env=TARGET_ARCH=aarch64");
        } else {
            println!("cargo:rustc-env=TARGET_ARCH=x86_64");
        }
    } else {
        println!("cargo:warning=Unknown target: {}", target);
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
