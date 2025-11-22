wit_bindgen::generate!({
    world: "importer",
});

struct Component;

impl Guest for Component {
    fn process(input: String) -> String {
        // Use the imported logger functions
        wasmtime4j::imports::logger::log(&format!("Processing: {}", input));
        wasmtime4j::imports::logger::log_level(1, "Debug level log");

        format!("Processed: {}", input)
    }
}

export!(Component);
