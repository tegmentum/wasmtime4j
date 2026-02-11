;; Custom page size 1 byte - i64 load
(module
  (memory 8 8 (pagesize 0x1))
  (func (export "load64") (param i32) (result i64)
    local.get 0
    i64.load
  )
)
