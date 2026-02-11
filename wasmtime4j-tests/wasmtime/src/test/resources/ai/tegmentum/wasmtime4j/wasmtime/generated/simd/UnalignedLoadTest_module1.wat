(module
  (memory 1)
  ;; Initialize 8 bytes at offset 1 with value 1 in each i32 position
  (data (i32.const 1) "\01\00\00\00\01\00\00\00")

  (func (export "unaligned_load") (result v128)
    ;; Create v128 with values [0, 0, 1, 1]
    (v128.const i32x4 0 0 1 1)
    ;; Load v128 from memory at offset 1 (unaligned)
    (v128.load offset=1 (i32.const 0))
    ;; XOR the two vectors
    (v128.xor)
  )
)
