wit_bindgen::generate!({
    path: "wit",
    world: "basic-types",
});

struct Component;

impl Guest for Component {
    fn test_bool(input: bool) -> bool {
        // Return the negation
        !input
    }

    fn test_s32(input: i32) -> i32 {
        // Return input + 1
        input.wrapping_add(1)
    }

    fn test_s64(input: i64) -> i64 {
        // Return input + 1
        input.wrapping_add(1)
    }

    fn test_float64(input: f64) -> f64 {
        // Return input * 2.0
        input * 2.0
    }

    fn test_char(input: char) -> char {
        // Return the next character
        char::from_u32(input as u32 + 1).unwrap_or(input)
    }

    fn test_string(input: String) -> String {
        // Return "Hello, " + input
        format!("Hello, {}", input)
    }

    fn test_multi_params(a: i32, b: i32) -> i32 {
        // Return a + b
        a.wrapping_add(b)
    }

    fn test_multi_returns(input: i32) -> (i32, i32) {
        // Return (input, input * 2)
        (input, input.wrapping_mul(2))
    }
}

export!(Component);
