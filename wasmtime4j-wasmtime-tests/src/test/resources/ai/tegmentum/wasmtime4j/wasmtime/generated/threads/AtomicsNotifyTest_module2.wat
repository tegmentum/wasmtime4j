(module
  (memory 1 1 shared)
  (func (export "notify_shared") (result i32) (memory.atomic.notify (i32.const 0) (i32.const -1)))
)
