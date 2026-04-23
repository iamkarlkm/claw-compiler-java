package com.q3lives.binding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 代码生成结果
 */
public class GenerationResult {
    private final Map<String, String> files; // 文件名 -> 内容
    private final List<String> warnings;
    private final List<String> errors;
    
    public GenerationResult() {
        this.files = new LinkedHashMap<>();
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
    }
    
    public void addFile(String fileName, String content) {
        files.put(fileName, content);
    }
    
    public void addWarning(String warning) {
        warnings.add(warning);
    }
    
    public void addError(String error) {
        errors.add(error);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    // Getters
    public Map<String, String> getFiles() { return Collections.unmodifiableMap(files); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
}
