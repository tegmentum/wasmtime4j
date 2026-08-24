//! Shared WASI configuration helpers used by both `component::linker::WasiP2Config`
//! and `wasi_preview2::WasiPreview2Config` to avoid code duplication.

// wasmtime 48 collapsed the DirPerms/FilePerms bit-flag pair into a single
// two-value FsPerms enum (ReadOnly / ReadWrite).
#[cfg(feature = "wasi")]
use wasmtime_wasi::{FsPerms, WasiCtxBuilder};

#[cfg(feature = "wasi")]
use crate::component::CallbackSocketAddrCheck;

/// Applies network configuration to a `WasiCtxBuilder`.
#[cfg(feature = "wasi")]
pub fn apply_network_config(
    builder: &mut WasiCtxBuilder,
    inherit_network: bool,
    allow_tcp: bool,
    allow_udp: bool,
    allow_ip_name_lookup: bool,
) {
    builder.allow_tcp(allow_tcp);
    builder.allow_udp(allow_udp);
    builder.allow_ip_name_lookup(allow_ip_name_lookup);
    if inherit_network {
        builder.inherit_network();
    }
}

/// Applies the socket address check callback to a `WasiCtxBuilder`.
#[cfg(feature = "wasi")]
pub fn apply_socket_addr_check(
    builder: &mut WasiCtxBuilder,
    check: Option<CallbackSocketAddrCheck>,
) {
    if let Some(check) = check {
        builder.socket_addr_check(move |addr, reason| {
            use std::net::SocketAddr;
            let (ip_version, ip_bytes, port) = match addr {
                SocketAddr::V4(v4) => (4i32, v4.ip().octets().to_vec(), v4.port()),
                SocketAddr::V6(v6) => (6i32, v6.ip().octets().to_vec(), v6.port()),
            };
            // FFI codes must stay in sync with `SocketAddrUse.java`. wasmtime 48
            // reshaped this enum: `UdpConnect` is gone (UDP connect now piggy-
            // backs on `UdpBind`) and `UdpOutgoingDatagram` was renamed to
            // `UdpSend`; `TcpListen`, `TcpAccept`, and `UdpReceive` are new.
            let use_type = match reason {
                wasmtime_wasi::sockets::SocketAddrUse::TcpBind => 0i32,
                wasmtime_wasi::sockets::SocketAddrUse::TcpListen => 1,
                wasmtime_wasi::sockets::SocketAddrUse::TcpAccept => 2,
                wasmtime_wasi::sockets::SocketAddrUse::TcpConnect => 3,
                wasmtime_wasi::sockets::SocketAddrUse::UdpBind => 4,
                wasmtime_wasi::sockets::SocketAddrUse::UdpSend => 5,
                wasmtime_wasi::sockets::SocketAddrUse::UdpReceive => 6,
            };
            let result = (check.check_fn)(
                check.callback_id,
                ip_version,
                ip_bytes.as_ptr(),
                ip_bytes.len(),
                port,
                use_type,
            );
            Box::pin(async move { result != 0 })
        });
    }
}

/// Applies custom clock and RNG callbacks to a `WasiCtxBuilder`.
#[cfg(feature = "wasi")]
pub fn apply_clock_and_rng_config(
    builder: &mut WasiCtxBuilder,
    insecure_random_seed: Option<u64>,
    max_random_size: Option<u64>,
    wall_clock: Option<crate::component::CallbackWallClock>,
    monotonic_clock: Option<crate::component::CallbackMonotonicClock>,
    secure_random: Option<crate::component::CallbackRng>,
    insecure_random: Option<crate::component::CallbackRng>,
) {
    if let Some(seed) = insecure_random_seed {
        builder.insecure_random_seed(seed as u128);
    }
    if let Some(max_size) = max_random_size {
        builder.max_random_size(max_size);
    }
    if let Some(clock) = wall_clock {
        builder.wall_clock(clock);
    }
    if let Some(clock) = monotonic_clock {
        builder.monotonic_clock(clock);
    }
    if let Some(rng) = secure_random {
        builder.secure_random(rng);
    }
    if let Some(rng) = insecure_random {
        builder.insecure_random(rng);
    }
}

/// Converts the FFI access-mode code sent from Java to a wasmtime `FsPerms`.
///
/// Wire codes must stay in sync with `FsPerms.java`: 0 = `READ_ONLY`, 1 =
/// `READ_WRITE`. Any unknown value falls back to `READ_ONLY` so a stale caller
/// cannot inadvertently escalate a preopen to read/write.
#[cfg(feature = "wasi")]
pub fn decode_fs_perms(code: u32) -> FsPerms {
    match code {
        1 => FsPerms::ReadWrite,
        _ => FsPerms::ReadOnly,
    }
}
