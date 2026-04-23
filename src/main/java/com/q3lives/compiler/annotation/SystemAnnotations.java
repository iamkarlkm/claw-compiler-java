// ==================== SystemAnnotations.java ====================
package com.q3lives.compiler.annotation;

import com.q3lives.compiler.frontend.ASTNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

/**
 * 5个系统注解处理器
 * 
 * @@description / @@param / @@return / @@example / @@deprecated
 */
@Slf4j
public class SystemAnnotations {


    private final List<SystemAnnotation> annotations = new ArrayList<>();

    public void process(ASTNode ast) {
        processNode(ast);
        log.info("处理了 {} 个系统注解", annotations.size());
    }

    private void processNode(ASTNode node) {
        if (node.getType() == ASTNode.NodeType.ANNOTATION) {
            String category = node.getAttribute("category");
            if ("system".equals(category)) {
                String name = node.getAttribute("name");
                String args = node.getAttribute("arguments");
                SystemAnnotation annotation = parseAnnotation(name, args, node.getLine());
                if (annotation != null) {
                    annotations.add(annotation);
                    log.debug("系统注解: {}", annotation);
                }
            }
        }
        for (ASTNode child : node.getChildren()) {
            processNode(child);
        }
    }

    private SystemAnnotation parseAnnotation(String name, String args, int line) {
        if (name == null) return null;

        SystemAnnotationType type = switch (name) {
            case "description" -> SystemAnnotationType.DESCRIPTION;
            case "param" -> SystemAnnotationType.PARAM;
            case "return" -> SystemAnnotationType.RETURN;
            case "example" -> SystemAnnotationType.EXAMPLE;
            case "deprecated" -> SystemAnnotationType.DEPRECATED;
            default -> null;
        };

        if (type == null) return null;

        SystemAnnotation annotation = new SystemAnnotation(type, line);

        if (args != null) {
            // 解析引号包围的参数
            List<String> parsedArgs = parseQuotedArgs(args);
            annotation.setArguments(parsedArgs);
        }

        return annotation;
    }

    private List<String> parseQuotedArgs(String args) {
        List<String> result = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (c == '"') {
                if (inQuote) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                inQuote = !inQuote;
            } else if (inQuote) {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /** 获取 @@description 注解 */
    public Optional<SystemAnnotation> getDescription() {
        return annotations.stream()
                .filter(a -> a.type == SystemAnnotationType.DESCRIPTION)
                .findFirst();
    }

    public List<SystemAnnotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    public enum SystemAnnotationType {
        DESCRIPTION, PARAM, RETURN, EXAMPLE, DEPRECATED
    }

    @Getter
    public static class SystemAnnotation {
        private final SystemAnnotationType type;
        private final int line;
        private List<String> arguments = new ArrayList<>();

        public SystemAnnotation(SystemAnnotationType type, int line) {
            this.type = type;
            this.line = line;
        }

        public void setArguments(List<String> arguments) { this.arguments = arguments; }

        @Override
        public String toString() {
            return String.format("@@%s(%s) [line:%d]", type, arguments, line);
        }

        public List<String> getArguments() {
            return arguments;
        }

        public SystemAnnotationType getType() {
            return type;
        }
    }
}

