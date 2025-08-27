//! Build information utility

use std::env;

fn main() {
    println!("Wasmtime4j Native Library Build Information");
    println!("==========================================");
    println!("Version: {}", env!("CARGO_PKG_VERSION"));
    println!("Wasmtime Version: {}", wasmtime4j_native::WASMTIME_VERSION);
    println!("Target Architecture: {}", env!("CARGO_CFG_TARGET_ARCH"));
    println!("Target OS: {}", env!("CARGO_CFG_TARGET_OS"));
    println!("Target Family: {}", env!("CARGO_CFG_TARGET_FAMILY"));
    
    if let Ok(profile) = env::var("PROFILE") {
        println!("Build Profile: {}", profile);
    }
    
    if let Ok(out_dir) = env::var("OUT_DIR") {
        println!("Output Directory: {}", out_dir);
    }
}