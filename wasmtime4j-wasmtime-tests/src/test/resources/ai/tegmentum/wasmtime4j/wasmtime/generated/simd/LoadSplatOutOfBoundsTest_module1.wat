(module
  ;; Memory with 0-6 pages (starts with 0 pages)
  (memory 0 6)

  (func (export "x") (result i32)
    ;; Aligned load at offset 0 - should trap with no memory
    (v128.load32_splat (i32.const 0))
    (v128.any_true)
  )
)
