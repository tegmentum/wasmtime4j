# Wasmtime GC API Migration Guide
**Wasmtime Version**: 37.0.2
**Target**: Fix 91 compilation errors in GC modules
**Date**: 2025-11-03

## Migration Summary

The GC implementation was written for an older Wasmtime version. This guide documents the API changes and provides patterns for systematic migration.

## Critical API Changes

### 1. I31Ref → I31

**Old API** (removed in 37.0.2):
```rust
use wasmtime::I31Ref;

match I31Ref::new(value) {
    Some(i31_ref) => {
        let rooted = i31_ref.to_any_ref().rooted(&mut store);
        // ...
    }
    None => // handle error
}

// Later retrieval:
if let Some(i31_ref) = rooted_ref.as_ref().i31_ref() {
    let signed_value = i31_ref.get_s();
    let unsigned_value = i31_ref.get_u();
}
```

**New API** (37.0.2):
```rust
use wasmtime::I31;

// Create I31 - now returns Option<I31>
let i31 = I31::new_i32(value)?;  // or I31::wrapping_i32(value)

// Convert to AnyRef and root (pattern may vary)
let any_ref = i31.to_any()?;  // Check actual API
let rooted = any_ref.rooted(&mut store);

// Later retrieval:
if let Some(i31) = rooted.as_i31()? {  // Check actual dereferencing pattern
    let signed_value = i31.get_i32();
    let unsigned_value = i31.get_u32();
}
```

**Migration Pattern**:
```bash
# Find: I31Ref::new\(
# Replace: I31::new_i32(

# Find: \.get_s\(\)
# Replace: .get_i32()

# Find: \.get_u\(\)
# Replace: .get_u32()

# Find: wasmtime::I31Ref
# Replace: wasmtime::I31
```

**Affected Files**:
- `gc_operations.rs:842` - Constructor
- `gc_operations.rs:892` - Retrieval method i31_ref()
- `gc_operations.rs:896-898` - get_s()/get_u()
- `gc_operations.rs:1072` - GcReferenceType::I31Ref pattern match
- `gc_operations.rs:1155` - ref_test with I31Ref

### 2. Rooted<T> Trait Bounds

**Issue**: `Rooted<AnyRef>::as_ref()` trait bounds changed (18+ errors)

**Investigation Needed**:
```rust
// Current pattern (may not work):
let rooted_ref: Rooted<AnyRef> = ...;
rooted_ref.as_ref()  // ERROR: trait bounds not satisfied

// Potential fix 1: Dereference directly
(*rooted_ref).some_method()

// Potential fix 2: Use Deref trait
use std::ops::Deref;
rooted_ref.deref().some_method()

// Potential fix 3: Check if pattern changed
// Wasmtime docs say "Rooted<T> dereferences to its underlying T"
rooted_ref.some_method()  // Direct call without as_ref()
```

**Test Pattern**:
```rust
// Example to validate in small test:
let i31 = I31::new_i32(42)?;
let any = i31.to_any()?;
let rooted = any.rooted(&mut store);

// Try each:
// rooted.as_ref()
// *rooted
// rooted.deref()
// rooted.method()
```

### 3. FieldType Constructor (18+ errors)

**Old API** (removed):
```rust
FieldType::I32
FieldType::I64
FieldType::F32
FieldType::F64
```

**New API**:
```rust
use wasmtime::{FieldType, Mutability, StorageType, ValType};

// For value types:
FieldType::new(Mutability::Var, ValType::I32.into())
FieldType::new(Mutability::Const, ValType::I64.into())

// For packed types:
FieldType::new(Mutability::Var, StorageType::I8)
FieldType::new(Mutability::Const, StorageType::I16)
```

**Note**: The project has a custom `FieldType` enum in `gc_types.rs`. These errors likely refer to **Wasmtime's FieldType** being used to create GC types, not the custom enum.

**Search for Wasmtime FieldType usage**:
```bash
# Find places where Wasmtime FieldType is constructed
# (not our custom enum matches)
grep -n "wasmtime::FieldType" gc_operations.rs
```

### 4. StorageType::Val Variant (3 errors)

**Old API** (removed):
```rust
match storage_type {
    StorageType::Val(val_type) => // handle value type
    StorageType::I8 => // handle packed i8
    StorageType::I16 => // handle packed i16
}
```

**New API**:
```rust
// StorageType no longer has Val variant
// Value types use From<ValType>:
let storage: StorageType = ValType::I32.into();
let field = FieldType::new(Mutability::Var, storage);

// Check with is_* methods:
if storage.is_val_type() {  // Check actual API
    // ...
}
```

### 5. V128::to_le_bytes() (2 errors)

**Old API**:
```rust
let v128_value: V128 = ...;
let bytes = v128_value.to_le_bytes();
```

**New API**:
```rust
// Investigate actual V128 API in 37.0.2
let v128_value: V128 = ...;
let bytes = v128_value.as_u128().to_le_bytes();  // Possible fix
// OR
let bytes: [u8; 16] = unsafe { std::mem::transmute(v128_value) };
```

### 6. WasmtimeError::from_string (8 errors)

**Location**: `error.rs`

**Old API**:
```rust
WasmtimeError::from_string("error message")
```

**New API**:
```rust
// Option 1: Use existing variant
WasmtimeError::Runtime { message: "error message".to_string() }

// Option 2: Add helper method to error.rs
impl WasmtimeError {
    pub fn from_string(msg: impl Into<String>) -> Self {
        WasmtimeError::Runtime { message: msg.into() }
    }
}
```

