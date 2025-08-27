package ai.tegmentum.wasmtime4j;

/**
 * Represents the type information of a WebAssembly export.
 * 
 * <p>This class provides metadata about an export, including its name and type.
 * 
 * @since 1.0.0
 */
public final class ExportType {
    
    private final String name;
    private final WasmType type;
    
    /**
     * Creates a new export type.
     * 
     * @param name the name of the export
     * @param type the type of the export
     */
    public ExportType(final String name, final WasmType type) {
        this.name = name;
        this.type = type;
    }
    
    /**
     * Gets the name of the export.
     * 
     * @return the export name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the type of the export.
     * 
     * @return the export type
     */
    public WasmType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return String.format("ExportType{name='%s', type=%s}", name, type);
    }
}