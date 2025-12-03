/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.maven.wit;

import java.util.List;
import java.util.Objects;

/**
 * Base interface for WIT definitions (records, variants, enums, functions, etc.).
 *
 * @since 1.0.0
 */
public sealed interface WitDefinition {

    /**
     * Returns the name of this definition.
     *
     * @return the definition name
     */
    String name();

    /**
     * A field in a record type.
     *
     * @param name the field name
     * @param type the field type
     */
    record Field(String name, WitType type) {
        public Field {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }
    }

    /**
     * A case in a variant type.
     *
     * @param name    the case name
     * @param payload the optional payload type
     */
    record Case(String name, WitType payload) {
        public Case {
            Objects.requireNonNull(name, "name cannot be null");
            // payload may be null for unit cases
        }
    }

    /**
     * A parameter in a function.
     *
     * @param name the parameter name
     * @param type the parameter type
     */
    record Param(String name, WitType type) {
        public Param {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(type, "type cannot be null");
        }
    }

    /**
     * Record type definition.
     *
     * @param name   the record name
     * @param fields the record fields
     */
    record RecordDef(String name, List<Field> fields) implements WitDefinition {
        public RecordDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(fields, "fields cannot be null");
            fields = List.copyOf(fields);
        }
    }

    /**
     * Variant type definition.
     *
     * @param name  the variant name
     * @param cases the variant cases
     */
    record VariantDef(String name, List<Case> cases) implements WitDefinition {
        public VariantDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(cases, "cases cannot be null");
            cases = List.copyOf(cases);
        }
    }

    /**
     * Enum type definition.
     *
     * @param name   the enum name
     * @param values the enum values
     */
    record EnumDef(String name, List<String> values) implements WitDefinition {
        public EnumDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(values, "values cannot be null");
            values = List.copyOf(values);
        }
    }

    /**
     * Flags type definition (set of named boolean flags).
     *
     * @param name  the flags type name
     * @param flags the flag names
     */
    record FlagsDef(String name, List<String> flags) implements WitDefinition {
        public FlagsDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(flags, "flags cannot be null");
            flags = List.copyOf(flags);
        }
    }

    /**
     * Type alias definition.
     *
     * @param name       the alias name
     * @param targetType the target type
     */
    record TypeAlias(String name, WitType targetType) implements WitDefinition {
        public TypeAlias {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(targetType, "targetType cannot be null");
        }
    }

    /**
     * Resource type definition.
     *
     * @param name        the resource name
     * @param constructor the optional constructor function
     * @param methods     the resource methods
     */
    record ResourceDef(String name, FuncDef constructor, List<FuncDef> methods) implements WitDefinition {
        public ResourceDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(methods, "methods cannot be null");
            methods = List.copyOf(methods);
        }
    }

    /**
     * Function definition.
     *
     * @param name    the function name
     * @param params  the function parameters
     * @param results the function return types
     */
    record FuncDef(String name, List<Param> params, List<WitType> results) implements WitDefinition {
        public FuncDef {
            Objects.requireNonNull(name, "name cannot be null");
            Objects.requireNonNull(params, "params cannot be null");
            Objects.requireNonNull(results, "results cannot be null");
            params = List.copyOf(params);
            results = List.copyOf(results);
        }
    }
}
