package ai.tegmentum.wasmtime4j;

/**
 * Represents the type information of a WebAssembly import.
 * 
 * <p>This class provides metadata about an import requirement, including
 * its module name, field name, and expected type.
 * 
 * @since 1.0.0
 */
public final class ImportType {
    
    private final String moduleName;
    private final String name;
    private final WasmType type;
    
    /**
     * Creates a new import type.
     * 
     * @param moduleName the module name of the import
     * @param name the name of the import
     * @param type the expected type of the import
     */
    public ImportType(final String moduleName, final String name, final WasmType type) {
        this.moduleName = moduleName;
        this.name = name;
        this.type = type;
    }
    
    /**
     * Gets the module name of the import.
     * 
     * @return the module name
     */
    public String getModuleName() {
        return moduleName;
    }
    
    /**
     * Gets the name of the import.
     * 
     * @return the import name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the expected type of the import.
     * 
     * @return the import type
     */
    public WasmType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return String.format("ImportType{module='%s', name='%s', type=%s}", moduleName, name, type);
    }
}