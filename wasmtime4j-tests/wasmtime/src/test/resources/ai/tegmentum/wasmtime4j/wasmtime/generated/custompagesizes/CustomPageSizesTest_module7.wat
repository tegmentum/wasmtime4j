;; Multi-memory with different page sizes - copy operations
(module
  (memory $small 10 (pagesize 1))
  (memory $large 1 (pagesize 65536))

  (data (memory $small) (i32.const 0) "\11\22\33\44")
  (data (memory $large) (i32.const 0) "\55\66\77\88")

  (func (export "copy-small-to-large") (param i32 i32 i32)
    (memory.copy $large $small (local.get 0) (local.get 1) (local.get 2))
  )

  (func (export "copy-large-to-small") (param i32 i32 i32)
    (memory.copy $small $large (local.get 0) (local.get 1) (local.get 2))
  )

  (func (export "load8-small") (param i32) (result i32)
    (i32.load8_u $small (local.get 0))
  )

  (func (export "load8-large") (param i32) (result i32)
    (i32.load8_u $large (local.get 0))
  )
)
