package com.q3lives.ir;

import com.q3lives.compiler.annotation.AnnotationResult;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.generators.IRGenerator;
import com.q3lives.compiler.generators.ffi.FFIBindingTable;
import java.util.List;


/**
 * ClawIR - Claw编译器中间表示的完整封装
 *
 * 包含：
 *   - IRProgram（IR指令序列）
 *   - StructureContext（结构信息，供后续代码生成参考）
 *   - SemanticContext（语义信息，供后续类型检查/优化参考）
 *   - AnnotationResult（注解信息，供后续钩子插入参考）
 *   - FFIBindingTable（FFI外部函数接口绑定表，可选）
 */
public class ClawIR {

    private final IRGenerator.IRProgram irProgram;
    private final StructureContext structureContext;
    private final SemanticContext semanticContext;
    private final AnnotationResult annotationResult;

    /** FFI 绑定表，用于跨语言调用代码生成（可选） */
    private FFIBindingTable ffiBindingTable;

    public ClawIR(IRGenerator.IRProgram irProgram,
                  StructureContext structureContext,
                  SemanticContext semanticContext,
                  AnnotationResult annotationResult) {
        this.irProgram = irProgram;
        this.structureContext = structureContext;
        this.semanticContext = semanticContext;
        this.annotationResult = annotationResult;
    }

    /** 兼容旧代码的无参构造（测试用） */
    public ClawIR() {
        this.irProgram = new IRGenerator.IRProgram("test");
        this.structureContext = null;
        this.semanticContext = null;
        this.annotationResult = null;
    }

    public IRGenerator.IRProgram getIrProgram() { return irProgram; }
    public StructureContext getStructureContext() { return structureContext; }
    public SemanticContext getSemanticContext() { return semanticContext; }
    public AnnotationResult getAnnotationResult() { return annotationResult; }

    public FFIBindingTable getFfiBindingTable() { return ffiBindingTable; }
    public void setFfiBindingTable(FFIBindingTable ffiBindingTable) {
        this.ffiBindingTable = ffiBindingTable;
    }

    /** 检查是否包含 FFI 声明 */
    public boolean hasFFIBindings() {
        return ffiBindingTable != null && ffiBindingTable.hasExternDeclarations();
    }

    /**
     * 检查IR是否有效（无语义错误）
     */
    public boolean isValid() {
        // 如果没有语义上下文（如测试场景），只要有 IRProgram 即认为有效
        if (semanticContext == null) {
            return irProgram != null;
        }
        return !semanticContext.hasErrors();
    }

    /**
     * 获取IR摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ClawIR Summary ===");
        sb.append("Module: ").append(irProgram.getSourceFileName()).append(" ");
        sb.append("Top-level blocks: ").append(irProgram.getTopLevelBlocks().size()).append(" ");
        sb.append("Total instructions: ").append(irProgram.getAllInstructions().size()).append("");
        sb.append("Metadata entries: ").append(irProgram.getMetadata().size()).append(" ");
        sb.append("Valid: ").append(isValid()).append(" ");
        if (semanticContext != null) {
            sb.append("Types: ").append(semanticContext.getTypeRegistry().size()).append(" ");
            sb.append("Functions: ").append(semanticContext.getFunctionSignatures().size()).append(" ");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return irProgram.toString();
    }

    public String getModuleName() {
        return irProgram != null ? irProgram.getModuleName() : "Unknown";
    }

    public List<IRGenerator.IRBasicBlock> getNodes(){
       // 返回顶层块
       return irProgram != null ? irProgram.getTopLevelBlocks() : new java.util.ArrayList<>();
    }
}
