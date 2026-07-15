//! Spike: host-observable filesystem-denial hook on the component / WASI-preopen path.
//!
//! Companion to `~/git/svalinn/handoff/denial-signals/FS-DENIAL-HOOK.md`.
//!
//! Background: on the component/WASI-preopen path, wasmtime-wasi enforces preopen
//! bounds internally (`wasmtime_wasi::filesystem::Dir::open_at`) and refuses an
//! out-of-preopen / missing open with NO host-visible callback. The prior finding
//! concluded there was "no hook" because `WasiCtxBuilder` exposes no `fs_access_check`
//! analogous to `socket_addr_check`.
//!
//! This spike demonstrates that a host-observable interception point DOES exist one
//! layer up: the `wasi:filesystem/types` interface is registered into the component
//! `Linker` as ordinary host functions via the PUBLIC generated
//! `wasmtime_wasi::p2::bindings::filesystem::types::add_to_linker`, parameterised by a
//! `HasData` view type. By supplying our OWN view type that forwards every method to
//! wasmtime-wasi's audited `WasiFilesystemCtxView` implementation -- intercepting only
//! `open-at` (and `stat-at`) to observe the returned `Err(ErrorCode::*)` -- we get a
//! synchronous, host-side denial signal WITH the raw guest path in hand, and WITHOUT
//! forking wasmtime-wasi and WITHOUT reimplementing any filesystem semantics.
//!
//! Two things are proven here:
//!   1. `install_observed_filesystem` type-checks and installs on a real component
//!      `Linker`, shadowing the default `wasi:filesystem/types` binding
//!      (the exact integration wasmtime4j-native would perform after
//!      `add_to_linker_async`).
//!   2. Calling `open-at` for a denied path drives wasmtime-wasi's real enforcement
//!      and the observer fires with the attempted path -- exercised directly, no
//!      guest component required.
//!
//! Run with:
//!   cargo run -p wasmtime4j-native --example fs_denial_hook
//!
//! This file is an example target: it is NOT compiled by a normal `cargo build`
//! and does not participate in the cdylib, so it cannot destabilise the shipped build.

use std::sync::atomic::{AtomicU64, Ordering};

use wasmtime::component::{HasData, Linker, Resource, ResourceTable};
use wasmtime::{Config, Engine, Store};

use wasmtime_wasi::filesystem::{Descriptor, WasiFilesystemCtxView, WasiFilesystemView};
use wasmtime_wasi::p2::bindings::filesystem::preopens;
use wasmtime_wasi::p2::bindings::filesystem::types::{
    self, Host as FsTypesHost, HostDescriptor, HostDirectoryEntryStream,
};
use wasmtime_wasi::p2::{DynInputStream, DynOutputStream, FsError, FsResult};
use wasmtime_wasi::{DirPerms, FilePerms, WasiCtx, WasiCtxBuilder, WasiCtxView, WasiView};

/// Denials observed by the interposing filesystem view (process-global for the spike;
/// production would carry a `DenialSink` on the view instead).
static OBSERVED_DENIALS: AtomicU64 = AtomicU64::new(0);

/// Store state implementing `WasiView`.
struct SpikeState {
    ctx: WasiCtx,
    table: ResourceTable,
}

impl WasiView for SpikeState {
    fn ctx(&mut self) -> WasiCtxView<'_> {
        WasiCtxView {
            ctx: &mut self.ctx,
            table: &mut self.table,
        }
    }
}

/// `HasData` marker selecting our interposing view as the data type the generated
/// `filesystem::types::add_to_linker` will hand to each host function.
struct ObservedFs;

impl HasData for ObservedFs {
    type Data<'a> = ObservedFsView<'a>;
}

/// Interposing `wasi:filesystem/types` view. Wraps wasmtime-wasi's own
/// `WasiFilesystemCtxView` and forwards every method to it, observing denials on the
/// path-taking operations before returning the (unmodified) result to the guest.
struct ObservedFsView<'a> {
    inner: WasiFilesystemCtxView<'a>,
}

impl<'a> ObservedFsView<'a> {
    fn new(inner: WasiFilesystemCtxView<'a>) -> Self {
        Self { inner }
    }
}

