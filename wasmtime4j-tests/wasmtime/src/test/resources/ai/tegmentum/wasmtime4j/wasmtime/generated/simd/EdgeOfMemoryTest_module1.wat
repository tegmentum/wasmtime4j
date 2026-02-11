(module
  (memory 1)

  ;; Test 8-bit lane operations 1 byte from end of memory
  (func (export "1-byte-from-end") (param i32)
    ;; v128.load8_splat - loads single byte and replicates
    (drop (v128.load8_splat (local.get 0)))
    ;; v128.store8_lane - stores single byte from lane
    (v128.store8_lane 0 (local.get 0) (v128.const i8x16 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0))
    ;; i8x16.replace_lane and extract - modify and retrieve lane
    (drop (i8x16.extract_lane_s 0
      (i8x16.replace_lane 0 (v128.const i8x16 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0) (i32.const 0))))
  )

  ;; Test 16-bit lane operations 2 bytes from end of memory
  (func (export "2-byte-from-end") (param i32)
    ;; v128.load16_splat - loads 2 bytes and replicates
    (drop (v128.load16_splat (local.get 0)))
    ;; v128.store16_lane - stores 2 bytes from lane
    (v128.store16_lane 0 (local.get 0) (v128.const i16x8 0 0 0 0 0 0 0 0))
    ;; i16x8.replace_lane and extract
    (drop (i16x8.extract_lane_s 0
      (i16x8.replace_lane 0 (v128.const i16x8 0 0 0 0 0 0 0 0) (i32.const 0))))
  )

  ;; Test 32-bit lane operations 4 bytes from end of memory
  (func (export "4-byte-from-end") (param i32)
    ;; v128.load32_splat - loads 4 bytes and replicates
    (drop (v128.load32_splat (local.get 0)))
    ;; v128.store32_lane - stores 4 bytes from lane
    (v128.store32_lane 0 (local.get 0) (v128.const i32x4 0 0 0 0))
    ;; i32x4.replace_lane and extract
    (drop (i32x4.extract_lane 0
      (i32x4.replace_lane 0 (v128.const i32x4 0 0 0 0) (i32.const 0))))
    ;; f32x4.replace_lane and extract
    (drop (f32x4.extract_lane 0
      (f32x4.replace_lane 0 (v128.const f32x4 0 0 0 0) (f32.const 0))))
  )

  ;; Test 64-bit lane operations 8 bytes from end of memory
  (func (export "8-byte-from-end") (param i32)
    ;; v128.load64_splat - loads 8 bytes and replicates
    (drop (v128.load64_splat (local.get 0)))
    ;; v128.store64_lane - stores 8 bytes from lane
    (v128.store64_lane 0 (local.get 0) (v128.const i64x2 0 0))
    ;; i64x2.replace_lane and extract
    (drop (i64x2.extract_lane 0
      (i64x2.replace_lane 0 (v128.const i64x2 0 0) (i64.const 0))))
    ;; f64x2.replace_lane and extract
    (drop (f64x2.extract_lane 0
      (f64x2.replace_lane 0 (v128.const f64x2 0 0) (f64.const 0))))
  )
)
