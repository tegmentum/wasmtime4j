(module
  (memory 1 1)
  (func (export "notify") (result i32) (memory.atomic.notify (i32.const 0) (i32.const -1)))
)
