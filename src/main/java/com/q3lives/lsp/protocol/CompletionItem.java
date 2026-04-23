package com.q3lives.lsp.protocol;

/**
 * LSP 补全项增强
 *
 * 扩展标准的 CompletionItem 以支持 Claw 特定功能
 */
public class CompletionItem extends org.eclipse.lsp4j.CompletionItem {

    public CompletionItem() {
        super();
    }

    public CompletionItem(String label, org.eclipse.lsp4j.CompletionItemKind kind, String detail, String documentation, String insertText) {
        super(label);
        this.setKind(kind);
    }

    /**
     * 创建基础类型补全项
     */
    public static CompletionItem createTypeCompletion(String typeName, String description) {
        return new CompletionItem(
            typeName,
            getCompletionItemKind(typeName),
            typeName,
            description,
            typeName
        );
    }

    /**
     * 创建函数补全项
     */
    public static CompletionItem createFunctionCompletion(String functionName, String params, String returnType, String description) {
        String detail = String.format("function (%s) -> %s", params, returnType);

        return new CompletionItem(
            functionName,
            org.eclipse.lsp4j.CompletionItemKind.Function,
            detail,
            description,
            String.format("%s($0)", functionName)
        );
    }

    /**
     * 创建变量补全项
     */
    public static CompletionItem createVariableCompletion(String variableName, String typeName, String description) {
        return new CompletionItem(
            variableName,
            org.eclipse.lsp4j.CompletionItemKind.Variable,
            typeName,
            description,
            variableName
        );
    }

    /**
     * 创建注解补全项
     */
    public static CompletionItem createAnnotationCompletion(String annotationName, String description) {
        return new CompletionItem(
            annotationName,
            org.eclipse.lsp4j.CompletionItemKind.Snippet,
            annotationName,
            description,
            "@" + annotationName
        );
    }

    /**
     * 获取类型对应的 CompletionItemKind
     */
    private static org.eclipse.lsp4j.CompletionItemKind getCompletionItemKind(String typeName) {
        String lower = typeName.toLowerCase();
        if (lower.equals("int") || lower.equals("integer")) {
            return org.eclipse.lsp4j.CompletionItemKind.Value;
        } else if (lower.equals("float") || lower.equals("double")) {
            return org.eclipse.lsp4j.CompletionItemKind.Value;
        } else if (lower.equals("string")) {
            return org.eclipse.lsp4j.CompletionItemKind.Value;
        } else if (lower.equals("bool") || lower.equals("boolean")) {
            return org.eclipse.lsp4j.CompletionItemKind.Value;
        } else if (lower.equals("void")) {
            return org.eclipse.lsp4j.CompletionItemKind.Class;
        } else {
            return org.eclipse.lsp4j.CompletionItemKind.Text;
        }
    }
}
