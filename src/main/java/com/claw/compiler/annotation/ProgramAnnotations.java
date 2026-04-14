// ==================== ProgramAnnotations.java ====================
package com.claw.compiler.annotation;

import com.claw.compiler.frontend.ASTNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 4个程序注解处理器
 * 
 * @BeforeName / @AfterName - 构造/析构函数
 * @BeforeProps / @AfterProps - 属性变更监听
 */
@Slf4j
public class ProgramAnnotations {

    private static final Pattern ARGS_PATTERN = 
        Pattern.compile("\"([^\"]*?)\"\\s*,\\s*\"([^\"]*?)\"");

    @Getter
    private final List<ProgramAnnotation> annotations = new ArrayList<>();

    /**
     * 处理AST中的程序注解
     */
    public void process(ASTNode ast) {
        processNode(ast);
        log.info("处理了 {} 个程序注解", annotations.size());
    }

    private void processNode(ASTNode node) {
        if (node.getType() == ASTNode.NodeType.ANNOTATION) {
            String category = node.getAttribute("category");
            if ("program".equals(category)) {
                String name = node.getAttribute("name");
                String args = node.getAttribute("arguments");
                ProgramAnnotation annotation = parseAnnotation(name, args, node.getLine());
                if (annotation != null) {
                    annotations.add(annotation);
                    log.debug("程序注解: {}", annotation);
                }
            }
        }
        for (ASTNode child : node.getChildren()) {
            processNode(child);
        }
    }

    private ProgramAnnotation parseAnnotation(String name, String args, int line) {
        if (name == null) return null;

        AnnotationType type = switch (name) {
            case "BeforeName" -> AnnotationType.BEFORE_NAME;
            case "AfterName" -> AnnotationType.AFTER_NAME;
            case "BeforeProps" -> AnnotationType.BEFORE_PROPS;
            case "AfterProps" -> AnnotationType.AFTER_PROPS;
            default -> null;
        };

        if (type == null) return null;

        ProgramAnnotation annotation = new ProgramAnnotation(type, line);

        if (args != null) {
            if (type == AnnotationType.BEFORE_NAME || type == AnnotationType.AFTER_NAME) {
                // @BeforeName("method_name", "target")
                Matcher m = ARGS_PATTERN.matcher(args);
                if (m.find()) {
                    annotation.setMethodName(m.group(1));
                    annotation.setTarget(m.group(2));
                }
            } else {
                // @BeforeProps("prop1,prop2")
                String propsStr = args.replaceAll("\"", "").trim();
                String[] props = propsStr.split(",");
                for (String prop : props) {
                    annotation.addProperty(prop.trim());
                }
            }
        }

        return annotation;
    }

    /** 获取构造函数注解 */
    public List<ProgramAnnotation> getConstructorAnnotations() {
        return annotations.stream()
                .filter(a -> a.type == AnnotationType.BEFORE_NAME)
                .toList();
    }

    /** 获取析构函数注解 */
    public List<ProgramAnnotation> getDestructorAnnotations() {
        return annotations.stream()
                .filter(a -> a.type == AnnotationType.AFTER_NAME)
                .toList();
    }

    /** 获取属性变更前监听注解 */
    public List<ProgramAnnotation> getBeforePropsAnnotations() {
        return annotations.stream()
                .filter(a -> a.type == AnnotationType.BEFORE_PROPS)
                .toList();
    }

    /** 获取属性变更后监听注解 */
    public List<ProgramAnnotation> getAfterPropsAnnotations() {
        return annotations.stream()
                .filter(a -> a.type == AnnotationType.AFTER_PROPS)
                .toList();
    }

    public List<ProgramAnnotation> getAnnotations() {
        return Collections.unmodifiableList(annotations);
    }

    public enum AnnotationType {
        BEFORE_NAME, AFTER_NAME, BEFORE_PROPS, AFTER_PROPS
    }

    @Getter
    public static class ProgramAnnotation {
        private final AnnotationType type;
        private final int line;
        private String methodName;
        private String target;
        private final List<String> properties = new ArrayList<>();

        public ProgramAnnotation(AnnotationType type, int line) {
            this.type = type;
            this.line = line;
        }

        public void setMethodName(String name) { this.methodName = name; }
        public void setTarget(String target) { this.target = target; }
        public void addProperty(String property) { properties.add(property); }

        @Override
        public String toString() {
            return String.format("@%s(method=%s, target=%s, props=%s) [line:%d]",
                    type, methodName, target, properties, line);
        }

        public String getTarget() {
            return target;
        }

        public String getMethodName() {
            return methodName;
        }
    }
}

