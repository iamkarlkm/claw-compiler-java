package com.q3lives.lsp.provider;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.DiagnosticSeverity;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.compiler.hierarchy.CodeBlock;
import com.q3lives.lsp.utils.DiagnosticGenerator;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * 增强版 LSP 语法检查/诊断提供器
 *
 * 提供全面的语法检查功能：
 * - 语法错误检测
 * - 类型错误检测
 * - 注解验证
 * - 性能问题检测
 * - 最佳实践检查
 */
public class EnhancedDiagnosticProvider extends DiagnosticProvider {

    private final SemanticContext semanticContext;
    private final CompletionProvider completionProvider;
    private final PerformanceMonitor performanceMonitor;
    private static final String OPERATION_NAME = "diagnostic";

    // 错误模式
    private static final Pattern SYNTAX_ERROR_PATTERN = Pattern.compile(
        "\\b(if|else|while|for|function|type|aspect|return|break|continue)\\s*\\("
    );
    private static final Pattern TYPE_ERROR_PATTERN = Pattern.compile(
        "\\b(String|Int|Float|Bool)\\s*\\+\\s*(\\d+|\\d+\\.\\d+)\\b"
    );
    private static final Pattern UNDEFINED_VAR_PATTERN = Pattern.compile(
        "\\b[a-zA-Z_][a-zA-Z0-9_]*\\b(?=\\s*[=;,)])"
    );

    // 诊断缓存
    private final Map<String, List<Diagnostic>> diagnosticCache = new HashMap<>();

