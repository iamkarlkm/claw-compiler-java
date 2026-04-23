package com.q3lives.ir;

import com.q3lives.compiler.annotation.AnnotationResult;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.generators.IRGenerator;
import java.util.List;


/**
 * ClawIR - Claw编译器中间表示的完整封装
 * 
 * 包含：
 *   - IRProgram（IR指令序列）
 *   - StructureContext（结构信息，供后续代码生成参考）
 *   - SemanticContext（语义信息，供后续类型检查/优化参考）
 *   - AnnotationResult（注解信息，供后续钩子插入参考）
 */
public class ClawIR {

    private final IRGenerator.IRProgram irProgram;
    private final StructureContext structureContext;
    private final SemanticContext semanticContext;
    private final AnnotationResult annotationResult;

    public ClawIR(IRGenerator.IRProgram irProgram,
                  StructureContext structureContext,
                  SemanticContext semanticContext,
                  AnnotationResult annotationResult) {
        this.irProgram = irProgram;
        this.structureContext = structureContext;
        this.semanticContext = semanticContext;
        this.annotationResult = annotationResult;
    }

    public IRGenerator.IRProgram getIrProgram() { return irProgram; }
    public StructureContext getStructureContext() { return structureContext; }
    public SemanticContext getSemanticContext() { return semanticContext; }
    public AnnotationResult getAnnotationResult() { return annotationResult; }

    /**
     * 检查IR是否有效（无语义错误）
     */
    public boolean isValid() {
        return semanticContext != null && !semanticContext.hasErrors();
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
