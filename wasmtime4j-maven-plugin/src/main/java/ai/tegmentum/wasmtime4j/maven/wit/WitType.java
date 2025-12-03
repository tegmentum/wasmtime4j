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
import java.util.Optional;

/**
 * Represents a WIT type in the parsed AST.
 *
 * @since 1.0.0
 */
public sealed interface WitType {

    /**
     * Returns the Java type name for this WIT type.
     *
     * @return the Java type name
     */
    String toJavaType();

    /**
     * Primitive boolean type.
     */
    record Bool() implements WitType {
        @Override
        public String toJavaType() {
            return "boolean";
        }
    }

    /**
     * Primitive 8-bit signed integer.
     */
    record S8() implements WitType {
        @Override
        public String toJavaType() {
            return "byte";
        }
    }

    /**
     * Primitive 8-bit unsigned integer.
     */
    record U8() implements WitType {
        @Override
        public String toJavaType() {
            return "short"; // Java has no unsigned byte
        }
    }

    /**
     * Primitive 16-bit signed integer.
     */
    record S16() implements WitType {
        @Override
        public String toJavaType() {
            return "short";
        }
    }

    /**
     * Primitive 16-bit unsigned integer.
     */
    record U16() implements WitType {
        @Override
        public String toJavaType() {
            return "int"; // Java has no unsigned short
        }
    }

    /**
     * Primitive 32-bit signed integer.
     */
    record S32() implements WitType {
        @Override
        public String toJavaType() {
            return "int";
        }
    }

    /**
     * Primitive 32-bit unsigned integer.
     */
    record U32() implements WitType {
        @Override
        public String toJavaType() {
            return "long"; // Java has no unsigned int
        }
    }

    /**
     * Primitive 64-bit signed integer.
     */
    record S64() implements WitType {
        @Override
        public String toJavaType() {
            return "long";
        }
    }

    /**
     * Primitive 64-bit unsigned integer.
     */
    record U64() implements WitType {
        @Override
        public String toJavaType() {
            return "java.math.BigInteger"; // Java has no unsigned long
        }
    }

    /**
     * Primitive 32-bit floating point.
     */
    record F32() implements WitType {
        @Override
        public String toJavaType() {
            return "float";
        }
    }

    /**
     * Primitive 64-bit floating point.
     */
    record F64() implements WitType {
        @Override
        public String toJavaType() {
            return "double";
        }
    }

    /**
     * Unicode character.
     */
    record Char() implements WitType {
        @Override
        public String toJavaType() {
            return "int"; // Unicode code point
        }
    }

    /**
     * Unicode string.
     */
    record WitString() implements WitType {
        @Override
        public String toJavaType() {
            return "String";
        }
    }

    /**
     * List type.
     *
     * @param elementType the element type
     */
    record WitList(WitType elementType) implements WitType {
        public WitList {
            Objects.requireNonNull(elementType, "elementType cannot be null");
        }

        @Override
        public String toJavaType() {
            return "java.util.List<" + boxType(elementType.toJavaType()) + ">";
        }
    }

    /**
     * Option type (nullable).
     *
     * @param valueType the value type
     */
    record WitOption(WitType valueType) implements WitType {
        public WitOption {
            Objects.requireNonNull(valueType, "valueType cannot be null");
        }

        @Override
        public String toJavaType() {
            return "java.util.Optional<" + boxType(valueType.toJavaType()) + ">";
        }
    }

    /**
     * Result type with ok and error variants.
     *
     * @param okType  the success type (may be null for unit)
     * @param errType the error type (may be null for unit)
     */
    record WitResult(WitType okType, WitType errType) implements WitType {
        @Override
        public String toJavaType() {
            final String ok = okType != null ? boxType(okType.toJavaType()) : "Void";
            final String err = errType != null ? boxType(errType.toJavaType()) : "Void";
            return "ai.tegmentum.wasmtime4j.component.Result<" + ok + ", " + err + ">";
        }
    }

    /**
     * Tuple type.
     *
     * @param types the tuple element types
     */
    record WitTuple(List<WitType> types) implements WitType {
        public WitTuple {
            Objects.requireNonNull(types, "types cannot be null");
            types = List.copyOf(types);
        }

        @Override
        public String toJavaType() {
            if (types.size() == 2) {
                return "ai.tegmentum.wasmtime4j.component.Tuple2<"
                    + boxType(types.get(0).toJavaType()) + ", "
                    + boxType(types.get(1).toJavaType()) + ">";
            }
            // For larger tuples, generate record
            return "Object[]";
        }
    }

    /**
     * Named type reference.
     *
     * @param name the type name
     */
    record TypeRef(String name) implements WitType {
        public TypeRef {
            Objects.requireNonNull(name, "name cannot be null");
        }

        @Override
        public String toJavaType() {
            return toPascalCase(name);
        }
    }

    /**
     * Convert primitive type to boxed type.
     *
     * @param type the type name
     * @return boxed type if primitive, otherwise unchanged
     */
    private static String boxType(final String type) {
        return switch (type) {
            case "boolean" -> "Boolean";
            case "byte" -> "Byte";
            case "short" -> "Short";
            case "int" -> "Integer";
            case "long" -> "Long";
            case "float" -> "Float";
            case "double" -> "Double";
            case "char" -> "Character";
            default -> type;
        };
    }

    /**
     * Convert kebab-case to PascalCase.
     *
     * @param name the name in kebab-case
     * @return the name in PascalCase
     */
    static String toPascalCase(final String name) {
        final StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Convert kebab-case to camelCase.
     *
     * @param name the name in kebab-case
     * @return the name in camelCase
     */
    static String toCamelCase(final String name) {
        final String pascal = toPascalCase(name);
        if (pascal.isEmpty()) {
            return pascal;
        }
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }
}
