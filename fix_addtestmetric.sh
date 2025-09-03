#!/bin/bash

# Function to fix malformed addTestMetric comments in a Java file
fix_file() {
    local file="$1"
    echo "Processing: $file"
    
    # Create a backup
    cp "$file" "$file.backup"
    
    # Use sed to fix malformed addTestMetric comments
    # This regex finds lines that start with '// addTestMetric(' and then fixes the following lines
    awk '
    BEGIN { in_addtestmetric = 0; indent = ""; }
    
    # Detect start of malformed addTestMetric comment
    /^[[:space:]]*\/\/ addTestMetric\(/ {
        in_addtestmetric = 1
        # Extract the indentation from the comment line
        match($0, /^[[:space:]]*/)
        indent = substr($0, RSTART, RLENGTH)
        print $0
        next
    }
    
    # If we are in addTestMetric and line is not already commented
    in_addtestmetric && !/^[[:space:]]*\/\// {
        # Check if this line closes the addTestMetric call
        if (/\);[[:space:]]*$/) {
            # This is the closing line, comment it out and end the block
            gsub(/^[[:space:]]*/, indent "//     ")
            print $0
            in_addtestmetric = 0
            next
        } else {
            # This is a continuation line, comment it out
            gsub(/^[[:space:]]*/, indent "//     ")
            print $0
            next
        }
    }
    
    # If already commented or not in addTestMetric, print as-is
    { 
        if (in_addtestmetric && /^[[:space:]]*\/\//) {
            # Already commented line in addTestMetric block
            if (/\);[[:space:]]*$/) {
                in_addtestmetric = 0
            }
        }
        print $0 
    }
    ' "$file" > "$file.tmp"
    
    mv "$file.tmp" "$file"
    echo "Fixed: $file"
}

# Find all Java files with malformed addTestMetric comments and fix them
find wasmtime4j-tests -name "*.java" -exec grep -l "// addTestMetric(" {} \; | while read file; do
    fix_file "$file"
done

echo "All files processed"