## Systematic Fix Procedure

### Phase 1: Add Helper Methods (30 minutes)

**File**: `src/error.rs`
```rust
impl WasmtimeError {
    /// Create error from string (migration helper)
    pub fn from_string(msg: impl Into<String>) -> Self {
        WasmtimeError::Runtime {
            message: msg.into(),
        }
    }
}
```

### Phase 2: Fix I31 Operations (1-2 hours)

**File**: `src/gc_operations.rs`

1. **Line 842** - i31_new function:
```rust
// OLD:
match wasmtime::I31Ref::new(value) {
    Some(i31_ref) => {
        let rooted_ref = i31_ref.to_any_ref().rooted(&mut self.store);
        // ...
    }

// NEW:
let i31 = match wasmtime::I31::new_i32(value) {
    Some(i31) => i31,
    None => {
        return RealRefOperationResult {
            success: false,
            error: Some("Failed to create I31 reference".to_string()),
            // ...
        };
    }
};

// Convert to AnyRef (investigate exact method)
let any_ref = i31.to_any()?;  // or similar
let rooted_ref = any_ref.rooted(&mut self.store);
```

2. **Line 892-898** - i31_get function:
```rust
// OLD:
match rooted_ref.as_ref().i31_ref() {
    Some(i31_ref) => {
        let value = if signed {
            i31_ref.get_s()
        } else {
            i31_ref.get_u() as i32
        };

// NEW:
let i31 = match rooted_ref.as_i31() {  // or similar method
    Some(i31) => i31,
    None => {
        return RealRefOperationResult {
            success: false,
            error: Some("Not an I31 reference".to_string()),
            // ...
        };
    }
};

let value = if signed {
    i31.get_i32()
} else {
    i31.get_u32() as i32
};
```

3. **Line 1072** - ref_cast_to_heap_type:
```rust
// OLD:
GcReferenceType::I31Ref => HeapType::I31,

// NEW: (likely unchanged, this is our enum not Wasmtime's)
GcReferenceType::I31Ref => HeapType::I31,
```

### Phase 3: Fix Rooted<AnyRef> Issues (2-3 hours)

**Pattern**: Search for `.as_ref()` calls on `Rooted<AnyRef>`

```bash
grep -n "\.as_ref()" src/gc_operations.rs
```

**Test Each Fix**:
1. Try removing `.as_ref()` (Rooted dereferences to T)
2. Try `*rooted` instead
3. Compile and check errors

### Phase 4: Fix FieldType/StorageType (2-3 hours)

Search for Wasmtime FieldType construction patterns and update systematically.

### Phase 5: Enable Modules (1 hour)

1. Uncomment in `src/lib.rs`:
```rust
pub mod gc_operations;
pub mod gc;
```

2. Compile and fix remaining cascading errors

3. Uncomment jni_gc_bindings.rs

## Quick Win: Test Minimal Fix

Before full migration, test the pattern with minimal changes:

```bash
cd wasmtime4j-native

# Create test file
cat > /tmp/i31_test.rs << 'EOF'
use wasmtime::*;

fn main() -> Result<()> {
    let mut config = Config::new();
    config.wasm_gc(true);

    let engine = Engine::new(&config)?;
    let mut store = Store::new(&engine, ());

    // Test new I31 API
    let i31 = I31::new_i32(42).ok_or_else(|| anyhow!("Failed to create I31"))?;
    println!("Created I31: {}", i31.get_i32());

    // Test rooting
    let any = /* figure out conversion */;
    let rooted = any.rooted(&mut store);

    Ok(())
}
EOF

# Test compilation
rustc --edition 2021 --extern wasmtime=/path/to/wasmtime /tmp/i31_test.rs
```

## Estimated Timeline

| Phase | Time | Description |
|-------|------|-------------|
| Phase 1 | 30m | Add WasmtimeError::from_string helper |
| Phase 2 | 2h | Fix I31 operations (4 locations) |
| Phase 3 | 3h | Fix Rooted<AnyRef> issues (~18 errors) |
| Phase 4 | 3h | Fix FieldType/StorageType (~21 errors) |
| Phase 5 | 1h | Enable modules and fix cascading errors |
| **Total** | **9-10h** | Systematic migration |

## Validation Checklist

After each phase:
- [ ] Code compiles without errors in that file
- [ ] No new warnings introduced
- [ ] Pattern is consistent across similar code
- [ ] Test the fix with minimal example

Final validation:
- [ ] All modules uncommented in lib.rs
- [ ] Full codebase compiles: `cargo build --release --features jni-bindings`
- [ ] No GC-related compilation errors
- [ ] Create simple GC test to verify functionality

## References

- **Wasmtime I31 Docs**: https://docs.wasmtime.dev/api/wasmtime/struct.I31.html
- **Wasmtime FieldType Docs**: https://docs.wasmtime.dev/api/wasmtime/struct.FieldType.html
- **Wasmtime Rooted Docs**: https://docs.wasmtime.dev/api/wiggle/wasmtime_crate/struct.Rooted.html
- **GC Implementation Status**: See `GC_IMPLEMENTATION_STATUS.md`

## Notes

- Always compile after each file to catch cascading errors
- The custom `FieldType` enum in `gc_types.rs` is separate from Wasmtime's `FieldType`
- Rooted<T> supposedly dereferences to T, test this thoroughly
- Some methods may have been renamed or moved to different traits

---

**Status**: Ready for implementation when GC API stabilizes or immediate need arises.
