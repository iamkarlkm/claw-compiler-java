package com.q3lives.lsp.provider;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;

import com.q3lives.compiler.context.SemanticContext;
import com.q3lives.lsp.utils.DiagnosticGenerator;
import com.q3lives.lsp.utils.PerformanceMonitor;

/**
 * LSP 悬停信息提供器
 *
 * 负责实现 "悬停" 功能：
 * - 显示符号类型信息
 * - 显示函数参数
 * - 显示类型信息
 * - 显示文档字符串
 */
public class HoverProvider {

    private final SemanticContext semanticContext;
    private final CompletionProvider completionProvider;

    // 符号提取模式
    private static final Pattern SYMBOL_PATTERN = Pattern.compile(
        "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b"
    );

    public HoverProvider(SemanticContext semanticContext, CompletionProvider completionProvider) {
        this.semanticContext = semanticContext;
        this.completionProvider = completionProvider;
    }

    /**
     * 提供悬停信息
     *
     * @param document 文档内容
     * @param position 光标位置
     * @return 悬停信息
     */
    public Hover provideHover(String document, Position position) {
        long startTime = System.currentTimeMillis();

        try {
            // 解析文档内容
            String[] lines = document.split("\n");

            // 检查行号是否有效
            if (position.getLine() < 0 || position.getLine() >= lines.length) {
                return null;
            }

            // 获取当前行
            String currentLine = lines[position.getLine()];

            // 检查character是否在有效范围内
            if (position.getCharacter() < 0 || position.getCharacter() > currentLine.length()) {
                return null;
            }

            // 识别当前符号
            String symbolName = extractSymbolName(currentLine, position.getCharacter());

            if (symbolName == null || symbolName.isEmpty()) {
                return null;
            }

            // 获取符号信息
            // 生成悬停内容
            String hoverText = generateHoverText(symbolName);

            // 创建悬停对象
            Hover hover = new Hover();
            hover.setContents(new MarkupContent(
                MarkupKind.MARKDOWN,
                hoverText
            ));

            // 设置悬停范围
            org.eclipse.lsp4j.Range range = new org.eclipse.lsp4j.Range(
                new org.eclipse.lsp4j.Position(position.getLine(), position.getCharacter() - symbolName.length()),
                position
            );
            hover.setRange(range);

            return hover;

        } catch (Exception e) {
            System.err.println("Error providing hover: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            // 记录性能
            PerformanceMonitor.getInstance().record("hover",
                System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 从行中提取符号名称
     *
     * @param line 当前行
     * @param charIndex 光标位置
     * @return 符号名称
     */
    private String extractSymbolName(String line, int charIndex) {
        if (charIndex <= 0) {
            return null;
        }

        // 从光标位置向前查找符号的开始
        int start = charIndex - 1;
        while (start >= 0 && isSymbolChar(line.charAt(start))) {
            start--;
        }

        start++;

        // 从光标位置向后查找符号的结束
        int end = charIndex;
        while (end < line.length() && isSymbolChar(line.charAt(end))) {
            end++;
        }

        if (start >= end) {
            return null;
        }

        // 返回最短的匹配符号（光标位置到符号结束），而不是整个符号
        String longestSymbol = line.substring(start, end).trim();
        String shortestSymbol = line.substring(start, charIndex).trim();

        // 返回非空的符号（优先返回最短匹配）
        if (!shortestSymbol.isEmpty()) {
            return shortestSymbol;
        }
        if (!longestSymbol.isEmpty()) {
            return longestSymbol;
        }

        return null;
    }

    /**
     * 检查是否是符号字符
     */
    private boolean isSymbolChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == ':' || c == '.';
    }

    /**
     * 获取符号信息
     *
     * @param symbolName 符号名称
     * @return 符号信息
     */
    private SymbolInfo getSymbolInfo(String symbolName) {
        // TODO: 从语义上下文中获取符号信息
        // 这里应该从 semanticContext 中查找符号的类型、参数、返回值等信息

        // 临时返回通用符号信息
        SymbolInfo info = new SymbolInfo();
        info.setName(symbolName);
        info.setType("unknown");
        info.setParams(new java.util.ArrayList<>());
        info.setReturnType("any");

        // 根据名称判断类型
        info.setType(determineSymbolType(symbolName));

        return info;
    }

    /**
     * 判断符号类型
     *
     * @param name 符号名称
     * @return 类型字符串
     */
    private String determineSymbolType(String name) {
        // 常见的函数名模式
        if (name.matches("(print|input|len)\\w*")) {
            return "function";
        }

        // 常见的关键字
        if (name.equals("if") || name.equals("else") || name.equals("while") ||
            name.equals("for") || name.equals("return") || name.equals("break") ||
            name.equals("continue") || name.equals("switch") || name.equals("case") ||
            name.equals("default")) {
            return "keyword";
        }

        // 常见的类型名
        if (name.equals("Int") || name.equals("Float") || name.equals("String") ||
            name.equals("Bool") || name.equals("Void") || name.equals("Any")) {
            return "type";
        }

        // 常见的注解
        if (name.equals("@Before") || name.equals("@After") || name.equals("@Around") ||
            name.equals("@AfterReturning") || name.equals("@AfterThrowing") ||
            name.equals("@Aspect")) {
            return "annotation";
        }

        // 默认为变量
        return "variable";
    }

    /**
     * 生成悬停文本
     *
     * @param name 符号名称
     * @return Markdown 格式的悬停文本
     */
    private String generateHoverText(String name) {
        StringBuilder sb = new StringBuilder();

        // 符号名称
        sb.append("### ").append(name).append("\n\n");

        // 确定类型
        String symbolType = determineSymbolType(name);

        if ("type".equals(symbolType)) {
            sb.append("**Type:** `").append(name).append("`\n");
            sb.append("\n基础类型：int, float, string, boolean, void, any\n");
        } else if ("function".equals(symbolType)) {
            sb.append("**Function:** ").append(name).append("\n\n");

            // 临时参数（TODO: 从编译器获取真实参数）
            sb.append("**Parameters:**\n");
            sb.append("- `a: any` (临时参数)\n");
            sb.append("- `b: any` (临时参数)\n");

            // 返回类型
            sb.append("\n**Return Type:** `any`\n");
        } else if ("variable".equals(symbolType)) {
            sb.append("**Variable:** ").append(name).append("\n\n");

            // 变量类型
            sb.append("**Type:** `any`\n");

            // TODO: 添加默认值
            // sb.append("**Default Value:** `").append(defaultValue).append("`\n");
        } else if ("annotation".equals(symbolType)) {
            sb.append("**Annotation:** ").append(name).append("\n\n");

            // 注解描述
            String description = getAnnotationDescription(name);
            sb.append("**Description:**\n").append(description).append("\n");
        } else if ("keyword".equals(symbolType)) {
            sb.append("**Keyword:** ").append(name).append("\n\n");

            // 关键字描述
            String description = getKeywordDescription(name);
            sb.append("**Description:**\n").append(description).append("\n");
        }

        return sb.toString();
    }

    /**
     * 获取注解描述
     */
    private String getAnnotationDescription(String annotationName) {
        return switch (annotationName) {
            case "@Before" -> "@Before - 前置通知，在方法执行前调用";
            case "@After" -> "@After - 后置通知，在方法执行后调用";
            case "@Around" -> "@Around - 环绕通知，可以控制方法执行";
            case "@AfterReturning" -> "@AfterReturning - 返回值后通知，在方法成功返回后调用";
            case "@AfterThrowing" -> "@AfterThrowing - 异常后通知，在方法抛出异常后调用";
            case "@Aspect" -> "@Aspect - 切面定义，定义通知的切入点";
            default -> "自定义注解，用于 AOP 编程";
        };
    }

    /**
     * 获取关键字描述
     */
    private String getKeywordDescription(String keyword) {
        return switch (keyword) {
            case "if" -> "if - 条件语句，执行条件为真的代码块";
            case "else" -> "else - 条件语句，当 if 条件为假时执行";
            case "while" -> "while - 循环语句，当条件为真时持续执行";
            case "for" -> "for - 循环语句，遍历集合或范围";
            case "function" -> "function - 定义函数";
            case "aspect" -> "aspect - 定义切面";
            case "return" -> "return - 从函数返回";
            case "break" -> "break - 跳出循环";
            case "continue" -> "continue - 继续下一次循环";
            default -> "控制流语句";
        };
    }

    /**
     * 符号信息类
     */
    private static class SymbolInfo {
        private String name;
        private String type;
        private List<String> params;
        private String returnType;
        private String defaultValue;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getParams() {
            return params;
        }

        public void setParams(List<String> params) {
            this.params = params;
        }

        public String getReturnType() {
            return returnType;
        }

        public void setReturnType(String returnType) {
            this.returnType = returnType;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }
}
