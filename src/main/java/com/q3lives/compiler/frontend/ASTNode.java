// ==================== ASTNode.java ====================
package com.q3lives.compiler.frontend;

import lombok.Getter;
import lombok.Setter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.q3lives.compiler.common.CompilerConstants;

/**
 * 抽象语法树节点
 */
@Getter
public class ASTNode {

    public ASTNode(NodeType nodeType, int startLine) {
        this.type = nodeType;
        this.line = startLine;
        this.children = new ArrayList<>(CompilerConstants.AST_CHILDREN_INITIAL_CAPACITY);
        this.attributes = new HashMap<>();
        this.visited = false;
    }

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
        FLOW_STATEMENT, IDENTIFIER, LITERAL,AOP_ASPECT,AOP_ADVICE
    }

    private final NodeType type;
    @Setter
    private int line;
    private final List<ASTNode> children;
    private final Map<String, Object> attributes;
    @Setter
    private ASTNode parent;

    // 循环检测标记
    private boolean visited;
    // 深度限制
    private static final int MAX_DEPTH = CompilerConstants.MAX_AST_DEPTH;
    private int depth = 0;

    public ASTNode(NodeType type) {
        this(type, 0);
    }

    public void addChild(ASTNode child) {
        // 检查循环引用
        if (hasCycle(child)) {
            throw new IllegalStateException("检测到循环引用: 在节点 " + this.type + " 中添加子节点 " + child.type);
        }

        // 检查深度限制
        if (this.depth + 1 > MAX_DEPTH) {
            throw new IllegalStateException("AST深度超过限制: " + MAX_DEPTH);
        }

        child.setParent(this);
        child.depth = this.depth + 1;
        children.add(child);
    }

    /**
     * 检查是否存在循环引用
     */
    private boolean hasCycle(ASTNode child) {
        if (child == null) {
            return false;
        }

        ASTNode current = this;
        Set<ASTNode> visitedNodes = new HashSet<>();

        while (current != null) {
            if (visitedNodes.contains(current)) {
                return true; // 检测到循环
            }
            visitedNodes.add(current);
            current = current.parent;
        }

        return false;
    }

    /**
     * 节点访问重置（用于循环检测）
     */
    public void resetVisited() {
        this.visited = false;
        resetChildrenVisited(this);
    }

    private void resetChildrenVisited(ASTNode node) {
        for (ASTNode child : node.children) {
            if (!child.visited) continue;
            child.visited = false;
            resetChildrenVisited(child);
        }
    }

    /**
     * 检查树深度
     */
    public int getDepth() {
        return depth;
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

    /**
     * 创建AST节点的深拷贝
     */
    public ASTNode deepCopy() {
        ASTNode copy = new ASTNode(this.type, this.line);
        copy.depth = this.depth;
        copy.parent = null;
        copy.visited = this.visited;

        // 复制属性
        copy.attributes.putAll(this.attributes);

        // 递归复制子节点
        for (ASTNode child : this.children) {
            copy.addChild(child.deepCopy());
        }

        return copy;
    }
}