    public EnhancedDiagnosticProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        super(semanticContext, completionProvider);
        this.semanticContext = semanticContext;
        this.completionProvider = completionProvider;
        this.performanceMonitor = PerformanceMonitor.getInstance();
    }

    /**
     * 提供增强的诊断检查
     */
    @Override
    public List<Diagnostic> diagnose(TextDocumentItem document) {
        long startTime = performanceMonitor.start(OPERATION_NAME);

        try {
            // 检查缓存
            String cacheKey = document.getUri() + ":" + document.getVersion();
            List<Diagnostic> cached = diagnosticCache.get(cacheKey);
            if (cached != null) {
                performanceMonitor.end(OPERATION_NAME, startTime);
                return cached;
            }

            List<Diagnostic> diagnostics = new ArrayList<>();

            // 0. 编译器错误（最高优先级）
            diagnostics.addAll(getCompilationErrors(document.getUri()));

            // 1. 基础语法检查
            diagnostics.addAll(checkSyntaxEnhanced(document));

            // 2. 增强的类型检查
            diagnostics.addAll(checkTypesEnhanced(document));

            // 3. 注解验证
            diagnostics.addAll(checkAnnotationsEnhanced(document));

            // 4. 性能检查
            diagnostics.addAll(checkPerformance(document));

            // 5. 最佳实践检查
            diagnostics.addAll(checkBestPractices(document));

            // 6. 作用域检查
            diagnostics.addAll(checkScopes(document));

            // 缓存结果
            diagnosticCache.put(cacheKey, diagnostics);

            performanceMonitor.end(OPERATION_NAME, startTime);
            return diagnostics;

        } catch (Exception e) {
            performanceMonitor.end(OPERATION_NAME, startTime);
            System.err.println("Error in diagnostics: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 增强的语法检查
     */
    private List<Diagnostic> checkSyntaxEnhanced(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查未闭合的括号
            if (!checkBrackets(line, '(', ')')) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的括号 - 检查括号匹配",
                    range
                ));
            }

            // 检查未闭合的大括号
            if (!checkBrackets(line, '{', '}')) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的大括号 - 检查大括号匹配",
                    range
                ));
            }

            // 检查未闭合的方括号
            if (!checkBrackets(line, '[', ']')) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的方括号 - 检查方括号匹配",
                    range
                ));
            }

            // 检查未闭合的字符串
            if (!checkQuotes(line)) {
                Range range = DiagnosticGenerator.createRange(i, line.length() - 1);
                diagnostics.add(DiagnosticGenerator.createSyntaxError(
                    "未闭合的字符串或字符",
                    range
                ));
            }

            // 检查分号使用（在语句末尾）
            if (line.trim().endsWith("}") && !line.trim().endsWith("};")) {
                // 可能需要检查是否缺少分号
            }

            // 检查关键字使用
            if (line.contains("function ")) {
                String[] parts = line.split("function\\s+");
                if (parts.length > 1) {
                    String funcName = parts[1].split("\\s*\\(")[0].trim();
                    if (funcName.isEmpty()) {
                        Range range = DiagnosticGenerator.createRange(i, line.indexOf("function"));
                        diagnostics.add(DiagnosticGenerator.createSyntaxError(
                            "函数名不能为空",
                            range
                        ));
                    }
                }
            }
        }

        return diagnostics;
    }

    /**
     * 增强的类型检查
     */
    private List<Diagnostic> checkTypesEnhanced(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查类型不匹配
            Matcher matcher = TYPE_ERROR_PATTERN.matcher(line);
            if (matcher.find()) {
                Range range = DiagnosticGenerator.createRange(i, matcher.start());
                diagnostics.add(DiagnosticGenerator.createTypeError(
                    "类型错误: 无法将字符串与数字进行运算",
                    range
                ));
            }

            // 检查可能的未定义变量
            matcher = UNDEFINED_VAR_PATTERN.matcher(line);
            while (matcher.find()) {
                String varName = matcher.group();
                // 这里应该检查变量是否已定义
                if (!isVariableDefined(varName, i, content)) {
                    Range range = DiagnosticGenerator.createRange(i, matcher.start());
                    diagnostics.add(DiagnosticGenerator.createTypeError(
                        "未定义的变量: " + varName,
                        range
                    ));
                }
            }

            // 检查函数调用
            if (line.contains("(") && line.contains(")")) {
                // 检查函数参数数量
                String funcName = extractFunctionName(line);
                if (funcName != null && !isValidFunctionCall(funcName, line)) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf(funcName));
                    diagnostics.add(DiagnosticGenerator.createTypeError(
                        "函数调用参数不正确: " + funcName,
                        range
                    ));
                }
            }
        }

        return diagnostics;
    }

    /**
     * 增强的注解验证
     */
    private List<Diagnostic> checkAnnotationsEnhanced(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查 @Aspect 注解
            if (line.contains("@Aspect")) {
                // 检查 Aspect 是否有对应的实现
                if (!hasAspectImplementation(i, content)) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf("@Aspect"));
                    diagnostics.add(DiagnosticGenerator.createSyntaxError(
                        "@Aspect 注解需要对应的实现块",
                        range
                    ));
                }
            }

            // 检查 @Before/@After/@Around 注解
            if (line.contains("@Before") || line.contains("@After") || line.contains("@Around")) {
                if (!hasValidPointcut(line)) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf("@"));
                    diagnostics.add(DiagnosticGenerator.createSyntaxError(
                        "切点表达式无效",
                        range
                    ));
                }
            }

            // 检查注解顺序
            if (!isValidAnnotationOrder(line)) {
                Range range = DiagnosticGenerator.createRange(i, line.indexOf("@"));
                diagnostics.add(DiagnosticGenerator.createWarning(
                    "注解顺序建议: @Aspect > @Before > @After > @Around",
                    range
                ));
            }
        }

        return diagnostics;
    }

    /**
     * 性能检查
     */
    private List<Diagnostic> checkPerformance(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查可能的循环中的字符串连接
            if (line.contains("while") && line.contains("+")) {
                Range range = DiagnosticGenerator.createRange(i, line.indexOf("+"));
                diagnostics.add(DiagnosticGenerator.createWarning(
                    "在循环中进行字符串连接可能影响性能",
                    range
                ));
            }

            // 检查可能的内存泄漏
            if (line.contains("malloc") && !line.contains("free")) {
                Range range = DiagnosticGenerator.createRange(i, line.indexOf("malloc"));
                diagnostics.add(DiagnosticGenerator.createWarning(
                    "分配的内存需要释放，避免内存泄漏",
                    range
                ));
            }

            // 检查递归深度
            if (line.contains("function") && line.contains("return")) {
                // 这里应该检查是否有递归调用
            }
        }

        return diagnostics;
    }

    /**
     * 最佳实践检查
     */
    private List<Diagnostic> checkBestPractices(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查函数名约定
            if (line.startsWith("function ")) {
                String funcName = line.substring("function ".length()).split("\\(")[0].trim();
                if (!funcName.matches("[a-z][a-zA-Z0-9_]*")) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf("function"));
                    diagnostics.add(DiagnosticGenerator.createWarning(
                        "函数名应该使用小驼峰命名法",
                        range
                    ));
                }
            }

            // 检查变量名约定
            if (line.startsWith("var ")) {
                String varName = line.substring("var ".length()).split(":")[0].trim();
                if (!varName.matches("[a-z][a-zA-Z0-9_]*")) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf("var"));
                    diagnostics.add(DiagnosticGenerator.createWarning(
                        "变量名应该使用小驼峰命名法",
                        range
                    ));
                }
            }

            // 检查常量定义
            if (line.startsWith("const ")) {
                // 检查常量是否使用全大写
                String constName = line.substring("const ".length()).split("=")[0].trim();
                if (!constName.matches("[A-Z][A-Z0-9_]*")) {
                    Range range = DiagnosticGenerator.createRange(i, line.indexOf("const"));
                    diagnostics.add(DiagnosticGenerator.createWarning(
                        "常量应该使用全大写下划线分隔",
                        range
                    ));
                }
            }
        }

        return diagnostics;
    }

    /**
     * 作用域检查
     */
    private List<Diagnostic> checkScopes(TextDocumentItem document) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        String content = document.getText();
        String[] lines = content.split("\n");

        int braceLevel = 0;
        Stack<Integer> braceStack = new Stack<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 检查大括号
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                if (c == '{') {
                    braceStack.push(i);
                    braceLevel++;
                } else if (c == '}') {
                    if (braceStack.isEmpty()) {
                        Range range = DiagnosticGenerator.createRange(i, j);
                        diagnostics.add(DiagnosticGenerator.createSyntaxError(
                            "多余的右大括号",
                            range
                        ));
                    } else {
                        braceStack.pop();
                        braceLevel--;
                    }
                }
            }
        }

        // 检查未闭合的大括号
        while (!braceStack.isEmpty()) {
            int lineNum = braceStack.pop();
            Range range = DiagnosticGenerator.createRange(lineNum, 0);
            diagnostics.add(DiagnosticGenerator.createSyntaxError(
                "未闭合的大括号，在第 " + (lineNum + 1) + " 行开始",
                range
            ));
        }

        return diagnostics;
    }

    // ==================== 辅助方法 ====================

    /**
     * 检查括号匹配
     */
    private boolean checkBrackets(String line, char open, char close) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == open) {
                count++;
            } else if (c == close) {
                count--;
            }
        }
        return count == 0;
    }

    /**
     * 检查引号
     */
    private boolean checkQuotes(String line) {
        boolean inSingle = false;
        boolean inDouble = false;

        for (char c : line.toCharArray()) {
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            }
        }

        return !inSingle && !inDouble;
    }

    /**
     * 检查变量是否已定义
     */
    private boolean isVariableDefined(String varName, int currentLine, String content) {
        String[] lines = content.split("\n");

        // 向上查找变量定义
        for (int i = currentLine - 1; i >= 0; i--) {
            String line = lines[i];
            if (line.contains("var " + varName) || line.contains("const " + varName)) {
                return true;
            }
            // 如果遇到函数定义，则停止查找
            if (line.contains("function ")) {
                break;
            }
        }

        return false;
    }

    /**
     * 提取函数名
     */
    private String extractFunctionName(String line) {
        if (line.contains("function ")) {
            String[] parts = line.split("function\\s+");
            if (parts.length > 1) {
                return parts[1].split("\\s*\\(")[0].trim();
            }
        }
        return null;
    }

    /**
     * 检查函数调用是否有效
     */
    private boolean isValidFunctionCall(String funcName, String line) {
        // 这里应该根据函数定义检查参数数量
        // 暂时返回 true
        return true;
    }

    /**
     * 检查是否有 Aspect 实现
     */
    private boolean hasAspectImplementation(int line, String content) {
        String[] lines = content.split("\n");

        for (int i = line + 1; i < lines.length; i++) {
            if (lines[i].trim().startsWith("aspect ")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查切点表达式是否有效
     */
    private boolean hasValidPointcut(String line) {
        // 检查切点表达式
        return line.contains("execution") || line.contains("within") || line.contains("this");
    }

    /**
     * 检查注解顺序
     */
    private boolean isValidAnnotationOrder(String line) {
        // Aspect 应该在其他注解之前
        return !line.contains("@Before") && !line.contains("@After") && !line.contains("@Around")
            || !line.contains("@Aspect");
    }

    // ==================== 编译错误管理 ====================

    // 编译错误缓存：URI -> 错误列表
    private final Map<String, List<String>> compilationErrors = new HashMap<>();

    /**
     * 设置指定文档的编译错误
     */
    public void setCompilationErrors(String uri, List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            compilationErrors.remove(uri);
        } else {
            compilationErrors.put(uri, new ArrayList<>(errors));
        }
        // 清除诊断缓存，确保下次 diagnose 会包含新的编译错误
        diagnosticCache.clear();
    }

    /**
     * 清除指定文档的编译错误
     */
    public void clearCompilationErrors(String uri) {
        compilationErrors.remove(uri);
        diagnosticCache.clear();
    }

    /**
     * 将编译错误字符串转换为 Diagnostic 对象
     */
    private List<Diagnostic> getCompilationErrors(String uri) {
        List<String> errors = compilationErrors.get(uri);
        if (errors == null || errors.isEmpty()) {
            return Collections.emptyList();
        }

        List<Diagnostic> diagnostics = new ArrayList<>();
        for (String error : errors) {
            // 尝试从错误信息中解析行号（格式："... 在第 X 行" 或 "line X"）
            int line = 0;
            int column = 0;

            java.util.regex.Pattern linePattern = java.util.regex.Pattern.compile(
                "(?:第|line|Line|at)\\s*(\\d+)");
            java.util.regex.Matcher matcher = linePattern.matcher(error);
            if (matcher.find()) {
                try {
                    line = Integer.parseInt(matcher.group(1)) - 1; // LSP 行号从 0 开始
                    if (line < 0) line = 0;
                } catch (NumberFormatException ignored) {
                }
            }

            Range range = new Range(
                new Position(line, column),
                new Position(line, column + error.length())
            );
            Diagnostic diagnostic = new Diagnostic(range, error, DiagnosticSeverity.Error, "claw-compiler");
            diagnostics.add(diagnostic);
        }
        return diagnostics;
    }
}