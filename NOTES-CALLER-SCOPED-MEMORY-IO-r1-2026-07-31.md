# F-Wasmtime4j-Caller-Scoped-Memory-IO r.1 — implement

Charter at fiji: doctrine/specs/f-wasmtime4j-caller-scoped-memory-io-charter-2026-07-31.md

Adds `Caller.readMemory(String, long, int)` + `Caller.writeMemory(String, long, byte[])` scoped ops
so host callbacks can read/write caller's exported memory safely (routes through
`caller.get_export(name).into_memory()` + `Memory::read/write(&mut ctx, ...)`).

r.1 scope:
- Caller interface: 2 default UOE methods
- JniCaller: 2 impls + native declarations
- caller.rs: 2 new #[no_mangle] fns
- JniCallerScopedMutationTest: round-trip write/read from callback
