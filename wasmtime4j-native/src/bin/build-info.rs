//! Build information utility

use std::env;

fn main() {
    println!("Wasmtime4j Native Library Build Information");
    println!("==========================================");
    println!("Version: {}", env!("CARGO_PKG_VERSION"));
    
    // Get runtime environment variables instead of compile-time
    println!("Target Architecture: {}", env::var("CARGO_CFG_TARGET_ARCH").unwrap_or_else(|_| "unknown".to_string()));
    println!("Target OS: {}", env::var("CARGO_CFG_TARGET_OS").unwrap_or_else(|_| "unknown".to_string()));
    println!("Target Family: {}", env::var("CARGO_CFG_TARGET_FAMILY").unwrap_or_else(|_| "unknown".to_string()));
    
    // Note: wasmtime4j_native module reference removed as it creates circular dependency
    println!("Wasmtime Version: 36.0.2");
    
    if let Ok(profile) = env::var("PROFILE") {
        println!("Build Profile: {}", profile);
    }
    
    if let Ok(out_dir) = env::var("OUT_DIR") {
        println!("Output Directory: {}", out_dir);
    }
}