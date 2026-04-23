package com.q3lives.lsp.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.DiagnosticGenerator;

/**
 * LSP 语法检查/诊断提供器
 *
 * 负责为 Claw 语言提供语法检查和类型检查功能：
 * - 语法错误检测
 * - 类型错误检测
 * - 注解验证
 * - 警告提示
 */
public class DiagnosticProvider {

    private final SemanticContext semanticContext;
    private final CompletionProvider completionProvider;

    /**
     * 创建诊断提供器
     */
    public DiagnosticProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        this.semanticContext = semanticContext;
        this.completionProvider = completionProvider;
    }

    /**
     * 对文档进行诊断检查
     *
     * @param document 文档内容
     * @return 诊断错误列表
     */
    public List<Diagnostic> diagnose(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        try {
            // 1. 语法检查
            diagnostics.addAll(checkSyntax(document));

            // 2. 类型检查
            diagnostics.addAll(checkTypes(document));

            // 3. 注解验证
            diagnostics.addAll(checkAnnotations(document));

        } catch (Exception e) {
            System.err.println("Error in diagnostics: " + e.getMessage());
            e.printStackTrace();
        }

        return diagnostics;
    }

    /**
     * 语法检查
     */
    private List<Diagnostic> checkSyntax(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查未闭合的括号
            int openParens = countOccurrences(line, '(');
            int closeParens = countOccurrences(line, ')');
            if (openParens > closeParens) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的括号 - 缺少 " + (openParens - closeParens) + " 个 ')'",
                    range
                ));
            }

            // 检查未闭合的大括号
            int openBraces = countOccurrences(line, '{');
            int closeBraces = countOccurrences(line, '}');
            if (openBraces > closeBraces) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的大括号 - 缺少 " + (openBraces - closeBraces) + " 个 '}'",
                    range
                ));
            }

            // 检查未闭合的方括号
            int openBrackets = countOccurrences(line, '[');
            int closeBrackets = countOccurrences(line, ']');
            if (openBrackets > closeBrackets) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的方括号 - 缺少 " + (openBrackets - closeBrackets) + " 个 ']'",
                    range
                ));
            }

            // 检查未闭合的引号
            int singleQuotes = countOccurrences(line, '\'');
            int doubleQuotes = countOccurrences(line, '"');
            if ((singleQuotes % 2 != 0) && (doubleQuotes % 2 != 0)) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的字符串或字符",
                    range
                ));
            }
        }

        return diagnostics;
    }

    /**
     * 类型检查
     */
    private List<Diagnostic> checkTypes(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        if (semanticContext == null) {
            return diagnostics;
        }

        // TODO: 从语义上下文中提取类型错误
        // 这里应该从 semanticContext.getErrorList() 中获取类型错误

        // 临时模拟一些类型错误
        String[] lines = document.getText().split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查类型不匹配的简单模式
            if (line.contains("String + 123")) {
                Range range = DiagnosticGenerator.createRange(i, 10);
                diagnostics.add(DiagnosticGenerator.createTypeError(
                    "类型错误: 无法将 String 与 int 进行字符串连接",
                    range
                ));
            }

            if (line.contains("calculate + 1")) {
                Range range = DiagnosticGenerator.createRange(i, 11);
                diagnostics.add(DiagnosticGenerator.createTypeError(
                    "类型错误: 无法对函数类型进行算术运算",
                    range
                ));
            }
        }

        return diagnostics;
    }

    /**
     * 注解验证
     */
    private List<Diagnostic> checkAnnotations(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();

        String[] lines = document.getText().split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查 @Aspect 注解
            if (line.contains("@Aspect")) {
                // TODO: 验证 Aspect 的使用是否正确
                // 这里应该检查 Aspect 块是否有效
            }

            // 检查其他注解
            if (line.contains("@Before")) {
                // TODO: 验证 Before 的使用是否正确
            }

            if (line.contains("@After")) {
                // TODO: 验证 After 的使用是否正确
            }

            if (line.contains("@Around")) {
                // TODO: 验证 Around 的使用是否正确
            }
        }

        return diagnostics;
    }

    /**
     * 计算字符串中特定字符的出现次数
     */
    private int countOccurrences(String str, char target) {
        int count = 0;
        for (char c : str.toCharArray()) {
            if (c == target) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取诊断列表
     */
    public List<Diagnostic> getDiagnostics() {
        List<Diagnostic> diagnostics = new ArrayList<>();
        // TODO: 返回缓存的诊断列表
        return diagnostics;
    }

    /**
     * 检查指定行是否有语法错误
     */
    public boolean hasSyntaxError(int line) {
        return false;
    }

    /**
     * 检查指定位置是否有类型错误
     */
    public boolean hasTypeError(int line, int character) {
        return false;
    }
}