/// The denial-observation seam: called for every path-based open/stat that
/// wasmtime-wasi's real implementation refused. `path` is the raw guest-supplied
/// string -- exactly the datum the prior finding said was unavailable on this path.
fn observe_denial(op: &str, path: &str, err: &FsError) {
    OBSERVED_DENIALS.fetch_add(1, Ordering::SeqCst);
    // In production this becomes a structured DenialEvent emitted to the host sink /
    // JNI callback. For the spike, print it.
    eprintln!("[fs-denial] op={op} path={path:?} -> {err:?}");
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.Host  (free functions)
// ---------------------------------------------------------------------------
impl FsTypesHost for ObservedFsView<'_> {
    fn convert_error_code(&mut self, err: FsError) -> wasmtime::Result<types::ErrorCode> {
        FsTypesHost::convert_error_code(&mut self.inner, err)
    }

    fn filesystem_error_code(
        &mut self,
        err: Resource<wasmtime::Error>,
    ) -> wasmtime::Result<Option<types::ErrorCode>> {
        FsTypesHost::filesystem_error_code(&mut self.inner, err)
    }
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.HostDescriptor  (methods on `descriptor`)
// ---------------------------------------------------------------------------
impl HostDescriptor for ObservedFsView<'_> {
    async fn advise(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
        len: types::Filesize,
        advice: types::Advice,
    ) -> FsResult<()> {
        HostDescriptor::advise(&mut self.inner, fd, offset, len, advice).await
    }

    async fn sync_data(&mut self, fd: Resource<types::Descriptor>) -> FsResult<()> {
        HostDescriptor::sync_data(&mut self.inner, fd).await
    }

    async fn get_flags(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::DescriptorFlags> {
        HostDescriptor::get_flags(&mut self.inner, fd).await
    }

    async fn get_type(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::DescriptorType> {
        HostDescriptor::get_type(&mut self.inner, fd).await
    }

    async fn set_size(
        &mut self,
        fd: Resource<types::Descriptor>,
        size: types::Filesize,
    ) -> FsResult<()> {
        HostDescriptor::set_size(&mut self.inner, fd, size).await
    }

    async fn set_times(
        &mut self,
        fd: Resource<types::Descriptor>,
        atim: types::NewTimestamp,
        mtim: types::NewTimestamp,
    ) -> FsResult<()> {
        HostDescriptor::set_times(&mut self.inner, fd, atim, mtim).await
    }

    async fn read(
        &mut self,
        fd: Resource<types::Descriptor>,
        len: types::Filesize,
        offset: types::Filesize,
    ) -> FsResult<(Vec<u8>, bool)> {
        HostDescriptor::read(&mut self.inner, fd, len, offset).await
    }

    async fn write(
        &mut self,
        fd: Resource<types::Descriptor>,
        buf: Vec<u8>,
        offset: types::Filesize,
    ) -> FsResult<types::Filesize> {
        HostDescriptor::write(&mut self.inner, fd, buf, offset).await
    }

    async fn read_directory(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<Resource<types::DirectoryEntryStream>> {
        HostDescriptor::read_directory(&mut self.inner, fd).await
    }

    async fn sync(&mut self, fd: Resource<types::Descriptor>) -> FsResult<()> {
        HostDescriptor::sync(&mut self.inner, fd).await
    }

    async fn create_directory_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        HostDescriptor::create_directory_at(&mut self.inner, fd, path).await
    }

    async fn stat(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::DescriptorStat> {
        HostDescriptor::stat(&mut self.inner, fd).await
    }

    async fn stat_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
    ) -> FsResult<types::DescriptorStat> {
        let observed_path = path.clone();
        let r = HostDescriptor::stat_at(&mut self.inner, fd, path_flags, path).await;
        if let Err(e) = &r {
            observe_denial("stat-at", &observed_path, e);
        }
        r
    }

    async fn set_times_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
        atim: types::NewTimestamp,
        mtim: types::NewTimestamp,
    ) -> FsResult<()> {
        HostDescriptor::set_times_at(&mut self.inner, fd, path_flags, path, atim, mtim).await
    }

    async fn link_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        old_path_flags: types::PathFlags,
        old_path: String,
        new_descriptor: Resource<types::Descriptor>,
        new_path: String,
    ) -> FsResult<()> {
        HostDescriptor::link_at(
            &mut self.inner,
            fd,
            old_path_flags,
            old_path,
            new_descriptor,
            new_path,
        )
        .await
    }

    async fn open_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
        oflags: types::OpenFlags,
        flags: types::DescriptorFlags,
    ) -> FsResult<Resource<types::Descriptor>> {
        // Raw guest path captured BEFORE delegating -- available here, unlike inside
        // wasmtime-wasi's internal open_at where no host frame runs on refusal.
        let observed_path = path.clone();
        let r = HostDescriptor::open_at(&mut self.inner, fd, path_flags, path, oflags, flags).await;
        if let Err(e) = &r {
            // This is the denial signal the component/preopen path previously lacked.
            observe_denial("open-at", &observed_path, e);
        }
        r
    }

    fn drop(&mut self, fd: Resource<types::Descriptor>) -> wasmtime::Result<()> {
        HostDescriptor::drop(&mut self.inner, fd)
    }

    async fn readlink_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<String> {
        HostDescriptor::readlink_at(&mut self.inner, fd, path).await
    }

    async fn remove_directory_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        HostDescriptor::remove_directory_at(&mut self.inner, fd, path).await
    }

    async fn rename_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        old_path: String,
        new_fd: Resource<types::Descriptor>,
        new_path: String,
    ) -> FsResult<()> {
        HostDescriptor::rename_at(&mut self.inner, fd, old_path, new_fd, new_path).await
    }

    async fn symlink_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        src_path: String,
        dest_path: String,
    ) -> FsResult<()> {
        HostDescriptor::symlink_at(&mut self.inner, fd, src_path, dest_path).await
    }

    async fn unlink_file_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        HostDescriptor::unlink_file_at(&mut self.inner, fd, path).await
    }

    fn read_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
    ) -> FsResult<Resource<DynInputStream>> {
        HostDescriptor::read_via_stream(&mut self.inner, fd, offset)
    }

    fn write_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
    ) -> FsResult<Resource<DynOutputStream>> {
        HostDescriptor::write_via_stream(&mut self.inner, fd, offset)
    }

    fn append_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<Resource<DynOutputStream>> {
        HostDescriptor::append_via_stream(&mut self.inner, fd)
    }

    async fn is_same_object(
        &mut self,
        a: Resource<types::Descriptor>,
        b: Resource<types::Descriptor>,
    ) -> wasmtime::Result<bool> {
        HostDescriptor::is_same_object(&mut self.inner, a, b).await
    }

    async fn metadata_hash(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::MetadataHashValue> {
        HostDescriptor::metadata_hash(&mut self.inner, fd).await
    }

    async fn metadata_hash_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
    ) -> FsResult<types::MetadataHashValue> {
        HostDescriptor::metadata_hash_at(&mut self.inner, fd, path_flags, path).await
    }
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.HostDirectoryEntryStream
// ---------------------------------------------------------------------------
impl HostDirectoryEntryStream for ObservedFsView<'_> {
    async fn read_directory_entry(
        &mut self,
        stream: Resource<types::DirectoryEntryStream>,
    ) -> FsResult<Option<types::DirectoryEntry>> {
        HostDirectoryEntryStream::read_directory_entry(&mut self.inner, stream).await
    }

    fn drop(&mut self, stream: Resource<types::DirectoryEntryStream>) -> wasmtime::Result<()> {
        HostDirectoryEntryStream::drop(&mut self.inner, stream)
    }
}

