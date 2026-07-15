//! Host-observable filesystem-denial interposition for the component / WASI-preopen path.
//!
//! Productionised from `wasmtime4j-native/examples/fs_denial_hook.rs` (spike committed as
//! `f9da3825`) and the design writeup `~/git/svalinn/handoff/denial-signals/FS-DENIAL-HOOK.md`
//! §2(a-linker).
//!
//! On the component/WASI-preopen path, `wasmtime-wasi` enforces preopen bounds internally
//! (`wasmtime_wasi::filesystem::Dir::open_at`) and refuses an out-of-preopen / missing open with
//! NO host-visible callback. A host-observable interception point DOES exist one layer up: the
//! `wasi:filesystem/types` interface is registered into the component `Linker` as ordinary host
//! functions via the generated `add_to_linker`, parameterised by a `HasData` view type. By
//! supplying our OWN view type that forwards every method to `wasmtime-wasi`'s audited
//! `WasiFilesystemCtxView` -- intercepting only `open-at` and `stat-at` to OBSERVE the returned
//! `Err(ErrorCode::*)` -- we get a synchronous, host-side denial signal WITH the raw guest path
//! in hand, WITHOUT forking `wasmtime-wasi` and WITHOUT reimplementing any filesystem semantics.
//!
//! This module mirrors the SYNC WASI bindings (`bindings::sync::filesystem::types`), because the
//! capability-policy instantiation path (`instantiate_component_with_wasi`) installs WASI via
//! `wasmtime_wasi::p2::add_to_linker_sync`. The interposition is OBSERVE-ONLY: it never changes
//! enforcement -- `wasmtime-wasi` still performs the real open and returns the real `Err`
//! unchanged; the observer merely sees a copy of the raw path, the operation, and the classified
//! `FsError` reason before that `Err` flows back to the guest.
//!
//! The interposition is installed ONLY when a `CallbackFsAccessObserver` is configured; with no
//! observer the default `wasi:filesystem/types` binding is left untouched (zero overhead, no
//! behaviour change).

#![cfg(feature = "wasi")]

use wasmtime::component::{HasData, Linker, Resource};
use wasmtime_wasi::filesystem::{WasiFilesystemCtxView, WasiFilesystemView};
// The async ErrorCode is the payload of `FsError` (`TrappableError<types::ErrorCode>`); we
// downcast to it to classify a denial reason regardless of the sync/async binding in use.
use wasmtime_wasi::p2::bindings::filesystem::types as async_types;
// The sync `wasi:filesystem/types` bindings -- the ones `add_to_linker_sync` registers.
use wasmtime_wasi::p2::bindings::sync::filesystem::types;
use wasmtime_wasi::p2::bindings::sync::io::streams;
use wasmtime_wasi::p2::{FsError, FsResult};

use crate::component::{CallbackFsAccessObserver, ComponentStoreData};

/// `HasData` marker selecting our interposing view as the data type the generated
/// `filesystem::types::add_to_linker` will hand to each host function.
pub struct ObservedFs;

impl HasData for ObservedFs {
    type Data<'a> = ObservedFsView<'a>;
}

/// Interposing `wasi:filesystem/types` view. Wraps `wasmtime-wasi`'s own `WasiFilesystemCtxView`
/// and forwards every method to it, observing denials on the path-taking operations before
/// returning the (unmodified) result to the guest.
pub struct ObservedFsView<'a> {
    inner: WasiFilesystemCtxView<'a>,
    observer: Option<CallbackFsAccessObserver>,
}

/// Maps an `FsError` to `(reason, discriminant)` where `reason` is the kebab-case WIT
/// `error-code` name (e.g. "not-permitted", "no-entry", "access") and `discriminant` is its
/// numeric position in the `wasi:filesystem/types.error-code` enum. A trap (no classifiable
/// `ErrorCode`) surfaces as `("unknown", -1)`.
fn classify(err: &FsError) -> (&'static str, i32) {
    let code = match err.downcast_ref() {
        Some(code) => code,
        None => return ("unknown", -1),
    };
    use async_types::ErrorCode as E;
    match code {
        E::Access => ("access", 0),
        E::WouldBlock => ("would-block", 1),
        E::Already => ("already", 2),
        E::BadDescriptor => ("bad-descriptor", 3),
        E::Busy => ("busy", 4),
        E::Deadlock => ("deadlock", 5),
        E::Quota => ("quota", 6),
        E::Exist => ("exist", 7),
        E::FileTooLarge => ("file-too-large", 8),
        E::IllegalByteSequence => ("illegal-byte-sequence", 9),
        E::InProgress => ("in-progress", 10),
        E::Interrupted => ("interrupted", 11),
        E::Invalid => ("invalid", 12),
        E::Io => ("io", 13),
        E::IsDirectory => ("is-directory", 14),
        E::Loop => ("loop", 15),
        E::TooManyLinks => ("too-many-links", 16),
        E::MessageSize => ("message-size", 17),
        E::NameTooLong => ("name-too-long", 18),
        E::NoDevice => ("no-device", 19),
        E::NoEntry => ("no-entry", 20),
        E::NoLock => ("no-lock", 21),
        E::InsufficientMemory => ("insufficient-memory", 22),
        E::InsufficientSpace => ("insufficient-space", 23),
        E::NotDirectory => ("not-directory", 24),
        E::NotEmpty => ("not-empty", 25),
        E::NotRecoverable => ("not-recoverable", 26),
        E::Unsupported => ("unsupported", 27),
        E::NoTty => ("no-tty", 28),
        E::NoSuchDevice => ("no-such-device", 29),
        E::Overflow => ("overflow", 30),
        E::NotPermitted => ("not-permitted", 31),
        E::Pipe => ("pipe", 32),
        E::ReadOnly => ("read-only", 33),
        E::InvalidSeek => ("invalid-seek", 34),
        E::TextFileBusy => ("text-file-busy", 35),
        E::CrossDevice => ("cross-device", 36),
    }
}

