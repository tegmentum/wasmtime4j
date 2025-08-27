package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly table.
 * 
 * <p>Tables are resizable arrays of references (like function references)
 * that can be accessed by WebAssembly code. They provide a way to implement
 * indirect function calls and other dynamic behavior.
 * 
 * @since 1.0.0
 */
public interface WasmTable {
    
    /**
     * Gets the current size of the table.
     * 
     * @return the current number of elements in the table
     */
    int getSize();
    
    /**
     * Grows the table by the specified number of elements.
     * 
     * @param elements the number of elements to add
     * @param initValue the initial value for new elements
     * @return the previous size, or -1 if growth failed
     */
    int grow(final int elements, final Object initValue);
    
    /**
     * Gets the maximum size of the table.
     * 
     * @return the maximum number of elements, or -1 if unlimited
     */
    int getMaxSize();
    
    /**
     * Gets an element from the table at the given index.
     * 
     * @param index the index of the element
     * @return the element at the index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    Object get(final int index);
    
    /**
     * Sets an element in the table at the given index.
     * 
     * @param index the index of the element
     * @param value the value to set
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    void set(final int index, final Object value);
    
    /**
     * Gets the element type of this table.
     * 
     * @return the element type
     */
    WasmValueType getElementType();
}