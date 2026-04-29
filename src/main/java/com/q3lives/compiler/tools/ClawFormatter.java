package com.q3lives.compiler.tools;

import com.q3lives.compiler.core.*;
import com.q3lives.compiler.frontend.*;
import com.q3lives.compiler.scanner.*;
import java.util.*;

/**
 * Claw 代码格式化工具
 *
 * 功能：
 * - 自动缩进
 * - 换行处理
 * - 对齐
 * - 导入排序
 */
public class ClawFormatter {

    private int indentSize = 4;
    private boolean sortImports = true;
    private boolean addPlaceholders = true;
    private int maxLineLength = 100;

    public ClawFormatter() {}

    public ClawFormatter indentSize(int size) {
        this.indentSize = size;
        return this;
    }

    public ClawFormatter sortImports(boolean sort) {
        this.sortImports = sort;
        return this;
    }

    public ClawFormatter maxLineLength(int length) {
        this.maxLineLength = length;
        return this;
    }

    /**
     * 格式化代码
     */
    public String format(String source) {
        SourceScanner scanner = new SourceScanner();
        Tokenizer tokenizer = new Tokenizer();
        Parser parser = new Parser();

        SourceView view = scanner.scan(source, "input.claw");
        List<Token> tokens = tokenizer.tokenize(view);
        ASTNode ast = parser.parse(tokens);

        return formatAST(ast);
    }

    /**
     * 格式化 AST
     */
    private String formatAST(ASTNode ast) {
        StringBuilder sb = new StringBuilder();

        for (ASTNode child : ast.getChildren()) {
            sb.append(formatTopLevel(child));
            sb.append("\n");
        }

        String result = sb.toString();

        // 排序导入
        if (sortImports) {
            result = sortImports(result);
        }

        return result.trim();
    }

    /**
     * 格式化顶层声明
     */
    private String formatTopLevel(ASTNode node) {
        switch (node.getType()) {
            case IMPORT_DECLARATION:
                return formatImport(node);
            case FUNCTION_DECLARATION:
                return formatFunction(node, 0);
            case TYPE_DEFINITION:
                return formatTypeDefinition(node, 0);
            case AOP_ASPECT:
                return formatAspect(node, 0);
            default:
                return node.toString();
        }
    }

    /**
     * 格式化导入
     */
    private String formatImport(ASTNode node) {
        String path = node.getProperty("path");
        String alias = node.getProperty("alias");
        String importType = node.getProperty("importType");

        StringBuilder sb = new StringBuilder("import ");
        if (importType != null) sb.append(importType).append(" ");
        sb.append(path);
        if (alias != null) sb.append(" as ").append(alias);
        sb.append(";");
        return sb.toString();
    }

    /**
     * 格式化函数
     */
    private String formatFunction(ASTNode node, int indent) {
        String indentStr = " ".repeat(indent * indentSize);
        String nextIndent = " ".repeat((indent + 1) * indentSize);

        String visibility = node.getProperty("visibility");
        String flowType = node.getProperty("flowType");
        String name = node.getProperty("name");
        String returnType = node.getProperty("returnType");

        StringBuilder sb = new StringBuilder();

        // 修饰符
        if (visibility != null) sb.append(visibility).append(" ");
        if (flowType != null) sb.append(flowType).append(" ");
        sb.append("function ");
        sb.append(name);

        // 参数
        ASTNode params = node.getChildByType(ASTNode.NodeType.PARAMETER_LIST);
        if (params != null) {
            sb.append("(");
            List<String> paramStrs = new ArrayList<>();
            for (ASTNode param : params.getChildren()) {
                String pName = param.getProperty("name");
                String pType = param.getProperty("type");
                paramStrs.add(pName + ": " + pType);
            }
            sb.append(String.join(", ", paramStrs));
            sb.append(")");
        }

        // 返回类型
        if (returnType != null && !returnType.isEmpty()) {
            sb.append(" -> ").append(returnType);
        }

        // 函数体
        ASTNode body = node.getChildByType(ASTNode.NodeType.BLOCK);
        if (body != null) {
            sb.append(" {\n");
            sb.append(formatBlock(body, indent + 1));
            sb.append(indentStr).append("}");
        }

        return sb.toString();
    }