impl ObservedFsView<'_> {
    /// The denial-observation seam: called for every path-based open/stat that `wasmtime-wasi`'s
    /// real implementation refused. `path` is the raw guest-supplied string.
    fn observe(&self, op: &str, path: &str, err: &FsError) {
        let observer = match self.observer {
            Some(o) => o,
            None => return,
        };
        let (reason, code) = classify(err);
        (observer.observe_fn)(
            observer.callback_id,
            path.as_ptr(),
            path.len(),
            op.as_ptr(),
            op.len(),
            reason.as_ptr(),
            reason.len(),
            code,
        );
    }
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.Host  (free functions)
// ---------------------------------------------------------------------------
impl types::Host for ObservedFsView<'_> {
    fn convert_error_code(&mut self, err: FsError) -> wasmtime::Result<types::ErrorCode> {
        types::Host::convert_error_code(&mut self.inner, err)
    }

    fn filesystem_error_code(
        &mut self,
        err: Resource<streams::Error>,
    ) -> wasmtime::Result<Option<types::ErrorCode>> {
        types::Host::filesystem_error_code(&mut self.inner, err)
    }
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.HostDescriptor  (methods on `descriptor`)
// ---------------------------------------------------------------------------
impl types::HostDescriptor for ObservedFsView<'_> {
    fn advise(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
        len: types::Filesize,
        advice: types::Advice,
    ) -> FsResult<()> {
        types::HostDescriptor::advise(&mut self.inner, fd, offset, len, advice)
    }

    fn sync_data(&mut self, fd: Resource<types::Descriptor>) -> FsResult<()> {
        types::HostDescriptor::sync_data(&mut self.inner, fd)
    }

    fn get_flags(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::DescriptorFlags> {
        types::HostDescriptor::get_flags(&mut self.inner, fd)
    }

    fn get_type(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::DescriptorType> {
        types::HostDescriptor::get_type(&mut self.inner, fd)
    }

    fn set_size(
        &mut self,
        fd: Resource<types::Descriptor>,
        size: types::Filesize,
    ) -> FsResult<()> {
        types::HostDescriptor::set_size(&mut self.inner, fd, size)
    }

    fn set_times(
        &mut self,
        fd: Resource<types::Descriptor>,
        atim: types::NewTimestamp,
        mtim: types::NewTimestamp,
    ) -> FsResult<()> {
        types::HostDescriptor::set_times(&mut self.inner, fd, atim, mtim)
    }

    fn read(
        &mut self,
        fd: Resource<types::Descriptor>,
        len: types::Filesize,
        offset: types::Filesize,
    ) -> FsResult<(Vec<u8>, bool)> {
        types::HostDescriptor::read(&mut self.inner, fd, len, offset)
    }

    fn write(
        &mut self,
        fd: Resource<types::Descriptor>,
        buf: Vec<u8>,
        offset: types::Filesize,
    ) -> FsResult<types::Filesize> {
        types::HostDescriptor::write(&mut self.inner, fd, buf, offset)
    }

    fn read_directory(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<Resource<types::DirectoryEntryStream>> {
        types::HostDescriptor::read_directory(&mut self.inner, fd)
    }

    fn sync(&mut self, fd: Resource<types::Descriptor>) -> FsResult<()> {
        types::HostDescriptor::sync(&mut self.inner, fd)
    }

    fn create_directory_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::create_directory_at(&mut self.inner, fd, path)
    }

    fn stat(&mut self, fd: Resource<types::Descriptor>) -> FsResult<types::DescriptorStat> {
        types::HostDescriptor::stat(&mut self.inner, fd)
    }

    fn stat_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
    ) -> FsResult<types::DescriptorStat> {
        // Raw guest path captured BEFORE delegating -- available here, unlike inside
        // wasmtime-wasi's internal stat_at where no host frame runs on refusal.
        let observed_path = path.clone();
        let r = types::HostDescriptor::stat_at(&mut self.inner, fd, path_flags, path);
        if let Err(e) = &r {
            self.observe("stat-at", &observed_path, e);
        }
        r
    }

    fn set_times_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
        atim: types::NewTimestamp,
        mtim: types::NewTimestamp,
    ) -> FsResult<()> {
        types::HostDescriptor::set_times_at(&mut self.inner, fd, path_flags, path, atim, mtim)
    }

    fn link_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        old_path_flags: types::PathFlags,
        old_path: String,
        new_descriptor: Resource<types::Descriptor>,
        new_path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::link_at(
            &mut self.inner,
            fd,
            old_path_flags,
            old_path,
            new_descriptor,
            new_path,
        )
    }

    fn open_at(
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
        let r = types::HostDescriptor::open_at(&mut self.inner, fd, path_flags, path, oflags, flags);
        if let Err(e) = &r {
            // This is the denial signal the component/preopen path previously lacked.
            self.observe("open-at", &observed_path, e);
        }
        r
    }

    fn drop(&mut self, fd: Resource<types::Descriptor>) -> wasmtime::Result<()> {
        types::HostDescriptor::drop(&mut self.inner, fd)
    }

    fn readlink_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<String> {
        types::HostDescriptor::readlink_at(&mut self.inner, fd, path)
    }

    fn remove_directory_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::remove_directory_at(&mut self.inner, fd, path)
    }

    fn rename_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        old_path: String,
        new_fd: Resource<types::Descriptor>,
        new_path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::rename_at(&mut self.inner, fd, old_path, new_fd, new_path)
    }

    fn symlink_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        src_path: String,
        dest_path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::symlink_at(&mut self.inner, fd, src_path, dest_path)
    }

    fn unlink_file_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path: String,
    ) -> FsResult<()> {
        types::HostDescriptor::unlink_file_at(&mut self.inner, fd, path)
    }

    fn read_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
    ) -> FsResult<Resource<streams::InputStream>> {
        types::HostDescriptor::read_via_stream(&mut self.inner, fd, offset)
    }

    fn write_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
        offset: types::Filesize,
    ) -> FsResult<Resource<streams::OutputStream>> {
        types::HostDescriptor::write_via_stream(&mut self.inner, fd, offset)
    }

    fn append_via_stream(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<Resource<streams::OutputStream>> {
        types::HostDescriptor::append_via_stream(&mut self.inner, fd)
    }

    fn is_same_object(
        &mut self,
        a: Resource<types::Descriptor>,
        b: Resource<types::Descriptor>,
    ) -> wasmtime::Result<bool> {
        types::HostDescriptor::is_same_object(&mut self.inner, a, b)
    }

    fn metadata_hash(
        &mut self,
        fd: Resource<types::Descriptor>,
    ) -> FsResult<types::MetadataHashValue> {
        types::HostDescriptor::metadata_hash(&mut self.inner, fd)
    }

    fn metadata_hash_at(
        &mut self,
        fd: Resource<types::Descriptor>,
        path_flags: types::PathFlags,
        path: String,
    ) -> FsResult<types::MetadataHashValue> {
        types::HostDescriptor::metadata_hash_at(&mut self.inner, fd, path_flags, path)
    }
}

