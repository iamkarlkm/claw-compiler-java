// ==================== GeneratedCode.java ====================
package com.claw.compiler.pipeline;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

/**
 * 生成的代码
 */
@Getter
@Builder
public class GeneratedCode {

    /** 目标代码（主输出） */
    private final String targetCode;

    /** 中间表示 */
    private final String intermediateRepresentation;

    /** 伪代码 */
    private final String pseudoCode;

    /** 元数据（来自系统注解） */
    private final Map<String, String> metadata;

    /** 源文件名 */
    private final String sourceFileName;

    /** 生成的函数数量 */
    private final int functionCount;

    /** 生成的类型数量 */
    private final int typeCount;

    public GeneratedCode(String targetCode, String intermediateRepresentation, String pseudoCode, Map<String, String> metadata, String sourceFileName, int functionCount, int typeCount) {
        this.targetCode = targetCode;
        this.intermediateRepresentation = intermediateRepresentation;
        this.pseudoCode = pseudoCode;
        this.metadata = metadata;
        this.sourceFileName = sourceFileName;
        this.functionCount = functionCount;
        this.typeCount = typeCount;
    }
    
    

    @Override
    public String toString() {
        return String.format("GeneratedCode{file=%s, functions=%d, types=%d, codeLength=%d}",
                sourceFileName, functionCount, typeCount,
                targetCode != null ? targetCode.length() : 0);
    }
}