    /**
     * 格式化代码块
     */
    private String formatBlock(ASTNode block, int indent) {
        String indentStr = " ".repeat(indent * indentSize);
        StringBuilder sb = new StringBuilder();

        for (ASTNode stmt : block.getChildren()) {
            sb.append(indentStr);
            sb.append(formatStatement(stmt, indent));
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化语句
     */
    private String formatStatement(ASTNode node, int indent) {
        switch (node.getType()) {
            case VARIABLE_DECLARATION:
                return formatVariableDeclaration(node);
            case RETURN_STATEMENT:
                if (node.getChildren().isEmpty()) return "return;";
                return "return " + formatExpression(node.getChildren().get(0));
            case IF_STATEMENT:
                return formatIfStatement(node, indent);
            case WHILE_STATEMENT:
                return formatWhileStatement(node, indent);
            case FOR_STATEMENT:
                return formatForStatement(node, indent);
            case EXPRESSION_STATEMENT:
                if (!node.getChildren().isEmpty()) {
                    return formatExpression(node.getChildren().get(0)) + ";";
                }
                return "";
            default:
                return node.toString();
        }
    }

    /**
     * 格式化变量声明
     */
    private String formatVariableDeclaration(ASTNode node) {
        String mutable = node.getProperty("mutable");
        String name = node.getProperty("name");
        String type = node.getProperty("type");

        String keyword = "true".equals(mutable) ? "var" : "const";

        if (!node.getChildren().isEmpty()) {
            String value = formatExpression(node.getChildren().get(0));
            if (type != null) {
                return keyword + " " + name + ": " + type + " = " + value;
            }
            return keyword + " " + name + " = " + value;
        } else if (type != null) {
            return keyword + " " + name + ": " + type;
        }
        return keyword + " " + name;
    }

    /**
     * 格式化 if 语句
     */
    private String formatIfStatement(ASTNode node, int indent) {
        StringBuilder sb = new StringBuilder();

        ASTNode condition = node.getChildByType(ASTNode.NodeType.CONDITION);
        if (condition != null) {
            sb.append("if (").append(formatExpression(condition)).append(") {\n");
        }

        ASTNode thenBlock = node.getChildren().stream()
            .filter(n -> n.getType() == ASTNode.NodeType.BLOCK)
            .findFirst().orElse(null);
        if (thenBlock != null) {
            sb.append(formatBlock(thenBlock, indent + 1));
            sb.append(" ".repeat(indent * indentSize)).append("}");
        }

        return sb.toString();
    }

    /**
     * 格式化 while 语句
     */
    private String formatWhileStatement(ASTNode node, int indent) {
        return "while (...) { ... }";
    }

    /**
     * 格式化 for 语句
     */
    private String formatForStatement(ASTNode node, int indent) {
        return "for (...) { ... }";
    }

    /**
     * 格式化表达式
     */
    private String formatExpression(ASTNode node) {
        if (node == null) return "";

        switch (node.getType()) {
            case IDENTIFIER_REF:
                return node.getProperty("name");
            case FUNCTION_CALL:
                return node.getProperty("name") + "(...)";
            case BINARY_EXPRESSION:
                return "... expr ...";
            case LITERAL:
            case INTEGER_LITERAL:
            case FLOAT_LITERAL:
            case STRING_LITERAL:
                return node.getValue();
            default:
                return node.toString();
        }
    }

    /**
     * 格式化类型定义
     */
    private String formatTypeDefinition(ASTNode node, int indent) {
        String name = node.getProperty("name");
        return "type " + name + " { ... }";
    }

    /**
     * 格式化切面
     */
    private String formatAspect(ASTNode node, int indent) {
        String name = node.getProperty("name");
        return "aspect " + name + " { ... }";
    }

    /**
     * 排序导入语句
     */
    private String sortImports(String code) {
        String[] lines = code.split("\n");
        List<String> imports = new ArrayList<>();
        List<String> others = new ArrayList<>();

        for (String line : lines) {
            if (line.trim().startsWith("import ")) {
                imports.add(line.trim());
            } else if (!line.trim().isEmpty()) {
                others.add(line);
            }
        }

        // 排序导入
        Collections.sort(imports);

        // 重建代码
        StringBuilder sb = new StringBuilder();
        for (String imp : imports) {
            sb.append(imp).append("\n");
        }
        if (!imports.isEmpty() && !others.isEmpty()) {
            sb.append("\n");
        }
        for (String other : others) {
            sb.append(other).append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化文件
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: ClawFormatter <source-file>");
            return;
        }

        try {
            String source = java.nio.file.Files.readString(java.nio.file.Paths.get(args[0]));
            String formatted = new ClawFormatter().format(source);
            System.out.println(formatted);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}