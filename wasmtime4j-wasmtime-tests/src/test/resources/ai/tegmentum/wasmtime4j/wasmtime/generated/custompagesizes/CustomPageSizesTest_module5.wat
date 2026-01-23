;; Custom page size 1 byte - memory operations
(module
  (memory 0 (pagesize 1))
  (func (export "size") (result i32)
    memory.size
  )
  (func (export "grow") (param i32) (result i32)
    (memory.grow (local.get 0))
  )
  (func (export "load") (param i32) (result i32)
    (i32.load8_u (local.get 0))
  )
  (func (export "store") (param i32 i32)
    (i32.store8 (local.get 0) (local.get 1))
  )
)
