// ==================== EntityRecognizer.java ====================
package com.claw.compiler.decomposer;

import com.claw.compiler.hierarchy.BlockType;
import com.claw.compiler.hierarchy.CodeBlock;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 实体识别器 - 识别代码中的各种实体
 */
@Slf4j
public class EntityRecognizer {

    // private static final Pattern FUNC_SIGNATURE = 
    //     Pattern.compile("(normal|exception|flow)?\\s*(public|private)?\\s*function\\s+(\\w+)\\s*\$(.*)\$\\s*(->[\\s\\w]+)?");
        private static final Pattern FUNC_SIGNATURE = 
    Pattern.compile("(normal|exception|flow)?\\s*(public|private)?\\s*function\\s+(\\w+)\\s*\\((.*)\\)\\s*(->[\\s\\w]+)?");

    private static final Pattern VAR_DECL = 
        Pattern.compile("(var|const)\\s+(\\w+)\\s*(?::\\s*(\\w+))?\\s*(?:=\\s*(.+))?");
    private static final Pattern TYPE_DEF = 
        Pattern.compile("(public|private)?\\s*type\\s+(\\w+)");
    private static final Pattern IMPORT_STMT = 
        Pattern.compile("import\\s+(.+)");
        //这个修改将\$替换为\\(和\\)，以正确匹配函数声明中的参数列表括号。这样既解决了编译错误，也使正则表达式能够正确匹配代码中的函数声明结构。
        private static final Pattern PROGRAM_ANNOTATION = 
    Pattern.compile("@(BeforeName|AfterName|BeforeProps|AfterProps)\\s*\\((.+)\\)");

    // private static final Pattern PROGRAM_ANNOTATION = 
    //     Pattern.compile("@(BeforeName|AfterName|BeforeProps|AfterProps)\\s*\$(.+)\$");
        private static final Pattern SYSTEM_ANNOTATION = 
    Pattern.compile("@@(description|param|return|example|deprecated)\\s*\\((.+)\\)");

    // private static final Pattern SYSTEM_ANNOTATION = 
    //     Pattern.compile("@@(description|param|return|example|deprecated)\\s*\$(.+)\$");

    /**
     * 从代码块识别实体
     */
    public RecognizedEntity recognize(CodeBlock block) {
        if (block.getContent() == null || block.getContent().isBlank()) {
            return null;
        }

        return switch (block.getBlockType()) {
            case FUNCTION_BLOCK -> recognizeFunction(block);
            case TYPE_DEFINITION_BLOCK -> recognizeType(block);
            case VARIABLE_DECLARATION_BLOCK -> recognizeVariable(block);
            case IMPORT_DECLARATION_BLOCK -> recognizeImport(block);
            case ANNOTATION_BLOCK -> recognizeAnnotation(block);
            default -> null;
        };
    }

    private RecognizedEntity recognizeFunction(CodeBlock block) {
        String firstLine = getFirstEffectiveLine(block);
        Matcher m = FUNC_SIGNATURE.matcher(firstLine);
        if (m.find()) {
            RecognizedEntity entity = new RecognizedEntity(EntityType.FUNCTION, block);
            entity.setName(m.group(3));
            entity.setAttribute("flowType", m.group(1));
            entity.setAttribute("visibility", m.group(2));
            entity.setAttribute("parameters", m.group(4));
            entity.setAttribute("returnType", m.group(5));
            return entity;
        }
        return null;
    }

    private RecognizedEntity recognizeType(CodeBlock block) {
        String firstLine = getFirstEffectiveLine(block);
        Matcher m = TYPE_DEF.matcher(firstLine);
        if (m.find()) {
            RecognizedEntity entity = new RecognizedEntity(EntityType.TYPE_DEFINITION, block);
            entity.setName(m.group(2));
            entity.setAttribute("visibility", m.group(1));
            return entity;
        }
        return null;
    }

    private RecognizedEntity recognizeVariable(CodeBlock block) {
        String content = block.getContent().trim();
        Matcher m = VAR_DECL.matcher(content);
        if (m.find()) {
            boolean isConst = "const".equals(m.group(1));
            RecognizedEntity entity = new RecognizedEntity(
                isConst ? EntityType.CONSTANT : EntityType.VARIABLE, block);
            entity.setName(m.group(2));
            entity.setAttribute("typeAnnotation", m.group(3));
            entity.setAttribute("initialValue", m.group(4));
            entity.setAttribute("mutable", !isConst);
            return entity;
        }
        return null;
    }

    private RecognizedEntity recognizeImport(CodeBlock block) {
        String content = block.getContent().trim();
        Matcher m = IMPORT_STMT.matcher(content);
        if (m.find()) {
            RecognizedEntity entity = new RecognizedEntity(EntityType.IMPORT, block);
            entity.setName(m.group(1).trim());
            return entity;
        }
        return null;
    }

    private RecognizedEntity recognizeAnnotation(CodeBlock block) {
        String content = block.getContent().trim();
        Matcher m = PROGRAM_ANNOTATION.matcher(content);
        if (m.find()) {
            RecognizedEntity entity = new RecognizedEntity(EntityType.ANNOTATION, block);
            entity.setName("@" + m.group(1));
            entity.setAttribute("annotationType", "program");
            entity.setAttribute("arguments", m.group(2));
            return entity;
        }
        m = SYSTEM_ANNOTATION.matcher(content);
        if (m.find()) {
            RecognizedEntity entity = new RecognizedEntity(EntityType.ANNOTATION, block);
            entity.setName("@@" + m.group(1));
            entity.setAttribute("annotationType", "system");
            entity.setAttribute("arguments", m.group(2));
            return entity;
        }
        return null;
    }

    private String getFirstEffectiveLine(CodeBlock block) {
        return block.getLines().stream()
                .filter(l -> l.isEffective())
                .map(l -> l.getTrimmedContent())
                .findFirst()
                .orElse("");
    }

    /** 识别结果 */
    @Getter
    public static class RecognizedEntity {
        private final EntityType type;
        private final CodeBlock sourceBlock;
        private String name;
        private final Map<String, Object> attributes = new HashMap<>();

        public RecognizedEntity(EntityType type, CodeBlock sourceBlock) {
            this.type = type;
            this.sourceBlock = sourceBlock;
        }

        public void setName(String name) { this.name = name; }

        public void setAttribute(String key, Object value) {
            if (value != null) attributes.put(key, value);
        }

        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String key) {
            return (T) attributes.get(key);
        }

        @Override
        public String toString() {
            return String.format("Entity[%s: %s, attrs=%s]", type, name, attributes);
        }
    }
}

