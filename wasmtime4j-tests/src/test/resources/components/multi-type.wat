;; Component with multiple parameter types: process(bool, s64, float64) -> s64
(component
  (core module $m
    ;; Function type: (i32, i64, f64) -> i64
    ;; bool is represented as i32, s64 as i64, float64 as f64
    (type (func (param i32 i64 f64) (result i64)))
    (export "process" (func 0))
    (func (type 0) (param i32 i64 f64) (result i64)
      ;; If bool is true (non-zero), return s64 + truncated float64
      ;; Otherwise return just s64
      local.get 0              ;; get bool
      if (result i64)
        local.get 1            ;; get s64
        local.get 2            ;; get float64
        i64.trunc_f64_s        ;; convert float64 to i64
        i64.add                ;; add them
      else
        local.get 1            ;; just return s64
      end
    )
  )
  (core instance $i (instantiate $m))
  (type (func (param "add" bool) (param "base" s64) (param "delta" float64) (result s64)))
  (alias core export $i "process" (core func))
  (func (type 0) (canon lift (core func 0)))
  (export "process" (func 0))
)