/// Getter handed to the generated `add_to_linker`: produces the interposing view.
fn observed_fs_getter(state: &mut SpikeState) -> ObservedFsView<'_> {
    ObservedFsView::new(state.filesystem())
}

/// Installs the interposing `wasi:filesystem/types` binding onto `linker`, shadowing
/// whatever the default `add_to_linker_async` registered. This is the exact call
/// wasmtime4j-native would add after `wasmtime_wasi::p2::add_to_linker_async(linker)`.
fn install_observed_filesystem(linker: &mut Linker<SpikeState>) -> wasmtime::Result<()> {
    linker.allow_shadowing(true);
    types::add_to_linker::<SpikeState, ObservedFs>(linker, observed_fs_getter)?;
    Ok(())
}

fn main() -> wasmtime::Result<()> {
    // ---- Proof 1: the interposition installs on a real component Linker ----
    let mut config = Config::new();
    config.wasm_component_model(true);
    let engine = Engine::new(&config)?;

    let mut linker: Linker<SpikeState> = Linker::new(&engine);
    wasmtime_wasi::p2::add_to_linker_async(&mut linker)?;
    install_observed_filesystem(&mut linker)?;
    println!("proof-1 OK: observed wasi:filesystem/types installed (shadowing default)");

    // ---- Proof 2: the observer fires on a denied open, no guest needed ----
    // Preopen a fresh temp dir (read-only) holding exactly one real file.
    let tmp = std::env::temp_dir().join(format!("fs-denial-spike-{}", std::process::id()));
    std::fs::create_dir_all(&tmp)?;
    std::fs::write(tmp.join("allowed.txt"), b"ok")?;

    let mut builder = WasiCtxBuilder::new();
    builder.allow_blocking_current_thread(true); // makes open_at resolve synchronously
    builder.preopened_dir(&tmp, "/", DirPerms::READ, FilePerms::READ)?;
    let ctx = builder.build();

    let mut state = SpikeState {
        ctx,
        table: ResourceTable::new(),
    };
    // A Store is required so the WasiCtx's resource table is well-formed for opens.
    let _store: Store<()> = Store::new(&engine, ());

    // Helper: fetch a fresh root-preopen descriptor resource.
    let fresh_root = |state: &mut SpikeState| -> wasmtime::Result<Resource<Descriptor>> {
        let mut view = state.filesystem();
        let dirs = preopens::Host::get_directories(&mut view)?;
        Ok(dirs.into_iter().next().expect("one preopen").0)
    };

    let before = OBSERVED_DENIALS.load(Ordering::SeqCst);

    // (a) Allowed open -> succeeds, observer must NOT fire.
    {
        let root = fresh_root(&mut state)?;
        let mut observed = ObservedFsView::new(state.filesystem());
        let r = futures::executor::block_on(HostDescriptor::open_at(
            &mut observed,
            root,
            types::PathFlags::empty(),
            "allowed.txt".to_string(),
            types::OpenFlags::empty(),
            types::DescriptorFlags::READ,
        ));
        assert!(r.is_ok(), "allowed open should succeed: {r:?}");
    }
    assert_eq!(
        OBSERVED_DENIALS.load(Ordering::SeqCst),
        before,
        "allowed open must not register a denial"
    );
    println!("proof-2a OK: allowed open succeeded, no denial observed");

    // (b) Denied open: file absent from the preopen -> wasmtime-wasi refuses,
    //     observer MUST fire with the raw guest path.
    {
        let root = fresh_root(&mut state)?;
        let mut observed = ObservedFsView::new(state.filesystem());
        let r = futures::executor::block_on(HostDescriptor::open_at(
            &mut observed,
            root,
            types::PathFlags::empty(),
            "secret-not-materialised.txt".to_string(),
            types::OpenFlags::empty(),
            types::DescriptorFlags::READ,
        ));
        assert!(r.is_err(), "denied open should fail");
    }

    // (c) Denied open: traversal escape out of the preopen.
    {
        let root = fresh_root(&mut state)?;
        let mut observed = ObservedFsView::new(state.filesystem());
        let r = futures::executor::block_on(HostDescriptor::open_at(
            &mut observed,
            root,
            types::PathFlags::empty(),
            "../../../../etc/passwd".to_string(),
            types::OpenFlags::empty(),
            types::DescriptorFlags::READ,
        ));
        assert!(r.is_err(), "traversal escape should fail");
    }

    let after = OBSERVED_DENIALS.load(Ordering::SeqCst);
    assert_eq!(after - before, 2, "expected exactly two observed denials");
    println!("proof-2bc OK: {} denials observed on the component/preopen path", after - before);

    let _ = std::fs::remove_dir_all(&tmp);
    println!("SPIKE PASSED: host-observable fs-denial hook is feasible without forking wasmtime-wasi");
    Ok(())
}
