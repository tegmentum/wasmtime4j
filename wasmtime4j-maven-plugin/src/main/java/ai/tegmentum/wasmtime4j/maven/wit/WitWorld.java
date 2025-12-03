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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a parsed WIT world definition.
 *
 * <p>A world defines the imports and exports of a WebAssembly component.
 *
 * @since 1.0.0
 */
public final class WitWorld {

    private final String packageName;
    private final String name;
    private final List<WitDefinition> types;
    private final List<WitDefinition.FuncDef> imports;
    private final List<WitDefinition.FuncDef> exports;
    private final List<String> importedInterfaces;
    private final List<String> exportedInterfaces;

    private WitWorld(final Builder builder) {
        this.packageName = builder.packageName;
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.types = Collections.unmodifiableList(new ArrayList<>(builder.types));
        this.imports = Collections.unmodifiableList(new ArrayList<>(builder.imports));
        this.exports = Collections.unmodifiableList(new ArrayList<>(builder.exports));
        this.importedInterfaces = Collections.unmodifiableList(new ArrayList<>(builder.importedInterfaces));
        this.exportedInterfaces = Collections.unmodifiableList(new ArrayList<>(builder.exportedInterfaces));
    }

    /**
     * Returns the package name (e.g., "wasi:cli@1.0.0").
     *
     * @return the package name, or null if not specified
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Returns the world name.
     *
     * @return the world name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type definitions in this world.
     *
     * @return the type definitions
     */
    public List<WitDefinition> getTypes() {
        return types;
    }

    /**
     * Returns the imported functions.
     *
     * @return the imported functions
     */
    public List<WitDefinition.FuncDef> getImports() {
        return imports;
    }

    /**
     * Returns the exported functions.
     *
     * @return the exported functions
     */
    public List<WitDefinition.FuncDef> getExports() {
        return exports;
    }

    /**
     * Returns the names of imported interfaces.
     *
     * @return the imported interface names
     */
    public List<String> getImportedInterfaces() {
        return importedInterfaces;
    }

    /**
     * Returns the names of exported interfaces.
     *
     * @return the exported interface names
     */
    public List<String> getExportedInterfaces() {
        return exportedInterfaces;
    }

    /**
     * Creates a new builder for WitWorld.
     *
     * @param name the world name
     * @return a new builder
     */
    public static Builder builder(final String name) {
        return new Builder(name);
    }

    /**
     * Builder for WitWorld.
     */
    public static final class Builder {
        private String packageName;
        private final String name;
        private final List<WitDefinition> types = new ArrayList<>();
        private final List<WitDefinition.FuncDef> imports = new ArrayList<>();
        private final List<WitDefinition.FuncDef> exports = new ArrayList<>();
        private final List<String> importedInterfaces = new ArrayList<>();
        private final List<String> exportedInterfaces = new ArrayList<>();

        private Builder(final String name) {
            this.name = name;
        }

        /**
         * Sets the package name.
         *
         * @param packageName the package name
         * @return this builder
         */
        public Builder packageName(final String packageName) {
            this.packageName = packageName;
            return this;
        }

        /**
         * Adds a type definition.
         *
         * @param type the type definition
         * @return this builder
         */
        public Builder addType(final WitDefinition type) {
            this.types.add(Objects.requireNonNull(type, "type cannot be null"));
            return this;
        }

        /**
         * Adds an imported function.
         *
         * @param func the function definition
         * @return this builder
         */
        public Builder addImport(final WitDefinition.FuncDef func) {
            this.imports.add(Objects.requireNonNull(func, "func cannot be null"));
            return this;
        }

        /**
         * Adds an exported function.
         *
         * @param func the function definition
         * @return this builder
         */
        public Builder addExport(final WitDefinition.FuncDef func) {
            this.exports.add(Objects.requireNonNull(func, "func cannot be null"));
            return this;
        }

        /**
         * Adds an imported interface reference.
         *
         * @param interfaceName the interface name
         * @return this builder
         */
        public Builder addImportedInterface(final String interfaceName) {
            this.importedInterfaces.add(Objects.requireNonNull(interfaceName, "interfaceName cannot be null"));
            return this;
        }

        /**
         * Adds an exported interface reference.
         *
         * @param interfaceName the interface name
         * @return this builder
         */
        public Builder addExportedInterface(final String interfaceName) {
            this.exportedInterfaces.add(Objects.requireNonNull(interfaceName, "interfaceName cannot be null"));
            return this;
        }

        /**
         * Builds the WitWorld.
         *
         * @return the built WitWorld
         */
        public WitWorld build() {
            return new WitWorld(this);
        }
    }

    @Override
    public String toString() {
        return "WitWorld{"
            + "packageName='" + packageName + '\''
            + ", name='" + name + '\''
            + ", types=" + types.size()
            + ", imports=" + imports.size()
            + ", exports=" + exports.size()
            + ", importedInterfaces=" + importedInterfaces
            + ", exportedInterfaces=" + exportedInterfaces
            + '}';
    }
}