// ---------------------------------------------------------------------------
// wasi:filesystem/types.HostDirectoryEntryStream
// ---------------------------------------------------------------------------
impl types::HostDirectoryEntryStream for ObservedFsView<'_> {
    fn read_directory_entry(
        &mut self,
        stream: Resource<types::DirectoryEntryStream>,
    ) -> FsResult<Option<types::DirectoryEntry>> {
        types::HostDirectoryEntryStream::read_directory_entry(&mut self.inner, stream)
    }

    fn drop(&mut self, stream: Resource<types::DirectoryEntryStream>) -> wasmtime::Result<()> {
        types::HostDirectoryEntryStream::drop(&mut self.inner, stream)
    }
}

/// Getter handed to the generated `add_to_linker`: produces the interposing view, carrying the
/// configured observer copied out of the store data (a `Copy` fn-pointer + id).
fn observed_fs_getter(state: &mut ComponentStoreData) -> ObservedFsView<'_> {
    let observer = state.fs_access_observer;
    ObservedFsView {
        inner: state.filesystem(),
        observer,
    }
}

/// Installs the interposing `wasi:filesystem/types` binding onto `linker`, shadowing the default
/// binding registered by `wasmtime_wasi::p2::add_to_linker_sync`. Call this AFTER that default
/// binding and ONLY when an observer is configured.
pub fn install_observed_filesystem(
    linker: &mut Linker<ComponentStoreData>,
) -> wasmtime::Result<()> {
    linker.allow_shadowing(true);
    types::add_to_linker::<ComponentStoreData, ObservedFs>(linker, observed_fs_getter)?;
    Ok(())
}
