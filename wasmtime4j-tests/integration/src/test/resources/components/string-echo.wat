;; Component with string function: echo(string) -> string
(component
  (core module $m
    ;; Memory for string operations
    (memory (export "memory") 1)

    ;; Simple bump allocator - next available byte
    (global $next (mut i32) (i32.const 0))

    ;; realloc function required by component model for string allocation
    ;; realloc(old_ptr, old_size, align, new_size) -> new_ptr
    (func (export "cabi_realloc") (param i32 i32 i32 i32) (result i32)
      (local $ptr i32)
      ;; Get current position
      global.get $next
      local.set $ptr

      ;; Advance by new_size (ignore alignment for simplicity)
      global.get $next
      local.get 3
      i32.add
      global.set $next

      ;; Return allocated pointer
      local.get $ptr
    )

    ;; echo function: takes (ptr, len) and returns (ptr, len)
    ;; In component model, strings are passed as (pointer, length) pairs
    ;; The result is stored in memory at offset 0
    (func (export "echo") (param i32 i32) (result i32)
      ;; Copy input string to a new location using realloc
      ;; For simplicity, we just return the same pointer
      ;; Store result at memory location 0: [ptr, len]
      (i32.store (i32.const 0) (local.get 0))
      (i32.store (i32.const 4) (local.get 1))
      ;; Return pointer to result struct
      (i32.const 0)
    )
  )

  (core instance $i (instantiate $m))

  ;; Type for string->string function
  (type (func (param "input" string) (result string)))

  ;; Alias the memory and realloc for canon lift
  (alias core export $i "memory" (core memory))
  (alias core export $i "cabi_realloc" (core func $realloc))
  (alias core export $i "echo" (core func $echo))

  ;; Lift the core function to component function
  (func (type 0) (canon lift (core func $echo) (memory 0) (realloc (func $realloc))))

  (export "echo" (func 0))
)
