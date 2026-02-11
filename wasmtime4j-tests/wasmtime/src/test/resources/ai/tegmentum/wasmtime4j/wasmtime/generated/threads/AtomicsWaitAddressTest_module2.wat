(module
  (type (;0;) (func))
  (func $main (type 0)
    i32.const -64
    i32.const -63
    memory.atomic.notify offset=65536
    unreachable)
  (memory (;0;) 4 4)
  (export "main" (func $main))
)
