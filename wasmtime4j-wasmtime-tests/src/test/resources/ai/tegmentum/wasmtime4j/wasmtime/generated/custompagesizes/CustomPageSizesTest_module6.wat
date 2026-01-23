;; Custom page size 65536 - memory grow limits
(module
  (memory 0 (pagesize 65536))
  (func (export "size") (result i32)
    memory.size
  )
  (func (export "grow") (param i32) (result i32)
    (memory.grow (local.get 0))
  )
)
