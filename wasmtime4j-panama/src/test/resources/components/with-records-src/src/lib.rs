wit_bindgen::generate!({
    path: "wit",
    world: "with-records",
});

struct Component;

impl Guest for Component {
    fn echo_person(p: Person) -> Person {
        p
    }

    fn echo_point(p: Point) -> Point {
        p
    }

    fn create_person(name: String, age: u32, email: String) -> Person {
        Person { name, age, email }
    }
}

export!(Component);
