package com.q3lives.pipeline;

import com.q3lives.ir.ClawIR;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 编译结果
 */
public class CompilationResult {

    static CompilationResult failure(String moduleName, String message, List<String> errors, long elapsed) {
        CompilationResult result = new CompilationResult(moduleName);
        if (message != null && !message.isBlank()) {
            result.addError(message);
        }
        if (errors != null && !errors.isEmpty()) {
            result.addErrors(errors);
        }
        result.setPhaseComplete("failure");
        return result;
    }

    @SuppressWarnings("unchecked")
    static CompilationResult successBuilder(String moduleName, Object object, long elapsed) {
        CompilationResult result = new CompilationResult(moduleName);
        if (object instanceof Map<?, ?> map) {
            result.setGeneratedFiles((Map<String, String>) map);
        } else if (object instanceof ClawIR ir) {
            result.setIR(ir);
        }
        result.setPhaseComplete("success");
        return result;
    }
    private final String moduleName;
    private final List<String> errors;
    private final List<String> warnings;
    private final Set<String> completedPhases;
    private Map<String, String> generatedFiles;
    private ClawIR ir;
    
    public CompilationResult(String moduleName) {
        this.moduleName = moduleName;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.completedPhases = new LinkedHashSet<>();
    }
    
    public void setPhaseComplete(String phase) {
        completedPhases.add(phase);
    }
    
    public void addError(String error) { errors.add(error); }
    public void addErrors(List<String> errs) { errors.addAll(errs); }
    public void addWarnings(List<String> warns) { warnings.addAll(warns); }
    
    public void setGeneratedFiles(Map<String, String> files) { 
        this.generatedFiles = files; 
    }
    public void setIR(ClawIR ir) { this.ir = ir; }
    
    public boolean isSuccess() { return errors.isEmpty() && generatedFiles != null; }
    
    // Getters
    public String getModuleName() { return moduleName; }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public Set<String> getCompletedPhases() { return Collections.unmodifiableSet(completedPhases); }
    public Map<String, String> getGeneratedFiles() { return generatedFiles; }
    public ClawIR getIR() { return ir; }
}
