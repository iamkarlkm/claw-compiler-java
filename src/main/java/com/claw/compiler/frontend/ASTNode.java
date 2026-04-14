// ==================== ASTNode.java ====================
package com.claw.compiler.frontend;

import lombok.Getter;
import lombok.Setter;
import java.util.*;

/**
 * 抽象语法树节点
 */
@Getter
public class ASTNode {

    public NodeType getType() {
        return type;
    }

    public List<ASTNode> getChildren() {
        return children;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int startLine) {
        this.line = startLine;
    }

    public enum NodeType {
        // 顶层
        PROGRAM, MODULE,
        // 声明
        FUNCTION_DECLARATION, TYPE_DEFINITION, VARIABLE_DECLARATION,
        IMPORT_DECLARATION, PARAMETER_LIST, PARAMETER,
        // 语句
        BLOCK, EXPRESSION_STATEMENT, RETURN_STATEMENT, ASSIGNMENT,
        IF_STATEMENT, ELSE_CLAUSE, FOR_STATEMENT, WHILE_STATEMENT,
        BREAK_STATEMENT, CONTINUE_STATEMENT, THROW_STATEMENT,
        CATCH_CLAUSE,
        // 表达式
        EXPRESSION, BINARY_EXPRESSION, UNARY_EXPRESSION,
        FUNCTION_CALL, MEMBER_ACCESS, ARRAY_LITERAL,
        CONDITION,
        // 字面量
        INTEGER_LITERAL, FLOAT_LITERAL, STRING_LITERAL, BOOLEAN_LITERAL, NULL_LITERAL,
        IDENTIFIER_REF,
        // 注解
        ANNOTATION,
        // 操作流
        FLOW_STATEMENT, IDENTIFIER, LITERAL
    }

    private final NodeType type;
    @Setter
    private int line;
    private final List<ASTNode> children;
    private final Map<String, Object> attributes;
    @Setter
    private ASTNode parent;

    public ASTNode(NodeType type) {
        this.type = type;
        this.children = new ArrayList<>();
        this.attributes = new HashMap<>();
    }

    public void addChild(ASTNode child) {
        child.setParent(this);
        children.add(child);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    /** 以树形格式输出 */
    public String toTreeString() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(this, "", true, sb);
        return sb.toString();
    }

    private void buildTreeString(ASTNode node, String prefix, boolean isLast, StringBuilder sb) {
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        sb.append(node.type);
        if (!node.attributes.isEmpty()) {
            sb.append(" ").append(node.attributes);
        }
        sb.append(" [line:").append(node.line).append("]");
        sb.append(" ");

        for (int i = 0; i < node.children.size(); i++) {
            buildTreeString(node.children.get(i),
                    prefix + (isLast ? "    " : "│   "),
                    i == node.children.size() - 1, sb);
        }
    }

    public String getProperty(String string) {
        Object value = attributes.get(string);
        return value == null ? null : value.toString();
    }

    public ASTNode getChildByType(NodeType nodeType) {
        for (ASTNode child : children) {
            if (child.getType() == nodeType) {
                return child;
            }
        }
        return null;
    }

    public String getValue() {
        String value = getProperty("value");
        if (value != null) {
            return value;
        }
        return getProperty("name");
    }
}

