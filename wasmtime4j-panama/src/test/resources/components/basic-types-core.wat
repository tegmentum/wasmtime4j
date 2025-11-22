;; Core WebAssembly module implementing basic type test functions
(module
  ;; Memory for string handling
  (memory (export "memory") 1)

  ;; test-bool: func(bool) -> bool
  ;; Simply returns the input
  (func (export "test-bool") (param i32) (result i32)
    local.get 0
  )

  ;; test-s32: func(s32) -> s32
  ;; Returns input + 1
  (func (export "test-s32") (param i32) (result i32)
    local.get 0
    i32.const 1
    i32.add
  )

  ;; test-s64: func(s64) -> s64
  ;; Returns input + 1
  (func (export "test-s64") (param i64) (result i64)
    local.get 0
    i64.const 1
    i64.add
  )

  ;; test-float64: func(float64) -> float64
  ;; Returns input * 2.0
  (func (export "test-float64") (param f64) (result f64)
    local.get 0
    f64.const 2.0
    f64.mul
  )

  ;; test-char: func(char) -> char
  ;; Returns the next character (input + 1)
  (func (export "test-char") (param i32) (result i32)
    local.get 0
    i32.const 1
    i32.add
  )

  ;; test-string: func(string) -> string
  ;; For simplicity, just return a pointer to a fixed string
  ;; In a real implementation, this would be more complex
  (func (export "test-string") (param i32 i32) (result i32 i32)
    ;; Store test string at memory offset 0
    i32.const 0  ;; offset
    i32.const 5  ;; length
  )

  ;; test-multi-params: func(s32, s32) -> s32
  ;; Returns a + b
  (func (export "test-multi-params") (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add
  )

  ;; test-multi-returns: func(s32) -> (s32, s32)
  ;; Returns (input, input * 2)
  (func (export "test-multi-returns") (param i32) (result i32 i32)
    local.get 0
    local.get 0
    i32.const 2
    i32.mul
  )

  ;; Initialize memory with test string
  (data (i32.const 0) "Hello")
)
