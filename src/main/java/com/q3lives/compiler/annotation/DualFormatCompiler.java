// ==================== DualFormatCompiler.java ====================
package com.q3lives.compiler.annotation;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * 双格式编译器 - 同时生成人类可读和机器可读的注解格式
 */
@Slf4j
public class DualFormatCompiler {

    private static final Logger log = LoggerFactory.getLogger(ProgramAnnotations.class);

    private final DescriptionConverter converter = new DescriptionConverter();

    /**
     * 编译注解为双格式
     */
    public DualFormat compile(SystemAnnotations.SystemAnnotation annotation) {
        String humanReadable = formatHumanReadable(annotation);
        String machineReadable = formatMachineReadable(annotation);

        return new DualFormat(humanReadable, machineReadable);
    }

    private String formatHumanReadable(SystemAnnotations.SystemAnnotation annotation) {
        StringBuilder sb = new StringBuilder();
        sb.append("@@").append(annotation.getType().name().toLowerCase());
        sb.append("(");
        for (int i = 0; i < annotation.getArguments().size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(annotation.getArguments().get(i)).append("\"");
        }
        sb.append(")");
        return sb.toString();
    }

    private String formatMachineReadable(SystemAnnotations.SystemAnnotation annotation) {
        if (annotation.getType() == SystemAnnotations.SystemAnnotationType.DESCRIPTION &&
            annotation.getArguments().size() >= 2) {
            return converter.convert(annotation.getArguments().get(0), annotation.getArguments().get(1));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", annotation.getType().name());
        map.put("args", annotation.getArguments());
        StringBuilder sb = new StringBuilder("{");
        map.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (sb.length() > 1) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    public record DualFormat(String humanReadable, String machineReadable) {}
}
