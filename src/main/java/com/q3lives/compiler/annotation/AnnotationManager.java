// ==================== AnnotationManager.java ====================
package com.q3lives.compiler.annotation;

import com.q3lives.compiler.annotation.ProgramAnnotations.AnnotationType;
import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.context.StructureContext;
import com.q3lives.compiler.frontend.ASTNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 注解管理器 - 统一管理程序注解和系统注解
 */
@Getter
@Slf4j
public class AnnotationManager {


    private final ProgramAnnotations programAnnotations;
    private final SystemAnnotations systemAnnotations;
    private final DescriptionConverter descriptionConverter;

    public AnnotationManager() {
        this.programAnnotations = new ProgramAnnotations();
        this.systemAnnotations = new SystemAnnotations();
        this.descriptionConverter = new DescriptionConverter();
    }

    /**
     * 处理AST中的所有注解
     */
    public void processAnnotations(ASTNode ast) {
        log.info("开始处理注解系统");
        programAnnotations.process(ast);
        systemAnnotations.process(ast);

        // 转换 @@description 为机器可读格式
        java.util.Optional<SystemAnnotations.SystemAnnotation> description = systemAnnotations.getDescription();
        if (description.isPresent()) {
            SystemAnnotations.SystemAnnotation desc = description.get();
            Object argsObject = desc.getArguments();
            if (argsObject instanceof java.util.List<?> descriptionArgs && descriptionArgs.size() >= 2) {
                String humanDesc = (String) descriptionArgs.get(0);
                String ioSpec = (String) descriptionArgs.get(1);
                String machineFormat = descriptionConverter.convert(humanDesc, ioSpec);
                log.info("@@description 机器可读格式: {}", machineFormat);
            }
        }

        log.info("注解处理完成: {} 个程序注解, {} 个系统注解",
                programAnnotations.getAnnotations().size(),
                systemAnnotations.getAnnotations().size());
    }

    public AnnotationType process(StructureContext structureCtx, SemanticContext semanticCtx) {
        log.debug("AnnotationManager.process is not yet implemented for StructureContext/SemanticContext.");
        return null;
    }

    public ProgramAnnotations getProgramAnnotations() {
        return programAnnotations;
    }
}

