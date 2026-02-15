;; Simple WAST test file for testing WAST execution
(module
  (func (export "add") (param i32 i32) (result i32)
    local.get 0
    local.get 1
    i32.add
  )
)

;; Test that add function works correctly
(assert_return (invoke "add" (i32.const 1) (i32.const 2)) (i32.const 3))
(assert_return (invoke "add" (i32.const 10) (i32.const 20)) (i32.const 30))
(assert_return (invoke "add" (i32.const 0) (i32.const 0)) (i32.const 0))
