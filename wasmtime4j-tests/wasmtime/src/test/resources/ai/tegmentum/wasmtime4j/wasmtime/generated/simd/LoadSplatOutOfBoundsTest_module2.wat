(module
  ;; Memory with 1-6 pages (starts with 1 page = 64KB)
  (memory 1 6)

  (func (export "x") (result i32)
    ;; Unaligned load at offset 1 - should succeed with memory available
    (v128.load32_splat offset=1 (i32.const 0))
    (v128.any_true)
  )
)
